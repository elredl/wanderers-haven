package com.wanderershaven;

import com.wanderershaven.classsystem.ClassSystemBootstrap;
import com.wanderershaven.command.WhCommand;
import com.wanderershaven.network.WanderersHavenNetworking;
import com.wanderershaven.skill.SkillEffectService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WanderersHavenMod implements ModInitializer {
	public static final String MOD_ID = "wanderers_haven";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ClassSystemBootstrap.initialize();
		WanderersHavenNetworking.registerCommon();
		WanderersHavenNetworking.registerServerReceiver();
		WanderersHavenNetworking.registerSleepHook();
		SkillEffectService.register();
		CommandRegistrationCallback.EVENT.register(WhCommand::register);
		LOGGER.info("Wanderers Haven initialized");
	}
}
