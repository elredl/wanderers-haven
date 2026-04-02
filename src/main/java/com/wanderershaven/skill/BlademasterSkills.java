package com.wanderershaven.skill;

import com.wanderershaven.classsystem.evolution.EvolutionSkillSet;
import java.util.List;

/**
 * Skill definitions for the Blademaster evolution path (Warrior → Blademaster).
 *
 * Upgrade chains of note:
 *   Bludgeon      [PW1 active] → Circular Slash [PW3 active exclusive]
 *   Heavy Strikes [PW1 active] → Focus          [PW4 active exclusive]
 */
public final class BlademasterSkills {

	private static final String CLASS = "warrior";

	// ── Capstone ──────────────────────────────────────────────────────────────

	private static final SkillDefinition BLADE_MASTERY = skill(
		"warrior_blademaster_blade_mastery", 3,
		"Blade Mastery",
		"Your heavy blade is an extension of your will. While wielding a blademaster weapon, "
			+ "gain 25% bonus damage and 18% attack speed."
	);

	// ── Exclusive roll pool ───────────────────────────────────────────────────

	private static final SkillDefinition CIRCULAR_SLASH = activeUpgrade(
		"warrior_blademaster_circular_slash", 3,
		"Circular Slash",
		"One sweeping arc — no angle left uncut. Deal 2.5× weapon damage to every enemy within "
			+ "4 blocks of you. (15 sec cooldown)",
		"warrior_bludgeon"
	);

	private static final SkillDefinition FOCUS = activeUpgrade(
		"warrior_blademaster_focus", 4,
		"Focus",
		"Every strike becomes inevitable. Activate to gain 20% bonus damage and a 60% chance to "
			+ "dodge any incoming attack for 30 seconds. (2 min cooldown)",
		"warrior_heavy_strikes"
	);

	private BlademasterSkills() {}

	// ── API ───────────────────────────────────────────────────────────────────

	public static EvolutionSkillSet skillSet() {
		return new EvolutionSkillSet(BLADE_MASTERY, List.of(CIRCULAR_SLASH, FOCUS));
	}

	public static List<SkillDefinition> all() {
		return List.of(BLADE_MASTERY, CIRCULAR_SLASH, FOCUS);
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private static SkillDefinition skill(String id, int powerLevel, String displayName, String description) {
		return new SkillDefinition(id, CLASS, powerLevel, displayName, description, null, false);
	}

	private static SkillDefinition activeUpgrade(String id, int powerLevel, String displayName, String description, String supersedesId) {
		return new SkillDefinition(id, CLASS, powerLevel, displayName, description, supersedesId, true);
	}
}
