package com.wanderershaven.network;

import com.wanderershaven.classsystem.ClassDefinition;
import com.wanderershaven.classsystem.ClassInferenceEngine;
import com.wanderershaven.classsystem.ClassSystemBootstrap;
import com.wanderershaven.classsystem.evolution.ClassEvolutionDef;
import com.wanderershaven.skill.ActiveSkillSlots;
import com.wanderershaven.skill.SkillDefinition;
import com.wanderershaven.skill.SkillEffectService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class WanderersHavenNetworking {
	private WanderersHavenNetworking() {
	}

	/** Register payload types for both directions. Must be called during common init. */
	public static void registerCommon() {
		PayloadTypeRegistry.playS2C().register(OpenClassSelectionPayload.TYPE, OpenClassSelectionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SyncPlayerSkillsPayload.TYPE, SyncPlayerSkillsPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenSkillManagementPayload.TYPE, OpenSkillManagementPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenEvolutionSelectionPayload.TYPE, OpenEvolutionSelectionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(PlaySkillAnimationPayload.TYPE, PlaySkillAnimationPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ClassDecisionPayload.TYPE, ClassDecisionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(UpdateActiveSkillSlotsPayload.TYPE, UpdateActiveSkillSlotsPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(UseActiveSkillPayload.TYPE, UseActiveSkillPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(EvolutionChoicePayload.TYPE, EvolutionChoicePayload.CODEC);
	}

	/** Register all C2S handlers that run on the server. */
	public static void registerServerReceiver() {
		// Class decision
		ServerPlayNetworking.registerGlobalReceiver(ClassDecisionPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				ClassInferenceEngine engine = ClassSystemBootstrap.engine();
				UUID playerId = context.player().getUUID();
				switch (payload.decision()) {
					case ACCEPT -> engine.acceptClass(playerId, payload.classId());
					case DENY -> engine.denyClass(playerId, payload.classId());
					case PERMANENT_DENY -> engine.permanentlyDenyClass(playerId, payload.classId());
				}
			});
		});

		// Active slot update — player saved new slot config from Skill Management screen
		ServerPlayNetworking.registerGlobalReceiver(UpdateActiveSkillSlotsPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				List<String> incoming = payload.slots();
				String[] arr = new String[5];
				for (int i = 0; i < 5; i++) {
					arr[i] = (i < incoming.size()) ? incoming.get(i) : null;
				}
				// Only allow binding skills the player actually owns
				Set<String> allOwned = allOwnedSkillIds(player);
				for (int i = 0; i < 5; i++) {
					if (arr[i] != null && !allOwned.contains(arr[i])) {
						arr[i] = null;
					}
				}
				ClassSystemBootstrap.activeSkillSlots().setAll(player.getUUID(), arr);
			});
		});

		// Evolution choice — player selected an evolution path
		ServerPlayNetworking.registerGlobalReceiver(EvolutionChoicePayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				boolean accepted = ClassSystemBootstrap.evolutionEngine().acceptEvolution(player.getUUID(), payload.evolutionId());
				if (accepted) {
					player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
						"[Wanderers Haven] Evolution locked in: " + payload.evolutionId()
					));
				}
			});
		});

		// Active skill use — player activated a slot from the radial menu
		ServerPlayNetworking.registerGlobalReceiver(UseActiveSkillPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				int slot = payload.slotIndex();
				if (slot < 0 || slot >= 5) return;
				String skillId = ClassSystemBootstrap.activeSkillSlots().get(player.getUUID(), slot);
				if (skillId == null) return;
				executeActiveSkill(player, skillId);
			});
		});
	}

	/**
	 * Register a tick hook that fires the appropriate sleep screen when a player
	 * first enters a bed:
	 *   - Pending class decisions present → ClassSelectionScreen
	 *   - No pending decisions             → SkillManagementScreen
	 */
	public static void registerSleepHook() {
		Set<UUID> sleepingPlayers = ConcurrentHashMap.newKeySet();
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				UUID uuid = player.getUUID();
				if (player.isSleeping()) {
					if (sleepingPlayers.add(uuid)) {
						// Always show skill management (with evolution bundled); class decisions open on top if present
						sendOpenSkillManagement(player);
						sendPendingClassesTo(player);
					}
				} else {
					sleepingPlayers.remove(uuid);
				}
			}
		});

		// Clean up slot data and queued notifications when a player disconnects
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ClassSystemBootstrap.activeSkillSlots().remove(handler.player.getUUID());
			PlayerNotificationStore.clear(handler.player.getUUID());
		});
	}

	/** Collect all owned skill IDs across every class and send them to the player's client. */
	public static void sendSkillsTo(ServerPlayer player) {
		List<String> allSkillIds = allOwnedSkillIds(player).stream().collect(Collectors.toList());
		ServerPlayNetworking.send(player, new SyncPlayerSkillsPayload(allSkillIds));
	}

	/**
	 * Build the pending-class payload and send it.
	 * Returns true if classes were sent, false if the player had none pending.
	 */
	public static boolean sendPendingClassesTo(ServerPlayer player) {
		ClassInferenceEngine engine = ClassSystemBootstrap.engine();
		Set<String> pending = engine.pendingClasses(player.getUUID());
		if (pending.isEmpty()) {
			return false;
		}

		Map<String, ClassDefinition> definitions = engine.registeredClasses();
		List<OpenClassSelectionPayload.PendingClassEntry> entries = new ArrayList<>();
		for (String classId : pending) {
			ClassDefinition def = definitions.get(classId);
			if (def == null) continue;
			entries.add(new OpenClassSelectionPayload.PendingClassEntry(
				classId,
				def.displayName(),
				engine.denyCount(player.getUUID(), classId)
			));
		}

		if (!entries.isEmpty()) {
			ServerPlayNetworking.send(player, new OpenClassSelectionPayload(entries));
			return true;
		}
		return false;
	}

	/**
	 * If the player has a pending evolution offer that they haven't resolved yet,
	 * re-send the selection screen. Called on sleep so players never permanently miss it.
	 */
	public static void sendPendingEvolutionTo(ServerPlayer player) {
		List<ClassEvolutionDef> pending = ClassSystemBootstrap.evolutionEngine().getPendingOffer(player.getUUID());
		if (pending.isEmpty()) return;
		// Derive base class from the first offer (all entries share the same base class)
		String baseClassId = pending.get(0).baseClassId();
		sendEvolutionSelection(player, baseClassId, pending);
	}

	/** Build and send the OpenEvolutionSelectionPayload to the given player. */
	public static void sendEvolutionSelection(ServerPlayer player, String baseClassId, List<ClassEvolutionDef> offers) {
		List<OpenEvolutionSelectionPayload.EvolutionEntry> entries = offers.stream()
			.map(def -> new OpenEvolutionSelectionPayload.EvolutionEntry(
				def.id(), def.displayName(), def.description()))
			.collect(Collectors.toList());
		ServerPlayNetworking.send(player, new OpenEvolutionSelectionPayload(baseClassId, entries));
	}

	/** Build and send the OpenSkillManagementPayload to the given player, bundling any pending evolution. */
	public static void sendOpenSkillManagement(ServerPlayer player) {
		// Process any deferred level-ups: roll skills, apply effects, queue notifications,
		// set evolution offers. Must happen before reading owned skills or notifications below.
		ClassSystemBootstrap.ingestionService().processQueuedLevelUps(player);

		// Collect all owned skill definitions, preserving the active flag
		List<OpenSkillManagementPayload.SkillEntry> skillEntries =
			ClassSystemBootstrap.engine().registeredClasses().keySet().stream()
				.flatMap(classId -> ClassSystemBootstrap.skillEngine()
					.ownedSkills(player.getUUID(), classId).stream())
				.map(def -> new OpenSkillManagementPayload.SkillEntry(
					def.id(), def.displayName(), def.powerLevel(), def.description(), def.active()))
				.collect(Collectors.toList());

		// Current slot bindings
		String[] arr = ClassSystemBootstrap.activeSkillSlots().getAll(player.getUUID());
		List<String> slotList = new ArrayList<>(5);
		for (String s : arr) slotList.add(s);

		// Bundle any pending evolution offer
		List<ClassEvolutionDef> pendingEvo = ClassSystemBootstrap.evolutionEngine().getPendingOffer(player.getUUID());
		String evoBaseClass = "";
		List<OpenSkillManagementPayload.EvolutionEntry> evoOffers = List.of();
		if (!pendingEvo.isEmpty()) {
			evoBaseClass = pendingEvo.get(0).baseClassId();
			evoOffers = pendingEvo.stream()
				.map(def -> new OpenSkillManagementPayload.EvolutionEntry(def.id(), def.displayName(), def.description()))
				.collect(Collectors.toList());
		}

		// Drain queued notifications (level-ups + skill grants since last GUI open)
		List<String> notifications = PlayerNotificationStore.drain(player.getUUID());

		ServerPlayNetworking.send(player, new OpenSkillManagementPayload(skillEntries, slotList, evoBaseClass, evoOffers, notifications));
	}

	// -------------------------------------------------------------------------
	// Active skill execution
	// -------------------------------------------------------------------------

	/**
	 * Execute an active skill for the given player.
	 */
	private static void executeActiveSkill(ServerPlayer player, String skillId) {
		switch (skillId) {
			case "warrior_heavy_strikes"   -> SkillEffectService.executeHeavyStrikes(player);
			case "warrior_battle_cry_weak" -> SkillEffectService.executeBattleCryWeak(player);
			case "warrior_bludgeon"        -> SkillEffectService.executeBludgeon(player);
			case "warrior_piercing_charge" -> SkillEffectService.executePiercingCharge(player);
		}
	}

	// -------------------------------------------------------------------------
	// Internal helpers
	// -------------------------------------------------------------------------

	private static Set<String> allOwnedSkillIds(ServerPlayer player) {
		return ClassSystemBootstrap.engine().registeredClasses().keySet().stream()
			.flatMap(classId -> ClassSystemBootstrap.skillEngine()
				.ownedSkillIds(player.getUUID(), classId).stream())
			.collect(Collectors.toSet());
	}
}
