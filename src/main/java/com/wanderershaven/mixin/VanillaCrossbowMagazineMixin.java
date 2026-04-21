package com.wanderershaven.mixin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public abstract class VanillaCrossbowMagazineMixin {

	private static final int MAG_SIZE = 2;
	private static final float SHOT_VELOCITY = 2.9f;

	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void wh_useMagazineCrossbow(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		ItemStack weapon = player.getItemInHand(hand);
		ChargedProjectiles loaded = weapon.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);

		if (!loaded.isEmpty()) {
			if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
				cir.setReturnValue(InteractionResult.CONSUME);
				return;
			}

			List<ItemStack> remaining = new ArrayList<>(loaded.getItems());
			ItemStack ammo = remaining.removeFirst();
			weapon.set(DataComponents.CHARGED_PROJECTILES,
				remaining.isEmpty() ? ChargedProjectiles.EMPTY : ChargedProjectiles.of(remaining));

			fireShot(serverLevel, serverPlayer, weapon, ammo);
			serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
				SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.9f, 0.95f + level.random.nextFloat() * 0.1f);
			cir.setReturnValue(InteractionResult.CONSUME);
			return;
		}

		if (player instanceof ServerPlayer serverPlayer
			&& !serverPlayer.getAbilities().instabuild
			&& countArrowAmmo(serverPlayer) < MAG_SIZE) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}

	@Inject(method = "tryLoadProjectiles", at = @At("RETURN"))
	private static void wh_fillMagazine(LivingEntity shooter, ItemStack crossbow, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()) {
			return;
		}

		ChargedProjectiles loaded = crossbow.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
		List<ItemStack> projectiles = new ArrayList<>(loaded.getItems());
		if (projectiles.size() != 1) {
			return;
		}

		if (shooter instanceof Player player && player.getAbilities().instabuild) {
			projectiles.add(projectiles.getFirst().copyWithCount(1));
			crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(projectiles));
			return;
		}

		if (!(shooter instanceof Player player)) {
			return;
		}

		ItemStack secondProjectile = player.getProjectile(crossbow);
		if (secondProjectile.isEmpty()) {
			crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
			return;
		}

		projectiles.add(secondProjectile.copyWithCount(1));
		secondProjectile.shrink(1);
		crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(projectiles));
	}

	private static int countArrowAmmo(ServerPlayer player) {
		int arrows = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.getItem() instanceof ArrowItem) {
				arrows += stack.getCount();
				if (arrows >= MAG_SIZE) {
					return arrows;
				}
			}
		}
		return arrows;
	}

	private static void fireShot(ServerLevel level, ServerPlayer player, ItemStack weapon, ItemStack ammoForShot) {
		ArrowItem arrowItem = ammoForShot.getItem() instanceof ArrowItem shotArrow ? shotArrow : (ArrowItem) Items.ARROW;
		AbstractArrow arrow = arrowItem.createArrow(level, ammoForShot, player, weapon);
		arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, SHOT_VELOCITY, 1.0f);
		if (player.getAbilities().instabuild) {
			arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
		}
		level.addFreshEntity(arrow);
	}
}
