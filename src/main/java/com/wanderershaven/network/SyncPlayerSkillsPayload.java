package com.wanderershaven.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S2C payload that tells the client which skill IDs the player currently owns. */
public record SyncPlayerSkillsPayload(List<String> skillIds) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SyncPlayerSkillsPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("wanderers_haven", "sync_skills"));

	public static final StreamCodec<FriendlyByteBuf, SyncPlayerSkillsPayload> CODEC = StreamCodec.of(
		(buf, payload) -> {
			buf.writeVarInt(payload.skillIds().size());
			for (String id : payload.skillIds()) {
				buf.writeUtf(id);
			}
		},
		buf -> {
			int count = buf.readVarInt();
			List<String> ids = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				ids.add(buf.readUtf());
			}
			return new SyncPlayerSkillsPayload(ids);
		}
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
