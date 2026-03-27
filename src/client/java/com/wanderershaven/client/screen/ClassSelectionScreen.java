package com.wanderershaven.client.screen;

import com.wanderershaven.network.ClassDecisionPayload;
import com.wanderershaven.network.OpenClassSelectionPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Full-screen class selection GUI. Shows one pending class at a time.
 *
 * Normal view:
 *   Title / divider / subtitle / class card / Decline + Accept / skip link / footer
 *
 * Confirmation view (3rd decline attempt):
 *   Title / divider / warning text / two vanilla buttons (Yes / No)
 */
public class ClassSelectionScreen extends Screen {

	// Card + button sizing
	private static final int CARD_W   = 270;
	private static final int CARD_H   = 58;
	private static final int BTN_W    = 125;
	private static final int BTN_H    = 28;

	// Palette
	private static final int C_WHITE    = 0xFFFFFFFF;
	private static final int C_GOLD     = 0xFFFFD700;
	private static final int C_GREY     = 0xFFAAAAAA;
	private static final int C_ORANGE   = 0xFFFF9922;
	private static final int C_CARD     = 0xEE0C0A07;
	private static final int C_CARD_HL  = 0xFF221E10;
	private static final int C_CARD_BD  = 0xFF6B5028;
	private static final int C_DIVIDER  = 0xFF7A5C30;
	private static final int C_RED      = 0xFFAA1E1E;
	private static final int C_RED_HL   = 0xFF8A1616;
	private static final int C_GREEN    = 0xFF236B23;
	private static final int C_GREEN_HL = 0xFF1A521A;

	// Decorative section divider rendered as text
	private static final String DIVIDER = "\u2500\u2500\u2500 \u25c6 \u25c6 \u25c6 \u2500\u2500\u2500";

	private final List<OpenClassSelectionPayload.PendingClassEntry> pendingClasses;
	private int currentIndex      = 0;
	private boolean showingConfirmation = false;

	// Bounding boxes for the two custom painted buttons
	private int declineX, declineY, acceptX, acceptY;

	public ClassSelectionScreen(List<OpenClassSelectionPayload.PendingClassEntry> pendingClasses) {
		super(Component.literal("The Grand Design"));
		this.pendingClasses = new ArrayList<>(pendingClasses);
	}

	// ── Init ──────────────────────────────────────────────────────────────────

	@Override
	protected void init() {
		rebuildButtons();
	}

	private void rebuildButtons() {
		clearWidgets();

		if (currentIndex >= pendingClasses.size()) return;

		int cx = width / 2;
		int cy = height / 2;

		if (showingConfirmation) {
			addRenderableWidget(Button.builder(
				Component.literal("Yes, remove forever"),
				btn -> {
					send(pendingClasses.get(currentIndex).classId(), ClassDecisionPayload.DecisionType.PERMANENT_DENY);
					advance();
				}
			).bounds(cx - 130, cy + 30, 122, 22).build());

			addRenderableWidget(Button.builder(
				Component.literal("No, keep it pending"),
				btn -> {
					showingConfirmation = false;
					rebuildButtons();
				}
			).bounds(cx + 8, cy + 30, 122, 22).build());
		} else {
			addRenderableWidget(Button.builder(
				Component.literal("Skip for now"),
				btn -> onClose()
			).bounds(cx - 38, cy + 72, 76, 16).build());
		}
	}

	// ── Rendering ─────────────────────────────────────────────────────────────

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		if (currentIndex >= pendingClasses.size()) {
			super.render(g, mx, my, delta);
			return;
		}

		OpenClassSelectionPayload.PendingClassEntry entry = pendingClasses.get(currentIndex);
		int cx = width / 2;
		int cy = height / 2;

		// ── Title section (shared between both views)
		g.drawCenteredString(font, "The Grand Design", cx, cy - 95, C_WHITE);
		g.drawCenteredString(font, DIVIDER, cx, cy - 80, C_DIVIDER);

		if (showingConfirmation) {
			renderConfirmation(g, entry, cx, cy);
		} else {
			renderNormal(g, entry, cx, cy, mx, my);
		}

