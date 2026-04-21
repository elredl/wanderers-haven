package com.wanderershaven.mixin;

import net.minecraft.world.item.BowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public abstract class BowItemLongbowMixin {

	@Inject(method = "getPowerForTime", at = @At("HEAD"), cancellable = true)
	private static void wh_useLongbowPowerCurve(int useTicks, CallbackInfoReturnable<Float> cir) {
		float charge = (float) useTicks / 40.0f;
		float power = (charge * charge + charge * 2.0f) / 3.0f;
		if (power > 1.85f) {
			power = 1.85f;
		}
		cir.setReturnValue(power);
	}
}
