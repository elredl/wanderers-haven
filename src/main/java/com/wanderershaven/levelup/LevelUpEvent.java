package com.wanderershaven.levelup;

import java.util.UUID;

public record LevelUpEvent(UUID playerId, String classId, int newLevel) {}
