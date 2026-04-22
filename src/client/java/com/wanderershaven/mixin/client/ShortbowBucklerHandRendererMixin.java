package com.wanderershaven.mixin.client;

import com.wanderershaven.item.ModItems;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemInHandRenderer.class)
public abstract class ShortbowBucklerHandRendererMixin {

	@Redirect(
		method = "renderArmWithItem",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUsedItemHand()Lnet/minecraft/world/InteractionHand;"
		)
	)
	private InteractionHand wh_forceShortbowAsOffhandUse(AbstractClientPlayer player) {
		if (player.isUsingItem() && player.getMainHandItem().getItem() == ModItems.SHORTBOW_BUCKLER) {
			return InteractionHand.OFF_HAND;
		}
		return player.getUsedItemHand();
	}
}
