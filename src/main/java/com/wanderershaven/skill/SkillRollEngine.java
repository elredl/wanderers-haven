package com.wanderershaven.skill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Handles skill roll probability, power-level selection, and skill award tracking.
 *
 * Roll rules:
 *  - Every multiple of 5 is a guaranteed roll from the current window's PW.
 *  - Level 65+ always rolls (100% chance).
 *  - Levels 1–64 have a linearly-scaling chance: ~20% at level 1, 90% at level 64.
 *
 * Power-level selection (non-guaranteed rolls):
 *  - Window PW (W) = level / 10 + 1, capped at 10.
 *  - W == 1 : PW1 = 100%
 *  - W == 2 : PW1 = 70%,  PW2 = 30%
 *  - W >= 3 : PW(W-1) = 60% (dominant), PW(W) = 25%, remaining 15% split equally below.
 *
 * Duplicates are never awarded — owned skills are excluded from candidate pools.
 * If the chosen PW is exhausted, the engine falls back to any remaining unowned skill.
 */
public final class SkillRollEngine {
	private final Random random = new Random();

	// classId → powerLevel → skills at that PW
	private final Map<String, Map<Integer, List<SkillDefinition>>> registry = new ConcurrentHashMap<>();

	// playerId → classId → owned skill IDs
	private final Map<UUID, Map<String, Set<String>>> playerSkills = new ConcurrentHashMap<>();

	// evolutionId → set of skill IDs exclusive to that evolution
	// Exclusive skills are still registered in the main registry under their classId,
	// but they are filtered out for players who have not accepted that evolution.
	private final Map<String, Set<String>> evolutionExclusiveIds = new ConcurrentHashMap<>();

	// playerId → baseClassId → evolutionId the player has accepted
	private final Map<UUID, Map<String, String>> playerEvolutions = new ConcurrentHashMap<>();

	public SkillRollEngine(Collection<SkillDefinition> initialSkills) {
		for (SkillDefinition skill : initialSkills) {
			register(skill);
		}
	}

	public void register(SkillDefinition skill) {
		registry.computeIfAbsent(skill.classId(), k -> new ConcurrentHashMap<>())
			.computeIfAbsent(skill.powerLevel(), k -> new ArrayList<>())
			.add(skill);
	}

	/**
	 * Register a list of skills as exclusive to a specific evolution path, then add
	 * them to the main registry. Exclusive skills are only offered to players who
	 * have accepted that evolution — all others never see them in rolls.
	 *
	 * <p>All exclusive skills must use the base class ID (e.g. {@code "warrior"})
	 * so they appear in the player's unified skill list.
	 */
	public void registerEvolutionSkills(String evolutionId, List<SkillDefinition> skills) {
		Set<String> ids = evolutionExclusiveIds.computeIfAbsent(evolutionId, k -> ConcurrentHashMap.newKeySet());
		for (SkillDefinition skill : skills) {
			register(skill);
			ids.add(skill.id());
		}
	}

	/**
	 * Record that a player has accepted an evolution, unlocking that evolution's
	 * exclusive skill pool for future rolls.
	 */
	public void grantEvolutionAccess(UUID playerId, String baseClassId, String evolutionId) {
		playerEvolutions.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
			.put(baseClassId, evolutionId);
	}

	/** The evolution ID the player has accepted for the given base class, or null. */
	public String playerEvolutionFor(UUID playerId, String baseClassId) {
		Map<String, String> byClass = playerEvolutions.get(playerId);
		return byClass == null ? null : byClass.get(baseClassId);
	}

