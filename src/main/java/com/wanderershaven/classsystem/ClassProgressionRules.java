package com.wanderershaven.classsystem;

public record ClassProgressionRules(
	double multiclassPenaltyPerObtainedClass,
	int recentSignalHistoryLimit
) {
	public static ClassProgressionRules defaults() {
		return new ClassProgressionRules(0.25, 128);
	}

	public double classGainScaling(int obtainedClassCount, boolean classAlreadyObtained) {
		if (classAlreadyObtained) {
			return 1.0;
		}

		return 1.0 / (1.0 + (obtainedClassCount * multiclassPenaltyPerObtainedClass));
	}
}
