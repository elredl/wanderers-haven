package com.wanderershaven.stat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Central manager for all player attribute modifiers contributed by skills and buffs.
 *
 * Sources are registered via {@link #register} with their {@link StatContribution}s.
 * Every server tick the engine re-evaluates every source for every online player,
 * adding, updating, or removing modifiers as conditions and amounts change.
 *
 * Timed buffs use {@link #activateSource} / {@link #deactivateSource} to flip a flag
 * that their contributions' condition predicates read.
 */
public final class PlayerStatEngine {

	// source ID → ordered list of contributions
	private final Map<String, List<StatContribution>> table = new LinkedHashMap<>();

	// player → source → per-contribution last-applied amounts (null entry = not applied)
	private final Map<UUID, Map<String, Double[]>> lastApplied = new ConcurrentHashMap<>();

	// timed-buff activation flags per player
	private final Map<UUID, Set<String>> activeSources = new ConcurrentHashMap<>();

	// variable buff amounts (e.g. Fury Unleashed stores the fury% bonus here)
	private final Map<UUID, Map<String, Double>> buffAmounts = new ConcurrentHashMap<>();

	// ── Registration ──────────────────────────────────────────────────────────

	public void register(String sourceId, StatContribution... contributions) {
		table.put(sourceId, List.of(contributions));
	}

	// ── Timed-buff control ────────────────────────────────────────────────────

	/** Mark a timed-buff source as active; the engine will apply its modifiers on the next tick. */
	public void activateSource(UUID playerId, String sourceId) {
		activeSources.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(sourceId);
	}

	/**
	 * Mark a timed-buff source as inactive and immediately remove its modifiers.
	 * Pass {@code null} for {@code player} only when the player is offline
	 * (MC already strips transient modifiers on logout).
	 */
	public void deactivateSource(UUID playerId, String sourceId, ServerPlayer player) {
		Set<String> active = activeSources.get(playerId);
		if (active != null) active.remove(sourceId);

		List<StatContribution> contributions = table.get(sourceId);
		if (contributions == null) return;
		Double[] state = stateFor(playerId, sourceId, contributions.size());
		for (int i = 0; i < contributions.size(); i++) {
			if (state[i] == null) continue;
			if (player != null) {
				AttributeInstance inst = player.getAttribute(contributions.get(i).attribute());
				if (inst != null) inst.removeModifier(contributions.get(i).modifierId());
			}
			state[i] = null;
		}
	}

	/** Set the effective amount for a variable buff (call before {@link #activateSource}). */
	public void setBuffAmount(UUID playerId, String sourceId, double amount) {
		buffAmounts.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(sourceId, amount);
	}

	/** Read a stored buff amount (used inside dynamic contribution lambdas). */
	public double getBuffAmount(UUID playerId, String sourceId) {
		Map<String, Double> map = buffAmounts.get(playerId);
		return map != null ? map.getOrDefault(sourceId, 0.0) : 0.0;
	}

	/** True if a timed-buff source is currently flagged as active for the player. */
	public boolean isSourceActive(UUID playerId, String sourceId) {
		Set<String> active = activeSources.get(playerId);
		return active != null && active.contains(sourceId);
	}

	// ── Lifecycle ─────────────────────────────────────────────────────────────

	/** Apply all stat contributions for a player from scratch (call on join or skill grant). */
	public void applyAll(ServerPlayer player) {
		for (String sourceId : table.keySet()) {
			updateSource(player, sourceId);
		}
	}

	/** Re-evaluate all contributions for every online player. Register as an END_SERVER_TICK listener. */
	public void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			for (String sourceId : table.keySet()) {
				updateSource(player, sourceId);
			}
		}
	}

	/** Remove all tracked state for a player (call on logout). */
	public void removePlayer(UUID playerId) {
		lastApplied.remove(playerId);
		activeSources.remove(playerId);
		buffAmounts.remove(playerId);
	}

	// ── Internal ──────────────────────────────────────────────────────────────

	private void updateSource(ServerPlayer player, String sourceId) {
		List<StatContribution> contributions = table.get(sourceId);
		if (contributions == null) return;
		Double[] state = stateFor(player.getUUID(), sourceId, contributions.size());

		for (int i = 0; i < contributions.size(); i++) {
			StatContribution sc = contributions.get(i);
			boolean active = sc.condition().test(player);

			if (!active) {
				if (state[i] != null) {
					AttributeInstance inst = player.getAttribute(sc.attribute());
					if (inst != null) inst.removeModifier(sc.modifierId());
					state[i] = null;
				}
				continue;
			}

			double newAmount = sc.amount().applyAsDouble(player);
			if (state[i] != null && Math.abs(state[i] - newAmount) < 1e-9) continue;

			AttributeInstance inst = player.getAttribute(sc.attribute());
			if (inst == null) continue;
			inst.removeModifier(sc.modifierId());
			inst.addTransientModifier(new AttributeModifier(sc.modifierId(), newAmount, sc.operation()));
			state[i] = newAmount;
		}
	}

	private Double[] stateFor(UUID playerId, String sourceId, int size) {
		return lastApplied
			.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
			.computeIfAbsent(sourceId, k -> new Double[size]);
	}
}