		super.render(g, mx, my, delta);
	}

	private void renderNormal(
		GuiGraphics g,
		OpenClassSelectionPayload.PendingClassEntry entry,
		int cx, int cy, int mx, int my
	) {
		// Subtitle
		g.drawCenteredString(font, "You have earned a Class.", cx, cy - 62, C_GREY);
		g.drawCenteredString(font, "Choose your new path:", cx, cy - 48, C_WHITE);

		// Class card
		int cardX = cx - CARD_W / 2;
		int cardY = cy - 38;
		boolean cardHov = mx >= cardX && mx < cardX + CARD_W && my >= cardY && my < cardY + CARD_H;
		g.fill(cardX, cardY, cardX + CARD_W, cardY + CARD_H, cardHov ? C_CARD_HL : C_CARD);
		drawBorder(g, cardX, cardY, CARD_W, CARD_H, C_CARD_BD);
		g.drawCenteredString(font, entry.displayName(), cx, cardY + CARD_H / 2 - 4, C_GOLD);

		// Deny warning or progress counter
		if (entry.denyCount() > 0) {
			g.drawCenteredString(font, "Warning: Declined " + entry.denyCount() + "/3 times", cx, cy + 28, C_ORANGE);
		} else if (pendingClasses.size() > 1) {
			g.drawCenteredString(font, "Class " + (currentIndex + 1) + " of " + pendingClasses.size(), cx, cy + 28, C_GREY);
		}

		// Decline / Accept buttons (custom painted, handled in mouseClicked)
		int btnY = cy + 40;
		declineX = cx - BTN_W - 6;
		declineY = btnY;
		acceptX  = cx + 6;
		acceptY  = btnY;

		boolean decHov = mx >= declineX && mx < declineX + BTN_W && my >= declineY && my < declineY + BTN_H;
		boolean accHov = mx >= acceptX  && mx < acceptX  + BTN_W && my >= acceptY  && my < acceptY  + BTN_H;

		drawColorButton(g, declineX, declineY, BTN_W, BTN_H, "Decline", decHov ? C_RED_HL : C_RED);
		drawColorButton(g, acceptX,  acceptY,  BTN_W, BTN_H, "Accept",  accHov ? C_GREEN_HL : C_GREEN);

		// Footer
		g.drawCenteredString(font, DIVIDER, cx, cy + 94, C_DIVIDER);
		g.drawCenteredString(font, "Choose wisely. Your decision will shape your destiny.", cx, cy + 108, C_GREY);
	}

	private void renderConfirmation(
		GuiGraphics g,
		OpenClassSelectionPayload.PendingClassEntry entry,
		int cx, int cy
	) {
		g.drawCenteredString(font, "Are you sure?", cx, cy - 60, 0xFFFF4444);
		g.drawCenteredString(font, "Declining \u00ab" + entry.displayName() + "\u00bb a third time", cx, cy - 40, C_WHITE);
		g.drawCenteredString(font, "will permanently close this opportunity.", cx, cy - 26, C_WHITE);
		g.drawCenteredString(font, "There will be no other chances.", cx, cy - 2, C_ORANGE);
	}

	// ── Input ─────────────────────────────────────────────────────────────────

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
		double mx = event.x();
		double my = event.y();

		if (!showingConfirmation && currentIndex < pendingClasses.size()) {
			OpenClassSelectionPayload.PendingClassEntry entry = pendingClasses.get(currentIndex);

			if (hits(mx, my, acceptX, acceptY, BTN_W, BTN_H)) {
				send(entry.classId(), ClassDecisionPayload.DecisionType.ACCEPT);
				advance();
				return true;
			}

			if (hits(mx, my, declineX, declineY, BTN_W, BTN_H)) {
				if (entry.denyCount() >= 2) {
					showingConfirmation = true;
					rebuildButtons();
				} else {
					send(entry.classId(), ClassDecisionPayload.DecisionType.DENY);
					advance();
				}
				return true;
			}
		}

		return super.mouseClicked(event, consumed);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private void advance() {
		currentIndex++;
		showingConfirmation = false;
		if (currentIndex >= pendingClasses.size()) {
			onClose();
		} else {
			rebuildButtons();
		}
	}

	private static void send(String classId, ClassDecisionPayload.DecisionType decision) {
		ClientPlayNetworking.send(new ClassDecisionPayload(classId, decision));
	}

	private static boolean hits(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private void drawColorButton(GuiGraphics g, int x, int y, int w, int h, String label, int bg) {
		g.fill(x, y, x + w, y + h, bg);
		drawBorder(g, x, y, w, h, darken(bg, 0.55f));
		g.drawCenteredString(font, label, x + w / 2, y + h / 2 - 4, C_WHITE);
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
