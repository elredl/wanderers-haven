package com.wanderershaven.skill;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Stores each player's 5 active skill slot bindings.
 *
 * Slots are indexed 0-4. A null entry means the slot is empty.
 * Thread-safe: slot arrays are replaced atomically.
 */
public final class ActiveSkillSlots {
	private static final int SLOT_COUNT = 5;

	private final Map<UUID, String[]> slots = new ConcurrentHashMap<>();

	/** Returns the skill ID bound to the given slot, or null if empty. */
	public String get(UUID playerId, int slot) {
		String[] arr = slots.get(playerId);
		if (arr == null || slot < 0 || slot >= SLOT_COUNT) return null;
		return arr[slot];
	}

	/** Returns a defensive copy of the player's 5 slots (nulls = empty). */
	public String[] getAll(UUID playerId) {
		String[] arr = slots.get(playerId);
		return arr == null ? new String[SLOT_COUNT] : Arrays.copyOf(arr, SLOT_COUNT);
	}

	/**
	 * Replace the player's entire slot configuration.
	 * {@code newSlots} must have exactly 5 entries; nulls are treated as empty.
	 */
	public void setAll(UUID playerId, String[] newSlots) {
		if (newSlots.length != SLOT_COUNT) {
			throw new IllegalArgumentException("Expected " + SLOT_COUNT + " slots, got " + newSlots.length);
		}
		slots.put(playerId, Arrays.copyOf(newSlots, SLOT_COUNT));
	}

	/** Remove a player's slot data (called on logout to free memory). */
	public void remove(UUID playerId) {
		slots.remove(playerId);
	}
}
