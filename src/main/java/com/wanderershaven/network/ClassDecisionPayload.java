package com.wanderershaven.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent from the client to the server carrying the player's decision about a pending class.
 */
public record ClassDecisionPayload(String classId, DecisionType decision) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ClassDecisionPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("wanderers_haven", "class_decision"));

	public static final StreamCodec<FriendlyByteBuf, ClassDecisionPayload> CODEC = StreamCodec.of(
		(buf, value) -> {
			buf.writeUtf(value.classId());
			buf.writeVarInt(value.decision().ordinal());
		},
		buf -> new ClassDecisionPayload(buf.readUtf(), DecisionType.values()[buf.readVarInt()])
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public enum DecisionType {
		ACCEPT,
		DENY,
		PERMANENT_DENY
	}
}
