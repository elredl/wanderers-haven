package com.wanderershaven.skill;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Shared tracker for timed attribute debuffs on non-player entities. */
public final class EntityAttributeDebuffService {

	private record DebuffEntry(Holder<Attribute> attribute, Identifier modifierId, long expiresAt) {}

	private final Map<UUID, Map<Identifier, DebuffEntry>> tracked = new ConcurrentHashMap<>();

	public void apply(
		LivingEntity target,
		Holder<Attribute> attribute,
		Identifier modifierId,
		double amount,
		AttributeModifier.Operation operation,
		long expiresAt
	) {
		AttributeInstance instance = target.getAttribute(attribute);
		if (instance == null) return;
		instance.removeModifier(modifierId);
		instance.addTransientModifier(new AttributeModifier(modifierId, amount, operation));

		tracked
			.computeIfAbsent(target.getUUID(), ignored -> new ConcurrentHashMap<>())
			.put(modifierId, new DebuffEntry(attribute, modifierId, expiresAt));
	}

	public void tick(MinecraftServer server) {
		tracked.entrySet().removeIf(entry -> {
			UUID entityId = entry.getKey();
			for (ServerLevel level : server.getAllLevels()) {
				net.minecraft.world.entity.Entity entity = level.getEntity(entityId);
				if (!(entity instanceof LivingEntity living)) continue;

				Map<Identifier, DebuffEntry> byModifier = entry.getValue();
				byModifier.entrySet().removeIf(modEntry -> {
					DebuffEntry debuff = modEntry.getValue();
					if (level.getGameTime() < debuff.expiresAt()) return false;
					AttributeInstance instance = living.getAttribute(debuff.attribute());
					if (instance != null) {
						instance.removeModifier(debuff.modifierId());
					}
					return true;
				});
				return byModifier.isEmpty();
			}
			return true;
		});
	}
}
