package com.wanderershaven.classsystem;

import java.util.Set;

public final class ClassPrerequisites {
	private ClassPrerequisites() {
	}

	public static ClassPrerequisite hasClass(String classId) {
		return context -> context.obtainedClasses().contains(classId);
	}

	public static ClassPrerequisite hasAllClasses(Set<String> classIds) {
		return context -> context.obtainedClasses().containsAll(classIds);
	}

	public static ClassPrerequisite minimumAffinity(String classId, double minimumAffinity) {
		return context -> context.affinities().getOrDefault(classId, 0.0) >= minimumAffinity;
	}

	public static ClassPrerequisite alwaysTrue() {
		return context -> true;
	}
}
