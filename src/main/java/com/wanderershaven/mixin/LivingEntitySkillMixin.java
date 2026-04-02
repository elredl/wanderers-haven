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
			if (attacker != null) {
				amount *= SkillEffectService.getBattleCryAttackerMult(attacker.getUUID(), level.getGameTime());
			}
			// Pre-application damage reduction (same mechanic as Projectile Protection)
			float mult = SkillEffectService.getDamageMultiplier(player, source);
			return original.call(level, source, amount * mult);
		}
		// Aura of Righteousness — enemies within 17 blocks of a paladin take 15% more damage
		amount *= SkillEffectService.getPaladinAuraEnemyMult((LivingEntity)(Object) this, level);
		// Burning Justice — paladin's strikes deal 18% bonus damage (36% for undead) and ignite
		if (source.getEntity() instanceof ServerPlayer attacker) {
			amount *= SkillEffectService.getBurningJusticeDamageMult((LivingEntity)(Object) this, attacker);
			SkillEffectService.applyBurningJusticeFireOnHit((LivingEntity)(Object) this, attacker);
		}
		return original.call(level, source, amount);
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
		if (effect.getEffect() != MobEffects.POISON) return effect;
		float mult = SkillEffectService.getPoisonDurationMultiplier(player);
		if (mult >= 1.0f) return effect;
		int shorter = Math.max(1, (int) (effect.getDuration() * mult));
		return new MobEffectInstance(
			effect.getEffect(), shorter, effect.getAmplifier(),
			effect.isAmbient(), effect.isVisible(), effect.showIcon()
		);
	}
}
