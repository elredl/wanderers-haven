package com.wanderershaven.classsystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PlayerClassProfile {
	private final Map<String, Double> affinities;
	private final Set<String> obtainedClasses;
	private final long lastUpdatedTime;

	public PlayerClassProfile(Map<String, Double> affinities, Set<String> obtainedClasses, long lastUpdatedTime) {
		this.affinities = new HashMap<>(affinities);
		this.obtainedClasses = new HashSet<>(obtainedClasses);
		this.lastUpdatedTime = lastUpdatedTime;
	}

	public Map<String, Double> affinities() {
		return Collections.unmodifiableMap(affinities);
	}

	public Set<String> obtainedClasses() {
		return Collections.unmodifiableSet(obtainedClasses);
	}

	public long lastUpdatedTime() {
		return lastUpdatedTime;
	}

	public String dominantAffinityClass() {
		return affinities.entrySet().stream()
			.max(Map.Entry.comparingByValue())
			.map(Map.Entry::getKey)
			.orElse("");
	}
}
