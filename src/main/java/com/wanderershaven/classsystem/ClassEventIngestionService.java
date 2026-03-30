package com.wanderershaven.classsystem;

import com.wanderershaven.classsystem.evolution.ClassEvolutionDef;
import com.wanderershaven.classsystem.evolution.ClassEvolutionEngine;
import com.wanderershaven.network.PlayerNotificationStore;
import com.wanderershaven.compat.BetterCombatCompat;
import com.wanderershaven.compat.WeaponCategoryResolver;
import com.wanderershaven.levelup.ClassLevelEngine;
import com.wanderershaven.levelup.LevelUpEvent;
import com.wanderershaven.skill.SkillDefinition;
import com.wanderershaven.skill.SkillEffectService;
import com.wanderershaven.skill.SkillGrantResult;
import com.wanderershaven.skill.SkillRollEngine;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;

public final class ClassEventIngestionService {
	private static final int SAMPLE_INTERVAL_TICKS = 100;
	private static final double SOCIAL_RANGE = 20.0;
	private static final double STRUCTURE_RANGE = 32.0;
	private static final long SOCIAL_COOLDOWN_TICKS = 200;
	private static final long STRUCTURE_COOLDOWN_TICKS = 300;

	private final ClassInferenceEngine inferenceEngine;
	private final ClassLevelEngine levelEngine;
	private final SkillRollEngine skillEngine;
	private final ClassEvolutionEngine evolutionEngine;
	private final Map<UUID, PlayerObservation> observations = new ConcurrentHashMap<>();

	public ClassEventIngestionService(
		ClassInferenceEngine inferenceEngine,
		ClassLevelEngine levelEngine,
		SkillRollEngine skillEngine,
		ClassEvolutionEngine evolutionEngine
	) {
		this.inferenceEngine = inferenceEngine;
		this.levelEngine = levelEngine;
		this.skillEngine = skillEngine;
		this.evolutionEngine = evolutionEngine;
	}

