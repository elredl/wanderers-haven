package com.wanderershaven.skill;

import static com.wanderershaven.skill.SkillDefs.activeUpgrade;
import static com.wanderershaven.skill.SkillDefs.passive;

import com.wanderershaven.classsystem.evolution.EvolutionSkillSet;
import java.util.List;

/** Skill definitions for the Executioner evolution path (Warrior -> Executioner). */
public final class ExecutionerSkills {

	private static final String CLASS = "warrior";

	private static final SkillDefinition REAP_LIFE = passive(
		CLASS,
		"reap_life", 3,
		"Reap Life",
		"Gain back 15% of your missing health whenever you kill an enemy."
	);

	private static final SkillDefinition GRASSCUTTER = activeUpgrade(
		CLASS,
		"grasscutter", 3,
		"Grasscutter",
		"Slash all enemies within 3 blocks for 150% weapon damage, dash forward 3 blocks, and slash again. "
			+ "(20 sec cooldown)",
		"piercing_charge"
	);

	private static final SkillDefinition SHADOW_STEP = activeUpgrade(
		CLASS,
		"shadow_step", 4,
		"Shadow Step",
		"Become invulnerable and gain 50% movement speed for 5 seconds, but cannot deal damage during that time. "
			+ "When it ends, deal 200% weapon damage to all enemies you passed through. (60 sec cooldown)",
		"heavy_strikes"
	);

	private ExecutionerSkills() {}

	public static EvolutionSkillSet skillSet() {
		return new EvolutionSkillSet(REAP_LIFE, List.of(GRASSCUTTER, SHADOW_STEP));
	}

	public static List<SkillDefinition> all() {
		return List.of(REAP_LIFE, GRASSCUTTER, SHADOW_STEP);
	}
}
