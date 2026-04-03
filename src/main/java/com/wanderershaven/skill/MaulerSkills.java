package com.wanderershaven.skill;

import static com.wanderershaven.skill.SkillDefs.activeUpgrade;
import static com.wanderershaven.skill.SkillDefs.passive;

import com.wanderershaven.classsystem.evolution.EvolutionSkillSet;
import java.util.List;

/** Skill definitions for the Mauler evolution path (Warrior -> Mauler). */
public final class MaulerSkills {

	private static final String CLASS = "warrior";

	private static final SkillDefinition CRUSHING_BLOWS = passive(
		CLASS,
		"crushing_blows", 3,
		"Crushing Blows",
		"Your heavy weapon caves in anything it touches. While holding a mauler weapon, deal 40% more damage."
	);

	private static final SkillDefinition GROUND_SLAM = activeUpgrade(
		CLASS,
		"ground_slam", 3,
		"Ground Slam",
		"Crash your weapon into the earth, dealing 200% weapon damage to all enemies within 6 blocks and slowing them by 40%. "
			+ "(15 sec cooldown)",
		"bludgeon"
	);

	private static final SkillDefinition CRUSHING_LEAP = activeUpgrade(
		CLASS,
		"crushing_leap", 4,
		"Crushing Leap",
		"Launch forward in a massive leap and slam down, dealing 300% weapon damage to all enemies within 3 blocks on landing. "
			+ "You take no fall damage during the leap. (20 sec cooldown)",
		"piercing_charge"
	);

	private MaulerSkills() {}

	public static EvolutionSkillSet skillSet() {
		return new EvolutionSkillSet(CRUSHING_BLOWS, List.of(GROUND_SLAM, CRUSHING_LEAP));
	}

	public static List<SkillDefinition> all() {
		return List.of(CRUSHING_BLOWS, GROUND_SLAM, CRUSHING_LEAP);
	}
}
