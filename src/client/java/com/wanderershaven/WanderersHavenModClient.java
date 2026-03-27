package com.wanderershaven;

import com.wanderershaven.client.ClientSkillState;
import com.wanderershaven.client.WanderersHavenKeybindings;
import com.wanderershaven.client.hud.DangersenseHud;
import com.wanderershaven.client.screen.ClassSelectionScreen;
import com.wanderershaven.client.screen.RadialSkillMenuScreen;
import com.wanderershaven.client.screen.SkillManagementScreen;
import com.wanderershaven.network.OpenClassSelectionPayload;
import com.wanderershaven.network.OpenSkillManagementPayload;
import com.wanderershaven.network.SyncPlayerSkillsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class WanderersHavenModClient implements ClientModInitializer {

	/** Whether the radial-menu key was pressed on the previous tick. */
	private boolean radialWasDown = false;

	@Override
	public void onInitializeClient() {
		// Register keybindings
		WanderersHavenKeybindings.register();

		// Open the class-selection screen when the server sends pending classes
		ClientPlayNetworking.registerGlobalReceiver(OpenClassSelectionPayload.TYPE, (payload, context) ->
			context.client().execute(() ->
				context.client().setScreen(new ClassSelectionScreen(payload.pendingClasses()))
			)
		);

		// Update the local skill state when the server syncs skill ownership
		ClientPlayNetworking.registerGlobalReceiver(SyncPlayerSkillsPayload.TYPE, (payload, context) ->
			context.client().execute(() ->
				ClientSkillState.update(payload.skillIds())
			)
		);

		// Open the Skill Management screen when the server signals it (triggered by sleeping)
		ClientPlayNetworking.registerGlobalReceiver(OpenSkillManagementPayload.TYPE, (payload, context) ->
			context.client().execute(() -> {
				// Only open if no screen is already active (e.g. class selection takes priority)
				if (context.client().screen == null) {
					ClientSkillState.updateSlots(payload.slots());
					context.client().setScreen(
						new SkillManagementScreen(payload.ownedSkills(), payload.slots())
					);
				}
			})
		);

		// Radial menu — open on key press, close (and fire) on release
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			boolean isDown = WanderersHavenKeybindings.radialMenu.isDown();

			if (isDown && !radialWasDown && client.screen == null) {
				// Key just pressed — open the radial menu
				client.execute(() -> client.setScreen(new RadialSkillMenuScreen()));
			} else if (!isDown && radialWasDown && client.screen instanceof RadialSkillMenuScreen radial) {
				// Key just released while radial is open — fire and close
				client.execute(radial::onRadialKeyReleased);
			}

			radialWasDown = isDown;
		});

		// Register Dangersense HUD overlay
		DangersenseHud.register();
	}
}
