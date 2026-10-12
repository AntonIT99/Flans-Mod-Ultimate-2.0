# Feature Gap Analysis — Flan's Mod NPC Vehicles → NPC module

- **Reference:** `C:\Users\alpha\Documents\Minecraft-Development\Flans-Mod-NPC-Vehicles` (commit `22c0b0e4`, version 8.2.2, Forge 1.7.10, CustomNPC+ 1.11.1)
- **Target:** `../src/npcs` of Flan's Mod Ultimate 2.0 (`master` @ `692902545` plus the uncommitted driving, flight, submarine and diagnostics work in the working tree, Forge 1.20.1, Custom NPCs 1.20.1 GBPort), together with the public API in `src/main/java/com/flansmodultimate/api` that the module uses
- **Direction:** reference → target only. Target-only features are left out, such as item-driven equipment combat, paintjob browsing, the 64x32 renderer, inherited type properties, model armour, hull-assisted aiming and steering animation.
- **Scope note:** the reference's ~600 generated `Entity*`/`Model*` classes are per-model data. The target replaces them with `FlanModelEntities`, which registers a model for every loaded AA gun and driveable. That data-driven equivalent is not reported.

Unless noted, reference paths are relative to `../src/main/java/com/wolffsmod` and target paths to `src/npcs/java/com/wolffsmod/npcs/`.

**Status.** No finding is open. The first audit (reference `7999bb16`, version 7.1.0) listed 35 findings and the second (reference `22c0b0e4`, version 8.2.2) nine; all are closed or not applicable and no longer listed. They are documented in the wiki page `NPC-Vehicles-and-Soldiers.md`.

Closed does not mean verified: none of it has been run in a client yet. See [Areas requiring deeper audit](#areas-requiring-deeper-audit).

---

## Summary

| Subsystem | Feature | Status | Confidence |
| --------- | ------- | ------ | ---------- |
| — | — | — | — |

Totals: 0 MISSING, 0 PARTIAL, 0 UNCERTAIN.

Not applicable to the target:

- **Per-pack model toggles** (`ContentPacks.loadPacksConfig`): Flan's Mod Ultimate 2 already switches whole content packs on and off, and only loaded packs contribute NPC models.
- **Guard for modern TAP attachment rendering** (8.0.1): fixes a 1.7.10 content-pack incompatibility.
- **Stat import for Custom NPCs' own weapons:** Custom NPCs 1.20.1 ships no weapon items.
- **Block-read cache for path searches:** Minecraft 1.20.1 already caches block reads within a search.

Deliberate differences from the reference, which are not gaps:

- Every per-NPC setting lives in the Wolff's Mod tab, not in the Custom NPCs AI, display and stats pages.
- Frustum-culling exemptions are replaced by culling against the whole model's bounds.
- The ground driving controller is on by default and has fewer values than the reference's: steering rate, maximum steering angle and maximum turn rate are one hull turn rate, and the minimum following distance is half the following distance.
- A watercraft becomes a submarine by its running depth rather than by a separate type, and its minimum water depth is the Watercraft profile's.
- The path debug preview works for any NPC, not only for the driving controller.
- Server-wide target-search intervals are saved with the world and edited on the tab's Server page, not in a config file. Only the near and the far interval are set; the bands in between scale.
- Benchmarks time NPC ticks, path searches, target searches and NPC drawing as wholes. The reference's per-phase render and cache timings are not reproduced, because the target's render path is shared with real driveables and has its own diagnostics.

## Areas requiring deeper audit

- **In-game verification:** nothing listed in the wiki's Vehicle simulation and Movement and senses sections has been run in a client. Unit tests cover the arithmetic and the settings, and a headless server run shows the server-side mixins applied. Still to check in game: the editor pages and their paging, the direction of every rotation on real models, wheel, track, propeller and leg motion, mecha arm pitch, sight and fire through transparent blocks, and combat behaviour.
- **Driving controller on real terrain:** compare a target vehicle with a reference one on straight roads, 90° turns, S-curves, dead ends and one-block steps, for each preset, and two or more vehicles following the same target. The turning circle of wheeled vehicles and the backing-up recovery are the parts most likely to need tuning.
- **Flight types:** check that a plane's attack runs, its loiter circle and its climb over rising terrain work with Custom NPCs' flying movement, and that a helicopter hovers steadily.
- **Submarines:** check depth keeping over a sloping seabed and that the path search still leads a submerged NPC along the surface nodes above it.
- **Terrain profile mixin:** the node-evaluator hook behind the Ground and Watercraft profiles is not exercised by the headless run, because its class is not loaded before Custom NPCs' startup crash in that environment.
- **Watercraft footprint:** the reference requires water under a watercraft's whole footprint. The target filters path nodes by the depth at the node and relies on the path search's own footprint test; a wide ship next to a shore needs a look.
- **Benchmarks:** run both once on a busy scene and confirm that the reports appear under `logs/wolffsmodnpcs-benchmarks/` and that the NPC shares are plausible. The client benchmark counts shadow passes of shader packs as draws.
- **Server page permissions:** confirm on a dedicated server that a non-operator can read the page but not change it.
- **Large NPCs without a Flan model:** the reference could exempt any large NPC from frustum culling. In 1.20.1 the hitbox grows with the NPC's size, so this was not ported; confirm that very large humanoid NPCs do not vanish at the screen edge.
- **Reference notes after 8.0.5:** the flight, submarine and aircraft-turn commits have no release notes. They were read from source only, so intended behaviour beyond what the code shows may be missing.
