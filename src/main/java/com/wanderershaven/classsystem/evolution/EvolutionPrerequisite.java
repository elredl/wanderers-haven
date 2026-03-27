package com.wanderershaven.classsystem.evolution;

/**
 * A single condition that must be satisfied for an evolution path to be offered.
 *
 * Prerequisites are composed in {@link ClassEvolutionDef} — all must be met for
 * the path to become eligible. Multiple prerequisites act as AND logic; OR logic
 * can be achieved by registering multiple evolution definitions that share an ID
 * or by writing a composite lambda.
 */
@FunctionalInterface
public interface EvolutionPrerequisite {

	boolean isMet(EvolutionContext context);

	// ── Convenience factories ─────────────────────────────────────────────────

	/** Player has killed at least {@code minimum} entities in the given category. */
	static EvolutionPrerequisite killsOfCategory(String category, int minimum) {
		return ctx -> ctx.killsOfCategory(category) >= minimum;
	}

	/** Player has survived {@code minimum} hits while at or below 10% max health. */
	static EvolutionPrerequisite nearDeathSurvivals(int minimum) {
		return ctx -> ctx.nearDeathSurvivals() >= minimum;
	}

	/** Player has made {@code minimum} total kills across all categories. */
	static EvolutionPrerequisite totalKills(int minimum) {
		return ctx -> ctx.totalKills() >= minimum;
	}

	/** Player has blocked {@code minimum} hits with a shield. */
	static EvolutionPrerequisite blockedHits(int minimum) {
		return ctx -> ctx.blockedHits() >= minimum;
	}

	/** Player is at or above the given class level. */
	static EvolutionPrerequisite classLevel(int minimum) {
		return ctx -> ctx.classLevel() >= minimum;
	}

	/** Player has already obtained the given class. */
	static EvolutionPrerequisite hasClass(String classId) {
		return ctx -> ctx.ownedClassIds().contains(classId);
	}

	/** Player has made {@code minimum} kills using the given weapon category. */
	static EvolutionPrerequisite killsWithWeapon(String weaponCategory, int minimum) {
		return ctx -> ctx.killsWithWeapon(weaponCategory) >= minimum;
	}
}
