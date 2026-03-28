package com.wanderershaven.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C payload that opens the Evolution Selection screen on the client.
 * Carries the list of eligible evolution paths and the base class that triggered them.
 */
public record OpenEvolutionSelectionPayload(
	String baseClassId,
	List<EvolutionEntry> offers
) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<OpenEvolutionSelectionPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("wanderers_haven", "open_evolution_selection"));

	public static final StreamCodec<FriendlyByteBuf, OpenEvolutionSelectionPayload> CODEC = StreamCodec.of(
		(buf, payload) -> {
			buf.writeUtf(payload.baseClassId());
			buf.writeVarInt(payload.offers().size());
			for (EvolutionEntry e : payload.offers()) {
				buf.writeUtf(e.id());
				buf.writeUtf(e.displayName());
				buf.writeUtf(e.description());
			}
		},
		buf -> {
			String baseClassId = buf.readUtf();
			int count = buf.readVarInt();
			List<EvolutionEntry> offers = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				offers.add(new EvolutionEntry(buf.readUtf(), buf.readUtf(), buf.readUtf()));
			}
			return new OpenEvolutionSelectionPayload(baseClassId, offers);
		}
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public record EvolutionEntry(String id, String displayName, String description) {}
}
