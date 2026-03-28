package com.wanderershaven.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C payload that opens the Skill Management screen on the client.
 * Carries the player's owned skill details and current active slot bindings.
 */
public record OpenSkillManagementPayload(
	List<SkillEntry> ownedSkills,
	List<String> slots        // size 5; empty string "" = empty slot
) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<OpenSkillManagementPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("wanderers_haven", "open_skill_management"));

	public static final StreamCodec<FriendlyByteBuf, OpenSkillManagementPayload> CODEC = StreamCodec.of(
		(buf, payload) -> {
			buf.writeVarInt(payload.ownedSkills().size());
			for (SkillEntry e : payload.ownedSkills()) {
				buf.writeUtf(e.id());
				buf.writeUtf(e.displayName());
				buf.writeVarInt(e.powerLevel());
				buf.writeUtf(e.description());
			}
			for (String slot : payload.slots()) {
				buf.writeUtf(slot == null ? "" : slot);
			}
		},
		buf -> {
			int count = buf.readVarInt();
			List<SkillEntry> skills = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				skills.add(new SkillEntry(buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readUtf()));
			}
			List<String> slots = new ArrayList<>(5);
			for (int i = 0; i < 5; i++) {
				String s = buf.readUtf();
				slots.add(s.isEmpty() ? null : s);
			}
			return new OpenSkillManagementPayload(skills, slots);
		}
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/** A skill entry suitable for rendering in the Skill Management screen. */
	public record SkillEntry(String id, String displayName, int powerLevel, String description) {}
}
