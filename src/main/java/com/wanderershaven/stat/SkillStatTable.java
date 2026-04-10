package com.wanderershaven.stat;

import com.wanderershaven.classsystem.ClassSystemBootstrap;
import com.wanderershaven.compat.WeaponCategoryResolver;
import com.wanderershaven.skill.SkillEffectService;
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
	 *   - Permanent skill:  the skill's own ID  (e.g. "lesser_strength")
 *   - Timed buff:       "<source>_buff"      (e.g. "heavy_strikes_buff")
 */
public final class SkillStatTable {
	private SkillStatTable() {}

	public static void register(PlayerStatEngine engine) {

		// ── Permanent skill bonuses ────────────────────────────────────────────

		// Lesser Strength / Enhanced Strength — Enhanced supersedes Lesser (distinct IDs, exclusive conditions)
		engine.register("lesser_strength",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("lesser_strength"),
				0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "lesser_strength") && !owns(p, "enhanced_strength")));

		engine.register("enhanced_strength",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("enhanced_strength"),
				0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "enhanced_strength") && !owns(p, "greater_strength")));

		engine.register("greater_strength",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("greater_strength"),
				0.35, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "greater_strength") && !owns(p, "immense_strength")));

		engine.register("immense_strength",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("immense_strength"),
				0.50, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "immense_strength") && !owns(p, "legendary_strength")));

		engine.register("legendary_strength",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("legendary_strength"),
				0.62, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "legendary_strength") && !owns(p, "mythic_strength")));

		engine.register("mythic_strength",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("mythic_strength"),
				0.77, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "mythic_strength")));

		// Lesser Dexterity / Enhanced Dexterity
		engine.register("lesser_dexterity",
			StatContribution.always(Attributes.ATTACK_SPEED, id("lesser_dexterity"),
				0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "lesser_dexterity") && !owns(p, "enhanced_dexterity")));

		engine.register("enhanced_dexterity",
			StatContribution.always(Attributes.ATTACK_SPEED, id("enhanced_dexterity"),
				0.18, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "enhanced_dexterity") && !owns(p, "greater_dexterity")));

		engine.register("greater_dexterity",
			StatContribution.always(Attributes.ATTACK_SPEED, id("greater_dexterity"),
				0.28, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "greater_dexterity") && !owns(p, "immense_dexterity")));

		engine.register("immense_dexterity",
			StatContribution.always(Attributes.ATTACK_SPEED, id("immense_dexterity"),
				0.40, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "immense_dexterity") && !owns(p, "legendary_dexterity")));

		engine.register("legendary_dexterity",
			StatContribution.always(Attributes.ATTACK_SPEED, id("legendary_dexterity"),
				0.50, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "legendary_dexterity") && !owns(p, "mythic_dexterity")));

		engine.register("mythic_dexterity",
			StatContribution.always(Attributes.ATTACK_SPEED, id("mythic_dexterity"),
				0.65, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "mythic_dexterity")));

		// Lesser Speed / Enhanced Speed
		engine.register("lesser_speed",
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("lesser_speed"),
				0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "lesser_speed") && !owns(p, "enhanced_speed")));

		engine.register("enhanced_speed",
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("enhanced_speed"),
				0.18, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "enhanced_speed") && !owns(p, "greater_speed")));

		engine.register("greater_speed",
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("greater_speed"),
				0.28, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "greater_speed") && !owns(p, "immense_speed")));

		engine.register("immense_speed",
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("immense_speed"),
				0.40, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "immense_speed") && !owns(p, "legendary_speed")));

		engine.register("legendary_speed",
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("legendary_speed"),
				0.50, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "legendary_speed") && !owns(p, "mythic_speed")));

		engine.register("mythic_speed",
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("mythic_speed"),
				0.65, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "mythic_speed")));

		// Lightfooted — +15% movement speed while in combat
		engine.register("lightfooted",
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("lightfooted"),
				0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "lightfooted")
					&& !owns(p, "fleetfooted")
					&& !owns(p, "battlefield_unmatched_mobility")
					&& SkillEffectService.isInCombat(p)));

		engine.register("fleetfooted",
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("fleetfooted"),
				0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "fleetfooted")
					&& !owns(p, "battlefield_unmatched_mobility")
					&& SkillEffectService.isInCombat(p)));

		engine.register("battlefield_unmatched_mobility",
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("battlefield_unmatched_mobility"),
				0.30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "battlefield_unmatched_mobility") && SkillEffectService.isInCombat(p)));

		// Fortitude / Greater Fortitude — permanent max-health increase
		engine.register("fortitude",
			StatContribution.always(Attributes.MAX_HEALTH, id("fortitude_health"),
				6.0, AttributeModifier.Operation.ADD_VALUE,
				p -> owns(p, "fortitude") && !owns(p, "greater_fortitude")));

		engine.register("greater_fortitude",
			StatContribution.always(Attributes.MAX_HEALTH, id("greater_fortitude_health"),
				12.0, AttributeModifier.Operation.ADD_VALUE,
				p -> owns(p, "greater_fortitude") && !owns(p, "perfect_constitution")));

		engine.register("perfect_constitution",
			StatContribution.always(Attributes.MAX_HEALTH, id("perfect_constitution_health"),
				20.0, AttributeModifier.Operation.ADD_VALUE,
				p -> owns(p, "perfect_constitution") && !owns(p, "giants_constitution")));

		engine.register("giants_constitution",
			StatContribution.always(Attributes.MAX_HEALTH, id("giants_constitution_health"),
				40.0, AttributeModifier.Operation.ADD_VALUE,
				p -> owns(p, "giants_constitution") && !owns(p, "divinity_incarnate")));

		engine.register("divinity_incarnate",
			StatContribution.always(Attributes.MAX_HEALTH, id("divinity_incarnate_health"),
				80.0, AttributeModifier.Operation.ADD_VALUE,
				p -> owns(p, "divinity_incarnate")));

		// Battle-Hardened — +5% multiplicative boost to core combat stats
		engine.register("battle_hardened",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("battle_hardened_damage"),
				0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "battle_hardened")),
			StatContribution.always(Attributes.ATTACK_SPEED, id("battle_hardened_speed"),
				0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "battle_hardened")),
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("battle_hardened_move"),
				0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "battle_hardened")),
			StatContribution.always(Attributes.MAX_HEALTH, id("battle_hardened_health"),
				0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "battle_hardened")),
			StatContribution.always(Attributes.ARMOR, id("battle_hardened_armor"),
				0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "battle_hardened") && !owns(p, "body_of_galas")));

		engine.register("body_of_galas",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("body_of_galas_damage"),
				0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "body_of_galas")),
			StatContribution.always(Attributes.ATTACK_SPEED, id("body_of_galas_speed"),
				0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "body_of_galas")),
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("body_of_galas_move"),
				0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "body_of_galas")),
			StatContribution.always(Attributes.MAX_HEALTH, id("body_of_galas_health"),
				0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "body_of_galas")),
			StatContribution.always(Attributes.ARMOR, id("body_of_galas_armor"),
				0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "body_of_galas") && !owns(p, "transcendent_form")));

		engine.register("transcendent_form",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("transcendent_form_damage"),
				0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "transcendent_form")),
			StatContribution.always(Attributes.ATTACK_SPEED, id("transcendent_form_speed"),
				0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "transcendent_form")),
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("transcendent_form_move"),
				0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "transcendent_form")),
			StatContribution.always(Attributes.MAX_HEALTH, id("transcendent_form_health"),
				0.35, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "transcendent_form")),
			StatContribution.always(Attributes.ARMOR, id("transcendent_form_armor"),
				0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "transcendent_form")));

		// Stand Your Ground — permanent knockback immunity
		engine.register("stand_your_ground",
			StatContribution.always(Attributes.KNOCKBACK_RESISTANCE, id("knockback_immunity"),
				1.0, AttributeModifier.Operation.ADD_VALUE,
				p -> owns(p, "stand_your_ground")));

		// ── Dynamic (amount recalculated each tick) ────────────────────────────

		// Berserker Rage / Enhanced Berserker Rage — 0% bonus at full health -> 50% at 1 HP
		engine.register("berserker_rage",
			StatContribution.dynamic(Attributes.ATTACK_DAMAGE, id("berserker_rage"),
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				SkillStatTable::berserkerRageAmount,
				p -> owns(p, "berserker_rage") || owns(p, "enhanced_berserker_rage")));

		engine.register("enhanced_berserker_rage",
			StatContribution.dynamic(Attributes.ATTACK_SPEED, id("enhanced_berserker_rage_speed"),
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				SkillStatTable::enhancedBerserkerRageSpeedAmount,
				p -> owns(p, "enhanced_berserker_rage")));

		// ── Conditional (weapon-gated, rechecked each tick) ───────────────────

		// Swift Blade (Duelist) — +20% attack speed while holding a light blade
		engine.register("swift_blade",
			StatContribution.always(Attributes.ATTACK_SPEED, id("swift_blade"),
				0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "swift_blade") && isDuelistWeapon(p)));

		// Blade Mastery (Blademaster) — +25% damage and +18% attack speed while holding a heavy blade
		engine.register("blade_mastery",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("blade_mastery_damage"),
				0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "blade_mastery") && isBlademasterWeapon(p)),
			StatContribution.always(Attributes.ATTACK_SPEED, id("blade_mastery_speed"),
				0.18, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "blade_mastery") && isBlademasterWeapon(p)));

		// Spear Mastery (Spearmaster) — +20% damage and +20% range while holding a spear-class weapon
		engine.register("spear_mastery",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("spear_mastery_damage"),
				0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "spear_mastery") && isSpearmasterWeapon(p)),
			StatContribution.always(Attributes.ENTITY_INTERACTION_RANGE, id("spear_mastery_range"),
				0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "spear_mastery") && isSpearmasterWeapon(p)));

		// Crushing Blows (Mauler) — +40% damage while holding mauler weapons
		engine.register("crushing_blows",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("crushing_blows_damage"),
				0.40, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> owns(p, "crushing_blows") && isMaulerWeapon(p)));

		// ── Timed buffs (activated / deactivated by SkillEffectService) ────────

		// Heavy Strikes — +12% damage for 10 seconds
		engine.register("heavy_strikes_buff",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("heavy_strikes"),
				0.12, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> engine.isSourceActive(p.getUUID(), "heavy_strikes_buff")));

		// Maddening Strikes — +12% damage and +30% attack speed for 10 seconds
		engine.register("maddening_strikes_buff",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("maddening_strikes_damage"),
				0.12, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> engine.isSourceActive(p.getUUID(), "maddening_strikes_buff")),
			StatContribution.always(Attributes.ATTACK_SPEED, id("maddening_strikes_speed"),
				0.30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> engine.isSourceActive(p.getUUID(), "maddening_strikes_buff")));

		// Savage Onslaught — +30% damage and +20% attack speed for 10 seconds
		engine.register("savage_onslaught_buff",
			StatContribution.always(Attributes.ATTACK_DAMAGE, id("savage_onslaught_damage"),
				0.30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> engine.isSourceActive(p.getUUID(), "savage_onslaught_buff")),
			StatContribution.always(Attributes.ATTACK_SPEED, id("savage_onslaught_speed"),
				0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> engine.isSourceActive(p.getUUID(), "savage_onslaught_buff")));

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

		// Lengthened Strikes — +20% attack range for 20 seconds
		engine.register("lengthened_strikes_buff",
			StatContribution.always(Attributes.ENTITY_INTERACTION_RANGE, id("lengthened_strikes_range"),
				0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> engine.isSourceActive(p.getUUID(), "lengthened_strikes_buff")));

		// Dance of the Butterfly — +50% movement speed for 5 seconds
		engine.register("dance_of_butterfly_buff",
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("dance_of_butterfly_speed"),
				0.50, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> engine.isSourceActive(p.getUUID(), "dance_of_butterfly_buff")));

		// Shadow Step — +50% movement speed for 5 seconds
		engine.register("shadow_step_buff",
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("shadow_step_speed"),
				0.50, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> engine.isSourceActive(p.getUUID(), "shadow_step_buff")));

		// Battlefield: Unmatched Mobility — +15% speed for 4s after each hit
		engine.register("battlefield_mobility_attack_buff",
			StatContribution.always(Attributes.MOVEMENT_SPEED, id("battlefield_mobility_attack_speed"),
				0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				p -> engine.isSourceActive(p.getUUID(), "battlefield_mobility_attack_buff")));
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

	/** Enhanced Berserker Rage: 0% at full health, 30% at 1 HP, clamped. */
	private static double enhancedBerserkerRageSpeedAmount(ServerPlayer player) {
		float maxHp = player.getMaxHealth();
		float hp    = player.getHealth();
		float span  = maxHp - 1.0f;
		return (span > 0) ? Math.min(0.3, 0.3 * (maxHp - hp) / span) : 0.0;
	}

	private static boolean isDuelistWeapon(ServerPlayer player) {
		return "duelist".equals(WeaponCategoryResolver.resolveFromItem(player.getMainHandItem()));
	}

	private static boolean isBlademasterWeapon(ServerPlayer player) {
		return "blademaster".equals(WeaponCategoryResolver.resolveFromItem(player.getMainHandItem()));
	}

	private static boolean isSpearmasterWeapon(ServerPlayer player) {
		return "spear".equals(WeaponCategoryResolver.resolveFromItem(player.getMainHandItem()));
	}

	private static boolean isMaulerWeapon(ServerPlayer player) {
		return "mauler".equals(WeaponCategoryResolver.resolveFromItem(player.getMainHandItem()));
	}
}
