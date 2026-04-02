package com.wanderershaven.skill;

import com.wanderershaven.classsystem.evolution.EvolutionSkillSet;
import java.util.List;

/**
 * Skill definitions for the Vanguard evolution path (Warrior → Vanguard).
 *
 * Upgrade chains of note:
 *   Bludgeon [PW1 active] → Shield Bash [PW3 active exclusive]
 */
public final class VanguardSkills {

	private static final String CLASS = "warrior";

	// ── Capstone ──────────────────────────────────────────────────────────────

	private static final SkillDefinition SHIELDS_PROTECTION = skill(
		"shields_protection", 3,
		"Shield's Protection",
		"Your shield anticipates the blow before you do. While holding a shield, one incoming attack "
			+ "is completely blocked automatically every 7 seconds — no reaction required."
	);

	// ── Exclusive roll pool ───────────────────────────────────────────────────

	private static final SkillDefinition SHIELD_BASH = activeUpgrade(
		"shield_bash", 3,
		"Shield Bash",
		"Drive your shield into every enemy in front of you, launching them back and reducing their armor "
			+ "by 25% for 5 seconds. All targets hit are stunned for 1 second. Requires a shield in hand. (15 sec cooldown)",
		"bludgeon"
	);

	private static final SkillDefinition STAND_YOUR_GROUND = skill(
		"stand_your_ground", 4,
		"Stand Your Ground",
		"You do not move. You cannot be knocked back. Each consecutive hit you absorb stacks 5% damage "
			+ "reduction (up to 60%) for 15 seconds — the timer resets on every hit."
	);

	private VanguardSkills() {}

	// ── API ───────────────────────────────────────────────────────────────────

	public static EvolutionSkillSet skillSet() {
		return new EvolutionSkillSet(SHIELDS_PROTECTION, List.of(SHIELD_BASH, STAND_YOUR_GROUND));
	}

	public static List<SkillDefinition> all() {
		return List.of(SHIELDS_PROTECTION, SHIELD_BASH, STAND_YOUR_GROUND);
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private static SkillDefinition skill(String id, int powerLevel, String displayName, String description) {
		return SkillDefs.passive(CLASS, id, powerLevel, displayName, description);
	}

	private static SkillDefinition activeUpgrade(String id, int powerLevel, String displayName, String description, String supersedesId) {
		return SkillDefs.activeUpgrade(CLASS, id, powerLevel, displayName, description, supersedesId);
	}
}
