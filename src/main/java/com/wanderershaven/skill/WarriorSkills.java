package com.wanderershaven.skill;

import java.util.List;

/**
 * All skill definitions for the Warrior class, organised by power level.
 *
 * Power level flavour guide:
 *   PW 1-2  : minor passive buffs -- noticeable but subtle
 *   PW 3-4  : meaningful combat improvements -- changes how fights feel
 *   PW 5-6  : strong, class-defining abilities -- clearly impactful
 *   PW 7-8  : near-legendary power -- other players notice
 *   PW 9-10 : world-class / reality-altering -- the stuff of legends
 */
public final class WarriorSkills {
	private static final String CLASS = "warrior";

	private WarriorSkills() {}

	public static List<SkillDefinition> all() {
		return List.of(
			// -----------------------------------------------------------------
			// PW1 -- Minor passives. A new warrior's first edge.
			// -----------------------------------------------------------------
			skill("lesser_strength", 1,
				"Lesser Strength",
				"Your body grows accustomed to the weight of a weapon. Melee attacks deal 10% more damage."),

			skill("tough_skin", 1,
				"Tough Skin",
				"Calluses and scars harden your body. Take 5% less damage from all sources, and cactus can no longer harm you."),

			skill("lesser_dexterity", 1,
				"Lesser Dexterity",
				"Your reflexes sharpen with every fight. Gain 10% attack speed."),

			skill("lesser_speed", 1,
				"Lesser Speed",
				"Constant training has made you lighter on your feet. Gain 10% movement speed."),

			skill("dangersense", 1,
				"Dangersense",
				"A red warning appears on your screen whenever a hostile mob is within 4 blocks."),

			skill("lesser_endurance", 1,
				"Lesser Endurance",
				"You have learned to endure punishment. Take 10% less damage from all sources."),

			skill("lucky_dodge", 1,
				"Lucky Dodge",
				"Some warriors are simply lucky. Once every 10 minutes, the first projectile that would have hit you passes harmlessly by."),

			skill("minor_poison_resistance", 1,
				"Minor Poison Resistance",
				"Your body has built up a tolerance to toxins. Poison effects last 10% shorter on you."),

			skill("slow_metabolism", 1,
				"Slow Metabolism",
				"Years of campaign rations have trained your body to make do with less. Hunger depletes 10% slower."),

			activeSkill("heavy_strikes", 1,
				"Heavy Strikes",
				"You channel your strength into every blow. Activate to deal 12% more damage for 10 seconds. (30 sec cooldown)"),

			activeSkill("battle_cry_weak", 1,
				"Battle Cry (Weak)",
				"Your war cry rattles the nerves of nearby enemies. Activate to weaken all hostile mobs and players within 8 blocks: they deal 8% less damage and take 8% more damage for 8 seconds. (45 sec cooldown)"),

			activeSkill("bludgeon", 1,
				"Bludgeon",
				"A brutal strike that catches enemies off-guard. Activate to deal weapon damage to all enemies in a cone in front of you, reducing their armor by 15% for 5 seconds. (15 sec cooldown)"),

			activeSkill("piercing_charge", 1,
				"Piercing Charge",
				"You hurl yourself forward like a battering ram. Activate to dash 5 blocks in the direction you are facing, dealing 130% weapon damage to all enemies in your path. (20 sec cooldown)"),

			// -----------------------------------------------------------------
			// PW2 -- Small improvements that start shaping a playstyle.
			// -----------------------------------------------------------------
			upgrade("enhanced_strength", 2,
				"Enhanced Strength",
				"Your muscles have hardened beyond what most warriors achieve. Melee attacks deal 20% more damage.",
				"lesser_strength"),

			upgrade("enhanced_dexterity", 2,
				"Enhanced Dexterity",
				"Your hands move faster than eyes can follow. Gain 18% attack speed.",
				"lesser_dexterity"),

			upgrade("enhanced_speed", 2,
				"Enhanced Speed",
				"The battlefield blurs around you. Gain 18% movement speed.",
				"lesser_speed"),

			upgrade("enhanced_endurance", 2,
				"Enhanced Endurance",
				"Your body has been tempered through relentless punishment. Take 18% less damage from all sources.",
				"lesser_endurance"),

			upgrade("lesser_poison_resistance", 2,
				"Lesser Poison Resistance",
				"Toxins barely slow you down anymore. Poison effects last 20% shorter on you.",
				"minor_poison_resistance"),

			upgrade("iron_skin", 2,
				"Iron Skin",
				"Your body has been hardened beyond flesh. Take 12% less damage from all sources, cactus cannot harm you, and arrows deal 10% less damage.",
				"tough_skin"),

			// -----------------------------------------------------------------
			// PW3 -- Meaningful combat improvements.
			// -----------------------------------------------------------------
			skill("lightfooted", 3,
				"Lightfooted",
				"Gain 15% movement speed and 50% jump boost while in combat."),

			skill("wound_closure", 3,
				"Wound Closure",
				"Passively regenerate health while in combat."),

			skill("first_strike", 3,
				"First Strike",
				"The first attack against an enemy deals 30% more damage."),

			upgrade("greater_dangersense", 3,
				"Greater Dangersense",
				"Keeps Dangersense: a red warning appears when a hostile mob is within 4 blocks. Also take 20% less damage from attacks from behind you.",
				"dangersense"),

			skill("measured_strikes", 3,
				"Measured Strikes",
				"Attacks have a 10% chance to ignore 20% enemy armor."),

			skill("critical_hits", 3,
				"Critical Hits",
				"Gain 10% chance to critically hit for 150% damage."),

			upgrade("enhanced_poison_resistance", 3,
				"Enhanced Poison Resistance",
				"Poison effects last 20% shorter on you.",
				"lesser_poison_resistance"),

			skill("lesser_resistance_fire", 3,
				"Lesser Resistance: Fire",
				"Fire effect lasts 15% less."),

			skill("lesser_resistance_blades", 3,
				"Lesser Resistance: Blades",
				"Take 10% less damage from slashing weapons."),

			skill("lesser_resistance_bludgeoning", 3,
				"Lesser Resistance: Bludgeoning",
				"Take 10% less damage from bludgeoning weapons and unarmed attacks."),

			skill("lesser_resistance_piercing", 3,
				"Lesser Resistance: Piercing",
				"Take 10% less damage from piercing weapons (spears, arrows, and similar attacks)."),

			skill("lesser_resistance_magic", 3,
				"Lesser Resistance: Magic",
				"Weakness, Nausea, and Slowness effects last 10% shorter on you."),

			// -----------------------------------------------------------------
			// PW4 -- Strong upgrades that lock in a build.
			// -----------------------------------------------------------------
			upgrade("greater_strength", 4,
				"Greater Strength",
				"Builds on Enhanced Strength. Melee attacks deal 35% more damage.",
				"enhanced_strength"),

			upgrade("greater_dexterity", 4,
				"Greater Dexterity",
				"Builds on Enhanced Dexterity. Gain 28% attack speed.",
				"enhanced_dexterity"),

			upgrade("greater_speed", 4,
				"Greater Speed",
				"Builds on Enhanced Speed. Gain 28% movement speed.",
				"enhanced_speed"),

			upgrade("greater_endurance", 4,
				"Greater Endurance",
				"Builds on Enhanced Endurance. Take 28% less damage from all sources.",
				"enhanced_endurance"),

			upgrade("lesser_resistance_elements", 4,
				"Lesser Resistance: Elements",
				"Builds on Lesser Resistance: Fire. Take 10% less damage from elemental sources.",
				"lesser_resistance_fire"),

			upgrade("enhanced_resistance_magic", 4,
				"Enhanced Resistance: Magic",
				"Builds on Lesser Resistance: Magic. Weakness, Nausea, and Slowness effects last 18% shorter on you.",
				"lesser_resistance_magic"),

			skill("lesser_appraisal", 4,
				"Lesser Appraisal",
				"You can see the HP, level, and name of mobs/players up to 5 levels above you."),

			upgrade("enhanced_resistance_blades", 4,
				"Enhanced Resistance: Blades",
				"Builds on Lesser Resistance: Blades. Take 18% less damage from slashing weapons.",
				"lesser_resistance_blades"),

			upgrade("enhanced_resistance_bludgeoning", 4,
				"Enhanced Resistance: Bludgeoning",
				"Builds on Lesser Resistance: Bludgeoning. Take 18% less damage from bludgeoning weapons and unarmed attacks.",
				"lesser_resistance_bludgeoning"),

			upgrade("critical_rhythm", 4,
				"Critical Rhythm",
				"Builds on Critical Hits. Start at 10% crit chance, then gain +2% per successful hit up to 20%. Crits deal 150% damage.",
				"critical_hits")
		);
	}

	private static SkillDefinition skill(String id, int powerLevel, String displayName, String description) {
		return SkillDefs.passive(CLASS, id, powerLevel, displayName, description);
	}

	private static SkillDefinition activeSkill(String id, int powerLevel, String displayName, String description) {
		return SkillDefs.active(CLASS, id, powerLevel, displayName, description);
	}

	private static SkillDefinition upgrade(String id, int powerLevel, String displayName, String description, String supersedesId) {
		return SkillDefs.upgrade(CLASS, id, powerLevel, displayName, description, supersedesId);
	}
}
