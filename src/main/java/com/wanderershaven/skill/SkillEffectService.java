package com.wanderershaven.skill;

import com.wanderershaven.classsystem.ClassSystemBootstrap;
import com.wanderershaven.network.WanderersHavenNetworking;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.wanderershaven.network.PlaySkillAnimationPayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Applies and removes active skill effects for players.
 *
 * Attribute-based skills (Strength, Dexterity, Speed) are applied as transient
 * AttributeModifiers so they are fresh each session and removed automatically on logout.
 *
 * Event-based skills are split between this service (Second Wind, Last Stand) and
 * mixins (Tough Skin, Lesser Endurance, Lucky Dodge, Poison Resistance, Slow Metabolism).
 */
public final class SkillEffectService {
	private SkillEffectService() {}

	// -------------------------------------------------------------------------
	// Attribute modifier IDs
	// -------------------------------------------------------------------------

	public static final Identifier LESSER_STRENGTH_MOD   = Identifier.fromNamespaceAndPath("wanderers_haven", "lesser_strength");
	public static final Identifier LESSER_DEXTERITY_MOD  = Identifier.fromNamespaceAndPath("wanderers_haven", "lesser_dexterity");
	public static final Identifier LESSER_SPEED_MOD      = Identifier.fromNamespaceAndPath("wanderers_haven", "lesser_speed");
	public static final Identifier LAST_STAND_DAMAGE_MOD = Identifier.fromNamespaceAndPath("wanderers_haven", "last_stand_damage");
	public static final Identifier LAST_STAND_SPEED_MOD  = Identifier.fromNamespaceAndPath("wanderers_haven", "last_stand_speed");
	public static final Identifier HEAVY_STRIKES_MOD     = Identifier.fromNamespaceAndPath("wanderers_haven", "heavy_strikes");
	public static final Identifier BLUDGEON_ARMOR_MOD    = Identifier.fromNamespaceAndPath("wanderers_haven", "bludgeon_armor");

	// -------------------------------------------------------------------------
	// Cooldowns and state
	// -------------------------------------------------------------------------

	/** Lucky Dodge  — 10 min = 12 000 ticks */
	private static final long LUCKY_DODGE_COOLDOWN_TICKS         = 12_000L;
	/** Second Wind (Minor) — 10 min = 12 000 ticks */
	private static final long SECOND_WIND_MINOR_COOLDOWN_TICKS   = 12_000L;
	/** Second Wind (Lesser) — 8 min = 9 600 ticks */
	private static final long SECOND_WIND_LESSER_COOLDOWN_TICKS  = 9_600L;
	/** Last Stand (Minor) — 10 min = 12 000 ticks */
	private static final long LAST_STAND_MINOR_COOLDOWN_TICKS    = 12_000L;
	/** Last Stand (Lesser) — 8 min = 9 600 ticks */
	private static final long LAST_STAND_LESSER_COOLDOWN_TICKS   = 9_600L;
	/** Last Stand buff duration — 10 seconds = 200 ticks */
	private static final long LAST_STAND_DURATION_TICKS          = 200L;

	/** Heavy Strikes — 30 sec cooldown = 600 ticks, 10 sec duration = 200 ticks */
	private static final long HEAVY_STRIKES_COOLDOWN_TICKS  = 600L;
	private static final long HEAVY_STRIKES_DURATION_TICKS  = 200L;
	/** Battle Cry (Weak) — 45 sec cooldown = 900 ticks, 8 sec debuff duration = 160 ticks */
	private static final long BATTLE_CRY_WEAK_COOLDOWN_TICKS = 900L;
	private static final long BATTLE_CRY_WEAK_DURATION_TICKS = 160L;
	/** Bludgeon — 15 sec cooldown = 300 ticks, 5 sec armor debuff = 100 ticks */
	private static final long BLUDGEON_COOLDOWN_TICKS        = 300L;
	private static final long BLUDGEON_DEBUFF_DURATION_TICKS = 100L;
	/** Piercing Charge — 20 sec cooldown = 400 ticks */
	private static final long PIERCING_CHARGE_COOLDOWN_TICKS = 400L;

