package com.wanderershaven.levelup;

/**
 * Defines the XP requirements for each level using a quadratic curve:
 *
 *   totalXpForLevel(n) = baseXp * n²
 *
 * Examples with baseXp = 100:
 *   Level 1  →   100 XP total
 *   Level 2  →   400 XP total  (300 XP to advance)
 *   Level 5  →  2500 XP total
 *   Level 10 → 10000 XP total
 *   Level 20 → 40000 XP total
 */
public record LevelCurve(double baseXp, int maxLevel) {
	public static LevelCurve defaults() {
		return new LevelCurve(100.0, 100);
	}

	/** Total accumulated XP required to reach level {@code n} from zero. */
	public double totalXpForLevel(int level) {
		if (level <= 0) {
			return 0.0;
		}
		return baseXp * level * level;
	}

	/** Level the player is at given their total accumulated XP. */
	public int levelAtXp(double xp) {
		if (xp < baseXp) {
			return 0;
		}
		return Math.min((int) Math.floor(Math.sqrt(xp / baseXp)), maxLevel);
	}

	/** XP still needed to advance from {@code currentLevel} to the next. */
	public double xpToNextLevel(int currentLevel) {
		if (currentLevel >= maxLevel) {
			return 0.0;
		}
		return totalXpForLevel(currentLevel + 1) - totalXpForLevel(currentLevel);
	}

	/** XP earned within the current level (resets conceptually each level). */
	public double xpProgressInLevel(double totalXp, int currentLevel) {
		return totalXp - totalXpForLevel(currentLevel);
	}
}
