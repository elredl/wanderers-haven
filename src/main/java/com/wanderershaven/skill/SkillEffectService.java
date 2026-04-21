package com.wanderershaven.skill;

import com.wanderershaven.classsystem.ClassSystemBootstrap;
import com.wanderershaven.compat.WeaponCategoryResolver;
import com.wanderershaven.network.WanderersHavenNetworking;
import com.wanderershaven.network.PlaySkillAnimationPayload;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.damagesource.CombatRules;

/**
 * Applies and manages active skill effects for players.
 *
 * Attribute-based stat contributions (strength, speed, DR, etc.) are now
 * declared in {@link com.wanderershaven.stat.SkillStatTable} and managed
 * automatically by {@link com.wanderershaven.stat.PlayerStatEngine}.
 * This service handles: cooldowns, timed-buff activation/expiry, active skill
 * execution, and event-driven skills (Second Wind, Last Stand, Battle Cry, etc.).
 */
public final class SkillEffectService {
	private SkillEffectService() {}

	// ── Modifier IDs used for enemy debuffs (applied to non-player entities) ──

	public static final Identifier BLUDGEON_ARMOR_MOD    = Identifier.fromNamespaceAndPath("wanderers_haven", "bludgeon_armor");
	public static final Identifier SHIELD_BASH_ARMOR_MOD = Identifier.fromNamespaceAndPath("wanderers_haven", "shield_bash_armor");
	public static final Identifier HARMONY_PAIN_SLOW_MOD = Identifier.fromNamespaceAndPath("wanderers_haven", "harmony_pain_slow");
	public static final Identifier MADDENING_ARMOR_MOD   = Identifier.fromNamespaceAndPath("wanderers_haven", "maddening_armor");
	public static final Identifier INTIMIDATING_AURA_SLOW_MOD = Identifier.fromNamespaceAndPath("wanderers_haven", "intimidating_aura_slow");
	public static final Identifier MOCKING_SHOUT_SLOW_MOD = Identifier.fromNamespaceAndPath("wanderers_haven", "mocking_shout_slow");
	public static final Identifier TERRORIZING_HOWL_FEAR_SLOW_MOD = Identifier.fromNamespaceAndPath("wanderers_haven", "terrorizing_howl_fear_slow");

	// ── Cooldown / duration constants ─────────────────────────────────────────

	private static final long LUCKY_DODGE_COOLDOWN_TICKS         = 12_000L;
	private static final long SECOND_WIND_MINOR_COOLDOWN_TICKS   = 12_000L;
	private static final long SECOND_WIND_LESSER_COOLDOWN_TICKS  =  9_600L;
	private static final long LAST_STAND_MINOR_COOLDOWN_TICKS    = 12_000L;
	private static final long LAST_STAND_LESSER_COOLDOWN_TICKS   =  9_600L;
	private static final long LAST_STAND_DURATION_TICKS          =   200L;
	private static final long HEAVY_STRIKES_COOLDOWN_TICKS       =   600L;
	private static final long HEAVY_STRIKES_DURATION_TICKS       =   200L;
	private static final long BLEED_DURATION_TICKS               =    60L;
	private static final long BLEED_TICK_INTERVAL_TICKS          =    20L;
	private static final float BLEED_HEALING_MULT                =   0.50f;
	private static final long BATTLE_CRY_WEAK_COOLDOWN_TICKS     =   900L;
	private static final long BATTLE_CRY_WEAK_DURATION_TICKS     =   160L;
	private static final long BLUDGEON_COOLDOWN_TICKS            =   300L;
	private static final long BLUDGEON_DEBUFF_DURATION_TICKS     =   100L;
	private static final long PIERCING_CHARGE_COOLDOWN_TICKS     =   400L;
	private static final long SHADOW_STEP_COOLDOWN_TICKS         = 1_200L;
	private static final long SHADOW_STEP_DURATION_TICKS         =   100L;
	private static final long GROUND_SLAM_SLOW_DURATION_TICKS    =   100L;
	private static final long CRUSHING_LEAP_AIR_WINDOW_TICKS     =    80L;
	private static final long MOCKING_SHOUT_COOLDOWN_TICKS       =   900L;
	private static final long MOCKING_SHOUT_DURATION_TICKS       =   160L;
	private static final long TERRORIZING_HOWL_FEAR_TICKS        =    60L;
	private static final long SPINNING_SLASH_COOLDOWN_TICKS      =   400L;
	private static final long FURY_DECAY_INTERVAL_TICKS          =    20L;
	private static final long OVERCLOCK_SUSTAIN_TICKS            =   100L;
	private static final long SAVAGE_ONSLAUGHT_COOLDOWN_TICKS    =   600L;
	private static final long SAVAGE_ONSLAUGHT_DURATION_TICKS    =   200L;
	private static final long FURY_BECOMES_FLESH_COOLDOWN_TICKS  =   300L;
	private static final long FURY_NOW_BLESSING_DURATION_TICKS   =   200L;
	private static final long FURY_UNLEASHED_COOLDOWN_TICKS      = 18_000L;
	private static final long FURY_UNLEASHED_DURATION_TICKS      = 1_200L;
	private static final long SMITE_COOLDOWN_TICKS               =   400L;
	private static final long CIRCULAR_SLASH_COOLDOWN_TICKS      =   300L;
	private static final long FOCUS_COOLDOWN_TICKS               = 2_400L;
	private static final long FOCUS_DURATION_TICKS               =   600L;
	private static final long AVOID_DEATH_COOLDOWN_TICKS         = 6_000L;
	private static final long PARRY_WINDOW_TICKS                 =    10L;
	private static final long FLASH_STEP_COOLDOWN_TICKS          =   160L;
	private static final long FLASH_STEP_SPEED_DURATION_TICKS    =    60L;
	private static final long DANCE_OF_FALLING_PETALS_COOLDOWN_TICKS = 600L;
	private static final long DANCE_OF_FALLING_PETALS_DURATION_TICKS = 200L;
	private static final long DANCE_OF_FALLING_PETALS_PULSE_INTERVAL_TICKS = 8L;
	private static final long HARMONY_OF_PAIN_COOLDOWN_TICKS     =   900L;
	private static final long HARMONY_OF_PAIN_DURATION_TICKS     =   200L;
	private static final long DANCE_OF_THE_BUTTERFLY_COOLDOWN_TICKS = 400L;
	private static final long DANCE_OF_THE_BUTTERFLY_DURATION_TICKS = 100L;
	private static final long TRIPLE_THRUST_COOLDOWN_TICKS       =   160L;
	private static final long LENGTHENED_STRIKES_COOLDOWN_TICKS  = 1_200L;
	private static final long LENGTHENED_STRIKES_DURATION_TICKS  =   400L;
	private static final long SHIELDS_PROTECTION_COOLDOWN_TICKS  =   140L;
	private static final long SHIELD_BASH_COOLDOWN_TICKS         =   300L;
	private static final long SHIELD_BASH_ARMOR_DEBUFF_TICKS     =   100L;
	private static final int  SHIELD_BASH_STUN_TICKS             =    20;
	private static final long STAND_YOUR_GROUND_WINDOW_TICKS     =   300L;
	private static final int  STAND_YOUR_GROUND_MAX_STACKS       =    12;
	private static final long COMBAT_WINDOW_TICKS                =   120L;
	private static final long DANGERSENSE_WARN_INTERVAL_TICKS    =    20L;
	private static final long WOUND_CLOSURE_HEAL_INTERVAL_TICKS  =    20L;
	private static final long APPRAISAL_INTERVAL_TICKS            =    20L;
	private static final long INTIMIDATING_AURA_DEBUFF_TICKS      =    30L;
	private static final long BATTLEFIELD_HIT_SPEED_TICKS         =    80L;

	private static final String EFX_LAST_STAND = "last_stand";
	private static final String EFX_HEAVY_STRIKES = "heavy_strikes";
	private static final String EFX_MADDENING_STRIKES = "maddening_strikes";
	private static final String EFX_SAVAGE_ONSLAUGHT = "savage_onslaught";
	private static final String EFX_FURY_NOW_BLESSING = "fury_now_blessing";
	private static final String EFX_FURY_UNLEASHED = "fury_unleashed";
	private static final String EFX_FOCUS = "focus";
	private static final String EFX_PARRY_WINDOW = "parry_window";
	private static final String EFX_FLASH_STEP = "flash_step_speed";
	private static final String EFX_FALLING_PETALS = "falling_petals";
	private static final String EFX_BUTTERFLY = "dance_of_butterfly";
	private static final String EFX_SHADOW_STEP = "shadow_step";
	private static final String EFX_LENGTHENED_STRIKES = "lengthened_strikes";
	private static final String EFX_BATTLEFIELD_HIT_SPEED = "battlefield_hit_speed";

	private static final SkillCooldownService COOLDOWNS = new SkillCooldownService();
	private static final TimedEffectTracker EFFECTS = new TimedEffectTracker();
	private static final CombatDebuffService COMBAT_DEBUFFS = new CombatDebuffService();
	private static final EntityAttributeDebuffService ATTRIBUTE_DEBUFFS = new EntityAttributeDebuffService();
	private static final ActiveSkillRegistry ACTIVE_SKILLS = buildActiveSkillRegistry();

	// ── State maps ────────────────────────────────────────────────────────────

	private static final Map<UUID, Float> furyPoints                  = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> furyDecayLastTickAt          = new ConcurrentHashMap<>();
	private static final Map<UUID, Double> furyBlessingCritChanceBonus = new ConcurrentHashMap<>();
	private static final Map<UUID, Double> furyBlessingCritDamageBonus = new ConcurrentHashMap<>();
	private static final Map<UUID, Set<UUID>> danceOfTheButterflyHitEntities = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> crushingLeapPendingLanding   = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> crushingLeapNoFallUntil      = new ConcurrentHashMap<>();
	private static final Map<UUID, Set<UUID>> shadowStepPassedEntities = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> sygStacks                 = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>    sygLastHitAt              = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>    inCombatUntil             = new ConcurrentHashMap<>();
	private static final Map<UUID, Map<UUID, Long>> firstStrikeMarks  = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> dangersenseLastWarnAt         = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> woundClosureLastHealAt        = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> fireTicksLastSeen          = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> criticalRhythmStacks       = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> appraisalLastShownAt          = new ConcurrentHashMap<>();
	private static final Map<UUID, BleedState> bleedingTargets        = new ConcurrentHashMap<>();
	private static final Map<UUID, Map<UUID, Integer>> maddeningArmorStacks = new ConcurrentHashMap<>();
	private static final Map<UUID, GrasscutterFollowup> grasscutterFollowups = new ConcurrentHashMap<>();
	private static final Map<UUID, FearState> fearedEntities          = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> overclockSustainUntil        = new ConcurrentHashMap<>();
	private static final Set<UUID> defyFinalCallUsedInCombat           = ConcurrentHashMap.newKeySet();

	// Stun (Shield Bash)
	private static final Map<UUID, Long> stunnedEntities = new ConcurrentHashMap<>();

	private record BleedState(long expiresAt, long nextTickAt, float perTickDamage) {}
	private record GrasscutterFollowup(long executeAt, float damage) {}
	private record FearState(UUID sourcePlayerId, long fearEndsAt) {}

	// ── Registration ──────────────────────────────────────────────────────────

