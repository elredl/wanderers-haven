package com.wanderershaven.classsystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ClassDefinition {
	private final String id;
	private final String displayName;
	private final double obtainThreshold;
	private final Map<ClassSignalType, Double> signalWeights;
	private final List<ClassPrerequisite> prerequisites;
	private final List<IntentMultiplier> intentMultipliers;

	private ClassDefinition(
		String id,
		String displayName,
		double obtainThreshold,
		Map<ClassSignalType, Double> signalWeights,
		List<ClassPrerequisite> prerequisites,
		List<IntentMultiplier> intentMultipliers
	) {
		this.id = id;
		this.displayName = displayName;
		this.obtainThreshold = obtainThreshold;
		this.signalWeights = Collections.unmodifiableMap(new EnumMap<>(signalWeights));
		this.prerequisites = List.copyOf(prerequisites);
		this.intentMultipliers = List.copyOf(intentMultipliers);
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	public double obtainThreshold() {
		return obtainThreshold;
	}

	public double signalWeight(ClassSignalType signalType) {
		return signalWeights.getOrDefault(signalType, 0.0);
	}

	public List<ClassPrerequisite> prerequisites() {
		return prerequisites;
	}

	public List<IntentMultiplier> intentMultipliers() {
		return intentMultipliers;
	}

	public static Builder builder(String id, String displayName) {
		return new Builder(id, displayName);
	}

	public static final class Builder {
		private final String id;
		private final String displayName;
		private double obtainThreshold = 100.0;
		private final EnumMap<ClassSignalType, Double> signalWeights = new EnumMap<>(ClassSignalType.class);
		private final List<ClassPrerequisite> prerequisites = new ArrayList<>();
		private final List<IntentMultiplier> intentMultipliers = new ArrayList<>();

		private Builder(String id, String displayName) {
			this.id = id;
			this.displayName = displayName;
		}

		public Builder obtainThreshold(double value) {
			this.obtainThreshold = value;
			return this;
		}

		public Builder weight(ClassSignalType signalType, double weight) {
			signalWeights.put(signalType, weight);
			return this;
		}

		public Builder prerequisite(ClassPrerequisite prerequisite) {
			prerequisites.add(prerequisite);
			return this;
		}

		public Builder intentMultiplier(IntentMultiplier intentMultiplier) {
			intentMultipliers.add(intentMultiplier);
			return this;
		}

		public ClassDefinition build() {
			return new ClassDefinition(id, displayName, obtainThreshold, signalWeights, prerequisites, intentMultipliers);
		}
	}
}
