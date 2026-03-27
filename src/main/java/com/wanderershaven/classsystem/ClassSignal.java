package com.wanderershaven.classsystem;

import java.util.Map;
import java.util.UUID;

public record ClassSignal(
	UUID playerId,
	ClassSignalType type,
	double weight,
	long gameTime,
	String worldKey,
	Map<String, String> context
) {
	public ClassSignal {
		context = Map.copyOf(context);
	}

	public static ClassSignal of(UUID playerId, ClassSignalType type, double weight, long gameTime, String worldKey) {
		return new ClassSignal(playerId, type, weight, gameTime, worldKey, Map.of());
	}
}
