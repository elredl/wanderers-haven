package com.wanderershaven.classsystem;

import com.wanderershaven.classsystem.evolution.ClassEvolutionEngine;
import com.wanderershaven.classsystem.evolution.WarriorEvolutions;
import com.wanderershaven.levelup.ClassLevelEngine;
import com.wanderershaven.levelup.DefaultFeatDefinitions;
import com.wanderershaven.levelup.FeatDefinition;
import com.wanderershaven.skill.ActiveSkillSlots;
import com.wanderershaven.skill.BerserkerSkills;
import com.wanderershaven.skill.BlademasterSkills;
import com.wanderershaven.skill.DuelistSkills;
import com.wanderershaven.skill.PaladinSkills;
import com.wanderershaven.skill.VanguardSkills;
import com.wanderershaven.skill.SkillDefinition;
import com.wanderershaven.skill.SkillRollEngine;
import com.wanderershaven.skill.WarriorSkills;
import com.wanderershaven.stat.PlayerStatEngine;
import com.wanderershaven.stat.SkillStatTable;
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
		SkillRollEngine engine = new SkillRollEngine(WarriorSkills.all());
		// Register evolution-exclusive skills at startup so forceGrantSkill can find them
		// and the roll engine can filter them for non-evolution players.
		// Add each new evolution's skills here as they are designed.
		engine.registerEvolutionSkills("warrior_berserker", BerserkerSkills.all());
		engine.registerEvolutionSkills("warrior_paladin",   PaladinSkills.all());
		engine.registerEvolutionSkills("warrior_vanguard",  VanguardSkills.all());
		engine.registerEvolutionSkills("warrior_duelist",      DuelistSkills.all());
		engine.registerEvolutionSkills("warrior_blademaster",  BlademasterSkills.all());
		return engine;
	}
	private static final PlayerStatEngine STAT_ENGINE = buildStatEngine();

	private static PlayerStatEngine buildStatEngine() {
		PlayerStatEngine engine = new PlayerStatEngine();
		SkillStatTable.register(engine);
		return engine;
	}

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

	public static PlayerStatEngine statEngine() {
		return STAT_ENGINE;
	}

	public static void registerSkill(SkillDefinition skill) {
		SKILL_ENGINE.register(skill);
	}

	public static List<ClassConditionNotification> drainNotifications(java.util.UUID playerId) {
		return ENGINE.drainNotifications(playerId);
	}
}
