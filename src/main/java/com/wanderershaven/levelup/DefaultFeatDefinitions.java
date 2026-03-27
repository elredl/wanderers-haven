package com.wanderershaven.levelup;

import java.util.List;

public final class DefaultFeatDefinitions {
	private DefaultFeatDefinitions() {}

	public static List<FeatDefinition> create() {
		return List.of(warrior());
	}

	/**
	 * Warrior levelling is driven entirely by killing.
	 *
	 * Base XP per kill (before bonuses):
	 *   Easy mob   (zombie, skeleton…)  →  20 XP  × 1.0 difficulty  =  20 XP
	 *   Medium mob (witch, vindicator…) →  20 XP  × 1.5 difficulty  =  30 XP
	 *   Hard mob   (blaze, enderman…)   →  20 XP  × 2.5 difficulty  =  50 XP
	 *   Boss       (dragon, wither)     →  20 XP  × 10  difficulty  = 200 XP
	 *   Player (level 0)                →  20 XP  × 3.0 difficulty  =  60 XP
	 *   Player (level 10)               →  20 XP  × 8.0 difficulty  = 160 XP
	 *
	 * All values are further scaled by:
	 *   - level bonus:  +5% per attacker level
	 *   - streak bonus: up to 3× for 21+ kills within a 15-second window
	 *
	 * Level curve (baseXp = 100, quadratic):
	 *   Level 1  →   100 XP total   (~5 easy kills)
	 *   Level 2  →   400 XP total   (~20 easy kills)
	 *   Level 5  →  2500 XP total   (~125 easy kills)
	 *   Level 10 → 10000 XP total   (~500 easy kills, or far fewer with harder targets)
	 */
	private static FeatDefinition warrior() {
		return FeatDefinition.builder("warrior")
			.levelCurve(new LevelCurve(100.0, 100))
			.observer(FeatObservers.mobKill(20.0))
			.observer(FeatObservers.playerKill(20.0))
			.build();
	}
}
