package com.wanderershaven.levelup;

import com.wanderershaven.classsystem.ClassSignal;
import com.wanderershaven.classsystem.ClassSignalType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Observes class signals, runs all registered {@link FeatDefinition}s, accumulates XP,
 * and fires {@link LevelUpEvent}s when a player advances a level.
 *
 * This engine is deliberately separate from the affinity inference engine — it answers
 * "how skilled is this player?" while the affinity engine answers "what class fits this player?".
 *
 * Kill streak window: consecutive kills within {@value STREAK_EXPIRY_TICKS} game ticks
 * (~15 real seconds at 20 TPS) are treated as a single streak for bonus XP.
 */
public final class ClassLevelEngine {
	private static final long STREAK_EXPIRY_TICKS = 300;

	private final Map<String, FeatDefinition> definitions = new ConcurrentHashMap<>();
	private final Map<UUID, PlayerLevelState> stateByPlayer = new ConcurrentHashMap<>();
	private final Map<UUID, ConcurrentLinkedQueue<LevelUpEvent>> pendingLevelUps = new ConcurrentHashMap<>();

	public ClassLevelEngine(Collection<FeatDefinition> initialDefinitions) {
		for (FeatDefinition def : initialDefinitions) {
			definitions.put(def.classId(), def);
		}
	}

	public void registerFeatDefinition(FeatDefinition definition) {
		definitions.put(definition.classId(), definition);
	}

	/**
	 * Process a signal through all registered feat definitions.
	 * Must be called from the server tick thread; not designed for concurrent signal submission.
	 */
	public void observe(ClassSignal signal) {
		if (definitions.isEmpty()) {
			return;
		}

		PlayerLevelState state = stateByPlayer.computeIfAbsent(signal.playerId(), id -> new PlayerLevelState());

		synchronized (state) {
			updateStreak(state, signal);

			for (FeatDefinition def : definitions.values()) {
				// playerLevelLookup reads other players' states without holding their locks —
				// safe because all calls originate from the single server tick thread.
				FeatContext ctx = new FeatContext(
					signal,
					levelFor(state, def),
					state.killStreak,
					targetId -> {
						PlayerLevelState targetState = stateByPlayer.get(targetId);
						return targetState == null ? 0 : levelFor(targetState, def);
					}
				);

				double xpGained = 0.0;
				for (FeatObserver observer : def.observers()) {
					xpGained += observer.observe(ctx);
				}

				if (xpGained > 0.0) {
					applyXp(state, def, signal.playerId(), xpGained);
				}
			}
		}
	}

	/** Drain all pending level-up events for a player. Call after {@link #observe}. */
	public List<LevelUpEvent> drainLevelUpEvents(UUID playerId) {
		ConcurrentLinkedQueue<LevelUpEvent> queue = pendingLevelUps.get(playerId);
		if (queue == null || queue.isEmpty()) {
			return List.of();
		}

		List<LevelUpEvent> result = new ArrayList<>();
		LevelUpEvent event;
		while ((event = queue.poll()) != null) {
			result.add(event);
		}
		return result;
	}

	/** Current level for a player in a given class (0 = not yet reached level 1). */
	public int classLevel(UUID playerId, String classId) {
		FeatDefinition def = definitions.get(classId);
		if (def == null) {
			return 0;
		}

		PlayerLevelState state = stateByPlayer.get(playerId);
		if (state == null) {
			return 0;
		}

		synchronized (state) {
			return levelFor(state, def);
		}
	}

	/**
	 * Force the player up by exactly one level in the given class.
	 * Does nothing if the class has no registered feat definition or the player is already at max level.
	 */
	public void grantLevel(UUID playerId, String classId) {
		FeatDefinition def = definitions.get(classId);
		if (def == null) {
			return;
		}

		PlayerLevelState state = stateByPlayer.computeIfAbsent(playerId, id -> new PlayerLevelState());

		synchronized (state) {
			int currentLevel = levelFor(state, def);
			if (currentLevel >= def.levelCurve().maxLevel()) {
				return;
			}

			double xpNeeded = def.levelCurve().totalXpForLevel(currentLevel + 1) - state.totalXpFor(classId);
			if (xpNeeded > 0) {
				applyXp(state, def, playerId, xpNeeded);
			}
		}
	}

	/** Full level profile snapshot for a player in a given class. */
	public Optional<ClassLevelProfile> profile(UUID playerId, String classId) {
		FeatDefinition def = definitions.get(classId);
		if (def == null) {
			return Optional.empty();
		}

		PlayerLevelState state = stateByPlayer.get(playerId);
		double totalXp = 0.0;
		if (state != null) {
			synchronized (state) {
				totalXp = state.totalXpFor(classId);
			}
		}

		int level = def.levelCurve().levelAtXp(totalXp);
		double progressXp = def.levelCurve().xpProgressInLevel(totalXp, level);
		double xpToNext = def.levelCurve().xpToNextLevel(level);
		return Optional.of(new ClassLevelProfile(classId, level, progressXp, xpToNext));
	}

	// -------------------------------------------------------------------------
	// Internals
	// -------------------------------------------------------------------------

	private void updateStreak(PlayerLevelState state, ClassSignal signal) {
		if (signal.type() != ClassSignalType.KILL) {
			return;
		}

		long now = signal.gameTime();
		if (state.lastKillTick >= 0 && (now - state.lastKillTick) > STREAK_EXPIRY_TICKS) {
			state.killStreak = 0;
		}
		state.killStreak++;
		state.lastKillTick = now;
	}

	private int levelFor(PlayerLevelState state, FeatDefinition def) {
		return def.levelCurve().levelAtXp(state.totalXpFor(def.classId()));
	}

	private void applyXp(PlayerLevelState state, FeatDefinition def, UUID playerId, double xp) {
		double xpBefore = state.totalXpFor(def.classId());
		state.addXp(def.classId(), xp);
		double xpAfter = state.totalXpFor(def.classId());

		int levelBefore = def.levelCurve().levelAtXp(xpBefore);
		int levelAfter  = def.levelCurve().levelAtXp(xpAfter);

		if (levelAfter > levelBefore) {
			pendingLevelUps
				.computeIfAbsent(playerId, id -> new ConcurrentLinkedQueue<>())
				.add(new LevelUpEvent(playerId, def.classId(), levelAfter));
		}
	}

	private static final class PlayerLevelState {
		private final Map<String, Double> totalXpByClass = new HashMap<>();
		private int killStreak = 0;
		private long lastKillTick = -1;

		double totalXpFor(String classId) {
			return totalXpByClass.getOrDefault(classId, 0.0);
		}

		void addXp(String classId, double xp) {
			totalXpByClass.merge(classId, xp, Double::sum);
		}
	}
}
