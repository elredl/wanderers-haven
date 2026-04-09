package com.wanderershaven.skill;

import static com.wanderershaven.skill.SkillDefs.activeUpgrade;
import static com.wanderershaven.skill.SkillDefs.passive;

import com.wanderershaven.classsystem.evolution.EvolutionSkillSet;
import java.util.List;

/**
 * Skill definitions for the Spearmaster evolution path (Warrior -> Spearmaster).
 *
 * Upgrade chains of note:
 *   Bludgeon      [PW1 active] -> Triple Thrust      [PW3 active exclusive]
 *   Heavy Strikes [PW1 active] -> Lengthened Strikes [PW4 active exclusive]
 */
public final class SpearmasterSkills {

	private static final String CLASS = "warrior";

	// -- Capstone ---------------------------------------------------------------

	private static final SkillDefinition SPEAR_MASTERY = passive(
		CLASS,
		"spear_mastery", 3,
		"Spear Mastery",
		"Your spear control is absolute. While wielding a spearmaster weapon, gain 20% attack range and 20% damage."
	);

	// -- Exclusive roll pool ----------------------------------------------------

	private static final SkillDefinition TRIPLE_THRUST = activeUpgrade(
		CLASS,
		"triple_thrust", 3,
		"Triple Thrust",
		"Unleash three rapid thrusts in front of you, each dealing 1.5x weapon damage at 1.3x your current range. (8 sec cooldown)",
		"bludgeon"
	);

	private static final SkillDefinition LENGTHENED_STRIKES = activeUpgrade(
		CLASS,
		"lengthened_strikes", 4,
		"Spear: Lengthened Strikes",
		"For 20 seconds, gain another 20% attack range and deal up to 25% bonus damage based on your distance from the target "
			+ "(max at max attack range). (1 min cooldown)",
		"heavy_strikes"
	);

	private SpearmasterSkills() {}

	// -- API --------------------------------------------------------------------

	public static EvolutionSkillSet skillSet() {
		return new EvolutionSkillSet(SPEAR_MASTERY, List.of(TRIPLE_THRUST, LENGTHENED_STRIKES));
	}

	public static List<SkillDefinition> all() {
		return List.of(SPEAR_MASTERY, TRIPLE_THRUST, LENGTHENED_STRIKES);
	}
}
