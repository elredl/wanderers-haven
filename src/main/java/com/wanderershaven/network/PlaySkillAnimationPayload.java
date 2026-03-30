package com.wanderershaven.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C payload telling the client to play the animation for an active skill.
 *
 * Currently the client handler triggers a vanilla arm swing.
 * Replace each case in {@code SkillAnimationHandler} with a GeckoLib
 * animation call once animations are fully implemented.
 */
public record PlaySkillAnimationPayload(String skillId) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PlaySkillAnimationPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("wanderers_haven", "play_skill_animation"));

	public static final StreamCodec<FriendlyByteBuf, PlaySkillAnimationPayload> CODEC = StreamCodec.of(
		(buf, payload) -> buf.writeUtf(payload.skillId()),
		buf -> new PlaySkillAnimationPayload(buf.readUtf())
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
