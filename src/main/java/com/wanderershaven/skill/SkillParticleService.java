package com.wanderershaven.skill;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

/** Centralized particle presets used by skill effects. */
public final class SkillParticleService {

	private SkillParticleService() {}

	public static void battleCryWeakCast(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		double x = player.getX();
		double y = player.getY() + 1.0;
		double z = player.getZ();
		level.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y, z, 40, 1.5, 0.8, 1.5, 0.4);
		level.sendParticles(ParticleTypes.CRIT,           x, y, z, 25, 1.5, 0.8, 1.5, 0.6);
	}

	public static void harmonyOfPainCast(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		double x = player.getX();
		double y = player.getY() + 1.0;
		double z = player.getZ();
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 35, 1.4, 0.7, 1.4, 0.2);
		level.sendParticles(ParticleTypes.CRIT,         x, y, z, 25, 1.4, 0.7, 1.4, 0.5);
	}

	public static void fallingPetalsPulse(ServerPlayer player, List<LivingEntity> targets) {
		if (!(player.level() instanceof ServerLevel level)) return;

		// Area bloom around the damage field.
		level.sendParticles(ParticleTypes.CHERRY_LEAVES,
			player.getX(), player.getY() + 1.0, player.getZ(),
			90, 3.0, 1.2, 3.0, 0.02);

		// Extra density where damage is actually being applied.
		for (LivingEntity target : targets) {
			level.sendParticles(ParticleTypes.CHERRY_LEAVES,
				target.getX(), target.getY() + 1.0, target.getZ(),
				16, 0.45, 0.7, 0.45, 0.01);
		}
	}

	public static void grasscutterSlashBurst(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) return;
		double x = player.getX();
		double y = player.getY() + 1.0;
		double z = player.getZ();

		// Large circular slash presence around the player.
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 55, 1.9, 0.9, 1.9, 0.02);
		level.sendParticles(ParticleTypes.CRIT,         x, y, z, 70, 2.1, 1.0, 2.1, 0.05);
	}
}
