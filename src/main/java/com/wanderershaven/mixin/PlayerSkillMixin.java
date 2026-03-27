package com.wanderershaven.mixin;

import com.wanderershaven.skill.SkillEffectService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Handles player-specific warrior skill hooks:
 *   Slow Metabolism — hunger exhaustion depletes 10% slower
 */
@Mixin(Player.class)
public abstract class PlayerSkillMixin {

	@ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float wh_modifyExhaustion(float exhaustion) {
		if (!((Object) this instanceof ServerPlayer player)) return exhaustion;
		if (!SkillEffectService.hasSlowMetabolism(player)) return exhaustion;
		return exhaustion * 0.9f;
	}
}
