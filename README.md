# Flan's Mod Ultimate 2

Flan's Mod Ultimate 2 is an unofficial continuation of Flan's Mod. It loads and modernizes legacy Flan's Mod content packs, including weapons, ammo, armor, gun boxes, armor boxes, workbenches, paintjobs, sounds, particles, and related gameplay systems.

The repository maintains four Minecraft targets. Choose the branch that matches the version and mod loader you want to develop for or run.

## Supported Versions

| Branch | Minecraft | Loader | Java | Build plugin |
| --- | --- | --- | --- | --- |
| `master` | 1.20.1 | Forge 47.4.x | 17 | ForgeGradle |
| `1.21.1` | 1.21.1 | NeoForge 21.1.x | 21 | ModDevGradle |
| `26.1.2` (WIP - not supported yet) | 26.1.2 | NeoForge 26.1.2.x | 25 | ModDevGradle |
| `26.2` (WIP - not supported yet) | 26.2 | NeoForge 26.2.x | 25 | ModDevGradle |

The main mod ID is `flansmodultimate`. The project is in beta: some legacy systems are complete, while others remain in progress.

## Features

- Loads legacy-style Flan's Mod content packs with compatibility-oriented parsing, including fallback encodings for older and Chinese packs.
- Registers guns, bullets, grenades, armor, attachments, tools, parts, boxes, workbenches, paintjob tables, entities, particles, sounds, menus, and creative tabs.
- Supports custom armor rendering and configuration behavior.
- Includes gun handling, reload logic, fire modes, hit markers, scopes, recoil, spread, melee behavior, block-hit effects, and network synchronization.
- Includes optional digital ammo support with server-side storage, HUD synchronization, admin commands, and configurable supply blocks.
- Packages the Packs Manager companion mod, bundled packs, and official content packs separately from the main mod.

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

Run the development client or server (`runClientDebug` logs at DEBUG, the others at INFO):

```powershell
.\gradlew.bat runClient
.\gradlew.bat runClientDebug
.\gradlew.bat runServer
```

For multiplayer testing, run `runClient` (`Player1`) and `runClient2` (`Player2`)
in separate terminals. For IntelliJ, run `genIntellijRuns` and use the two client
run configurations. The second client stores its settings, saves, and content in
`run/second-client`; put matching external packs in `run/second-client/flan` as
well as `run/flan`. Bundled packs load in both clients. These offline development
identities need a local test server with `online-mode=false`; an authenticated
LAN host rejects them as invalid sessions.

Generate data:

```powershell
.\gradlew.bat runData
```

Build the separate Packs Manager, content-pack, and NPC Vehicles & Soldiers artifacts:

```powershell
.\gradlew.bat packsManagerJar
.\gradlew.bat officialPacksJar
.\gradlew.bat npcsJar
```

Build artifacts are written under `build/libs/`.

NPC Vehicles & Soldiers uses `2.X.Y`: `2.X` identifies the main mod's API
compatibility line, and `Y` is the NPC module's independent revision, starting at
zero. For example, NPC `2.2.0` and `2.2.1` both support main mod `2.2` and `2.2.x`,
with a dependency range of `[2.2,2.3)`. Compatible main-mod fixes advance its patch
version without requiring an NPC release. API changes advance the main
mod's minor version (for example, `2.2` to `2.3`), and the NPC module moves with it,
starting at `2.3.0`. Update `archiveVersion` in `src/npcs/fmu-module.gradle`, the main-mod
range in `src/npcs/resources/META-INF/mods.toml`, and add `changelogs/wolffsmodnpcs/<version>.md` together.
Content packs keep their existing independent versions; they do not use this API
compatibility scheme. The published API artifact's `api_version` equals the main mod's `2.X` line.

Each module also has a separate, opt-in CurseForge upload task. Configure its
project URL and ID in its `fmu-module.gradle`, then run a task such as `publishCurseForgeOfficialPacks` from
the checkout for the Minecraft version you want to release. See
[CurseForge releases](docs/curseforge-releases.md) for all tasks, token setup,
per-module settings, and upload previews.

## Repository Layout

