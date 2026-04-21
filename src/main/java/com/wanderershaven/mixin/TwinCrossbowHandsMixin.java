package com.wanderershaven.mixin;

import com.wanderershaven.item.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class TwinCrossbowHandsMixin {

	@Inject(method = "getItemInHand", at = @At("HEAD"), cancellable = true)
	private void wh_overrideDualWieldVisuals(InteractionHand hand, CallbackInfoReturnable<ItemStack> cir) {
		if (!((Object) this instanceof Player player)) {
			return;
		}

		ItemStack mainHand = player.getMainHandItem();
		ItemStack offHand = player.getOffhandItem();
		if (hand == InteractionHand.OFF_HAND && mainHand.getItem() == ModItems.TWIN_CROSSBOW) {
			cir.setReturnValue(mainHand);
		} else if (hand == InteractionHand.MAIN_HAND && offHand.getItem() == ModItems.TWIN_CROSSBOW) {
			cir.setReturnValue(offHand);
		} else if (hand == InteractionHand.OFF_HAND && mainHand.getItem() == ModItems.SHORTBOW_BUCKLER) {
			cir.setReturnValue(new ItemStack(Items.BOW));
		} else if (hand == InteractionHand.MAIN_HAND && offHand.getItem() == ModItems.SHORTBOW_BUCKLER) {
			cir.setReturnValue(new ItemStack(Items.SHIELD));
		}
	}
}
