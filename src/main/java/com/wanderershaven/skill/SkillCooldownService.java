package com.wanderershaven.skill;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Centralized cooldown storage for skill actions. */
public final class SkillCooldownService {

	private final Map<String, Map<UUID, Long>> startedAtByKey = new ConcurrentHashMap<>();

	public boolean isReady(UUID playerId, String key, long now, long cooldownTicks) {
		return remainingTicks(playerId, key, now, cooldownTicks) == 0L;
	}

	public long remainingTicks(UUID playerId, String key, long now, long cooldownTicks) {
		Long startedAt = startedAtByKey
			.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>())
			.get(playerId);
		if (startedAt == null) return 0L;
		long elapsed = now - startedAt;
		if (elapsed >= cooldownTicks) return 0L;
		return cooldownTicks - elapsed;
	}

	public void start(UUID playerId, String key, long now) {
		startedAtByKey
			.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>())
			.put(playerId, now);
	}
}
