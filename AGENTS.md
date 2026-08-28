# AGENTS.md

Nested `AGENTS.md` files add rules for their directories.

## Targets

| Branch | Minecraft | Loader | Java | Build plugin |
| --- | --- | --- | --- | --- |
| `master` | 1.20.1 | Forge 47.4.x | 17 | ForgeGradle |
| `1.21.1` | 1.21.1 | NeoForge 21.1.x | 21 | ModDevGradle |
| `26.1.2` | 26.1.2 | NeoForge 26.1.2.x | 25 | ModDevGradle |
| `26.2` | 26.2 | NeoForge 26.2.x | 25 | ModDevGradle |

Check the current branch and `gradle.properties` before version-specific changes.
When porting, preserve behavior; adapt loader APIs, events, registration, networking,
and metadata rather than applying code mechanically. `master` uses Forge and
`mods.toml`; newer branches use NeoForge and generated metadata. The 26.x branches
use modern render-state extraction and Mojang names; 26.2 uses feature rendering.

## Repository Rules

- Keep gameplay state server-authoritative. Do not initialize client-only classes
  from common or server code.
- Preserve deterministic legacy-content loading and compatibility. Do not bulk-change
  definition whitespace, casing, filenames, encodings, or layouts.
- Do not edit generated output, runtime files, or generated metadata; change the
  generator or its input instead.
- Keep the main mod, bundled packs, and official packs as separate artifacts. Run
  `packsJar` and/or `officialPacksJar` when their inputs or packaging change.
- In a mixed worktree, preserve unrelated changes and stage explicit paths only.

## Build and Validation

Use the Gradle wrapper. Common tasks are `test`, `build`, `runData`, `packsJar`, and
`officialPacksJar`; use `--stacktrace` only to diagnose a failed build. Run focused
tests first. Run a full build after loader setup, registries, networking, entities,
resources, source sets, or packaging changes. Keep `gradlew` executable.

Before completion, review the scoped diff, run relevant checks and `git diff --check`,
and report validation that could not be performed.
