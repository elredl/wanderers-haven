package com.wanderershaven.classsystem.evolution;

import com.wanderershaven.skill.SkillDefinition;
import java.util.List;

/**
 * The skill set attached to one evolution path.
 *
 * <p><b>capstoneSkill</b> — Granted immediately when a player accepts this evolution.
 * It replaces the guaranteed skill roll that would otherwise fire at the evolution
 * milestone (e.g. level 25). Null until the capstone for this path is designed.
 *
 * <p><b>exclusiveSkills</b> — Skills that enter the roll pool exclusively for players
 * who have accepted this evolution. They are rolled through the normal level-up
 * mechanism going forward, and do not appear in any other evolution's pool.
 * All exclusive skills must use the <em>base</em> class ID (e.g. {@code "warrior"})
 * so they show up correctly in the player's skill list and UI.
 * Empty until evolution-specific skills are designed.
 */
public record EvolutionSkillSet(
	SkillDefinition capstoneSkill,
	List<SkillDefinition> exclusiveSkills
) {
	public EvolutionSkillSet {
		exclusiveSkills = List.copyOf(exclusiveSkills);
	}

	/** Placeholder: capstone and exclusive skills not yet defined. */
	public static EvolutionSkillSet empty() {
		return new EvolutionSkillSet(null, List.of());
	}
}
