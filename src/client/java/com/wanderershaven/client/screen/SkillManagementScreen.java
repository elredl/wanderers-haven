package com.wanderershaven.client.screen;

import com.wanderershaven.client.ClientSkillState;
import com.wanderershaven.network.OpenSkillManagementPayload;
import com.wanderershaven.network.UpdateActiveSkillSlotsPayload;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Sleep screen tab that lets the player bind owned skills to 5 active slots.
 *
 * Layout:
 *   Title + divider
 *   5 slot panels in a row — click to select, right-click to clear
 *   Scrollable skill list — click a skill while a slot is selected to bind it
 *   Done button (saves to server and closes)
 */
public class SkillManagementScreen extends Screen {

	// Palette (matches ClassSelectionScreen)
	private static final int C_WHITE    = 0xFFFFFFFF;
	private static final int C_GOLD     = 0xFFFFD700;
	private static final int C_GREY     = 0xFFAAAAAA;
	private static final int C_DARK     = 0xEE0C0A07;
	private static final int C_CARD     = 0xEE0C0A07;
	private static final int C_CARD_BD  = 0xFF6B5028;
	private static final int C_DIVIDER  = 0xFF7A5C30;
	private static final int C_SEL_BD   = 0xFFFFD700;
	private static final int C_ROW_HOV  = 0x33FFFFFF;
	private static final int C_ROW_SEL  = 0x55FFD700;
	private static final int C_EMPTY    = 0xFF666666;

	private static final String DIVIDER = "\u2500\u2500\u2500 \u25c6 \u25c6 \u25c6 \u2500\u2500\u2500";

	// Slot geometry
	private static final int SLOT_W     = 55;
	private static final int SLOT_H     = 36;
	private static final int SLOT_GAP   = 6;

	// Skill list geometry
	private static final int LIST_ROW_H = 22;
	private static final int LIST_ROWS  = 8; // visible rows

	private final List<OpenSkillManagementPayload.SkillEntry> skills;
	/** Working copy of slot bindings (null = empty). */
	private final String[] slots = new String[5];

	/** Which slot is selected for binding (-1 = none). */
	private int selectedSlot = -1;
	/** Top of the visible skill list (entry index). */
	private int scrollOffset = 0;

	// Cached geometry (computed in init)
	private int slotsY;
	private int listX, listY, listW;

	public SkillManagementScreen(
		List<OpenSkillManagementPayload.SkillEntry> ownedSkills,
		List<String> currentSlots
	) {
		super(Component.literal("Active Skills"));
		this.skills = new ArrayList<>(ownedSkills);
		for (int i = 0; i < 5; i++) {
			this.slots[i] = (i < currentSlots.size()) ? currentSlots.get(i) : null;
		}
	}

	@Override
	protected void init() {
		int cx = width / 2;
		slotsY = height / 2 - 80;

		// List geometry
		listW = Math.min(320, width - 40);
		listX = cx - listW / 2;
		listY = slotsY + SLOT_H + 30;

		// Done button
		addRenderableWidget(Button.builder(
			Component.literal("Done"),
			btn -> saveAndClose()
		).bounds(cx - 40, height / 2 + 100, 80, 22).build());
	}

	// ── Rendering ─────────────────────────────────────────────────────────────

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		int cx = width / 2;
		int cy = height / 2;

		// Title
		g.drawCenteredString(font, "Active Skills", cx, cy - 120, C_GOLD);
		g.drawCenteredString(font, DIVIDER, cx, cy - 108, C_DIVIDER);
		g.drawCenteredString(font, "Bind skills to active slots. Use them with the Radial Menu (Middle Mouse).", cx, cy - 94, C_GREY);

		// Slots section header
		g.drawCenteredString(font, "Active Slots", cx, slotsY - 12, C_WHITE);
		renderSlots(g, mx, my);

		// Skill list header
		int listHeaderY = listY - 12;
		g.drawString(font, "Owned Skills", listX, listHeaderY, C_WHITE);
		if (selectedSlot >= 0) {
			g.drawString(font, " \u2190 Click a skill to assign to Slot " + (selectedSlot + 1), listX + font.width("Owned Skills"), listHeaderY, C_GOLD);
		}

		// Skill list background
		int listH = LIST_ROWS * LIST_ROW_H + 4;
		g.fill(listX - 2, listY - 2, listX + listW + 2, listY + listH, 0xAA000000);
		drawBorder(g, listX - 2, listY - 2, listW + 4, listH + 2, C_CARD_BD);

		renderSkillList(g, mx, my);

		// Scroll hint
		if (skills.size() > LIST_ROWS) {
			g.drawCenteredString(font, "\u2191\u2193 Scroll", cx, listY + listH + 4, C_GREY);
		}

		// Footer
		g.drawCenteredString(font, DIVIDER, cx, height / 2 + 86, C_DIVIDER);

		// Tooltip — show description of hovered skill
		renderHoveredSkillTooltip(g, mx, my);

