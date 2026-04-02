package com.wanderershaven.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks active timed effects by effect key and player UUID. */
public final class TimedEffectTracker {

	public record Window(long startedAt, long expiresAt) {}

	private final Map<String, Map<UUID, Window>> windowsByKey = new ConcurrentHashMap<>();

	public void start(UUID playerId, String key, long now, long durationTicks) {
		windowsByKey
			.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>())
			.put(playerId, new Window(now, now + durationTicks));
	}

	public Window get(UUID playerId, String key) {
		Map<UUID, Window> byPlayer = windowsByKey.get(key);
		if (byPlayer == null) return null;
		return byPlayer.get(playerId);
	}

	public boolean isActive(UUID playerId, String key, long now) {
		Window window = get(playerId, key);
		if (window == null) return false;
		if (now >= window.expiresAt()) {
			clear(playerId, key);
			return false;
		}
		return true;
	}

	public void clear(UUID playerId, String key) {
		Map<UUID, Window> byPlayer = windowsByKey.get(key);
		if (byPlayer == null) return;
		byPlayer.remove(playerId);
	}

	public Map<UUID, Window> snapshot(String key) {
		Map<UUID, Window> byPlayer = windowsByKey.get(key);
		if (byPlayer == null || byPlayer.isEmpty()) return Map.of();
		return new HashMap<>(byPlayer);
	}
}
