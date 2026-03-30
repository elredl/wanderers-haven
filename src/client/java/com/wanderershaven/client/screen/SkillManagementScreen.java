package com.wanderershaven.client.screen;

import com.wanderershaven.client.ClientSkillState;
import com.wanderershaven.network.EvolutionChoicePayload;
import com.wanderershaven.network.OpenSkillManagementPayload;
import com.wanderershaven.network.UpdateActiveSkillSlotsPayload;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Sleep screen that lets the player browse and bind their skills, resolve
 * pending evolution offers, and review queued level-up / skill notifications —
 * all in one place.
 *
 * Layout (Skills tab):
 *   Tab bar (only when evolution is pending): [Skills] [Evolution ★]
 *   Title + divider + subtitle
 *   "What's New" panel (only when notifications are present)
 *   5 skill-wheel slot panels
 *   Scrollable skill list
 *   Done button
 */
public class SkillManagementScreen extends Screen {

	// ── Palette ───────────────────────────────────────────────────────────────
	private static final int C_WHITE      = 0xFFFFFFFF;
	private static final int C_GOLD       = 0xFFFFD700;
	private static final int C_GREY       = 0xFFAAAAAA;
	private static final int C_CARD       = 0xEE0C0A07;
	private static final int C_CARD_HL    = 0xFF221E10;
	private static final int C_CARD_BD    = 0xFF6B5028;
	private static final int C_DIVIDER    = 0xFF7A5C30;
	private static final int C_SEL_BD     = 0xFFFFD700;
	private static final int C_ROW_HOV    = 0x33FFFFFF;
	private static final int C_EMPTY      = 0xFF666666;
	private static final int C_SECTION    = 0xFFAA8844;
	private static final int C_GREEN      = 0xFF236B23;
	private static final int C_GREEN_HL   = 0xFF1A521A;
	/** Notification colour — level-up */
	private static final int C_NOTIF_LVL  = 0xFFFFD700;
	/** Notification colour — skill obtained */
	private static final int C_NOTIF_SKILL = 0xFF66DD66;
	/** Notification colour — skill change */
	private static final int C_NOTIF_CHG  = 0xFF66CCFF;

	private static final String DIVIDER_STR = "\u2500\u2500\u2500 \u25c6 \u25c6 \u25c6 \u2500\u2500\u2500";

	// ── Geometry constants ────────────────────────────────────────────────────
	private static final int SLOT_W        = 55;
	private static final int SLOT_H        = 36;
	private static final int SLOT_GAP      = 6;
	private static final int LIST_ROW_H    = 22;
	private static final int TAB_H         = 18;
	private static final int EVO_BTN_W     = 160;
	private static final int EVO_BTN_H     = 28;
	private static final int EVO_NAV_W     = 52;
	private static final int EVO_NAV_H     = 20;
	private static final int NOTIF_ROW_H   = 12;
	private static final int NOTIF_MAX_VIS = 4;

	// ── Tabs ──────────────────────────────────────────────────────────────────
	private static final int TAB_SKILLS    = 0;
	private static final int TAB_EVOLUTION = 1;
	private int activeTab = TAB_SKILLS;

	// ── Skill data ────────────────────────────────────────────────────────────
	private final List<OpenSkillManagementPayload.SkillEntry> skills;
	private final String[] slots = new String[5];

	/** Unified scroll list: items are SkillEntry (skill row) or String (section header). */
	private final List<Object> listItems;

	private int selectedSlot   = -1;
	private int scrollOffset   = 0;

	// ── Evolution data ────────────────────────────────────────────────────────
	private final String evolutionBaseClassId;
	private final List<OpenSkillManagementPayload.EvolutionEntry> evolutionOffers;
	private int evolutionIndex = 0;

	// ── Notifications ─────────────────────────────────────────────────────────
	private final List<String> notifications;
	private int notifScrollOffset = 0;

	// ── Computed layout (set in init) ─────────────────────────────────────────
	private int contentW;
	private int tabBarY, tabSkillsX, tabEvoX, tabW;
	private int slotsY;
	private int listX, listY, listW, listRows;
	/** Y of the top of the "What's New" header (only valid when notifications non-empty). */
	private int notifPanelY;
	/** Height of the notification box (rows only, not header). 0 if no notifications. */
	private int notifBoxH;