	/**
	 * Attempt to award a skill to a player on reaching {@code newLevel} in {@code classId}.
	 *
	 * If the chosen skill has a {@code supersedesId} and the player owns that skill,
	 * the old skill is atomically removed and replaced by the new one.
	 *
	 * Returns the grant result (including whether an upgrade occurred), or empty if no
	 * roll happened or all skills are already owned.
	 */
	public Optional<SkillGrantResult> tryRollSkill(UUID playerId, String classId, int newLevel) {
		if (!shouldRoll(newLevel)) {
			return Optional.empty();
		}

		Map<Integer, List<SkillDefinition>> byPw = registry.get(classId);
		if (byPw == null || byPw.isEmpty()) {
			return Optional.empty();
		}

		Set<String> owned = ownedSkillIds(playerId, classId);
		String evolutionId = playerEvolutionFor(playerId, classId);
		int W = windowPw(newLevel);

		int selectedPw = (newLevel % 5 == 0) ? W : rollPowerLevel(W, byPw);

		List<SkillDefinition> candidates = unownedAt(byPw, owned, selectedPw).stream()
			.filter(s -> isAccessibleTo(s.id(), evolutionId))
			.collect(Collectors.toList());

		// Fallback: any unowned skill across all PWs if the target PW is exhausted
		if (candidates.isEmpty()) {
			candidates = allUnowned(byPw, owned).stream()
				.filter(s -> isAccessibleTo(s.id(), evolutionId))
				.collect(Collectors.toList());
		}

		if (candidates.isEmpty()) {
			return Optional.empty();
		}

		SkillDefinition chosen = candidates.get(random.nextInt(candidates.size()));

		// Get (or create) the mutable owned set for this player+class.
		Set<String> ownedSet = playerSkills
			.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
			.computeIfAbsent(classId, k -> ConcurrentHashMap.newKeySet());

		// If this skill upgrades a lesser version the player already owns, swap it out.
		String supersededId = null;
		if (chosen.supersedesId() != null && ownedSet.contains(chosen.supersedesId())) {
			ownedSet.remove(chosen.supersedesId());
			supersededId = chosen.supersedesId();
		}

		ownedSet.add(chosen.id());
		return Optional.of(new SkillGrantResult(chosen, supersededId));
	}

	/** IDs of all skills this player owns in the given class. */
	public Set<String> ownedSkillIds(UUID playerId, String classId) {
		Map<String, Set<String>> byClass = playerSkills.get(playerId);
		if (byClass == null) {
			return Set.of();
		}
		Set<String> owned = byClass.get(classId);
		if (owned == null || owned.isEmpty()) {
			return Set.of();
		}
		Set<String> effective = ConcurrentHashMap.newKeySet();
		effective.addAll(owned);
		pruneSupersededOwnedSkills(classId, effective);
		return Set.copyOf(effective);
	}

	/** All registered skills across all classes, sorted by class then power level. */
	public List<SkillDefinition> allSkills() {
		return registry.values().stream()
			.flatMap(byPw -> byPw.values().stream())
			.flatMap(List::stream)
			.sorted(Comparator.comparing(SkillDefinition::classId).thenComparingInt(SkillDefinition::powerLevel))
			.toList();
	}

	/**
	 * Directly grant a skill to a player by skill ID, bypassing roll logic.
	 * Returns the granted skill, or empty if the skill ID is not registered.
	 */
	public Optional<SkillDefinition> forceGrantSkill(UUID playerId, String skillId) {
		for (Map<Integer, List<SkillDefinition>> byPw : registry.values()) {
			for (List<SkillDefinition> skills : byPw.values()) {
				for (SkillDefinition skill : skills) {
					if (skill.id().equals(skillId)) {
						Set<String> ownedSet = playerSkills
							.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
							.computeIfAbsent(skill.classId(), k -> ConcurrentHashMap.newKeySet())
						;
						if (skill.supersedesId() != null) {
							ownedSet.remove(skill.supersedesId());
						}
						ownedSet.add(skill.id());
						pruneSupersededOwnedSkills(skill.classId(), ownedSet);
						return Optional.of(skill);
					}
				}
			}
		}
		return Optional.empty();
	}

	/** Find a skill by its ID across all registered classes. */
	public Optional<SkillDefinition> findById(String id) {
		return registry.values().stream()
			.flatMap(byPw -> byPw.values().stream())
			.flatMap(List::stream)
			.filter(s -> s.id().equals(id))
			.findFirst();
	}

	/** Full SkillDefinition objects the player owns in the given class, sorted by PW. */
	public List<SkillDefinition> ownedSkills(UUID playerId, String classId) {
		Set<String> ids = ownedSkillIds(playerId, classId);
		return registry.getOrDefault(classId, Map.of()).values().stream()
			.flatMap(List::stream)
			.filter(s -> ids.contains(s.id()))
			.sorted(Comparator.comparingInt(SkillDefinition::powerLevel))
			.toList();
	}

	// -------------------------------------------------------------------------
	// Roll logic
	// -------------------------------------------------------------------------

	/**
	 * Whether a skill roll happens at all for this level.
	 * Multiples of 5 and levels 65+ are always true.
	 * Levels 1–64 scale linearly from ~20% to 90%.
	 */
	private boolean shouldRoll(int level) {
		if (level % 5 == 0) {
			return true;
		}
		if (level >= 65) {
			return true;
		}
		double chance = 0.20 + (level - 1) * (0.70 / 63.0);
		return random.nextDouble() < chance;
	}

