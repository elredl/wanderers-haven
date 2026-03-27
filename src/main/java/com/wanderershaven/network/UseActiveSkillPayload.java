package com.wanderershaven.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S payload sent when the player activates an active skill via the radial menu.
 *
 * {@code slotIndex} is 0-based (0–4).
 */
public record UseActiveSkillPayload(int slotIndex) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<UseActiveSkillPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("wanderers_haven", "use_active_skill"));

	public static final StreamCodec<FriendlyByteBuf, UseActiveSkillPayload> CODEC = StreamCodec.of(
		(buf, payload) -> buf.writeVarInt(payload.slotIndex()),
		buf -> new UseActiveSkillPayload(buf.readVarInt())
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
