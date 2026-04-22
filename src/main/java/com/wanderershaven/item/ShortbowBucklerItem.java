package com.wanderershaven.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;

public final class ShortbowBucklerItem extends AxeItem {

	private static final float MAX_POWER = 0.82f;
	private static final float CHARGE_TICKS = 12.0f;
	private static final float PROJECTILE_SPEED_MULT = 2.4f;
	private static final float PROJECTILE_INACCURACY = 1.2f;
	private static final double DAMAGE_MULT = 0.8d;
	private static final float BONUS_MELEE_DAMAGE = 0.0f;
	private static final double KNOCKBACK_STRENGTH = 0.8d;

	public ShortbowBucklerItem(ToolMaterial material, float attackDamage, float attackSpeed, Properties properties) {
		super(material, attackDamage, attackSpeed, properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!player.getAbilities().instabuild && player.getProjectile(stack).isEmpty()) {
			return InteractionResult.FAIL;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
		if (!level.isClientSide() && livingEntity instanceof ServerPlayer player) {
			player.addEffect(new MobEffectInstance(MobEffects.SPEED, 2, 0, false, false, false));
		}
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
		if (!(livingEntity instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) {
			return false;
		}

		int useTicks = getUseDuration(stack, livingEntity) - timeCharged;
		float charge = useTicks / CHARGE_TICKS;
		float power = (charge * charge + charge * 2.0f) / 3.0f;
		if (power > MAX_POWER) {
			power = MAX_POWER;
		}
		if (power < 0.1f) {
			return false;
		}

		ItemStack projectile = player.getProjectile(stack);
		boolean infinite = player.getAbilities().instabuild;
		if (projectile.isEmpty() && !infinite) {
			return false;
		}

		ItemStack ammoForShot = projectile.isEmpty() ? new ItemStack(Items.ARROW) : projectile.copyWithCount(1);
		ArrowItem arrowItem = ammoForShot.getItem() instanceof ArrowItem shotArrow ? shotArrow : (ArrowItem) Items.ARROW;
		AbstractArrow arrow = arrowItem.createArrow(serverLevel, ammoForShot, player, stack);
		arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, power * PROJECTILE_SPEED_MULT, PROJECTILE_INACCURACY);
		arrow.setBaseDamage(1.6d * DAMAGE_MULT);
		if (infinite) {
			arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
		}

		serverLevel.addFreshEntity(arrow);
		if (!infinite && !projectile.isEmpty()) {
			projectile.shrink(1);
		}

		stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
			SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f,
			1.2f / (level.random.nextFloat() * 0.4f + 1.2f) + power * 0.4f);
		player.awardStat(Stats.ITEM_USED.get(this));
		return true;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 72000;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		applyMeleeHitEffects(target, attacker);
		stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
	}

	public static void applyMeleeHitEffects(LivingEntity target, LivingEntity attacker) {
		target.hurt(attacker instanceof Player player ? attacker.damageSources().playerAttack(player) : attacker.damageSources().mobAttack(attacker), BONUS_MELEE_DAMAGE);
		target.knockback(KNOCKBACK_STRENGTH, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
	}
}
