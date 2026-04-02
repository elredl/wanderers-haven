# Wanderers Haven Context

This file is a quick implementation guide so future changes need less repo exploration.

## Core Architecture

- Class + subclass content is centralized in `src/main/java/com/wanderershaven/classsystem/DefaultClassContent.java`.
- A class bundle is represented by `src/main/java/com/wanderershaven/classsystem/ClassContentDefinition.java`:
  - `ClassDefinition` (how class affinity is earned)
  - base skills (normal roll pool)
  - evolutions/subclasses (`ClassEvolutionDef` list)
- Bootstrapping is automatic in `src/main/java/com/wanderershaven/classsystem/ClassSystemBootstrap.java`:
  - builds skill roll engine from all class bundles
  - registers evolution skill pools from each evolution's `EvolutionSkillSet`
  - builds evolution engine from all class bundles

## Where To Edit Things

### Add a New Base Class

1. Create class skill definitions file (similar to `WarriorSkills.java`).
2. Create class evolution definitions file (similar to `WarriorEvolutions.java`).
3. Add one bundle entry in `DefaultClassContent.all()`.
4. Ensure any new passive/timed stat sources are declared in `SkillStatTable`.

No manual bootstrap wiring should be needed if the class is in `DefaultClassContent`.

### Add a New Subclass / Evolution

1. Add an evolution entry in the class evolution file (e.g. `WarriorEvolutions.java`) using `EvolutionDefs.evolution(...)`.
2. Add evolution skill set class (e.g. `BladeDancerSkills.java`) returning `EvolutionSkillSet`.
3. Reference that skill set in the `ClassEvolutionDef` constructor.

No manual skill-engine evolution registration is needed; bootstrap reads evolution skill sets automatically.

### Add a New Active Skill

1. Add/mark skill as `active` in skill definitions.
2. Implement behavior in `SkillEffectService`.
3. Register handler in `buildActiveSkillRegistry()` in `SkillEffectService`.

Centralized utilities available:

- `ActiveSkillRegistry` (`skillId -> handler`)
- `SkillCooldownService` (shared cooldown bookkeeping)
- `TimedEffectTracker` (shared timed window tracking)
- `CombatDebuffService` (shared dealt/taken debuff multipliers)
- `EntityAttributeDebuffService` (shared timed attribute debuffs on entities)
- targeting helpers inside `SkillEffectService` (`nearbyEnemies`, `coneEnemies`, `pathBox`)

### Create Skill Definitions Faster

- Use `SkillDefs` helpers in `src/main/java/com/wanderershaven/skill/SkillDefs.java`:
  - `passive(...)`
  - `active(...)`
  - `upgrade(...)`
  - `activeUpgrade(...)`
- Evolution/base skill files can call these directly or through local wrappers.

### Add Passive or Timed Stat Buffs

- Add contributions in `src/main/java/com/wanderershaven/stat/SkillStatTable.java`.
- Activate/deactivate timed source IDs from `SkillEffectService` through `PlayerStatEngine` (`activateSource` / `deactivateSource`).

## Active Skill Flow

1. Client uses active slot.
2. `WanderersHavenNetworking` resolves slot -> `skillId`.
3. Networking calls `SkillEffectService.executeActiveSkill(player, skillId)`.
4. `ActiveSkillRegistry` dispatches the handler.

## Files Most Frequently Touched

- `DefaultClassContent.java` (class/subclass registration)
- `<Class>Skills.java` + `<Class>Evolutions.java` (content definitions)
- `SkillEffectService.java` (active behavior)
- `SkillStatTable.java` (stat modifiers)
- `WanderersHavenNetworking.java` (usually minimal after registry refactor)

## Validation

Run after gameplay-system changes:

```bash
./gradlew compileJava
```

If changing combat behavior substantially, also test in-game active usage, cooldown messages, and buff/debuff expiry.