| Path | Purpose |
| --- | --- |
| `src/main/java` | Main mod Java sources |
| `src/main/resources` | Main mod resources, assets, data, Mixin configuration, and loader metadata |
| `src/packsmanager/java` | Packs Manager companion-mod sources and extraction logic |
| `src/packsmanager/resources` | Packs Manager metadata and bundled-pack version marker |
| `src/officialpacks` | Optional official content packs and their entrypoint |
| `src/npcs` | Flan's Ultimate 2: Wolff's NPC Vehicles & Soldiers, a separate mod requiring Flan's Mod Ultimate and Custom NPCs |
| `libs` | Local mod jars used as development dependencies |
| `run` | Development runtime directory |
| `run-data` | Data-generation runtime directory |
| `docs` | Project documentation |
| `TODO` | Current work items and feature notes |

## Configuration and Content Packs

The mod registers client and common loader configuration files at runtime. Common settings cover gameplay, damage, armor, guns, shootables, sound ranges, penetration, digital ammo, and enchantment modules.

Legacy packs are loaded through the mod's content-loading system. The optional **Flan's Mod Ultimate Packs Manager** companion mod allows mod authors and pack distributors to ship one or more legacy content packs inside a normal mod jar. At startup, it extracts the bundled `flan` directory into the content-pack directory configured by the main mod, while versioned extraction state prevents unnecessary repeat extraction.

For local development, `packsManagerJar` bundles the contents of `run/flan` into the jar's `flan/` directory and writes `[<Minecraft version>] Flan's Mod Ultimate Packs Manager-<version>.jar` to `build/libs/`.

Crafting-table and furnace recipes appear in Minecraft's recipe book after you collect a relevant ingredient. Legacy pack recipes receive these unlocks automatically when their recipe data is generated. Recipes supplied separately by a data pack keep that pack's own unlock rules.

With optional **JEI 15.20 or newer** installed on Forge 1.20.1, recipe lookups also show vehicle assembly and gun-box purchases, their material quantities, compatible engines and crafting stations. Vehicle assembly works at either the Vehicle Crafting Table or Gun Modification Table; gun-box purchases require the particular box shown in the recipe. Click a custom crafting screen's title to open its JEI category. JEI shows the requirements; crafting still happens through the workbench's own controls. For development, add `-PwithJei` to `runClient`.

At the Paintjob Table or Gun Modification Table, left-click a paintjob to pay its listed ingredients. Right-click its icon to replace all ordinary dyes with one rainbow paint can. Any rainbow cans already listed for a special paintjob are additional: a paintjob requiring dyes and one can costs two cans with this option. Hover over the icon to see the total can cost. Free paintjobs remain free.

## Development Notes

- See [Platform-independent development practices](docs/platform-independent-practices.md) for shared helpers, item data ownership and cross-version tests.
- Java sources are compiled with UTF-8 encoding.
- The project uses Sponge Mixin and generates a refmap for `flansmodultimate`.
- Keep loader-specific APIs, metadata, event registration, networking, and Java toolchain appropriate to the checked-out branch. The 26.1.2 and 26.2 branches use NeoForge's modern rendering/extraction APIs and Mojang's deobfuscated names; 26.2 additionally uses the feature-rendering pipeline introduced in that release.

## Documentation

Project wiki pages live on the [GitHub wiki](https://github.com/AntonIT99/Flans-Mod-Ultimate-2.0/wiki).

See [Optional external mod compatibility](docs/optional-mod-compatibility.md) for supported integrations, Minecraft versions, features and project links.

## Upstream Projects, Attribution & License

Flan's Mod Ultimate 2 is an unofficial and independently maintained
modern continuation and reimplementation of the Flan's Mod ecosystem.

The project's primary legacy source and reference implementation was
**Flan's Mod Ultimate Stability Edition (FMUSE)**:
https://github.com/Unknown025/Flans-Mod-Plus

FMUSE itself continues the lineage of Flan's Mod Ultimate and the original
Flan's Mod.

Flan's Mod Ultimate 2 is not affiliated with, sponsored by, or endorsed by
Jamioflan, the current Flan's Mod developers, Flan's Mod: Reloaded, or the
maintainers of the upstream projects.

For detailed upstream attribution and project lineage, see
[ATTRIBUTION.md](ATTRIBUTION.md).

Flan's Mod Ultimate 2 is distributed under the
**Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported
(CC BY-NC-SA 3.0)** license. The companion mod Flan's Ultimate 2: NPC
Vehicles & Soldiers (`src/npcs`) is distributed under the same license.

See [LICENSE](LICENSE) for the complete license terms.

The public API in `com.flansmodultimate.api` is an exception: it is licensed
under the **MIT License**, so other mods may build and distribute addons against
it freely. See [LICENSE-API](LICENSE-API) and the
[API wiki page](https://github.com/AntonIT99/Flans-Mod-Ultimate-2.0/wiki/API).
