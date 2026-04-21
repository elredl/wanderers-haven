package com.wanderershaven.item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class TwinCrossbowItem extends Item {

	private static final int RELOAD_TICKS = 22;
	private static final int SECOND_SHOT_DELAY_TICKS = 6;
	private static final float SHOT_VELOCITY = 2.1f;
	private static final float SHOT_INACCURACY = 2.2f;
	private static final double SHOT_DAMAGE = 0.8d;
	private static final List<PendingShot> PENDING_SHOTS = new ArrayList<>();

	public TwinCrossbowItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack weapon = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(weapon)) {
			return InteractionResult.FAIL;
		}

		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}

		boolean fired = false;
		fired |= fireShot(serverLevel, serverPlayer, weapon, -3.0f);

		if (!fired) {
			return InteractionResult.FAIL;
		}

		long fireAtTick = serverLevel.getServer().overworld().getGameTime() + SECOND_SHOT_DELAY_TICKS;
		PENDING_SHOTS.add(new PendingShot(serverPlayer.getUUID(), fireAtTick, 3.0f));

		serverPlayer.getCooldowns().addCooldown(weapon, RELOAD_TICKS);
		serverPlayer.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 8, 0, false, false, false));
		serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
			SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.85f, 0.95f + level.random.nextFloat() * 0.1f);
		return InteractionResult.CONSUME;
	}

	private static boolean fireShot(ServerLevel level, ServerPlayer player, ItemStack weapon, float yawOffset) {
		ItemStack projectile = player.getProjectile(weapon);
		boolean infinite = player.getAbilities().instabuild;
		if (projectile.isEmpty() && !infinite) {
			return false;
		}

		ItemStack ammoForShot = projectile.isEmpty() ? new ItemStack(Items.ARROW) : projectile.copyWithCount(1);
		ArrowItem arrowItem = ammoForShot.getItem() instanceof ArrowItem shotArrow ? shotArrow : (ArrowItem) Items.ARROW;
		AbstractArrow arrow = arrowItem.createArrow(level, ammoForShot, player, weapon);
		arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + yawOffset, 0.0f, SHOT_VELOCITY, SHOT_INACCURACY);
		arrow.setBaseDamage(SHOT_DAMAGE);
		if (infinite) {
			arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
		}

		level.addFreshEntity(arrow);
		if (!infinite && !projectile.isEmpty()) {
			projectile.shrink(1);
		}
		return true;
	}

	public static void tickServer(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		Iterator<PendingShot> iterator = PENDING_SHOTS.iterator();
		while (iterator.hasNext()) {
			PendingShot pendingShot = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(pendingShot.playerId());
			if (player == null || !player.isAlive()) {
				iterator.remove();
				continue;
			}
			if (now < pendingShot.fireAtTick()) {
				continue;
			}
			if (!(player.level() instanceof ServerLevel level)) {
				iterator.remove();
				continue;
			}

			ItemStack weapon = player.getMainHandItem();
			if (!(weapon.getItem() instanceof TwinCrossbowItem)) {
				weapon = player.getOffhandItem();
			}
			if (weapon.getItem() instanceof TwinCrossbowItem) {
				boolean fired = fireShot(level, player, weapon, pendingShot.yawOffset());
				if (fired) {
					level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.8f,
						1.05f + level.random.nextFloat() * 0.08f);
				}
			}
			iterator.remove();
		}
	}

	private record PendingShot(UUID playerId, long fireAtTick, float yawOffset) {}
}
