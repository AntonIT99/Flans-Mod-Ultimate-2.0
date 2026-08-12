# AGENTS.md

This file is the primary working guide for AI coding agents in this repository. It applies to the entire repository unless a more specific `AGENTS.md` exists in a subdirectory.

## Project Overview

Flan's Mod Ultimate 2 is an unofficial continuation of Flan's Mod. It loads and modernizes legacy Flan content packs and implements weapons, ammunition, armor, attachments, boxes, workbenches, paintjobs, driveables, teams, networking, rendering, configuration, and compatibility systems.

The repository maintains two active platform branches:

| Branch | Minecraft | Loader | Java | Build plugin |
| --- | --- | --- | --- | --- |
| `master` | 1.20.1 | Forge 47.4.x | 17 | ForgeGradle |
| `1.21.1` | 1.21.1 | NeoForge 21.1.x | 21 | ModDevGradle |

Always check the current branch and `gradle.properties` before changing loader APIs. Do not copy Forge 1.20.1 code into the NeoForge branch, or NeoForge 1.21.1 code into `master`, without adapting imports, events, registration, networking, config, pack metadata, and changed Minecraft APIs.

## Important Paths

- `src/main/java/com/flansmodultimate`: primary mod implementation.
- `src/main/java/com/flansmodultimate/client`: client-only input, models, HUD, and rendering.
- `src/main/java/com/flansmodultimate/common`: shared gameplay, entities, items, types, driveables, teams, and inventories.
- `src/main/java/com/flansmodultimate/network`: packet definitions and network registration.
- `src/main/java/com/flansmodultimate/config`: client and common configuration.
- `src/main/java/com/flansmodultimate/event`: loader event handlers.
- `src/main/java/com/flansmodultimate/mixin`: Mixins; keep mixin JSON and method targets synchronized.
- `src/main/java/com/flansmodultimate/platform`: NeoForge-specific platform adapters; currently present on `1.21.1`.
- `src/main/java/com/flansmod`: legacy-compatible model and utility classes used by imported content.
- `src/main/resources`: assets, data, mixin configuration, and main-mod resources.
- `src/main/templates`: generated NeoForge metadata templates on `1.21.1`.
- `src/generated/resources`: data-generator output. Do not hand-edit generated files when a generator is responsible for them.
- `src/packs`: optional bundled content-pack mod source set.
- `src/officialpacks`: optional official content-pack mod, assets, definitions, and legacy compiled models.
- `src/test/java`: unit tests for content providers, coordinate conversion, collision/suspension helpers, and parsing utilities.
- `run`: local game runtime and user content-pack directory.
- `run-data`: data-generation runtime directory.
- `build`: generated build output; never commit it.
- `libs`: local development-only mod dependencies.

## Build and Verification

Use the Gradle wrapper. On Windows use `gradlew.bat`; on Linux/macOS use `./gradlew`.

Common commands:

```powershell
.\gradlew.bat build --stacktrace
.\gradlew.bat test
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runData
.\gradlew.bat packsJar
.\gradlew.bat officialPacksJar
```

For a normal code change, run the narrowest relevant test first and then `build`. Changes to loader setup, resources, source sets, content packaging, networking, entities, or registries require a full build. A successful compilation alone is not sufficient when tests or packaging are affected.

GitHub Actions builds `master` with Java 17 and `1.21.1` with Java 21 on Linux. Keep `gradlew` executable in Git (`100755`) and keep workflows branch-specific.

## Branch-Specific Rules

### `master`

- Target Forge for Minecraft 1.20.1 and Java 17.
- Forge packages normally use `net.minecraftforge.*`.
- Main metadata is stored in `src/main/resources/META-INF/mods.toml`.
- Official-packs metadata uses Forge `mods.toml`.
- Preserve ForgeGradle, Parchment, MixinGradle, reobfuscation, and source-set behavior in `build.gradle`.

### `1.21.1`

- Target NeoForge for Minecraft 1.21.1 and Java 21.
- Loader packages normally use `net.neoforged.*`.
- Main metadata is generated from `src/main/templates/META-INF/neoforge.mods.toml`.
- Official-packs metadata uses `META-INF/neoforge.mods.toml`.
- Preserve ModDevGradle source-set bindings and generated metadata tasks.
- Account for Minecraft 1.21 API changes such as builder-based synchronized entity data, pack repository APIs, entity interpolation signatures, and NeoForge config/network registration.

