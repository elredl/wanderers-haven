package com.wanderershaven.stat;

import com.wanderershaven.classsystem.ClassSystemBootstrap;
import com.wanderershaven.compat.WeaponCategoryResolver;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Declarative table of every skill and buff stat contribution in the mod.
 *
 * To add a new stat effect: call {@code engine.register(...)} with a source ID
 * and one or more {@link StatContribution}s. The {@link PlayerStatEngine} handles
 * apply/remove/update automatically based on condition predicates.
 *
 * Source naming convention:
 *   - Permanent skill:  the skill's own ID  (e.g. "warrior_lesser_strength")
 *   - Timed buff:       "<source>_buff"      (e.g. "heavy_strikes_buff")
 */
public final class SkillStatTable {
	private SkillStatTable() {}

	public static void register(PlayerStatEngine engine) {

		// ── Permanent skill bonuses ────────────────────────────────────────────

		// Lesser Strength / Enhanced Strength — Enhanced supersedes Lesser (distinct IDs, exclusive conditions)
		engine.register("warrior_lesser_strength",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("lesser_strength"),
				0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "warrior_lesser_strength") && !owns(p, "warrior_enhanced_strength")));

		engine.register("warrior_enhanced_strength",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("enhanced_strength"),
				0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "warrior_enhanced_strength")));

		// Lesser Dexterity / Enhanced Dexterity
		engine.register("warrior_lesser_dexterity",
			StatContribution.always(Attributes.ATTACK_SPEED, id("lesser_dexterity"),
				0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "warrior_lesser_dexterity") && !owns(p, "warrior_enhanced_dexterity")));

		engine.register("warrior_enhanced_dexterity",
			StatContribution.always(Attributes.ATTACK_SPEED, id("enhanced_dexterity"),
				0.18, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "warrior_enhanced_dexterity")));

		// Lesser Speed / Enhanced Speed
		engine.register("warrior_lesser_speed",
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("lesser_speed"),
				0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "warrior_lesser_speed") && !owns(p, "warrior_enhanced_speed")));

		engine.register("warrior_enhanced_speed",
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("enhanced_speed"),
				0.18, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "warrior_enhanced_speed")));

		// Stand Your Ground — permanent knockback immunity
		engine.register("warrior_vanguard_stand_your_ground",
			StatContribution.always(Attributes.KNOCKBACK_RESISTANCE, id("knockback_immunity"),
				1.0, AttributeModifier.Operation.ADD_VALUE,
				p -> owns(p, "warrior_vanguard_stand_your_ground")));

		// ── Dynamic (amount recalculated each tick) ────────────────────────────

		// Berserker Rage — 0% bonus at full health → 50% at 1 HP
		engine.register("warrior_berserker_rage",
			StatContribution.dynamic(Attributes.ATTACK_DAMAGE, id("berserker_rage"),
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				SkillStatTable::berserkerRageAmount,
				p -> owns(p, "warrior_berserker_rage")));

		// ── Conditional (weapon-gated, rechecked each tick) ───────────────────

		// Swift Blade (Duelist) — +20% attack speed while holding a light blade
		engine.register("warrior_duelist_swift_blade",
			StatContribution.always(Attributes.ATTACK_SPEED, id("swift_blade"),
				0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "warrior_duelist_swift_blade") && isDuelistWeapon(p)));

		// Blade Mastery (Blademaster) — +25% damage and +18% attack speed while holding a heavy blade
		engine.register("warrior_blademaster_blade_mastery",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("blade_mastery_damage"),
				0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "warrior_blademaster_blade_mastery") && isBlademasterWeapon(p)),
			StatContribution.always(Attributes.ATTACK_SPEED, id("blade_mastery_speed"),
				0.18, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "warrior_blademaster_blade_mastery") && isBlademasterWeapon(p)));

		// ── Timed buffs (activated / deactivated by SkillEffectService) ────────

		// Heavy Strikes — +12% damage for 10 seconds
		engine.register("heavy_strikes_buff",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("heavy_strikes"),
				0.12, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> engine.isSourceActive(p.getUUID(), "heavy_strikes_buff")));

		// Last Stand — +8% or +12% damage+speed for 10 seconds (amount set at activation)
		engine.register("last_stand_buff",
			StatContribution.dynamic(Attributes.ATTACK_DAMAGE, id("last_stand_damage"),
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> engine.getBuffAmount(p.getUUID(), "last_stand_buff"),
				p -> engine.isSourceActive(p.getUUID(), "last_stand_buff")),
			StatContribution.dynamic(Attributes.ATTACK_SPEED, id("last_stand_speed"),
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> engine.getBuffAmount(p.getUUID(), "last_stand_buff"),
				p -> engine.isSourceActive(p.getUUID(), "last_stand_buff")));

		// Fury Unleashed — fury×1% damage + fury×1% size for 1 minute (amounts set at activation)
		engine.register("fury_unleashed_buff",
			StatContribution.dynamic(Attributes.ATTACK_DAMAGE, id("fury_unleashed_damage"),
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> engine.getBuffAmount(p.getUUID(), "fury_unleashed_buff"),
				p -> engine.isSourceActive(p.getUUID(), "fury_unleashed_buff")),
			StatContribution.dynamic(Attributes.SCALE, id("fury_unleashed_scale"),
				AttributeModifier.Operation.ADD_VALUE,
				p -> engine.getBuffAmount(p.getUUID(), "fury_unleashed_buff"),
				p -> engine.isSourceActive(p.getUUID(), "fury_unleashed_buff")));

		// Flash Step — +30% movement speed for 3 seconds
		engine.register("flash_step_buff",
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("flash_step_speed"),
				0.30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> engine.isSourceActive(p.getUUID(), "flash_step_buff")));

		// Focus — +20% damage for 30 seconds
		engine.register("focus_buff",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("focus_damage"),
				0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> engine.isSourceActive(p.getUUID(), "focus_buff")));
	}

	// ── Private helpers ────────────────────────────────────────────────────────

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("wanderers_haven", path);
	}

	private static boolean owns(ServerPlayer player, String skillId) {
		return ClassSystemBootstrap.skillEngine()
			.ownedSkillIds(player.getUUID(), "warrior")
			.contains(skillId);
	}

	/** Berserker Rage: 0% at full health, 50% at 1 HP, clamped. */
	private static double berserkerRageAmount(ServerPlayer player) {
		float maxHp = player.getMaxHealth();
		float hp    = player.getHealth();
		float span  = maxHp - 1.0f;
		return (span > 0) ? Math.min(0.5, 0.5 * (maxHp - hp) / span) : 0.0;
	}

	private static boolean isDuelistWeapon(ServerPlayer player) {
		return "duelist".equals(WeaponCategoryResolver.resolveFromItem(player.getMainHandItem()));
	}

	private static boolean isBlademasterWeapon(ServerPlayer player) {
		return "blademaster".equals(WeaponCategoryResolver.resolveFromItem(player.getMainHandItem()));
	}
}
