package com.wanderershaven.classsystem;

import com.wanderershaven.classsystem.evolution.WarriorEvolutions;
import com.wanderershaven.skill.WarriorSkills;
import java.util.List;

/** Central source of default class + subclass content. */
public final class DefaultClassContent {
	private DefaultClassContent() {}

	public static List<ClassContentDefinition> all() {
		return List.of(warriorContent());
	}

	private static ClassContentDefinition warriorContent() {
		ClassDefinition warrior = ClassDefinition.builder("warrior", "Warrior")
			.obtainThreshold(100.0)
			.weight(ClassSignalType.COMBAT_HIT, 1.0)
			.weight(ClassSignalType.KILL, 0.8)
			.weight(ClassSignalType.DAMAGE_TAKEN, 0.3)
			.intentMultiplier(IntentMultipliers.whenContext("target", "minecraft:player", 1.5))
			.intentMultiplier(IntentMultipliers.whenContext("weapon_category", "sword", 1.1))
			.intentMultiplier(IntentMultipliers.whenContext("weapon_category", "claymore", 1.15))
			.intentMultiplier(IntentMultipliers.whenContext("weapon_category", "axe", 1.1))
			.intentMultiplier(IntentMultipliers.whenContext("weapon_category", "hammer", 1.1))
			.intentMultiplier(IntentMultipliers.whenContext("weapon_category", "spear", 1.1))
			.intentMultiplier(IntentMultipliers.whenContext("weapon_category", "lance", 1.1))
			.intentMultiplier(IntentMultipliers.whenContext("two_handed", "true", 1.15))
			.intentMultiplier(IntentMultipliers.whenContextMinInt("combo_hit", 2, 1.1))
			.build();

		return new ClassContentDefinition(
			warrior,
			WarriorSkills.all(),
			WarriorEvolutions.all()
		);
	}
}
