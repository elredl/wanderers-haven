package com.wanderershaven.client.hud;

import com.wanderershaven.client.ClientSkillState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.monster.Monster;

/** Dangersense HUD overlay icon in the bottom-right corner. */
@Environment(EnvType.CLIENT)
public final class DangersenseHud {
	private static final int ICON_SIZE = 12;
	private static final int PADDING_RIGHT = 8;
	private static final int PADDING_BOTTOM = 42;
	private static final int BORDER = 0xFF2B0A0A;
	private static final int BG = 0xD02A0000;
	private static final int CENTER = 0xFFFF5A5A;
	private static final int PULSE_BASE = 0xFFB30000;
	private static final double RADIUS = 4.0;

	private DangersenseHud() {}

	public static void register() {
		HudRenderCallback.EVENT.register(DangersenseHud::render);
	}

	private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		if (mc.options.hideGui) return;
		if (!hasDangersenseSkill()) return;

		boolean danger = !mc.level.getEntitiesOfClass(
			Monster.class,
			mc.player.getBoundingBox().inflate(RADIUS),
			e -> !e.isRemoved()
		).isEmpty();

		if (!danger) return;

		int screenWidth = mc.getWindow().getGuiScaledWidth();
		int screenHeight = mc.getWindow().getGuiScaledHeight();
		int x = screenWidth - PADDING_RIGHT - ICON_SIZE;
		int y = screenHeight - PADDING_BOTTOM - ICON_SIZE;

		long time = mc.level.getGameTime();
		int pulse = (int) (20 + (Math.sin(time * 0.35) + 1.0) * 35);
		int pulseAlpha = (pulse & 0xFF) << 24;
		int pulseColor = pulseAlpha | (PULSE_BASE & 0x00FFFFFF);

		graphics.fill(x - 2, y - 2, x + ICON_SIZE + 2, y + ICON_SIZE + 2, pulseColor);
		drawDiamond(graphics, x, y, ICON_SIZE, BORDER);
		drawDiamond(graphics, x + 1, y + 1, ICON_SIZE - 2, BG);
		drawDiamond(graphics, x + 4, y + 4, ICON_SIZE - 8, CENTER);
	}

	private static boolean hasDangersenseSkill() {
		return ClientSkillState.has("dangersense")
			|| ClientSkillState.has("greater_dangersense")
			|| ClientSkillState.has("perception_nothing_slips_my_grasp");
	}

	private static void drawDiamond(GuiGraphics graphics, int x, int y, int size, int color) {
		int cx = x + size / 2;
		int cy = y + size / 2;
		for (int row = 0; row < size; row++) {
			int dy = Math.abs(row - size / 2);
			int half = Math.max(1, size / 2 - dy);
			graphics.fill(cx - half, y + row, cx + half + 1, y + row + 1, color);
		}
	}
}
