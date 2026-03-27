package com.wanderershaven.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wanderershaven.classsystem.ClassSystemBootstrap;
import com.wanderershaven.network.WanderersHavenNetworking;
import com.wanderershaven.skill.SkillDefinition;
import java.util.Optional;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class WhCommand {
	private WhCommand() {
	}

	public static void register(
		CommandDispatcher<CommandSourceStack> dispatcher,
		CommandBuildContext context,
		Commands.CommandSelection selection
	) {
		dispatcher.register(
			Commands.literal("wh")
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
				.then(Commands.literal("gui")
					.then(Commands.argument("player", EntityArgument.player())
						.executes(ctx -> executeGui(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))
					// Also allow /wh gui with no target to open for the executing player
					.executes(ctx -> executeGui(ctx.getSource(), ctx.getSource().getPlayerOrException())))
				.then(Commands.literal("giveclass")
					.then(Commands.argument("player", EntityArgument.player())
						.then(Commands.argument("class", StringArgumentType.string())
							.suggests((ctx, builder) -> {
								ClassSystemBootstrap.engine().registeredClasses().keySet()
									.forEach(builder::suggest);
								return builder.buildFuture();
							})
							.executes(ctx -> executeGiveClass(
								ctx.getSource(),
								EntityArgument.getPlayer(ctx, "player"),
								StringArgumentType.getString(ctx, "class")
							)))))
				.then(Commands.literal("levelup")
					.then(Commands.argument("player", EntityArgument.player())
						.then(Commands.argument("class", StringArgumentType.string())
							.suggests((ctx, builder) -> {
								ClassSystemBootstrap.engine().registeredClasses().keySet()
									.forEach(builder::suggest);
								return builder.buildFuture();
							})
							.executes(ctx -> executeLevelUp(
								ctx.getSource(),
								EntityArgument.getPlayer(ctx, "player"),
								StringArgumentType.getString(ctx, "class"),
								1
							))
							.then(Commands.argument("levels", IntegerArgumentType.integer(1, 100))
								.executes(ctx -> executeLevelUp(
									ctx.getSource(),
									EntityArgument.getPlayer(ctx, "player"),
									StringArgumentType.getString(ctx, "class"),
									IntegerArgumentType.getInteger(ctx, "levels")
								))))))
				.then(Commands.literal("giveskill")
					.then(Commands.argument("player", EntityArgument.player())
						.then(Commands.argument("skill", StringArgumentType.string())
							.suggests((ctx, builder) -> {
								ClassSystemBootstrap.skillEngine().allSkills().stream()
									.map(SkillDefinition::id)
									.forEach(builder::suggest);
								return builder.buildFuture();
							})
							.executes(ctx -> executeGiveSkill(
								ctx.getSource(),
								EntityArgument.getPlayer(ctx, "player"),
								StringArgumentType.getString(ctx, "skill")
							)))))
		);
	}

	private static int executeGui(CommandSourceStack source, ServerPlayer target) {
		var engine = ClassSystemBootstrap.engine();
		// If the player has no pending classes, force all registered (not yet obtained or
		// permanently denied) classes into pending so the GUI has something to show.
		if (engine.pendingClasses(target.getUUID()).isEmpty()) {
			engine.registeredClasses().keySet()
				.forEach(classId -> engine.forcePending(target.getUUID(), classId));
		}

		WanderersHavenNetworking.sendPendingClassesTo(target);
		source.sendSuccess(
			() -> Component.literal("[WH] Opened class selection GUI for " + target.getName().getString()),
			true
		);
		return 1;
	}

	private static int executeGiveClass(CommandSourceStack source, ServerPlayer target, String classId) {
		if (!ClassSystemBootstrap.engine().registeredClasses().containsKey(classId)) {
			source.sendFailure(Component.literal("[WH] Unknown class: " + classId));
			return 0;
		}

		ClassSystemBootstrap.engine().forceGrantClass(target.getUUID(), classId);
		source.sendSuccess(
			() -> Component.literal("[WH] Granted class [" + classId + "] to " + target.getName().getString()),
			true
		);
		target.sendSystemMessage(Component.literal(
			"[Wanderers Haven] You have been granted the [" + classId + "] class!"
		));
		return 1;
	}

	private static int executeLevelUp(CommandSourceStack source, ServerPlayer target, String classId, int levels) {
		if (!ClassSystemBootstrap.engine().registeredClasses().containsKey(classId)) {
			source.sendFailure(Component.literal("[WH] Unknown class: " + classId));
			return 0;
		}

		for (int i = 0; i < levels; i++) {
			ClassSystemBootstrap.levelEngine().grantLevel(target.getUUID(), classId);
		}
		int newLevel = ClassSystemBootstrap.levelEngine().classLevel(target.getUUID(), classId);

		source.sendSuccess(
			() -> Component.literal(
				"[WH] " + target.getName().getString() + " is now Level " + newLevel + " in [" + classId + "]"
			),
			true
		);
		target.sendSystemMessage(Component.literal(
			"[Wanderers Haven] Admin granted you Level " + newLevel + " in [" + classId + "]!"
		));
		return 1;
	}

	private static int executeGiveSkill(CommandSourceStack source, ServerPlayer target, String skillId) {
		Optional<SkillDefinition> result = ClassSystemBootstrap.skillEngine().forceGrantSkill(target.getUUID(), skillId);

		if (result.isEmpty()) {
			source.sendFailure(Component.literal("[WH] Unknown skill: " + skillId));
			return 0;
		}

		SkillDefinition skill = result.get();
		source.sendSuccess(
			() -> Component.literal(
				"[WH] Granted skill [" + skill.displayName() + "] (PW" + skill.powerLevel() + ") to " + target.getName().getString()
			),
			true
		);
		target.sendSystemMessage(Component.literal(
			"[Wanderers Haven] Skill granted: [" + skill.displayName() + "] (PW" + skill.powerLevel() + ") — " + skill.description()
		));
		return 1;
	}
}