	// Evolution hit-test bounds (set during render, read during mouseClicked)
	private int evoAcceptX, evoAcceptY;
	private int evoPrevX, evoPrevY;
	private int evoNextX, evoNextY;

	// ── Constructor ───────────────────────────────────────────────────────────

	public SkillManagementScreen(
		List<OpenSkillManagementPayload.SkillEntry> ownedSkills,
		List<String> currentSlots,
		String evolutionBaseClassId,
		List<OpenSkillManagementPayload.EvolutionEntry> evolutionOffers,
		List<String> notifications
	) {
		super(Component.literal("Skills & Abilities"));
		this.skills            = new ArrayList<>(ownedSkills);
		this.evolutionBaseClassId = evolutionBaseClassId;
		this.evolutionOffers   = evolutionOffers;
		this.notifications     = notifications;
		for (int i = 0; i < 5; i++) {
			this.slots[i] = (i < currentSlots.size()) ? currentSlots.get(i) : null;
		}
		this.listItems = buildItemList(ownedSkills);
	}

	private static List<Object> buildItemList(List<OpenSkillManagementPayload.SkillEntry> skills) {
		List<OpenSkillManagementPayload.SkillEntry> active = skills.stream()
			.filter(OpenSkillManagementPayload.SkillEntry::active).collect(Collectors.toList());
		List<OpenSkillManagementPayload.SkillEntry> passive = skills.stream()
			.filter(s -> !s.active()).collect(Collectors.toList());
		List<Object> items = new ArrayList<>();
		if (!active.isEmpty()) {
			items.add("\u2500 Active Skills \u2500");
			items.addAll(active);
		}
		if (!passive.isEmpty()) {
			items.add("\u2500 Passive Skills \u2500");
			items.addAll(passive);
		}
		return items;
	}

	private boolean hasEvolution() {
		return !evolutionOffers.isEmpty();
	}

	// ── Init ──────────────────────────────────────────────────────────────────

	@Override
	protected void init() {
		int cx = width / 2;
		int cy = height / 2;

		contentW   = Math.min(340, width - 40);
		tabBarY    = cy - 130;
		tabW       = contentW / 2;
		tabSkillsX = cx - contentW / 2;
		tabEvoX    = cx;
		listW      = contentW;
		listX      = cx - contentW / 2;

		int contentTop = tabBarY + (hasEvolution() ? TAB_H + 2 : 2);

		// Notification panel: sits between subtitle and slot wheel label.
		// notifPanelY is the Y of the "What's New" header line.
		// notifBoxH is the height of the bordered notification box below it.
		if (!notifications.isEmpty()) {
			int visRows = Math.min(NOTIF_MAX_VIS, notifications.size());
			notifBoxH   = visRows * NOTIF_ROW_H + 6; // 3px top + 3px bottom padding
			notifPanelY = contentTop + 38;            // below title(8)+divider(12)+subtitle(8)+margin(10)
			// Total space consumed: header(10) + gap(4) + box(notifBoxH) + gap(6) = 20 + notifBoxH
			slotsY = notifPanelY + 10 + 4 + notifBoxH + 8;
		} else {
			notifBoxH   = 0;
			notifPanelY = 0;
			slotsY      = contentTop + 50;
		}

		listY    = slotsY + SLOT_H + 28;
		listRows = hasEvolution() ? 5 : 7;

		int listH       = listRows * LIST_ROW_H;
		int scrollHintH = (listItems.size() > listRows) ? 12 : 0;
		int doneY       = listY + listH + scrollHintH + 8;

		addRenderableWidget(Button.builder(
			Component.literal("Done"),
			btn -> saveAndClose()
		).bounds(cx - 40, doneY, 80, 22).build());
	}

	// ── Rendering ─────────────────────────────────────────────────────────────

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		int cx = width / 2;

		if (hasEvolution()) {
			renderTabBar(g, mx, my, cx);
		}

		if (activeTab == TAB_SKILLS) {
			renderSkillsTab(g, mx, my, cx);
		} else {
			renderEvolutionTab(g, mx, my, cx);
		}

