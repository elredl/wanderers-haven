package com.wanderershaven.classsystem.evolution;

import com.wanderershaven.classsystem.ClassSignal;
import com.wanderershaven.classsystem.ClassSignalType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tracks per-player playstyle signal data and evaluates evolution eligibility.
 *
 * Responsibilities:
 *   - Maintain a lightweight {@link PlayerSignalLedger} per player from enriched signals
 *   - Determine which evolution paths a player qualifies for at a given level milestone
 *   - Track pending evolution offers that have not yet been accepted
 *
 * This engine is deliberately separate from {@code AffinityClassInferenceEngine} —
 * that engine handles long-term affinity for initial class unlock, while this one
 * tracks discrete playstyle counts specifically for milestone-gated progression.
 */
public final class ClassEvolutionEngine {

	/** Near-death threshold: < 10% HP when a hit lands = near-death survival. */
	private static final float NEAR_DEATH_THRESHOLD = 0.10f;

	/** Registered evolutions grouped by base class ID. */
	private final Map<String, List<ClassEvolutionDef>> evolutionsByBase = new ConcurrentHashMap<>();

	/** Per-player running signal counts used for prerequisite evaluation. */
	private final Map<UUID, PlayerSignalLedger> ledgers = new ConcurrentHashMap<>();

	/**
	 * Evolution offers that have been presented to a player but not yet resolved.
	 * Maps player UUID → list of eligible evolutions at the milestone.
	 * Cleared when the player makes a choice (or when a new milestone overwrites it).
	 */
	private final Map<UUID, List<ClassEvolutionDef>> pendingOffers = new ConcurrentHashMap<>();

	/** Records which evolution each player has committed to, keyed by base class ID. */
	private final Map<UUID, Map<String, String>> acceptedEvolutions = new ConcurrentHashMap<>();

	public ClassEvolutionEngine(Collection<ClassEvolutionDef> initialEvolutions) {
		initialEvolutions.forEach(this::register);
	}

	// ── Registration ──────────────────────────────────────────────────────────

	public void register(ClassEvolutionDef def) {
		evolutionsByBase.computeIfAbsent(def.baseClassId(), k -> new ArrayList<>()).add(def);
	}

	// ── Signal ingestion ──────────────────────────────────────────────────────

	/**
	 * Update the player's playstyle ledger from an enriched signal.
	 * Called by {@code ClassEventIngestionService} after every {@code ingest()}.
	 */
	public void processSignal(ClassSignal signal) {
		PlayerSignalLedger ledger = ledgers.computeIfAbsent(signal.playerId(), k -> new PlayerSignalLedger());

		switch (signal.type()) {
			case KILL -> {
				ledger.totalKills++;
				String category = signal.context().get("entity_category");
				if (category != null) {
					ledger.killsByCategory.merge(category, 1, Integer::sum);
				}
				String weaponCategory = signal.context().get("weapon_category");
				if (weaponCategory != null) {
					ledger.killsByWeaponCategory.merge(weaponCategory, 1, Integer::sum);
				}
			}
			case DAMAGE_TAKEN -> {
				// Near-death survival: player took a hit and survived at < 10% HP
				String ratioStr = signal.context().get("health_ratio");
				if (ratioStr != null) {
					try {
						if (Float.parseFloat(ratioStr) < NEAR_DEATH_THRESHOLD) {
							ledger.nearDeathSurvivals++;
						}
					} catch (NumberFormatException ignored) {}
				}
				// Blocked hit (shield parry)
				if ("true".equals(signal.context().get("blocked"))) {
					ledger.blockedHits++;
				}
			}
			default -> {}
		}
	}

	// ── Eligibility ───────────────────────────────────────────────────────────

	/**
	 * Returns all evolution paths the player qualifies for at this milestone.
	 *
	 * @param playerId      the player to evaluate
	 * @param baseClassId   the class that just hit a 25-level milestone
	 * @param classLevel    the milestone level (25, 50, 75, ...)
	 * @param ownedClassIds all class IDs the player currently holds
	 */
	public List<ClassEvolutionDef> eligibleEvolutions(
		UUID playerId,
		String baseClassId,
		int classLevel,
		Set<String> ownedClassIds
	) {
		List<ClassEvolutionDef> candidates = evolutionsByBase.getOrDefault(baseClassId, List.of());
		if (candidates.isEmpty()) return List.of();

		EvolutionContext ctx = buildContext(playerId, classLevel, ownedClassIds);
		return candidates.stream()
			.filter(def -> def.isEligible(ctx))
			.collect(Collectors.toList());
	}

	/** Build a full {@link EvolutionContext} for a player. */
	public EvolutionContext buildContext(UUID playerId, int classLevel, Set<String> ownedClassIds) {
		PlayerSignalLedger ledger = ledgers.getOrDefault(playerId, new PlayerSignalLedger());
		return new EvolutionContext(
			Map.copyOf(ledger.killsByCategory),
			Map.copyOf(ledger.killsByWeaponCategory),
			ledger.nearDeathSurvivals,
			ledger.totalKills,
			ledger.blockedHits,
			Set.copyOf(ownedClassIds),
			classLevel
		);
	}

	// ── Pending offers ────────────────────────────────────────────────────────

	/** Record that this player has been offered the given evolutions to choose from. */
	public void setPendingOffer(UUID playerId, List<ClassEvolutionDef> offer) {
		pendingOffers.put(playerId, List.copyOf(offer));
	}

	/** Returns the current pending evolution offer for this player, or empty if none. */
	public List<ClassEvolutionDef> getPendingOffer(UUID playerId) {
		return pendingOffers.getOrDefault(playerId, List.of());
	}

	/** Clear the pending offer (call after the player makes a selection). */
	public void clearPendingOffer(UUID playerId) {
		pendingOffers.remove(playerId);
	}

	// ── Acceptance ────────────────────────────────────────────────────────────

	/**
	 * Record that the player has accepted an evolution path.
	 *
	 * Returns the accepted {@link ClassEvolutionDef} so the caller can apply the
	 * capstone skill and register exclusive skills. Returns empty if the evolution
	 * ID is not registered.
	 */
	public Optional<ClassEvolutionDef> acceptEvolution(UUID playerId, String evolutionId) {
		for (Map.Entry<String, List<ClassEvolutionDef>> entry : evolutionsByBase.entrySet()) {
			for (ClassEvolutionDef def : entry.getValue()) {
				if (def.id().equals(evolutionId)) {
					acceptedEvolutions
						.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
						.put(def.baseClassId(), evolutionId);
					clearPendingOffer(playerId);
					return Optional.of(def);
				}
			}
		}
		return Optional.empty();
	}

	/** Returns the accepted evolution ID for the given base class, or null if none chosen. */
	public String acceptedEvolution(UUID playerId, String baseClassId) {
		Map<String, String> byBase = acceptedEvolutions.get(playerId);
		return byBase == null ? null : byBase.get(baseClassId);
	}

	// ── Debug / admin ─────────────────────────────────────────────────────────

	/** All registered evolutions for a base class. */
	public List<ClassEvolutionDef> evolutionsFor(String baseClassId) {
		return List.copyOf(evolutionsByBase.getOrDefault(baseClassId, List.of()));
	}

	// ── Internal state ────────────────────────────────────────────────────────

	private static final class PlayerSignalLedger {
		final Map<String, Integer> killsByCategory = new ConcurrentHashMap<>();
		final Map<String, Integer> killsByWeaponCategory = new ConcurrentHashMap<>();
		volatile int totalKills;
		volatile int nearDeathSurvivals;
		volatile int blockedHits;
	}
}
