package com.wanderershaven.levelup;

import com.wanderershaven.classsystem.ClassSignal;
import java.util.UUID;
import java.util.function.Function;

/**
 * All information a FeatObserver needs to calculate XP for a single signal.
 *
 * @param signal             the raw class signal being evaluated
 * @param attackerLevel      the attacker's current level in the class being evaluated
 * @param killStreak         consecutive kills within the active streak window
 * @param playerLevelLookup  looks up another player's level in this same class by UUID;
 *                           used to scale XP for player kills by target level
 */
public record FeatContext(
	ClassSignal signal,
	int attackerLevel,
	int killStreak,
	Function<UUID, Integer> playerLevelLookup
) {}
