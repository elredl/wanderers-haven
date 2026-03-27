package com.wanderershaven.classsystem;

import java.util.Set;

public final class IntentMultipliers {
	private IntentMultipliers() {
	}

	public static IntentMultiplier always(double value) {
		return context -> value;
	}

	public static IntentMultiplier whenContext(String key, String value, double multiplier) {
		return context -> value.equals(context.signal().context().get(key)) ? multiplier : 1.0;
	}

	/**
	 * Applies {@code multiplier} when the integer stored at {@code key} in the signal context
	 * is greater than or equal to {@code minValue}. Returns 1.0 if the key is absent or not
	 * parseable as an integer. Useful for combo-hit depth checks.
	 */
	public static IntentMultiplier whenContextMinInt(String key, int minValue, double multiplier) {
		return context -> {
			String val = context.signal().context().get(key);
			if (val == null) {
				return 1.0;
			}
			try {
				return Integer.parseInt(val) >= minValue ? multiplier : 1.0;
			} catch (NumberFormatException ignored) {
				return 1.0;
			}
		};
	}

	public static IntentMultiplier coherentHabitationSpace(
		Set<String> furnishingTypes,
		int lookbackEvents,
		double multiplier
	) {
		return context -> {
			String zone = context.signal().context().get("zone");
			String furnishing = context.signal().context().get("furnishing");

			if (zone == null || furnishing == null || !furnishingTypes.contains(furnishing)) {
				return 1.0;
			}

			int startIndex = Math.max(0, context.recentSignals().size() - lookbackEvents);
			Set<String> seenFurnishings = new java.util.HashSet<>();

			for (int i = startIndex; i < context.recentSignals().size(); i++) {
				ClassSignal recent = context.recentSignals().get(i);
				if (!zone.equals(recent.context().get("zone"))) {
					continue;
				}

				String recentFurnishing = recent.context().get("furnishing");
				if (recentFurnishing != null && furnishingTypes.contains(recentFurnishing)) {
					seenFurnishings.add(recentFurnishing);
				}
			}

			return seenFurnishings.containsAll(furnishingTypes) ? multiplier : 1.0;
		};
	}
}
