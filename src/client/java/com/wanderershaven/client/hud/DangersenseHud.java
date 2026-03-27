package com.wanderershaven.client.hud;

import com.wanderershaven.client.ClientSkillState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.monster.Monster;

/**
 * Dangersense HUD overlay.
 *
 * Draws a red "!" in the top-right corner of the screen whenever a hostile
 * mob is within 4 blocks of the local player (requires the Dangersense skill).
 */
@Environment(EnvType.CLIENT)
public final class DangersenseHud {
	private static final int DANGER_COLOUR = 0xFFFF2222;
	private static final double RADIUS = 4.0;

	private DangersenseHud() {}

	public static void register() {
		HudRenderCallback.EVENT.register(DangersenseHud::render);
	}

	private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		if (mc.options.hideGui) return;
		if (!ClientSkillState.has("warrior_dangersense")) return;

		boolean danger = !mc.level.getEntitiesOfClass(
			Monster.class,
			mc.player.getBoundingBox().inflate(RADIUS),
			e -> !e.isRemoved()
		).isEmpty();

		if (!danger) return;

		// Draw "!" in the top-right corner with a contrasting shadow
		int screenWidth = mc.getWindow().getGuiScaledWidth();
		graphics.drawString(mc.font, "!", screenWidth - 14, 8, DANGER_COLOUR, true);
	}
}
