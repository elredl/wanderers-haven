package com.wanderershaven.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wanderershaven.classsystem.ClassSystemBootstrap;
import com.wanderershaven.classsystem.evolution.ClassEvolutionDef;
import com.wanderershaven.classsystem.evolution.EvolutionContext;
import com.wanderershaven.network.PlayerNotificationStore;
import com.wanderershaven.network.WanderersHavenNetworking;
import com.wanderershaven.skill.SkillDefinition;
import com.wanderershaven.skill.SkillEffectService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
				.then(Commands.literal("progress")
					.then(Commands.argument("player", EntityArgument.player())
						.then(Commands.argument("class", StringArgumentType.string())
							.suggests((ctx, builder) -> {
								ClassSystemBootstrap.engine().registeredClasses().keySet()
									.forEach(builder::suggest);
								return builder.buildFuture();
							})
							.executes(ctx -> executeProgress(
								ctx.getSource(),
								EntityArgument.getPlayer(ctx, "player"),
								StringArgumentType.getString(ctx, "class")
							)))))
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
		WanderersHavenNetworking.sendOpenSkillManagement(target);
		source.sendSuccess(
			() -> Component.literal("[WH] Opened skill management GUI for " + target.getName().getString()),
			true
		);
		return 1;
	}

	private static int executeProgress(CommandSourceStack source, ServerPlayer target, String classId) {
		if (!ClassSystemBootstrap.engine().registeredClasses().containsKey(classId)) {
			source.sendFailure(Component.literal("[WH] Unknown class: " + classId));
			return 0;
		}

		var uuid = target.getUUID();
		int level = ClassSystemBootstrap.levelEngine().classLevel(uuid, classId);
		int nextMilestone = ((level / 25) + 1) * 25;

		// Build context (pass empty set for owned classes — we only need raw stats here)
		EvolutionContext ctx = ClassSystemBootstrap.evolutionEngine().buildContext(uuid, level, Set.of());

		source.sendSuccess(() -> Component.literal(
			"\u00a7e--- " + target.getName().getString() + " / " + classId + " ---"), false);
		source.sendSuccess(() -> Component.literal(
			"\u00a77Level: \u00a7f" + level + "  \u00a77(next evolution milestone: \u00a7fLevel " + nextMilestone + "\u00a77)"), false);

		// Combat stats
		source.sendSuccess(() -> Component.literal(
			"\u00a77Kills: \u00a7f" + ctx.totalKills()
			+ "  \u00a77Near-death: \u00a7f" + ctx.nearDeathSurvivals()
			+ "  \u00a77Blocked hits: \u00a7f" + ctx.blockedHits()), false);

		if (!ctx.killsByWeaponCategory().isEmpty()) {
			String wk = ctx.killsByWeaponCategory().entrySet().stream()
				.sorted(java.util.Map.Entry.<String,Integer>comparingByValue().reversed())
				.map(e -> e.getKey() + "=" + e.getValue())
				.collect(Collectors.joining(", "));
			source.sendSuccess(() -> Component.literal("\u00a77Weapon kills: \u00a7f" + wk), false);
		}

		if (!ctx.killsByCategory().isEmpty()) {
			String ek = ctx.killsByCategory().entrySet().stream()
				.sorted(java.util.Map.Entry.<String,Integer>comparingByValue().reversed())
				.map(e -> e.getKey() + "=" + e.getValue())
				.collect(Collectors.joining(", "));
			source.sendSuccess(() -> Component.literal("\u00a77Entity kills: \u00a7f" + ek), false);
		}

		// Evolution status
		String accepted = ClassSystemBootstrap.evolutionEngine().acceptedEvolution(uuid, classId);
		if (accepted != null) {
			source.sendSuccess(() -> Component.literal("\u00a7aEvolution path: " + accepted), false);
		} else {
			List<ClassEvolutionDef> all = ClassSystemBootstrap.evolutionEngine().evolutionsFor(classId);
			if (all.isEmpty()) {
				source.sendSuccess(() -> Component.literal("\u00a77No evolution paths registered for this class."), false);
			} else {
				source.sendSuccess(() -> Component.literal("\u00a77Evolution paths:"), false);
				for (ClassEvolutionDef evo : all) {
					boolean eligible = evo.isEligible(ctx);
					String marker = eligible ? "\u00a7a\u2713 " : "\u00a7c\u2717 ";
					source.sendSuccess(() -> Component.literal(
						"  " + marker + "\u00a7f" + evo.displayName()), false);
				}
			}
		}

		// Owned skills count
		long skillCount = ClassSystemBootstrap.skillEngine()
			.ownedSkillIds(uuid, classId).size();
		source.sendSuccess(() -> Component.literal(
			"\u00a77Skills owned: \u00a7f" + skillCount), false);

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
				+ " — notifications will appear in their next GUI open."
			),
			true
		);
		// Level-up processing (skill rolls, notifications) is deferred to the next GUI open.
		return 1;
	}

	private static int executeGiveSkill(CommandSourceStack source, ServerPlayer target, String skillId) {
		Optional<SkillDefinition> result = ClassSystemBootstrap.skillEngine().forceGrantSkill(target.getUUID(), skillId);

		if (result.isEmpty()) {
			source.sendFailure(Component.literal("[WH] Unknown skill: " + skillId));
			return 0;
		}

		SkillDefinition skill = result.get();
		SkillEffectService.applySkill(target, skill.id());
		PlayerNotificationStore.recordSkillGrant(target.getUUID(), skill.displayName());

		source.sendSuccess(
			() -> Component.literal(
				"[WH] Granted skill [" + skill.displayName() + "] (PW" + skill.powerLevel() + ") to "
				+ target.getName().getString() + " — notification queued for their next GUI open."
			),
			true
		);
		return 1;
	}
}
