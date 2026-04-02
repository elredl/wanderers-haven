package com.wanderershaven.classsystem;

import com.wanderershaven.classsystem.evolution.ClassEvolutionEngine;
import com.wanderershaven.classsystem.evolution.ClassEvolutionDef;
import com.wanderershaven.classsystem.evolution.EvolutionSkillSet;
import com.wanderershaven.levelup.ClassLevelEngine;
import com.wanderershaven.levelup.DefaultFeatDefinitions;
import com.wanderershaven.levelup.FeatDefinition;
import com.wanderershaven.skill.ActiveSkillSlots;
import com.wanderershaven.skill.SkillDefinition;
import com.wanderershaven.skill.SkillRollEngine;
import com.wanderershaven.stat.PlayerStatEngine;
import com.wanderershaven.stat.SkillStatTable;
import java.util.ArrayList;
import java.util.List;

public final class ClassSystemBootstrap {
	private static final ClassInferenceEngine ENGINE = new AffinityClassInferenceEngine(
		ClassProgressionRules.defaults(),
		DefaultClassDefinitions.create()
	);
	private static final ClassLevelEngine LEVEL_ENGINE = new ClassLevelEngine(
		DefaultFeatDefinitions.create()
	);
	private static final SkillRollEngine SKILL_ENGINE = buildSkillEngine();

	private static SkillRollEngine buildSkillEngine() {
		List<ClassContentDefinition> content = DefaultClassContent.all();
		List<SkillDefinition> baseSkills = content.stream()
			.flatMap(c -> c.baseSkills().stream())
			.toList();
		SkillRollEngine engine = new SkillRollEngine(baseSkills);
		for (ClassContentDefinition classContent : content) {
			for (ClassEvolutionDef evolution : classContent.evolutions()) {
				List<SkillDefinition> evolutionSkills = skillsForEvolution(evolution.skillSet());
				if (!evolutionSkills.isEmpty()) {
					engine.registerEvolutionSkills(evolution.id(), evolutionSkills);
				}
			}
		}
		return engine;
	}
	private static final PlayerStatEngine STAT_ENGINE = buildStatEngine();

	private static PlayerStatEngine buildStatEngine() {
		PlayerStatEngine engine = new PlayerStatEngine();
		SkillStatTable.register(engine);
		return engine;
	}

	private static final ClassEvolutionEngine EVOLUTION_ENGINE = new ClassEvolutionEngine(
		DefaultClassContent.all().stream()
			.flatMap(c -> c.evolutions().stream())
			.toList()
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

	public static PlayerStatEngine statEngine() {
		return STAT_ENGINE;
	}

	public static void registerSkill(SkillDefinition skill) {
		SKILL_ENGINE.register(skill);
	}

	private static List<SkillDefinition> skillsForEvolution(EvolutionSkillSet skillSet) {
		if (skillSet == null) return List.of();
		List<SkillDefinition> all = new ArrayList<>(skillSet.exclusiveSkills());
		if (skillSet.capstoneSkill() != null) {
			all.add(skillSet.capstoneSkill());
		}
		return all;
	}

	public static List<ClassConditionNotification> drainNotifications(java.util.UUID playerId) {
		return ENGINE.drainNotifications(playerId);
	}
}
