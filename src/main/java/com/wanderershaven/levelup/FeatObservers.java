package com.wanderershaven.levelup;

import com.wanderershaven.classsystem.ClassSignalType;
import java.util.UUID;

/**
 * Factory methods for the built-in {@link FeatObserver} implementations.
 *
 * XP formula shared by mob and player kill observers:
 *
 *   xp = baseXp * difficulty * levelBonus * streakBonus
 *
 * where:
 *   levelBonus  = 1.0 + (attackerLevel * 0.05)   — 5% more XP per level earned
 *   streakBonus = see streakMultiplier()           — up to 3.0x for large kill streaks
 */
public final class FeatObservers {
	private FeatObservers() {}

	/**
	 * Awards XP for killing any non-player entity, scaled by {@link MobDifficulty}.
	 * Passive mobs (difficulty 0.0) yield no XP.
	 *
	 * @param baseXp base XP for an easy hostile (difficulty 1.0) at level 0
	 */
	public static FeatObserver mobKill(double baseXp) {
		return context -> {
			if (context.signal().type() != ClassSignalType.KILL) {
				return 0.0;
			}

			String target = context.signal().context().get("target");
			if (target == null || "minecraft:player".equals(target)) {
				return 0.0;
			}

			double difficulty = MobDifficulty.of(target);
			if (difficulty <= 0.0) {
				return 0.0;
			}

			return baseXp * difficulty * levelBonus(context) * streakBonus(context);
		};
	}

	/**
	 * Awards XP for killing another player, scaled by the target's class level.
	 * A level-0 player counts as difficulty 3.0; each level they hold adds +0.5.
	 *
	 * @param baseXp base XP multiplied against the computed target difficulty
	 */
	public static FeatObserver playerKill(double baseXp) {
		return context -> {
			if (context.signal().type() != ClassSignalType.KILL) {
				return 0.0;
			}

			String target = context.signal().context().get("target");
			if (!"minecraft:player".equals(target)) {
				return 0.0;
			}

			int targetLevel = resolveTargetLevel(context);
			double targetDifficulty = 3.0 + (targetLevel * 0.5);

			return baseXp * targetDifficulty * levelBonus(context) * streakBonus(context);
		};
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static int resolveTargetLevel(FeatContext context) {
		String uuidStr = context.signal().context().get("target_uuid");
		if (uuidStr == null) {
			return 0;
		}
		try {
			return Math.max(0, context.playerLevelLookup().apply(UUID.fromString(uuidStr)));
		} catch (IllegalArgumentException ignored) {
			return 0;
		}
	}

	/**
	 * 5% additive bonus per attacker level.
	 * Level 0 → 1.0×, Level 10 → 1.5×, Level 20 → 2.0×
	 */
	private static double levelBonus(FeatContext context) {
		return 1.0 + (context.attackerLevel() * 0.05);
	}

	/**
	 * Kill streak multiplier. Streaks reset if no kill lands within 300 game ticks (~15 s).
	 *
	 *  1–2  kills → 1.0×  (no bonus)
	 *  3–5  kills → 1.3×
	 *  6–10 kills → 1.7×
	 * 11–20 kills → 2.2×
	 * 21+   kills → 3.0×
	 */
	private static double streakBonus(FeatContext context) {
		int streak = context.killStreak();
		if (streak <= 2)  return 1.0;
		if (streak <= 5)  return 1.3;
		if (streak <= 10) return 1.7;
		if (streak <= 20) return 2.2;
		return 3.0;
	}
}
