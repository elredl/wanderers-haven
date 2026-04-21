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
 *   Mocking Shout -> Terrorizing Howl
 *   Piercing Charge -> Spinning Slash
 *   Spinning Slash -> Raging Cyclone
 *   Berserker Rage -> Enhanced Berserker Rage
 *   Heavy Strikes -> Savage Onslaught
 *   Savage Onslaught -> Massacre
 *   Bludgeon -> Fury Becomes Flesh
 *   Fury Becomes Flesh -> Fury, Now Blessing
 *   Battle Fury -> Rage Engine / Overclock / Greater Battle Fury
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

	private static final SkillDefinition RAGE_ENGINE = upgrade(
		CLASS,
		"rage_engine", 5,
		"Rage Engine",
		"Builds on Battle Fury. While under 50% health, your Fury generation is increased by 50%.",
		"battle_fury"
	);

	private static final SkillDefinition OVERCLOCK = upgrade(
		CLASS,
		"overclock", 5,
		"Overclock",
		"Builds on Battle Fury. Gain +30% attack speed while at 100 Fury, and keep it for 5 seconds after consuming Fury.",
		"battle_fury"
	);

	private static final SkillDefinition LAST_BREATH = passive(
		CLASS,
		"last_breath", 5,
		"Last Breath",
		"While below 15% health, gain 40% damage reduction."
	);

	private static final SkillDefinition TERRORIZING_HOWL = activeUpgrade(
		CLASS,
		"terrorizing_howl", 5,
		"Terrorizing Howl",
		"All nearby enemies are feared for 3 seconds (slowed by 60% and fleeing from you), then become taunted, "
			+ "deal 30% less damage, take 20% more damage, and remain slowed by 20%. (45 sec cooldown)",
		"mocking_shout"
	);

	private static final SkillDefinition RAGING_CYCLONE = activeUpgrade(
		CLASS,
		"raging_cyclone", 5,
		"Raging Cyclone",
		"Dash 13 blocks in a violent arc, dealing 250% weapon damage to enemies in your path and gaining 5 Fury per enemy hit. "
			+ "(20 sec cooldown)",
		"spinning_slash"
	);

	private static final SkillDefinition GREATER_BATTLE_FURY = upgrade(
		CLASS,
		"greater_battle_fury", 5,
		"Greater Battle Fury",
		"Builds on Battle Fury. Increases Fury storage to 150. Fury-consuming abilities use up to 100 Fury per activation.",
		"battle_fury"
	);

	private static final SkillDefinition ENDLESS_RAGE = passive(
		CLASS,
		"endless_rage", 6,
		"Endless Rage",
		"Your Fury no longer decays while out of combat."
	);

	private static final SkillDefinition MASSACRE = activeUpgrade(
		CLASS,
		"massacre", 6,
		"Massacre",
		"Upgrade of Savage Onslaught. For 10 seconds: +30% damage, +20% attack speed, 25% armor/DR bypass, "
			+ "10% lifesteal, but you take 30% more damage. (30 sec cooldown)",
		"savage_onslaught"
	);

	private static final SkillDefinition PAIN_FUELS_ME = passive(
		CLASS,
		"pain_fuels_me", 6,
		"Pain Fuels Me",
		"Gain Fury equal to 15% of damage received."
	);

	private static final SkillDefinition RAGE_RESERVES = passive(
		CLASS,
		"rage_reserves", 6,
		"Rage Reserves",
		"Gain +100 maximum Fury."
	);

	private static final SkillDefinition FURY_NOW_BLESSING = activeUpgrade(
		CLASS,
		"fury_now_blessing", 6,
		"Fury, Now Blessing",
		"Upgrade of Fury Becomes Flesh. Heal 50% more from consumed Fury. Also gain +0.25% critical hit chance and "
			+ "+0.5% critical hit damage per Fury consumed for 10 seconds. (15 sec cooldown)",
		"fury_becomes_flesh"
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
				FURY_BECOMES_FLESH,
				RAGE_ENGINE,
				OVERCLOCK,
				LAST_BREATH,
				TERRORIZING_HOWL,
				RAGING_CYCLONE,
				GREATER_BATTLE_FURY,
				ENDLESS_RAGE,
				MASSACRE,
				PAIN_FUELS_ME,
				RAGE_RESERVES,
				FURY_NOW_BLESSING
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
			FURY_BECOMES_FLESH,
			RAGE_ENGINE,
			OVERCLOCK,
			LAST_BREATH,
			TERRORIZING_HOWL,
			RAGING_CYCLONE,
			GREATER_BATTLE_FURY,
			ENDLESS_RAGE,
			MASSACRE,
			PAIN_FUELS_ME,
			RAGE_RESERVES,
			FURY_NOW_BLESSING
		);
	}
}
