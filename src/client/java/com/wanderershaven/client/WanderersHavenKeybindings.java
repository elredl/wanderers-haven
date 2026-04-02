package com.wanderershaven.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * Registers and exposes all Wanderers Haven client keybindings.
 * Call {@link #register()} during client initialisation.
 */
@Environment(EnvType.CLIENT)
public final class WanderersHavenKeybindings {

	/** Custom keybinding category shown in the controls screen. */
	public static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath("wanderers_haven", "keycategory"));

	/** Opens the radial active-skill menu while held. Default: Middle Mouse Button. */
	public static KeyMapping radialMenu;

	/** Opens the character stats screen. Default: M. */
	public static KeyMapping statsScreen;

	private WanderersHavenKeybindings() {}

	public static void register() {
		radialMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.wanderers_haven.radial_menu",
			InputConstants.Type.MOUSE,
			2, // GLFW_MOUSE_BUTTON_MIDDLE
			CATEGORY
		));

		statsScreen = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.wanderers_haven.stats",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_M,
			CATEGORY
		));
	}
}
