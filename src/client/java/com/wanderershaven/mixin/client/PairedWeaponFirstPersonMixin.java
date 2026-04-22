package com.wanderershaven.mixin.client;

import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wanderershaven.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class PairedWeaponFirstPersonMixin {

	@Shadow @Final private Minecraft minecraft;
	@Shadow @Final private ItemModelResolver itemModelResolver;
	@Shadow private ItemStack mainHandItem;
	@Shadow private ItemStack offHandItem;
	@Shadow private float mainHandHeight;
	@Shadow private float oMainHandHeight;
	@Shadow private float offHandHeight;
	@Shadow private float oOffHandHeight;

	@Shadow
	private void renderArmWithItem(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand,
			float swingProgress, ItemStack stack, float equipProgress, PoseStack poseStack,
			SubmitNodeCollector collector, int packedLight) {}

	@Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void wh_renderPairedWeapons(float tickDelta, PoseStack poseStack, SubmitNodeCollector collector,
			LocalPlayer player, int packedLight, CallbackInfo ci) {
		ItemStack playerMain = player.getMainHandItem();
		ItemStack playerOff = player.getOffhandItem();

		boolean twinActive = playerMain.getItem() == ModItems.TWIN_CROSSBOW || playerOff.getItem() == ModItems.TWIN_CROSSBOW;
		boolean shortbowBucklerActive = playerMain.getItem() == ModItems.SHORTBOW_BUCKLER;
		if (!twinActive && !shortbowBucklerActive) {
			return;
		}

		ci.cancel();

		float attackAnim = player.getAttackAnim(tickDelta);
		InteractionHand swingingHand = MoreObjects.firstNonNull(player.swingingArm, InteractionHand.MAIN_HAND);
		float pitch = player.getXRot(tickDelta);

		float xBob = Mth.lerp(tickDelta, player.xBobO, player.xBob);
		float yBob = Mth.lerp(tickDelta, player.yBobO, player.yBob);
		poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees((player.getViewXRot(tickDelta) - xBob) * 0.1f));
		poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((player.getViewYRot(tickDelta) - yBob) * 0.1f));

		ItemStack twinVisual = playerMain.getItem() == ModItems.TWIN_CROSSBOW ? playerMain : playerOff;
		ItemStack renderedMain = shortbowBucklerActive ? new ItemStack(Items.SHIELD) : twinVisual;
		ItemStack renderedOff = shortbowBucklerActive ? player.getItemInHand(InteractionHand.OFF_HAND) : twinVisual;

		float mainSwing = swingingHand == InteractionHand.MAIN_HAND ? attackAnim : 0.0f;
		float mainEquip = itemModelResolver.swapAnimationScale(this.mainHandItem)
			* (1.0f - Mth.lerp(tickDelta, this.oMainHandHeight, this.mainHandHeight));
		if (shortbowBucklerActive) {
			int side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
			poseStack.pushPose();
			poseStack.translate(side * 0.11f, -0.03f, 0.02f);
			poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(side * -24.0f));
			poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(side * 11.0f));
			renderArmWithItem(player, tickDelta, pitch, InteractionHand.MAIN_HAND, mainSwing, renderedMain, mainEquip,
				poseStack, collector, packedLight);
			poseStack.popPose();
		} else {
			renderArmWithItem(player, tickDelta, pitch, InteractionHand.MAIN_HAND, mainSwing, renderedMain, mainEquip,
				poseStack, collector, packedLight);
		}

		float offSwing = swingingHand == InteractionHand.OFF_HAND ? attackAnim : 0.0f;
		float offEquip = itemModelResolver.swapAnimationScale(this.offHandItem)
			* (1.0f - Mth.lerp(tickDelta, this.oOffHandHeight, this.offHandHeight));
		renderArmWithItem(player, tickDelta, pitch, InteractionHand.OFF_HAND, offSwing, renderedOff, offEquip,
			poseStack, collector, packedLight);

		this.minecraft.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
		this.minecraft.renderBuffers().bufferSource().endBatch();
	}
}
