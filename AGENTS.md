# AGENTS.md

Repository-wide guidance. A nested `AGENTS.md` or `AGENTS.override.md` takes precedence for its directory.

## Targets

| Branch | Minecraft | Loader | Java | Build plugin |
| --- | --- | --- | --- | --- |
| `master` | 1.20.1 | Forge 47.4.x | 17 | ForgeGradle |
| `1.21.1` | 1.21.1 | NeoForge 21.1.x | 21 | ModDevGradle |

Check the current branch and `gradle.properties` first. When porting, preserve behavior but adapt loader imports, events, registration, networking, metadata, and Minecraft APIs; do not apply cross-branch code mechanically.

## Layout

- `src/main/java/com/flansmodultimate`: main code; `client` is client-only, while `common` contains shared gameplay.
- `network`, `config`, `event`, and `mixin`: packets, configuration, loader events, and Mixins.
- `platform`: NeoForge adapters on `1.21.1`; `src/main/templates`: generated NeoForge metadata inputs.
- `src/main/resources` and `src/generated/resources`: resources and generated data; do not hand-edit generator-owned output.
- `src/packs` and `src/officialpacks`: separately packaged content mods.
- `src/test/java`: unit tests. `run`, `run-data`, and `build` are generated/local directories.

## Commands

Use the Gradle wrapper (`./gradlew` on Linux/macOS):

```powershell
.\gradlew.bat test
.\gradlew.bat build --stacktrace
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runData
.\gradlew.bat packsJar officialPacksJar
```

Run focused tests first. Run a full build after changes to loader setup, registries, networking, entities, resources, source sets, or packaging. CI uses Java 17 for `master` and Java 21 for `1.21.1`; keep `gradlew` executable.

## Project Rules

- `master` uses `net.minecraftforge.*` and `src/main/resources/META-INF/mods.toml`; `1.21.1` uses `net.neoforged.*` and generated `neoforge.mods.toml` metadata.
- Keep gameplay state authoritative on the server and client-only classes out of common/server initialization. Validate entity, menu, and inventory access in packet handlers.
- Preserve deterministic legacy-content loading and compatibility. Do not bulk-change definition whitespace, casing, filenames, encodings, or layouts.
- Treat `src/officialpacks/resources/flans_models` as imported binary assets. Preserve exclusions for legacy `*PackMod.class` bootstrap classes.
- Keep the main mod, bundled packs, and official packs as separate artifacts. Run `packsJar` or `officialPacksJar` when their inputs or packaging change.
- Reuse existing coordinate, collision, suspension, and interpolation helpers for driveables. Add tests for deterministic parsers and pure logic where practical.
- Keep Mixin JSON, targets, and signatures synchronized. Avoid early static registry access.
- Do not edit or commit generated output, runtime files, or unrelated working-tree changes. Stage explicit paths in a mixed worktree.

Before completion, review the scoped diff, run relevant checks plus `git diff --check`, and report any validation that was not possible.
