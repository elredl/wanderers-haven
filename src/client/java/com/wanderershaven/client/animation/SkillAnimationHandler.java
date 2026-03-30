package com.wanderershaven.client.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

/**
 * Client-side handler for active skill animations.
 *
 * Each case currently plays a vanilla arm swing as a placeholder.
 * When GeckoLib player animations are implemented, replace each
 * {@code swing()} call with the corresponding GeckoLib animation trigger.
 */
public final class SkillAnimationHandler {
	private SkillAnimationHandler() {}

	public static void play(String skillId) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		switch (skillId) {
			// TODO: replace with GeckoLib animation per skill
			case "warrior_bludgeon"        -> mc.player.swing(InteractionHand.MAIN_HAND);
			case "warrior_piercing_charge" -> mc.player.swing(InteractionHand.MAIN_HAND);
			case "warrior_heavy_strikes"   -> mc.player.swing(InteractionHand.MAIN_HAND);
			case "warrior_battle_cry_weak" -> mc.player.swing(InteractionHand.MAIN_HAND);
			default                        -> mc.player.swing(InteractionHand.MAIN_HAND);
		}
	}
}
