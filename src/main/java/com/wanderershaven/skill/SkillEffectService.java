package com.wanderershaven.skill;

import com.wanderershaven.classsystem.ClassSystemBootstrap;
import com.wanderershaven.network.WanderersHavenNetworking;
import com.wanderershaven.network.PlaySkillAnimationPayload;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;

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

	// ── Cooldown / duration constants ─────────────────────────────────────────

	private static final long LUCKY_DODGE_COOLDOWN_TICKS         = 12_000L;
	private static final long SECOND_WIND_MINOR_COOLDOWN_TICKS   = 12_000L;
	private static final long SECOND_WIND_LESSER_COOLDOWN_TICKS  =  9_600L;
	private static final long LAST_STAND_MINOR_COOLDOWN_TICKS    = 12_000L;
	private static final long LAST_STAND_LESSER_COOLDOWN_TICKS   =  9_600L;
	private static final long LAST_STAND_DURATION_TICKS          =   200L;
	private static final long HEAVY_STRIKES_COOLDOWN_TICKS       =   600L;
	private static final long HEAVY_STRIKES_DURATION_TICKS       =   200L;
	private static final long BATTLE_CRY_WEAK_COOLDOWN_TICKS     =   900L;
	private static final long BATTLE_CRY_WEAK_DURATION_TICKS     =   160L;
	private static final long BLUDGEON_COOLDOWN_TICKS            =   300L;
	private static final long BLUDGEON_DEBUFF_DURATION_TICKS     =   100L;
	private static final long PIERCING_CHARGE_COOLDOWN_TICKS     =   400L;
	private static final long FIGHTING_SPIRIT_COOLDOWN_TICKS     = 6_000L;
	private static final long FIGHTING_SPIRIT_DURATION_TICKS     =   600L;
	private static final long FURY_UNLEASHED_COOLDOWN_TICKS      = 18_000L;
	private static final long FURY_UNLEASHED_DURATION_TICKS      = 1_200L;
	private static final long SMITE_COOLDOWN_TICKS               =   400L;
	private static final long CIRCULAR_SLASH_COOLDOWN_TICKS      =   300L;
	private static final long FOCUS_COOLDOWN_TICKS               = 2_400L;
	private static final long FOCUS_DURATION_TICKS               =   600L;
	private static final long PARRY_WINDOW_TICKS                 =    10L;
	private static final long FLASH_STEP_COOLDOWN_TICKS          =   160L;
	private static final long FLASH_STEP_SPEED_DURATION_TICKS    =    60L;
	private static final long SHIELDS_PROTECTION_COOLDOWN_TICKS  =   140L;
	private static final long SHIELD_BASH_COOLDOWN_TICKS         =   300L;
	private static final long SHIELD_BASH_ARMOR_DEBUFF_TICKS     =   100L;
	private static final int  SHIELD_BASH_STUN_TICKS             =    20;
	private static final long STAND_YOUR_GROUND_WINDOW_TICKS     =   300L;
	private static final int  STAND_YOUR_GROUND_MAX_STACKS       =    12;

	// ── State maps ────────────────────────────────────────────────────────────

	private static final Map<UUID, Long>  luckyDodgeCooldowns         = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  secondWindCooldowns         = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  lastStandCooldowns          = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  lastStandActivatedAt        = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  heavyStrikesCooldowns       = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  heavyStrikesActivatedAt     = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  battleCryWeakCooldowns      = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  battleCryDebuffedEntities   = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  bludgeonCooldowns           = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  bludgeonedEntities          = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  piercingChargeCooldowns     = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  fightingSpiritCooldowns     = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  fightingSpiritActivatedAt   = new ConcurrentHashMap<>();
	private static final Map<UUID, Float> furyPoints                  = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  furyUnleashedCooldowns      = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  furyUnleashedActivatedAt    = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  smiteCooldowns              = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  circularSlashCooldowns      = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  focusCooldowns              = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  focusActivatedAt            = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  parryActivatedAt            = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  flashStepCooldowns          = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  flashStepSpeedExpiresAt     = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  shieldsProtectionLastBlock  = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>  shieldBashCooldowns         = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> sygStacks                 = new ConcurrentHashMap<>();
	private static final Map<UUID, Long>    sygLastHitAt              = new ConcurrentHashMap<>();

	// Stun (Shield Bash)
	private static final Map<UUID, Long> stunnedEntities = new ConcurrentHashMap<>();

	// ── Registration ──────────────────────────────────────────────────────────

	/** Register all event listeners. Call during mod initialisation. */
	public static void register() {
		// Apply engine-managed attribute modifiers and sync skills on join
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			ClassSystemBootstrap.statEngine().applyAll(player);
			WanderersHavenNetworking.sendSkillsTo(player);
		});

		// Damage event: accumulate fury, increment SYG stacks, trigger Second Wind / Fighting Spirit / Last Stand
		ServerLivingEntityEvents.AFTER_DAMAGE.register(
			(entity, source, baseDamage, actualDamage, blocked) -> {
				if (!(entity instanceof ServerPlayer player)) return;
				accumulateFury(player, actualDamage);
				incrementSygStack(player);
				float ratio = player.getHealth() / player.getMaxHealth();
				if (ratio < 0.4f) {
					trySecondWind(player);
					tryFightingSpirit(player);
					tryActivateLastStand(player);
				}
			}
		);

		// PlayerStatEngine — single tick that manages all attribute contributions
		ServerTickEvents.END_SERVER_TICK.register(server ->
			ClassSystemBootstrap.statEngine().tick(server));

		// Last Stand — expire buff after 10 seconds
		ServerTickEvents.END_SERVER_TICK.register(server ->
			lastStandActivatedAt.entrySet().removeIf(entry -> {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					ClassSystemBootstrap.statEngine().deactivateSource(entry.getKey(), "last_stand_buff", null);
					return true;
				}
				if (player.level().getGameTime() - entry.getValue() >= LAST_STAND_DURATION_TICKS) {
					removeLastStand(player);
					return true;
				}
				return false;
			})
		);

		// Heavy Strikes — expire buff after 10 seconds
		ServerTickEvents.END_SERVER_TICK.register(server ->
			heavyStrikesActivatedAt.entrySet().removeIf(entry -> {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					ClassSystemBootstrap.statEngine().deactivateSource(entry.getKey(), "heavy_strikes_buff", null);
					return true;
				}
				if (player.level().getGameTime() - entry.getValue() >= HEAVY_STRIKES_DURATION_TICKS) {
					removeHeavyStrikes(player);
					return true;
				}
				return false;
			})
		);

		// Bludgeon / Shield Bash — remove enemy armor reduction after expiry
		ServerTickEvents.END_SERVER_TICK.register(server ->
			bludgeonedEntities.entrySet().removeIf(entry -> {
				for (ServerLevel sl : server.getAllLevels()) {
					net.minecraft.world.entity.Entity entity = sl.getEntity(entry.getKey());
					if (entity instanceof LivingEntity living) {
						if (sl.getGameTime() >= entry.getValue()) {
							AttributeInstance armorInst = living.getAttribute(Attributes.ARMOR);
							if (armorInst != null) {
								armorInst.removeModifier(BLUDGEON_ARMOR_MOD);
								armorInst.removeModifier(SHIELD_BASH_ARMOR_MOD);
							}
							return true;
						}
						return false;
					}
				}
				return true;
			})
		);

		// Fighting Spirit — expire 50% DR window after 30 seconds
		ServerTickEvents.END_SERVER_TICK.register(server ->
			fightingSpiritActivatedAt.entrySet().removeIf(entry -> {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) return true;
				if (player.level().getGameTime() - entry.getValue() >= FIGHTING_SPIRIT_DURATION_TICKS) {
					player.sendSystemMessage(Component.literal(
						"\u00a77[Wanderers Haven]\u00a7r Fighting Spirit faded."));
					return true;
				}
				return false;
			})
		);

		// Fury Unleashed — expire damage/size buff after 1 minute
		ServerTickEvents.END_SERVER_TICK.register(server ->
			furyUnleashedActivatedAt.entrySet().removeIf(entry -> {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					ClassSystemBootstrap.statEngine().deactivateSource(entry.getKey(), "fury_unleashed_buff", null);
					return true;
				}
				if (player.level().getGameTime() - entry.getValue() >= FURY_UNLEASHED_DURATION_TICKS) {
					removeFuryUnleashedBuff(player);
					return true;
				}
				return false;
			})
		);

		// Flash Step — expire speed buff
		ServerTickEvents.END_SERVER_TICK.register(server ->
			flashStepSpeedExpiresAt.entrySet().removeIf(entry -> {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					ClassSystemBootstrap.statEngine().deactivateSource(entry.getKey(), "flash_step_buff", null);
					return true;
				}
				if (player.level().getGameTime() >= entry.getValue()) {
					ClassSystemBootstrap.statEngine().deactivateSource(player.getUUID(), "flash_step_buff", player);
					return true;
				}
				return false;
			})
		);

		// Focus — expire damage amp and dodge chance after 30 seconds
		ServerTickEvents.END_SERVER_TICK.register(server ->
			focusActivatedAt.entrySet().removeIf(entry -> {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) {
					ClassSystemBootstrap.statEngine().deactivateSource(entry.getKey(), "focus_buff", null);
					return true;
				}
				if (player.level().getGameTime() - entry.getValue() >= FOCUS_DURATION_TICKS) {
					ClassSystemBootstrap.statEngine().deactivateSource(player.getUUID(), "focus_buff", player);
					player.sendSystemMessage(Component.literal(
						"\u00a77[Wanderers Haven]\u00a7r Focus faded."));
					return true;
				}
				return false;
			})
		);

		// Stand Your Ground — expire hit stacks after 15 seconds of no damage
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (!ClassSystemBootstrap.skillEngine()
						.ownedSkillIds(player.getUUID(), "warrior")
						.contains("warrior_vanguard_stand_your_ground")) continue;
				Long lastHit = sygLastHitAt.get(player.getUUID());
				if (lastHit == null) continue;
				if (player.level().getGameTime() - lastHit >= STAND_YOUR_GROUND_WINDOW_TICKS) {
					sygStacks.remove(player.getUUID());
					sygLastHitAt.remove(player.getUUID());
				}
			}
		});

		// Entity stun (Shield Bash)
		registerStunTick();
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

	// ── Mixin helpers ─────────────────────────────────────────────────────────

	/**
	 * Returns a composite incoming damage multiplier for damage-reduction skills.
	 * Stacks multiplicatively.
	 */
	public static float getDamageMultiplier(ServerPlayer player, DamageSource source) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		float mult = 1.0f;
		if      (skills.contains("warrior_iron_skin"))          mult *= 0.88f;
		else if (skills.contains("warrior_tough_skin"))         mult *= 0.95f;
		if (source != null
				&& source.getDirectEntity() instanceof AbstractArrow
				&& skills.contains("warrior_iron_skin")) {
			mult *= 0.90f;
		}
		if      (skills.contains("warrior_enhanced_endurance")) mult *= 0.82f;
		else if (skills.contains("warrior_lesser_endurance"))   mult *= 0.90f;
		// Fighting Spirit — 50% DR during active window
		if (skills.contains("warrior_berserker_fighting_spirit")) {
			Long activatedAt = fightingSpiritActivatedAt.get(player.getUUID());
			if (activatedAt != null
					&& player.level().getGameTime() - activatedAt < FIGHTING_SPIRIT_DURATION_TICKS) {
				mult *= 0.50f;
			}
		}
		// Aura of Righteousness — 15% DR while a paladin is within 17 blocks
		if (hasPaladinAuraNearby(player)) {
			mult *= 0.85f;
		}
		// Stand Your Ground — up to 60% DR from stacked hits within the 15-sec window
		if (skills.contains("warrior_vanguard_stand_your_ground")) {
			int stacks = getSygStacks(player);
			if (stacks > 0) {
				mult *= (1.0f - Math.min(stacks * 0.05f, 0.60f));
			}
		}
		return mult;
	}

	/** True if the player has Tough Skin or Iron Skin (cactus immunity). */
	public static boolean hasCactusImmunity(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		return skills.contains("warrior_tough_skin") || skills.contains("warrior_iron_skin");
	}

	/** Returns true and starts the 10-minute cooldown if Lucky Dodge fires. */
	public static boolean tryLuckyDodge(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!skills.contains("warrior_lucky_dodge")) return false;
		long now = player.level().getGameTime();
		Long last = luckyDodgeCooldowns.get(player.getUUID());
		if (last != null && now - last < LUCKY_DODGE_COOLDOWN_TICKS) return false;
		luckyDodgeCooldowns.put(player.getUUID(), now);
		return true;
	}

	/** Duration multiplier for poison effects (0.80 for Lesser, 0.90 for Minor, 1.0 otherwise). */
	public static float getPoisonDurationMultiplier(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (skills.contains("warrior_lesser_poison_resistance")) return 0.80f;
		if (skills.contains("warrior_minor_poison_resistance"))  return 0.90f;
		return 1.0f;
	}

	/** True if the player has Slow Metabolism. */
	public static boolean hasSlowMetabolism(ServerPlayer player) {
		return ClassSystemBootstrap.skillEngine()
			.ownedSkillIds(player.getUUID(), "warrior")
			.contains("warrior_slow_metabolism");
	}

	// ── Second Wind ───────────────────────────────────────────────────────────

	private static void trySecondWind(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		boolean hasLesser = skills.contains("warrior_second_wind_lesser");
		boolean hasMinor  = skills.contains("warrior_second_wind_minor");
		if (!hasLesser && !hasMinor) return;

		long now = player.level().getGameTime();
		Long last = secondWindCooldowns.get(player.getUUID());
		long cooldown = hasLesser ? SECOND_WIND_LESSER_COOLDOWN_TICKS : SECOND_WIND_MINOR_COOLDOWN_TICKS;
		if (last != null && now - last < cooldown) return;

		secondWindCooldowns.put(player.getUUID(), now);
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
		boolean hasLesser = skills.contains("warrior_last_stand_lesser");
		boolean hasMinor  = skills.contains("warrior_last_stand_minor");
		if (!hasLesser && !hasMinor) return;
		if (lastStandActivatedAt.containsKey(player.getUUID())) return;

		long now = player.level().getGameTime();
		Long last = lastStandCooldowns.get(player.getUUID());
		long cooldown = hasLesser ? LAST_STAND_LESSER_COOLDOWN_TICKS : LAST_STAND_MINOR_COOLDOWN_TICKS;
		if (last != null && now - last < cooldown) return;

		activateLastStand(player, hasLesser);
	}

	private static void activateLastStand(ServerPlayer player, boolean lesser) {
		long now = player.level().getGameTime();
		lastStandActivatedAt.put(player.getUUID(), now);
		lastStandCooldowns.put(player.getUUID(), now);
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
		Long last = heavyStrikesCooldowns.get(player.getUUID());
		if (last != null && now - last < HEAVY_STRIKES_COOLDOWN_TICKS) {
			long remaining = HEAVY_STRIKES_COOLDOWN_TICKS - (now - last);
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Heavy Strikes is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		if (heavyStrikesActivatedAt.containsKey(player.getUUID())) return;
		heavyStrikesActivatedAt.put(player.getUUID(), now);
		heavyStrikesCooldowns.put(player.getUUID(), now);
		ClassSystemBootstrap.statEngine().activateSource(player.getUUID(), "heavy_strikes_buff");
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Heavy Strikes! +12% damage for 10 seconds. (30 sec cooldown)"));
	}

	private static void removeHeavyStrikes(ServerPlayer player) {
		ClassSystemBootstrap.statEngine().deactivateSource(player.getUUID(), "heavy_strikes_buff", player);
		player.sendSystemMessage(Component.literal(
			"\u00a77[Wanderers Haven]\u00a7r Heavy Strikes faded."));
	}

	// ── Battle Cry (Weak) (active) ────────────────────────────────────────────

	public static void executeBattleCryWeak(ServerPlayer player) {
		long now = player.level().getGameTime();
		Long last = battleCryWeakCooldowns.get(player.getUUID());
		if (last != null && now - last < BATTLE_CRY_WEAK_COOLDOWN_TICKS) {
			long remaining = BATTLE_CRY_WEAK_COOLDOWN_TICKS - (now - last);
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Battle Cry is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		battleCryWeakCooldowns.put(player.getUUID(), now);
		long expiresAt = now + BATTLE_CRY_WEAK_DURATION_TICKS;

		ServerLevel serverLevel = (ServerLevel) player.level();
		double px = player.getX(), py = player.getY() + 1.0, pz = player.getZ();
		serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, px, py, pz, 40, 1.5, 0.8, 1.5, 0.4);
		serverLevel.sendParticles(ParticleTypes.CRIT,           px, py, pz, 25, 1.5, 0.8, 1.5, 0.6);

		List<LivingEntity> nearby = player.level().getEntitiesOfClass(
			LivingEntity.class,
			player.getBoundingBox().inflate(3.5),
			e -> e != player && (e instanceof Enemy || (e instanceof Player && e != player))
		);
		if (nearby.isEmpty()) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Battle Cry! No enemies within range."));
			return;
		}
		for (LivingEntity enemy : nearby) {
			battleCryDebuffedEntities.put(enemy.getUUID(), expiresAt);
		}
		int count = nearby.size();
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Battle Cry! " + count + " enem" + (count == 1 ? "y" : "ies") +
			" weakened for 8 seconds. (45 sec cooldown)"));
	}

	/** Returns 0.92f if the attacker is Battle Cry debuffed (8% less damage dealt). */
	public static float getBattleCryAttackerMult(UUID attackerUUID, long gameTime) {
		Long expiresAt = battleCryDebuffedEntities.get(attackerUUID);
		if (expiresAt == null) return 1.0f;
		if (gameTime >= expiresAt) { battleCryDebuffedEntities.remove(attackerUUID); return 1.0f; }
		return 0.92f;
	}

	/** Returns 1.08f if the target is Battle Cry debuffed (8% more damage taken). */
	public static float getBattleCryTargetMult(UUID targetUUID, long gameTime) {
		Long expiresAt = battleCryDebuffedEntities.get(targetUUID);
		if (expiresAt == null) return 1.0f;
		if (gameTime >= expiresAt) { battleCryDebuffedEntities.remove(targetUUID); return 1.0f; }
		return 1.08f;
	}

	// ── Bludgeon (active) ─────────────────────────────────────────────────────

	public static void executeBludgeon(ServerPlayer player) {
		long now = player.level().getGameTime();
		Long last = bludgeonCooldowns.get(player.getUUID());
		if (last != null && now - last < BLUDGEON_COOLDOWN_TICKS) {
			long remaining = BLUDGEON_COOLDOWN_TICKS - (now - last);
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Bludgeon is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		bludgeonCooldowns.put(player.getUUID(), now);
		ServerPlayNetworking.send(player, new PlaySkillAnimationPayload("warrior_bludgeon"));

		List<LivingEntity> targets = player.level().getEntitiesOfClass(
			LivingEntity.class,
			player.getBoundingBox().inflate(3.5),
			e -> e != player && isInFrontCone(player, e, 3.5, 0.5)
		);
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
			AttributeInstance armorInst = target.getAttribute(Attributes.ARMOR);
			if (armorInst != null) {
				armorInst.removeModifier(BLUDGEON_ARMOR_MOD);
				armorInst.addTransientModifier(
					new AttributeModifier(BLUDGEON_ARMOR_MOD, -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
				bludgeonedEntities.put(target.getUUID(), expiresAt);
			}
			hit++;
		}
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Bludgeon! Hit " + hit + " target" + (hit == 1 ? "" : "s") +
			", armor reduced by 15% for 5 seconds. (15 sec cooldown)"));
	}

	// ── Piercing Charge (active) ──────────────────────────────────────────────

	public static void executePiercingCharge(ServerPlayer player) {
		long now = player.level().getGameTime();
		Long last = piercingChargeCooldowns.get(player.getUUID());
		if (last != null && now - last < PIERCING_CHARGE_COOLDOWN_TICKS) {
			long remaining = PIERCING_CHARGE_COOLDOWN_TICKS - (now - last);
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Piercing Charge is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		piercingChargeCooldowns.put(player.getUUID(), now);
		Vec3 look = player.getLookAngle();
		Vec3 startPos = player.position();
		Vec3 endPos = startPos.add(look.scale(5.0));
		AABB dashBox = new AABB(
			Math.min(startPos.x, endPos.x) - 0.6, startPos.y - 0.1, Math.min(startPos.z, endPos.z) - 0.6,
			Math.max(startPos.x, endPos.x) + 0.6, startPos.y + 2.1, Math.max(startPos.z, endPos.z) + 0.6
		);
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

	// ── Fighting Spirit (passive — triggers at ≤40% HP) ──────────────────────

	private static void tryFightingSpirit(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!skills.contains("warrior_berserker_fighting_spirit")) return;
		if (fightingSpiritActivatedAt.containsKey(player.getUUID())) return;

		long now  = player.level().getGameTime();
		Long last = fightingSpiritCooldowns.get(player.getUUID());
		if (last != null && now - last < FIGHTING_SPIRIT_COOLDOWN_TICKS) return;

		fightingSpiritCooldowns.put(player.getUUID(), now);
		fightingSpiritActivatedAt.put(player.getUUID(), now);
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Fighting Spirit! 50% damage resistance for 30 seconds. (5 min cooldown)"));
	}

	// ── Fury Unleashed (passive accumulation + active burst) ──────────────────

	private static void accumulateFury(ServerPlayer player, float damageTaken) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!skills.contains("warrior_berserker_fury_unleashed")) return;
		float current = furyPoints.getOrDefault(player.getUUID(), 0.0f);
		furyPoints.put(player.getUUID(), Math.min(100.0f, current + damageTaken));
	}

	public static int getFuryPoints(UUID playerId) {
		return Math.round(furyPoints.getOrDefault(playerId, 0.0f));
	}

	public static void executeFuryUnleashed(ServerPlayer player) {
		long now  = player.level().getGameTime();
		Long last = furyUnleashedCooldowns.get(player.getUUID());
		if (last != null && now - last < FURY_UNLEASHED_COOLDOWN_TICKS) {
			long remaining = FURY_UNLEASHED_COOLDOWN_TICKS - (now - last);
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Fury Unleashed is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		if (furyUnleashedActivatedAt.containsKey(player.getUUID())) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Fury Unleashed is already active."));
			return;
		}
		float fury = furyPoints.getOrDefault(player.getUUID(), 0.0f);
		if (fury < 1.0f) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r No fury stored yet — take damage to build Fury."));
			return;
		}
		furyPoints.put(player.getUUID(), 0.0f);
		furyUnleashedCooldowns.put(player.getUUID(), now);
		furyUnleashedActivatedAt.put(player.getUUID(), now);
		double bonus = fury * 0.01;
		ClassSystemBootstrap.statEngine().setBuffAmount(player.getUUID(), "fury_unleashed_buff", bonus);
		ClassSystemBootstrap.statEngine().activateSource(player.getUUID(), "fury_unleashed_buff");
		int furyInt = Math.round(fury);
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Fury Unleashed! +" + furyInt + "% damage and +" + furyInt +
			"% size for 1 minute. (15 min cooldown)"));
	}

	private static void removeFuryUnleashedBuff(ServerPlayer player) {
		ClassSystemBootstrap.statEngine().deactivateSource(player.getUUID(), "fury_unleashed_buff", player);
		player.sendSystemMessage(Component.literal(
			"\u00a77[Wanderers Haven]\u00a7r Fury Unleashed faded."));
	}

	// ── Circular Slash (Blademaster — active) ─────────────────────────────────

	public static void executeCircularSlash(ServerPlayer player) {
		long now  = player.level().getGameTime();
		Long last = circularSlashCooldowns.get(player.getUUID());
		if (last != null && now - last < CIRCULAR_SLASH_COOLDOWN_TICKS) {
			long remaining = CIRCULAR_SLASH_COOLDOWN_TICKS - (now - last);
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Circular Slash is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		List<LivingEntity> targets = player.level().getEntitiesOfClass(
			LivingEntity.class, player.getBoundingBox().inflate(4.0), e -> e != player);
		if (targets.isEmpty()) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Circular Slash! No enemies in range."));
			return;
		}
		circularSlashCooldowns.put(player.getUUID(), now);
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
		Long last = focusCooldowns.get(player.getUUID());
		if (last != null && now - last < FOCUS_COOLDOWN_TICKS) {
			long remaining = FOCUS_COOLDOWN_TICKS - (now - last);
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Focus is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		if (focusActivatedAt.containsKey(player.getUUID())) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Focus is already active."));
			return;
		}
		focusCooldowns.put(player.getUUID(), now);
		focusActivatedAt.put(player.getUUID(), now);
		ClassSystemBootstrap.statEngine().activateSource(player.getUUID(), "focus_buff");
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Focus! +20% damage and 60% dodge chance for 30 seconds. (2 min cooldown)"));
	}

	/** Returns true (cancelling the hit) if Focus is active and the 60% dodge roll succeeds. */
	public static boolean tryFocusDodge(ServerPlayer player) {
		Long activatedAt = focusActivatedAt.get(player.getUUID());
		if (activatedAt == null) return false;
		if (player.level().getGameTime() - activatedAt >= FOCUS_DURATION_TICKS) return false;
		if (player.level().random.nextFloat() >= 0.60f) return false;
		player.sendSystemMessage(Component.literal(
			"\u00a7e[Wanderers Haven]\u00a7r Focus dodged an attack!"));
		return true;
	}

	// ── Parry (Duelist capstone — active counter-attack) ─────────────────────

	public static void executeParry(ServerPlayer player) {
		parryActivatedAt.put(player.getUUID(), player.level().getGameTime());
		player.sendSystemMessage(Component.literal(
			"\u00a7e[Wanderers Haven]\u00a7r Parry ready!"));
	}

	/** Called from the mixin. Returns true (cancelling damage) if parry fires and counter-attacks. */
	public static boolean tryParry(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!skills.contains("warrior_duelist_parry")) return false;
		Long activatedAt = parryActivatedAt.get(player.getUUID());
		if (activatedAt == null) return false;
		long now = player.level().getGameTime();
		if (now - activatedAt > PARRY_WINDOW_TICKS) {
			parryActivatedAt.remove(player.getUUID());
			return false;
		}
		parryActivatedAt.remove(player.getUUID());
		float counterDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5f;
		List<LivingEntity> nearby = player.level().getEntitiesOfClass(
			LivingEntity.class, player.getBoundingBox().inflate(2.0), e -> e != player);
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
		Long last = flashStepCooldowns.get(player.getUUID());
		if (last != null && now - last < FLASH_STEP_COOLDOWN_TICKS) {
			long remaining = FLASH_STEP_COOLDOWN_TICKS - (now - last);
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Flash Step is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		Vec3 look   = player.getLookAngle();
		Vec3 dest   = findFlashStepDestination(player, look, 5.0);
		float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 3.0f;

		Vec3 start3d = player.position();
		AABB dashBox = new AABB(
			Math.min(start3d.x, dest.x) - 0.6, start3d.y - 0.1, Math.min(start3d.z, dest.z) - 0.6,
			Math.max(start3d.x, dest.x) + 0.6, start3d.y + 2.1, Math.max(start3d.z, dest.z) + 0.6
		);
		List<LivingEntity> inPath = player.level().getEntitiesOfClass(
			LivingEntity.class, dashBox, e -> e != player);
		for (LivingEntity target : inPath) {
			target.hurt(player.damageSources().playerAttack(player), damage);
		}

		player.teleportTo(dest.x, dest.y, dest.z);
		flashStepCooldowns.put(player.getUUID(), now);
		ClassSystemBootstrap.statEngine().activateSource(player.getUUID(), "flash_step_buff");
		flashStepSpeedExpiresAt.put(player.getUUID(), now + FLASH_STEP_SPEED_DURATION_TICKS);

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
		if (!skills.contains("warrior_vanguard_shields_protection")) return false;
		boolean holdingShield =
			player.getMainHandItem().getItem() instanceof net.minecraft.world.item.ShieldItem
			|| player.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem;
		if (!holdingShield) return false;
		long now  = player.level().getGameTime();
		Long last = shieldsProtectionLastBlock.get(player.getUUID());
		if (last != null && now - last < SHIELDS_PROTECTION_COOLDOWN_TICKS) return false;
		shieldsProtectionLastBlock.put(player.getUUID(), now);
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
		Long last = shieldBashCooldowns.get(player.getUUID());
		if (last != null && now - last < SHIELD_BASH_COOLDOWN_TICKS) {
			long remaining = SHIELD_BASH_COOLDOWN_TICKS - (now - last);
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Shield Bash is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		List<LivingEntity> targets = player.level().getEntitiesOfClass(
			LivingEntity.class,
			player.getBoundingBox().inflate(3.5),
			e -> e != player && isInFrontCone(player, e, 3.5, 0.5)
		);
		if (targets.isEmpty()) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Shield Bash! No targets in range."));
			return;
		}
		shieldBashCooldowns.put(player.getUUID(), now);
		long debuffExpiry = now + SHIELD_BASH_ARMOR_DEBUFF_TICKS;
		Vec3 playerPos = player.position();
		int hit = 0;
		for (LivingEntity target : targets) {
			Vec3 dir = target.position().subtract(playerPos).normalize();
			target.knockback(2.0, -dir.x, -dir.z);
			AttributeInstance armorInst = target.getAttribute(Attributes.ARMOR);
			if (armorInst != null) {
				armorInst.removeModifier(SHIELD_BASH_ARMOR_MOD);
				armorInst.addTransientModifier(new AttributeModifier(
					SHIELD_BASH_ARMOR_MOD, -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
				bludgeonedEntities.put(target.getUUID(), debuffExpiry);
			}
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
		if (!skills.contains("warrior_vanguard_stand_your_ground")) return;
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
				.contains("warrior_paladin_aura_of_righteousness")
		).isEmpty();
	}

	public static float getPaladinAuraEnemyMult(LivingEntity target, ServerLevel level) {
		boolean paladinNearby = !level.getEntitiesOfClass(
			ServerPlayer.class,
			target.getBoundingBox().inflate(17.0),
			p -> ClassSystemBootstrap.skillEngine()
				.ownedSkillIds(p.getUUID(), "warrior")
				.contains("warrior_paladin_aura_of_righteousness")
		).isEmpty();
		return paladinNearby ? 1.15f : 1.0f;
	}

	// ── Burning Justice (Paladin — passive on-hit) ────────────────────────────

	public static float getBurningJusticeDamageMult(LivingEntity target, ServerPlayer attacker) {
		if (!ClassSystemBootstrap.skillEngine()
				.ownedSkillIds(attacker.getUUID(), "warrior")
				.contains("warrior_paladin_burning_justice")) return 1.0f;
		return target.getType().is(EntityTypeTags.UNDEAD) ? 1.36f : 1.18f;
	}

	public static void applyBurningJusticeFireOnHit(LivingEntity target, ServerPlayer attacker) {
		if (!ClassSystemBootstrap.skillEngine()
				.ownedSkillIds(attacker.getUUID(), "warrior")
				.contains("warrior_paladin_burning_justice")) return;
		target.igniteForSeconds(5);
	}

	// ── Smite (Paladin — active) ──────────────────────────────────────────────

	public static void executeSmite(ServerPlayer player) {
		long now  = player.level().getGameTime();
		Long last = smiteCooldowns.get(player.getUUID());
		if (last != null && now - last < SMITE_COOLDOWN_TICKS) {
			long remaining = SMITE_COOLDOWN_TICKS - (now - last);
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Smite is on cooldown (" + (remaining / 20) + "s remaining)."));
			return;
		}
		List<LivingEntity> targets = player.level().getEntitiesOfClass(
			LivingEntity.class,
			player.getBoundingBox().inflate(3.5),
			e -> e != player && isInFrontCone(player, e, 3.5, 0.5)
		);
		if (targets.isEmpty()) {
			player.sendSystemMessage(Component.literal(
				"\u00a7e[Wanderers Haven]\u00a7r Smite! No targets in range."));
			return;
		}
		smiteCooldowns.put(player.getUUID(), now);
		ServerLevel level    = (ServerLevel) player.level();
		float   weaponDmg    = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		long    debuffExpiry = now + BLUDGEON_DEBUFF_DURATION_TICKS;
		for (LivingEntity target : targets) {
			boolean isUndead = target.getType().is(EntityTypeTags.UNDEAD);
			target.hurt(player.damageSources().playerAttack(player), weaponDmg * (isUndead ? 5.0f : 2.5f));
			AttributeInstance armorInst = target.getAttribute(Attributes.ARMOR);
			if (armorInst != null) {
				armorInst.removeModifier(BLUDGEON_ARMOR_MOD);
				armorInst.addTransientModifier(new AttributeModifier(
					BLUDGEON_ARMOR_MOD, -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
				bludgeonedEntities.put(target.getUUID(), debuffExpiry);
			}
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

	private static boolean isInFrontCone(ServerPlayer player, LivingEntity entity, double maxDist, double minDot) {
		Vec3 toEntity = entity.position().subtract(player.position());
		double dist = toEntity.length();
		if (dist > maxDist || dist < 0.01) return false;
		return player.getLookAngle().dot(toEntity.normalize()) >= minDot;
	}
}
