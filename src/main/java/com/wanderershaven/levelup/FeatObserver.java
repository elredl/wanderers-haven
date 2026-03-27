package com.wanderershaven.levelup;

@FunctionalInterface
public interface FeatObserver {
	/**
	 * Evaluate this feat against the given context and return the XP earned.
	 * Return 0.0 if this feat does not apply to the signal.
	 */
	double observe(FeatContext context);
}