	private static final Map<UUID, Long> luckyDodgeCooldowns   = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> secondWindCooldowns   = new ConcurrentHashMap<>();
	/** Maps player UUID → game tick when Last Stand was activated (buff expiry reference). */
	private static final Map<UUID, Long> lastStandCooldowns    = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> lastStandActivatedAt  = new ConcurrentHashMap<>();

	private static final Map<UUID, Long> heavyStrikesCooldowns   = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> heavyStrikesActivatedAt = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> battleCryWeakCooldowns  = new ConcurrentHashMap<>();
	/** Maps debuffed entity UUID → game tick when the Battle Cry (Weak) debuff expires. Cleaned lazily and via tick event. */
	private static final Map<UUID, Long> battleCryDebuffedEntities = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> bludgeonCooldowns       = new ConcurrentHashMap<>();
	/** Maps bludgeoned entity UUID → game tick when the armor reduction expires. */
	private static final Map<UUID, Long> bludgeonedEntities      = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> piercingChargeCooldowns = new ConcurrentHashMap<>();

	// -------------------------------------------------------------------------
	// Registration
	// -------------------------------------------------------------------------

	/** Register all event listeners. Call during mod initialisation. */
	public static void register() {
		// Apply attribute effects and sync skills when a player joins
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			applyAll(player);
			WanderersHavenNetworking.sendSkillsTo(player);
		});

		// Second Wind + Last Stand — trigger when health crosses below 40 %
		ServerLivingEntityEvents.AFTER_DAMAGE.register(
			(entity, source, baseDamage, actualDamage, blocked) -> {
				if (!(entity instanceof ServerPlayer player)) return;
				float ratio = player.getHealth() / player.getMaxHealth();
				if (ratio < 0.4f) {
					trySecondWind(player);
					tryActivateLastStand(player);
				}
			}
		);

		// Last Stand — remove buff after 10 seconds (200 ticks)
		ServerTickEvents.END_SERVER_TICK.register(server ->
			lastStandActivatedAt.entrySet().removeIf(entry -> {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) return true; // logged out — clean up silently
				long elapsed = player.level().getGameTime() - entry.getValue();
				if (elapsed >= LAST_STAND_DURATION_TICKS) {
					removeLastStand(player);
					return true;
				}
				return false;
			})
		);

		// Heavy Strikes — remove +12% damage buff after 10 seconds (200 ticks)
		ServerTickEvents.END_SERVER_TICK.register(server ->
			heavyStrikesActivatedAt.entrySet().removeIf(entry -> {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player == null) return true;
				long elapsed = player.level().getGameTime() - entry.getValue();
				if (elapsed >= HEAVY_STRIKES_DURATION_TICKS) {
					removeHeavyStrikes(player);
					return true;
				}
				return false;
			})
		);

		// Bludgeon — remove armor reduction from hit entities after 5 seconds (100 ticks)
		ServerTickEvents.END_SERVER_TICK.register(server ->
			bludgeonedEntities.entrySet().removeIf(entry -> {
				for (ServerLevel sl : server.getAllLevels()) {
					net.minecraft.world.entity.Entity entity = sl.getEntity(entry.getKey());
					if (entity instanceof LivingEntity living) {
						if (sl.getGameTime() >= entry.getValue()) {
							AttributeInstance armorInst = living.getAttribute(Attributes.ARMOR);
							if (armorInst != null) armorInst.removeModifier(BLUDGEON_ARMOR_MOD);
							return true;
						}
						return false;
					}
				}
				return true; // entity not found — clean up
			})
		);
	}

	// -------------------------------------------------------------------------
	// Apply effects on join / skill grant
	// -------------------------------------------------------------------------

	/** Apply all owned skill attribute effects for a player (called on join). */
	public static void applyAll(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		for (String id : skills) {
			applySkill(player, id);
		}
	}

	/**
	 * Apply a single skill's attribute effect immediately.
	 * Call this right after granting a skill so the player feels it straight away.
	 * Pure-event skills (cactus immunity, Lucky Dodge, Poison Resistance, Slow Metabolism)
	 * are checked dynamically in the mixin — no explicit apply step needed.
	 */
	public static void applySkill(ServerPlayer player, String skillId) {
		switch (skillId) {
			case "warrior_lesser_strength" -> applyAttr(
				player, Attributes.ATTACK_DAMAGE, LESSER_STRENGTH_MOD,
				0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

			// Enhanced version reuses the same modifier ID — removes and re-adds at higher value
			case "warrior_enhanced_strength" -> applyAttr(
				player, Attributes.ATTACK_DAMAGE, LESSER_STRENGTH_MOD,
				0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

			case "warrior_lesser_dexterity" -> applyAttr(
				player, Attributes.ATTACK_SPEED, LESSER_DEXTERITY_MOD,
				0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

			case "warrior_enhanced_dexterity" -> applyAttr(
				player, Attributes.ATTACK_SPEED, LESSER_DEXTERITY_MOD,
				0.18, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

			case "warrior_lesser_speed" -> applyAttr(
				player, Attributes.MOVEMENT_SPEED, LESSER_SPEED_MOD,
				0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

			case "warrior_enhanced_speed" -> applyAttr(
				player, Attributes.MOVEMENT_SPEED, LESSER_SPEED_MOD,
				0.18, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

			// Damage-reduction and event skills are checked dynamically — no apply step needed
		}
	}

	// -------------------------------------------------------------------------
	// Mixin helpers
	// -------------------------------------------------------------------------

	/**
	 * Incoming damage multiplier from damage-reduction skills.
	 * Stacks multiplicatively: ToughSkin(×0.95) × LesserEndurance(×0.90) = 14.5 % total.
	 */
	/**
	 * Returns a damage multiplier for the player given the incoming source.
	 * Pass {@code null} for source when the source is unavailable (legacy call sites).
	 *
	 * Arrow reduction for Iron Skin mirrors the Projectile Protection enchantment:
	 * damage is reduced before it is applied, not healed back afterwards.
	 */
	public static float getDamageMultiplier(ServerPlayer player, DamageSource source) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		float mult = 1.0f;
		// Iron Skin supersedes Tough Skin — only one can be owned at a time
		if      (skills.contains("warrior_iron_skin"))         mult *= 0.88f;
		else if (skills.contains("warrior_tough_skin"))        mult *= 0.95f;
		// Arrow-specific reduction for Iron Skin (pre-application, same as Projectile Protection)
		if (source != null
				&& source.getDirectEntity() instanceof AbstractArrow
				&& skills.contains("warrior_iron_skin")) {
			mult *= 0.90f;
		}
		// Enhanced Endurance supersedes Lesser Endurance
		if      (skills.contains("warrior_enhanced_endurance")) mult *= 0.82f;
		else if (skills.contains("warrior_lesser_endurance"))   mult *= 0.90f;
		return mult;
	}

	/** True if the player has Tough Skin or Iron Skin (cactus immunity). */
	public static boolean hasCactusImmunity(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		return skills.contains("warrior_tough_skin") || skills.contains("warrior_iron_skin");
	}

	/**
	 * Attempt to trigger Lucky Dodge.
	 * Returns true and starts the 10-minute cooldown if the dodge fires.
	 */
	public static boolean tryLuckyDodge(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (!skills.contains("warrior_lucky_dodge")) return false;
		long now = player.level().getGameTime();
		Long last = luckyDodgeCooldowns.get(player.getUUID());
		if (last != null && now - last < LUCKY_DODGE_COOLDOWN_TICKS) return false;
		luckyDodgeCooldowns.put(player.getUUID(), now);
		return true;
	}

	/**
	 * Duration multiplier for poison effects (checked from the addEffect mixin).
	 * Returns 0.80 for Lesser Poison Resistance, 0.90 for Minor, 1.0 if neither.
	 */
	public static float getPoisonDurationMultiplier(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		if (skills.contains("warrior_lesser_poison_resistance")) return 0.80f;
		if (skills.contains("warrior_minor_poison_resistance"))  return 0.90f;
		return 1.0f;
	}

	/** True if the player has Slow Metabolism (checked from the causeFoodExhaustion mixin). */
	public static boolean hasSlowMetabolism(ServerPlayer player) {
		return ClassSystemBootstrap.skillEngine()
			.ownedSkillIds(player.getUUID(), "warrior")
			.contains("warrior_slow_metabolism");
	}

	// -------------------------------------------------------------------------
	// Second Wind
	// -------------------------------------------------------------------------

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
			player.heal(9.0f); // 4.5 hearts = 9 HP
			player.sendSystemMessage(Component.literal(
				"\u00a76[Wanderers Haven]\u00a7r Second Wind! Recovered 4.5 hearts. (8 min cooldown)"
			));
		} else {
			player.heal(6.0f); // 3 hearts = 6 HP
			player.sendSystemMessage(Component.literal(
				"\u00a76[Wanderers Haven]\u00a7r Second Wind! Recovered 3 hearts. (10 min cooldown)"
			));
		}
	}

	// -------------------------------------------------------------------------
	// Last Stand
	// -------------------------------------------------------------------------

	private static void tryActivateLastStand(ServerPlayer player) {
		Set<String> skills = ClassSystemBootstrap.skillEngine().ownedSkillIds(player.getUUID(), "warrior");
		boolean hasLesser = skills.contains("warrior_last_stand_lesser");
		boolean hasMinor  = skills.contains("warrior_last_stand_minor");
		if (!hasLesser && !hasMinor) return;
		if (lastStandActivatedAt.containsKey(player.getUUID())) return; // buff already running

		long now = player.level().getGameTime();
		Long last = lastStandCooldowns.get(player.getUUID());
		long cooldown = hasLesser ? LAST_STAND_LESSER_COOLDOWN_TICKS : LAST_STAND_MINOR_COOLDOWN_TICKS;
		if (last != null && now - last < cooldown) return; // on cooldown

		activateLastStand(player, hasLesser);
	}

	private static void activateLastStand(ServerPlayer player, boolean lesser) {
		long now = player.level().getGameTime();
		lastStandActivatedAt.put(player.getUUID(), now);
		lastStandCooldowns.put(player.getUUID(), now);
		double boost = lesser ? 0.12 : 0.08;
		applyAttr(player, Attributes.ATTACK_DAMAGE, LAST_STAND_DAMAGE_MOD,
			boost, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		applyAttr(player, Attributes.ATTACK_SPEED,  LAST_STAND_SPEED_MOD,
			boost, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		if (lesser) {
			player.sendSystemMessage(Component.literal(
				"\u00a7c[Wanderers Haven]\u00a7r Last Stand! +12% attack damage and speed for 10 seconds. (8 min cooldown)"
			));
		} else {
			player.sendSystemMessage(Component.literal(
				"\u00a7c[Wanderers Haven]\u00a7r Last Stand! +8% attack damage and speed for 10 seconds. (10 min cooldown)"
			));
		}
	}

	private static void removeLastStand(ServerPlayer player) {
		AttributeInstance dmgInst = player.getAttribute(Attributes.ATTACK_DAMAGE);
		if (dmgInst != null) dmgInst.removeModifier(LAST_STAND_DAMAGE_MOD);
		AttributeInstance spdInst = player.getAttribute(Attributes.ATTACK_SPEED);
		if (spdInst != null) spdInst.removeModifier(LAST_STAND_SPEED_MOD);
		player.sendSystemMessage(Component.literal(
			"\u00a77[Wanderers Haven]\u00a7r Last Stand faded."
		));
	}

	// -------------------------------------------------------------------------
	// Heavy Strikes (active)
	// -------------------------------------------------------------------------

	public static void executeHeavyStrikes(ServerPlayer player) {
		long now = player.level().getGameTime();
		Long last = heavyStrikesCooldowns.get(player.getUUID());
		if (last != null && now - last < HEAVY_STRIKES_COOLDOWN_TICKS) {
			long remaining = HEAVY_STRIKES_COOLDOWN_TICKS - (now - last);
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Heavy Strikes is on cooldown (" + (remaining / 20) + "s remaining)."
			));
			return;
		}
		if (heavyStrikesActivatedAt.containsKey(player.getUUID())) return; // already active
		heavyStrikesActivatedAt.put(player.getUUID(), now);
		heavyStrikesCooldowns.put(player.getUUID(), now);
		applyAttr(player, Attributes.ATTACK_DAMAGE, HEAVY_STRIKES_MOD,
			0.12, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Heavy Strikes! +12% damage for 10 seconds. (30 sec cooldown)"
		));
	}

	private static void removeHeavyStrikes(ServerPlayer player) {
		AttributeInstance inst = player.getAttribute(Attributes.ATTACK_DAMAGE);
		if (inst != null) inst.removeModifier(HEAVY_STRIKES_MOD);
		player.sendSystemMessage(Component.literal(
			"\u00a77[Wanderers Haven]\u00a7r Heavy Strikes faded."
		));
	}

	// -------------------------------------------------------------------------
	// Battle Cry (Weak) (active)
	// -------------------------------------------------------------------------

	public static void executeBattleCryWeak(ServerPlayer player) {
		long now = player.level().getGameTime();
		Long last = battleCryWeakCooldowns.get(player.getUUID());
		if (last != null && now - last < BATTLE_CRY_WEAK_COOLDOWN_TICKS) {
			long remaining = BATTLE_CRY_WEAK_COOLDOWN_TICKS - (now - last);
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Battle Cry is on cooldown (" + (remaining / 20) + "s remaining)."
			));
			return;
		}
		battleCryWeakCooldowns.put(player.getUUID(), now);
		long expiresAt = now + BATTLE_CRY_WEAK_DURATION_TICKS;
		// Angry particle burst visible to all nearby players
		ServerLevel serverLevel = (ServerLevel) player.level();
		double px = player.getX();
		double py = player.getY() + 1.0;
		double pz = player.getZ();
		serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, px, py, pz, 40, 1.5, 0.8, 1.5, 0.4);
		serverLevel.sendParticles(ParticleTypes.CRIT,           px, py, pz, 25, 1.5, 0.8, 1.5, 0.6);
		List<LivingEntity> nearby = player.level().getEntitiesOfClass(
			LivingEntity.class,
			player.getBoundingBox().inflate(3.5),
			e -> e != player && (e instanceof Monster || (e instanceof Player && e != player))
		);
		if (nearby.isEmpty()) {
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Battle Cry! No enemies within range."
			));
			return;
		}
		for (LivingEntity enemy : nearby) {
			battleCryDebuffedEntities.put(enemy.getUUID(), expiresAt);
		}
		int count = nearby.size();
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Battle Cry! " + count + " enem" + (count == 1 ? "y" : "ies") +
			" weakened for 8 seconds. (45 sec cooldown)"
		));
	}

	/**
	 * Returns 0.92f if the attacker is battle-cry debuffed (they deal 8% less damage), 1.0f otherwise.
	 * Called from the mixin when a player is hit.
	 */
	public static float getBattleCryAttackerMult(UUID attackerUUID, long gameTime) {
		Long expiresAt = battleCryDebuffedEntities.get(attackerUUID);
		if (expiresAt == null) return 1.0f;
		if (gameTime >= expiresAt) {
			battleCryDebuffedEntities.remove(attackerUUID);
			return 1.0f;
		}
		return 0.92f;
	}

	/**
	 * Returns 1.08f if the target is battle-cry debuffed (they take 8% more damage), 1.0f otherwise.
	 * Called from the mixin when any LivingEntity is hurt.
	 */
	public static float getBattleCryTargetMult(UUID targetUUID, long gameTime) {
		Long expiresAt = battleCryDebuffedEntities.get(targetUUID);
		if (expiresAt == null) return 1.0f;
		if (gameTime >= expiresAt) {
			battleCryDebuffedEntities.remove(targetUUID);
			return 1.0f;
		}
		return 1.08f;
	}

	// -------------------------------------------------------------------------
	// Bludgeon (active)
	// -------------------------------------------------------------------------

	public static void executeBludgeon(ServerPlayer player) {
		long now = player.level().getGameTime();
		Long last = bludgeonCooldowns.get(player.getUUID());
		if (last != null && now - last < BLUDGEON_COOLDOWN_TICKS) {
			long remaining = BLUDGEON_COOLDOWN_TICKS - (now - last);
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Bludgeon is on cooldown (" + (remaining / 20) + "s remaining)."
			));
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
				"\u00a77[Wanderers Haven]\u00a7r Bludgeon! No targets in range."
			));
			return;
		}
		float weaponDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		long expiresAt = now + BLUDGEON_DEBUFF_DURATION_TICKS;
		ServerLevel level = (ServerLevel) player.level();
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
			", armor reduced by 15% for 5 seconds. (15 sec cooldown)"
		));
	}

	// -------------------------------------------------------------------------
	// Piercing Charge (active)
	// -------------------------------------------------------------------------

	public static void executePiercingCharge(ServerPlayer player) {
		long now = player.level().getGameTime();
		Long last = piercingChargeCooldowns.get(player.getUUID());
		if (last != null && now - last < PIERCING_CHARGE_COOLDOWN_TICKS) {
			long remaining = PIERCING_CHARGE_COOLDOWN_TICKS - (now - last);
			player.sendSystemMessage(Component.literal(
				"\u00a77[Wanderers Haven]\u00a7r Piercing Charge is on cooldown (" + (remaining / 20) + "s remaining)."
			));
			return;
		}
		piercingChargeCooldowns.put(player.getUUID(), now);
		Vec3 look = player.getLookAngle();
		Vec3 startPos = player.position();
		Vec3 endPos = startPos.add(look.scale(5.0));
		// Detect entities along the expected dash path and damage them immediately
		AABB dashBox = new AABB(
			Math.min(startPos.x, endPos.x) - 0.6, startPos.y - 0.1, Math.min(startPos.z, endPos.z) - 0.6,
			Math.max(startPos.x, endPos.x) + 0.6, startPos.y + 2.1, Math.max(startPos.z, endPos.z) + 0.6
		);
		List<LivingEntity> inPath = player.level().getEntitiesOfClass(
			LivingEntity.class, dashBox, e -> e != player
		);
		float weaponDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.3f;
		int hit = 0;
		for (LivingEntity target : inPath) {
			target.hurt(player.damageSources().playerAttack(player), weaponDamage);
			hit++;
		}
		// Launch the player as a velocity-based dash (~2.2 blocks/tick, decays to ~5 blocks on ground)
		// Wall collision resolves naturally through the physics engine
		player.setDeltaMovement(look.scale(2.2));
		player.hurtMarked = true; // force velocity packet sync to client
		player.sendSystemMessage(Component.literal(
			"\u00a7c[Wanderers Haven]\u00a7r Piercing Charge!" +
			(hit > 0 ? " Hit " + hit + " enem" + (hit == 1 ? "y" : "ies") + "." : "") +
			" (20 sec cooldown)"
		));
	}

	// -------------------------------------------------------------------------
	// Internal helpers
	// -------------------------------------------------------------------------

	/** True if {@code entity} is within {@code maxDist} and within the look cone (dot ≥ minDot). */
	private static boolean isInFrontCone(ServerPlayer player, LivingEntity entity, double maxDist, double minDot) {
		Vec3 toEntity = entity.position().subtract(player.position());
		double dist = toEntity.length();
		if (dist > maxDist || dist < 0.01) return false;
		return player.getLookAngle().dot(toEntity.normalize()) >= minDot;
	}

	private static void applyAttr(
		ServerPlayer player,
		Holder<Attribute> attribute,
		Identifier modifierId,
		double amount,
		AttributeModifier.Operation operation
	) {
		AttributeInstance inst = player.getAttribute(attribute);
		if (inst == null) return;
		inst.removeModifier(modifierId);
		inst.addTransientModifier(new AttributeModifier(modifierId, amount, operation));
	}
}
