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

## Large cross-version release merges

When bringing a release from `master` into a maintained NeoForge branch, treat
the Git merge as a way to collect changes, not as the completed port:

1. Check both worktrees for local changes, record both heads and their merge
   base, and run a destination baseline compile. Preserve unrelated files.
2. Inventory source-only commits and changed paths by subsystem before merging.
   Count source and destination commits separately; previous merge commits can
   hide a large new release delta. Record a short port plan/report when the
   change set spans multiple systems.
3. Merge with `--no-commit` in the destination worktree. Group conflicts by
   build/metadata, common gameplay, networking, client/rendering, resources,
   and separately packaged packs. Resolve a subsystem and compile it before
   treating conflict resolution as finished.
4. For behavior, prefer the new `master` semantics. For integration, retain the
   destination's loader-specific implementation: Java/toolchain, Gradle plugin,
   source sets, metadata templates, events, registries, payload registration,
   Minecraft APIs, and rendering. Inspect the surrounding call chain whenever
   both branches edited the same file.
5. Audit cleanly auto-merged files too. A source-only Java file may merge without
   a conflict while still importing Forge or calling a removed Minecraft API.
   Search for source-loader imports and compile errors across the full result.
6. Keep built-in packs, official packs, and any branch-specific packaged packs
   separate. Moves of content definitions and large binary assets can defeat
   rename detection; validate final source-set membership and jar contents,
   rather than judging the raw diff size or copying generated artifacts.
7. Update the report with resolved seams, validation, and remaining runtime
   risks. Commit the merge only after the destination builds and the scoped diff
   has been checked, or explicitly record why a gate is unavailable.

### 1.20.1 Forge to 1.21.1 NeoForge findings

- A conflict resolution that retains the target side of a declaration block can
  leave source-only call sites in auto-merged code. Check each new referenced
  config value, resource location, registry entry, synced entity datum, menu
  page, and packet registration against its initializer and consumer. A clean
  Java compile is necessary but does not catch an unregistered packet or an
  omitted packaged module.
- NeoForge event-bus `post` returns the event. For cancellable gameplay events,
  test `.isCanceled()` on the returned event; a direct boolean check is a Forge
  API leftover. Use `ICancellableEvent` on custom cancellable event classes.
- Pass `HolderLookup.Provider` when serializing item stacks or their embedded
  magazines. Use the destination's `ItemStackData` boundary for custom data and
  stack persistence; 1.21 item stacks no longer expose the old mutable NBT tag
  methods. Entity synced data must be defined through
  `SynchedEntityData.Builder` in every subclass that adds accessors.
- 1.21 menu opening needs the complete buffer contract expected by the menu's
  network constructor. Recheck all fields when replacing `NetworkHooks` with
  `ServerPlayer.openMenu`. Screen background rendering also takes mouse
  coordinates and partial tick, and scroll callbacks take both axes.
- World data migration must use `Path`-based NBT IO with an `NbtAccounter` and
  `Path`-based replacement. Recipe conditions use NeoForge condition codecs and
  `neoforge:conditions` data. In 1.21.1, recipes live under `data/<namespace>/recipe/`,
  loot tables under `loot_table/`, and block tags under `tags/block/`. Crafting
  recipe outputs use `result.id` while ingredients still use `item` or `tag`.
  Biome modifiers live under `data/<namespace>/neoforge/biome_modifier/` and
  use `neoforge:add_features`. Audit both the main mod and packaged content
  packs; a successful Java compile does not validate these paths or schemas.
- Rendering is a separate porting phase. `VertexConsumer`, `BufferBuilder`,
  shaders, GUI layers, and post-processing changed enough that mechanical
  renames can compile partially while still producing the wrong frame. Verify
  the destination renderer API and test client startup and representative scenes.
  In 1.21.1 `fog_distance` takes `(vec3 position, int shape)`, so source shaders
  using the old model-view argument can compile as resources but fail during
  client reload. An optional OpenGL shader test can catch this before launch.
- Keep optional modules discoverable through their `src/<module>/fmu-module.gradle`
  descriptors when porting the root build. Under ModDevGradle, register each
  module source set, NeoForge mod binding, compile dependency on main, and jar
  task; convert Forge `mods.toml` and pack metadata in newly added modules.
  Inspect produced jars and check that all expected mod IDs load in a dev run.
- The packaged content module's logical recipe roots have no `pack.mcmeta`.
  Forge 1.20.1 supplies their pack metadata when constructing `Pack.Info`;
  NeoForge 1.21.1 must likewise construct `Pack.Metadata` for them rather than
  calling `Pack.readMetaAndCreate`. Otherwise the repository silently skips
  those packs. Check server logs for `Missing metadata in pack` and compare
  loaded recipe counts before and after a packaging port.
- NeoForge's `IConfigSpec.ILoadedConfig` is sealed. Tests that attached an
  anonymous Forge loaded config need a NeoForge test fixture or loader context;
  updating the import alone is insufficient.
- Compare every mixin class with the entries in `flansmodultimate.mixins.json`.
  Git can carry a mixin source file across a merge while leaving it unregistered,
  so compilation and startup still pass with the feature silently inactive.
  Restore the intended common and client roster, then run both a dedicated
  server and a client with mixins enabled. For 1.21.1, inspect transformed
  Minecraft bytecode or sources when an injection fails: player sneaking edge
  collision moved into `canFallAtLeast`, `MultiBufferSource.BufferSource` uses
  `startedBuilders`, the camera boom constant is a float, player renderer
  rotations gained a scale parameter, and world creation no longer ticks.
  Runtime startup does not load every screen or prove in-world behavior, so
  inspect selectors for later-loaded targets as well.
- NeoForge 1.21.1 runs official Minecraft names and ModDevGradle does not
  generate the Forge branch's refmap. Remove a stale `refmap` entry from the
  NeoForge mixin JSON after verifying both game sides with the full roster;
  keep Forge's refmap configuration on `master`.

This section supplements the smaller end-to-end workflow below; it does not
make the destination's branch-specific APIs subordinate to `master` files.

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