		super.render(g, mx, my, delta);
	}

	private void renderSlots(GuiGraphics g, int mx, int my) {
		int totalW = 5 * SLOT_W + 4 * SLOT_GAP;
		int startX = width / 2 - totalW / 2;

		for (int i = 0; i < 5; i++) {
			int sx = startX + i * (SLOT_W + SLOT_GAP);
			int sy = slotsY;
			boolean selected = (selectedSlot == i);
			boolean hovered = mx >= sx && mx < sx + SLOT_W && my >= sy && my < sy + SLOT_H;

			// Background
			g.fill(sx, sy, sx + SLOT_W, sy + SLOT_H, C_CARD);
			// Border — gold if selected, normal otherwise
			drawBorder(g, sx, sy, SLOT_W, SLOT_H, selected ? C_SEL_BD : (hovered ? 0xFF9B7840 : C_CARD_BD));

			// Slot number label
			g.drawCenteredString(font, String.valueOf(i + 1), sx + SLOT_W / 2, sy + 3, C_GREY);

			// Skill name (or "Empty")
			String skillId = slots[i];
			String label;
			int labelColor;
			if (skillId != null) {
				OpenSkillManagementPayload.SkillEntry entry = findSkill(skillId);
				label = entry != null ? abbreviate(entry.displayName(), SLOT_W - 4) : skillId;
				labelColor = C_WHITE;
			} else {
				label = "Empty";
				labelColor = C_EMPTY;
			}
			g.drawCenteredString(font, label, sx + SLOT_W / 2, sy + SLOT_H / 2 + 1, labelColor);
		}
	}

	private void renderSkillList(GuiGraphics g, int mx, int my) {
		int visible = Math.min(LIST_ROWS, skills.size() - scrollOffset);
		for (int i = 0; i < visible; i++) {
			OpenSkillManagementPayload.SkillEntry skill = skills.get(scrollOffset + i);
			int ry = listY + i * LIST_ROW_H;
			boolean hovered = mx >= listX && mx < listX + listW && my >= ry && my < ry + LIST_ROW_H;
			boolean isBound = isSkillBound(skill.id());

			// Row background
			int rowBg = hovered ? C_ROW_HOV : (isBound ? 0x2200AAFF : 0);
			if (rowBg != 0) g.fill(listX, ry, listX + listW, ry + LIST_ROW_H, rowBg);

			// Skill name
			g.drawString(font, skill.displayName(), listX + 4, ry + (LIST_ROW_H - 8) / 2, isBound ? 0xFFAAAAFF : C_WHITE);
			// PW badge (right-aligned)
			String pwLabel = "PW" + skill.powerLevel();
			g.drawString(font, pwLabel, listX + listW - font.width(pwLabel) - 4, ry + (LIST_ROW_H - 8) / 2, C_GREY);
		}
	}

	// ── Input ─────────────────────────────────────────────────────────────────

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
		double mx = event.x();
		double my = event.y();
		int button = event.button();

		// Slot click
		int totalW = 5 * SLOT_W + 4 * SLOT_GAP;
		int startX = width / 2 - totalW / 2;
		for (int i = 0; i < 5; i++) {
			int sx = startX + i * (SLOT_W + SLOT_GAP);
			int sy = slotsY;
			if (mx >= sx && mx < sx + SLOT_W && my >= sy && my < sy + SLOT_H) {
				if (button == 1) { // right-click to clear
					slots[i] = null;
					if (selectedSlot == i) selectedSlot = -1;
				} else {
					selectedSlot = (selectedSlot == i) ? -1 : i;
				}
				return true;
			}
		}

		// Skill list click
		if (selectedSlot >= 0 && mx >= listX && mx < listX + listW) {
			int row = (int)((my - listY) / LIST_ROW_H);
			int idx = scrollOffset + row;
			if (row >= 0 && idx < skills.size()) {
				if (button == 0) { // left-click to assign
					slots[selectedSlot] = skills.get(idx).id();
					selectedSlot = -1; // deselect after binding
					return true;
				}
			}
		}

		return super.mouseClicked(event, consumed);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		int maxOffset = Math.max(0, skills.size() - LIST_ROWS);
		scrollOffset = (int) Math.max(0, Math.min(maxOffset, scrollOffset - scrollY));
		return true;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private void saveAndClose() {
		// Send updated slots to server
		List<String> slotList = Arrays.asList(slots);
		ClientPlayNetworking.send(new UpdateActiveSkillSlotsPayload(slotList));
		// Sync client state immediately
		ClientSkillState.updateSlots(slotList);
		onClose();
	}

	private boolean isSkillBound(String skillId) {
		for (String s : slots) {
			if (skillId.equals(s)) return true;
		}
		return false;
	}

	private OpenSkillManagementPayload.SkillEntry findSkill(String skillId) {
		for (OpenSkillManagementPayload.SkillEntry e : skills) {
			if (e.id().equals(skillId)) return e;
		}
		return null;
	}

	private String abbreviate(String text, int maxWidth) {
		if (font.width(text) <= maxWidth) return text;
		while (text.length() > 1 && font.width(text + "...") > maxWidth) {
			text = text.substring(0, text.length() - 1);
		}
		return text + "...";
	}

	private void renderHoveredSkillTooltip(GuiGraphics g, int mx, int my) {
		if (!(mx >= listX && mx < listX + listW)) return;
		int row = (my - listY) / LIST_ROW_H;
		int idx = scrollOffset + row;
		if (row < 0 || idx >= skills.size()) return;
		int ry = listY + row * LIST_ROW_H;
		if (my < ry || my >= ry + LIST_ROW_H) return;

		OpenSkillManagementPayload.SkillEntry skill = skills.get(idx);
		List<Component> tooltip = List.of(
			Component.literal(skill.displayName()).withStyle(s -> s.withColor(0xFFD700)),
			Component.literal("Power Level " + skill.powerLevel()).withStyle(s -> s.withColor(0xAAAAAA)),
			Component.literal(""),
			Component.literal(skill.description()).withStyle(s -> s.withColor(0xFFFFFF))
		);
		g.setComponentTooltipForNextFrame(font, tooltip, mx, my);
	}

	private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x,         y,         x + w,     y + 1,     color);
		g.fill(x,         y + h - 1, x + w,     y + h,     color);
		g.fill(x,         y + 1,     x + 1,     y + h - 1, color);
		g.fill(x + w - 1, y + 1,     x + w,     y + h - 1, color);
	}
}
