package com.wanderershaven.skill;

/** Shared factory helpers for common skill-definition patterns. */
public final class SkillDefs {
	private SkillDefs() {}

	public static SkillDefinition passive(String classId, String id, int powerLevel, String displayName, String description) {
		return new SkillDefinition(id, classId, powerLevel, displayName, description, null, false);
	}

	public static SkillDefinition active(String classId, String id, int powerLevel, String displayName, String description) {
		return new SkillDefinition(id, classId, powerLevel, displayName, description, null, true);
	}

	public static SkillDefinition upgrade(String classId, String id, int powerLevel, String displayName, String description, String supersedesId) {
		return new SkillDefinition(id, classId, powerLevel, displayName, description, supersedesId, false);
	}

	public static SkillDefinition activeUpgrade(String classId, String id, int powerLevel, String displayName, String description, String supersedesId) {
		return new SkillDefinition(id, classId, powerLevel, displayName, description, supersedesId, true);
	}
}
