package com.wanderershaven.classsystem.evolution;

import com.wanderershaven.skill.BerserkerSkills;
import com.wanderershaven.skill.BlademasterSkills;
import com.wanderershaven.skill.DuelistSkills;
import com.wanderershaven.skill.PaladinSkills;
import com.wanderershaven.skill.VanguardSkills;
import java.util.List;

/**
 * Defines all evolution paths for the Warrior class.
 *
 * Each path is offered at level 25 when the corresponding playstyle
 * prerequisites are met. A player who qualifies for multiple paths will
 * be shown all of them and must pick one.
 *
 * Weapon-gated paths (Duelist, Bladedancer, Blademaster, Spearmaster,
 * Headhunter, Mauler, Executioner) that require Simply Swords weapons are
 * naturally self-gating — if SS is not installed, no kills ever accumulate
 * against those categories, so they are never offered.
 */
public final class WarriorEvolutions {

	private static final String BASE = "warrior";

	private WarriorEvolutions() {}

	public static List<ClassEvolutionDef> all() {
		return List.of(
			berserker(), paladin(), vanguard(),
			duelist(), bladeDancer(), blademaster(),
			spearmaster(), headhunter(), mauler(), executioner()
		);
	}

	// ── Playstyle paths ───────────────────────────────────────────────────────

	/**
	 * Berserker — thrives at low health, near-death damage scaling.
	 */
	private static ClassEvolutionDef berserker() {
		return new ClassEvolutionDef(
			"warrior_berserker",
			BASE,
			"Berserker",
			"Your rage ignites when blood is drawn. You have proven that you do not flinch at death's edge — "
				+ "now your fury rewards it. The lower your health, the greater your power.",
			List.of(
				EvolutionPrerequisite.classLevel(25),
				EvolutionPrerequisite.nearDeathSurvivals(20)
			),
			BerserkerSkills.skillSet()
		);
	}

	/**
	 * Paladin — holy warrior, specialises in purging the undead.
	 */
	private static ClassEvolutionDef paladin() {
		return new ClassEvolutionDef(
			"warrior_paladin",
			BASE,
			"Paladin",
			"You have walked through darkness and emerged with conviction. Countless undead have fallen to your blade — "
				+ "a higher calling awaits. Become a holy warrior, blessed against the forces of undeath.",
			List.of(
				EvolutionPrerequisite.classLevel(25),
				EvolutionPrerequisite.killsOfCategory("undead", 200)
			),
			PaladinSkills.skillSet()
		);
	}

	/**
	 * Vanguard — shield specialist, turns defence into dominance.
	 */
	private static ClassEvolutionDef vanguard() {
		return new ClassEvolutionDef(
			"warrior_vanguard",
			BASE,
			"Vanguard",
			"Two hundred times you raised your shield and two hundred times you walked away. "
				+ "Your defence is not passive — it is a wall that advances. Become a Vanguard and lead from the front.",
			List.of(
				EvolutionPrerequisite.classLevel(25),
				EvolutionPrerequisite.blockedHits(200)
			),
			VanguardSkills.skillSet()
		);
	}

	// ── Weapon-gated paths ────────────────────────────────────────────────────

	/**
	 * Duelist — precision and speed with light single-handed blades.
	 * Weapons: Sword, Rapier, Sai, Katana, Cutlass.
	 */
	private static ClassEvolutionDef duelist() {
		return new ClassEvolutionDef(
			"warrior_duelist",
			BASE,
			"Duelist",
			"You have never needed brute force. A precise strike, a fast draw, a clean finish — "
				+ "that has always been enough. Become a Duelist and turn speed and precision into an art form.",
			List.of(
				EvolutionPrerequisite.classLevel(25),
				EvolutionPrerequisite.killsWithWeapon("duelist", 200)
			),
			DuelistSkills.skillSet()
		);
	}

