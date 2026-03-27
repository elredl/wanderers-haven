package com.wanderershaven.client.screen;

import com.wanderershaven.client.ClientSkillState;
import com.wanderershaven.network.UseActiveSkillPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Radial active-skill menu, shown while the player holds the radial-menu key
 * (default: Middle Mouse Button).
 *
 * 5 skill slots are arranged at equal angular intervals around the centre.
 * Moving the mouse highlights the nearest segment. Releasing the key (or mouse
 * button 2) activates the highlighted skill and closes the menu.
 *
 * A dead zone around the centre prevents accidental activations.
 */
public class RadialSkillMenuScreen extends Screen {

	private static final int DEAD_ZONE   = 28;   // px from center — no selection
	private static final int INNER_R     = 30;   // inner ring radius
	private static final int OUTER_R     = 90;   // label placement radius
	private static final int CIRCLE_R    = 100;  // visual outer radius of segments
	private static final int SEGMENT_PIX = 64;   // pixel diameter of label box

	// Palette
	private static final int C_WHITE     = 0xFFFFFFFF;
	private static final int C_GOLD      = 0xFFFFD700;
	private static final int C_GREY      = 0xFFAAAAAA;
	private static final int C_BG        = 0xCC000000;
	private static final int C_SEG_NORM  = 0xAA1A1a1A;
	private static final int C_SEG_HOV   = 0xCC3A3020;
	private static final int C_SEG_BD    = 0xFF4A3A18;
	private static final int C_SEG_BD_HL = 0xFFFFD700;
	private static final int C_EMPTY     = 0xFF555555;

	/**
	 * Number of radial steps used to approximate the arc boundary.
	 * Higher = smoother but slower (fine at 12).
	 */
	private static final int ARC_STEPS = 12;

	/** Currently highlighted segment (0-4), or -1 if in dead zone. */
	private int highlighted = -1;

	/** Cached screen centre (set in init). */
	private int cx, cy;

	public RadialSkillMenuScreen() {
		super(Component.empty());
	}

	@Override
	protected void init() {
		cx = width  / 2;
		cy = height / 2;
	}

	// ── Rendering ─────────────────────────────────────────────────────────────

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		// Full-screen semi-transparent dark overlay
		g.fill(0, 0, width, height, 0x88000000);

		// Recalculate highlight from current mouse position
		highlighted = getSegment(mx, my);

		// Draw 5 segments
		for (int i = 0; i < 5; i++) {
			renderSegment(g, i, i == highlighted);
		}

		// Center circle label
		String center = highlighted >= 0 ? "Release to use" : "Move to select";
		int centerBg = 0xCC1A1A1A;
		int labelR = 28;
		g.fill(cx - labelR, cy - labelR, cx + labelR, cy + labelR, centerBg);
		drawBorder(g, cx - labelR, cy - labelR, labelR * 2, labelR * 2, C_SEG_BD);
		g.drawCenteredString(font, "\u25c6", cx, cy - 12, C_GOLD);
		g.drawCenteredString(font, center, cx, cy, C_GREY);

		// Slot number labels around the outside (helps players learn positions)
		for (int i = 0; i < 5; i++) {
			double angle = segmentAngleRad(i);
			int lx = cx + (int)(Math.sin(angle) * (OUTER_R + 20));
			int ly = cy - (int)(Math.cos(angle) * (OUTER_R + 20));
			g.drawCenteredString(font, String.valueOf(i + 1), lx, ly - 4, C_GREY);
		}

		super.render(g, mx, my, delta);
	}

	private void renderSegment(GuiGraphics g, int index, boolean active) {
		double angle = segmentAngleRad(index);

		// Label box position (centre of the segment)
		int lx = cx + (int)(Math.sin(angle) * OUTER_R);
		int ly = cy - (int)(Math.cos(angle) * OUTER_R);

		int half = SEGMENT_PIX / 2;
		int bx = lx - half;
		int by = ly - half;

		String skillId = ClientSkillState.getSlot(index);
		boolean empty  = (skillId == null);
		String label   = empty ? "Empty" : getDisplayLabel(skillId);

		// Background
		g.fill(bx, by, bx + SEGMENT_PIX, by + SEGMENT_PIX, active ? C_SEG_HOV : C_SEG_NORM);
		drawBorder(g, bx, by, SEGMENT_PIX, SEGMENT_PIX, active ? C_SEG_BD_HL : C_SEG_BD);

		// Skill name (may wrap slightly)
		g.drawCenteredString(font, label, lx, by + SEGMENT_PIX / 2 - 4, empty ? C_EMPTY : (active ? C_GOLD : C_WHITE));
	}

	// ── Input ─────────────────────────────────────────────────────────────────

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 2) { // middle mouse released
			activateHighlighted();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/**
	 * Called by the tick handler when the radial-menu key is no longer held.
	 * Activates the highlighted skill (if any) and closes.
	 */
	public void onRadialKeyReleased() {
		activateHighlighted();
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private void activateHighlighted() {
		if (highlighted >= 0 && ClientSkillState.getSlot(highlighted) != null) {
			ClientPlayNetworking.send(new UseActiveSkillPayload(highlighted));
		}
		onClose();
	}

	/**
	 * Returns the segment index (0-4) that the mouse points to, or -1 if in the dead zone.
	 *
	 * Segment 0 is at the top (north). Segments go clockwise at 72° intervals.
	 */
	private int getSegment(int mx, int my) {
		double dx = mx - cx;
		double dy = my - cy;
		if (dx * dx + dy * dy < DEAD_ZONE * DEAD_ZONE) return -1;

		// atan2: 0 = east, angles clockwise (y grows downward)
		// Adjust so 0 = north (top), clockwise: add 90°
		double angleDeg = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
		if (angleDeg < 0) angleDeg += 360.0;
		angleDeg = angleDeg % 360.0;

		// Each segment spans 72°. Offset by half a segment so segment 0 is centred on north.
		return (int)((angleDeg + 36.0) / 72.0) % 5;
	}

	/** Returns the centre angle of segment {@code i} in radians (0 = north, clockwise). */
	private static double segmentAngleRad(int i) {
		return Math.toRadians(i * 72.0);
	}

	private static String getDisplayLabel(String skillId) {
		// Strip "warrior_" prefix and replace underscores with spaces for compactness
		String s = skillId.replaceFirst("^[a-z]+_", "").replace('_', ' ');
		if (s.length() > 0) {
			s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
		}
		return s;
	}

	private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x,         y,         x + w,     y + 1,     color);
		g.fill(x,         y + h - 1, x + w,     y + h,     color);
		g.fill(x,         y + 1,     x + 1,     y + h - 1, color);
		g.fill(x + w - 1, y + 1,     x + w,     y + h - 1, color);
	}
}
