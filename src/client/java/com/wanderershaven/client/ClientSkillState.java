package com.wanderershaven.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Holds the local player's current skill IDs and active slot bindings on the client side. */
@Environment(EnvType.CLIENT)
public final class ClientSkillState {
	private static final Set<String> skills = Collections.synchronizedSet(new HashSet<>());
	private static final String[] activeSlots = new String[5];

	private ClientSkillState() {}

	// ── Skill ownership ───────────────────────────────────────────────────────

	/** Replace the current skill set with the given list (called on sync from server). */
	public static void update(List<String> ids) {
		skills.clear();
		skills.addAll(ids);
	}

	/** Returns true if the local player owns the given skill. */
	public static boolean has(String skillId) {
		return skills.contains(skillId);
	}

	// ── Active slot bindings ─────────────────────────────────────────────────

	/** Replace all 5 active slot bindings (null = empty). */
	public static synchronized void updateSlots(List<String> slots) {
		for (int i = 0; i < 5; i++) {
			activeSlots[i] = (i < slots.size()) ? slots.get(i) : null;
		}
	}

	/** Returns the skill ID bound to the given slot (0-4), or null if empty. */
	public static synchronized String getSlot(int index) {
		if (index < 0 || index >= 5) return null;
		return activeSlots[index];
	}

	/** Returns a defensive copy of all 5 slot bindings. */
	public static synchronized String[] getAllSlots() {
		return Arrays.copyOf(activeSlots, 5);
	}

	/** Bind a skill to a slot locally (before saving to server). */
	public static synchronized void setSlot(int index, String skillId) {
		if (index >= 0 && index < 5) activeSlots[index] = skillId;
	}
}
