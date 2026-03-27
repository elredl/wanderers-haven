package com.wanderershaven.classsystem;

import java.util.UUID;

public record ClassConditionNotification(
	UUID playerId,
	String classId,
	String message,
	long gameTime
) {
}