	/** Register all event listeners. Call during mod initialisation. */
	public static void register() {
		// Apply engine-managed attribute modifiers and sync skills on join
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			ClassSystemBootstrap.statEngine().applyAll(player);
			WanderersHavenNetworking.sendSkillsTo(player);
		});

		// Damage event: increment SYG stacks, trigger Second Wind / Last Stand
		ServerLivingEntityEvents.AFTER_DAMAGE.register(
			(entity, source, baseDamage, actualDamage, blocked) -> {
				if (!(entity instanceof ServerPlayer player)) return;
				incrementSygStack(player);
				Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
				if (actualDamage > 0.0f && skills.contains("pain_fuels_me")) {
					addFury(player, actualDamage * 0.15f, skills);
				}
				float ratio = player.getHealth() / player.getMaxHealth();
				if (ratio < 0.4f) {
					trySecondWind(player);
					tryActivateLastStand(player);
				}
			}
		);

		// Reap Life — heal 15% missing health on kill
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, attacker, killedEntity, damageSource) -> {
			if (!(attacker instanceof ServerPlayer player)) return;
			applyReapLifeOnKill(player);
		});

		// PlayerStatEngine — single tick that manages all attribute contributions
		ServerTickEvents.END_SERVER_TICK.register(server ->
			ClassSystemBootstrap.statEngine().tick(server));

		// Last Stand — expire buff after 10 seconds
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<UUID, TimedEffectTracker.Window> entry : EFFECTS.snapshot(EFX_LAST_STAND).entrySet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					ClassSystemBootstrap.statEngine().deactivateSource(entry.getKey(), "last_stand_buff", null);
					EFFECTS.clear(entry.getKey(), EFX_LAST_STAND);
					continue;
				}
				if (player.level().getGameTime() >= entry.getValue().expiresAt()) {
					removeLastStand(player);
					EFFECTS.clear(player.getUUID(), EFX_LAST_STAND);
				}
			}
		});

		// Heavy Strikes — expire buff after 10 seconds
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<UUID, TimedEffectTracker.Window> entry : EFFECTS.snapshot(EFX_HEAVY_STRIKES).entrySet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					ClassSystemBootstrap.statEngine().deactivateSource(entry.getKey(), "heavy_strikes_buff", null);
					EFFECTS.clear(entry.getKey(), EFX_HEAVY_STRIKES);
					continue;
				}
				if (player.level().getGameTime() >= entry.getValue().expiresAt()) {
					removeHeavyStrikes(player);
					EFFECTS.clear(player.getUUID(), EFX_HEAVY_STRIKES);
				}
			}
		});

		// Maddening Strikes — expire buff and clear per-target armor stacks
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<UUID, TimedEffectTracker.Window> entry : EFFECTS.snapshot(EFX_MADDENING_STRIKES).entrySet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					ClassSystemBootstrap.statEngine().deactivateSource(entry.getKey(), "maddening_strikes_buff", null);
					maddeningArmorStacks.remove(entry.getKey());
					EFFECTS.clear(entry.getKey(), EFX_MADDENING_STRIKES);
					continue;
				}
				if (player.level().getGameTime() >= entry.getValue().expiresAt()) {
					ClassSystemBootstrap.statEngine().deactivateSource(player.getUUID(), "maddening_strikes_buff", player);
					maddeningArmorStacks.remove(player.getUUID());
					player.sendSystemMessage(Component.literal(
						"\u00a77[Wanderers Haven]\u00a7r Maddening Strikes faded."));
					EFFECTS.clear(player.getUUID(), EFX_MADDENING_STRIKES);
				}
			}
		});

		// Bleed — periodic damage ticks and expiry
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			bleedingTargets.entrySet().removeIf(entry -> {
				UUID targetId = entry.getKey();
				BleedState state = entry.getValue();
				for (ServerLevel level : server.getAllLevels()) {
					net.minecraft.world.entity.Entity entity = level.getEntity(targetId);
					if (!(entity instanceof LivingEntity living)) continue;
					long now = level.getGameTime();
					if (now >= state.expiresAt()) return true;
					if (now >= state.nextTickAt()) {
						living.hurt(level.damageSources().magic(), state.perTickDamage());
						entry.setValue(new BleedState(state.expiresAt(), now + BLEED_TICK_INTERVAL_TICKS, state.perTickDamage()));
					}
					return false;
				}
				return true;
			});
		});

		// Grasscutter — delayed follow-up slash after short dash travel
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			grasscutterFollowups.entrySet().removeIf(entry -> {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) return true;
				long now = player.level().getGameTime();
				GrasscutterFollowup followup = entry.getValue();
				if (now < followup.executeAt()) return false;
				int hit = grasscutterSlash(player, followup.damage());
				player.sendSystemMessage(Component.literal(
					"\u00a7c[Wanderers Haven]\u00a7r Grasscutter follow-up slash!"
					+ (hit > 0 ? " Hit " + hit + " enem" + (hit == 1 ? "y" : "ies") + "." : "")));
				return true;
			});
		});

		// Timed entity-attribute debuffs (armor slow etc.)
		ServerTickEvents.END_SERVER_TICK.register(ATTRIBUTE_DEBUFFS::tick);

		// Crushing Leap — trigger landing slam and cleanup no-fall windows
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			crushingLeapPendingLanding.entrySet().removeIf(entry -> {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					crushingLeapNoFallUntil.remove(entry.getKey());
					return true;
				}
				long now = player.level().getGameTime();
				if (now >= entry.getValue()) {
					return true;
				}
				if (!player.onGround()) {
					return false;
				}
				crushingLeapLand(player);
				return true;
			});
			crushingLeapNoFallUntil.entrySet().removeIf(entry -> {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) return true;
				return player.level().getGameTime() >= entry.getValue();
			});
		});

		// Savage Onslaught — expire offensive buff after 10 seconds
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<UUID, TimedEffectTracker.Window> entry : EFFECTS.snapshot(EFX_SAVAGE_ONSLAUGHT).entrySet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					ClassSystemBootstrap.statEngine().deactivateSource(entry.getKey(), "savage_onslaught_buff", null);
					EFFECTS.clear(entry.getKey(), EFX_SAVAGE_ONSLAUGHT);
					continue;
				}
				if (player.level().getGameTime() >= entry.getValue().expiresAt()) {
					ClassSystemBootstrap.statEngine().deactivateSource(player.getUUID(), "savage_onslaught_buff", player);
					player.sendSystemMessage(Component.literal(
						"\u00a77[Wanderers Haven]\u00a7r Savage Onslaught faded."));
					EFFECTS.clear(player.getUUID(), EFX_SAVAGE_ONSLAUGHT);
				}
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<UUID, TimedEffectTracker.Window> entry : EFFECTS.snapshot(EFX_FURY_UNLEASHED).entrySet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					ClassSystemBootstrap.statEngine().deactivateSource(entry.getKey(), "fury_unleashed_buff", null);
					EFFECTS.clear(entry.getKey(), EFX_FURY_UNLEASHED);
					continue;
				}
				if (player.level().getGameTime() >= entry.getValue().expiresAt()) {
					ClassSystemBootstrap.statEngine().deactivateSource(player.getUUID(), "fury_unleashed_buff", player);
					player.sendSystemMessage(Component.literal(
						"\u00a77[Wanderers Haven]\u00a7r Fury Unleashed faded."));
					EFFECTS.clear(player.getUUID(), EFX_FURY_UNLEASHED);
				}
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<UUID, TimedEffectTracker.Window> entry : EFFECTS.snapshot(EFX_FURY_NOW_BLESSING).entrySet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					furyBlessingCritChanceBonus.remove(entry.getKey());
					furyBlessingCritDamageBonus.remove(entry.getKey());
					EFFECTS.clear(entry.getKey(), EFX_FURY_NOW_BLESSING);
					continue;
				}
				if (player.level().getGameTime() >= entry.getValue().expiresAt()) {
					furyBlessingCritChanceBonus.remove(player.getUUID());
					furyBlessingCritDamageBonus.remove(player.getUUID());
					EFFECTS.clear(player.getUUID(), EFX_FURY_NOW_BLESSING);
				}
			}
		});

		// Flash Step — expire speed buff
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<UUID, TimedEffectTracker.Window> entry : EFFECTS.snapshot(EFX_FLASH_STEP).entrySet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					ClassSystemBootstrap.statEngine().deactivateSource(entry.getKey(), "flash_step_buff", null);
					EFFECTS.clear(entry.getKey(), EFX_FLASH_STEP);
					continue;
				}
				if (player.level().getGameTime() >= entry.getValue().expiresAt()) {
					ClassSystemBootstrap.statEngine().deactivateSource(player.getUUID(), "flash_step_buff", player);
					EFFECTS.clear(player.getUUID(), EFX_FLASH_STEP);
				}
			}
		});

		// Battlefield mobility hit speed — expire +15% speed window
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<UUID, TimedEffectTracker.Window> entry : EFFECTS.snapshot(EFX_BATTLEFIELD_HIT_SPEED).entrySet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					ClassSystemBootstrap.statEngine().deactivateSource(entry.getKey(), "battlefield_mobility_attack_buff", null);
					EFFECTS.clear(entry.getKey(), EFX_BATTLEFIELD_HIT_SPEED);
					continue;
				}
				if (player.level().getGameTime() >= entry.getValue().expiresAt()) {
					ClassSystemBootstrap.statEngine().deactivateSource(player.getUUID(), "battlefield_mobility_attack_buff", player);
					EFFECTS.clear(player.getUUID(), EFX_BATTLEFIELD_HIT_SPEED);
				}
			}
		});

		// Focus — expire damage amp and dodge chance after 30 seconds
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<UUID, TimedEffectTracker.Window> entry : EFFECTS.snapshot(EFX_FOCUS).entrySet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					ClassSystemBootstrap.statEngine().deactivateSource(entry.getKey(), "focus_buff", null);
					EFFECTS.clear(entry.getKey(), EFX_FOCUS);
					continue;
				}
				if (player.level().getGameTime() >= entry.getValue().expiresAt()) {
					ClassSystemBootstrap.statEngine().deactivateSource(player.getUUID(), "focus_buff", player);
					player.sendSystemMessage(Component.literal(
						"\u00a77[Wanderers Haven]\u00a7r Focus faded."));
					EFFECTS.clear(player.getUUID(), EFX_FOCUS);
				}
			}
		});

		// Lengthened Strikes — expire temporary range bonus and distance scaling window
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<UUID, TimedEffectTracker.Window> entry : EFFECTS.snapshot(EFX_LENGTHENED_STRIKES).entrySet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					ClassSystemBootstrap.statEngine().deactivateSource(entry.getKey(), "lengthened_strikes_buff", null);
					EFFECTS.clear(entry.getKey(), EFX_LENGTHENED_STRIKES);
					continue;
				}
				if (player.level().getGameTime() >= entry.getValue().expiresAt()) {
					ClassSystemBootstrap.statEngine().deactivateSource(player.getUUID(), "lengthened_strikes_buff", player);
					player.sendSystemMessage(Component.literal(
						"\u00a77[Wanderers Haven]\u00a7r Spear: Lengthened Strikes faded."));
					EFFECTS.clear(player.getUUID(), EFX_LENGTHENED_STRIKES);
				}
			}
		});

		// Dance of Falling Petals — pulse damage 2.5x/sec for 10 seconds
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<UUID, TimedEffectTracker.Window> entry : EFFECTS.snapshot(EFX_FALLING_PETALS).entrySet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					EFFECTS.clear(entry.getKey(), EFX_FALLING_PETALS);
					continue;
				}
				long now = player.level().getGameTime();
				if (now >= entry.getValue().expiresAt()) {
					player.sendSystemMessage(Component.literal(
						"\u00a77[Wanderers Haven]\u00a7r Dance of Falling Petals faded."));
					EFFECTS.clear(player.getUUID(), EFX_FALLING_PETALS);
					continue;
				}
				long elapsed = now - entry.getValue().startedAt();
				if (elapsed % DANCE_OF_FALLING_PETALS_PULSE_INTERVAL_TICKS == 0L) {
					danceOfFallingPetalsPulse(player);
				}
			}
		});


		// Dance of the Butterfly — expire movement speed buff and damage window
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<UUID, TimedEffectTracker.Window> entry : EFFECTS.snapshot(EFX_BUTTERFLY).entrySet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					ClassSystemBootstrap.statEngine().deactivateSource(entry.getKey(), "dance_of_butterfly_buff", null);
					danceOfTheButterflyHitEntities.remove(entry.getKey());
					EFFECTS.clear(entry.getKey(), EFX_BUTTERFLY);
					continue;
				}
				if (player.level().getGameTime() >= entry.getValue().expiresAt()) {
					ClassSystemBootstrap.statEngine().deactivateSource(player.getUUID(), "dance_of_butterfly_buff", player);
					danceOfTheButterflyHitEntities.remove(player.getUUID());
					player.sendSystemMessage(Component.literal(
						"\u00a77[Wanderers Haven]\u00a7r Dance of the Butterfly faded."));
					EFFECTS.clear(player.getUUID(), EFX_BUTTERFLY);
					continue;
				}
				danceOfTheButterflyTickDamage(player);
			}
		});

		// Shadow Step — track pass-through targets, then burst at expiry
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (Map.Entry<UUID, TimedEffectTracker.Window> entry : EFFECTS.snapshot(EFX_SHADOW_STEP).entrySet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					ClassSystemBootstrap.statEngine().deactivateSource(entry.getKey(), "shadow_step_buff", null);
					shadowStepPassedEntities.remove(entry.getKey());
					EFFECTS.clear(entry.getKey(), EFX_SHADOW_STEP);
					continue;
				}

				Set<UUID> passed = shadowStepPassedEntities.computeIfAbsent(player.getUUID(), id -> ConcurrentHashMap.newKeySet());
				for (LivingEntity touched : nearbyEnemies(player, 0.8)) {
					passed.add(touched.getUUID());
				}

				if (player.level().getGameTime() < entry.getValue().expiresAt()) {
					continue;
				}

				ClassSystemBootstrap.statEngine().deactivateSource(player.getUUID(), "shadow_step_buff", player);
				EFFECTS.clear(player.getUUID(), EFX_SHADOW_STEP);
				shadowStepBurst(player, passed);
				shadowStepPassedEntities.remove(player.getUUID());
			}
		});

		// Stand Your Ground — expire hit stacks after 15 seconds of no damage
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (!ClassSystemBootstrap.skillEngine()
						.ownedSkillIds(player.getUUID(), "warrior")
						.contains("stand_your_ground")) continue;
				Long lastHit = sygLastHitAt.get(player.getUUID());
				if (lastHit == null) continue;
				if (player.level().getGameTime() - lastHit >= STAND_YOUR_GROUND_WINDOW_TICKS) {
					sygStacks.remove(player.getUUID());
					sygLastHitAt.remove(player.getUUID());
				}
			}
		});

		// Base warrior combat-passive loop
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				handleFuryDecay(player);
				handleDangersense(player);
				handleLightfootedJump(player);
				handleWoundClosure(player);
				handleFireResistance(player);
				handleLesserAppraisal(player);
				handleIntimidatingAura(player);
			}
			pruneCombatState(server);
		});

		// Entity stun (Shield Bash)
		registerStunTick();

		// Terrorizing Howl fear phase (flee, then transition into taunt/debuff phase)
		ServerTickEvents.END_SERVER_TICK.register(server ->
			fearedEntities.entrySet().removeIf(entry -> {
				for (ServerLevel level : server.getAllLevels()) {
					net.minecraft.world.entity.Entity raw = level.getEntity(entry.getKey());
					if (!(raw instanceof LivingEntity enemy)) continue;
					FearState state = entry.getValue();
					long now = level.getGameTime();
					if (now < state.fearEndsAt()) {
						if (enemy instanceof Mob mob) {
							net.minecraft.world.entity.Entity src = level.getEntity(state.sourcePlayerId());
							if (src instanceof ServerPlayer source) {
								Vec3 away = mob.position().subtract(source.position());
								if (away.lengthSqr() < 0.0001) away = source.getLookAngle().scale(-1.0);
								Vec3 flee = mob.position().add(away.normalize().scale(6.0));
								mob.setTarget(null);
								mob.getNavigation().moveTo(flee.x, mob.getY(), flee.z, 1.25);
							}
						}
						return false;
					}

					net.minecraft.world.entity.Entity src = level.getEntity(state.sourcePlayerId());
					if (src instanceof ServerPlayer source && source.level() == level) {
						long expiresAt = now + MOCKING_SHOUT_DURATION_TICKS;
						COMBAT_DEBUFFS.apply(enemy.getUUID(), expiresAt, 0.70f, 1.20f);
						ATTRIBUTE_DEBUFFS.apply(
							enemy,
							Attributes.MOVEMENT_SPEED,
							MOCKING_SHOUT_SLOW_MOD,
							-0.20,
							AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
							expiresAt
						);
						if (enemy instanceof Mob mob) {
							mob.setTarget(source);
						}
					}
					return true;
				}
				return true;
			})
		);
	}

	// ── Apply on join / skill grant ───────────────────────────────────────────

	/** Re-apply all engine-managed stat contributions for a player (called on join or skill grant). */
	public static void applyAll(ServerPlayer player) {
		ClassSystemBootstrap.statEngine().applyAll(player);
	}

	/**
	 * Apply a single skill's effects immediately after it is granted.
	 * Attribute contributions are handled by the engine; this triggers an
	 * immediate refresh so the player feels the change without waiting a tick.
	 */
	public static void applySkill(ServerPlayer player, String skillId) {
		ClassSystemBootstrap.statEngine().applyAll(player);
	}

	public static void executeActiveSkill(ServerPlayer player, String skillId) {
		ACTIVE_SKILLS.execute(player, skillId);
	}

	private static ActiveSkillRegistry buildActiveSkillRegistry() {
		ActiveSkillRegistry registry = new ActiveSkillRegistry();
		registry.register("heavy_strikes", SkillEffectService::executeHeavyStrikes);
		registry.register("maddening_strikes", SkillEffectService::executeMaddeningStrikes);
		registry.register("battle_cry_weak", SkillEffectService::executeBattleCryWeak);
		registry.register("mocking_shout", SkillEffectService::executeMockingShout);
		registry.register("terrorizing_howl", SkillEffectService::executeTerrorizingHowl);
		registry.register("bludgeon", SkillEffectService::executeBludgeon);
		registry.register("fury_becomes_flesh", SkillEffectService::executeFuryBecomesFlesh);
		registry.register("fury_now_blessing", SkillEffectService::executeFuryNowBlessing);
		registry.register("ground_slam", SkillEffectService::executeGroundSlam);
		registry.register("piercing_charge", SkillEffectService::executePiercingCharge);
		registry.register("spinning_slash", SkillEffectService::executeSpinningSlash);
		registry.register("raging_cyclone", SkillEffectService::executeRagingCyclone);
		registry.register("savage_onslaught", SkillEffectService::executeSavageOnslaught);
		registry.register("massacre", SkillEffectService::executeMassacre);
		registry.register("fury_unleashed", SkillEffectService::executeFuryUnleashed);
		registry.register("grasscutter", SkillEffectService::executeGrasscutter);
		registry.register("crushing_leap", SkillEffectService::executeCrushingLeap);
		registry.register("shadow_step", SkillEffectService::executeShadowStep);
		registry.register("smite", SkillEffectService::executeSmite);
		registry.register("shield_bash", SkillEffectService::executeShieldBash);
		registry.register("parry", SkillEffectService::executeParry);
		registry.register("flash_step", SkillEffectService::executeFlashStep);
		registry.register("dance_of_falling_petals", SkillEffectService::executeDanceOfFallingPetals);
		registry.register("harmony_of_pain", SkillEffectService::executeHarmonyOfPain);
		registry.register("dance_of_the_butterfly", SkillEffectService::executeDanceOfTheButterfly);
		registry.register("circular_slash", SkillEffectService::executeCircularSlash);
		registry.register("focus", SkillEffectService::executeFocus);
		registry.register("triple_thrust", SkillEffectService::executeTripleThrust);
		registry.register("lengthened_strikes", SkillEffectService::executeLengthenedStrikes);
		return registry;
	}

	// ── Mixin helpers ─────────────────────────────────────────────────────────

	/**
	 * Returns a composite incoming damage multiplier for damage-reduction skills.
	 * Stacks multiplicatively.
	 */
	public static float getDamageMultiplier(ServerPlayer player, DamageSource source) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		float mult = 1.0f;
		if      (skills.contains("skin_of_adamantium")) mult *= 0.75f;
		else if (skills.contains("diamond_skin"))       mult *= 0.80f;
		else if (skills.contains("iron_skin"))          mult *= 0.88f;
		else if (skills.contains("tough_skin"))         mult *= 0.95f;
		if (source != null
				&& source.getDirectEntity() instanceof AbstractArrow
				&& (skills.contains("iron_skin") || skills.contains("diamond_skin") || skills.contains("skin_of_adamantium"))) {
			mult *= 0.90f;
		}
		if      (skills.contains("mythic_endurance"))    mult *= 0.32f;
		else if (skills.contains("legendary_endurance")) mult *= 0.47f;
		else if (skills.contains("immense_endurance"))  mult *= 0.57f;
		else if (skills.contains("greater_endurance"))  mult *= 0.72f;
		else if (skills.contains("enhanced_endurance")) mult *= 0.82f;
		else if (skills.contains("lesser_endurance"))   mult *= 0.90f;
		if (skills.contains("skin_of_adamantium") && isMagicDamage(source)) {
			mult *= 0.60f;
		}
		if (source != null && source.is(DamageTypes.FALL)) {
			Long noFallUntil = crushingLeapNoFallUntil.get(player.getUUID());
			if (noFallUntil != null && player.level().getGameTime() <= noFallUntil) {
				crushingLeapNoFallUntil.remove(player.getUUID());
				return 0.0f;
			}
		}
		if (hasOnslaughtLineSkill(skills)
				&& EFFECTS.isActive(player.getUUID(), EFX_SAVAGE_ONSLAUGHT, player.level().getGameTime())) {
			mult *= 1.30f;
		}
		if (skills.contains("last_breath") && player.getMaxHealth() > 0.0f
				&& (player.getHealth() / player.getMaxHealth()) <= 0.15f) {
			mult *= 0.60f;
		}
		// Aura of Righteousness — 15% DR while a paladin is within 17 blocks
		if (hasPaladinAuraNearby(player)) {
			mult *= 0.85f;
		}
		// Stand Your Ground — up to 60% DR from stacked hits within the 15-sec window
		if (skills.contains("stand_your_ground")) {
			int stacks = getSygStacks(player);
			if (stacks > 0) {
				mult *= (1.0f - Math.min(stacks * 0.05f, 0.60f));
			}
		}
		return mult;
	}

	public static boolean isShadowStepActive(ServerPlayer player) {
		return EFFECTS.isActive(player.getUUID(), EFX_SHADOW_STEP, player.level().getGameTime());
	}

	public static float getOutgoingDamageMultiplier(ServerPlayer attacker) {
		return isShadowStepActive(attacker) ? 0.0f : 1.0f;
	}

	public static void applyReapLifeOnKill(ServerPlayer attacker) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(attacker.getUUID(), "warrior");
		if (!skills.contains("reap_life")) return;
		float missing = attacker.getMaxHealth() - attacker.getHealth();
		if (missing <= 0.0f) return;
		attacker.heal(missing * 0.15f);
	}

	public static void applyReapLifeLifesteal(ServerPlayer attacker, float dealtDamage, boolean applied) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(attacker.getUUID(), "warrior");
		if (!applied || dealtDamage <= 0.0f) return;
		if (!skills.contains("massacre")) return;
		if (!EFFECTS.isActive(attacker.getUUID(), EFX_SAVAGE_ONSLAUGHT, attacker.level().getGameTime())) return;
		attacker.heal(dealtDamage * 0.10f);
	}

	/** True if the player has Tough Skin or Iron Skin (cactus immunity). */
	public static boolean hasCactusImmunity(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		return skills.contains("tough_skin")
			|| skills.contains("iron_skin")
			|| skills.contains("diamond_skin")
			|| skills.contains("skin_of_adamantium");
	}

	/** Returns true and starts the 10-minute cooldown if Lucky Dodge fires. */
	public static boolean tryLuckyDodge(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!skills.contains("lucky_dodge") ) return false;
		long now = player.level().getGameTime();
		if (!COOLDOWNS.isReady(player.getUUID(), "lucky_dodge", now, LUCKY_DODGE_COOLDOWN_TICKS)) return false;
		COOLDOWNS.start(player.getUUID(), "lucky_dodge", now);
		return true;
	}

	/** Duration multiplier for poison effects (0.80 for Lesser, 0.90 for Minor, 1.0 otherwise). */
	public static float getPoisonDurationMultiplier(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (skills.contains("poison_immunity")) return 0.0f;
		if (skills.contains("enhanced_poison_resistance")) return 0.80f;
		if (skills.contains("lesser_poison_resistance")) return 0.80f;
		if (skills.contains("minor_poison_resistance"))  return 0.90f;
		return 1.0f;
	}

	/** Duration multiplier for magic debuffs (Weakness, Nausea, Slowness). */
	public static float getMagicDebuffDurationMultiplier(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (skills.contains("divine_resistance_magic")) return 0.49f;
		if (skills.contains("titanic_resistance_magic")) return 0.64f;
		if (skills.contains("greater_resistance_magic")) return 0.72f;
		if (skills.contains("enhanced_resistance_magic")) return 0.82f;
		if (skills.contains("lesser_resistance_magic")) return 0.90f;
		return 1.0f;
	}

	public static float getSlowAndStunDurationMultiplier(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		return skills.contains("indomitable") ? 0.50f : 1.0f;
	}

	/** Incoming damage multiplier from Greater Dangersense and weapon-type resistances. */
	public static float getDamageTypeResistanceMultiplier(ServerPlayer player, DamageSource source) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		float mult = 1.0f;

		if (skills.contains("perception_nothing_slips_my_grasp") && isBehindAttack(player, source)) {
			mult *= 0.50f;
		} else if (skills.contains("greater_dangersense") && isBehindAttack(player, source)) {
			mult *= 0.80f;
		}

		if (skills.contains("divine_resistance_elements") && isElementalDamage(source)) {
			mult *= 0.57f;
		} else if (skills.contains("titanic_resistance_elements") && isElementalDamage(source)) {
			mult *= 0.72f;
		} else if (skills.contains("greater_resistance_elements") && isElementalDamage(source)) {
			mult *= 0.80f;
		} else if (skills.contains("lesser_resistance_elements") && isElementalDamage(source)) {
			mult *= 0.90f;
		}

		String category = incomingWeaponCategory(source);
		if (skills.contains("divine_resistance_piercing") && "piercing".equals(category)) {
			mult *= 0.49f;
		} else if (skills.contains("titanic_resistance_piercing") && "piercing".equals(category)) {
			mult *= 0.64f;
		} else if (skills.contains("greater_resistance_piercing") && "piercing".equals(category)) {
			mult *= 0.72f;
		} else if (skills.contains("enhanced_resistance_piercing") && "piercing".equals(category)) {
			mult *= 0.82f;
		} else if (skills.contains("lesser_resistance_piercing") && "piercing".equals(category)) {
			mult *= 0.90f;
		}
		if (skills.contains("divine_resistance_bludgeoning") && "bludgeoning".equals(category)) {
			mult *= 0.49f;
		} else if (skills.contains("titanic_resistance_bludgeoning") && "bludgeoning".equals(category)) {
			mult *= 0.64f;
		} else if (skills.contains("greater_resistance_bludgeoning") && "bludgeoning".equals(category)) {
			mult *= 0.72f;
		} else if (skills.contains("enhanced_resistance_bludgeoning") && "bludgeoning".equals(category)) {
			mult *= 0.82f;
		} else if (skills.contains("lesser_resistance_bludgeoning") && "bludgeoning".equals(category)) {
			mult *= 0.90f;
		}
		if (skills.contains("divine_resistance_blades") && "blades".equals(category)) {
			mult *= 0.49f;
		} else if (skills.contains("titanic_resistance_blades") && "blades".equals(category)) {
			mult *= 0.64f;
		} else if (skills.contains("greater_resistance_blades") && "blades".equals(category)) {
			mult *= 0.72f;
		} else if (skills.contains("enhanced_resistance_blades") && "blades".equals(category)) {
			mult *= 0.82f;
		} else if (skills.contains("lesser_resistance_blades") && "blades".equals(category)) {
			mult *= 0.90f;
		}
		return mult;
	}

	/** Attacker-side multipliers for First Strike, crits, and Measured Strikes. */
	public static float getAttackerSkillDamageMultiplier(ServerPlayer attacker, LivingEntity target, DamageSource source, float preArmorAmount) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(attacker.getUUID(), "warrior");
		float mult = 1.0f;
		long now = attacker.level().getGameTime();
		double blessingChance = 0.0;
		double blessingCritDamage = 0.0;
		if (EFFECTS.isActive(attacker.getUUID(), EFX_FURY_NOW_BLESSING, now)) {
			blessingChance = furyBlessingCritChanceBonus.getOrDefault(attacker.getUUID(), 0.0);
			blessingCritDamage = furyBlessingCritDamageBonus.getOrDefault(attacker.getUUID(), 0.0);
		}

		if (skills.contains("the_first_blow_decides_it") && isFirstStrike(attacker, target, now)) {
			mult *= 3.00f;
		} else if (skills.contains("greater_first_strike") && isFirstStrike(attacker, target, now)) {
			mult *= 1.45f;
		} else if (skills.contains("first_strike") && isFirstStrike(attacker, target, now)) {
			mult *= 1.30f;
		}

		if (skills.contains("perfect_appraisal")) {
			int attackerLevel = appraisalLevel(attacker);
			int targetLevel = appraisalLevel(target);
			if (attackerLevel >= targetLevel + 3) {
				mult *= 1.30f;
			}
		}

		boolean critTriggered = false;
		if (skills.contains("the_universe_it_sings_for_me")) {
			int stacks = Math.min(criticalRhythmStacks.getOrDefault(attacker.getUUID(), 0), 16);
			float chance = Math.min(0.80f, stacks * 0.05f);
			if (attacker.level().random.nextFloat() < chance) {
				mult *= 2.50f;
				critTriggered = true;
			}
		} else if (skills.contains("greater_critical_rhythm")) {
			int stacks = Math.min(criticalRhythmStacks.getOrDefault(attacker.getUUID(), 0), 20);
			float chance = Math.min(0.30f, 0.10f + (stacks * 0.01f));
			if (attacker.level().random.nextFloat() < chance) {
				mult *= 1.50f;
				critTriggered = true;
			}
		} else if (skills.contains("critical_rhythm")) {
			int stacks = Math.min(criticalRhythmStacks.getOrDefault(attacker.getUUID(), 0), 5);
			float chance = 0.10f + (stacks * 0.02f);
			if (attacker.level().random.nextFloat() < chance) {
				mult *= 1.50f;
				critTriggered = true;
			}
		} else if (skills.contains("critical_hits") && attacker.level().random.nextFloat() < 0.10f) {
			mult *= 1.50f;
			critTriggered = true;
		}

		if (!critTriggered && hasFuryEngine(skills)) {
			float fury = furyPoints.getOrDefault(attacker.getUUID(), 0.0f);
			float chance = (float) Math.min(1.0, Math.min(0.30f, fury * 0.003f) + blessingChance);
			if (attacker.level().random.nextFloat() < chance) {
				mult *= 1.50f;
				critTriggered = true;
			}
		}

		if (!critTriggered && blessingChance > 0.0 && attacker.level().random.nextDouble() < blessingChance) {
			mult *= 1.50f;
			critTriggered = true;
		}

		if (critTriggered && blessingCritDamage > 0.0) {
			mult *= (float) (1.0 + blessingCritDamage);
		}

		if (skills.contains("killing_blow") && target.getMaxHealth() > 0.0f
				&& (target.getHealth() / target.getMaxHealth()) <= 0.20f) {
			mult *= 1.30f;
		}

		if (skills.contains("my_strikes_stop_for_nothing") && attacker.level().random.nextFloat() < 0.15f) {
			mult *= measuredStrikeMultiplier(target, source, preArmorAmount, 0.50f, 0.50f);
		} else if (skills.contains("precise_strikes") && attacker.level().random.nextFloat() < 0.15f) {
			mult *= measuredStrikeMultiplier(target, source, preArmorAmount, 0.30f, 0.15f);
		} else if (skills.contains("measured_strikes") && attacker.level().random.nextFloat() < 0.10f) {
			mult *= measuredStrikeMultiplier(target, source, preArmorAmount, 0.20f, 0.0f);
		}

		if (hasOnslaughtLineSkill(skills)
				&& EFFECTS.isActive(attacker.getUUID(), EFX_SAVAGE_ONSLAUGHT, attacker.level().getGameTime())) {
			float bypass = skills.contains("massacre") ? 0.25f : 0.15f;
			mult *= measuredStrikeMultiplier(target, source, preArmorAmount, bypass, bypass);
		}

		return mult;
	}

	public static void markInCombat(ServerPlayer player) {
		markInCombat(player.getUUID(), player.level().getGameTime());
	}

	public static void markInCombat(UUID playerId, long gameTime) {
		inCombatUntil.put(playerId, gameTime + COMBAT_WINDOW_TICKS);
	}

	public static void recordSuccessfulHit(ServerPlayer attacker) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(attacker.getUUID(), "warrior");
		addBattleFuryStack(attacker);
		if (skills.contains("critical_rhythm") || skills.contains("greater_critical_rhythm") || skills.contains("the_universe_it_sings_for_me")) {
			int current = criticalRhythmStacks.getOrDefault(attacker.getUUID(), 0);
			int maxStacks;
			if (skills.contains("the_universe_it_sings_for_me")) maxStacks = 16;
			else if (skills.contains("greater_critical_rhythm")) maxStacks = 20;
			else maxStacks = 5;
			criticalRhythmStacks.put(attacker.getUUID(), Math.min(maxStacks, current + 1));
		}
		if (skills.contains("battlefield_unmatched_mobility")) {
			long now = attacker.level().getGameTime();
			EFFECTS.start(attacker.getUUID(), EFX_BATTLEFIELD_HIT_SPEED, now, BATTLEFIELD_HIT_SPEED_TICKS);
			ClassSystemBootstrap.statEngine().activateSource(attacker.getUUID(), "battlefield_mobility_attack_buff");
		}
	}

	public static boolean isInCombat(ServerPlayer player) {
		Long until = inCombatUntil.get(player.getUUID());
		return until != null && player.level().getGameTime() <= until;
	}

	public static boolean tryAvoidDeath(ServerPlayer player, float incomingDamage) {
		if (incomingDamage < player.getHealth()) return false;
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!skills.contains("avoid_death") && !skills.contains("defy_the_final_call")) return false;
		long now = player.level().getGameTime();
		if (!isInCombat(player)) return false;
		if (skills.contains("defy_the_final_call")) {
			if (defyFinalCallUsedInCombat.contains(player.getUUID())) return false;
			defyFinalCallUsedInCombat.add(player.getUUID());
			float before = player.getHealth();
			float healed = Math.max(1.0f, player.getMaxHealth() - before);
			player.setHealth(player.getMaxHealth());
			triggerDefyFinalCallShockwave(player, healed);
			player.sendSystemMessage(Component.literal(
				"\u00a76[Wanderers Haven]\u00a7r Defy the Final Call! Death denied and your shockwave devastates nearby enemies."
			));
			return true;
		}

		if (!COOLDOWNS.isReady(player.getUUID(), "avoid_death", now, AVOID_DEATH_COOLDOWN_TICKS)) return false;
		COOLDOWNS.start(player.getUUID(), "avoid_death", now);
		float restored = Math.max(1.0f, player.getMaxHealth() * 0.20f);
		player.setHealth(Math.min(player.getMaxHealth(), restored));
		player.sendSystemMessage(Component.literal(
			"\u00a76[Wanderers Haven]\u00a7r Avoid Death triggered! Restored 20% health. (5 min cooldown)"
		));
		return true;
	}

	public static void tryDiamondSkinReflect(ServerPlayer player, DamageSource source, float takenDamage) {
		if (takenDamage <= 0.0f) return;
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!skills.contains("diamond_skin") && !skills.contains("skin_of_adamantium")) return;
		if (player.level().random.nextFloat() >= 0.10f) return;
		net.minecraft.world.entity.Entity attacker = source.getEntity();
		if (!(attacker instanceof LivingEntity living)) return;
		if (living == player) return;
		float reflected = takenDamage * 0.20f;
		if (reflected <= 0.0f) return;
		living.hurt(player.damageSources().thorns(player), reflected);
	}

	/** True if the player has Slow Metabolism. */
	public static boolean hasSlowMetabolism(ServerPlayer player) {
		return ClassSystemBootstrap.skillEngine()
			.ownedSkillIds(player.getUUID(), "warrior")
			.contains("slow_metabolism");
	}

	// ── Second Wind ───────────────────────────────────────────────────────────

	private static void trySecondWind(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		boolean hasLesser = skills.contains("second_wind_lesser");
		boolean hasMinor  = skills.contains("second_wind_minor");
		if (!hasLesser && !hasMinor) return;

		long now = player.level().getGameTime();
		long cooldown = hasLesser ? SECOND_WIND_LESSER_COOLDOWN_TICKS : SECOND_WIND_MINOR_COOLDOWN_TICKS;
		if (!COOLDOWNS.isReady(player.getUUID(), "second_wind", now, cooldown)) return;
		COOLDOWNS.start(player.getUUID(), "second_wind", now);
		if (hasLesser) {
			player.heal(9.0f);
			player.sendSystemMessage(Component.literal(
				"\u00a76[Wanderers Haven]\u00a7r Second Wind! Recovered 4.5 hearts. (8 min cooldown)"));
		} else {
			player.heal(6.0f);
			player.sendSystemMessage(Component.literal(
				"\u00a76[Wanderers Haven]\u00a7r Second Wind! Recovered 3 hearts. (10 min cooldown)"));
		}
	}

	// ── Last Stand ────────────────────────────────────────────────────────────

	private static void tryActivateLastStand(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		boolean hasLesser = skills.contains("last_stand_lesser");
		boolean hasMinor  = skills.contains("last_stand_minor");
		if (!hasLesser && !hasMinor) return;
		if (EFFECTS.isActive(player.getUUID(), EFX_LAST_STAND, player.level().getGameTime())) return;

		long now = player.level().getGameTime();
		long cooldown = hasLesser ? LAST_STAND_LESSER_COOLDOWN_TICKS : LAST_STAND_MINOR_COOLDOWN_TICKS;
		if (!COOLDOWNS.isReady(player.getUUID(), "last_stand", now, cooldown)) return;

		activateLastStand(player, hasLesser);
	}

	private static void activateLastStand(ServerPlayer player, boolean lesser) {
		long now = player.level().getGameTime();
		EFFECTS.start(player.getUUID(), EFX_LAST_STAND, now, LAST_STAND_DURATION_TICKS);
		COOLDOWNS.start(player.getUUID(), "last_stand", now);
		double boost = lesser ? 0.12 : 0.08;
		ClassSystemBootstrap.statEngine().setBuffAmount(player.getUUID(), "last_stand_buff", boost);
		ClassSystemBootstrap.statEngine().activateSource(player.getUUID(), "last_stand_buff");
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Last Stand! +" + (int)(boost * 100) + "% attack damage"
			+ " and speed for 10 seconds. (" + (lesser ? "8" : "10") + " min cooldown)"));
	}

	private static void removeLastStand(ServerPlayer player) {
		ClassSystemBootstrap.statEngine().deactivateSource(player.getUUID(), "last_stand_buff", player);
		player.sendSystemMessage(Component.literal(
			"\u00a77[Wanderers Haven]\u00a7r Last Stand faded."));
	}

	// ── Heavy Strikes (active) ────────────────────────────────────────────────

	public static void executeHeavyStrikes(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "heavy_strikes", now, HEAVY_STRIKES_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Heavy Strikes is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		if (EFFECTS.isActive(player.getUUID(), EFX_HEAVY_STRIKES, now)) return;
		EFFECTS.start(player.getUUID(), EFX_HEAVY_STRIKES, now, HEAVY_STRIKES_DURATION_TICKS);
		COOLDOWNS.start(player.getUUID(), "heavy_strikes", now);
		ClassSystemBootstrap.statEngine().activateSource(player.getUUID(), "heavy_strikes_buff");
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Heavy Strikes! +12% damage for 10 seconds. (30 sec cooldown)"));
	}

	public static void executeMaddeningStrikes(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "maddening_strikes", now, HEAVY_STRIKES_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Maddening Strikes is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		if (EFFECTS.isActive(player.getUUID(), EFX_MADDENING_STRIKES, now)) return;
		EFFECTS.start(player.getUUID(), EFX_MADDENING_STRIKES, now, HEAVY_STRIKES_DURATION_TICKS);
		COOLDOWNS.start(player.getUUID(), "maddening_strikes", now);
		ClassSystemBootstrap.statEngine().activateSource(player.getUUID(), "maddening_strikes_buff");
		maddeningArmorStacks.put(player.getUUID(), new ConcurrentHashMap<>());
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Maddening Strikes! +12% damage, +30% attack speed, and stacking armor shred for 10 seconds. (30 sec cooldown)"));
	}

	private static void removeHeavyStrikes(ServerPlayer player) {
		ClassSystemBootstrap.statEngine().deactivateSource(player.getUUID(), "heavy_strikes_buff", player);
		player.sendSystemMessage(Component.literal(
			"\u00a77[Wanderers Haven]\u00a7r Heavy Strikes faded."));
	}

	public static void onSuccessfulPlayerMeleeHit(ServerPlayer attacker, LivingEntity target, float hitDamage) {
		applyGrievousWounds(attacker, target, hitDamage);
		applyMaddeningArmorStack(attacker, target);
	}

	private static void applyGrievousWounds(ServerPlayer attacker, LivingEntity target, float hitDamage) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(attacker.getUUID(), "warrior");
		if (!skills.contains("grievous_wounds")) return;
		float chance = skills.contains("bloodthirst") ? 0.30f : 0.20f;
		if (attacker.getRandom().nextFloat() >= chance) return;
		long now = attacker.level().getGameTime();
		long expiresAt = now + BLEED_DURATION_TICKS;
		float perTickDamage = Math.max(0.1f, hitDamage * 0.10f);
		BleedState existing = bleedingTargets.get(target.getUUID());
		if (existing != null) {
			perTickDamage = Math.max(perTickDamage, existing.perTickDamage());
		}
		bleedingTargets.put(target.getUUID(), new BleedState(expiresAt, now + BLEED_TICK_INTERVAL_TICKS, perTickDamage));
	}

	private static void applyMaddeningArmorStack(ServerPlayer attacker, LivingEntity target) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(attacker.getUUID(), "warrior");
		if (!skills.contains("maddening_strikes")) return;
		TimedEffectTracker.Window active = EFFECTS.get(attacker.getUUID(), EFX_MADDENING_STRIKES);
		if (active == null || attacker.level().getGameTime() >= active.expiresAt()) return;

		Map<UUID, Integer> byTarget = maddeningArmorStacks.computeIfAbsent(attacker.getUUID(), id -> new ConcurrentHashMap<>());
		int stacks = Math.min(byTarget.getOrDefault(target.getUUID(), 0) + 1, 7);
		byTarget.put(target.getUUID(), stacks);

		double armorReduction = -0.05 * stacks;
		ATTRIBUTE_DEBUFFS.apply(
			target,
			Attributes.ARMOR,
			MADDENING_ARMOR_MOD,
			armorReduction,
			AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
			active.expiresAt()
		);
	}

	// ── Battle Cry (Weak) (active) ────────────────────────────────────────────

	public static void executeBattleCryWeak(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "battle_cry_weak", now, BATTLE_CRY_WEAK_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Battle Cry is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "battle_cry_weak", now);
		long expiresAt = now + BATTLE_CRY_WEAK_DURATION_TICKS;

		SkillParticleService.battleCryWeakCast(player);

		List<LivingEntity> nearby = nearbyEnemies(player, 3.5);
		if (nearby.isEmpty()) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Battle Cry! No enemies within range."));
			return;
		}
		for (LivingEntity enemy : nearby) {
			COMBAT_DEBUFFS.apply(enemy.getUUID(), expiresAt, 0.92f, 1.08f);
		}
		int count = nearby.size();
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Battle Cry! " + count + " enem" + (count == 1 ? "y" : "ies") +
			" weakened for 8 seconds. (45 sec cooldown)"));
	}

	// -- Harmony of Pain (Blade Dancer upgrade) --------------------------------

	public static void executeHarmonyOfPain(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "harmony_of_pain", now, HARMONY_OF_PAIN_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Harmony of Pain is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "harmony_of_pain", now);
		long expiresAt = now + HARMONY_OF_PAIN_DURATION_TICKS;

		SkillParticleService.harmonyOfPainCast(player);

		List<LivingEntity> nearby = nearbyEnemies(player, 5.0);
		if (nearby.isEmpty()) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Harmony of Pain! No enemies within range."));
			return;
		}

		for (LivingEntity enemy : nearby) {
			COMBAT_DEBUFFS.apply(enemy.getUUID(), expiresAt, 0.90f, 1.10f);
			ATTRIBUTE_DEBUFFS.apply(
				enemy,
				Attributes.MOVEMENT_SPEED,
				HARMONY_PAIN_SLOW_MOD,
				-0.40,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				expiresAt
			);
		}

		int count = nearby.size();
		player.sendSystemMessage(Component.literal(
			"\u00a7d[Wanderers Haven]\u00a7r Harmony of Pain! " + count + " enem" + (count == 1 ? "y" : "ies") +
			" afflicted for 10 seconds. (45 sec cooldown)"));
	}

	/** Returns 0.92f if the attacker is Battle Cry debuffed (8% less damage dealt). */
	public static float getBattleCryAttackerMult(UUID attackerUUID, long gameTime) {
		return COMBAT_DEBUFFS.dealtMult(attackerUUID, gameTime);
	}

	/** Returns 1.08f if the target is Battle Cry debuffed (8% more damage taken). */
	public static float getBattleCryTargetMult(UUID targetUUID, long gameTime) {
		return COMBAT_DEBUFFS.takenMult(targetUUID, gameTime);
	}

	// ── Bludgeon (active) ─────────────────────────────────────────────────────

	public static void executeBludgeon(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "bludgeon", now, BLUDGEON_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Bludgeon is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "bludgeon", now);
		ServerPlayNetworking.send(player, new PlaySkillAnimationPayload("bludgeon"));

		List<LivingEntity> targets = coneEnemies(player, 3.5, 0.5);
		if (targets.isEmpty()) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Bludgeon! No targets in range."));
			return;
		}
		float weaponDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		long expiresAt = now + BLUDGEON_DEBUFF_DURATION_TICKS;
		int hit = 0;
		for (LivingEntity target : targets) {
			target.hurt(player.damageSources().playerAttack(player), weaponDamage);
			ATTRIBUTE_DEBUFFS.apply(
				target,
				Attributes.ARMOR,
				BLUDGEON_ARMOR_MOD,
				-0.15,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				expiresAt
			);
			hit++;
		}
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Bludgeon! Hit " + hit + " target" + (hit == 1 ? "" : "s") +
			", armor reduced by 15% for 5 seconds. (15 sec cooldown)"));
	}

	public static void executeGroundSlam(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "ground_slam", now, BLUDGEON_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Ground Slam is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "ground_slam", now);
		float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0f;
		long expiresAt = now + GROUND_SLAM_SLOW_DURATION_TICKS;
		List<LivingEntity> targets = nearbyEnemies(player, 6.0);
		if (targets.isEmpty()) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Ground Slam! No enemies in range."));
			return;
		}
		for (LivingEntity target : targets) {
			target.hurt(player.damageSources().playerAttack(player), damage);
			ATTRIBUTE_DEBUFFS.apply(
				target,
				Attributes.MOVEMENT_SPEED,
				HARMONY_PAIN_SLOW_MOD,
				-0.40,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				expiresAt
			);
		}
		if (player.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.EXPLOSION,
				player.getX(), player.getY() + 0.3, player.getZ(),
				4, 1.0, 0.2, 1.0, 0.0);
		}
		int n = targets.size();
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Ground Slam! Hit " + n +
			" enem" + (n == 1 ? "y" : "ies") + " and slowed them. (15 sec cooldown)"));
	}

	// ── Piercing Charge (active) ──────────────────────────────────────────────

	public static void executePiercingCharge(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "piercing_charge", now, PIERCING_CHARGE_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Piercing Charge is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "piercing_charge", now);
		Vec3 look = player.getLookAngle();
		Vec3 startPos = player.position();
		Vec3 endPos = startPos.add(look.scale(5.0));
		AABB dashBox = pathBox(startPos, endPos, 0.6);
		List<LivingEntity> inPath = player.level().getEntitiesOfClass(
			LivingEntity.class, dashBox, e -> e != player);
		float weaponDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.3f;
		int hit = 0;
		for (LivingEntity target : inPath) {
			target.hurt(player.damageSources().playerAttack(player), weaponDamage);
			hit++;
		}
		player.setDeltaMovement(look.scale(2.2));
		player.hurtMarked = true;
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Piercing Charge!" +
			(hit > 0 ? " Hit " + hit + " enem" + (hit == 1 ? "y" : "ies") + "." : "") +
			" (20 sec cooldown)"));
	}

	public static void executeGrasscutter(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "grasscutter", now, PIERCING_CHARGE_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Grasscutter is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "grasscutter", now);

		float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5f;
		int firstHit = grasscutterSlash(player, damage);

		Vec3 look = player.getLookAngle().normalize();
		player.setDeltaMovement(look.x * 1.9, Math.max(player.getDeltaMovement().y, 0.1), look.z * 1.9);
		player.hurtMarked = true;
		grasscutterFollowups.put(player.getUUID(), new GrasscutterFollowup(now + 5L, damage));

		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Grasscutter! " +
			(firstHit > 0 ? "First slash hit " + firstHit + " enem" + (firstHit == 1 ? "y" : "ies") + ". " : "") +
			"Dashed forward into a second slash. " +
			"(20 sec cooldown)"));
	}

	private static int grasscutterSlash(ServerPlayer player, float damage) {
		List<LivingEntity> targets = nearbyEnemies(player, 3.0);
		for (LivingEntity target : targets) {
			target.hurt(player.damageSources().playerAttack(player), damage);
		}
		SkillParticleService.grasscutterSlashBurst(player);
		return targets.size();
	}

	public static void executeCrushingLeap(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "crushing_leap", now, PIERCING_CHARGE_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Crushing Leap is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "crushing_leap", now);
		Vec3 look = player.getLookAngle().normalize();
		player.setDeltaMovement(look.x * 1.35, 1.0, look.z * 1.35);
		player.hurtMarked = true;
		crushingLeapPendingLanding.put(player.getUUID(), now + CRUSHING_LEAP_AIR_WINDOW_TICKS);
		crushingLeapNoFallUntil.put(player.getUUID(), now + CRUSHING_LEAP_AIR_WINDOW_TICKS);
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Crushing Leap! Slam down to deal 300% AOE weapon damage. (20 sec cooldown)"));
	}

	public static void executeShadowStep(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "shadow_step", now, SHADOW_STEP_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Shadow Step is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		if (EFFECTS.isActive(player.getUUID(), EFX_SHADOW_STEP, now)) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Shadow Step is already active."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "shadow_step", now);
		EFFECTS.start(player.getUUID(), EFX_SHADOW_STEP, now, SHADOW_STEP_DURATION_TICKS);
		shadowStepPassedEntities.put(player.getUUID(), ConcurrentHashMap.newKeySet());
		ClassSystemBootstrap.statEngine().activateSource(player.getUUID(), "shadow_step_buff");
		player.sendSystemMessage(Component.literal(
			"\u00a78[Wanderers Haven]\u00a7r Shadow Step! Invulnerable for 5 seconds; damage triggers on expiry. (60 sec cooldown)"));
	}

	private static void shadowStepBurst(ServerPlayer player, Set<UUID> passedTargets) {
		float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0f;
		int hit = 0;
		if (player.level().getServer() == null) return;
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			for (UUID id : passedTargets) {
				net.minecraft.world.entity.Entity entity = level.getEntity(id);
				if (!(entity instanceof LivingEntity living)) continue;
				if (living == player) continue;
				living.hurt(player.damageSources().playerAttack(player), damage);
				hit++;
			}
		}
		if (player.level() instanceof ServerLevel serverLevel && hit > 0) {
			serverLevel.sendParticles(ParticleTypes.SMOKE,
				player.getX(), player.getY() + 1.0, player.getZ(),
				25, 1.0, 0.6, 1.0, 0.02);
		}
		player.sendSystemMessage(Component.literal(
			"\u00a78[Wanderers Haven]\u00a7r Shadow Step fades." +
			(hit > 0 ? " Struck " + hit + " enem" + (hit == 1 ? "y" : "ies") + "." : "")
		));
	}

	private static void crushingLeapLand(ServerPlayer player) {
		float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 3.0f;
		List<LivingEntity> targets = nearbyEnemies(player, 3.0);
		for (LivingEntity target : targets) {
			target.hurt(player.damageSources().playerAttack(player), damage);
		}
		if (player.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.EXPLOSION,
				player.getX(), player.getY() + 0.2, player.getZ(),
				8, 0.7, 0.2, 0.7, 0.0);
		}
		int n = targets.size();
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Crushing Leap impact! " +
			(n > 0 ? "Hit " + n + " enem" + (n == 1 ? "y" : "ies") + "." : "No enemies in blast radius.")));
	}

	// -- Dance of Falling Petals (Blade Dancer capstone) -----------------------

	public static void executeDanceOfFallingPetals(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "falling_petals", now, DANCE_OF_FALLING_PETALS_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Dance of Falling Petals is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		if (EFFECTS.isActive(player.getUUID(), EFX_FALLING_PETALS, now)) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Dance of Falling Petals is already active."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "falling_petals", now);
		EFFECTS.start(player.getUUID(), EFX_FALLING_PETALS, now, DANCE_OF_FALLING_PETALS_DURATION_TICKS);
		player.sendSystemMessage(Component.literal(
			"\u00a7d[Wanderers Haven]\u00a7r Dance of Falling Petals! Blade storm active for 10 seconds. (30 sec cooldown)"));
	}

	private static void danceOfFallingPetalsPulse(ServerPlayer player) {
		float pulseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.4f;
		List<LivingEntity> targets = nearbyEnemies(player, 5.0);
		for (LivingEntity target : targets) {
			target.hurt(player.damageSources().playerAttack(player), pulseDamage);
		}
		if (!targets.isEmpty()) SkillParticleService.fallingPetalsPulse(player, targets);
	}

	// -- Dance of the Butterfly (Blade Dancer upgrade) -------------------------

	public static void executeDanceOfTheButterfly(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "dance_of_butterfly", now, DANCE_OF_THE_BUTTERFLY_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Dance of the Butterfly is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		if (EFFECTS.isActive(player.getUUID(), EFX_BUTTERFLY, now)) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Dance of the Butterfly is already active."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "dance_of_butterfly", now);
		EFFECTS.start(player.getUUID(), EFX_BUTTERFLY, now, DANCE_OF_THE_BUTTERFLY_DURATION_TICKS);
		danceOfTheButterflyHitEntities.put(player.getUUID(), ConcurrentHashMap.newKeySet());
		ClassSystemBootstrap.statEngine().activateSource(player.getUUID(), "dance_of_butterfly_buff");
		player.sendSystemMessage(Component.literal(
			"\u00a7d[Wanderers Haven]\u00a7r Dance of the Butterfly! +50% speed for 5 seconds. (20 sec cooldown)"));
	}

	private static void danceOfTheButterflyTickDamage(ServerPlayer player) {
		Set<UUID> hitSet = danceOfTheButterflyHitEntities.computeIfAbsent(
			player.getUUID(),
			id -> ConcurrentHashMap.newKeySet()
		);
		float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5f;
		List<LivingEntity> touched = nearbyEnemies(player, 0.8);
		for (LivingEntity target : touched) {
			if (!hitSet.add(target.getUUID())) continue;
			target.hurt(player.damageSources().playerAttack(player), damage);
		}
	}

	// ── Berserker exclusive actives / passives ─────────────────────────────────

	private static void addBattleFuryStack(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!hasFuryEngine(skills)) return;
		float gain = 5.0f;
		if (skills.contains("rage_engine") && player.getMaxHealth() > 0.0f
				&& (player.getHealth() / player.getMaxHealth()) < 0.50f) {
			gain *= 1.5f;
		}
		addFury(player, gain, skills);
	}

	public static int getFuryPoints(UUID playerId) {
		return Math.round(furyPoints.getOrDefault(playerId, 0.0f));
	}

	public static boolean isOverclockActive(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!skills.contains("overclock")) return false;
		float fury = furyPoints.getOrDefault(player.getUUID(), 0.0f);
		if (fury >= 100.0f) return true;
		Long until = overclockSustainUntil.get(player.getUUID());
		return until != null && player.level().getGameTime() <= until;
	}

	private static void addFury(ServerPlayer player, float amount, Set<String> skills) {
		if (amount <= 0.0f || !hasFuryEngine(skills)) return;
		float cap = maxFury(skills);
		float current = furyPoints.getOrDefault(player.getUUID(), 0.0f);
		furyPoints.put(player.getUUID(), Math.min(cap, current + amount));
	}

	private static float consumeFuryForAbility(ServerPlayer player, Set<String> skills) {
		float fury = furyPoints.getOrDefault(player.getUUID(), 0.0f);
		if (fury <= 0.0f) return 0.0f;
		float consumed = skills.contains("greater_battle_fury") ? Math.min(100.0f, fury) : fury;
		furyPoints.put(player.getUUID(), Math.max(0.0f, fury - consumed));
		if (skills.contains("overclock") && fury >= 100.0f) {
			overclockSustainUntil.put(player.getUUID(), player.level().getGameTime() + OVERCLOCK_SUSTAIN_TICKS);
		}
		return consumed;
	}

	private static float maxFury(Set<String> skills) {
		float max = skills.contains("greater_battle_fury") ? 150.0f : 100.0f;
		if (skills.contains("rage_reserves")) {
			max += 100.0f;
		}
		return max;
	}

	private static boolean hasFuryEngine(Set<String> skills) {
		return skills.contains("battle_fury")
			|| skills.contains("rage_engine")
			|| skills.contains("overclock")
			|| skills.contains("greater_battle_fury")
			|| skills.contains("endless_rage")
			|| skills.contains("pain_fuels_me")
			|| skills.contains("rage_reserves")
			|| skills.contains("fury_now_blessing");
	}

	private static boolean hasOnslaughtLineSkill(Set<String> skills) {
		return skills.contains("savage_onslaught") || skills.contains("massacre");
	}

	public static void executeFuryUnleashed(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "fury_unleashed", now, FURY_UNLEASHED_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Fury Unleashed is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		if (EFFECTS.isActive(player.getUUID(), EFX_FURY_UNLEASHED, now)) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Fury Unleashed is already active."));
			return;
		}
		float fury = furyPoints.getOrDefault(player.getUUID(), 0.0f);
		if (fury < 1.0f) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r No Battle Fury stored yet — land attacks to build Fury."));
			return;
		}

		float furyConsumed = consumeFuryForAbility(player, skills);
		COOLDOWNS.start(player.getUUID(), "fury_unleashed", now);
		EFFECTS.start(player.getUUID(), EFX_FURY_UNLEASHED, now, FURY_UNLEASHED_DURATION_TICKS);

		double bonus = furyConsumed * 0.01;
		ClassSystemBootstrap.statEngine().setBuffAmount(player.getUUID(), "fury_unleashed_buff", bonus);
		ClassSystemBootstrap.statEngine().activateSource(player.getUUID(), "fury_unleashed_buff");

		int furyInt = Math.round(furyConsumed);
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Fury Unleashed! Consumed " + furyInt + " Battle Fury for +"
				+ furyInt + "% damage and +" + furyInt + "% size for 1 minute. (15 min cooldown)"));
	}

	public static void executeMockingShout(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "mocking_shout", now, MOCKING_SHOUT_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Mocking Shout is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "mocking_shout", now);
		long expiresAt = now + MOCKING_SHOUT_DURATION_TICKS;
		List<LivingEntity> nearby = nearbyEnemies(player, 8.0);
		if (nearby.isEmpty()) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Mocking Shout! No enemies within range."));
			return;
		}
		for (LivingEntity enemy : nearby) {
			COMBAT_DEBUFFS.apply(enemy.getUUID(), expiresAt, 0.70f, 1.0f);
			ATTRIBUTE_DEBUFFS.apply(
				enemy,
				Attributes.MOVEMENT_SPEED,
				MOCKING_SHOUT_SLOW_MOD,
				-0.20,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				expiresAt
			);
			if (enemy instanceof Mob mob) {
				mob.setTarget(player);
			}
		}
		int count = nearby.size();
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Mocking Shout! " + count + " enem" + (count == 1 ? "y" : "ies")
				+ " taunted, slowed, and weakened for 8 seconds. (45 sec cooldown)"));
	}

	public static void executeTerrorizingHowl(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "terrorizing_howl", now, MOCKING_SHOUT_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Terrorizing Howl is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "terrorizing_howl", now);
		long fearEndsAt = now + TERRORIZING_HOWL_FEAR_TICKS;
		List<LivingEntity> nearby = nearbyEnemies(player, 8.0);
		if (nearby.isEmpty()) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Terrorizing Howl! No enemies within range."));
			return;
		}
		for (LivingEntity enemy : nearby) {
			ATTRIBUTE_DEBUFFS.apply(
				enemy,
				Attributes.MOVEMENT_SPEED,
				TERRORIZING_HOWL_FEAR_SLOW_MOD,
				-0.60,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				fearEndsAt
			);
			if (enemy instanceof Mob mob) {
				mob.setTarget(null);
			}
			fearedEntities.put(enemy.getUUID(), new FearState(player.getUUID(), fearEndsAt));
		}
		int count = nearby.size();
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Terrorizing Howl! " + count + " enem" + (count == 1 ? "y" : "ies")
				+ " feared for 3 seconds, then taunted and debuffed for 8 seconds. (45 sec cooldown)"));
	}

	public static void executeSpinningSlash(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "spinning_slash", now, SPINNING_SLASH_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Spinning Slash is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "spinning_slash", now);
		Vec3 look = player.getLookAngle().normalize();
		Vec3 startPos = player.position();
		Vec3 endPos = startPos.add(look.scale(8.0));
		AABB dashBox = pathBox(startPos, endPos, 4.0);
		List<LivingEntity> inPath = player.level().getEntitiesOfClass(
			LivingEntity.class,
			dashBox,
			e -> e != player && (e instanceof Enemy || e instanceof Player)
		);
		float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0f;
		int hit = 0;
		for (LivingEntity target : inPath) {
			target.hurt(player.damageSources().playerAttack(player), damage);
			hit++;
		}
		player.setDeltaMovement(look.scale(2.8));
		player.hurtMarked = true;
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Spinning Slash!"
			+ (hit > 0 ? " Hit " + hit + " enem" + (hit == 1 ? "y" : "ies") + "." : "")
			+ " (20 sec cooldown)"));
	}

	public static void executeRagingCyclone(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "raging_cyclone", now, SPINNING_SLASH_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Raging Cyclone is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "raging_cyclone", now);
		Vec3 look = player.getLookAngle().normalize();
		Vec3 startPos = player.position();
		Vec3 endPos = startPos.add(look.scale(13.0));
		AABB dashBox = pathBox(startPos, endPos, 4.0);
		List<LivingEntity> inPath = player.level().getEntitiesOfClass(
			LivingEntity.class,
			dashBox,
			e -> e != player && (e instanceof Enemy || e instanceof Player)
		);
		float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.5f;
		int hit = 0;
		for (LivingEntity target : inPath) {
			target.hurt(player.damageSources().playerAttack(player), damage);
			hit++;
		}
		if (hit > 0) {
			addFury(player, hit * 5.0f, skills);
		}
		player.setDeltaMovement(look.scale(3.2));
		player.hurtMarked = true;
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Raging Cyclone!"
			+ (hit > 0 ? " Hit " + hit + " enem" + (hit == 1 ? "y" : "ies") + " and gained " + (hit * 5) + " Fury." : "")
			+ " (20 sec cooldown)"));
	}

	public static void executeSavageOnslaught(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "savage_onslaught", now, SAVAGE_ONSLAUGHT_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Savage Onslaught is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		if (EFFECTS.isActive(player.getUUID(), EFX_SAVAGE_ONSLAUGHT, now)) return;
		COOLDOWNS.start(player.getUUID(), "savage_onslaught", now);
		EFFECTS.start(player.getUUID(), EFX_SAVAGE_ONSLAUGHT, now, SAVAGE_ONSLAUGHT_DURATION_TICKS);
		ClassSystemBootstrap.statEngine().activateSource(player.getUUID(), "savage_onslaught_buff");
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Savage Onslaught! +30% damage, +20% attack speed, 15% armor/DR bypass, "
				+ "but take 30% more damage for 10 seconds. (30 sec cooldown)"));
	}

	public static void executeMassacre(ServerPlayer player) {
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "massacre", now, SAVAGE_ONSLAUGHT_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Massacre is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		if (EFFECTS.isActive(player.getUUID(), EFX_SAVAGE_ONSLAUGHT, now)) return;
		COOLDOWNS.start(player.getUUID(), "massacre", now);
		EFFECTS.start(player.getUUID(), EFX_SAVAGE_ONSLAUGHT, now, SAVAGE_ONSLAUGHT_DURATION_TICKS);
		ClassSystemBootstrap.statEngine().activateSource(player.getUUID(), "savage_onslaught_buff");
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Massacre! +30% damage, +20% attack speed, 25% armor/DR bypass, "
				+ "10% lifesteal, but take 30% more damage for 10 seconds. (30 sec cooldown)"));
	}

	public static void executeFuryBecomesFlesh(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "fury_becomes_flesh", now, FURY_BECOMES_FLESH_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Fury Becomes Flesh is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "fury_becomes_flesh", now);
		List<LivingEntity> targets = nearbyEnemies(player, 2.0);
		float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		int hit = 0;
		for (LivingEntity target : targets) {
			target.hurt(player.damageSources().playerAttack(player), damage);
			Vec3 away = target.position().subtract(player.position());
			Vec3 dir = away.lengthSqr() > 0.0001 ? away.normalize() : new Vec3(0.0, 0.0, 1.0);
			target.setDeltaMovement(dir.x * 1.0, Math.max(target.getDeltaMovement().y, 0.28), dir.z * 1.0);
			target.hurtMarked = true;
			hit++;
		}
		float furyConsumed = consumeFuryForAbility(player, skills);
		if (furyConsumed > 0.0f) {
			float heal = player.getMaxHealth() * 0.005f * furyConsumed;
			player.heal(heal);
		}
		int furyInt = Math.round(furyConsumed);
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Fury Becomes Flesh!"
				+ (hit > 0 ? " Hit " + hit + " enem" + (hit == 1 ? "y" : "ies") + "." : "")
				+ (furyInt > 0 ? " Consumed " + furyInt + " Fury for healing." : " No Fury to consume.")
				+ " (15 sec cooldown)"));
	}

	public static void executeFuryNowBlessing(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "fury_now_blessing", now, FURY_BECOMES_FLESH_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Fury, Now Blessing is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "fury_now_blessing", now);
		List<LivingEntity> targets = nearbyEnemies(player, 2.0);
		float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		int hit = 0;
		for (LivingEntity target : targets) {
			target.hurt(player.damageSources().playerAttack(player), damage);
			Vec3 away = target.position().subtract(player.position());
			Vec3 dir = away.lengthSqr() > 0.0001 ? away.normalize() : new Vec3(0.0, 0.0, 1.0);
			target.setDeltaMovement(dir.x * 1.0, Math.max(target.getDeltaMovement().y, 0.28), dir.z * 1.0);
			target.hurtMarked = true;
			hit++;
		}
		float furyConsumed = consumeFuryForAbility(player, skills);
		if (furyConsumed > 0.0f) {
			float heal = player.getMaxHealth() * 0.0075f * furyConsumed;
			player.heal(heal);
			furyBlessingCritChanceBonus.put(player.getUUID(), furyConsumed * 0.0025);
			furyBlessingCritDamageBonus.put(player.getUUID(), furyConsumed * 0.005);
			EFFECTS.start(player.getUUID(), EFX_FURY_NOW_BLESSING, now, FURY_NOW_BLESSING_DURATION_TICKS);
		} else {
			furyBlessingCritChanceBonus.remove(player.getUUID());
			furyBlessingCritDamageBonus.remove(player.getUUID());
			EFFECTS.clear(player.getUUID(), EFX_FURY_NOW_BLESSING);
		}
		int furyInt = Math.round(furyConsumed);
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Fury, Now Blessing!"
				+ (hit > 0 ? " Hit " + hit + " enem" + (hit == 1 ? "y" : "ies") + "." : "")
				+ (furyInt > 0
					? " Consumed " + furyInt + " Fury for stronger healing and empowered critical strikes for 10 seconds."
					: " No Fury to consume.")
				+ " (15 sec cooldown)"));
	}

	// ── Circular Slash (Blademaster — active) ─────────────────────────────────

	public static void executeCircularSlash(ServerPlayer player) {
		long now  = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "circular_slash", now, CIRCULAR_SLASH_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Circular Slash is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		List<LivingEntity> targets = nearbyEnemies(player, 4.0);
		if (targets.isEmpty()) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Circular Slash! No enemies in range."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "circular_slash", now);
		float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.5f;
		for (LivingEntity target : targets) {
			target.hurt(player.damageSources().playerAttack(player), damage);
		}
		int n = targets.size();
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Circular Slash! Hit " + n +
			" enem" + (n == 1 ? "y" : "ies") + ". (15 sec cooldown)"));
	}

	// ── Focus (Blademaster — active) ──────────────────────────────────────────

	public static void executeFocus(ServerPlayer player) {
		long now  = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "focus", now, FOCUS_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Focus is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		if (EFFECTS.isActive(player.getUUID(), EFX_FOCUS, now)) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Focus is already active."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "focus", now);
		EFFECTS.start(player.getUUID(), EFX_FOCUS, now, FOCUS_DURATION_TICKS);
		ClassSystemBootstrap.statEngine().activateSource(player.getUUID(), "focus_buff");
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Focus! +20% damage and 60% dodge chance for 30 seconds. (2 min cooldown)"));
	}

	/** Returns true (cancelling the hit) if Focus is active and the 60% dodge roll succeeds. */
	public static boolean tryFocusDodge(ServerPlayer player) {
		if (!EFFECTS.isActive(player.getUUID(), EFX_FOCUS, player.level().getGameTime())) return false;
		if (player.level().random.nextFloat() >= 0.60f) return false;
		player.sendSystemMessage(Component.literal(
			"\u00a7e[Wanderers Haven]\u00a7r Focus dodged an attack!"));
		return true;
	}

	// ── Spearmaster upgrades (active) ──────────────────────────────────────────

	public static void executeTripleThrust(ServerPlayer player) {
		if (!isSpearmasterWeapon(player)) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Triple Thrust requires a spearmaster weapon."));
			return;
		}
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "triple_thrust", now, TRIPLE_THRUST_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Triple Thrust is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		double thrustRange = currentSpearmasterRange(player) * 1.3;
		float thrustDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5f;
		int hit = 0;
		for (int i = 0; i < 3; i++) {
			List<LivingEntity> targets = coneEnemies(player, thrustRange, 0.45);
			for (LivingEntity target : targets) {
				target.hurt(player.damageSources().playerAttack(player), thrustDamage);
				hit++;
			}
		}
		COOLDOWNS.start(player.getUUID(), "triple_thrust", now);
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Triple Thrust!"
			+ (hit > 0 ? " Landed " + hit + " thrust hit" + (hit == 1 ? "" : "s") + "." : " No targets in range.")
			+ " (8 sec cooldown)"));
	}

	public static void executeLengthenedStrikes(ServerPlayer player) {
		if (!isSpearmasterWeapon(player)) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Spear: Lengthened Strikes requires a spearmaster weapon."));
			return;
		}
		long now = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "lengthened_strikes", now, LENGTHENED_STRIKES_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Spear: Lengthened Strikes is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		if (EFFECTS.isActive(player.getUUID(), EFX_LENGTHENED_STRIKES, now)) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Spear: Lengthened Strikes is already active."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "lengthened_strikes", now);
		EFFECTS.start(player.getUUID(), EFX_LENGTHENED_STRIKES, now, LENGTHENED_STRIKES_DURATION_TICKS);
		ClassSystemBootstrap.statEngine().activateSource(player.getUUID(), "lengthened_strikes_buff");
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Spear: Lengthened Strikes! +20% range and up to +25% distance-scaled damage for 20 seconds. (1 min cooldown)"));
	}

	public static float getSpearmasterDamageMultiplier(ServerPlayer attacker, LivingEntity target) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(attacker.getUUID(), "warrior");
		if (!isSpearmasterWeapon(attacker)) return 1.0f;

		float mult = 1.0f;

		if (skills.contains("lengthened_strikes")
				&& EFFECTS.isActive(attacker.getUUID(), EFX_LENGTHENED_STRIKES, attacker.level().getGameTime())) {
			double maxRange = Math.max(0.01, currentSpearmasterRange(attacker));
			double distance = attacker.distanceTo(target);
			double t = Math.max(0.0, Math.min(1.0, distance / maxRange));
			mult *= (float) (1.0 + 0.25 * t);
		}
		return mult;
	}

	public static float getHeadhunterDamageMultiplier(ServerPlayer attacker) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(attacker.getUUID(), "warrior");
		if (!skills.contains("bloodthirst")) return 1.0f;
		int bleedingNearby = countBleedingUnitsNearby(attacker, 8.0);
		return 1.0f + (0.08f * bleedingNearby);
	}

	public static float modifyIncomingHealing(LivingEntity entity, float baseAmount) {
		if (!isBleeding(entity.getUUID(), entity.level().getGameTime())) return baseAmount;
		return baseAmount * BLEED_HEALING_MULT;
	}

	// ── Parry (Duelist capstone — active counter-attack) ─────────────────────

	public static void executeParry(ServerPlayer player) {
		EFFECTS.start(player.getUUID(), EFX_PARRY_WINDOW, player.level().getGameTime(), PARRY_WINDOW_TICKS);
		player.sendSystemMessage(Component.literal(
			"\u00a7e[Wanderers Haven]\u00a7r Parry ready!"));
	}

	/** Called from the mixin. Returns true (cancelling damage) if parry fires and counter-attacks. */
	public static boolean tryParry(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!skills.contains("parry")) return false;
		TimedEffectTracker.Window window = EFFECTS.get(player.getUUID(), EFX_PARRY_WINDOW);
		if (window == null) return false;
		long now = player.level().getGameTime();
		if (now >= window.expiresAt()) {
			EFFECTS.clear(player.getUUID(), EFX_PARRY_WINDOW);
			return false;
		}
		EFFECTS.clear(player.getUUID(), EFX_PARRY_WINDOW);
		float counterDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5f;
		List<LivingEntity> nearby = nearbyEnemies(player, 2.0);
		for (LivingEntity enemy : nearby) {
			enemy.hurt(player.damageSources().playerAttack(player), counterDamage);
		}
		int n = nearby.size();
		player.sendSystemMessage(Component.literal(
			"\u00a7e[Wanderers Haven]\u00a7r Parry!" +
			(n > 0 ? " Counter-struck " + n + " enem" + (n == 1 ? "y" : "ies") + "." : "")));
		return true;
	}

	// ── Flash Step (Duelist — active) ─────────────────────────────────────────

	public static void executeFlashStep(ServerPlayer player) {
		long now  = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "flash_step", now, FLASH_STEP_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Flash Step is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		Vec3 look   = player.getLookAngle();
		Vec3 dest   = findFlashStepDestination(player, look, 5.0);
		float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 3.0f;

		Vec3 start3d = player.position();
		AABB dashBox = pathBox(start3d, dest, 0.6);
		List<LivingEntity> inPath = player.level().getEntitiesOfClass(
			LivingEntity.class, dashBox, e -> e != player);
		for (LivingEntity target : inPath) {
			target.hurt(player.damageSources().playerAttack(player), damage);
		}

		player.teleportTo(dest.x, dest.y, dest.z);
		COOLDOWNS.start(player.getUUID(), "flash_step", now);
		ClassSystemBootstrap.statEngine().activateSource(player.getUUID(), "flash_step_buff");
		EFFECTS.start(player.getUUID(), EFX_FLASH_STEP, now, FLASH_STEP_SPEED_DURATION_TICKS);

		int n = inPath.size();
		player.sendSystemMessage(Component.literal(
			"\u00a7e[Wanderers Haven]\u00a7r Flash Step!" +
			(n > 0 ? " Hit " + n + " enem" + (n == 1 ? "y" : "ies") + "." : "") +
			" +30% speed for 3 seconds. (8 sec cooldown)"));
	}

	private static Vec3 findFlashStepDestination(ServerPlayer player, Vec3 look, double maxDist) {
		Vec3 pos  = player.position();
		Vec3 step = look.normalize();
		Vec3 safe = pos;
		for (int i = 1; i <= (int) (maxDist * 2); i++) {
			Vec3 candidate = pos.add(step.scale(i * 0.5));
			net.minecraft.core.BlockPos bPos = net.minecraft.core.BlockPos.containing(candidate);
			if (!player.level().getBlockState(bPos).isAir()
					|| !player.level().getBlockState(bPos.above()).isAir()) break;
			safe = candidate;
		}
		return safe;
	}

	// ── Shield's Protection (Vanguard capstone — auto-block) ─────────────────

	/** Called from the mixin. Returns true (cancelling the hit) if the auto-block fires. */
	public static boolean tryShieldsProtectionBlock(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!skills.contains("shields_protection")) return false;
		boolean holdingShield =
			player.getMainHandItem().getItem() instanceof net.minecraft.world.item.ShieldItem
			|| player.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem;
		if (!holdingShield) return false;
		long now  = player.level().getGameTime();
		if (!COOLDOWNS.isReady(player.getUUID(), "shields_protection_block", now, SHIELDS_PROTECTION_COOLDOWN_TICKS)) return false;
		COOLDOWNS.start(player.getUUID(), "shields_protection_block", now);
		player.sendSystemMessage(Component.literal(
			"\u00a7b[Wanderers Haven]\u00a7r Shield's Protection! Attack blocked automatically."));
		return true;
	}

	// ── Shield Bash (Vanguard — active) ───────────────────────────────────────

	public static void executeShieldBash(ServerPlayer player) {
		boolean holdingShield =
			player.getMainHandItem().getItem() instanceof net.minecraft.world.item.ShieldItem
			|| player.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem;
		if (!holdingShield) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Shield Bash requires a shield in hand."));
			return;
		}
		long now  = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "shield_bash", now, SHIELD_BASH_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Shield Bash is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		List<LivingEntity> targets = coneEnemies(player, 3.5, 0.5);
		if (targets.isEmpty()) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Shield Bash! No targets in range."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "shield_bash", now);
		long debuffExpiry = now + SHIELD_BASH_ARMOR_DEBUFF_TICKS;
		Vec3 playerPos = player.position();
		int hit = 0;
		for (LivingEntity target : targets) {
			Vec3 dir = target.position().subtract(playerPos).normalize();
			target.knockback(2.0, -dir.x, -dir.z);
			ATTRIBUTE_DEBUFFS.apply(
				target,
				Attributes.ARMOR,
				SHIELD_BASH_ARMOR_MOD,
				-0.25,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				debuffExpiry
			);
			stunEntity(target, SHIELD_BASH_STUN_TICKS);
			hit++;
		}
		int n = hit;
		player.sendSystemMessage(Component.literal(
			"\u00a7b[Wanderers Haven]\u00a7r Shield Bash! Launched " + n +
			" enem" + (n == 1 ? "y" : "ies") + " and stunned them. (15 sec cooldown)"));
	}

	// ── Stand Your Ground (passive stacking DR) ───────────────────────────────

	private static void incrementSygStack(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!skills.contains("stand_your_ground")) return;
		int current = sygStacks.getOrDefault(player.getUUID(), 0);
		sygStacks.put(player.getUUID(), Math.min(current + 1, STAND_YOUR_GROUND_MAX_STACKS));
		sygLastHitAt.put(player.getUUID(), player.level().getGameTime());
	}

	private static int getSygStacks(ServerPlayer player) {
		Long lastHit = sygLastHitAt.get(player.getUUID());
		if (lastHit == null) return 0;
		if (player.level().getGameTime() - lastHit >= STAND_YOUR_GROUND_WINDOW_TICKS) return 0;
		return sygStacks.getOrDefault(player.getUUID(), 0);
	}

	// ── Entity stun (Shield Bash) ─────────────────────────────────────────────

	private static void stunEntity(LivingEntity entity, int durationTicks) {
		if (entity instanceof ServerPlayer sp) {
			float mult = getSlowAndStunDurationMultiplier(sp);
			durationTicks = Math.max(1, Math.round(durationTicks * mult));
		}
		entity.setDeltaMovement(0, entity.getDeltaMovement().y, 0);
		if (entity instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(true);
		stunnedEntities.put(entity.getUUID(), entity.level().getGameTime() + durationTicks);
	}

	private static void registerStunTick() {
		ServerTickEvents.END_SERVER_TICK.register(server ->
			stunnedEntities.entrySet().removeIf(entry -> {
				for (ServerLevel sl : server.getAllLevels()) {
					net.minecraft.world.entity.Entity e = sl.getEntity(entry.getKey());
					if (e == null) continue;
					if (sl.getGameTime() >= entry.getValue()) {
						if (e instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(false);
						return true;
					}
					e.setDeltaMovement(0, e.getDeltaMovement().y, 0);
					return false;
				}
				return true;
			})
		);
	}

	// ── Aura of Righteousness (Paladin — passive aura) ────────────────────────

	private static boolean hasPaladinAuraNearby(ServerPlayer player) {
		return !player.level().getEntitiesOfClass(
			ServerPlayer.class,
			player.getBoundingBox().inflate(17.0),
			p -> ClassSystemBootstrap.skillEngine()
				.ownedSkillIds(p.getUUID(), "warrior")
				.contains("aura_of_righteousness")
		).isEmpty();
	}

	public static float getPaladinAuraEnemyMult(LivingEntity target, ServerLevel level) {
		boolean paladinNearby = !level.getEntitiesOfClass(
			ServerPlayer.class,
			target.getBoundingBox().inflate(17.0),
			p -> ClassSystemBootstrap.skillEngine()
				.ownedSkillIds(p.getUUID(), "warrior")
				.contains("aura_of_righteousness")
		).isEmpty();
		return paladinNearby ? 1.15f : 1.0f;
	}

	// ── Burning Justice (Paladin — passive on-hit) ────────────────────────────

	public static float getBurningJusticeDamageMult(LivingEntity target, ServerPlayer attacker) {
		if (!ClassSystemBootstrap.skillEngine()
				.ownedSkillIds(attacker.getUUID(), "warrior")
				.contains("burning_justice")) return 1.0f;
		return target.getType().is(EntityTypeTags.UNDEAD) ? 1.36f : 1.18f;
	}

	public static void applyBurningJusticeFireOnHit(LivingEntity target, ServerPlayer attacker) {
		if (!ClassSystemBootstrap.skillEngine()
				.ownedSkillIds(attacker.getUUID(), "warrior")
				.contains("burning_justice")) return;
		target.igniteForSeconds(5);
	}

	// ── Smite (Paladin — active) ──────────────────────────────────────────────

	public static void executeSmite(ServerPlayer player) {
		long now  = player.level().getGameTime();
		long remaining = COOLDOWNS.remainingTicks(player.getUUID(), "smite", now, SMITE_COOLDOWN_TICKS);
		if (remaining > 0L) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Smite is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		List<LivingEntity> targets = coneEnemies(player, 3.5, 0.5);
		if (targets.isEmpty()) {
			player.sendSystemMessage(Component.literal(
				"\u00a7e[Wanderers Haven]\u00a7r Smite! No targets in range."));
			return;
		}
		COOLDOWNS.start(player.getUUID(), "smite", now);
		ServerLevel level    = (ServerLevel) player.level();
		float   weaponDmg    = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		long    debuffExpiry = now + BLUDGEON_DEBUFF_DURATION_TICKS;
		for (LivingEntity target : targets) {
			boolean isUndead = target.getType().is(EntityTypeTags.UNDEAD);
			target.hurt(player.damageSources().playerAttack(player), weaponDmg * (isUndead ? 5.0f : 2.5f));
			ATTRIBUTE_DEBUFFS.apply(
				target,
				Attributes.ARMOR,
				BLUDGEON_ARMOR_MOD,
				-0.15,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				debuffExpiry
			);
			target.addEffect(new MobEffectInstance(
				MobEffects.BLINDNESS, (int) BLUDGEON_DEBUFF_DURATION_TICKS, 0));
			LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
			bolt.setVisualOnly(true);
			bolt.snapTo(target.getX(), target.getY(), target.getZ());
			level.addFreshEntity(bolt);
		}
		int n = targets.size();
		player.sendSystemMessage(Component.literal(
			"\u00a7e[Wanderers Haven]\u00a7r Smite! Struck " + n +
			" enem" + (n == 1 ? "y" : "ies") + " with righteous lightning. (20 sec cooldown)"));
	}

	// ── Internal helpers ──────────────────────────────────────────────────────

	private static void pruneCombatState(net.minecraft.server.MinecraftServer server) {
		long now = server.getTickCount();
		inCombatUntil.entrySet().removeIf(entry -> now > entry.getValue());
		defyFinalCallUsedInCombat.removeIf(id -> {
			Long until = inCombatUntil.get(id);
			return until == null || now > until;
		});
		firstStrikeMarks.entrySet().removeIf(entry -> {
			entry.getValue().entrySet().removeIf(mark -> now > mark.getValue());
			return entry.getValue().isEmpty();
		});
		criticalRhythmStacks.entrySet().removeIf(entry -> {
			Long until = inCombatUntil.get(entry.getKey());
			return until == null || now > until;
		});
		overclockSustainUntil.entrySet().removeIf(entry -> now > entry.getValue());
	}

	private static void handleFuryDecay(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!hasFuryEngine(skills)) return;
		if (skills.contains("endless_rage")) return;
		float fury = furyPoints.getOrDefault(player.getUUID(), 0.0f);
		if (fury <= 0.0f) {
			furyDecayLastTickAt.remove(player.getUUID());
			return;
		}
		long now = player.level().getGameTime();
		if (isInCombat(player)) {
			furyDecayLastTickAt.put(player.getUUID(), now);
			return;
		}
		long last = furyDecayLastTickAt.getOrDefault(player.getUUID(), now);
		long elapsed = now - last;
		if (elapsed < FURY_DECAY_INTERVAL_TICKS) return;
		int steps = (int) (elapsed / FURY_DECAY_INTERVAL_TICKS);
		float reduced = Math.max(0.0f, fury - steps);
		furyPoints.put(player.getUUID(), reduced);
		furyDecayLastTickAt.put(player.getUUID(), last + (steps * FURY_DECAY_INTERVAL_TICKS));
	}

	private static void handleDangersense(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!skills.contains("dangersense")
				&& !skills.contains("greater_dangersense")
				&& !skills.contains("perception_nothing_slips_my_grasp")) return;
		if (nearbyEnemies(player, 4.0).isEmpty()) return;
		long now = player.level().getGameTime();
		long last = dangersenseLastWarnAt.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
		if (now - last < DANGERSENSE_WARN_INTERVAL_TICKS) return;
		dangersenseLastWarnAt.put(player.getUUID(), now);
	}

	private static void handleLightfootedJump(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!isInCombat(player)) return;
		int amp;
		if (skills.contains("battlefield_unmatched_mobility")) {
			amp = 1;
		} else if (skills.contains("fleetfooted")) {
			amp = 1;
		} else if (skills.contains("lightfooted")) {
			amp = 0;
		} else {
			return;
		}
		player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 40, amp, false, false, true));
	}

	private static void handleWoundClosure(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		boolean bodyNoWounds = skills.contains("my_body_fears_no_wounds");
		boolean woundRegen = skills.contains("wound_regeneration");
		boolean woundClose = skills.contains("wound_closure");
		if ((!woundClose && !woundRegen && !bodyNoWounds) || !isInCombat(player)) return;
		long now = player.level().getGameTime();
		long last = woundClosureLastHealAt.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
		if (now - last < WOUND_CLOSURE_HEAL_INTERVAL_TICKS) return;
		woundClosureLastHealAt.put(player.getUUID(), now);
		if (player.getHealth() < player.getMaxHealth()) {
			player.heal(bodyNoWounds ? 4.0f : (woundRegen ? 2.0f : 1.0f));
		}
	}

	private static void handleFireResistance(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!skills.contains("lesser_resistance_fire")
				&& !skills.contains("lesser_resistance_elements")
				&& !skills.contains("greater_resistance_elements")
				&& !skills.contains("titanic_resistance_elements")
				&& !skills.contains("divine_resistance_elements")) {
			fireTicksLastSeen.remove(player.getUUID());
			return;
		}
		int nowTicks = player.getRemainingFireTicks();
		if (nowTicks <= 0) {
			fireTicksLastSeen.put(player.getUUID(), 0);
			return;
		}
		int last = fireTicksLastSeen.getOrDefault(player.getUUID(), 0);
		if (nowTicks > last) {
			int reduced = Math.max(1, (int) Math.ceil(nowTicks * 0.85));
			player.setRemainingFireTicks(reduced);
			fireTicksLastSeen.put(player.getUUID(), reduced);
			return;
		}
		fireTicksLastSeen.put(player.getUUID(), nowTicks);
	}

	private static void handleIntimidatingAura(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		boolean imposedWill = skills.contains("my_will_imposed");
		if (!skills.contains("intimidating_aura") && !imposedWill) return;
		long now = player.level().getGameTime();
		long expiresAt = now + INTIMIDATING_AURA_DEBUFF_TICKS;
		int playerLevel = appraisalLevel(player);
		for (LivingEntity enemy : nearbyEnemies(player, 10.0)) {
			COMBAT_DEBUFFS.apply(enemy.getUUID(), expiresAt, 0.90f, 1.08f);
			ATTRIBUTE_DEBUFFS.apply(
				enemy,
				Attributes.MOVEMENT_SPEED,
				INTIMIDATING_AURA_SLOW_MOD,
				-0.10,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				expiresAt
			);
			if (imposedWill && enemy instanceof net.minecraft.world.entity.Mob && !isBossLike(enemy)) {
				int enemyLevel = appraisalLevel(enemy);
				if (playerLevel >= enemyLevel + 10) {
					stunEntity(enemy, 10);
				}
			}
		}
	}

	private static void handleLesserAppraisal(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		boolean perfect = skills.contains("perfect_appraisal");
		if (!skills.contains("lesser_appraisal") && !perfect) return;
		long now = player.level().getGameTime();
		long last = appraisalLastShownAt.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
		if (now - last < APPRAISAL_INTERVAL_TICKS) return;

		LivingEntity target = appraisalTarget(player, 16.0);
		if (target == null) return;

		int playerLevel = ClassSystemBootstrap.levelEngine().classLevel(player.getUUID(), "warrior");
		int targetLevel = appraisalLevel(target);
		if (!perfect && targetLevel > playerLevel + 5) return;

		appraisalLastShownAt.put(player.getUUID(), now);
		int hp = Math.round(target.getHealth());
		int maxHp = Math.round(target.getMaxHealth());
		String name = target.getDisplayName().getString();
		if (perfect) {
			String classes = appraisalClasses(target);
			player.displayClientMessage(Component.literal(
				"\u00a7e[Appraisal]\u00a7r " + name + "  Lv." + targetLevel + "  HP " + hp + "/" + maxHp + "  Classes: " + classes
			), true);
		} else {
			player.displayClientMessage(Component.literal(
				"\u00a7e[Appraisal]\u00a7r " + name + "  Lv." + targetLevel + "  HP " + hp + "/" + maxHp
			), true);
		}
	}

	private static boolean isBehindAttack(ServerPlayer player, DamageSource source) {
		net.minecraft.world.entity.Entity attacker = source.getEntity();
		if (!(attacker instanceof LivingEntity living)) return false;
		Vec3 forward = player.getLookAngle().normalize();
		Vec3 toAttacker = living.position().subtract(player.position()).normalize();
		return forward.dot(toAttacker) < -0.30;
	}

	private static String incomingWeaponCategory(DamageSource source) {
		if (source.getDirectEntity() instanceof AbstractArrow) {
			return "piercing";
		}
		net.minecraft.world.entity.Entity attacker = source.getEntity();
		if (!(attacker instanceof LivingEntity living)) return "bludgeoning";
		ItemStack held = living.getMainHandItem();
		String category = WeaponCategoryResolver.resolveFromItem(held);
		if (category == null) return "bludgeoning";
		return switch (category) {
			case "spear", "ranged" -> "piercing";
			case "mauler" -> "bludgeoning";
			case "duelist", "bladedancer", "blademaster", "axe", "scythe" -> "blades";
			default -> "bludgeoning";
		};
	}

	private static boolean isElementalDamage(DamageSource source) {
		if (source == null) return false;
		return source.is(DamageTypes.IN_FIRE)
			|| source.is(DamageTypes.ON_FIRE)
			|| source.is(DamageTypes.LAVA)
			|| source.is(DamageTypes.HOT_FLOOR)
			|| source.is(DamageTypes.LIGHTNING_BOLT)
			|| source.is(DamageTypes.FREEZE);
	}

	private static boolean isMagicDamage(DamageSource source) {
		if (source == null) return false;
		return source.is(DamageTypes.MAGIC)
			|| source.is(DamageTypes.INDIRECT_MAGIC);
	}

	private static LivingEntity appraisalTarget(ServerPlayer player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		LivingEntity best = null;
		double bestDist = range + 0.1;
		List<LivingEntity> candidates = player.level().getEntitiesOfClass(
			LivingEntity.class,
			player.getBoundingBox().inflate(range),
			e -> e != player
		);
		for (LivingEntity e : candidates) {
			if (!player.hasLineOfSight(e)) continue;
			Vec3 to = e.getEyePosition().subtract(eye);
			double dist = to.length();
			if (dist > range || dist < 0.01) continue;
			double dot = look.dot(to.normalize());
			if (dot < 0.965) continue;
			if (dist < bestDist) {
				bestDist = dist;
				best = e;
			}
		}
		return best;
	}

	private static int appraisalLevel(LivingEntity target) {
		if (target instanceof ServerPlayer sp) {
			return ClassSystemBootstrap.engine().profile(sp.getUUID())
				.map(profile -> profile.obtainedClasses().stream()
					.mapToInt(classId -> ClassSystemBootstrap.levelEngine().classLevel(sp.getUUID(), classId))
					.max()
					.orElse(ClassSystemBootstrap.levelEngine().classLevel(sp.getUUID(), "warrior")))
				.orElse(ClassSystemBootstrap.levelEngine().classLevel(sp.getUUID(), "warrior"));
		}
		String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
		double difficulty = com.wanderershaven.levelup.MobDifficulty.of(entityId);
		return Math.max(1, (int) Math.ceil(difficulty * 10.0));
	}

	private static String appraisalClasses(LivingEntity target) {
		if (!(target instanceof ServerPlayer sp)) {
			return "none";
		}
		return ClassSystemBootstrap.engine().profile(sp.getUUID())
			.map(profile -> {
				if (profile.obtainedClasses().isEmpty()) return "none";
				return String.join(", ", profile.obtainedClasses().stream().sorted().toList());
			})
			.orElse("none");
	}

	private static boolean isBossLike(LivingEntity entity) {
		String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
		double difficulty = com.wanderershaven.levelup.MobDifficulty.of(entityId);
		return difficulty >= 100.0;
	}

	private static void triggerDefyFinalCallShockwave(ServerPlayer player, float healedAmount) {
		if (healedAmount <= 0.0f) return;
		ServerLevel level = (ServerLevel) player.level();
		double x = player.getX();
		double y = player.getY() + 1.0;
		double z = player.getZ();
		level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 10, 0.5, 0.2, 0.5, 0.02);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);

		for (LivingEntity enemy : nearbyEnemies(player, 6.0)) {
			enemy.hurt(player.damageSources().playerAttack(player), healedAmount);
			Vec3 away = enemy.position().subtract(player.position());
			Vec3 dir = away.lengthSqr() > 0.0001 ? away.normalize() : new Vec3(0.0, 0.0, 1.0);
			enemy.setDeltaMovement(
				dir.x * 1.2,
				Math.max(enemy.getDeltaMovement().y, 0.35),
				dir.z * 1.2
			);
			enemy.hurtMarked = true;
		}
	}

	private static boolean isFirstStrike(ServerPlayer attacker, LivingEntity target, long now) {
		Map<UUID, Long> marks = firstStrikeMarks.computeIfAbsent(attacker.getUUID(), ignored -> new ConcurrentHashMap<>());
		Long until = marks.get(target.getUUID());
		if (until != null && now <= until) return false;
		marks.put(target.getUUID(), now + COMBAT_WINDOW_TICKS);
		return true;
	}

	private static float measuredStrikeMultiplier(LivingEntity target, DamageSource source, float preArmorAmount, float armorBypass, float drBypass) {
		float armor = (float) target.getAttributeValue(Attributes.ARMOR);
		float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
		float normal = CombatRules.getDamageAfterAbsorb(target, preArmorAmount, source, armor, toughness);
		if (normal <= 0.0001f) return 1.0f;
		float reducedArmor = Math.max(0.0f, armor * (1.0f - armorBypass));
		float ignored = CombatRules.getDamageAfterAbsorb(target, preArmorAmount, source, reducedArmor, toughness);
		float mult = Math.max(1.0f, ignored / normal);
		if (drBypass > 0.0f && target instanceof ServerPlayer sp) {
			float drMult = getSelfSkillDamageReductionMultiplier(sp, source);
			if (drMult > 0.0001f && drMult < 1.0f) {
				float effectiveDr = 1.0f - ((1.0f - drMult) * (1.0f - drBypass));
				mult *= Math.max(1.0f, effectiveDr / drMult);
			}
		}
		return mult;
	}

	private static float getSelfSkillDamageReductionMultiplier(ServerPlayer player, DamageSource source) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		float mult = 1.0f;

		if (skills.contains("perception_nothing_slips_my_grasp") && isBehindAttack(player, source)) mult *= 0.50f;
		else if (skills.contains("greater_dangersense") && isBehindAttack(player, source)) mult *= 0.80f;

		if (skills.contains("divine_resistance_elements") && isElementalDamage(source)) mult *= 0.57f;
		else if (skills.contains("titanic_resistance_elements") && isElementalDamage(source)) mult *= 0.72f;
		else if (skills.contains("greater_resistance_elements") && isElementalDamage(source)) mult *= 0.80f;
		else if (skills.contains("lesser_resistance_elements") && isElementalDamage(source)) mult *= 0.90f;

		String category = incomingWeaponCategory(source);
		if (skills.contains("divine_resistance_piercing") && "piercing".equals(category)) mult *= 0.49f;
		else if (skills.contains("titanic_resistance_piercing") && "piercing".equals(category)) mult *= 0.64f;
		else if (skills.contains("greater_resistance_piercing") && "piercing".equals(category)) mult *= 0.72f;
		else if (skills.contains("enhanced_resistance_piercing") && "piercing".equals(category)) mult *= 0.82f;
		else if (skills.contains("lesser_resistance_piercing") && "piercing".equals(category)) mult *= 0.90f;

		if (skills.contains("divine_resistance_bludgeoning") && "bludgeoning".equals(category)) mult *= 0.49f;
		else if (skills.contains("titanic_resistance_bludgeoning") && "bludgeoning".equals(category)) mult *= 0.64f;
		else if (skills.contains("greater_resistance_bludgeoning") && "bludgeoning".equals(category)) mult *= 0.72f;
		else if (skills.contains("enhanced_resistance_bludgeoning") && "bludgeoning".equals(category)) mult *= 0.82f;
		else if (skills.contains("lesser_resistance_bludgeoning") && "bludgeoning".equals(category)) mult *= 0.90f;

		if (skills.contains("divine_resistance_blades") && "blades".equals(category)) mult *= 0.49f;
		else if (skills.contains("titanic_resistance_blades") && "blades".equals(category)) mult *= 0.64f;
		else if (skills.contains("greater_resistance_blades") && "blades".equals(category)) mult *= 0.72f;
		else if (skills.contains("enhanced_resistance_blades") && "blades".equals(category)) mult *= 0.82f;
		else if (skills.contains("lesser_resistance_blades") && "blades".equals(category)) mult *= 0.90f;

		if (skills.contains("skin_of_adamantium")) mult *= 0.75f;
		else if (skills.contains("diamond_skin")) mult *= 0.80f;
		else if (skills.contains("iron_skin")) mult *= 0.88f;
		else if (skills.contains("tough_skin")) mult *= 0.95f;

		if (source != null && source.getDirectEntity() instanceof AbstractArrow
				&& (skills.contains("iron_skin") || skills.contains("diamond_skin") || skills.contains("skin_of_adamantium"))) {
			mult *= 0.90f;
		}

		if (skills.contains("mythic_endurance")) mult *= 0.32f;
		else if (skills.contains("legendary_endurance")) mult *= 0.47f;
		else if (skills.contains("immense_endurance")) mult *= 0.57f;
		else if (skills.contains("greater_endurance")) mult *= 0.72f;
		else if (skills.contains("enhanced_endurance")) mult *= 0.82f;
		else if (skills.contains("lesser_endurance")) mult *= 0.90f;

		if (skills.contains("skin_of_adamantium") && isMagicDamage(source)) mult *= 0.60f;

		if (hasOnslaughtLineSkill(skills)
				&& EFFECTS.isActive(player.getUUID(), EFX_SAVAGE_ONSLAUGHT, player.level().getGameTime())) {
			mult *= 1.30f;
		}

		if (skills.contains("last_breath") && player.getMaxHealth() > 0.0f
				&& (player.getHealth() / player.getMaxHealth()) <= 0.15f) {
			mult *= 0.60f;
		}

		if (skills.contains("stand_your_ground")) {
			int stacks = getSygStacks(player);
			if (stacks > 0) mult *= (1.0f - Math.min(stacks * 0.05f, 0.60f));
		}

		return mult;
	}

	private static boolean isSpearmasterWeapon(ServerPlayer player) {
		return "spear".equals(WeaponCategoryResolver.resolveFromItem(player.getMainHandItem()));
	}

	private static double currentSpearmasterRange(ServerPlayer player) {
		double range = 3.5;
		if (!isSpearmasterWeapon(player)) return range;
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (skills.contains("spear_mastery")) range *= 1.2;
		if (skills.contains("lengthened_strikes")
				&& EFFECTS.isActive(player.getUUID(), EFX_LENGTHENED_STRIKES, player.level().getGameTime())) {
			range *= 1.2;
		}
		return range;
	}

	private static boolean isBleeding(UUID entityId, long now) {
		BleedState bleed = bleedingTargets.get(entityId);
		if (bleed == null) return false;
		if (now >= bleed.expiresAt()) {
			bleedingTargets.remove(entityId);
			return false;
		}
		return true;
	}

	private static int countBleedingUnitsNearby(ServerPlayer attacker, double radius) {
		double r2 = radius * radius;
		long now = attacker.level().getGameTime();
		int count = 0;
		for (LivingEntity entity : attacker.level().getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(radius))) {
			if (entity == attacker) continue;
			if (entity.distanceToSqr(attacker) > r2) continue;
			if (isBleeding(entity.getUUID(), now)) count++;
		}
		return count;
	}

	private static boolean isInFrontCone(ServerPlayer player, LivingEntity entity, double maxDist, double minDot) {
		Vec3 toEntity = entity.position().subtract(player.position());
		double dist = toEntity.length();
		if (dist > maxDist || dist < 0.01) return false;
		return player.getLookAngle().dot(toEntity.normalize()) >= minDot;
	}

	private static List<LivingEntity> nearbyEnemies(ServerPlayer player, double range) {
		return player.level().getEntitiesOfClass(
			LivingEntity.class,
			player.getBoundingBox().inflate(range),
			e -> e != player && (e instanceof Enemy || (e instanceof Player && e != player))
		);
	}

	private static List<LivingEntity> coneEnemies(ServerPlayer player, double range, double minDot) {
		return player.level().getEntitiesOfClass(
			LivingEntity.class,
			player.getBoundingBox().inflate(range),
			e -> e != player && isInFrontCone(player, e, range, minDot)
		);
	}

	private static AABB pathBox(Vec3 startPos, Vec3 endPos, double pad) {
		return new AABB(
			Math.min(startPos.x, endPos.x) - pad, startPos.y - 0.1, Math.min(startPos.z, endPos.z) - pad,
			Math.max(startPos.x, endPos.x) + pad, startPos.y + 2.1, Math.max(startPos.z, endPos.z) + pad
		);
	}

}
