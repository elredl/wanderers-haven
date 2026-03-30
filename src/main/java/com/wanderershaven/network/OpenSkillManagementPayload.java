package com.wanderershaven.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C payload that opens the Skill Management screen on the client.
 * Carries owned skill details, active slot bindings, any pending evolution offer,
 * and queued level-up / skill notifications to display in the "What's New" panel.
 */
public record OpenSkillManagementPayload(
	List<SkillEntry> ownedSkills,
	List<String> slots,                    // size 5; null / "" = empty
	String evolutionBaseClassId,           // "" if no pending evolution
	List<EvolutionEntry> evolutionOffers,  // empty if no pending evolution
	List<String> notifications             // formatted notification strings; empty if nothing new
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
				buf.writeBoolean(e.active());
			}
			for (String slot : payload.slots()) {
				buf.writeUtf(slot == null ? "" : slot);
			}
			buf.writeUtf(payload.evolutionBaseClassId());
			buf.writeVarInt(payload.evolutionOffers().size());
			for (EvolutionEntry e : payload.evolutionOffers()) {
				buf.writeUtf(e.id());
				buf.writeUtf(e.displayName());
				buf.writeUtf(e.description());
			}
			buf.writeVarInt(payload.notifications().size());
			for (String n : payload.notifications()) {
				buf.writeUtf(n);
			}
		},
		buf -> {
			int skillCount = buf.readVarInt();
			List<SkillEntry> skills = new ArrayList<>(skillCount);
			for (int i = 0; i < skillCount; i++) {
				skills.add(new SkillEntry(
					buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readUtf(), buf.readBoolean()));
			}
			List<String> slots = new ArrayList<>(5);
			for (int i = 0; i < 5; i++) {
				String s = buf.readUtf();
				slots.add(s.isEmpty() ? null : s);
			}
			String evoBase = buf.readUtf();
			int evoCount = buf.readVarInt();
			List<EvolutionEntry> evoOffers = new ArrayList<>(evoCount);
			for (int i = 0; i < evoCount; i++) {
				evoOffers.add(new EvolutionEntry(buf.readUtf(), buf.readUtf(), buf.readUtf()));
			}
			int notifCount = buf.readVarInt();
			List<String> notifications = new ArrayList<>(notifCount);
			for (int i = 0; i < notifCount; i++) {
				notifications.add(buf.readUtf());
			}
			return new OpenSkillManagementPayload(skills, slots, evoBase, evoOffers, notifications);
		}
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/** A skill entry for the Skill Management screen. */
	public record SkillEntry(String id, String displayName, int powerLevel, String description, boolean active) {}

	/** An evolution path entry bundled with the skill management payload. */
	public record EvolutionEntry(String id, String displayName, String description) {}
}
