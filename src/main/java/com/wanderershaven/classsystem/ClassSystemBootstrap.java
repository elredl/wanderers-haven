package com.wanderershaven.classsystem;

import com.wanderershaven.classsystem.evolution.ClassEvolutionEngine;
import com.wanderershaven.classsystem.evolution.WarriorEvolutions;
import com.wanderershaven.levelup.ClassLevelEngine;
import com.wanderershaven.levelup.DefaultFeatDefinitions;
import com.wanderershaven.levelup.FeatDefinition;
import com.wanderershaven.skill.ActiveSkillSlots;
import com.wanderershaven.skill.SkillDefinition;
import com.wanderershaven.skill.SkillRollEngine;
import com.wanderershaven.skill.WarriorSkills;
import java.util.List;

public final class ClassSystemBootstrap {
	private static final ClassInferenceEngine ENGINE = new AffinityClassInferenceEngine(
		ClassProgressionRules.defaults(),
		DefaultClassDefinitions.create()
	);
	private static final ClassLevelEngine LEVEL_ENGINE = new ClassLevelEngine(
		DefaultFeatDefinitions.create()
	);
	private static final SkillRollEngine SKILL_ENGINE = new SkillRollEngine(
		WarriorSkills.all()
	);
	private static final ClassEvolutionEngine EVOLUTION_ENGINE = new ClassEvolutionEngine(
		WarriorEvolutions.all()
	);
	private static final ClassEventIngestionService INGESTION_SERVICE = new ClassEventIngestionService(
		ENGINE, LEVEL_ENGINE, SKILL_ENGINE, EVOLUTION_ENGINE
	);
	private static final ActiveSkillSlots ACTIVE_SKILL_SLOTS = new ActiveSkillSlots();
	private static boolean initialized;

	private ClassSystemBootstrap() {
	}

	public static synchronized void initialize() {
		if (initialized) {
			return;
		}

		INGESTION_SERVICE.registerHooks();
		initialized = true;
	}

	public static ClassInferenceEngine engine() {
		return ENGINE;
	}

	public static ClassLevelEngine levelEngine() {
		return LEVEL_ENGINE;
	}

	public static void registerClassDefinition(ClassDefinition classDefinition) {
		ENGINE.registerClass(classDefinition);
	}

	public static void registerFeatDefinition(FeatDefinition featDefinition) {
		LEVEL_ENGINE.registerFeatDefinition(featDefinition);
	}

	public static SkillRollEngine skillEngine() {
		return SKILL_ENGINE;
	}

	public static ClassEvolutionEngine evolutionEngine() {
		return EVOLUTION_ENGINE;
	}

	public static ClassEventIngestionService ingestionService() {
		return INGESTION_SERVICE;
	}

	public static ActiveSkillSlots activeSkillSlots() {
		return ACTIVE_SKILL_SLOTS;
	}

	public static void registerSkill(SkillDefinition skill) {
		SKILL_ENGINE.register(skill);
	}

	public static List<ClassConditionNotification> drainNotifications(java.util.UUID playerId) {
		return ENGINE.drainNotifications(playerId);
	}
}