	/**
	 * Window power level for a given player level.
	 * This is the highest PW available and the one used for guaranteed (×5) rolls.
	 *
	 *   Level  1– 9 → PW1
	 *   Level 10–19 → PW2
	 *   Level 20–29 → PW3
	 *   …
	 *   Level 90–99 → PW10  (capped)
	 */
	static int windowPw(int level) {
		return Math.min(level / 10 + 1, 10);
	}

	private int rollPowerLevel(int W, Map<Integer, List<SkillDefinition>> byPw) {
		Map<Integer, Double> weights = pwWeights(W);

		// Keep only PWs that actually have registered skills
		Map<Integer, Double> available = new LinkedHashMap<>();
		for (Map.Entry<Integer, Double> e : weights.entrySet()) {
			if (byPw.containsKey(e.getKey())) {
				available.put(e.getKey(), e.getValue());
			}
		}

		if (available.isEmpty()) {
			return W;
		}

		return weightedRandom(available);
	}

	/**
	 * Compute PW weights for the current window PW {@code W}.
	 * Maintains insertion order so weightedRandom iterates consistently.
	 */
	private static Map<Integer, Double> pwWeights(int W) {
		if (W == 1) {
			return Map.of(1, 1.0);
		}
		if (W == 2) {
			// PW1 is dominant (it's the only "previous")
			return mapOf(1, 0.70, 2, 0.30);
		}

		// W >= 3: PW(W-1) dominant at 60%, PW(W) secondary at 25%, rest share 15%
		Map<Integer, Double> weights = new LinkedHashMap<>();
		int lowerCount = W - 2;
		double perLower = 0.15 / lowerCount;
		for (int pw = 1; pw <= W - 2; pw++) {
			weights.put(pw, perLower);
		}
		weights.put(W - 1, 0.60);
		weights.put(W, 0.25);
		return weights;
	}

	private int weightedRandom(Map<Integer, Double> weights) {
		double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
		double roll = random.nextDouble() * total;
		double cumulative = 0.0;
		for (Map.Entry<Integer, Double> entry : weights.entrySet()) {
			cumulative += entry.getValue();
			if (roll < cumulative) {
				return entry.getKey();
			}
		}
		return weights.keySet().stream().max(Comparator.naturalOrder()).orElse(1);
	}

	// -------------------------------------------------------------------------
	// Candidate helpers
	// -------------------------------------------------------------------------

	private static List<SkillDefinition> unownedAt(
		Map<Integer, List<SkillDefinition>> byPw,
		Set<String> owned,
		int pw
	) {
		return byPw.getOrDefault(pw, List.of()).stream()
			.filter(s -> !owned.contains(s.id()))
			.toList();
	}

	private static List<SkillDefinition> allUnowned(
		Map<Integer, List<SkillDefinition>> byPw,
		Set<String> owned
	) {
		return byPw.values().stream()
			.flatMap(List::stream)
			.filter(s -> !owned.contains(s.id()))
			.toList();
	}

	/**
	 * Returns true if the skill is accessible to a player with the given evolution.
	 *
	 * A skill is accessible when it is not exclusive to any evolution, or when it is
	 * exclusive to exactly the evolution the player has accepted.
	 */
	private boolean isAccessibleTo(String skillId, String playerEvolution) {
		for (Map.Entry<String, Set<String>> entry : evolutionExclusiveIds.entrySet()) {
			if (entry.getValue().contains(skillId)) {
				return entry.getKey().equals(playerEvolution);
			}
		}
		return true;
	}

	// LinkedHashMap preserves insertion order for the weighted random iteration
	private static Map<Integer, Double> mapOf(int k1, double v1, int k2, double v2) {
		Map<Integer, Double> m = new LinkedHashMap<>();
		m.put(k1, v1);
		m.put(k2, v2);
		return m;
	}

	private void pruneSupersededOwnedSkills(String classId, Set<String> ownedIds) {
		if (ownedIds.isEmpty()) return;
		Map<String, SkillDefinition> defsById = registry.getOrDefault(classId, Map.of()).values().stream()
			.flatMap(List::stream)
			.collect(Collectors.toMap(SkillDefinition::id, s -> s, (a, b) -> a));
		if (defsById.isEmpty()) return;

		Set<String> snapshot = Set.copyOf(ownedIds);
		for (String id : snapshot) {
			SkillDefinition def = defsById.get(id);
			if (def == null) continue;
			String superseded = def.supersedesId();
			while (superseded != null) {
				ownedIds.remove(superseded);
				SkillDefinition previous = defsById.get(superseded);
				superseded = previous == null ? null : previous.supersedesId();
			}
		}
	}
}
