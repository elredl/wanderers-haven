package com.wanderershaven.mixin;

import com.wanderershaven.skill.SkillEffectService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Handles damage- and effect-related warrior skill hooks on LivingEntity:
 *   Tough Skin / Iron Skin  -- damage reduction + cactus immunity
 *   Lesser Endurance        -- damage reduction
 *   Lucky Dodge             -- cancel first projectile hit (10 min cooldown)
 *   Poison Resistance tiers -- reduce poison duration
 *
 * In MC 1.21.11 the old hurt(DamageSource, float) was replaced by
 * hurtServer(ServerLevel, DamageSource, float).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySkillMixin {

	// ── Damage cancellation (cactus immunity + Lucky Dodge) ──────────────────

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void wh_onHurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof ServerPlayer player)) return;

		// Tough Skin / Iron Skin: cactus immunity
		if (source.is(DamageTypes.CACTUS) && SkillEffectService.hasCactusImmunity(player)) {
			cir.setReturnValue(false);
			return;
		}

		// Lucky Dodge: absorb the first incoming projectile (10 min cooldown)
		if (source.getDirectEntity() instanceof Projectile && SkillEffectService.tryLuckyDodge(player)) {
			player.sendSystemMessage(Component.literal(
				"\u00a7c[Wanderers Haven]\u00a7r Lucky Dodge! Projectile absorbed. (10 min cooldown)"
			));
			cir.setReturnValue(false);
		}
	}

	// ── Damage amount reduction ───────────────────────────────────────────────

	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float wh_modifyIncomingDamage(float amount) {
		if (!((Object) this instanceof ServerPlayer player)) return amount;
		return amount * SkillEffectService.getDamageMultiplier(player);
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
