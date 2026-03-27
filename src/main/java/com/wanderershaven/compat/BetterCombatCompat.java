package com.wanderershaven.compat;

import net.bettercombat.api.AttackHand;
import net.bettercombat.api.EntityPlayer_BetterCombat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;

/**
 * Isolates all Better Combat API calls behind a runtime presence check.
 *
 * Better Combat is a soft dependency — this mod works without it, but
 * weapon category tracking on COMBAT_HIT signals requires it.
 *
 * Safety contract: BC class references live exclusively in method bodies.
 * The JVM loads class bodies lazily, so as long as no method here is called
 * unless {@link #LOADED} is true, no {@link NoClassDefFoundError} will occur
 * when BC is absent.
 *
 * Never call methods on this class without first checking {@link #LOADED}.
 */
public final class BetterCombatCompat {

	public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("bettercombat");

	private BetterCombatCompat() {}

	/**
	 * Returns the Better Combat weapon category for the player's active attack,
	 * or {@code null} if no attack is in progress.
	 *
	 * <b>Only call when {@link #LOADED} is {@code true}.</b>
	 */
	public static String getWeaponCategory(ServerPlayer player) {
		AttackHand attack = ((EntityPlayer_BetterCombat) player).getCurrentAttack();
		if (attack == null) return null;
		String category = attack.attributes().category();
		return (category != null && !category.isBlank()) ? category : null;
	}

	/**
	 * Enriches a COMBAT_HIT context map with Better Combat weapon attributes:
	 * {@code weapon_category}, {@code two_handed}, {@code combo_hit}.
	 *
	 * <b>Only call when {@link #LOADED} is {@code true}.</b>
	 */
	public static void enrichCombatHitContext(ServerPlayer player, Map<String, String> context) {
		AttackHand attack = ((EntityPlayer_BetterCombat) player).getCurrentAttack();
		if (attack == null) return;
		String category = attack.attributes().category();
		if (category != null && !category.isBlank()) {
			context.put("weapon_category", WeaponCategoryResolver.normalizeBcCategory(category));
		}
		context.put("two_handed", Boolean.toString(attack.attributes().isTwoHanded()));
		context.put("combo_hit", Integer.toString(attack.combo().current()));
	}
}
