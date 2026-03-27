package com.wanderershaven.classsystem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ClassInferenceEngine {
	void ingest(ClassSignal signal);

	void registerClass(ClassDefinition classDefinition);

	Map<String, ClassDefinition> registeredClasses();

	Optional<PlayerClassProfile> profile(UUID playerId);

	List<ClassConditionNotification> drainNotifications(UUID playerId);

	/** Classes that have met their threshold but are waiting for the player's decision at a bed. */
	Set<String> pendingClasses(UUID playerId);

	/** How many times the player has denied this class (0 = never denied). */
	int denyCount(UUID playerId, String classId);

	/** Move a pending class into the player's obtained set. */
	void acceptClass(UUID playerId, String classId);

	/** Reset affinity and remove from pending so the player can earn the class again. */
	void denyClass(UUID playerId, String classId);

	/** Permanently block a class from ever being offered to this player again. */
	void permanentlyDenyClass(UUID playerId, String classId);

	/** Bypass the affinity threshold and directly grant a class to a player. */
	void forceGrantClass(UUID playerId, String classId);

	/** Force a class into the player's pending set regardless of affinity or threshold. */
	void forcePending(UUID playerId, String classId);
}
