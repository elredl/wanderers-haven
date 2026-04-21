package com.wanderershaven.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

/**
 * Resolves a weapon category string from a player or item stack, using
 * the best available source across loaded soft dependencies.
 *
 * Internal vocabulary (maps directly to evolution prerequisite keys):
 *
 *   "duelist"     — Sword, Rapier, Sai, Katana, Cutlass  (light, single-handed blades)
 *   "bladedancer" — Chakram, Twinblades
 *   "blademaster" — Claymore, Longsword, Greatsword       (heavy, two-handed blades)
 *   "spear"       — Spear, Glaive, Warglaive, Halberd
 *   "axe"         — Axe                                   (regular axes only)
 *   "mauler"      — Greathammer, Greataxe
 *   "scythe"      — Scythe
 *   "ranged"      — Bow, Crossbow                         (tracked but no evolution yet)
 *
 * Better Combat category strings are mapped to this vocabulary via
 * {@link #normalizeBcCategory(String)}.
 */
public final class WeaponCategoryResolver {

	private WeaponCategoryResolver() {}

	/**
	 * Resolves the weapon category for a kill event.
	 *
	 * Priority:
	 *   1. Better Combat {@code currentAttack} category — most accurate, tracks
	 *      the actual combo weapon used. May be {@code null} if the attack
	 *      completed before the kill event fires.
	 *   2. Item tag / class fallback via {@link #resolveFromItem}.
	 */
	public static String resolveKillWeapon(ServerPlayer player) {
		if (BetterCombatCompat.LOADED) {
			String bcCategory = BetterCombatCompat.getWeaponCategory(player);
			if (bcCategory != null) return normalizeBcCategory(bcCategory);
		}
		return resolveFromItem(player.getMainHandItem());
	}

	/**
	 * Resolves weapon category from the item stack alone, without Better Combat.
	 * Used as a fallback and for non-BC contexts.
	 */
	public static String resolveFromItem(ItemStack stack) {
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();

		// ── Wanderers Haven custom weapons ────────────────────────────────────
		if (path.endsWith("_twinblades")) return "bladedancer";
		if (path.endsWith("_greatsword")) return "blademaster";
		if (path.endsWith("_rapier")) return "duelist";
		if (path.endsWith("_greathammer")) return "mauler";
		if (path.endsWith("_scythe")) return "scythe";
		if (path.endsWith("_mace")) return "mauler";
		if (path.endsWith("_crossbow")) return "ranged";
		if (path.endsWith("_buckler")) return "ranged";

		// ── Bladedancer ──────────────────────────────────────────────────────
		// ── Duelist (vanilla swords) ──────────────────────────────────────────
		if (stack.is(ItemTags.SWORDS))                   return "duelist";

		// ── Axe (regular only — greataxe is mauler above) ────────────────────
		if (stack.getItem() instanceof AxeItem)          return "axe";

		// ── Ranged ────────────────────────────────────────────────────────────
		if (stack.getItem() instanceof BowItem
			|| stack.getItem() instanceof CrossbowItem)  return "ranged";

		return null;
	}

	/**
	 * Maps Better Combat weapon category strings to our internal vocabulary.
	 *
	 * BC categories are defined in weapon config files per mod. Unknown categories pass through
	 * as-is so they still get stored in signals even if not yet mapped.
	 */
	static String normalizeBcCategory(String bcCategory) {
		return switch (bcCategory.toLowerCase()) {
			// Duelist — light single-handed blades
			case "sword", "rapier", "sai", "katana",
				"cutlass", "saber", "sabre"              -> "duelist";

			// Bladedancer
			case "chakram", "twinblade", "twin_blade"    -> "bladedancer";

			// Blademaster — heavy two-handed blades
			case "claymore", "longsword", "greatsword"   -> "blademaster";

			// Spear-class
			case "spear", "glaive", "warglaive",
				"war_glaive", "halberd", "trident"       -> "spear";

			// Axe (regular)
			case "axe", "hatchet"                        -> "axe";

			// Mauler — heavy blunt / great axes
			case "greataxe", "great_axe",
				"greathammer", "great_hammer",
				"warhammer", "war_hammer", "mace"       -> "mauler";

			// Scythe
			case "scythe"                                -> "scythe";

			// Ranged
			case "bow", "crossbow"                       -> "ranged";

			default                                      -> bcCategory;
		};
	}
}
