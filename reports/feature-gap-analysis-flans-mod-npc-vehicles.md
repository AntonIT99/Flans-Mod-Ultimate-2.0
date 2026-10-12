# Feature Gap Analysis — Flan's Mod NPC Vehicles → NPC module

- **Reference:** `C:\Users\alpha\Documents\Minecraft-Development\Flans-Mod-NPC-Vehicles` (commit `22c0b0e4`, version 8.2.2, Forge 1.7.10, CustomNPC+ 1.11.1)
- **Target:** `src/npcs` of Flan's Mod Ultimate 2.0 (`master` @ `ac009a9f1`, Forge 1.20.1, Custom NPCs 1.20.1 GBPort), together with the public API in `src/main/java/com/flansmodultimate/api` that the module uses
- **Direction:** reference → target only. Target-only features are left out, such as item-driven equipment combat, paintjob browsing, the 64x32 renderer, inherited type properties, model armour, hull-assisted aiming and steering animation.
- **Scope note:** the reference's ~600 generated `Entity*`/`Model*` classes are per-model data. The target replaces them with `FlanModelEntities`, which registers a model for every loaded AA gun and driveable. That data-driven equivalent is not reported.

Reference paths are relative to `src/main/java/com/wolffsmod` in the reference repository, and target paths to `src/npcs/java/com/wolffsmod/npcs/`. "Custom NPCs" below is the 1.20.1 jar in `libs/`, read from the `decompiled/` cache. Vanilla behaviour was read from the Forge 1.20.1 and 1.7.10 source jars.

