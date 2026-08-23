# Flan's Mod Ultimate 2

Flan's Mod Ultimate 2 is an unofficial continuation of Flan's Mod. It loads and modernizes legacy Flan's Mod content packs, including weapons, ammo, armor, gun boxes, armor boxes, workbenches, paintjobs, sounds, particles, and related gameplay systems.

The repository maintains four Minecraft targets. Choose the branch that matches the version and mod loader you want to develop for or run.

## Supported Versions

| Branch | Minecraft | Loader | Java | Build plugin |
| --- | --- | --- | --- | --- |
| `master` | 1.20.1 | Forge 47.4.x | 17 | ForgeGradle |
| `1.21.1` | 1.21.1 | NeoForge 21.1.x | 21 | ModDevGradle |
| `26.1.2` | 26.1.2 | NeoForge 26.1.2.x | 25 | ModDevGradle |
| `26.2` | 26.2 | NeoForge 26.2.x | 25 | ModDevGradle |

The main mod ID is `flansmodultimate`. The project is in beta: some legacy systems are complete, while others remain in progress.

## Features

- Loads legacy-style Flan's Mod content packs with compatibility-oriented parsing, including fallback encodings for older and Chinese packs.
- Registers guns, bullets, grenades, armor, attachments, tools, parts, boxes, workbenches, paintjob tables, entities, particles, sounds, menus, and creative tabs.
- Supports custom armor rendering and configuration behavior.
- Includes gun handling, reload logic, fire modes, hit markers, scopes, recoil, spread, melee behavior, block-hit effects, and network synchronization.
- Includes optional digital ammo support with server-side storage, HUD synchronization, admin commands, and configurable supply blocks.
- Packages bundled and official content packs separately from the main mod.

## Requirements

- Check out the branch for the Minecraft version you need.
- Use the JDK listed for that branch in the table above.
- Install the matching Forge or NeoForge runtime for gameplay; development builds use the included Gradle wrapper.

## Getting Started

Clone or open the repository and let Gradle import the project. If the IDE is missing libraries, refresh dependencies:

```powershell
.\gradlew.bat --refresh-dependencies
```

The 1.20.1 `master` branch also supports generating IDE run configurations:

```powershell
.\gradlew.bat genIntellijRuns
.\gradlew.bat genEclipseRuns
```

## Common Commands

Build the main mod:

```powershell
.\gradlew.bat build --stacktrace
```

Run the development client or server:

```powershell
.\gradlew.bat runClient
.\gradlew.bat runServer
```

Generate data:

```powershell
.\gradlew.bat runData
```

Build the separate content-pack artifacts:

```powershell
.\gradlew.bat packsJar
.\gradlew.bat officialPacksJar
```

Build artifacts are written under `build/libs/`.

## Repository Layout

| Path | Purpose |
| --- | --- |
| `src/main/java` | Main mod Java sources |
| `src/main/resources` | Main mod resources, assets, data, Mixin configuration, and loader metadata |
| `src/packs/java` | Optional bundled content-pack mod sources |
| `src/packs/resources` | Bundled content-pack resources |
| `src/officialpacks` | Optional official content packs and their entrypoint |
| `libs` | Local mod jars used as development dependencies |
| `run` | Development runtime directory |
| `run-data` | Data-generation runtime directory |
| `docs` | Project documentation |
| `TODO` | Current work items and feature notes |

## Configuration and Content Packs

The mod registers client and common loader configuration files at runtime. Common settings cover gameplay, damage, armor, guns, shootables, sound ranges, penetration, digital ammo, and enchantment modules.

Legacy packs are loaded through the mod's content-loading system. For local development, bundled pack content is copied from `run/flan` into `src/packs/resources/flan` when `packsJar` runs.

## Development Notes

- Java sources are compiled with UTF-8 encoding.
- The project uses Sponge Mixin and generates a refmap for `flansmodultimate`.
- Keep loader-specific APIs, metadata, event registration, networking, and Java toolchain appropriate to the checked-out branch. The 26.1.2 and 26.2 branches use NeoForge's modern rendering/extraction APIs and Mojang's deobfuscated names; 26.2 additionally uses the feature-rendering pipeline introduced in that release.
- `README.txt` contains legacy Forge MDK setup notes; use this file for the current project guidance.

## Documentation

Project wiki pages live on the [GitHub wiki](https://github.com/AntonIT99/Flans-Mod-Ultimate-2.0/wiki).

## Credits

Flan's Mod Ultimate 2 builds on the Flan's Mod concept created by Jamioflan.
