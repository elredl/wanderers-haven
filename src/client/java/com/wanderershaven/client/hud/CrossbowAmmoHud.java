package com.wanderershaven.client.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;

@Environment(EnvType.CLIENT)
public final class CrossbowAmmoHud {

	private static final int MAG_SIZE = 2;

	private CrossbowAmmoHud() {}

	public static void register() {
		HudRenderCallback.EVENT.register(CrossbowAmmoHud::render);
	}

	private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui) {
			return;
		}

		boolean inOffhand = false;
		ItemStack stack = mc.player.getMainHandItem();
		if (!(stack.getItem() instanceof CrossbowItem)) {
			stack = mc.player.getOffhandItem();
			if (!(stack.getItem() instanceof CrossbowItem)) {
				return;
			}
			inOffhand = true;
		}

		ChargedProjectiles loaded = stack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
		int loadedCount = Math.min(MAG_SIZE, loaded.getItems().size());
		String text = loadedCount + "/" + MAG_SIZE;

		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		int hotbarY = sh - 22;
		int x;
		int y;
		if (!inOffhand) {
			int slot = mc.player.getInventory().getSelectedSlot();
			int slotCenterX = sw / 2 - 91 + slot * 20 + 10;
			x = slotCenterX - mc.font.width(text) / 2;
			y = hotbarY + 2;
		} else {
			boolean offhandOnLeft = mc.player.getMainArm() == HumanoidArm.RIGHT;
			int offhandX = offhandOnLeft ? (sw / 2 - 91 - 26) : (sw / 2 + 91 + 10);
			x = offhandX - mc.font.width(text) / 2;
			y = hotbarY + 2;
		}
		graphics.drawString(mc.font, text, x, y, 0xFFEAC66B, true);
	}
}