	/**
	 * Blade Dancer — flowing, unpredictable combat with thrown and twin weapons.
	 * Weapons: Chakram, Twinblades. Requires Simply Swords.
	 */
	private static ClassEvolutionDef bladeDancer() {
		return new ClassEvolutionDef(
			"warrior_blade_dancer",
			BASE,
			"Blade Dancer",
			"Where others plant their feet and swing, you move. The battlefield is a stage and you never "
				+ "stop moving across it. Become a Blade Dancer and let your weapons find their own way home.",
			List.of(
				EvolutionPrerequisite.classLevel(25),
				EvolutionPrerequisite.killsWithWeapon("bladedancer", 200)
			)
		);
	}

	/**
	 * Blademaster — power and control with heavy two-handed blades.
	 * Weapons: Claymore, Longsword, Greatsword. Requires Simply Swords.
	 */
	private static ClassEvolutionDef blademaster() {
		return new ClassEvolutionDef(
			"warrior_blademaster",
			BASE,
			"Blademaster",
			"You do not fight with a sword — you command it. Every swing is deliberate, every cut inevitable. "
				+ "Become a Blademaster and let the weight of your blade speak for itself.",
			List.of(
				EvolutionPrerequisite.classLevel(25),
				EvolutionPrerequisite.killsWithWeapon("blademaster", 200)
			),
			BlademasterSkills.skillSet()
		);
	}

	/**
	 * Spearmaster — reach, control, and the advance.
	 * Weapons: Spear, Glaive, Warglaive, Halberd. Requires Simply Swords.
	 */
	private static ClassEvolutionDef spearmaster() {
		return new ClassEvolutionDef(
			"warrior_spearmaster",
			BASE,
			"Spearmaster",
			"You have kept your enemies at arm's length — and then some. The spear is not just a weapon; "
				+ "it is a statement of intent. Become a Spearmaster and turn reach itself into a weapon.",
			List.of(
				EvolutionPrerequisite.classLevel(25),
				EvolutionPrerequisite.killsWithWeapon("spear", 200)
			)
		);
	}

	/**
	 * Headhunter — fast, relentless, built around the axe.
	 * Weapons: Axe (regular only; greataxe is Mauler territory).
	 */
	private static ClassEvolutionDef headhunter() {
		return new ClassEvolutionDef(
			"warrior_headhunter",
			BASE,
			"Headhunter",
			"You do not take prisoners and you do not miss. Two hundred clean kills with an axe — "
				+ "not cleaved, not bludgeoned, finished. Become a Headhunter and make every swing count.",
			List.of(
				EvolutionPrerequisite.classLevel(25),
				EvolutionPrerequisite.killsWithWeapon("axe", 200)
			)
		);
	}

	/**
	 * Mauler — overwhelming force with heavy blunt weapons and great axes.
	 * Weapons: Greathammer, Greataxe. Requires Simply Swords.
	 */
	private static ClassEvolutionDef mauler() {
		return new ClassEvolutionDef(
			"warrior_mauler",
			BASE,
			"Mauler",
			"Armour is not a problem — it is a challenge you have already solved two hundred times. "
				+ "Nothing stands up twice to a weapon like yours. Become a Mauler and hit harder than anything else on the field.",
			List.of(
				EvolutionPrerequisite.classLevel(25),
				EvolutionPrerequisite.killsWithWeapon("mauler", 200)
			)
		);
	}

	/**
	 * Executioner — sweeping death with a scythe.
	 * Weapons: Scythe. Requires Simply Swords.
	 */
	private static ClassEvolutionDef executioner() {
		return new ClassEvolutionDef(
			"warrior_executioner",
			BASE,
			"Executioner",
			"The scythe was never meant for harvesting grain alone. You have reaped hundreds with its arc — "
				+ "enemies falling in sweeps, not strikes. Become an Executioner and make death itself efficient.",
			List.of(
				EvolutionPrerequisite.classLevel(25),
				EvolutionPrerequisite.killsWithWeapon("scythe", 200)
			)
		);
	}
}
