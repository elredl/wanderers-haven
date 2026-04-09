package com.wanderershaven.client.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Displays the local player's current computed attribute values.
 *
 * Reads directly from the client-side attribute instances (synced by vanilla),
 * so the values shown include all active skill and buff modifiers.
 */
@Environment(EnvType.CLIENT)
public class StatsScreen extends Screen {

	private static final int PANEL_WIDTH  = 220;
	private static final int PANEL_HEIGHT = 240;
	private static final int ROW_HEIGHT   = 14;
	private static final double BASE_MOVEMENT_SPEED = 0.1;

	public StatsScreen() {
		super(Component.literal("Character Stats"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected void init() {
		int panelX = (width  - PANEL_WIDTH)  / 2;
		int panelY = (height - PANEL_HEIGHT) / 2;
		addRenderableWidget(Button.builder(
			Component.literal("Done"), b -> onClose()
		).bounds(panelX + PANEL_WIDTH / 2 - 40, panelY + PANEL_HEIGHT - 26, 80, 18).build());
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		int panelX = (width  - PANEL_WIDTH)  / 2;
		int panelY = (height - PANEL_HEIGHT) / 2;

		// Background panel
		g.fill(panelX,     panelY,     panelX + PANEL_WIDTH,     panelY + PANEL_HEIGHT,     0xFF101010);
		g.fill(panelX,     panelY,     panelX + PANEL_WIDTH,     panelY + 1,                0xFF555555);
		g.fill(panelX,     panelY + PANEL_HEIGHT - 1, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF555555);
		g.fill(panelX,     panelY,     panelX + 1,               panelY + PANEL_HEIGHT,     0xFF555555);
		g.fill(panelX + PANEL_WIDTH - 1, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF555555);

		g.drawCenteredString(font, title, panelX + PANEL_WIDTH / 2, panelY + 8, 0xFFFFFFAA);
		g.fill(panelX + 8, panelY + 19, panelX + PANEL_WIDTH - 8, panelY + 20, 0xFF444444);

		Player player = Minecraft.getInstance().player;
		if (player == null) {
			super.render(g, mouseX, mouseY, delta);
			return;
		}

		int labelX = panelX + 12;
		int valueX = panelX + PANEL_WIDTH - 12;
		int y      = panelY + 26;

		// ── Offensive ────────────────────────────────────────────────────────
		g.drawString(font, "\u00a7eOffensive", labelX, y, 0xFFFFFFFF, false);
		y += ROW_HEIGHT;

		double atk = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		drawStat(g, labelX, valueX, y, "Attack Damage", String.format("%.2f", atk));
		y += ROW_HEIGHT;

		double spd = player.getAttributeValue(Attributes.ATTACK_SPEED);
		drawStat(g, labelX, valueX, y, "Attack Speed", String.format("%.2f /s", spd));
		y += ROW_HEIGHT + 4;

		// ── Defensive ────────────────────────────────────────────────────────
		g.drawString(font, "\u00a7eDefensive", labelX, y, 0xFFFFFFFF, false);
		y += ROW_HEIGHT;

		double hp = player.getAttributeValue(Attributes.MAX_HEALTH);
		drawStat(g, labelX, valueX, y, "Max Health", String.format("%.1f \u2665", hp / 2.0));
		y += ROW_HEIGHT;

		double armor = player.getAttributeValue(Attributes.ARMOR);
		drawStat(g, labelX, valueX, y, "Armor", String.format("%.1f", armor));
		y += ROW_HEIGHT;

		double toughness = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
		drawStat(g, labelX, valueX, y, "Armor Toughness", String.format("%.1f", toughness));
		y += ROW_HEIGHT;

		double kb = player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		String kbText = kb >= 1.0 ? "Immune" : String.format("%.0f%%", kb * 100);
		drawStat(g, labelX, valueX, y, "Knockback Resist", kbText);
		y += ROW_HEIGHT + 4;

		// ── Mobility ─────────────────────────────────────────────────────────
		g.drawString(font, "\u00a7eMobility", labelX, y, 0xFFFFFFFF, false);
		y += ROW_HEIGHT;

		double move = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
		double bonus = (move - BASE_MOVEMENT_SPEED) / BASE_MOVEMENT_SPEED * 100.0;
		String moveText = bonus >= 0.5 ? String.format("+%.0f%%", bonus) : "Base";
		drawStat(g, labelX, valueX, y, "Movement Speed", moveText);

		super.render(g, mouseX, mouseY, delta);
	}

	private void drawStat(GuiGraphics g, int labelX, int valueX, int y, String label, String value) {
		g.drawString(font, label, labelX + 4, y, 0xFFAAAAAA, false);
		g.drawString(font, value, valueX - font.width(value), y, 0xFFFFFFFF, false);
	}
}
