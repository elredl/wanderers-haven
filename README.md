# Wanderers Haven

Wanderers Haven is a Fabric RPG-combat mod for Minecraft `1.21.11`.

It adds a class progression system, skill trees and evolutions, active abilities, custom melee weapon families, and an in-progress archery kit.

## Current Features (So Far)

- Class framework with level progression and skill rolls.
- Warrior class with evolution paths:
  - Berserker
  - Paladin
  - Vanguard
  - Duelist
  - Blade Dancer
  - Blademaster
  - Spearmaster
  - Headhunter
  - Mauler
  - Executioner
- Active skill execution pipeline with cooldowns, timed effects, and shared combat helpers.
- Client UI/screens for class selection, skill management/evolution offers, radial active-skill selection, and stats view.
- Custom weapon families (rapiers, twinblades, greatswords, greathammers, scythes, maces).
- Archery foundation:
  - Vanilla bow renamed to **Longbow** with slower charge and stronger full draw.
  - **Quiver** item with a 5-slot internal container (arrow-only slots).
  - **Twin Crossbow** with dual-hand behavior and delayed second shot.
  - Vanilla crossbow supports a 2-bolt magazine behavior.
  - **Shortbow & Buckler** hybrid weapon (bow shot + melee bash recoil gameplay).
  - Crossbow ammo HUD overlay (`x/2`) over the hotbar slot.

## Requirements

- Minecraft `1.21.11`
- Java `21`
- Fabric Loader `>=0.18.3`
- Fabric API

Optional integration:

- Better Combat (`>=3.0.1` suggested)

## Build & Run (Dev)

```bash
./gradlew build
```

```bash
./gradlew runClient
```

Quick compile check used during gameplay-system work:

```bash
./gradlew compileJava
```

## Admin Commands

The mod registers `/wh` commands for testing/admin workflows (requires gamemaster-level permissions):

- `/wh gui [player]`
- `/wh giveclass <player> <class>`
- `/wh levelup <player> <class> [levels]`
- `/wh progress <player> <class>`
- `/wh giveskill <player> <skill>`

## Project Notes

- Core class/subclass content is centralized through `DefaultClassContent` and auto-bootstrapped by `ClassSystemBootstrap`.
- Skill stat effects are defined in `SkillStatTable`.
- Active skill behavior lives in `SkillEffectService`.
- The mod is under active iteration; balance and some feature behavior are still changing.

## License

This project is licensed under `CC0-1.0`.