		super.render(g, mx, my, delta);
	}

	private void renderTabBar(GuiGraphics g, int mx, int my, int cx) {
		boolean skillsActive = activeTab == TAB_SKILLS;
		boolean skillsHov    = !skillsActive && my >= tabBarY && my < tabBarY + TAB_H
		                       && mx >= tabSkillsX && mx < tabSkillsX + tabW;
		int skillsBg = skillsActive ? 0xFF1C1914 : (skillsHov ? 0xFF141210 : 0xFF0E0C0A);
		g.fill(tabSkillsX, tabBarY, tabSkillsX + tabW, tabBarY + TAB_H, skillsBg);
		if (skillsActive) g.fill(tabSkillsX, tabBarY, tabSkillsX + tabW, tabBarY + 2, C_GOLD);
		g.drawCenteredString(font, "Skills",
			tabSkillsX + tabW / 2, tabBarY + 5, skillsActive ? C_GOLD : C_GREY);

		boolean evoActive = activeTab == TAB_EVOLUTION;
		boolean evoHov    = !evoActive && my >= tabBarY && my < tabBarY + TAB_H
		                    && mx >= tabEvoX && mx < tabEvoX + tabW;
		int evoBg = evoActive ? 0xFF1C1914 : (evoHov ? 0xFF141210 : 0xFF0E0C0A);
		g.fill(tabEvoX, tabBarY, tabEvoX + tabW, tabBarY + TAB_H, evoBg);
		if (evoActive) g.fill(tabEvoX, tabBarY, tabEvoX + tabW, tabBarY + 2, C_GOLD);
		g.drawCenteredString(font, "Evolution \u2605",
			tabEvoX + tabW / 2, tabBarY + 5, evoActive ? C_GOLD : 0xFFFFAA00);

		g.fill(tabSkillsX, tabBarY + TAB_H, tabSkillsX + contentW, tabBarY + TAB_H + 1, C_DIVIDER);
	}

	private void renderSkillsTab(GuiGraphics g, int mx, int my, int cx) {
		int topY = tabBarY + (hasEvolution() ? TAB_H + 4 : 2);

		g.drawCenteredString(font, "Skills & Abilities", cx, topY, C_GOLD);
		g.drawCenteredString(font, DIVIDER_STR, cx, topY + 12, C_DIVIDER);
		g.drawCenteredString(font,
			"Bind active skills to wheel slots. Use with Middle Mouse.",
			cx, topY + 26, C_GREY);

		if (!notifications.isEmpty()) {
			renderNotificationPanel(g, mx, my, cx);
		}

		g.drawCenteredString(font, "Skill Wheel", cx, slotsY - 14, C_WHITE);
		renderSlots(g, mx, my);

		int listHeaderY = listY - 12;
		g.drawString(font, "Your Skills", listX, listHeaderY, C_WHITE);
		if (selectedSlot >= 0) {
			String hint = " \u2190 Click an active skill to assign to Slot " + (selectedSlot + 1);
			g.drawString(font, hint, listX + font.width("Your Skills"), listHeaderY, C_GOLD);
		}

		int listH = listRows * LIST_ROW_H + 4;
		g.fill(listX - 2, listY - 2, listX + listW + 2, listY + listH, 0xAA000000);
		drawBorder(g, listX - 2, listY - 2, listW + 4, listH + 2, C_CARD_BD);
		renderSkillList(g, mx, my);

		if (listItems.size() > listRows) {
			g.drawCenteredString(font, "\u2191\u2193 Scroll", cx, listY + listH + 4, C_GREY);
		}

		renderHoveredSkillTooltip(g, mx, my);
	}

	private void renderNotificationPanel(GuiGraphics g, int mx, int my, int cx) {
		// Header
		g.drawString(font, "What's New", listX, notifPanelY, C_GOLD);

		int boxY = notifPanelY + 12;
		g.fill(listX - 2, boxY - 2, listX + listW + 2, boxY + notifBoxH, 0xAA000000);
		drawBorder(g, listX - 2, boxY - 2, listW + 4, notifBoxH + 2, C_CARD_BD);

		int visRows = notifBoxH / NOTIF_ROW_H; // inverse of how notifBoxH was computed (minus padding)
		// Recompute visRows accurately
		visRows = Math.min(NOTIF_MAX_VIS, notifications.size());

		for (int i = 0; i < visRows; i++) {
			int idx = notifScrollOffset + i;
			if (idx >= notifications.size()) break;
			String msg = notifications.get(idx);
			int color = notifColor(msg);
			int ry = boxY + 3 + i * NOTIF_ROW_H;
			g.drawString(font, msg, listX + 4, ry, color);
		}

		// Scroll hint if there are more notifications than visible rows
		if (notifications.size() > visRows) {
			int remaining = notifications.size() - visRows - notifScrollOffset;
			if (remaining > 0) {
				g.drawString(font, "\u2193 " + remaining + " more...",
					listX + 4, boxY + notifBoxH - NOTIF_ROW_H, C_GREY);
			}
		}
	}

	private static int notifColor(String msg) {
		if (msg.startsWith("[Skill Change")) return C_NOTIF_CHG;
		if (msg.startsWith("[Skill"))        return C_NOTIF_SKILL;
		return C_NOTIF_LVL; // level-up
	}

	private void renderSlots(GuiGraphics g, int mx, int my) {
		int totalW  = 5 * SLOT_W + 4 * SLOT_GAP;
		int startX  = width / 2 - totalW / 2;
		for (int i = 0; i < 5; i++) {
			int sx = startX + i * (SLOT_W + SLOT_GAP);
			int sy = slotsY;
			boolean selected = (selectedSlot == i);
			boolean hovered  = mx >= sx && mx < sx + SLOT_W && my >= sy && my < sy + SLOT_H;

			g.fill(sx, sy, sx + SLOT_W, sy + SLOT_H, C_CARD);
			drawBorder(g, sx, sy, SLOT_W, SLOT_H,
				selected ? C_SEL_BD : (hovered ? 0xFF9B7840 : C_CARD_BD));

			g.drawCenteredString(font, String.valueOf(i + 1), sx + SLOT_W / 2, sy + 3, C_GREY);

			String skillId   = slots[i];
			String label;
			int    labelColor;
			if (skillId != null) {
				OpenSkillManagementPayload.SkillEntry entry = findSkill(skillId);
				label      = entry != null ? abbreviate(entry.displayName(), SLOT_W - 4) : skillId;
				labelColor = C_WHITE;
			} else {
				label      = "Empty";
				labelColor = C_EMPTY;
			}
			g.drawCenteredString(font, label, sx + SLOT_W / 2, sy + SLOT_H / 2 + 1, labelColor);
		}
	}

	private void renderSkillList(GuiGraphics g, int mx, int my) {
		int visible = Math.min(listRows, listItems.size() - scrollOffset);
		for (int i = 0; i < visible; i++) {
			Object item = listItems.get(scrollOffset + i);
			int ry = listY + i * LIST_ROW_H;

			if (item instanceof String header) {
				g.fill(listX, ry, listX + listW, ry + LIST_ROW_H, 0x44000000);
				g.drawCenteredString(font, header, listX + listW / 2, ry + (LIST_ROW_H - 8) / 2, C_SECTION);

			} else if (item instanceof OpenSkillManagementPayload.SkillEntry skill) {
				boolean hovered  = mx >= listX && mx < listX + listW && my >= ry && my < ry + LIST_ROW_H;
				boolean isBound  = isSkillBound(skill.id());
				boolean isActive = skill.active();

				int rowBg = hovered ? C_ROW_HOV : (isBound ? 0x2200AAFF : 0);
				if (rowBg != 0) g.fill(listX, ry, listX + listW, ry + LIST_ROW_H, rowBg);

				int nameColor = isBound ? 0xFFAAAAFF : (isActive ? C_WHITE : C_GREY);
				g.drawString(font, skill.displayName(), listX + 4, ry + (LIST_ROW_H - 8) / 2, nameColor);

				String pwLabel = "PW" + skill.powerLevel();
				int pwX = listX + listW - font.width(pwLabel) - 4;
				g.drawString(font, pwLabel, pwX, ry + (LIST_ROW_H - 8) / 2, C_GREY);

				if (isActive) {
					String tag = "[Active]";
					g.drawString(font, tag, pwX - font.width(tag) - 6, ry + (LIST_ROW_H - 8) / 2, C_GOLD);
				}
			}
		}
	}

	private void renderEvolutionTab(GuiGraphics g, int mx, int my, int cx) {
		OpenSkillManagementPayload.EvolutionEntry entry = evolutionOffers.get(evolutionIndex);

		int topY = tabBarY + TAB_H + 4;
		g.drawCenteredString(font, "Evolution Milestone", cx, topY, C_GOLD);
		g.drawCenteredString(font, DIVIDER_STR, cx, topY + 12, C_DIVIDER);
		g.drawCenteredString(font,
			"Your [" + evolutionBaseClassId + "] has reached a milestone.", cx, topY + 26, C_GREY);
		g.drawCenteredString(font, "Choose your evolution path:", cx, topY + 40, C_WHITE);

		int cardW = Math.min(290, contentW - 20);
		int cardX = cx - cardW / 2;
		int cardY = topY + 54;
		int cardH = 80;
		boolean cardHov = mx >= cardX && mx < cardX + cardW && my >= cardY && my < cardY + cardH;
		g.fill(cardX, cardY, cardX + cardW, cardY + cardH, cardHov ? C_CARD_HL : C_CARD);
		drawBorder(g, cardX, cardY, cardW, cardH, C_CARD_BD);
		g.drawCenteredString(font, entry.displayName(), cx, cardY + 10, C_GOLD);

		String desc = entry.description();
		int maxDescW = cardW - 16;
		if (font.width(desc) <= maxDescW) {
			g.drawCenteredString(font, desc, cx, cardY + 30, C_GREY);
		} else {
			int split = findWrapPoint(desc, maxDescW);
			g.drawCenteredString(font, desc.substring(0, split).trim(), cx, cardY + 22, C_GREY);
			g.drawCenteredString(font, desc.substring(split).trim(),    cx, cardY + 34, C_GREY);
		}

		int navY = cardY + cardH + 12;
		if (evolutionOffers.size() > 1) {
			evoPrevX = cx - 120; evoPrevY = navY;
			boolean prevHov = mx >= evoPrevX && mx < evoPrevX + EVO_NAV_W
			               && my >= evoPrevY && my < evoPrevY + EVO_NAV_H;
			drawColorButton(g, evoPrevX, evoPrevY, EVO_NAV_W, EVO_NAV_H,
				"\u25c4 Prev", prevHov ? 0xFF3A3030 : 0xFF2A2020);

			g.drawCenteredString(font,
				"Option " + (evolutionIndex + 1) + " of " + evolutionOffers.size(),
				cx, navY + 6, C_GREY);

			evoNextX = cx + 68; evoNextY = navY;
			boolean nextHov = mx >= evoNextX && mx < evoNextX + EVO_NAV_W
			               && my >= evoNextY && my < evoNextY + EVO_NAV_H;
			drawColorButton(g, evoNextX, evoNextY, EVO_NAV_W, EVO_NAV_H,
				"Next \u25ba", nextHov ? 0xFF3A3030 : 0xFF2A2020);
		}

		evoAcceptX = cx - EVO_BTN_W / 2;
		evoAcceptY = navY + EVO_NAV_H + 8;
		boolean accHov = mx >= evoAcceptX && mx < evoAcceptX + EVO_BTN_W
		              && my >= evoAcceptY && my < evoAcceptY + EVO_BTN_H;
		drawColorButton(g, evoAcceptX, evoAcceptY, EVO_BTN_W, EVO_BTN_H,
			"Choose this Path", accHov ? C_GREEN_HL : C_GREEN);

		g.drawCenteredString(font, "Your choice shapes your future. Choose wisely.",
			cx, evoAcceptY + EVO_BTN_H + 8, C_GREY);
	}

	// ── Input ─────────────────────────────────────────────────────────────────

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
		double mx = event.x();
		double my = event.y();
		int button = event.button();

		if (hasEvolution() && button == 0 && my >= tabBarY && my < tabBarY + TAB_H) {
			if (mx >= tabSkillsX && mx < tabSkillsX + tabW) { activeTab = TAB_SKILLS;    return true; }
			if (mx >= tabEvoX    && mx < tabEvoX    + tabW) { activeTab = TAB_EVOLUTION; return true; }
		}

		if (activeTab == TAB_SKILLS) {
			int totalW = 5 * SLOT_W + 4 * SLOT_GAP;
			int startX = width / 2 - totalW / 2;
			for (int i = 0; i < 5; i++) {
				int sx = startX + i * (SLOT_W + SLOT_GAP);
				if (mx >= sx && mx < sx + SLOT_W && my >= slotsY && my < slotsY + SLOT_H) {
					if (button == 1) {
						slots[i] = null;
						if (selectedSlot == i) selectedSlot = -1;
					} else {
						selectedSlot = (selectedSlot == i) ? -1 : i;
					}
					return true;
				}
			}

			if (selectedSlot >= 0 && mx >= listX && mx < listX + listW) {
				int row = (int)((my - listY) / LIST_ROW_H);
				int idx = scrollOffset + row;
				if (button == 0 && row >= 0 && idx < listItems.size()) {
					Object item = listItems.get(idx);
					if (item instanceof OpenSkillManagementPayload.SkillEntry skill && skill.active()) {
						slots[selectedSlot] = skill.id();
						selectedSlot = -1;
						return true;
					}
				}
			}

		} else if (activeTab == TAB_EVOLUTION && hasEvolution() && button == 0) {
			if (mx >= evoAcceptX && mx < evoAcceptX + EVO_BTN_W
			 && my >= evoAcceptY && my < evoAcceptY + EVO_BTN_H) {
				ClientPlayNetworking.send(new EvolutionChoicePayload(evolutionOffers.get(evolutionIndex).id()));
				onClose();
				return true;
			}
			if (evolutionOffers.size() > 1) {
				if (mx >= evoPrevX && mx < evoPrevX + EVO_NAV_W && my >= evoPrevY && my < evoPrevY + EVO_NAV_H) {
					evolutionIndex = Math.floorMod(evolutionIndex - 1, evolutionOffers.size());
					return true;
				}
				if (mx >= evoNextX && mx < evoNextX + EVO_NAV_W && my >= evoNextY && my < evoNextY + EVO_NAV_H) {
					evolutionIndex = Math.floorMod(evolutionIndex + 1, evolutionOffers.size());
					return true;
				}
			}
		}

		return super.mouseClicked(event, consumed);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		if (activeTab != TAB_SKILLS) return false;

		// Scroll notification panel if mouse is over it
		if (!notifications.isEmpty() && notifBoxH > 0) {
			int boxY = notifPanelY + 12;
			if (mx >= listX && mx < listX + listW && my >= boxY && my < boxY + notifBoxH) {
				int visRows  = Math.min(NOTIF_MAX_VIS, notifications.size());
				int maxOff   = Math.max(0, notifications.size() - visRows);
				notifScrollOffset = (int) Math.max(0, Math.min(maxOff, notifScrollOffset - scrollY));
				return true;
			}
		}

		// Otherwise scroll the skill list
		int maxOffset = Math.max(0, listItems.size() - listRows);
		scrollOffset = (int) Math.max(0, Math.min(maxOffset, scrollOffset - scrollY));
		return true;
	}

	@Override
	public boolean shouldCloseOnEsc() { return true; }

	@Override
	public boolean isPauseScreen() { return false; }

	// ── Helpers ───────────────────────────────────────────────────────────────

	private void saveAndClose() {
		List<String> slotList = Arrays.asList(slots);
		ClientPlayNetworking.send(new UpdateActiveSkillSlotsPayload(slotList));
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

	private int findWrapPoint(String text, int maxWidth) {
		int best = text.length();
		for (int i = text.length() - 1; i > 0; i--) {
			if (text.charAt(i) == ' ' && font.width(text.substring(0, i)) <= maxWidth) {
				best = i;
				break;
			}
		}
		return best;
	}

	private void renderHoveredSkillTooltip(GuiGraphics g, int mx, int my) {
		if (mx < listX || mx >= listX + listW) return;
		int row = (my - listY) / LIST_ROW_H;
		int idx = scrollOffset + row;
		if (row < 0 || idx >= listItems.size()) return;
		int ry = listY + row * LIST_ROW_H;
		if (my < ry || my >= ry + LIST_ROW_H) return;
		if (!(listItems.get(idx) instanceof OpenSkillManagementPayload.SkillEntry skill)) return;

		List<Component> tooltip = List.of(
			Component.literal(skill.displayName()).withStyle(s -> s.withColor(0xFFD700)),
			Component.literal((skill.active() ? "Active" : "Passive") + "  |  Power Level " + skill.powerLevel())
			         .withStyle(s -> s.withColor(0xAAAAAA)),
			Component.literal(""),
			Component.literal(skill.description()).withStyle(s -> s.withColor(0xFFFFFF))
		);
		g.setComponentTooltipForNextFrame(font, tooltip, mx, my);
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
		int a  = (color >> 24) & 0xFF;
		int r  = Math.min(255, (int)(((color >> 16) & 0xFF) * f));
		int gr = Math.min(255, (int)(((color >> 8)  & 0xFF) * f));
		int b  = Math.min(255, (int)(( color        & 0xFF) * f));
		return (a << 24) | (r << 16) | (gr << 8) | b;
	}
}
