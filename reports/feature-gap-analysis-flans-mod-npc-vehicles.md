# Feature Gap Analysis — Flan's Mod NPC Vehicles → NPC module

- **Reference:** `C:\Users\alpha\Documents\Minecraft-Development\Flans-Mod-NPC-Vehicles` (commit `22c0b0e4`, version 8.2.2, Forge 1.7.10, CustomNPC+ 1.11.1)
- **Target:** `src/npcs` of Flan's Mod Ultimate 2.0 (`master` @ `ac009a9f1`, Forge 1.20.1, Custom NPCs 1.20.1 GBPort), together with the public API in `src/main/java/com/flansmodultimate/api` that the module uses
- **Direction:** reference → target only. Target-only features are left out, such as item-driven equipment combat, paintjob browsing, the 64x32 renderer, inherited type properties, model armour, hull-assisted aiming and steering animation.
- **Scope note:** the reference's ~600 generated `Entity*`/`Model*` classes are per-model data. The target replaces them with `FlanModelEntities`, which registers a model for every loaded AA gun and driveable. That data-driven equivalent is not reported.

Reference paths are relative to `src/main/java/com/wolffsmod` in the reference repository, and target paths to `src/npcs/java/com/wolffsmod/npcs/`. "Custom NPCs" below is the 1.20.1 jar in `libs/`, read from the `decompiled/` cache. Vanilla behaviour was read from the Forge 1.20.1 and 1.7.10 source jars.

**Status.** Seven findings are open, all PARTIAL. They come from the third audit (2026-10-12), which went through the areas the second audit had left for a deeper look. The first audit (reference `7999bb16`, version 7.1.0) listed 35 findings and the second (reference `22c0b0e4`, version 8.2.2) nine; those are closed or not applicable and no longer listed. They are documented in the wiki page `NPC-Vehicles-and-Soldiers.md`.

