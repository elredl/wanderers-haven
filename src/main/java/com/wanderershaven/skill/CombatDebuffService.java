package com.wanderershaven.skill;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified damage dealt/taken debuffs for entities.
 *
 * Values are multiplicative factors where 1.0 means no modification.
 */
public final class CombatDebuffService {

	private record Debuff(long expiresAt, float dealtMult, float takenMult) {}

	private final Map<UUID, Debuff> debuffs = new ConcurrentHashMap<>();

	public void apply(UUID targetId, long expiresAt, float dealtMult, float takenMult) {
		Debuff incoming = new Debuff(expiresAt, dealtMult, takenMult);
		debuffs.merge(targetId, incoming, (oldValue, newValue) -> {
			if (newValue.expiresAt > oldValue.expiresAt) return newValue;
			float dealt = Math.min(oldValue.dealtMult, newValue.dealtMult);
			float taken = Math.max(oldValue.takenMult, newValue.takenMult);
			return new Debuff(oldValue.expiresAt, dealt, taken);
		});
	}

	public float dealtMult(UUID entityId, long now) {
		Debuff debuff = debuffs.get(entityId);
		if (debuff == null) return 1.0f;
		if (now >= debuff.expiresAt) {
			debuffs.remove(entityId);
			return 1.0f;
		}
		return debuff.dealtMult;
	}

	public float takenMult(UUID entityId, long now) {
		Debuff debuff = debuffs.get(entityId);
		if (debuff == null) return 1.0f;
		if (now >= debuff.expiresAt) {
			debuffs.remove(entityId);
			return 1.0f;
		}
		return debuff.takenMult;
	}
}
