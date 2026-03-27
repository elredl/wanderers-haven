package com.wanderershaven.classsystem.evolution;

import java.util.Map;
import java.util.Set;

/**
 * Snapshot of a player's accumulated playstyle data used to evaluate
 * whether they meet an evolution path's prerequisites.
 *
 * All counts are lifetime totals derived from enriched signal context,
 * not rolling windows — they reflect who the player has become, not
 * just what they did recently.
 */
public record EvolutionContext(
	/** Kill counts broken down by entity category.
	 *  Keys: "undead", "arthropod", "illager", "boss", "monster", "player", "neutral" */
	Map<String, Integer> killsByCategory,
	/** Kill counts broken down by weapon category.
	 *  Keys: "blade", "axe", "ranged" (null/missing = unarmed or unknown) */
	Map<String, Integer> killsByWeaponCategory,
	/** How many times the player survived taking a hit at below 10% of max health. */
	int nearDeathSurvivals,
	/** Total kills across all entity categories. */
	int totalKills,
	/** Total hits absorbed by a shield (blocked = true in DAMAGE_TAKEN signals). */
	int blockedHits,
	/** Class IDs the player has already obtained. */
	Set<String> ownedClassIds,
	/** Current level in the class being evolved. */
	int classLevel
) {
	/** Convenience accessor with a default of 0 for unseen entity categories. */
	public int killsOfCategory(String category) {
		return killsByCategory().getOrDefault(category, 0);
	}

	/** Convenience accessor with a default of 0 for unseen weapon categories. */
	public int killsWithWeapon(String weaponCategory) {
		return killsByWeaponCategory().getOrDefault(weaponCategory, 0);
	}
}
