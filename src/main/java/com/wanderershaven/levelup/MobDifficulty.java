package com.wanderershaven.levelup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Difficulty ratings for Minecraft entity types, used to scale XP from mob kills.
 *
 * Scale:
 *   0.0   — passive / trivially killed, no combat XP (animals, villagers, etc.)
 *   1.0   — easy hostile (zombie, skeleton, spider)
 *   1.5   — medium hostile (witch, vindicator, guardian)
 *   2.5   — hard hostile (blaze, enderman, evoker)
 *   3.0+  — elite / mini-boss tier (ravager, piglin brute)
 *   100.0 — boss (ender dragon, wither) — equivalent to ~100 easy kills
 *
 * Any entity type not explicitly listed defaults to 1.0 (easy hostile).
 * Purely passive mobs should be explicitly listed as 0.0 to opt them out.
 */
public final class MobDifficulty {
	private static final Map<String, Double> DIFFICULTIES;

	static {
		Map<String, Double> m = new HashMap<>();

		// Passive / non-combat — 0 XP
		m.put("minecraft:bat",            0.0);
		m.put("minecraft:chicken",        0.0);
		m.put("minecraft:cow",            0.0);
		m.put("minecraft:pig",            0.0);
		m.put("minecraft:sheep",          0.0);
		m.put("minecraft:rabbit",         0.0);
		m.put("minecraft:squid",          0.0);
		m.put("minecraft:glow_squid",     0.0);
		m.put("minecraft:cod",            0.0);
		m.put("minecraft:salmon",         0.0);
		m.put("minecraft:tropical_fish",  0.0);
		m.put("minecraft:pufferfish",     0.0);
		m.put("minecraft:fox",            0.0);
		m.put("minecraft:ocelot",         0.0);
		m.put("minecraft:cat",            0.0);
		m.put("minecraft:horse",          0.0);
		m.put("minecraft:donkey",         0.0);
		m.put("minecraft:mule",           0.0);
		m.put("minecraft:villager",       0.0);
		m.put("minecraft:wandering_trader", 0.0);
		m.put("minecraft:iron_golem",     0.0); // neutral, skip
		m.put("minecraft:snow_golem",     0.0);
		m.put("minecraft:allay",          0.0);
		m.put("minecraft:axolotl",        0.0);
		m.put("minecraft:frog",           0.0);
		m.put("minecraft:tadpole",        0.0);
		m.put("minecraft:camel",          0.0);
		m.put("minecraft:sniffer",        0.0);
		m.put("minecraft:armadillo",      0.0);

		// Easy hostiles — 1.0
		m.put("minecraft:zombie",         1.0);
		m.put("minecraft:skeleton",       1.0);
		m.put("minecraft:spider",         1.0);
		m.put("minecraft:creeper",        1.0);
		m.put("minecraft:drowned",        1.0);
		m.put("minecraft:husk",           1.0);
		m.put("minecraft:stray",          1.0);
		m.put("minecraft:zombie_villager", 1.0);
		m.put("minecraft:pillager",       1.0);
		m.put("minecraft:slime",          0.8);
		m.put("minecraft:magma_cube",     0.8);

		// Medium hostiles — 1.5
		m.put("minecraft:witch",          1.5);
		m.put("minecraft:cave_spider",    1.5);
		m.put("minecraft:vindicator",     1.5);
		m.put("minecraft:phantom",        1.5);
		m.put("minecraft:guardian",       1.5);
		m.put("minecraft:silverfish",     1.2);
		m.put("minecraft:endermite",      1.2);
		m.put("minecraft:piglin",         1.5);
		m.put("minecraft:zombified_piglin", 1.5);
		m.put("minecraft:bogged",         1.5);
		m.put("minecraft:breeze",         2.0);

		// Hard hostiles — 2.5
		m.put("minecraft:wither_skeleton", 2.5);
		m.put("minecraft:blaze",          2.5);
		m.put("minecraft:enderman",       2.5);
		m.put("minecraft:shulker",        2.5);
		m.put("minecraft:evoker",         2.5);
		m.put("minecraft:vex",            2.0);
		m.put("minecraft:hoglin",         2.5);
		m.put("minecraft:zoglin",         2.5);

		// Elite / mini-boss — 3.0+
		m.put("minecraft:ravager",        3.0);
		m.put("minecraft:piglin_brute",   3.0);
		m.put("minecraft:elder_guardian", 4.0);

		// Bosses — massive payouts
		m.put("minecraft:ender_dragon",   100.0);
		m.put("minecraft:wither",         100.0);

		DIFFICULTIES = Collections.unmodifiableMap(m);
	}

	private MobDifficulty() {}

	/**
	 * Returns the difficulty for the given entity type ID.
	 * Unknown entity types default to 1.0 (treated as easy hostiles).
	 * Returns 0.0 for passive mobs that should not grant combat XP.
	 */
	public static double of(String entityTypeId) {
		return DIFFICULTIES.getOrDefault(entityTypeId, 1.0);
	}
}
