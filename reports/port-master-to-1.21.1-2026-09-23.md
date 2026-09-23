# Master 2.0 release to NeoForge 1.21.1 port

## Baseline and strategy

- Source release: `master` at `6fc95a904` (Minecraft 1.20.1, Forge 47.4.x).
  Destination: `1.21.1` at `6b7b5bcd9` (Minecraft 1.21.1, NeoForge 21.1.248).
  Common ancestor: `6fb38574f`; 280 source-only and 40 destination-only commits.
- The destination passed `compileJava` before the merge. The source worktree had
  an unrelated untracked Python `__pycache__` directory; it was preserved.
- Strategy: merge with `--no-commit`, group conflicts by loader/build, common
  gameplay, networking, client rendering, apocalypse, and resources/packs.
  Keep the release behavior from `master` and express it through the target's
  NeoForge lifecycle and Minecraft 1.21.1 APIs. Compile and test each layer,
  then inspect packaged jars and run both game sides.

## Port decisions

- Resolved 72 initial merge conflicts. Kept ModDevGradle, Java 21, NeoForge
  metadata, payload registration, and target-specific rendering. Restored
  source additions to configuration, registries, driveables, apocalypse,
  menus, entities, input, and networking. The target packet bridge contains
  all 62 packet types registered by the source release; its protocol is `8`.
- Adapted item stack serialization, synced entity data, custom recipe
  conditions, resource packs, GUI callbacks, fluid capabilities, events, and
  render buffers to 1.21.1. Updated tests for NeoForge's sealed loaded-config
  interface and chained vertex consumer API.
- Kept pack modules discoverable from their `fmu-module.gradle` descriptors.
  The build now produces Classic, Manus, Warfare 44, and Wolff's Star Wars
  modules plus the Packs Manager. Their source sets bind to NeoForge dev runs.
  Ported Forge-only mod entrypoints and metadata for the new modules. Retained
  the optional encrypted-content packaging task from `master`. Removed the
  obsolete empty target-only `packsJar` module.
- Moved 343 recipe JSONs from `recipes/` to `recipe/`; changed 340 crafting
  output keys from `result.item` to `result.id`. Moved main loot tables to
  `loot_table/`, block tags to `tags/block/`, and the apocalypse biome modifier
  to the NeoForge path/type. Changed the gunpowder recipe to use
  `neoforge:conditions`.
- Client startup exposed an old `fog_distance` call in the rigid model shader.
  Updated it to the 1.21.1 signature and removed the unused uniform.
- Updated the authoritative `master` porting skill in three commits:
  `b82b856f`, `0f153f865`, and `3e605aa1`. The destination merge is
  `09b604ea`; the final skill sync is `cfef8d5da`.

## Validation

- `compileJava` passed after the semantic port.
- `test` passed: 852 tests, 3 skipped. The optional OpenGL shader test also
  passed with `FLANS_GPU_TEST=true`.
- `clean build` passed with all pack jars, including `packsManagerJar` and
  `officialPacksJar`. Each produced jar has `neoforge.mods.toml`; the main jar
  carries version `2.0` and the four optional pack modules are separate.
- The dedicated server loaded the mod and pack modules and reached `Done`.
  It was stopped after startup, so the Gradle `runServer` process exited with
  status 1 from termination, not from a startup crash.
- The client reached resource reload with all modules. The first run showed
  the shader error above; after the fix, the second `runClient` finished
  successfully. Existing local content packs generated duplicate-ID and
  invalid-definition warnings on both sides.
- The 1.21.1 wiki was inspected. No page advertises the unreleased 1.21.1
  build yet, so the user-facing wiki should be updated when that version is
  actually published.

## Remaining release checks

- Exercise joining a world, firing and reloading, driving and using vehicle
  inventory, optics/thermal rendering, apocalypse world selection, and both
  packet directions with a real client/server session. Startup checks cannot
  establish these gameplay behaviors.
- Check optional encrypted-content packaging with a real private module input.
  No plaintext input is present in this worktree, so the task is registered
  conditionally and its output was not exercised here.
- The source release has existing trailing whitespace in several staged docs
  and Java files, plus many content definitions. Before committing, the
  destination's changes relative to `master` passed `git diff --check` with
  no whitespace errors; the full inherited source diff retained its existing
  whitespace. Leave content-pack definitions byte-stable.
