package com.wanderershaven.classsystem;

import java.util.List;

public final class DefaultClassDefinitions {
	private DefaultClassDefinitions() {
	}

	public static List<ClassDefinition> create() {
		return List.of(
			warrior()
		);
	}

	// Earned by fighting in melee — hitting mobs/players directly, landing kills, and
	// sustaining damage in close-quarters combat. COMBAT_HIT fires via AttackEntityCallback
	// which only triggers on direct (melee) attacks, not projectiles.
	//
	// Better Combat enrichment adds three additional context keys to COMBAT_HIT signals:
	//   weapon_category — BC weapon type string (e.g. "sword", "axe", "claymore")
	//   two_handed      — "true" / "false"
	//   combo_hit       — combo index of this strike (0-based)
	//
	// Multipliers are independent and multiplicative:
	//   - Only one weapon_category multiplier fires per signal (exact string match)
	//   - two_handed and combo_hit stack on top of weapon_category
	//   - A claymore two-handed combo-3 strike against a player: 1.15 × 1.15 × 1.1 × 1.5 ≈ 2.18×
	private static ClassDefinition warrior() {
		return ClassDefinition.builder("warrior", "Warrior")
			.obtainThreshold(100.0)
			.weight(ClassSignalType.COMBAT_HIT, 1.0)
			.weight(ClassSignalType.KILL, 0.8)
			.weight(ClassSignalType.DAMAGE_TAKEN, 0.3)
			// PvP bonus
			.intentMultiplier(IntentMultipliers.whenContext("target", "minecraft:player", 1.5))
			// Weapon category bonuses — classic warrior-type weapons score higher
			.intentMultiplier(IntentMultipliers.whenContext("weapon_category", "sword", 1.1))
			.intentMultiplier(IntentMultipliers.whenContext("weapon_category", "claymore", 1.15))
			.intentMultiplier(IntentMultipliers.whenContext("weapon_category", "axe", 1.1))
			.intentMultiplier(IntentMultipliers.whenContext("weapon_category", "hammer", 1.1))
			.intentMultiplier(IntentMultipliers.whenContext("weapon_category", "spear", 1.1))
			.intentMultiplier(IntentMultipliers.whenContext("weapon_category", "lance", 1.1))
			// Two-handed weapons indicate commitment to melee
			.intentMultiplier(IntentMultipliers.whenContext("two_handed", "true", 1.15))
			// Executing deeper combo hits shows deliberate, skilled combat
			.intentMultiplier(IntentMultipliers.whenContextMinInt("combo_hit", 2, 1.1))
			.build();
	}
}
