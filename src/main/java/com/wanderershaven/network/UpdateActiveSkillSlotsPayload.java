package com.wanderershaven.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S payload sent when the player saves their active skill slot configuration
 * from the Skill Management screen.
 *
 * Contains exactly 5 slot entries. An empty string means the slot is cleared.
 */
public record UpdateActiveSkillSlotsPayload(List<String> slots) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<UpdateActiveSkillSlotsPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("wanderers_haven", "update_active_slots"));

	public static final StreamCodec<FriendlyByteBuf, UpdateActiveSkillSlotsPayload> CODEC = StreamCodec.of(
		(buf, payload) -> {
			for (String slot : payload.slots()) {
				buf.writeUtf(slot == null ? "" : slot);
			}
		},
		buf -> {
			List<String> slots = new ArrayList<>(5);
			for (int i = 0; i < 5; i++) {
				String s = buf.readUtf();
				slots.add(s.isEmpty() ? null : s);
			}
			return new UpdateActiveSkillSlotsPayload(slots);
		}
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
