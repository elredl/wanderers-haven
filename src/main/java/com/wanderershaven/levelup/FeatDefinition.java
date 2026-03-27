package com.wanderershaven.levelup;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds a set of {@link FeatObserver}s and a {@link LevelCurve} to a specific class ID.
 * All observers are evaluated for every incoming signal; each returns 0 if not applicable.
 */
public final class FeatDefinition {
	private final String classId;
	private final LevelCurve levelCurve;
	private final List<FeatObserver> observers;

	private FeatDefinition(String classId, LevelCurve levelCurve, List<FeatObserver> observers) {
		this.classId = classId;
		this.levelCurve = levelCurve;
		this.observers = List.copyOf(observers);
	}

	public String classId() {
		return classId;
	}

	public LevelCurve levelCurve() {
		return levelCurve;
	}

	public List<FeatObserver> observers() {
		return observers;
	}

	public static Builder builder(String classId) {
		return new Builder(classId);
	}

	public static final class Builder {
		private final String classId;
		private LevelCurve levelCurve = LevelCurve.defaults();
		private final List<FeatObserver> observers = new ArrayList<>();

		private Builder(String classId) {
			this.classId = classId;
		}

		public Builder levelCurve(LevelCurve curve) {
			this.levelCurve = curve;
			return this;
		}

		public Builder observer(FeatObserver observer) {
			observers.add(observer);
			return this;
		}

		public FeatDefinition build() {
			return new FeatDefinition(classId, levelCurve, observers);
		}
	}
}