**Status.** No finding is open. The third audit (2026-10-12) went through the areas the second audit had left for a deeper look and listed seven PARTIAL findings; all seven were assessed as real gaps and closed the same day, see [Closed after the third audit](#closed-after-the-third-audit). The first audit (reference `7999bb16`, version 7.1.0) listed 35 findings and the second (reference `22c0b0e4`, version 8.2.2) nine; those are closed or not applicable and no longer listed. They are documented in the wiki page `NPC-Vehicles-and-Soldiers.md`.

The third audit read code and logs, and its fixes were checked by unit tests and by a server start that applies the mixins. Nothing was driven in a client, so everything under [Areas requiring deeper audit](#areas-requiring-deeper-audit) still needs one.

---

## Closed after the third audit

| Subsystem | Feature | What the target does now |
| --------- | ------- | ------------------------ |
| Movement: ground driving | Braking for the end of the path | `sim/NpcDriving.arrival`: the throttle is limited to what braking can still shed before the point where the path ends (the stop distance, or the navigator's own reach for a wide NPC), down to a crawl of 20 %. A vehicle that was slowing for its goal brakes when it gets there. |
| Movement: ground driving | Turning circle of a wheeled vehicle | The turn limit is given the distance the NPC actually moved (`NpcVehicleState.getTravel`), not its motion after ground friction, so the circle has the radius that is set. |
| Movement: terrain profiles | Water depth under the whole footprint | `sim/NpcMobility.depth` measures every column of the footprint the path search uses: the deepest for Ground, the shallowest for Watercraft. |
| Movement: submarines | Following a path at running depth | `mixin/NpcGroundPathNavigatorMixin.wolffsmodnpcsPassBelowNodes`: a submerged submarine reaches a path node by passing below it. |
| Combat: aircraft | Holding fire below the attack altitude | The hold also applies in `combat/NpcEquipmentRangedGoal`, and at `performRangedAttack` and `doHurtTarget` in `mixin/EntityNPCInterfaceMixin`, which covers scripts and melee. |
| Diagnostics | Path searches of flying NPCs in the server benchmark | `mixin/PathNavigationMixin` times the search itself in every navigator an NPC uses, and failed searches are counted (`path_failures` in the CSV). A call that keeps its path is no longer counted as a search. |
| Rendering | Large NPC without a content-pack model and without a hitbox | `sim/NpcModels.unhitCullingBox`: such an NPC is culled by the width its hitbox would have. |

Two parts of those findings were judged legitimate differences and left as they are; they are listed under the deliberate differences below.

---

## Summary

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
- A vehicle whose path is taken away while it is at speed, for instance by the ranged AI once its target is in range, rolls out at its coasting rate. The reference brakes there, because its coasting rate is never reached; the target has coasting as a setting of its own. Only a vehicle that was already slowing for the end of its path brakes.
- A submarine has no minimum water depth of its own beyond the Watercraft profile's 16 blocks. The target does not need one: over water shallower than its running depth it rides higher instead of refusing the route.

## Settled by the third audit

These were on the deeper-audit list and need no further work, short of the client checks listed below.

- **Terrain profile mixin.** It applies. The dev-client log of 2026-10-12 02:49 (`run/logs/latest.log`) shows `NpcWalkNodeEvaluatorMixin` mixed into `noppes.npcs.ai.NpcWalkNodeEvaluator` when the world started, with no injection error, and the transformed class was exported to `run/.mixin.out`. The production name it targets, `m_77567_`, is declared by that class in the Custom NPCs jar. The same log shows 18 of the module's 20 mixins applied; `NpcArmorControlsMixin` and `NpcRangedControlsMixin` were not loaded because their screens were not opened.
- **Watercraft footprint.** Shores were handled by the path search itself; depths beyond 1 are measured under the whole footprint since the fix above.
- **Server page permissions.** `network/ServerControlPacket.handle` answers a read for anyone, and requires permission level 2 or the singleplayer owner for a change or a benchmark; a refused player gets the current values back. The packet is handled on the server thread. Not exercised on a dedicated server.
- **Benchmark report location.** `diag/BenchmarkReport.write` writes a `.txt` and a `.csv` to `logs/wolffsmodnpcs-benchmarks/` under the game directory. No benchmark has been run; that folder does not exist yet in `run/`.
- **Large NPCs.** The hitbox grows with the NPC's size in both `m_6972_` implementations, so the remaining case was the NPC without a hitbox, closed above.
- **Flight arithmetic.** Climb and descent limits, the attack-altitude hysteresis, the loiter circle, the plane's turn limit and the helicopter's turn rate match the reference value for value. Flying NPCs have no gravity in Custom NPCs 1.20.1 (`EntityNPCFlying.m_7023_`), so an idle helicopter's vertical speed decays to a hover.
- **Unit tests.** `com.wolffsmod.npcs.sim.*` and `com.wolffsmod.npcs.diag.*` pass at `ac009a9f1`.

### Reference notes after 8.0.5

The five commits without release notes (`5efc6bdc`, `71af6e1f`, `7bdf6798`, `0e4fda39`, `22c0b0e4`) were read in full, including their editor screens and saved values. They hold nothing beyond what the target has or what is reported above. Four things in them do not work as their names suggest, which matters when a target vehicle is compared with a reference one:

- **Submarines do not dive or path in the reference.** `SubmarineController` and `TerrainMobilityRules.hasWaterColumnDepth` find the water surface with `World.getTopSolidOrLiquidBlock`. In 1.7.10 that method skips liquids and returns the block above the sea floor, so the block below it is never water: the controller never engages and every path node of a submarine is rejected. The target follows what the feature was meant to do, so there is no working reference to compare it with.
- **Reference aircraft measure their height from the sea floor** over water, for the same reason, and are pushed down when more than 5 blocks above the height they aim for. The target measures from the water surface, and counts leaves as terrain.
- **The reference never coasts.** `advanceSpeed` picks the coasting rate only when the wanted and the current speed are both zero.
- **A reference plane without a target circles its home only while the NPC is set to wander.** Otherwise it flies straight on. A target plane always circles.

The last commit also sends the hull direction to clients while a vehicle moves, not only while it has a target. The target sends it whenever it changes.

## Areas requiring deeper audit

All of these need a running client; none could be done in this audit.

- **In-game verification:** nothing listed in the wiki's Vehicle simulation and Movement and senses sections has been checked on screen. Still to check: the editor pages and their paging, including the armour and ranged pages whose mixins no logged run has loaded; the direction of every rotation on real models; wheel, track, propeller and leg motion; mecha arm pitch; sight and fire through transparent blocks; and combat behaviour.
- **Driving controller on real terrain:** compare a target vehicle with a reference one on straight roads, 90° turns, S-curves, dead ends and one-block steps, for each preset. Look first at the halt at the end of a path and at the turning circle of a wheeled vehicle: both were corrected by arithmetic and unit tests, not measured in game, and the turning circle is now about 1.8 times tighter than before. The target also steers for a fixed distance ahead, where the reference shortens it at low speed and lengthens it at high speed. Two vehicles that meet head-on stop for each other and stay stopped, in the reference as well.
- **Flight types:** watch a plane's attack runs and its loiter circle, and a helicopter's hover. Custom NPCs' flying movement pushes the NPC toward its own waypoint every fifth tick, which the plane's heading overrides sideways but not vertically. A plane given a set flight speed still looks only 24 blocks ahead for terrain: at 20 blocks per second it climbs about 6 blocks in that distance, on top of the 5 blocks it keeps above its attack altitude.
- **Submarines:** confirm that a submarine now travels its paths at depths 1, 2 and 4 and deeper, and check depth keeping over a sloping seabed. With swimming allowed, Custom NPCs' swim goal pushes the NPC up each tick, which by arithmetic leaves it up to half a block above its running depth.
- **Terrain profiles of wide NPCs:** a Watercraft now needs its minimum depth under its whole hull, so check that a wide ship still finds its way through the channels it used before.
- **Benchmarks:** run both once on a busy scene and confirm that the reports appear and that the NPC shares are plausible. The client benchmark counts shadow passes of shader packs as draws.
- **Server page on a dedicated server:** one pass as a non-operator, to see the refusal message and the unchanged values.