The third audit read code and logs. It did not drive a client, so everything under [Areas requiring deeper audit](#areas-requiring-deeper-audit) still needs one. The seven findings are derived from the code and from arithmetic; none was reproduced in game.

---

## Movement: ground driving

### Braking for the end of the path — PARTIAL

Reference: `customnpc/GroundVehicleController.update`, `advanceSpeed`
Within three stop distances of the end of its path, the wanted speed falls in a straight line to zero at the stop distance. The braking rate applies whenever the wanted speed is below the current one, so a vehicle whose path ends or is taken away brakes as well. The coasting rate is never reached in practice.

Target checked: `sim/NpcDriving.drive`, `stopsShort`, `idle`, `approach`; `NpcTuning.Value.STOP_DISTANCE`, `DRIVE_COASTING`
`drive` slows the vehicle for a hull that points away, for corners and for traffic, but not for the distance to its goal. At the stop distance (0 by default, which is the last node) the path is dropped and `idle` sheds throttle at the coasting rate, 60 % per second by default.

Missing: slowing down ahead of the goal, and braking rather than coasting once it is reached. By arithmetic, a vehicle at full throttle rolls on for about 16 ticks' worth of its top speed: roughly 2 blocks at Custom NPCs walking speed 5, and 7 blocks at a land speed of 9 blocks per second. The same applies when the ranged AI stops the NPC because it is in range.

### Turning circle of a wheeled vehicle — PARTIAL

Reference: `customnpc/VehicleSteeringController.steer`
The turn rate is limited to speed divided by the minimum turn radius, using the controller's own speed. The radius triples at top speed.

Target checked: `sim/NpcDriving.drive` (`Situation.speed`), `NpcDriving.travel`; vanilla `LivingEntity.travel`
The same limit exists, and the radius doubles at full throttle. The speed it is given is `npc.getDeltaMovement().horizontalDistance()`, read before the NPC moves. At that moment it holds the last tick's movement already multiplied by ground friction, which is 0.6 × 0.91 = 0.546 on ordinary blocks. `NpcDrivingTest` passes true speeds, so it does not see this. `NpcVehicleState.getTravel()` already holds the distance actually moved.

Missing: a turning circle of the radius that is set. On ordinary ground the circle is about 1.8 times wider, and reaches about 3.7 times the set radius at full throttle.

## Movement: terrain profiles

### Water depth under the whole footprint — PARTIAL

Reference: `customnpc/TerrainMobilityRules.filterPathNode`
Every column under the NPC's footprint is tested. Ground rejects a node when any column is deeper than the wade depth. Watercraft rejects it unless every column has the minimum depth.

Target checked: `mixin/NpcWalkNodeEvaluatorMixin`, `sim/NpcMobility.filter`, `depth`; vanilla `WalkNodeEvaluator.getBlockPathType(BlockGetter, int, int, int, Mob)`
The hook filters the answer of the path search's own footprint test. That test merges the kinds of ground under the footprint into one, and a mix of water and walkable ground comes out as water. `depth` then measures only the column at the node's own corner, the one with the lowest x and z.

Missing: the depth test on the other columns, for NPCs wider than one block. A Watercraft node passes with water shallower than the minimum depth under its east or south part, and a Ground node passes with water deeper than the wade depth there. With the default minimum depth of 1 a Watercraft is not affected: a solid shore block at the waterline rejects the node through the footprint test itself, which answers the earlier question about a wide ship next to a shore.

## Movement: submarines

### Following a path at running depth — PARTIAL

Reference: `customnpc/SubmarineController`, `TerrainMobilityRules.hasWaterColumnDepth`, `SubGuiVehicleMobility` ("Depth below surface", default 4); vanilla 1.7.10 `PathNavigate.getEntityPosition`
A submarine is meant to travel its paths while it holds its depth. In 1.7.10 a swimming entity follows its path by the height of the water surface above it, so depth does not matter there. The reference's own controller never engages, see [Reference notes](#reference-notes-after-805).

Target checked: `sim/NpcSubmarine`; Custom NPCs `NpcGroundPathNavigator.m_7636_` (its path following); vanilla `WalkNodeEvaluator.getStart`, `GroundPathNavigation.getSurfaceY`, `MoveControl.tick`
Depth keeping is there: the NPC holds its running depth, rides higher over shallows and keeps its air. The path of a floating NPC is laid along the surface. Custom NPCs' navigator moves on to the next node only while the NPC's feet are less than one block above or below it, and it does not take its shortcut across water.

Missing: travel at a running depth of 3 blocks or more. By the code the NPC never gets past the first node of its path: it stays under it until the path times out, then starts again. A depth of 1 works and a depth of 2 is on the edge. Separately, the minimum water depth is shared with surface ships and ends at 16 blocks, where the reference has a submarine value of its own from 2 to 64 (default 8), so a submarine running deeper than 15 blocks cannot ask for water deeper than itself.

## Combat: aircraft

### Holding fire below the attack altitude — PARTIAL

Reference: `customnpc/AircraftFlightController.canAttack`, called from `mixin/MixinEntityNPCInterface.attackEntityAsMob` and `attackEntityWithRangedAttack`
An aircraft below its attack altitude makes no attack of any kind, melee or ranged, whatever triggers it.

Target checked: `sim/NpcFlight.holdsFire`, `mixin/EntityAIRangedAttackMixin.wolffsmodnpcsHoldFire`, `combat/NpcEquipmentRangedGoal`, `mixin/EntityNPCInterfaceMixin.wolffsmodnpcsFireFlanRound`, `wolffsmodnpcsItemMelee`
The hold is applied in one place, Custom NPCs' fallback ranged AI. That covers a plane or helicopter model firing its own guns.

Missing: the hold for a flying NPC that fights with held items (`NpcEquipmentRangedGoal`), for melee hits, and for ranged attacks triggered outside the fallback AI, such as scripts.

## Diagnostics

### Path searches of flying NPCs in the server benchmark — PARTIAL

Reference: `mixin/MixinBenchmarkFlyPathFinder`, `mixin/MixinBenchmarkPathFinder`, `benchmark/ServerBenchmark.endPath`
Ground and flying path searches are counted and timed separately, and searches that return no path are counted.

Target checked: `mixin/NpcGroundPathNavigatorMixin.createPath`, `diag/NpcProfiler.pathSearch`, `diag/ServerBenchmark`
Only searches of `NpcGroundPathNavigator` are timed. Flying NPCs use vanilla's `FlyingPathNavigation` and swimming ones `WaterBoundPathNavigation`, which the hook does not reach. Whether a search found a path is recorded for slow searches only.

Missing: path searches of flying and swimming NPCs in the benchmark's totals, and a count of failed searches. A scene of aircraft reports no path searches at all.

## Rendering

### Large NPC without a content-pack model and without a hitbox — PARTIAL

Reference: `mixin/MixinEntityCustomNpc.onUpdateHitbox` (`ignoreFrustumCheckForLargeEntities`, `largeEntitySize`)
Any NPC whose height or width reaches the configured size is drawn without a frustum check. Its height stays large when its hitbox is switched off, so it stays exempt.

Target checked: `mixin/EntityNPCInterfaceMixin.getBoundingBoxForCulling`, `sim/NpcVehicleState.cullingBox`; Custom NPCs `EntityNPCInterface.m_6972_`, `EntityCustomNpc.m_6972_`; vanilla `EntityRenderer.shouldRender`
The culling box is widened for content-pack models only. For every other NPC the hitbox does grow with its size, which settles the usual case. With the hitbox switched off, or for a killed NPC whose body is hidden, Custom NPCs makes the hitbox 0.00001 blocks wide, and vanilla culls by that box plus half a block.

Missing: an exemption or a wider box for a large NPC without a content-pack model whose hitbox is off. It is culled as soon as a column one block wide through its middle leaves the view, although a size-30 humanoid is several blocks wide.

---

## Summary

| Subsystem | Feature | Status | Confidence |
| --------- | ------- | ------ | ---------- |
| Movement: ground driving | Braking for the end of the path | PARTIAL | MEDIUM |
| Movement: ground driving | Turning circle of a wheeled vehicle | PARTIAL | MEDIUM |
| Movement: terrain profiles | Water depth under the whole footprint | PARTIAL | HIGH |
| Movement: submarines | Following a path at running depth | PARTIAL | MEDIUM |
| Combat: aircraft | Holding fire below the attack altitude | PARTIAL | HIGH |
| Diagnostics | Path searches of flying NPCs in the server benchmark | PARTIAL | HIGH |
| Rendering | Large NPC without a content-pack model and without a hitbox | PARTIAL | MEDIUM |

Totals: 0 MISSING, 7 PARTIAL, 0 UNCERTAIN.

Confidence is HIGH where the code alone decides the matter, and MEDIUM where the mechanism is certain from the code but its size in play was worked out rather than seen.

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

## Settled by the third audit

These were on the deeper-audit list and need no further work, short of the client checks listed below.

- **Terrain profile mixin.** It applies. The dev-client log of 2026-10-12 02:49 (`run/logs/latest.log`) shows `NpcWalkNodeEvaluatorMixin` mixed into `noppes.npcs.ai.NpcWalkNodeEvaluator` when the world started, with no injection error, and the transformed class was exported to `run/.mixin.out`. The production name it targets, `m_77567_`, is declared by that class in the Custom NPCs jar. The same log shows 18 of the module's 20 mixins applied; `NpcArmorControlsMixin` and `NpcRangedControlsMixin` were not loaded because their screens were not opened.
- **Watercraft footprint.** Answered by the finding above: shores are handled, depths beyond 1 are not.
- **Server page permissions.** `network/ServerControlPacket.handle` answers a read for anyone, and requires permission level 2 or the singleplayer owner for a change or a benchmark; a refused player gets the current values back. The packet is handled on the server thread. Not exercised on a dedicated server.
- **Benchmark report location.** `diag/BenchmarkReport.write` writes a `.txt` and a `.csv` to `logs/wolffsmodnpcs-benchmarks/` under the game directory. No benchmark has been run; that folder does not exist yet in `run/`.
- **Large NPCs.** The hitbox grows with the NPC's size in both `m_6972_` implementations, so the remaining case is the one in the finding above.
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
- **Driving controller on real terrain:** compare a target vehicle with a reference one on straight roads, 90° turns, S-curves, dead ends and one-block steps, for each preset. Look first at the two driving findings, the overshoot at the goal and the turning circle, since both were worked out rather than measured. The target also steers for a fixed distance ahead, where the reference shortens it at low speed and lengthens it at high speed. Two vehicles that meet head-on stop for each other and stay stopped, in the reference as well.
- **Flight types:** watch a plane's attack runs and its loiter circle, and a helicopter's hover. Custom NPCs' flying movement pushes the NPC toward its own waypoint every fifth tick, which the plane's heading overrides sideways but not vertically. A plane given a set flight speed still looks only 24 blocks ahead for terrain: at 20 blocks per second it climbs about 6 blocks in that distance, on top of the 5 blocks it keeps above its attack altitude.
- **Submarines:** confirm the path-following finding at depths 1, 2 and 4, and check depth keeping over a sloping seabed. With swimming allowed, Custom NPCs' swim goal pushes the NPC up each tick, which by arithmetic leaves it up to half a block above its running depth.
- **Benchmarks:** run both once on a busy scene and confirm that the reports appear and that the NPC shares are plausible. The client benchmark counts shadow passes of shader packs as draws.
- **Server page on a dedicated server:** one pass as a non-operator, to see the refusal message and the unchanged values.
