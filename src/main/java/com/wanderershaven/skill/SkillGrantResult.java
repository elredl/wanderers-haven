package com.wanderershaven.skill;

/**
 * Result of a successful skill roll.
 *
 * @param granted      the skill that was awarded
 * @param supersededId the ID of the skill that was replaced (upgraded), or
 *                     {@code null} if this was a fresh grant with no replacement
 */
public record SkillGrantResult(SkillDefinition granted, String supersededId) {

	/** True if this roll replaced an existing lesser/minor skill. */
	public boolean isUpgrade() {
		return supersededId != null;
	}
}
