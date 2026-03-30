package com.wanderershaven.skill;

/**
 * Describes a single skill that can be awarded to a player.
 *
 * Effects are not modelled here — this record is purely descriptive.
 * Actual gameplay effects (attribute modifiers, potion effects, etc.)
 * will be applied by a separate effect system keyed to the skill ID.
 *
 * @param id           unique identifier used to track ownership, e.g. "warrior_bloodlust"
 * @param classId      class this skill belongs to, e.g. "warrior"
 * @param powerLevel   strength tier 1–10 (1 = minor buff, 10 = world-altering)
 * @param displayName  human-readable name shown in chat and UI
 * @param description  short flavour + mechanical description shown to the player on award
 * @param supersedesId ID of the lesser/minor skill this upgrades, or {@code null} if none.
 *                     When this skill is rolled and the player already owns the superseded
 *                     skill, the old skill is removed and replaced by this one.
 */
public record SkillDefinition(
	String id,
	String classId,
	int powerLevel,
	String displayName,
	String description,
	String supersedesId,
	/** True if this skill must be manually activated (placeable in the skill wheel). */
	boolean active
) {}
