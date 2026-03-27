package com.wanderershaven.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent from the server to the client when a player enters a bed and has pending class decisions.
 */
public record OpenClassSelectionPayload(List<PendingClassEntry> pendingClasses) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenClassSelectionPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("wanderers_haven", "open_class_selection"));

	public static final StreamCodec<FriendlyByteBuf, OpenClassSelectionPayload> CODEC = StreamCodec.of(
		(buf, value) -> {
			buf.writeVarInt(value.pendingClasses().size());
			for (PendingClassEntry entry : value.pendingClasses()) {
				buf.writeUtf(entry.classId());
				buf.writeUtf(entry.displayName());
				buf.writeVarInt(entry.denyCount());
			}
		},
		buf -> {
			int size = buf.readVarInt();
			List<PendingClassEntry> entries = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				entries.add(new PendingClassEntry(buf.readUtf(), buf.readUtf(), buf.readVarInt()));
			}
			return new OpenClassSelectionPayload(entries);
		}
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/** The data the client needs to render and act on each pending class. */
	public record PendingClassEntry(String classId, String displayName, int denyCount) {}
}