	public void registerHooks() {
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			ingest(serverPlayer(player), ClassSignalType.BLOCK_BREAK, 1.0, Map.of("block", blockId(state.getBlock())));
		});

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClientSide()) {
				return InteractionResult.PASS;
			}

			ServerPlayer serverPlayer = serverPlayer(player);
			ingest(serverPlayer, ClassSignalType.BLOCK_INTERACT, 0.6, Map.of("hand", hand.name()));

			if (player.getItemInHand(hand).getItem() instanceof BlockItem blockItem) {
				ingest(serverPlayer, ClassSignalType.BLOCK_PLACE, 0.9, Map.of("item", itemId(blockItem)));
			}

			return InteractionResult.PASS;
		});

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClientSide()) {
				return InteractionResult.PASS;
			}

			if (entity instanceof Villager) {
				ingest(serverPlayer(player), ClassSignalType.VILLAGER_INTERACT, 1.0, Map.of("hand", hand.name()));
			}

			return InteractionResult.PASS;
		});

		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClientSide()) {
				return InteractionResult.PASS;
			}

			ServerPlayer serverPlayer = serverPlayer(player);
			Map<String, String> context = new HashMap<>();
			context.put("target", entityTypeId(entity.getType()));
			context.put("entity_category", entityCategory(entity));

			// Better Combat enrichment: weapon_category, two_handed, combo_hit.
			// Falls back to item-based resolution when BC is not installed.
			if (BetterCombatCompat.LOADED) {
				BetterCombatCompat.enrichCombatHitContext(serverPlayer, context);
			}
			if (!context.containsKey("weapon_category")) {
				String wc = WeaponCategoryResolver.resolveFromItem(player.getMainHandItem());
				if (wc != null) context.put("weapon_category", wc);
			}

			ingest(serverPlayer, ClassSignalType.COMBAT_HIT, 1.0, Map.copyOf(context));
			return InteractionResult.PASS;
		});

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (!(entity instanceof ServerPlayer player)) {
				return;
			}

			double riskWeight = 1.0 + (damageTaken / 4.0);

			if (source.getEntity() != null) {
				riskWeight += 0.4;
			}

			if (blocked) {
				riskWeight *= 0.75;
			}

			float healthRatio = player.getMaxHealth() > 0
				? player.getHealth() / player.getMaxHealth()
				: 0.0f;

			ingest(player, ClassSignalType.DAMAGE_TAKEN, riskWeight, Map.of(
				"damage", Float.toString(damageTaken),
				"health_ratio", Float.toString(healthRatio),
				"blocked", Boolean.toString(blocked)
			));
		});

		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, attacker, killedEntity, damageSource) -> {
			if (attacker instanceof ServerPlayer player) {
				Map<String, String> context = new HashMap<>();
				context.put("target", entityTypeId(killedEntity.getType()));
				context.put("entity_category", entityCategory(killedEntity));
				String wc = WeaponCategoryResolver.resolveKillWeapon(player);
				if (wc != null) context.put("weapon_category", wc);
				if (killedEntity instanceof Player killedPlayer) {
					context.put("target_uuid", killedPlayer.getUUID().toString());
				}
				ingest(player, ClassSignalType.KILL, 1.4, Map.copyOf(context));
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				samplePlayerSignals(player);
			}
		});
	}

	private void samplePlayerSignals(ServerPlayer player) {
		long gameTime = player.level().getDayTime();
		PlayerObservation observation = observations.computeIfAbsent(player.getUUID(), ignored -> captureBaseline(player));

		captureExploration(player, observation, gameTime);
		captureSocialSignals(player, observation, gameTime);

		if (gameTime % SAMPLE_INTERVAL_TICKS != 0) {
			return;
		}

		captureCraftingAndEconomy(player, observation);
	}

	private void captureCraftingAndEconomy(ServerPlayer player, PlayerObservation observation) {
		int craftedTotal = totalCraftedItems(player);
		if (craftedTotal > observation.craftedTotal) {
			ingest(player, ClassSignalType.CRAFT, craftedTotal - observation.craftedTotal, Map.of());
			observation.craftedTotal = craftedTotal;
		}

		int smeltingInteractions = totalCustomStat(player, Stats.INTERACT_WITH_FURNACE)
			+ totalCustomStat(player, Stats.INTERACT_WITH_BLAST_FURNACE)
			+ totalCustomStat(player, Stats.INTERACT_WITH_SMOKER);
		if (smeltingInteractions > observation.smeltingInteractions) {
			ingest(player, ClassSignalType.SMELT, smeltingInteractions - observation.smeltingInteractions, Map.of());
			observation.smeltingInteractions = smeltingInteractions;
		}

		int brewingInteractions = totalCustomStat(player, Stats.INTERACT_WITH_BREWINGSTAND);
		if (brewingInteractions > observation.brewingInteractions) {
			ingest(player, ClassSignalType.BREW, brewingInteractions - observation.brewingInteractions, Map.of());
			observation.brewingInteractions = brewingInteractions;
		}

		int trades = totalCustomStat(player, Stats.TRADED_WITH_VILLAGER);
		if (trades > observation.tradeCount) {
			ingest(player, ClassSignalType.TRADE, trades - observation.tradeCount, Map.of());
			observation.tradeCount = trades;
		}

		int blockPlaceUses = totalBlockItemUses(player);
		if (blockPlaceUses > observation.blockPlaceUses) {
			ingest(player, ClassSignalType.BLOCK_PLACE, (blockPlaceUses - observation.blockPlaceUses) * 0.5, Map.of("source", "stat_delta"));
			observation.blockPlaceUses = blockPlaceUses;
		}
	}

	private void captureExploration(ServerPlayer player, PlayerObservation observation, long gameTime) {
		ResourceKey<Biome> currentBiome = player.level().getBiome(player.blockPosition()).unwrapKey().orElse(null);

		if (currentBiome != null && observation.lastBiome != null && !observation.lastBiome.equals(currentBiome)) {
			ingest(player, ClassSignalType.BIOME_DISCOVERY, 1.0, Map.of("biome", currentBiome.identifier().toString()));
		}

		observation.lastBiome = currentBiome;

		AABB scanBox = new AABB(player.blockPosition()).inflate(STRUCTURE_RANGE);
		long nearbyVillagers = player.level().getEntitiesOfClass(Villager.class, scanBox, entity -> true).size();

		if (nearbyVillagers >= 3 && readyForSignal(observation, ClassSignalType.STRUCTURE_PROXIMITY, gameTime, STRUCTURE_COOLDOWN_TICKS)) {
			ingest(player, ClassSignalType.STRUCTURE_PROXIMITY, 1.0, Map.of("signal", "village_proximity"));
		}
	}

	private void captureSocialSignals(ServerPlayer player, PlayerObservation observation, long gameTime) {
		long nearbyPlayers = player.level()
			.getPlayers(other -> !other.getUUID().equals(player.getUUID()) && other.distanceToSqr(player) <= SOCIAL_RANGE * SOCIAL_RANGE)
			.size();

		boolean hasNearbyPlayers = nearbyPlayers > 0;
		if (hasNearbyPlayers && !observation.hadNearbyPlayers && readyForSignal(observation, ClassSignalType.NEARBY_PLAYERS, gameTime, SOCIAL_COOLDOWN_TICKS)) {
			ingest(player, ClassSignalType.NEARBY_PLAYERS, 1.0, Map.of("count", Long.toString(nearbyPlayers)));
		}
		observation.hadNearbyPlayers = hasNearbyPlayers;

		if (player.isSleeping() && hasNearbySleepingPlayer(player) && readyForSignal(observation, ClassSignalType.SHARED_BED, gameTime, SOCIAL_COOLDOWN_TICKS)) {
			ingest(player, ClassSignalType.SHARED_BED, 1.0, Map.of());
		}

		if (isConsumingFood(player) && hasNearbyEatingPlayer(player) && readyForSignal(observation, ClassSignalType.SHARED_MEAL, gameTime, SOCIAL_COOLDOWN_TICKS)) {
			ingest(player, ClassSignalType.SHARED_MEAL, 1.0, Map.of());
		}
	}

	private boolean hasNearbySleepingPlayer(ServerPlayer player) {
		return !player.level().getPlayers(other -> !other.getUUID().equals(player.getUUID())
			&& other.distanceToSqr(player) <= SOCIAL_RANGE * SOCIAL_RANGE
			&& other.isSleeping()).isEmpty();
	}

	private boolean hasNearbyEatingPlayer(ServerPlayer player) {
		return !player.level().getPlayers(other -> !other.getUUID().equals(player.getUUID())
			&& other.distanceToSqr(player) <= SOCIAL_RANGE * SOCIAL_RANGE
			&& isConsumingFood(other)).isEmpty();
	}

	private boolean isConsumingFood(Player player) {
		if (!player.isUsingItem()) {
			return false;
		}

		ItemUseAnimation animation = player.getUseItem().getUseAnimation();
		return animation == ItemUseAnimation.EAT || animation == ItemUseAnimation.DRINK;
	}

	private static ServerPlayer serverPlayer(Player player) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			throw new IllegalStateException("Expected server player");
		}

		return serverPlayer;
	}

	/**
	 * Process all deferred level-up events for a player: roll skills, apply attribute
	 * effects, queue GUI notifications, and set any pending evolution offers.
	 *
	 * Called from {@code WanderersHavenNetworking.sendOpenSkillManagement} just before
	 * building the payload, so every opened GUI shows the latest state.
	 */
	public void processQueuedLevelUps(ServerPlayer player) {
		List<LevelUpEvent> levelUps = levelEngine.drainLevelUpEvents(player.getUUID());
		for (LevelUpEvent event : levelUps) {
			String classDisplayName = getClassDisplayName(event.classId());
			PlayerNotificationStore.recordLevelUp(
				player.getUUID(), event.classId(), classDisplayName, event.newLevel());

			skillEngine.tryRollSkill(player.getUUID(), event.classId(), event.newLevel())
				.ifPresent(result -> {
					SkillEffectService.applySkill(player, result.granted().id());
					if (result.isUpgrade()) {
						String oldName = skillEngine.findById(result.supersededId())
							.map(SkillDefinition::displayName)
							.orElse(result.supersededId());
						PlayerNotificationStore.recordSkillChange(
							player.getUUID(), oldName, result.granted().displayName());
					} else {
						PlayerNotificationStore.recordSkillGrant(
							player.getUUID(), result.granted().displayName());
					}
				});

			if (event.newLevel() % 25 == 0) {
				Set<String> ownedClassIds = inferenceEngine.profile(player.getUUID())
					.map(PlayerClassProfile::obtainedClasses)
					.map(HashSet::new)
					.orElse(new HashSet<>());
				List<ClassEvolutionDef> offers = evolutionEngine.eligibleEvolutions(
					player.getUUID(), event.classId(), event.newLevel(), ownedClassIds
				);
				if (!offers.isEmpty()) {
					evolutionEngine.setPendingOffer(player.getUUID(), offers);
					// Evolution is bundled into the OpenSkillManagementPayload — no separate packet needed.
				}
			}
		}
	}

	private String getClassDisplayName(String classId) {
		ClassDefinition def = inferenceEngine.registeredClasses().get(classId);
		return def != null ? def.displayName() : classId;
	}

	private void ingest(ServerPlayer player, ClassSignalType type, double weight, Map<String, String> context) {
		String worldKey = player.level().dimension().identifier().toString();
		long gameTime = player.level().getDayTime();
		ClassSignal signal = new ClassSignal(player.getUUID(), type, weight, gameTime, worldKey, context);

		inferenceEngine.ingest(signal);
		levelEngine.observe(signal);
		evolutionEngine.processSignal(signal);
		// Level-up processing (skill rolls, notifications) is deferred to processQueuedLevelUps(),
		// which runs when the player opens the GUI via sleep or /wh gui.
	}

	private boolean readyForSignal(PlayerObservation observation, ClassSignalType signalType, long gameTime, long cooldown) {
		long lastSignalTick = observation.lastSignalTime.getOrDefault(signalType, 0L);

		if ((gameTime - lastSignalTick) < cooldown) {
			return false;
		}

		observation.lastSignalTime.put(signalType, gameTime);
		return true;
	}

	private int totalCraftedItems(ServerPlayer player) {
		int total = 0;

		for (Item item : BuiltInRegistries.ITEM) {
			total += player.getStats().getValue(Stats.ITEM_CRAFTED, item);
		}

		return total;
	}

	private int totalBlockItemUses(ServerPlayer player) {
		int total = 0;

		for (Item item : BuiltInRegistries.ITEM) {
			if (item instanceof BlockItem) {
				total += player.getStats().getValue(Stats.ITEM_USED, item);
			}
		}

		return total;
	}

	private int totalCustomStat(ServerPlayer player, Identifier statId) {
		return player.getStats().getValue(Stats.CUSTOM, statId);
	}

	private static String blockId(net.minecraft.world.level.block.Block block) {
		return BuiltInRegistries.BLOCK.getKey(block).toString();
	}

	private static String itemId(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	private static String entityTypeId(net.minecraft.world.entity.EntityType<?> type) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
	}

	/**
	 * Broad category label for an entity, used by the evolution engine to
	 * bucket kill and hit signals into playstyle dimensions.
	 *
	 * Categories (in priority order):
	 *   "player"     — another player
	 *   "boss"       — ender dragon, wither, elder guardian, warden
	 *   "undead"     — EntityTypeTags.UNDEAD (zombies, skeletons, phantoms, …)
	 *   "arthropod"  — EntityTypeTags.ARTHROPOD (spiders, silverfish, endermites, …)
	 *   "illager"    — EntityTypeTags.ILLAGER (pillagers, vindicators, …)
	 *   "monster"    — everything else that is a hostile mob
	 *   "neutral"    — passive / ambient entities
	 */
	private static String entityCategory(Entity entity) {
		if (entity instanceof Player) return "player";
		if (entity instanceof EnderDragon
			|| entity instanceof WitherBoss
			|| entity instanceof ElderGuardian
			|| entity instanceof Warden) return "boss";
		if (entity.getType().is(EntityTypeTags.UNDEAD)) return "undead";
		if (entity.getType().is(EntityTypeTags.ARTHROPOD)) return "arthropod";
		if (entity.getType().is(EntityTypeTags.ILLAGER)) return "illager";
		if (entity instanceof net.minecraft.world.entity.monster.Monster) return "monster";
		return "neutral";
	}

	private PlayerObservation captureBaseline(ServerPlayer player) {
		PlayerObservation observation = new PlayerObservation();
		observation.craftedTotal = totalCraftedItems(player);
		observation.smeltingInteractions = totalCustomStat(player, Stats.INTERACT_WITH_FURNACE)
			+ totalCustomStat(player, Stats.INTERACT_WITH_BLAST_FURNACE)
			+ totalCustomStat(player, Stats.INTERACT_WITH_SMOKER);
		observation.brewingInteractions = totalCustomStat(player, Stats.INTERACT_WITH_BREWINGSTAND);
		observation.tradeCount = totalCustomStat(player, Stats.TRADED_WITH_VILLAGER);
		observation.blockPlaceUses = totalBlockItemUses(player);
		observation.lastBiome = player.level().getBiome(player.blockPosition()).unwrapKey().orElse(null);
		observation.hadNearbyPlayers = false;
		return observation;
	}

	private static final class PlayerObservation {
		private int craftedTotal;
		private int smeltingInteractions;
		private int brewingInteractions;
		private int tradeCount;
		private int blockPlaceUses;
		private ResourceKey<Biome> lastBiome;
		private boolean hadNearbyPlayers;
		private final EnumMap<ClassSignalType, Long> lastSignalTime = new EnumMap<>(ClassSignalType.class);
	}
}
