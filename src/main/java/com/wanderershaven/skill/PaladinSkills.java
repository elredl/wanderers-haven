package com.wanderershaven.skill;

import com.wanderershaven.classsystem.evolution.EvolutionSkillSet;
import java.util.List;

/**
 * Skill definitions for the Paladin evolution path (Warrior → Paladin).
 *
 * Upgrade chains of note:
 *   Bludgeon [PW1 active] → Smite [PW4 active exclusive]
 */
public final class PaladinSkills {

	private static final String CLASS = "warrior";

	// ── Capstone ──────────────────────────────────────────────────────────────

	private static final SkillDefinition AURA_OF_RIGHTEOUSNESS = skill(
		"warrior_paladin_aura_of_righteousness", 3,
		"Aura of Righteousness",
		"A holy light radiates from you at all times. All allies within 17 blocks take 15% less damage. "
			+ "All enemies within that same radius take 15% more damage."
	);

	// ── Exclusive roll pool ───────────────────────────────────────────────────

	private static final SkillDefinition BURNING_JUSTICE = skill(
		"warrior_paladin_burning_justice", 3,
		"Burning Justice",
		"Your strikes carry divine fire. Each hit sets the target ablaze and deals 18% bonus damage — "
			+ "doubled against the undead."
	);

	private static final SkillDefinition SMITE = activeUpgrade(
		"warrior_paladin_smite", 4,
		"Smite",
		"Call down righteous lightning on all enemies in a cone before you. Deals massive damage — "
			+ "doubled against undead. Reduces armor by 15% and inflicts blindness for 5 seconds. (20 sec cooldown)",
		"warrior_bludgeon"
	);

	private PaladinSkills() {}

	// ── API ───────────────────────────────────────────────────────────────────

	public static EvolutionSkillSet skillSet() {
		return new EvolutionSkillSet(AURA_OF_RIGHTEOUSNESS, List.of(BURNING_JUSTICE, SMITE));
	}

	public static List<SkillDefinition> all() {
		return List.of(AURA_OF_RIGHTEOUSNESS, BURNING_JUSTICE, SMITE);
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private static SkillDefinition skill(String id, int powerLevel, String displayName, String description) {
		return new SkillDefinition(id, CLASS, powerLevel, displayName, description, null, false);
	}

	private static SkillDefinition activeUpgrade(String id, int powerLevel, String displayName, String description, String supersedesId) {
		return new SkillDefinition(id, CLASS, powerLevel, displayName, description, supersedesId, true);
	}
}
