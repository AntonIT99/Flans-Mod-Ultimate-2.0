# Flan's Mod Ultimate 2

Flan's Mod Ultimate 2 is an unofficial continuation of Flan's Mod for Minecraft 1.20.1. It focuses on loading and modernizing content packs designed for legacy Flan's Mod versions, including weapons, ammo, armor, gun boxes, armor boxes, workbenches, paintjobs, sounds, particles, and related gameplay systems.

This repository contains the main Forge mod and an optional bundled content-pack module.

## Project Status

| Item             | Value                         |
|------------------|-------------------------------|
| Minecraft        | 1.20.1                        |
| Forge            | 47.4.10                       |
| Java             | 17                            |
| Main mod id      | `flansmodultimate`            |
| Main mod version | `2.0-beta-3`                  |
| Packs mod id     | `flansmodultimate_packs`      |
| Packs version    | `1.0-beta-1`                  |
| Mappings         | Parchment `2023.09.03-1.20.1` |

The project is still in beta. Some legacy systems are implemented, some are work in progress.

## Features

- Loads legacy-style Flan's Mod content packs in a Minecraft 1.20.1 Forge environment.
- Registers guns, bullets, grenades, armor, attachments, tools, parts, boxes, workbenches, paintjob tables, entities, particles, sounds, menus, and creative tabs.
- Supports custom armor rendering and armor configuration behavior.
- Includes gun handling, reload logic, fire modes, hit markers, scopes, recoil, spread, melee behavior, block hit effects, and network synchronization.
- Includes optional digital ammo support with server-side storage, HUD sync, admin commands, and configurable supply blocks.
- Includes compatibility-oriented content loading, including fallback text encodings for older and Chinese content packs.
- Provides a separate `packs` source set for packaging bundled content packs.

## Requirements

- JDK 17
- Minecraft Forge for Minecraft 1.20.1
- Gradle wrapper included in this repository

## Getting Started

Clone or open the repository, then let Gradle import the project.

For IntelliJ IDEA:

```powershell
.\gradlew.bat genIntellijRuns
```

For Eclipse:

```powershell
.\gradlew.bat genEclipseRuns
```

To refresh dependencies if the IDE is missing libraries:

```powershell
.\gradlew.bat --refresh-dependencies
```

## Common Commands

Build the main mod:

```powershell
.\gradlew.bat build
```

Run the development client:

```powershell
.\gradlew.bat runClient
```

Run the development server:

```powershell
.\gradlew.bat runServer
```

Build the bundled packs jar:

```powershell
.\gradlew.bat packsJar
```

Generate data:

```powershell
.\gradlew.bat runData
```

Build artifacts are written under `build/libs/`.

## Documentation

Project wiki pages live on the [GitHub wiki](https://github.com/AntonIT99/Flans-Mod-Ultimate-2.0/wiki).

## Repository Layout

| Path                  | Purpose                                                      |
|-----------------------|--------------------------------------------------------------|
| `src/main/java`       | Main mod Java sources                                        |
| `src/main/resources`  | Main mod resources, assets, data, mixin config, and metadata |
| `src/packs/java`      | Optional bundled content-pack mod sources                    |
| `src/packs/resources` | Bundled content-pack resources                               |
| `libs`                | Local mod jars used as development dependencies              |
| `run`                 | Development runtime directory                                |
| `run-data`            | Data generation runtime directory                            |
| `docs`                | Project documentation area                                   |
| `TODO`                | Current work items and feature notes                         |

## Configuration

The mod registers client and common Forge config files at runtime. Current common settings cover gameplay, damage, armor, guns, shootables, sound ranges, penetration, digital ammo, and enchantment modules.

More detailed configuration documentation will be added later.

## Content Packs

Legacy content packs are loaded through the mod's content loading system. The project also contains a `packs` source set that can package bundled packs into a separate `flansmodultimate_packs` mod jar.

For local development, bundled pack content is copied from:

```text
run/flan
```

into:

```text
src/packs/resources/flan
```

when running:

```powershell
.\gradlew.bat packsJar
```

## Development Notes

- Java sources are compiled with UTF-8 encoding.
- The project uses Sponge Mixin and generates a refmap for `flansmodultimate`.
- The `packsJar` task builds the optional content-pack mod separately from the main mod jar.
- Existing `README.txt` contains Forge MDK setup notes.

## Credits

Flan's Mod Ultimate 2 builds on the Flan's Mod concept created by Jamioflan.
