package com.wanderershaven.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side per-player queue of level-up and skill notifications.
 *
 * Level-ups are deduplicated per class — only the highest level reached
 * between GUI openings is reported. Skill notifications are all retained
 * in order.
 *
 * Call {@link #drain(UUID)} when building the skill-management payload.
 * The queue is cleared on drain.
 */
public final class PlayerNotificationStore {
	private PlayerNotificationStore() {}

	/** classId → highest level reached since last drain */
	private static final Map<UUID, Map<String, int[]>> pendingLevels = new ConcurrentHashMap<>();
	/** classId → display name (paired with pendingLevels) */
	private static final Map<UUID, Map<String, String>> levelDisplayNames = new ConcurrentHashMap<>();
	/** ordered list of fully-formatted skill notification strings */
	private static final Map<UUID, List<String>> pendingSkills = new ConcurrentHashMap<>();

	public static void recordLevelUp(UUID playerId, String classId, String classDisplayName, int level) {
		pendingLevels
			.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
			.merge(classId, new int[]{level}, (a, b) -> a[0] >= b[0] ? a : b);
		levelDisplayNames
			.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
			.put(classId, classDisplayName);
	}

	public static void recordSkillGrant(UUID playerId, String skillDisplayName) {
		pendingSkills
			.computeIfAbsent(playerId, k -> Collections.synchronizedList(new ArrayList<>()))
			.add("[Skill - " + skillDisplayName + " Obtained!]");
	}

	public static void recordSkillChange(UUID playerId, String oldSkillName, String newSkillName) {
		pendingSkills
			.computeIfAbsent(playerId, k -> Collections.synchronizedList(new ArrayList<>()))
			.add("[Skill Change - " + oldSkillName + " -> " + newSkillName + "]");
	}

	/**
	 * Returns all queued notifications as formatted strings and clears the queue.
	 * Level-ups come first (one per class, highest level only), then skill
	 * notifications in order.
	 */
	public static List<String> drain(UUID playerId) {
		List<String> result = new ArrayList<>();

		Map<String, int[]> levels = pendingLevels.remove(playerId);
		Map<String, String> names  = levelDisplayNames.remove(playerId);
		if (levels != null) {
			for (Map.Entry<String, int[]> e : levels.entrySet()) {
				String displayName = (names != null) ? names.getOrDefault(e.getKey(), e.getKey()) : e.getKey();
				result.add("[" + displayName + " Level " + e.getValue()[0] + "!]");
			}
		}

		List<String> skills = pendingSkills.remove(playerId);
		if (skills != null) {
			result.addAll(skills);
		}

		return result;
	}

	/** Clear all queued notifications for a player (e.g. on disconnect). */
	public static void clear(UUID playerId) {
		pendingLevels.remove(playerId);
		levelDisplayNames.remove(playerId);
		pendingSkills.remove(playerId);
	}
}
