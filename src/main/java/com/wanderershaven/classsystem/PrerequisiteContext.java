package com.wanderershaven.classsystem;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record PrerequisiteContext(
	Map<String, Double> affinities,
	Set<String> obtainedClasses,
	List<ClassSignal> recentSignals
) {
}
