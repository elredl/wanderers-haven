package com.wanderershaven.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.wanderershaven.skill.SkillEffectService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Handles damage- and effect-related warrior skill hooks on LivingEntity.
 *
 * Uses {@code @WrapMethod} (MixinExtras) to wrap {@code hurtServer} so that
 * both the DamageSource and the damage amount are accessible in one place —
 * this mirrors how Projectile Protection reduces damage pre-application rather
 * than healing afterwards.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySkillMixin {

	// ── hurtServer wrapper: cancellations + pre-application damage reduction ─

	@WrapMethod(method = "hurtServer")
	private boolean wh_hurtServer(
		ServerLevel level, DamageSource source, float amount, Operation<Boolean> original
	) {
		// Battle Cry (Weak) — debuffed entities take 8% more damage from any source
		UUID selfId = ((LivingEntity)(Object) this).getUUID();
		amount *= SkillEffectService.getBattleCryTargetMult(selfId, level.getGameTime());

		if ((Object) this instanceof ServerPlayer player) {
			SkillEffectService.markInCombat(player);
			if (SkillEffectService.isShadowStepActive(player)) {
				return false;
			}
			// Focus — 60% dodge chance while buff is active
			if (SkillEffectService.tryFocusDodge(player)) {
				return false;
			}
			// Parry — counter-attack and deflect if the 0.5-sec window is active
			if (SkillEffectService.tryParry(player)) {
				return false;
			}
			// Shield's Protection — auto-block one attack every 7 sec while holding a shield
			if (SkillEffectService.tryShieldsProtectionBlock(player)) {
				return false;
			}
			// Cactus immunity (Tough Skin / Iron Skin)
			if (source.is(DamageTypes.CACTUS) && SkillEffectService.hasCactusImmunity(player)) {
				return false;
			}
			// Lucky Dodge — absorb first incoming projectile
			if (source.getDirectEntity() instanceof Projectile && SkillEffectService.tryLuckyDodge(player)) {
				player.sendSystemMessage(Component.literal(
					"\u00a7c[Wanderers Haven]\u00a7r Lucky Dodge! Projectile absorbed. (10 min cooldown)"
				));
				return false;
			}
			// Battle Cry (Weak) — debuffed attackers deal 8% less damage to this player
			Entity attacker = source.getEntity();
			ServerPlayer attackingPlayer = attacker instanceof ServerPlayer sp ? sp : null;
			if (attackingPlayer != null) {
				amount *= SkillEffectService.getOutgoingDamageMultiplier(attackingPlayer);
				amount *= SkillEffectService.getAttackerSkillDamageMultiplier(attackingPlayer, player, source, amount);
			}
			if (attacker != null) {
				amount *= SkillEffectService.getBattleCryAttackerMult(attacker.getUUID(), level.getGameTime());
				if (attacker instanceof ServerPlayer serverAttacker) {
					amount *= SkillEffectService.getSpearmasterDamageMultiplier(serverAttacker, player);
					amount *= SkillEffectService.getHeadhunterDamageMultiplier(serverAttacker);
					if (source.is(DamageTypes.PLAYER_ATTACK)) {
						SkillEffectService.onSuccessfulPlayerMeleeHit(serverAttacker, player, amount);
					}
				}
			}
			amount *= SkillEffectService.getDamageTypeResistanceMultiplier(player, source);
			// Pre-application damage reduction (same mechanic as Projectile Protection)
			float mult = SkillEffectService.getDamageMultiplier(player, source);
			float finalAmount = amount * mult;
			boolean applied = original.call(level, source, finalAmount);
			if (attackingPlayer != null) {
				SkillEffectService.markInCombat(attackingPlayer);
				if (applied && finalAmount > 0.0f) {
					SkillEffectService.recordSuccessfulHit(attackingPlayer);
				}
				SkillEffectService.applyReapLifeLifesteal(attackingPlayer, finalAmount, applied);
			}
			return applied;
		}
		// Aura of Righteousness — enemies within 17 blocks of a paladin take 15% more damage
		amount *= SkillEffectService.getPaladinAuraEnemyMult((LivingEntity)(Object) this, level);
		// Burning Justice — paladin's strikes deal 18% bonus damage (36% for undead) and ignite
		if (source.getEntity() instanceof ServerPlayer attacker) {
			SkillEffectService.markInCombat(attacker);
			amount *= SkillEffectService.getOutgoingDamageMultiplier(attacker);
			amount *= SkillEffectService.getAttackerSkillDamageMultiplier(attacker, (LivingEntity)(Object) this, source, amount);
			amount *= SkillEffectService.getSpearmasterDamageMultiplier(attacker, (LivingEntity)(Object) this);
			amount *= SkillEffectService.getHeadhunterDamageMultiplier(attacker);
			amount *= SkillEffectService.getBurningJusticeDamageMult((LivingEntity)(Object) this, attacker);
			SkillEffectService.applyBurningJusticeFireOnHit((LivingEntity)(Object) this, attacker);
			if (source.is(DamageTypes.PLAYER_ATTACK)) {
				SkillEffectService.onSuccessfulPlayerMeleeHit(attacker, (LivingEntity)(Object) this, amount);
			}
			boolean applied = original.call(level, source, amount);
			if (applied && amount > 0.0f) {
				SkillEffectService.recordSuccessfulHit(attacker);
			}
			SkillEffectService.applyReapLifeLifesteal(attacker, amount, applied);
			return applied;
		}
		return original.call(level, source, amount);
	}

	@ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float wh_reduceHealingWhileBleeding(float amount) {
		return SkillEffectService.modifyIncomingHealing((LivingEntity)(Object) this, amount);
	}

	// ── Poison Resistance: reduce poison effect duration ─────────────────────

	@ModifyVariable(
		method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
		at = @At("HEAD"),
		argsOnly = true,
		ordinal = 0
	)
	private MobEffectInstance wh_modifyPoisonDuration(MobEffectInstance effect) {
		if (!((Object) this instanceof ServerPlayer player)) return effect;
		float mult = 1.0f;
		if (effect.getEffect() == MobEffects.POISON) {
			mult = SkillEffectService.getPoisonDurationMultiplier(player);
		} else if (effect.getEffect() == MobEffects.WEAKNESS
				|| effect.getEffect() == MobEffects.NAUSEA
				|| effect.getEffect() == MobEffects.SLOWNESS) {
			mult = SkillEffectService.getMagicDebuffDurationMultiplier(player);
		}
		if (mult >= 1.0f) return effect;
		int shorter = Math.max(1, (int) (effect.getDuration() * mult));
		return new MobEffectInstance(
			effect.getEffect(), shorter, effect.getAmplifier(),
			effect.isAmbient(), effect.isVisible(), effect.showIcon()
		);
	}
}
