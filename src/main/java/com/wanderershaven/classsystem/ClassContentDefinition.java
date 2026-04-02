package com.wanderershaven.classsystem;

import com.wanderershaven.classsystem.evolution.ClassEvolutionDef;
import com.wanderershaven.skill.SkillDefinition;
import java.util.List;

/**
 * Bundles all content for one root class so registration is centralized.
 */
public record ClassContentDefinition(
	ClassDefinition classDefinition,
	List<SkillDefinition> baseSkills,
	List<ClassEvolutionDef> evolutions
) {
	public ClassContentDefinition {
		baseSkills = List.copyOf(baseSkills);
		evolutions = List.copyOf(evolutions);
	}
}
