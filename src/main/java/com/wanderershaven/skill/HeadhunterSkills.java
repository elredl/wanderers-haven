package com.wanderershaven.skill;

import static com.wanderershaven.skill.SkillDefs.activeUpgrade;
import static com.wanderershaven.skill.SkillDefs.passive;

import com.wanderershaven.classsystem.evolution.EvolutionSkillSet;
import java.util.List;

/** Skill definitions for the Headhunter evolution path (Warrior -> Headhunter). */
public final class HeadhunterSkills {

	private static final String CLASS = "warrior";

	private static final SkillDefinition GRIEVOUS_WOUNDS = passive(
		CLASS,
		"grievous_wounds", 3,
		"Grievous Wounds",
		"Your attacks have a 20% chance to inflict Bleed for 3 seconds. Bleeding targets take ticking damage "
			+ "equal to 30% of the original hit over the duration, and receive reduced healing and regeneration while bleeding."
	);

	private static final SkillDefinition MADDENING_STRIKES = activeUpgrade(
		CLASS,
		"maddening_strikes", 3,
		"Maddening Strikes",
		"For 10 seconds, gain 12% damage and 30% attack speed. During the effect, each strike applies stacking armor reduction "
			+ "(5% per stack, up to 35%) to targets you hit. (30 sec cooldown)",
		"heavy_strikes"
	);

	private static final SkillDefinition BLOODTHIRST = passive(
		CLASS,
		"bloodthirst", 4,
		"Bloodthirst",
		"Your bleed chance increases to 30%, and you gain 8% damage for each bleeding unit within 8 blocks."
	);

	private HeadhunterSkills() {}

	public static EvolutionSkillSet skillSet() {
		return new EvolutionSkillSet(GRIEVOUS_WOUNDS, List.of(MADDENING_STRIKES, BLOODTHIRST));
	}

	public static List<SkillDefinition> all() {
		return List.of(GRIEVOUS_WOUNDS, MADDENING_STRIKES, BLOODTHIRST);
	}
}
