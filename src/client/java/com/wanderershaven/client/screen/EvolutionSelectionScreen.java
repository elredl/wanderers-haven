package com.wanderershaven.client.screen;

import com.wanderershaven.network.EvolutionChoicePayload;
import com.wanderershaven.network.OpenEvolutionSelectionPayload;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Full-screen evolution path selection GUI.
 *
 * Shows one eligible evolution path at a time. The player may browse all options
 * with the Previous / Next buttons before committing. Accepting is permanent and
 * the screen cannot be closed without making a choice.
 *
 * Layout mirrors ClassSelectionScreen: title, divider, subtitle, card, buttons.
 */
public class EvolutionSelectionScreen extends Screen {

	// Card geometry
	private static final int CARD_W = 290;
	private static final int CARD_H = 80;

	// Accept button
	private static final int BTN_W  = 160;
	private static final int BTN_H  = 28;

	// Palette
	private static final int C_WHITE    = 0xFFFFFFFF;
	private static final int C_GOLD     = 0xFFFFD700;
	private static final int C_GREY     = 0xFFAAAAAA;
	private static final int C_CARD     = 0xEE0C0A07;
	private static final int C_CARD_HL  = 0xFF221E10;
	private static final int C_CARD_BD  = 0xFF6B5028;
	private static final int C_DIVIDER  = 0xFF7A5C30;
	private static final int C_GREEN    = 0xFF236B23;
	private static final int C_GREEN_HL = 0xFF1A521A;

	private static final String DIVIDER = "\u2500\u2500\u2500 \u25c6 \u25c6 \u25c6 \u2500\u2500\u2500";

	private final String baseClassId;
	private final List<OpenEvolutionSelectionPayload.EvolutionEntry> offers;

	private int currentIndex = 0;

	// Painted button bounds (Accept)
	private int acceptX, acceptY;

	public EvolutionSelectionScreen(
		String baseClassId,
		List<OpenEvolutionSelectionPayload.EvolutionEntry> offers
	) {
		super(Component.literal("Evolution Milestone"));
		this.baseClassId = baseClassId;
		this.offers = offers;
	}

	@Override
	protected void init() {
		rebuildButtons();
	}

	private void rebuildButtons() {
		clearWidgets();

		int cx = width / 2;
		int cy = height / 2;

		// Previous / Next navigation (only shown when more than one option)
		if (offers.size() > 1) {
			addRenderableWidget(Button.builder(
				Component.literal("\u25c4 Prev"),
				btn -> navigate(-1)
			).bounds(cx - 130, cy + 50, 55, 20).build());

			addRenderableWidget(Button.builder(
				Component.literal("Next \u25ba"),
				btn -> navigate(+1)
			).bounds(cx + 75, cy + 50, 55, 20).build());
		}
	}

	// ── Rendering ─────────────────────────────────────────────────────────────

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		int cx = width / 2;
		int cy = height / 2;

		OpenEvolutionSelectionPayload.EvolutionEntry entry = offers.get(currentIndex);

		// Header
		g.drawCenteredString(font, "Evolution Milestone", cx, cy - 100, C_GOLD);
		g.drawCenteredString(font, DIVIDER, cx, cy - 86, C_DIVIDER);
		g.drawCenteredString(font, "Your [" + baseClassId + "] has reached a milestone.", cx, cy - 70, C_GREY);
		g.drawCenteredString(font, "Choose your evolution path:", cx, cy - 56, C_WHITE);

		// Evolution card
		int cardX = cx - CARD_W / 2;
		int cardY = cy - 46;
		boolean cardHov = mx >= cardX && mx < cardX + CARD_W && my >= cardY && my < cardY + CARD_H;
		g.fill(cardX, cardY, cardX + CARD_W, cardY + CARD_H, cardHov ? C_CARD_HL : C_CARD);
		drawBorder(g, cardX, cardY, CARD_W, CARD_H, C_CARD_BD);

		// Evolution name
		g.drawCenteredString(font, entry.displayName(), cx, cardY + 10, C_GOLD);

		// Description (word-wrapped manually across two lines)
		String desc = entry.description();
		int maxW = CARD_W - 16;
		if (font.width(desc) <= maxW) {
			g.drawCenteredString(font, desc, cx, cardY + 30, C_GREY);
		} else {
			// Split at last space before maxW
			int split = findWrapPoint(desc, maxW);
			String line1 = desc.substring(0, split).trim();
			String line2 = desc.substring(split).trim();
			g.drawCenteredString(font, line1, cx, cardY + 22, C_GREY);
			g.drawCenteredString(font, line2, cx, cardY + 34, C_GREY);
		}

		// Navigation counter
		if (offers.size() > 1) {
			g.drawCenteredString(font, "Option " + (currentIndex + 1) + " of " + offers.size(), cx, cy + 40, C_GREY);
		}

		// Accept button (custom painted)
		acceptX = cx - BTN_W / 2;
		acceptY = cy + 78;
		boolean accHov = mx >= acceptX && mx < acceptX + BTN_W && my >= acceptY && my < acceptY + BTN_H;
		drawColorButton(g, acceptX, acceptY, BTN_W, BTN_H, "Choose this Path", accHov ? C_GREEN_HL : C_GREEN);

		// Footer
		g.drawCenteredString(font, DIVIDER, cx, cy + 116, C_DIVIDER);
		g.drawCenteredString(font, "Your choice shapes your future skills. Choose wisely.", cx, cy + 130, C_GREY);

		super.render(g, mx, my, delta);
	}

	// ── Input ─────────────────────────────────────────────────────────────────

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
		double mx = event.x();
		double my = event.y();

		if (hits(mx, my, acceptX, acceptY, BTN_W, BTN_H)) {
			ClientPlayNetworking.send(new EvolutionChoicePayload(offers.get(currentIndex).id()));
			onClose();
			return true;
		}

		return super.mouseClicked(event, consumed);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		// Player must make a choice — cannot dismiss
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private void navigate(int delta) {
		currentIndex = Math.floorMod(currentIndex + delta, offers.size());
	}

	private int findWrapPoint(String text, int maxWidth) {
		// Find last space before the text exceeds maxWidth
		int best = text.length();
		for (int i = text.length() - 1; i > 0; i--) {
			if (text.charAt(i) == ' ' && font.width(text.substring(0, i)) <= maxWidth) {
				best = i;
				break;
			}
		}
		return best;
	}

	private void drawColorButton(GuiGraphics g, int x, int y, int w, int h, String label, int bg) {
		g.fill(x, y, x + w, y + h, bg);
		drawBorder(g, x, y, w, h, darken(bg, 0.55f));
		g.drawCenteredString(font, label, x + w / 2, y + h / 2 - 4, C_WHITE);
	}

	private static boolean hits(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x,         y,         x + w,     y + 1,     color);
		g.fill(x,         y + h - 1, x + w,     y + h,     color);
		g.fill(x,         y + 1,     x + 1,     y + h - 1, color);
		g.fill(x + w - 1, y + 1,     x + w,     y + h - 1, color);
	}

	private static int darken(int color, float f) {
		int a = (color >> 24) & 0xFF;
		int r = Math.min(255, (int) (((color >> 16) & 0xFF) * f));
		int gr = Math.min(255, (int) (((color >> 8)  & 0xFF) * f));
		int b = Math.min(255, (int) (( color         & 0xFF) * f));
		return (a << 24) | (r << 16) | (gr << 8) | b;
	}
}
