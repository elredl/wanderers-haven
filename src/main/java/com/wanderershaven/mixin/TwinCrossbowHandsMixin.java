package com.wanderershaven.mixin;

import com.wanderershaven.item.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
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
	private static ItemStack shortbowVisualBow;

	private static ItemStack shortbowVisualBow() {
		if (shortbowVisualBow == null) {
			shortbowVisualBow = Items.BOW.getDefaultInstance();
		}
		return shortbowVisualBow;
	}

	@Inject(method = "getOffhandItem", at = @At("HEAD"), cancellable = true)
	private void wh_overrideOffhandVisual(CallbackInfoReturnable<ItemStack> cir) {
		if (!((Object) this instanceof Player player) || !player.level().isClientSide()) {
			return;
		}
		ItemStack mainHand = player.getItemBySlot(EquipmentSlot.MAINHAND);
		ItemStack offHand = player.getItemBySlot(EquipmentSlot.OFFHAND);
		if (mainHand.getItem() == ModItems.TWIN_CROSSBOW && offHand.getItem() != ModItems.TWIN_CROSSBOW) {
			cir.setReturnValue(mainHand);
			return;
		}
		if (mainHand.getItem() == ModItems.SHORTBOW_BUCKLER) {
			cir.setReturnValue(shortbowVisualBow());
		}
	}

	@Inject(method = "getMainHandItem", at = @At("HEAD"), cancellable = true)
	private void wh_overrideMainhandVisual(CallbackInfoReturnable<ItemStack> cir) {
		if (!((Object) this instanceof Player player) || !player.level().isClientSide()) {
			return;
		}
		ItemStack mainHand = player.getItemBySlot(EquipmentSlot.MAINHAND);
		ItemStack offHand = player.getItemBySlot(EquipmentSlot.OFFHAND);
		if (offHand.getItem() == ModItems.TWIN_CROSSBOW && mainHand.getItem() != ModItems.TWIN_CROSSBOW) {
			cir.setReturnValue(offHand);
			return;
		}
	}

	@Inject(method = "getItemInHand", at = @At("HEAD"), cancellable = true)
	private void wh_overrideDualWieldVisuals(InteractionHand hand, CallbackInfoReturnable<ItemStack> cir) {
		if (!((Object) this instanceof Player player)) {
			return;
		}
		// Visual-only hand spoofing: never affect server-side gameplay state.
		if (!player.level().isClientSide()) {
			return;
		}

		ItemStack mainHand = player.getItemBySlot(EquipmentSlot.MAINHAND);
		ItemStack offHand = player.getItemBySlot(EquipmentSlot.OFFHAND);
		if (hand == InteractionHand.OFF_HAND && mainHand.getItem() == ModItems.TWIN_CROSSBOW) {
			cir.setReturnValue(mainHand);
		} else if (hand == InteractionHand.MAIN_HAND && offHand.getItem() == ModItems.TWIN_CROSSBOW) {
			cir.setReturnValue(offHand);
		} else if (hand == InteractionHand.OFF_HAND && mainHand.getItem() == ModItems.SHORTBOW_BUCKLER) {
			cir.setReturnValue(shortbowVisualBow());
		}
	}

	@Inject(method = "getUsedItemHand", at = @At("HEAD"), cancellable = true)
	private void wh_forceOffhandUseForShortbowBuckler(CallbackInfoReturnable<InteractionHand> cir) {
		if (!((Object) this instanceof Player player) || !player.level().isClientSide()) {
			return;
		}
		if (player.isUsingItem() && player.getMainHandItem().getItem() == ModItems.SHORTBOW_BUCKLER) {
			cir.setReturnValue(InteractionHand.OFF_HAND);
		}
	}

	@Inject(method = "getUseItem", at = @At("HEAD"), cancellable = true)
	private void wh_forceBowUseItemForShortbowBuckler(CallbackInfoReturnable<ItemStack> cir) {
		if (!((Object) this instanceof Player player) || !player.level().isClientSide()) {
			return;
		}
		if (player.isUsingItem() && player.getMainHandItem().getItem() == ModItems.SHORTBOW_BUCKLER) {
			cir.setReturnValue(shortbowVisualBow());
		}
	}
}
