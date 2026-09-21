---
name: flans-version-porting
description: Port or implement version-dependent Flan's Mod Ultimate changes across maintained Forge and NeoForge branches or from legacy 1.7.10/1.12.2 sources. Use for loader migrations, behavioral archaeology, cross-branch ports, and changes involving version-specific APIs, metadata, rendering, registration, events, persistence, or networking.
---

# Flans Version Porting

Preserve behavior and content compatibility while expressing the feature in the
destination branch's architecture. Treat source code as evidence of semantics,
not as text to translate mechanically.

## Choose the workflow

- For a port among `master`, `1.21.1`, `26.1.2`, and `26.2`, use the maintained
  target table and the common workflow below.
- For implementation or migration based on 1.7.10, 1.12.2, or another legacy
  fork, read [references/legacy-to-modern.md](references/legacy-to-modern.md)
  before investigating or editing. It contains the source-authority order,
  repository paths, concept mappings, and legacy semantic traps.
- When both apply, first recover the behavior with the legacy workflow, then
  adapt that behavior to the destination branch.

## Maintained targets

| Branch | Minecraft | Loader | Java | Build plugin |
| --- | --- | --- | --- | --- |
| `master` | 1.20.1 | Forge 47.4.x | 17 | ForgeGradle |
| `1.21.1` | 1.21.1 | NeoForge 21.1.x | 21 | ModDevGradle |
| `26.1.2` | 26.1.2 | NeoForge 26.1.2.x | 25 | ModDevGradle |
| `26.2` | 26.2 | NeoForge 26.2.x | 25 | ModDevGradle |

This table is routing information. Verify the checked-out branch, working tree,
`gradle.properties`, and build script before editing; repository state wins.
Use `git show <branch>:<path>` for a quick comparison and a separate worktree
when the other branch must be built or edited. Do not switch a dirty checkout.

## Common workflow

1. Define the observable contract: authority side, state owner, lifecycle,
   persistence, packet direction, rendering result, content syntax, and failure
   behavior. Record intentional deviations separately.
2. Find the destination's nearest established implementation by semantic role,
   not only by old class or method name. Trace registration and call sites as
   well as the implementation.
3. Port the smallest end-to-end slice. Keep domain rules independent of loader
   glue where practical, and adapt the boundary to the destination API.
4. Audit all coupled surfaces: registration, events, networking, saved/synced
   state, client setup, resources/data generation, configuration, and packaging.
5. Validate on the destination branch, then compare the result against the
   observable contract rather than source-code similarity.

## Invariants and branch seams

- Keep gameplay state and validation server-authoritative. A client packet is a
  request, never proof that an action is legal.
- Never initialize client-only classes from common or dedicated-server paths.
- Preserve deterministic legacy-content loading, case-insensitive directive
  compatibility where already supported, registry/resource identities, save
  keys, and packet ordering unless migration is explicitly part of the task.
- Adapt loader lifecycle, events, registries, networking, metadata, mappings,
  and rendering architecture to the destination. Do not add a compatibility
  wrapper until the destination has been searched for its native equivalent.
- `master` uses Forge and `META-INF/mods.toml`; newer branches use NeoForge and
  generated metadata. Do not copy build or metadata files across this boundary.
- The 26.x branches use Mojang names and render-state extraction. `26.2` also
  uses feature rendering. Keep mutable entity/game access in extraction and
  render immutable state in the render phase.
- Do not overwrite unrelated branch-specific improvements to make files match.
  Compare semantics and merge only the required behavior.
- If the feature or its user-facing behavior is significant, inspect the sibling
  wiki repository and update it when documentation should change.

## Validation

Run focused tests first with the destination Gradle wrapper and required Java
version. Include a dedicated-server-safe check for client-sensitive changes and
exercise both packet directions for networking changes. Run a full `build` after
loader setup, registries, networking, entities, resources, source sets, or
packaging changes. Run `packsManagerJar` and/or `officialPacksJar` when their
inputs or packaging change. Finish with a scoped diff review and
`git diff --check`; report checks that could not be run.
