package com.wanderershaven.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Soft-dependency integration for Simply Swords.
 *
 * Detection is tag-based only — no Simply Swords classes are imported, so
 * this class is safe to load regardless of whether Simply Swords is installed.
 * When Simply Swords is absent the item tags simply match nothing.
 *
 * When Simply Swords is present, its weapons register under these tags and
 * are automatically picked up here without any further configuration.
 *
 * Future work: if we need to call into Simply Swords' spell or ability API,
 * follow the same isolation pattern as {@link BetterCombatCompat} — put all
 * SS class references in method bodies and guard call sites with {@link #LOADED}.
 */
public final class SimplySwordsCompat {

	public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("simplyswords");

	// ── Duelist weapons (light, single-handed blades) ─────────────────────────
	private static final TagKey<Item> RAPIERS    = tag("rapiers");
	private static final TagKey<Item> KATANAS    = tag("katanas");
	private static final TagKey<Item> CUTLASSES  = tag("cutlasses");
	private static final TagKey<Item> SAIS       = tag("sais");

	// ── Bladedancer weapons ───────────────────────────────────────────────────
	private static final TagKey<Item> CHAKRAMS   = tag("chakrams");
	private static final TagKey<Item> TWINBLADES = tag("twinblades");

	// ── Blademaster weapons (heavy, two-handed blades) ────────────────────────
	private static final TagKey<Item> LONGSWORDS = tag("longswords");
	private static final TagKey<Item> CLAYMORES  = tag("claymores");
	private static final TagKey<Item> GREATSWORDS = tag("greatswords");

	// ── Spear-class weapons ───────────────────────────────────────────────────
	private static final TagKey<Item> SPEARS     = tag("spears");
	private static final TagKey<Item> GLAIVES    = tag("glaives");
	private static final TagKey<Item> WARGLAIVES = tag("warglaives");
	private static final TagKey<Item> HALBERDS   = tag("halberds");

	// ── Mauler weapons (heavy blunt / great axes) ─────────────────────────────
	private static final TagKey<Item> GREATHAMMERS = tag("greathammers");
	private static final TagKey<Item> GREATAXES    = tag("greataxes");

	// ── Scythe ────────────────────────────────────────────────────────────────
	private static final TagKey<Item> SCYTHES    = tag("scythes");

	private SimplySwordsCompat() {}

	// Duelist
	public static boolean isRapier(ItemStack s)    { return s.is(RAPIERS); }
	public static boolean isKatana(ItemStack s)    { return s.is(KATANAS); }
	public static boolean isCutlass(ItemStack s)   { return s.is(CUTLASSES); }
	public static boolean isSai(ItemStack s)       { return s.is(SAIS); }

	// Bladedancer
	public static boolean isChakram(ItemStack s)   { return s.is(CHAKRAMS); }
	public static boolean isTwinblade(ItemStack s) { return s.is(TWINBLADES); }

	// Blademaster
	public static boolean isLongsword(ItemStack s) { return s.is(LONGSWORDS); }
	public static boolean isClaymore(ItemStack s)  { return s.is(CLAYMORES); }
	public static boolean isGreatsword(ItemStack s){ return s.is(GREATSWORDS); }

	// Spear-class
	public static boolean isSpear(ItemStack s)     { return s.is(SPEARS); }
	public static boolean isGlaive(ItemStack s)    { return s.is(GLAIVES); }
	public static boolean isWarglaive(ItemStack s) { return s.is(WARGLAIVES); }
	public static boolean isHalberd(ItemStack s)   { return s.is(HALBERDS); }

	// Mauler
	public static boolean isGreathammer(ItemStack s){ return s.is(GREATHAMMERS); }
	public static boolean isGreataxe(ItemStack s)   { return s.is(GREATAXES); }

	// Scythe
	public static boolean isScythe(ItemStack s)    { return s.is(SCYTHES); }

	private static TagKey<Item> tag(String path) {
		return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("simplyswords", path));
	}
}
