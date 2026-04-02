package com.wanderershaven.stat;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Describes one attribute modifier contribution from a skill or buff source.
 *
 * {@link #condition} determines whether the contribution is currently active.
 * {@link #amount} is re-evaluated every tick — static contributions just return
 * a constant, while dynamic ones (e.g. Berserker Rage) recalculate from player state.
 *
 * The {@link PlayerStatEngine} manages apply/remove automatically.
 */
public record StatContribution(
	Holder<Attribute> attribute,
	Identifier modifierId,
	AttributeModifier.Operation operation,
	ToDoubleFunction<ServerPlayer> amount,
	Predicate<ServerPlayer> condition
) {
	/** Fixed-amount contribution — applied whenever {@code condition} is true. */
	public static StatContribution always(
		Holder<Attribute> attr,
		Identifier id,
		double fixedAmount,
		AttributeModifier.Operation op,
		Predicate<ServerPlayer> cond
	) {
		return new StatContribution(attr, id, op, __ -> fixedAmount, cond);
	}

	/** Variable-amount contribution — amount is re-evaluated every tick. */
	public static StatContribution dynamic(
		Holder<Attribute> attr,
		Identifier id,
		AttributeModifier.Operation op,
		ToDoubleFunction<ServerPlayer> amountFn,
		Predicate<ServerPlayer> cond
	) {
		return new StatContribution(attr, id, op, amountFn, cond);
	}
}
