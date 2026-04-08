package com.wanderershaven.skill;

import com.wanderershaven.classsystem.evolution.EvolutionSkillSet;
import java.util.List;

/**
 * Skill definitions for the Berserker evolution path (Warrior → Berserker).
 *
 * Skills here are registered into the roll engine at startup via
 * {@link com.wanderershaven.classsystem.ClassSystemBootstrap} and tagged as exclusive
 * to {@code "warrior_berserker"} — they never appear in any other warrior's roll pool.
 *
 * Notable interaction:
 *   Fighting Spirit is a standalone Berserker-exclusive trigger skill.
 */
public final class BerserkerSkills {

	private static final String CLASS = "warrior";

	// ── Capstone ──────────────────────────────────────────────────────────────
	// Granted the moment the player accepts the Berserker evolution.
	// Replaces the guaranteed skill roll that would otherwise fire at level 25.

	private static final SkillDefinition BERSERKER_RAGE = skill(
		"berserker_rage", 3,
		"Berserker Rage",
		"Your pain is your power. The lower your health, the more damage you deal — "
			+ "up to 50% bonus damage when you are down to half a heart."
	);

	// ── Exclusive roll pool ───────────────────────────────────────────────────
	// Enter the roll pool only for players who have accepted the Berserker evolution.

	private static final SkillDefinition FIGHTING_SPIRIT = skill(
		"fighting_spirit", 3,
		"Fighting Spirit",
		"Adversity only makes you sharper. When you drop to 40% health, gain 50% damage resistance "
			+ "for 30 seconds. (5 min cooldown)"
	);

	/** Dual passive/active: stores incoming damage as Fury, then burns it for size and power. */
	private static final SkillDefinition FURY_UNLEASHED = activeSkill(
		"fury_unleashed", 4,
		"Fury Unleashed",
		"Passive: Every point of damage you take is stored as Fury (up to 100 points). "
			+ "Active: Consume all stored Fury — for each point spent, gain 1% bonus damage and grow slightly "
			+ "for 1 minute. (15 min cooldown)"
	);

	private BerserkerSkills() {}

	// ── API ───────────────────────────────────────────────────────────────────

	/**
	 * The skill set attached to the Berserker evolution:
	 * capstone + all exclusive skills for this path.
	 */
	public static EvolutionSkillSet skillSet() {
		return new EvolutionSkillSet(BERSERKER_RAGE, List.of(FIGHTING_SPIRIT, FURY_UNLEASHED));
	}

	/**
	 * All Berserker skills — capstone and exclusive — used for registration
	 * in the skill engine at startup.
	 */
	public static List<SkillDefinition> all() {
		return List.of(BERSERKER_RAGE, FIGHTING_SPIRIT, FURY_UNLEASHED);
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private static SkillDefinition skill(String id, int powerLevel, String displayName, String description) {
		return SkillDefs.passive(CLASS, id, powerLevel, displayName, description);
	}

	private static SkillDefinition activeSkill(String id, int powerLevel, String displayName, String description) {
		return SkillDefs.active(CLASS, id, powerLevel, displayName, description);
	}

}