When porting commits between branches, preserve behavior rather than applying code mechanically. Compile after each meaningful group of API adaptations.

## Architecture and Coding Guidance

- `FlansMod` is the central entry point and registry coordinator. Avoid adding unrelated behavior to its constructor.
- `ContentManager` and content-provider classes discover, preprocess, index, and load legacy content. Preserve deterministic ordering and conflict reporting.
- Type classes represent parsed legacy definitions. Treat legacy file compatibility as a public contract: tolerate established formats and encodings unless a migration is explicitly intended.
- Driveable code is sensitive to coordinate conventions, interpolation, physics, collision, passenger positioning, and client/server ownership. Reuse helpers such as legacy coordinate conversion and suspension/collision utilities instead of duplicating transformations.
- Keep authoritative gameplay state on the server. Client code may predict or interpolate presentation but must not become authoritative for damage, ammunition, inventory, vehicle state, or spawning.
- Register packets consistently on both sides and validate entity, menu, and inventory access in handlers.
- Keep client-only Minecraft classes out of common/server initialization paths.
- Use `ResourceLocation`, registry holders, and loader registration mechanisms appropriate to the active branch. Avoid static initialization that reads registries too early.
- Follow the existing Java style: four-space indentation, braces on their own lines, descriptive names, and small focused helpers. Do not reformat unrelated files.
- Add comments for legacy compatibility rules, non-obvious coordinate math, loader workarounds, and synchronization decisions. Avoid comments that merely restate the code.

## Content Packs and Resources

- Legacy content definitions are data, even when they use `.txt`; preserve their syntax, identifier casing expectations, recipe layout, and compatibility quirks.
- Do not bulk-normalize whitespace, line endings, encodings, or filenames in content packs without a dedicated reason and validation. Some legacy parsers and pack references are sensitive to exact names.
- Asset paths and resource identifiers must remain lowercase where Minecraft requires it.
- `src/officialpacks/resources/flans_models` contains legacy compiled model classes. Treat these as imported binary assets. Do not decompile, rewrite, or mass-delete them as part of unrelated work.
- Legacy `*PackMod.class` bootstrap classes under official model resources must not be discovered as active loader mods. Preserve the build exclusions that prevent accidental scanning.
- Keep the main mod, bundled packs, and official packs as separate artifacts. Verify `packsJar` or `officialPacksJar` when changing their source sets, metadata, entry points, resources, or packaging rules.
- Do not copy files from `run/flan` into tracked resources unless the task explicitly concerns updating bundled packs.

## Testing Expectations

- Add or update unit tests for pure logic, parsers, coordinate transforms, collision profiles, suspension calculations, and packaged-content behavior.
- Prefer extracting deterministic helpers from Minecraft-heavy classes when this makes behavior testable without launching the game.
- For rendering, input, networking, registry, or lifecycle changes that cannot be covered by unit tests, run the appropriate development client/server when practical and clearly report what was not runtime-tested.
- Check both logical sides for networking and entity changes. A client-only success does not prove dedicated-server compatibility.
- Run `git diff --check` before committing. Existing whitespace in imported legacy content may be intentional; do not clean it incidentally.

## Git and Workspace Safety

- Inspect `git status --short --branch` before editing.
- The working tree may contain user changes. Never discard, reset, overwrite, stage, or commit unrelated changes.
- Stage explicit paths when the tree is mixed. Do not use `git add -A` unless the entire worktree is known to belong to the current task.
- Do not use destructive commands such as `git reset --hard` or `git checkout --` on user work.
- Use a separate worktree when work on another branch would collide with local changes.
- Keep commits focused. Porting commits on `master` and `1.21.1` should use the same intent and message where practical, but their hashes will differ because their parent histories differ.
- Do not push, force-push, create releases, or modify branch protection unless the user explicitly requests it.

## Completion Checklist

Before handing off a change:

1. Confirm the active branch and its correct Minecraft, loader, and Java versions.
2. Review the complete diff for accidental generated files, binaries, content-pack churn, or unrelated edits.
3. Run relevant tests and the appropriate Gradle build/package tasks.
4. Run `git diff --check`, accounting for intentional legacy content whitespace.
5. Report the files changed, validation performed, remaining risks, and whether anything was committed or pushed.
