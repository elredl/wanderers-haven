package com.wanderershaven.skill;

import java.util.List;

/**
 * Warrior base-class skills that currently have concrete gameplay implementations.
 */
public final class WarriorSkills {
	private static final String CLASS = "warrior";

	private WarriorSkills() {}

	public static List<SkillDefinition> all() {
		return List.of(
			// PW1 -- Implemented baseline passives + actives.
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

			// PW2 -- Implemented upgrades.
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

			skill("fortitude", 3,
				"Fortitude",
				"Gain 3 permanent hearts."),

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

			upgrade("enhanced_resistance_piercing", 4,
				"Enhanced Resistance: Piercing",
				"Builds on Lesser Resistance: Piercing. Take 18% less damage from piercing weapons.",
				"lesser_resistance_piercing"),

			upgrade("critical_rhythm", 4,
				"Critical Rhythm",
				"Builds on Critical Hits. Start at 10% crit chance, then gain +2% per successful hit up to 20%. Crits deal 150% damage.",
				"critical_hits"),

			// -----------------------------------------------------------------
			// PW5 -- Heavy hitters and high-end survivability.
			// -----------------------------------------------------------------
			upgrade("greater_critical_rhythm", 5,
				"Greater Critical Rhythm",
				"Builds on Critical Rhythm. Crit chance starts at 10% and gains +1% per successful hit, up to 30%. Crits deal 150% damage.",
				"critical_rhythm"),

			skill("killing_blow", 5,
				"Killing Blow",
				"Deal 30% more damage to enemies under 20% health."),

			upgrade("greater_fortitude", 5,
				"Greater Fortitude",
				"Builds on Fortitude. Gain 6 permanent hearts.",
				"fortitude"),

			skill("avoid_death", 5,
				"Avoid Death",
				"The first time a killing blow would land in battle, survive and recover 20% health. (5 min cooldown)"),

			upgrade("greater_first_strike", 5,
				"Greater First Strike",
				"Builds on First Strike. Your opening hit against an enemy deals 45% more damage.",
				"first_strike"),

			upgrade("wound_regeneration", 5,
				"Wound Regeneration",
				"Builds on Wound Closure. Regenerate health faster while in combat.",
				"wound_closure"),

			upgrade("diamond_skin", 5,
				"Diamond Skin",
				"Builds on Iron Skin. Take 20% less damage from all sources and gain a 10% chance to reflect 20% of incoming damage.",
				"iron_skin"),

			skill("battle_hardened", 5,
				"Battle-Hardened",
				"Gain 5% multiplicative power to your core combat stats."),

			skill("intimidating_aura", 5,
				"Intimidating Aura",
				"Enemies within 10 blocks move 10% slower, deal 10% less damage, and take 8% more damage."),

			upgrade("precise_strikes", 5,
				"Precise Strikes",
				"Builds on Measured Strikes. Attacks have a 15% chance to bypass 30% armor and 15% damage reduction from skills.",
				"measured_strikes"),

			// -----------------------------------------------------------------
			// PW6 -- Elite upgrades and hard counters.
			// -----------------------------------------------------------------
			upgrade("body_of_galas", 6,
				"Body of Galas",
				"Builds on Battle-Hardened. Gain 10% multiplicative power to your core combat stats.",
				"battle_hardened"),

			upgrade("immense_strength", 6,
				"Immense Strength",
				"Builds on Greater Strength. Melee attacks deal 50% more damage.",
				"greater_strength"),

			upgrade("immense_dexterity", 6,
				"Immense Dexterity",
				"Builds on Greater Dexterity. Gain 40% attack speed.",
				"greater_dexterity"),

			upgrade("immense_speed", 6,
				"Immense Speed",
				"Builds on Greater Speed. Gain 40% movement speed.",
				"greater_speed"),

			upgrade("immense_endurance", 6,
				"Immense Endurance",
				"Builds on Greater Endurance. Take 43% less damage from all sources.",
				"greater_endurance"),

			upgrade("greater_resistance_blades", 6,
				"Greater Resistance: Blades",
				"Builds on Enhanced Resistance: Blades. Take 28% less damage from slashing weapons.",
				"enhanced_resistance_blades"),

			upgrade("greater_resistance_bludgeoning", 6,
				"Greater Resistance: Bludgeoning",
				"Builds on Enhanced Resistance: Bludgeoning. Take 28% less damage from bludgeoning weapons and unarmed attacks.",
				"enhanced_resistance_bludgeoning"),

			upgrade("greater_resistance_piercing", 6,
				"Greater Resistance: Piercing",
				"Builds on Enhanced Resistance: Piercing. Take 28% less damage from piercing weapons.",
				"enhanced_resistance_piercing"),

			upgrade("greater_resistance_magic", 6,
				"Greater Resistance: Magic",
				"Builds on Enhanced Resistance: Magic. Weakness, Nausea, and Slowness effects last 28% shorter on you.",
				"enhanced_resistance_magic"),

			upgrade("greater_resistance_elements", 6,
				"Greater Resistance: Elements",
				"Builds on Lesser Resistance: Elements. Take 20% less damage from elemental sources.",
				"lesser_resistance_elements"),

			upgrade("poison_immunity", 6,
				"Poison Immunity",
				"Builds on Enhanced Poison Resistance. Poison no longer damages you.",
				"enhanced_poison_resistance"),

			skill("indomitable", 6,
				"Indomitable",
				"Slows and stuns are reduced by 50% on you."),

			upgrade("perfect_constitution", 6,
				"Perfect Constitution",
				"Builds on Greater Fortitude. Gain 10 permanent hearts.",
				"greater_fortitude"),

			skill("fleetfooted", 6,
				"Fleetfooted",
				"Builds on Lightfooted. Gain 20% movement speed and stronger jump power while in combat."),

			// -----------------------------------------------------------------
			// PW7 -- Mythic tier upgrades.
			// -----------------------------------------------------------------
			upgrade("titanic_resistance_blades", 7,
				"Titanic Resistance: Blades",
				"Builds on Greater Resistance: Blades. Take 36% less damage from slashing weapons.",
				"greater_resistance_blades"),

			upgrade("titanic_resistance_bludgeoning", 7,
				"Titanic Resistance: Bludgeoning",
				"Builds on Greater Resistance: Bludgeoning. Take 36% less damage from bludgeoning weapons and unarmed attacks.",
				"greater_resistance_bludgeoning"),

			upgrade("titanic_resistance_piercing", 7,
				"Titanic Resistance: Piercing",
				"Builds on Greater Resistance: Piercing. Take 36% less damage from piercing weapons.",
				"greater_resistance_piercing"),

			upgrade("titanic_resistance_magic", 7,
				"Titanic Resistance: Magic",
				"Builds on Greater Resistance: Magic. Weakness, Nausea, and Slowness effects last 36% shorter on you.",
				"greater_resistance_magic"),

			upgrade("titanic_resistance_elements", 7,
				"Titanic Resistance: Elements",
				"Builds on Greater Resistance: Elements. Take 28% less damage from elemental sources.",
				"greater_resistance_elements"),

			upgrade("giants_constitution", 7,
				"Giant's Constitution",
				"Builds on Perfect Constitution. Gain 20 permanent hearts.",
				"perfect_constitution"),

			upgrade("perception_nothing_slips_my_grasp", 7,
				"Perception: Nothing Slips My Grasp",
				"Builds on Greater Dangersense. Keep all prior effects and reduce damage from attacks behind you by 50%.",
				"greater_dangersense"),

			upgrade("skin_of_adamantium", 7,
				"Skin of Adamantium",
				"Builds on Diamond Skin. Take 25% less damage from all sources and 40% less magic damage (currently potion harming and enchant-style sources), while still reflecting damage.",
				"diamond_skin"),

			upgrade("battlefield_unmatched_mobility", 7,
				"Battlefield: Unmatched Mobility",
				"Builds on Fleetfooted. Gain +10% additional speed in combat, 2x jump boost, and each hit grants +15% movement speed for 4 seconds.",
				"fleetfooted"),

			// -----------------------------------------------------------------
			// PW8 -- Apex physical mastery.
			// -----------------------------------------------------------------
			upgrade("legendary_strength", 8,
				"Legendary Strength",
				"Builds on Immense Strength. Melee attacks deal 62% more damage.",
				"immense_strength"),

			upgrade("legendary_dexterity", 8,
				"Legendary Dexterity",
				"Builds on Immense Dexterity. Gain 50% attack speed.",
				"immense_dexterity"),

			upgrade("legendary_speed", 8,
				"Legendary Speed",
				"Builds on Immense Speed. Gain 50% movement speed.",
				"immense_speed"),

			upgrade("legendary_endurance", 8,
				"Legendary Endurance",
				"Builds on Immense Endurance. Take 53% less damage from all sources.",
				"immense_endurance"),

			upgrade("transcendent_form", 8,
				"Transcendent Form",
				"Builds on Body of Galas. Gain 15% multiplicative power to all core combat stats and +20% max health.",
				"body_of_galas"),

			upgrade("perfect_appraisal", 8,
				"Perfect Appraisal",
				"Builds on Lesser Appraisal. See health, class info, and level of any enemy regardless of level, and deal 30% more damage to enemies at least 3 levels below you.",
				"lesser_appraisal"),

			upgrade("the_first_blow_decides_it", 8,
				"The First Blow Decides It",
				"Builds on Greater First Strike. Your first hit against an enemy deals 300% damage.",
				"greater_first_strike"),

			upgrade("my_body_fears_no_wounds", 8,
				"My Body Fears No Wounds",
				"Builds on Wound Regeneration. Regenerate much faster while in combat.",
				"wound_regeneration"),

			upgrade("my_strikes_stop_for_nothing", 8,
				"My Strikes Stop for Nothing",
				"Builds on Precise Strikes. Attacks have a 15% chance to bypass 50% armor and 50% damage reduction from skills.",
				"precise_strikes"),

			// -----------------------------------------------------------------
			// PW9 -- Divinity-tier combat mastery.
			// -----------------------------------------------------------------
			upgrade("mythic_strength", 9,
				"Mythic Strength",
				"Builds on Legendary Strength. Melee attacks deal 77% more damage.",
				"legendary_strength"),

			upgrade("mythic_dexterity", 9,
				"Mythic Dexterity",
				"Builds on Legendary Dexterity. Gain 65% attack speed.",
				"legendary_dexterity"),

			upgrade("mythic_speed", 9,
				"Mythic Speed",
				"Builds on Legendary Speed. Gain 65% movement speed.",
				"legendary_speed"),

			upgrade("mythic_endurance", 9,
				"Mythic Endurance",
				"Builds on Legendary Endurance. Take 68% less damage from all sources.",
				"legendary_endurance"),

			upgrade("divine_resistance_blades", 9,
				"Divine Resistance: Blades",
				"Builds on Titanic Resistance: Blades. Take 51% less damage from slashing weapons.",
				"titanic_resistance_blades"),

			upgrade("divine_resistance_bludgeoning", 9,
				"Divine Resistance: Bludgeoning",
				"Builds on Titanic Resistance: Bludgeoning. Take 51% less damage from bludgeoning weapons and unarmed attacks.",
				"titanic_resistance_bludgeoning"),

			upgrade("divine_resistance_piercing", 9,
				"Divine Resistance: Piercing",
				"Builds on Titanic Resistance: Piercing. Take 51% less damage from piercing weapons.",
				"titanic_resistance_piercing"),

			upgrade("divine_resistance_magic", 9,
				"Divine Resistance: Magic",
				"Builds on Titanic Resistance: Magic. Weakness, Nausea, and Slowness effects last 51% shorter on you.",
				"titanic_resistance_magic"),

			upgrade("divine_resistance_elements", 9,
				"Divine Resistance: Elements",
				"Builds on Titanic Resistance: Elements. Take 43% less damage from elemental sources.",
				"titanic_resistance_elements"),

			upgrade("the_universe_it_sings_for_me", 9,
				"The Universe, It Sings for Me",
				"Builds on Greater Critical Rhythm. Gain 5% crit chance per hit up to 80%. Critical hits deal 250% damage.",
				"greater_critical_rhythm"),

			upgrade("divinity_incarnate", 9,
				"Divinity Incarnate",
				"Builds on Giant's Constitution. Gain 40 permanent hearts.",
				"giants_constitution"),

			upgrade("my_will_imposed", 9,
				"My Will, Imposed",
				"Builds on Intimidating Aura. Non-boss mobs at least 10 levels below you are unable to move or attack within your aura.",
				"intimidating_aura"),

			upgrade("defy_the_final_call", 9,
				"Defy the Final Call",
				"Builds on Avoid Death. Once per combat, a killing blow instead restores full health and unleashes a shockwave that knocks back nearby enemies and damages them based on health restored.",
				"avoid_death")
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
