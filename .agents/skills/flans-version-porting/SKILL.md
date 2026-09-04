---
name: flans-version-porting
description: Port or implement version-dependent Flan's Mod Ultimate changes across its maintained Forge and NeoForge branches. Use for loader migrations, cross-branch ports, or changes involving version-specific APIs, metadata, rendering, registration, events, or networking.
---

# Flans Version Porting

Use this workflow whenever a change depends on the Minecraft branch or is being
ported between branches. Preserve behavior while adapting to the destination
loader and APIs; do not mechanically copy source.

## Maintained targets

| Branch | Minecraft | Loader | Java | Build plugin |
| --- | --- | --- | --- | --- |
| `master` | 1.20.1 | Forge 47.4.x | 17 | ForgeGradle |
| `1.21.1` | 1.21.1 | NeoForge 21.1.x | 21 | ModDevGradle |
| `26.1.2` | 26.1.2 | NeoForge 26.1.2.x | 25 | ModDevGradle |
| `26.2` | 26.2 | NeoForge 26.2.x | 25 | ModDevGradle |

Treat this table as routing information, then verify the checked-out branch and
`gradle.properties` before editing. Repository state is authoritative if a target
has changed since this guide was written.

## Porting rules

- Preserve observable behavior and data compatibility unless the task explicitly
  requests a behavior change.
- Adapt loader APIs, lifecycle events, registration, networking, metadata, names,
  and rendering architecture to the destination branch.
- `master` uses Forge and `mods.toml`; newer branches use NeoForge and generated
  metadata.
- The 26.x branches use Mojang names and modern render-state extraction. Branch
  `26.2` additionally uses feature rendering.
- Search the destination branch for an established equivalent before introducing
  a compatibility wrapper or parallel implementation.
- Keep common/server code free of client-only initialization. Preserve
  server-authoritative gameplay state and deterministic legacy-content loading.
- Do not overwrite unrelated branch-specific improvements merely to make files
  resemble the source branch.

## Validation

Run focused tests first with the destination branch's Gradle wrapper and Java
version. Run a full build after changes to loader setup, registries, networking,
entities, resources, source sets, or packaging. If bundled or official pack inputs
or packaging changed, also run `packsManagerJar` and/or `officialPacksJar` as
applicable. Review the scoped diff and run `git diff --check` before completion.
