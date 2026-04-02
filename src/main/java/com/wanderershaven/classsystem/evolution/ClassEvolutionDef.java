package com.wanderershaven.classsystem.evolution;

import java.util.List;

/**
 * Defines one evolution path branching from a base class.
 *
 * Evolutions are offered at fixed level milestones (every 25 levels) when a
 * player's playstyle data satisfies all prerequisites. A player selects at most
 * one evolution per milestone — the choice is permanent and shapes their skill
 * pool going forward.
 *
 * Future milestones (level 50, 75, 100) will offer deeper specialisations within
 * a chosen path; those are registered as separate {@code ClassEvolutionDef}s with
 * a {@code baseClassId} pointing at the previous evolution's {@code id}.
 */
public record ClassEvolutionDef(
	/** Unique identifier, e.g. {@code "warrior_berserker"}. */
	String id,
	/** The class this evolution branches from, e.g. {@code "warrior"}. */
	String baseClassId,
	/** Human-readable name shown in the evolution selection GUI. */
	String displayName,
	/** Flavour text and mechanical description shown to the player. */
	String description,
	/** All conditions that must be satisfied to be offered this path. */
	List<EvolutionPrerequisite> prerequisites,
	/**
	 * The capstone skill granted on acceptance and the exclusive skills added to
	 * this evolution's roll pool. Null until skills for this path are defined.
	 */
	EvolutionSkillSet skillSet
) {
	public ClassEvolutionDef {
		prerequisites = List.copyOf(prerequisites);
	}

	/** Convenience constructor for evolutions whose skill set is not yet defined. */
	public ClassEvolutionDef(
		String id,
		String baseClassId,
		String displayName,
		String description,
		List<EvolutionPrerequisite> prerequisites
	) {
		this(id, baseClassId, displayName, description, prerequisites, null);
	}

	/** Returns true if all prerequisites are met in the given context. */
	public boolean isEligible(EvolutionContext context) {
		return prerequisites.stream().allMatch(p -> p.isMet(context));
	}
}
