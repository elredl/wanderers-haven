package com.wanderershaven.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S payload carrying the player's evolution path selection.
 */
public record EvolutionChoicePayload(String evolutionId) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<EvolutionChoicePayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("wanderers_haven", "evolution_choice"));

	public static final StreamCodec<FriendlyByteBuf, EvolutionChoicePayload> CODEC = StreamCodec.of(
		(buf, payload) -> buf.writeUtf(payload.evolutionId()),
		buf -> new EvolutionChoicePayload(buf.readUtf())
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
