package com.wanderershaven;

import com.wanderershaven.client.ClientSkillState;
import com.wanderershaven.client.WanderersHavenKeybindings;
import com.wanderershaven.client.hud.DangersenseHud;
import com.wanderershaven.client.screen.ClassSelectionScreen;
import com.wanderershaven.client.screen.EvolutionSelectionScreen;
import com.wanderershaven.client.screen.RadialSkillMenuScreen;
import com.wanderershaven.client.screen.SkillManagementScreen;
import com.wanderershaven.network.OpenClassSelectionPayload;
import com.wanderershaven.network.OpenEvolutionSelectionPayload;
import com.wanderershaven.network.OpenSkillManagementPayload;
import com.wanderershaven.network.SyncPlayerSkillsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class WanderersHavenModClient implements ClientModInitializer {

	/**
	 * Payload received from the server but not yet shown because another screen was open.
	 * Opened as soon as no other screen is active (e.g. after ClassSelectionScreen closes).
	 */
	public static volatile OpenSkillManagementPayload pendingSkillManagement = null;

	/**
	 * Tracks whether the radial-menu button (MMB) was held on the last tick.
	 * Used to detect a fresh press so the radial screen only opens once per press.
	 */
	private static boolean radialWasHeld = false;

	@Override
	public void onInitializeClient() {
		WanderersHavenKeybindings.register();

		// Class-selection screen (pending class decisions)
		ClientPlayNetworking.registerGlobalReceiver(OpenClassSelectionPayload.TYPE, (payload, context) ->
			context.client().execute(() ->
				context.client().setScreen(new ClassSelectionScreen(payload.pendingClasses()))
			)
		);

		// Skill ownership sync
		ClientPlayNetworking.registerGlobalReceiver(SyncPlayerSkillsPayload.TYPE, (payload, context) ->
			context.client().execute(() ->
				ClientSkillState.update(payload.skillIds())
			)
		);

		// Skill management screen — always store; open immediately if no screen is active,
		// otherwise queue so it appears after ClassSelectionScreen (or anything else) closes.
		ClientPlayNetworking.registerGlobalReceiver(OpenSkillManagementPayload.TYPE, (payload, context) ->
			context.client().execute(() -> {
				ClientSkillState.updateSlots(payload.slots());
				if (context.client().screen == null) {
					context.client().setScreen(new SkillManagementScreen(payload.ownedSkills(), payload.slots()));
				} else {
					pendingSkillManagement = payload;
				}
			})
		);

		// Evolution selection screen
		ClientPlayNetworking.registerGlobalReceiver(OpenEvolutionSelectionPayload.TYPE, (payload, context) ->
			context.client().execute(() ->
				context.client().setScreen(new EvolutionSelectionScreen(payload.baseClassId(), payload.offers()))
			)
		);

		// Tick handler:
		//   1. Radial menu — detect MMB press via GLFW hardware state (avoids KeyMapping reset issues).
		//      Open on first tick where button is pressed; screen closes itself when released.
		//   2. Queued skill management — open once no screen is active.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Radial menu — use GLFW directly to detect a fresh press
			long window = Minecraft.getInstance().getWindow().handle();
			boolean radialHeld = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
			if (radialHeld && !radialWasHeld && client.screen == null) {
				client.execute(() -> client.setScreen(new RadialSkillMenuScreen()));
			}
			radialWasHeld = radialHeld;

			// Deferred skill management screen
			if (pendingSkillManagement != null && client.screen == null) {
				OpenSkillManagementPayload p = pendingSkillManagement;
				pendingSkillManagement = null;
				client.execute(() -> client.setScreen(new SkillManagementScreen(p.ownedSkills(), p.slots())));
			}
		});

		DangersenseHud.register();
	}
}
