package com.wanderershaven.skill;

import static com.wanderershaven.skill.SkillDefs.active;
import static com.wanderershaven.skill.SkillDefs.activeUpgrade;
import static com.wanderershaven.skill.SkillDefs.passive;

import com.wanderershaven.classsystem.evolution.EvolutionSkillSet;
import java.util.List;

/**
 * Skill definitions for the Duelist evolution path (Warrior → Duelist).
 *
 * Upgrade chains of note:
 *   Piercing Charge [PW1 active] → Flash Step [PW4 active exclusive]
 */
public final class DuelistSkills {

	private static final String CLASS = "warrior";

	// ── Capstone ──────────────────────────────────────────────────────────────

	private static final SkillDefinition PARRY = active(
		CLASS,
		"parry", 3,
		"Parry",
		"Timing is everything. Activate to open a 0.5-second parry window — any attack that lands "
			+ "within it is deflected, and every enemy within 2 blocks of you takes 150% of your "
			+ "weapon damage. No cooldown."
	);

	// ── Exclusive roll pool ───────────────────────────────────────────────────

	private static final SkillDefinition SWIFT_BLADE = passive(
		CLASS,
		"swift_blade", 3,
		"Swift Blade",
		"Light weapons, fast hands. Gain 20% attack speed while wielding any light blade."
	);

	private static final SkillDefinition FLASH_STEP = activeUpgrade(
		CLASS,
		"flash_step", 4,
		"Flash Step",
		"Blink 5 blocks in the direction you are looking, dealing 3× weapon damage to every enemy "
			+ "in your path and bursting forward at +30% speed for 3 seconds. (8 sec cooldown)",
		"piercing_charge"
	);

	private DuelistSkills() {}

	// ── API ───────────────────────────────────────────────────────────────────

	public static EvolutionSkillSet skillSet() {
		return new EvolutionSkillSet(PARRY, List.of(SWIFT_BLADE, FLASH_STEP));
	}

	public static List<SkillDefinition> all() {
		return List.of(PARRY, SWIFT_BLADE, FLASH_STEP);
	}

}
