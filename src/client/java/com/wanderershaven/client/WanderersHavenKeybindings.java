package com.wanderershaven.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * Registers and exposes all Wanderers Haven client keybindings.
 * Call {@link #register()} during client initialisation.
 */
@Environment(EnvType.CLIENT)
public final class WanderersHavenKeybindings {
	public static final String CATEGORY = "key.categories.wanderers_haven";

	/** Opens the radial active-skill menu while held. Default: Middle Mouse Button. */
	public static KeyMapping radialMenu;

	private WanderersHavenKeybindings() {}

	public static void register() {
		radialMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.wanderers_haven.radial_menu",
			InputConstants.Type.MOUSE,
			2, // GLFW_MOUSE_BUTTON_MIDDLE
			CATEGORY
		));
	}
}
