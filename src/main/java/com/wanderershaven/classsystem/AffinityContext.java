package com.wanderershaven.classsystem;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record AffinityContext(
	UUID playerId,
	ClassSignal signal,
	Map<String, Double> affinities,
	Set<String> obtainedClasses,
	List<ClassSignal> recentSignals
) {
}
