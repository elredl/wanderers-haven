package com.wanderershaven.levelup;

/**
 * A read-only snapshot of a player's level progress in a single class.
 *
 * @param classId        the class this profile belongs to
 * @param level          current level (0 = not yet reached level 1)
 * @param progressXp     XP earned within the current level
 * @param xpToNextLevel  XP still needed to reach the next level (0 at max level)
 */
public record ClassLevelProfile(String classId, int level, double progressXp, double xpToNextLevel) {}
