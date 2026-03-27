package com.wanderershaven.classsystem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class AffinityClassInferenceEngine implements ClassInferenceEngine {
	private final ClassProgressionRules progressionRules;
	private final Map<String, ClassDefinition> classDefinitions = new ConcurrentHashMap<>();
	private final Map<UUID, PlayerRuntimeState> stateByPlayer = new ConcurrentHashMap<>();
	private final Map<UUID, ConcurrentLinkedQueue<ClassConditionNotification>> notificationsByPlayer = new ConcurrentHashMap<>();

	public AffinityClassInferenceEngine(ClassProgressionRules progressionRules, Collection<ClassDefinition> definitions) {
		this.progressionRules = progressionRules;
		for (ClassDefinition definition : definitions) {
			classDefinitions.put(definition.id(), definition);
		}
	}

	@Override
	public void ingest(ClassSignal signal) {
		PlayerRuntimeState state = stateByPlayer.computeIfAbsent(signal.playerId(), ignored -> new PlayerRuntimeState());

		synchronized (state) {
			state.lastUpdatedTime = signal.gameTime();
			pushRecentSignal(state, signal);

			for (ClassDefinition definition : classDefinitions.values()) {
				applySignalToDefinition(state, definition, signal);
				tryObtainClass(state, definition, signal.playerId(), signal.gameTime());
			}
		}
	}

	@Override
	public void registerClass(ClassDefinition classDefinition) {
		classDefinitions.put(classDefinition.id(), classDefinition);
	}

	@Override
	public Map<String, ClassDefinition> registeredClasses() {
		return Map.copyOf(classDefinitions);
	}

	@Override
	public Optional<PlayerClassProfile> profile(UUID playerId) {
		PlayerRuntimeState state = stateByPlayer.get(playerId);
		if (state == null) {
			return Optional.empty();
		}

		synchronized (state) {
			return Optional.of(new PlayerClassProfile(state.affinities, state.obtainedClasses, state.lastUpdatedTime));
		}
	}

	@Override
	public List<ClassConditionNotification> drainNotifications(UUID playerId) {
		ConcurrentLinkedQueue<ClassConditionNotification> queue = notificationsByPlayer.get(playerId);
		if (queue == null || queue.isEmpty()) {
			return List.of();
		}

		List<ClassConditionNotification> drained = new ArrayList<>();
		ClassConditionNotification notification;
		while ((notification = queue.poll()) != null) {
			drained.add(notification);
		}

		return drained;
	}

	private void applySignalToDefinition(PlayerRuntimeState state, ClassDefinition definition, ClassSignal signal) {
		double baseWeight = definition.signalWeight(signal.type());
		if (baseWeight <= 0.0) {
			return;
		}

		AffinityContext affinityContext = new AffinityContext(
			signal.playerId(),
			signal,
			Map.copyOf(state.affinities),
			Set.copyOf(state.obtainedClasses),
			List.copyOf(state.recentSignals)
		);

		double intentMultiplier = 1.0;
		for (IntentMultiplier multiplier : definition.intentMultipliers()) {
			intentMultiplier *= Math.max(0.0, multiplier.apply(affinityContext));
		}

		double scaling = progressionRules.classGainScaling(state.obtainedClasses.size(), state.obtainedClasses.contains(definition.id()));
		double gain = signal.weight() * baseWeight * intentMultiplier * scaling;

		if (gain <= 0.0) {
			return;
		}

		state.affinities.merge(definition.id(), gain, Double::sum);
	}

	private void tryObtainClass(PlayerRuntimeState state, ClassDefinition definition, UUID playerId, long gameTime) {
		if (state.obtainedClasses.contains(definition.id())) {
			return;
		}

		if (state.pendingClasses.contains(definition.id())) {
			return;
		}

		if (state.permanentlyDenied.contains(definition.id())) {
			return;
		}

		double affinity = state.affinities.getOrDefault(definition.id(), 0.0);
		if (affinity < definition.obtainThreshold()) {
			return;
		}

		PrerequisiteContext context = new PrerequisiteContext(
			Map.copyOf(state.affinities),
			Set.copyOf(state.obtainedClasses),
			List.copyOf(state.recentSignals)
		);

		for (ClassPrerequisite prerequisite : definition.prerequisites()) {
			if (!prerequisite.isMet(context)) {
				return;
			}
		}

		state.pendingClasses.add(definition.id());
	}

	private void pushRecentSignal(PlayerRuntimeState state, ClassSignal signal) {
		state.recentSignals.addLast(signal);
		while (state.recentSignals.size() > progressionRules.recentSignalHistoryLimit()) {
			state.recentSignals.removeFirst();
		}
	}

	@Override
	public Set<String> pendingClasses(UUID playerId) {
		PlayerRuntimeState state = stateByPlayer.get(playerId);
		if (state == null) {
			return Set.of();
		}

		synchronized (state) {
			return Set.copyOf(state.pendingClasses);
		}
	}

	@Override
	public int denyCount(UUID playerId, String classId) {
		PlayerRuntimeState state = stateByPlayer.get(playerId);
		if (state == null) {
			return 0;
		}

		synchronized (state) {
			return state.denyCounts.getOrDefault(classId, 0);
		}
	}

	@Override
	public void acceptClass(UUID playerId, String classId) {
		PlayerRuntimeState state = stateByPlayer.get(playerId);
		if (state == null) {
			return;
		}

		synchronized (state) {
			if (!state.pendingClasses.remove(classId)) {
				return;
			}

			state.obtainedClasses.add(classId);
		}
	}

	@Override
	public void denyClass(UUID playerId, String classId) {
		PlayerRuntimeState state = stateByPlayer.get(playerId);
		if (state == null) {
			return;
		}

		synchronized (state) {
			state.pendingClasses.remove(classId);
			state.affinities.remove(classId);
			state.denyCounts.merge(classId, 1, Integer::sum);
		}
	}

	@Override
	public void permanentlyDenyClass(UUID playerId, String classId) {
		PlayerRuntimeState state = stateByPlayer.get(playerId);
		if (state == null) {
			return;
		}

		synchronized (state) {
			state.pendingClasses.remove(classId);
			state.affinities.remove(classId);
			state.permanentlyDenied.add(classId);
		}
	}

	@Override
	public void forcePending(UUID playerId, String classId) {
		PlayerRuntimeState state = stateByPlayer.computeIfAbsent(playerId, ignored -> new PlayerRuntimeState());

		synchronized (state) {
			if (state.obtainedClasses.contains(classId) || state.permanentlyDenied.contains(classId)) {
				return;
			}
			state.pendingClasses.add(classId);
		}
	}

	@Override
	public void forceGrantClass(UUID playerId, String classId) {
		PlayerRuntimeState state = stateByPlayer.computeIfAbsent(playerId, ignored -> new PlayerRuntimeState());

		synchronized (state) {
			state.pendingClasses.remove(classId);
			state.permanentlyDenied.remove(classId);
			state.obtainedClasses.add(classId);
		}
	}

	private static final class PlayerRuntimeState {
		private final Map<String, Double> affinities = new HashMap<>();
		private final Set<String> obtainedClasses = new HashSet<>();
		private final Set<String> pendingClasses = new HashSet<>();
		private final Map<String, Integer> denyCounts = new HashMap<>();
		private final Set<String> permanentlyDenied = new HashSet<>();
		private final Deque<ClassSignal> recentSignals = new ArrayDeque<>();
		private long lastUpdatedTime;
	}
}
