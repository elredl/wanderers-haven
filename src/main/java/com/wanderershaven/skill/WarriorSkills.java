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
				"Battle instincts keep you alert. A red warning appears on your screen whenever a hostile mob is within 4 blocks."),

			skill("lesser_endurance", 1,
				"Lesser Endurance",
				"You have learned to endure punishment. Take 10% less damage from all sources."),

			skill("lucky_dodge", 1,
				"Lucky Dodge",
				"Some warriors are simply lucky. Once every 10 minutes, the first projectile that would have hit you passes harmlessly by."),

			skill("second_wind_minor", 1,
				"Second Wind (Minor)",
				"When things look dire, your body finds hidden reserves. Dropping below 40% health instantly recovers 3 hearts. (10 min cooldown)"),

			skill("last_stand_minor", 1,
				"Last Stand (Minor)",
				"Cornered and desperate, you fight harder. Dropping below 40% health surges your combat power, granting 8% attack damage and speed for 10 seconds. (10 min cooldown)"),

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

			upgrade("second_wind_lesser", 2,
				"Second Wind (Lesser)",
				"Your reserves run deeper than before. Dropping below 40% health instantly recovers 4.5 hearts. (8 min cooldown)",
				"second_wind_minor"),

			upgrade("last_stand_lesser", 2,
				"Last Stand (Lesser)",
				"When cornered, you become something to be feared. Dropping below 40% health grants 12% attack damage and speed for 10 seconds. (8 min cooldown)",
				"last_stand_minor"),

			upgrade("lesser_poison_resistance", 2,
				"Lesser Poison Resistance",
				"Toxins barely slow you down anymore. Poison effects last 20% shorter on you.",
				"minor_poison_resistance"),

			upgrade("iron_skin", 2,
				"Iron Skin",
				"Your body has been hardened beyond flesh. Take 12% less damage from all sources, cactus cannot harm you, and arrows deal 10% less damage.",
				"tough_skin"),

			skill("adrenaline", 2,
				"Adrenaline",
				"When blood is drawn and danger is near, your body surges. Move faster when below half health."),

			skill("executioners_eye", 2,
				"Executioner's Eye",
				"You know exactly where to strike a weakened foe. Attacks against enemies below 30% health deal bonus damage."),

			skill("shield_mastery", 2,
				"Shield Mastery",
				"You've trained extensively with a shield. Block cooldown is reduced and blocks absorb more damage."),

			// -----------------------------------------------------------------
			// PW3 -- Combat starts to flow differently.
			// -----------------------------------------------------------------
			skill("battle_cry", 3,
				"Battle Cry",
				"The roar of a true warrior. Landing a kill briefly grants you a burst of speed and strength."),

			skill("iron_will", 3,
				"Iron Will",
				"Your resolve cannot be shaken. You resist knockback significantly -- small hits barely move you."),

			skill("bloodlust", 3,
				"Bloodlust",
				"Every kill fuels your body. Restore a small amount of health with each enemy you slay."),

			// -----------------------------------------------------------------
			// PW4 -- Defining traits that change how enemies experience you.
			// -----------------------------------------------------------------
			skill("berserker", 4,
				"Berserker",
				"The closer you are to death, the more dangerous you become. Melee damage scales up as your health drops."),

			skill("sweeping_edge", 4,
				"Sweeping Edge",
				"Your strikes carry through. Each melee hit deals partial damage to all enemies within striking distance."),

			skill("fortitude", 4,
				"Fortitude",
				"Your body has been forged through punishment. Gain a large increase to your maximum health pool."),

			// -----------------------------------------------------------------
			// PW5 -- Powerful. Other players will ask how you did that.
			// -----------------------------------------------------------------
			skill("war_god", 5,
				"War God",
				"The spirit of war flows through your veins. Receive a significant boost to damage, speed, and armour simultaneously."),

			skill("death_mark", 5,
				"Death Mark",
				"Any enemy you strike is marked for death. Marked enemies take substantially increased damage from all sources."),

			skill("tenacity", 5,
				"Tenacity",
				"Nothing stops your assault. You are strongly resistant to slowness, weakness, and all debilitating effects."),

			// -----------------------------------------------------------------
			// PW6 -- Strong enough to swing the outcome of any fight.
			// -----------------------------------------------------------------
			skill("rampage", 6,
				"Rampage",
				"Kill streaks no longer just reward -- they empower. Extended streaks grant escalating boosts to damage and speed."),

			skill("warlord", 6,
				"Warlord",
				"Your presence on the battlefield is a force multiplier. Allies within 16 blocks fight noticeably harder in your presence."),

			skill("unstoppable_force", 6,
				"Unstoppable Force",
				"You are a siege engine in human form. You break through enemy armour with ease and shrug off resistance effects."),

			// -----------------------------------------------------------------
			// PW7 -- Near-legendary. Enemies begin to fear your name.
			// -----------------------------------------------------------------
			skill("god_of_war", 7,
				"God of War",
				"You have surpassed the limits of ordinary warriors. Massive boosts to damage and survivability -- you are the battle."),

			skill("champion", 7,
				"Champion",
				"You have proven yourself beyond doubt. Enemies in your vicinity are inflicted with Weakness and Slow as your legend reaches them."),

			skill("titan_strike", 7,
				"Titan Strike",
				"Once per engagement, you may channel everything into a single blow that stuns and deals catastrophic damage to any target."),

			// -----------------------------------------------------------------
			// PW8 -- Legendary. You are a different class of threat.
			// -----------------------------------------------------------------
			skill("godslayer", 8,
				"Godslayer",
				"You have learned what it takes to kill things that should not die. Deal vastly increased damage to boss-tier enemies."),

			skill("eternal_rage", 8,
				"Eternal Rage",
				"Your rage sustains you. While in combat you regenerate health at a rapid rate that shocks those who witness it."),

			skill("avatar_of_war", 8,
				"Avatar of War",
				"You are war made flesh. All combat attributes -- damage, speed, armour, health -- are vastly amplified at once."),

			// -----------------------------------------------------------------
			// PW9 -- World-class power. Your kills ripple outward.
			// -----------------------------------------------------------------
			skill("death_incarnate", 9,
				"Death Incarnate",
				"Death follows in your wake. Each kill has a chance to animate a shadow effigy of the slain that fights for you briefly."),

			skill("world_breaker", 9,
				"World Breaker",
				"Your strikes carry seismic force. Blows send shockwaves that damage and launch all nearby enemies, not just the target."),

			skill("legend", 9,
				"Legend",
				"Your name alone is a weapon. Enemies near you are afflicted with dread -- their attack speed and damage are reduced."),

			// -----------------------------------------------------------------
			// PW10 -- Reality-altering. Reserved for warriors who have reached the summit.
			// -----------------------------------------------------------------
			skill("eternal_warrior", 10,
				"Eternal Warrior",
				"You have transcended mortality itself. In combat, lethal hits are denied once every 30 seconds -- you simply refuse to die."),

			skill("world_ender", 10,
				"World Ender",
				"Reality bends around your strikes. Every blow carries the force of a cataclysm. Nothing stands before you twice."),

			skill("death_god", 10,
				"Death God",
				"You have become the master of death. Each strike carries a chance to instantly end any living thing, regardless of its remaining health.")
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
