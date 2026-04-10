package com.wanderershaven.skill;

import static com.wanderershaven.skill.SkillDefs.activeUpgrade;
import static com.wanderershaven.skill.SkillDefs.passive;
import static com.wanderershaven.skill.SkillDefs.upgrade;

import com.wanderershaven.classsystem.evolution.EvolutionSkillSet;
import java.util.List;

/**
 * Skill definitions for the Berserker evolution path (Warrior → Berserker).
 *
 * Skills here are registered into the roll engine at startup via
 * {@link com.wanderershaven.classsystem.ClassSystemBootstrap} and tagged as exclusive
 * to {@code "warrior_berserker"} — they never appear in any other warrior's roll pool.
 *
 * Upgrade chains of note:
 *   Battle Cry (Weak) -> Mocking Shout
 *   Piercing Charge -> Spinning Slash
 *   Berserker Rage -> Enhanced Berserker Rage
 *   Heavy Strikes -> Savage Onslaught
 *   Bludgeon -> Fury Becomes Flesh
 */
public final class BerserkerSkills {

	private static final String CLASS = "warrior";

	// ── Capstone ──────────────────────────────────────────────────────────────
	// Granted the moment the player accepts the Berserker evolution.
	// Replaces the guaranteed skill roll that would otherwise fire at level 25.

	private static final SkillDefinition BERSERKER_RAGE = passive(
		CLASS,
		"berserker_rage", 3,
		"Berserker Rage",
		"Your pain is your power. The lower your health, the more damage you deal — "
			+ "up to 50% bonus damage when you are down to half a heart."
	);

	// ── Exclusive roll pool ───────────────────────────────────────────────────
	// Enter the roll pool only for players who have accepted the Berserker evolution.

	private static final SkillDefinition BATTLE_FURY = passive(
		CLASS,
		"battle_fury", 3,
		"Battle Fury",
		"Each successful attack grants 5 Fury (up to 100). Every Fury grants +0.3% critical strike chance."
	);

	private static final SkillDefinition MOCKING_SHOUT = activeUpgrade(
		CLASS,
		"mocking_shout", 3,
		"Mocking Shout",
		"Shout at all enemies in range, forcing their attention onto you. They deal 30% less damage "
			+ "and are slowed by 20%. (45 sec cooldown)",
		"battle_cry_weak"
	);

	private static final SkillDefinition SPINNING_SLASH = activeUpgrade(
		CLASS,
		"spinning_slash", 3,
		"Spinning Slash",
		"Dash 8 blocks forward, carving through nearby enemies during the charge for 200% weapon damage. "
			+ "(20 sec cooldown)",
		"piercing_charge"
	);

	private static final SkillDefinition ENHANCED_BERSERKER_RAGE = upgrade(
		CLASS,
		"enhanced_berserker_rage", 4,
		"Enhanced Berserker Rage",
		"Builds on Berserker Rage. Keep up to +50% damage from missing health and also gain up to "
			+ "+30% attack speed at half a heart.",
		"berserker_rage"
	);

	private static final SkillDefinition SAVAGE_ONSLAUGHT = activeUpgrade(
		CLASS,
		"savage_onslaught", 4,
		"Savage Onslaught",
		"Embrace reckless aggression for 10 seconds: take 30% more damage, but gain +30% damage, +20% "
			+ "attack speed, and your strikes bypass 15% armor and damage resistance. (30 sec cooldown)",
		"heavy_strikes"
	);

	private static final SkillDefinition FURY_BECOMES_FLESH = activeUpgrade(
		CLASS,
		"fury_becomes_flesh", 4,
		"Fury Becomes Flesh",
		"Release a close-range blast that damages and knocks back enemies within 2 blocks, then consume all "
			+ "stored Fury to heal 0.5% max health per Fury. (15 sec cooldown)",
		"bludgeon"
	);

	private BerserkerSkills() {}

	// ── API ───────────────────────────────────────────────────────────────────

	/**
	 * The skill set attached to the Berserker evolution:
	 * capstone + all exclusive skills for this path.
	 */
	public static EvolutionSkillSet skillSet() {
		return new EvolutionSkillSet(
			BERSERKER_RAGE,
			List.of(
				BATTLE_FURY,
				MOCKING_SHOUT,
				SPINNING_SLASH,
				ENHANCED_BERSERKER_RAGE,
				SAVAGE_ONSLAUGHT,
				FURY_BECOMES_FLESH
			)
		);
	}

	/**
	 * All Berserker skills — capstone and exclusive — used for registration
	 * in the skill engine at startup.
	 */
	public static List<SkillDefinition> all() {
		return List.of(
			BERSERKER_RAGE,
			BATTLE_FURY,
			MOCKING_SHOUT,
			SPINNING_SLASH,
			ENHANCED_BERSERKER_RAGE,
			SAVAGE_ONSLAUGHT,
			FURY_BECOMES_FLESH
		);
	}
}
