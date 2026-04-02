package com.wanderershaven.classsystem.evolution;

import java.util.ArrayList;
import java.util.List;

/** Builder helpers for concise evolution definitions. */
public final class EvolutionDefs {
	private EvolutionDefs() {}

	public static Builder evolution(String id, String baseClassId, String displayName, String description) {
		return new Builder(id, baseClassId, displayName, description);
	}

	public static final class Builder {
		private final String id;
		private final String baseClassId;
		private final String displayName;
		private final String description;
		private final List<EvolutionPrerequisite> prerequisites = new ArrayList<>();
		private EvolutionSkillSet skillSet;

		private Builder(String id, String baseClassId, String displayName, String description) {
			this.id = id;
			this.baseClassId = baseClassId;
			this.displayName = displayName;
			this.description = description;
		}

		public Builder requires(EvolutionPrerequisite prerequisite) {
			prerequisites.add(prerequisite);
			return this;
		}

		public Builder requiresAll(List<EvolutionPrerequisite> values) {
			prerequisites.addAll(values);
			return this;
		}

		public Builder skillSet(EvolutionSkillSet value) {
			this.skillSet = value;
			return this;
		}

		public ClassEvolutionDef build() {
			return new ClassEvolutionDef(id, baseClassId, displayName, description, prerequisites, skillSet);
		}
	}
}
