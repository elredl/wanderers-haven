package com.wanderershaven.skill;

import static com.wanderershaven.skill.SkillDefs.active;
import static com.wanderershaven.skill.SkillDefs.activeUpgrade;

import com.wanderershaven.classsystem.evolution.EvolutionSkillSet;
import java.util.List;

/**
 * Skill definitions for the Blade Dancer evolution path (Warrior -> Blade Dancer).
 *
 * Upgrade chains of note:
 *   Battle Cry (Weak) [PW1 active] -> Harmony of Pain [PW3 active exclusive]
 *   Piercing Charge   [PW1 active] -> Dance of the Butterfly [PW4 active exclusive]
 */
public final class BladeDancerSkills {

	private static final String CLASS = "warrior";

	// -- Capstone ---------------------------------------------------------------

	private static final SkillDefinition DANCE_OF_FALLING_PETALS = active(
		CLASS,
		"dance_of_falling_petals", 3,
		"Dance of Falling Petals",
		"You erupt into a chaotic blade dance, striking all enemies within 5 blocks 2.5 times per second for 10 seconds. "
			+ "Each pulse deals 40% weapon damage (up to 10x total over the full duration). (30 sec cooldown)"
	);

	// -- Exclusive roll pool ----------------------------------------------------

	private static final SkillDefinition HARMONY_OF_PAIN = activeUpgrade(
		CLASS,
		"harmony_of_pain", 3,
		"Harmony of Pain",
		"Unleash a battle cry that afflicts all enemies within 5 blocks for 10 seconds: they are slowed by 40%, "
			+ "take 10% more damage, and deal 10% less damage. (45 sec cooldown)",
		"battle_cry_weak"
	);

	private static final SkillDefinition DANCE_OF_THE_BUTTERFLY = activeUpgrade(
		CLASS,
		"dance_of_the_butterfly", 4,
		"Dance of the Butterfly",
		"Move with impossible grace for 5 seconds, gaining 50% movement speed and dealing 150% weapon damage "
			+ "to enemies you pass through. (20 sec cooldown)",
		"piercing_charge"
	);

	private BladeDancerSkills() {}

	// -- API --------------------------------------------------------------------

	public static EvolutionSkillSet skillSet() {
		return new EvolutionSkillSet(DANCE_OF_FALLING_PETALS, List.of(HARMONY_OF_PAIN, DANCE_OF_THE_BUTTERFLY));
	}

	public static List<SkillDefinition> all() {
		return List.of(DANCE_OF_FALLING_PETALS, HARMONY_OF_PAIN, DANCE_OF_THE_BUTTERFLY);
	}

}
