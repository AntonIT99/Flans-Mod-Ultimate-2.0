# Feature Gap Analysis — Flan's Mod NPC Vehicles → NPC module

- **Reference:** `C:\Users\alpha\Documents\Minecraft-Development\Flans-Mod-NPC-Vehicles` (commit `22c0b0e4`, version 8.2.2, Forge 1.7.10, CustomNPC+ 1.11.1)
- **Target:** `../src/npcs` of Flan's Mod Ultimate 2.0 (`master` @ `263d1fe44` plus the uncommitted vehicle simulation, rendering and AI work in the working tree, Forge 1.20.1, Custom NPCs 1.20.1 GBPort), together with the public API in `src/main/java/com/flansmodultimate/api` that the module uses
- **Direction:** reference → target only. Target-only features are left out, such as item-driven equipment combat, paintjob browsing, the 64x32 renderer, inherited type properties, model armour, hull-assisted aiming and steering animation.
- **Scope note:** the reference's ~600 generated `Entity*`/`Model*` classes are per-model data. The target replaces them with `FlanModelEntities`, which registers a model for every loaded AA gun and driveable. That data-driven equivalent is not reported.

Unless noted, reference paths are relative to `../src/main/java/com/wolffsmod` and target paths to `src/npcs/java/com/wolffsmod/npcs/`.

**Status of the first audit.** The first audit, against reference commit `7999bb16` (version 7.1.0), listed 35 findings. 33 are closed and no longer listed: all of Vehicle simulation, Rendering, Combat, AI/navigation and Display/sounds. They are documented in the wiki page `NPC-Vehicles-and-Soldiers.md`. What a closed finding left open is listed below as a finding of its own. The reference has since gained ground driving, flight and submarine controllers (versions 8.0.0 to 8.2.2), which this revision adds.

None of the closed findings has been run in a client yet; see [Areas requiring deeper audit](#areas-requiring-deeper-audit).

---

## Vehicle NPC movement

### Ground vehicle driving controller — PARTIAL

Reference: `customnpc/GroundVehicleController`, `GroundVehicleState`, `VehicleSteeringController`, `VehiclePathFollower`, `GroundVehicleType`, `GroundVehiclePreset`, `SubGuiGroundVehicleSettings`, `mixin/MixinEntityMoveHelperGroundVehicle`, `MixinDataAI` (`VERSION_8_0_0_GROUND_VEHICLE_MOVEMENT.md`, 8.0.2, 8.0.4, 8.0.5)
An opt-in controller for NPCs with the Ground terrain profile turns an existing path into vehicle-like driving. It moves forward or in reverse along the hull, with smoothed steering, a minimum turn radius that widens with speed, and acceleration, braking and coasting. It looks ahead along the path, combines straight nodes, smooths corners and slows down before them. It stops at a configurable distance, reverses a short way when its goal is behind it, and backs up and retries when stuck. Wheeled, Tracked and Heavy Tracked types differ, tracked ones may pivot at low speed, and nearby ground vehicles keep a following distance. Presets exist for Car, Truck, Bus, Light Tank, Heavy Tank and APC; every value is editable per NPC.

Target checked: `sim/NpcVehicleState.beforeTravel`, `sim/MountMotion.throttle`, `sim/NpcTuning` (`HULL_TURN_RATE`, `THROTTLE_FLOOR`), `mixin/EntityNPCInterfaceMixin.wolffsmodnpcsDrive`
**Present:** a vehicle drives where its hull points, the hull turns toward the wanted direction at a limited rate, and the vehicle slows down while the two differ, down to turning on the spot when the floor is 0.
**Missing:** acceleration, braking and coasting; a minimum turn radius; path lookahead, corner smoothing and slowing before corners; stop distance; reversing; stuck recovery; vehicle types and presets; following distance between vehicles.

### Ground path debug preview — MISSING

Reference: `customnpc/GroundVehicleController` (path debug), `SubGuiGroundVehicleSettings` (`Path debug`) (`VERSION_8_0_3_PATH_DEBUG.md`)
A per-NPC switch shows the remaining navigation path, the current steering target and the path's end as particles sent by the server twice a second, capped at 48 markers, and logs the controller's state once a second.

Target checked: `../src/npcs` (no particle or path debug output)
Not present.

Missing: an in-world preview of an NPC's path and steering target.

### Plane and helicopter flight AI — MISSING

Reference: `customnpc/AircraftFlightController`, `AircraftFlightType`, `SubGuiAircraftSettings`, `mixin/MixinEntityNPCInterface` (`canAttack` gates, `update`)
Flying NPCs can be set to Plane or Helicopter instead of the normal flying movement. Both hold a minimum attack altitude above the terrain under them, climbing when too low, and hold their fire while they recover altitude without giving up their target. A plane never stops: it flies along its heading and turns at a rate derived from its speed and loiter radius, toward its target or round a loiter circle when wandering. A helicopter may hover and turns its hull toward its target at up to 3° per tick. Terrain is sampled on a staggered schedule, and a debug switch logs the flight state.

Target checked: `sim/NpcVehicleState` (planes turn like any NPC), `sim/NpcMobility`, `mixin/*` (no hook on the flying movement type)
Not present. Plane NPCs fly with Custom NPCs' own flying movement.

Missing: flight types, altitude keeping above terrain, the hold-fire rule while climbing, plane turning circles and helicopter hovering.

### Submarine depth control — PARTIAL

Reference: `customnpc/SubmarineController`, `WatercraftType`, `TerrainMobilityRules` (shallow-water avoidance), `SubGuiVehicleMobility` (`Depth below surface`, `Minimum submarine depth`), `mixin/MixinDataAI`
A Watercraft NPC can be a Surface Ship or a Submarine. A submarine holds a set depth below the water surface, limited by the depth of the water under it, never runs out of air, and avoids water shallower than its own minimum depth.

Target checked: `sim/NpcMobility` (`filter`, `watercraft`, `tick`), `sim/NpcTuning` (`MIN_WATER_DEPTH`)
**Present:** watercraft paths avoid water shallower than a minimum depth.
**Missing:** the submarine type, running at a depth below the surface (a target watercraft always floats), and air that does not run out.

---

## Combat and rendering

### Rounds passing the blocks an NPC fires through — PARTIAL

Reference: `customnpc/TransparentBlockLos.projectileOrigin`, `mixin/MixinEntityNPCInterface.shootFlanProjectile`
In See + Fire Through mode the round is launched from where the line of fire leaves the last transparent block, so leaves, glass or bars in front of the NPC cannot stop it.

Target checked: `sim/NpcSight`, `sim/NpcVehicleState.holdsFire`, `combat/NpcRangedAttack.Attack.fireMuzzle`
**Present:** the NPC sees through and decides to fire through the allowed number of transparent blocks.
**Missing:** the round still starts at the muzzle. Whether it gets through depends on how Flan's Mod Ultimate treats that block and ammunition, which was not checked.

### Mecha arms following the aim — PARTIAL

Reference: `model/ModelFlanMecha.renderLeftArm`/`renderRightArm` (arm pitch from the NPC's look pitch, within `UpperArmLimit`/`LowerArmLimit`), barrel pitch from the driver seat
A mecha's arms and barrel raise and lower with where the NPC looks.

Target checked: `../main/java/com/flansmod/client/model/ModelMecha` (arm pitch from the turret pitch), `sim/NpcVehicleState.aimTurret`
**Present:** arms are drawn at their origins, and legs walk.
**Missing:** the arms stay level. A model without muzzles on a turret keeps its turret pitch at zero, and a mecha has none.

---

## Diagnostics and configuration

### Server-wide target search tuning and profiling — PARTIAL

Reference: `config/TargetSearchConfig` (band intervals, `DebugTargetSearchProfiling`), `customnpc/TargetSearchProfiler`, `mixin/MixinEntityAIClosestTargetProgressive`
The intervals of each search band are set once for the whole server, and an optional profiler logs how many selector and line-of-sight checks each band needed and how long it took.

Target checked: `sim/NpcTargetSearch`, `sim/NpcTuning` (`SEARCH_NEAR_INTERVAL`, `SEARCH_FAR_INTERVAL`)
**Present:** banded search with a near and a far interval per NPC.
**Missing:** changing the intervals for every NPC at once, and the profiling counters.

### Client and server benchmarks — MISSING

Reference: `benchmark/*` (`/wolffbenchmark`, `/wolffserverbenchmark`, `VehicleBenchmark`, `ServerBenchmark`, `SlowPathLog`, reports under `logs/wolff-benchmarks/`), `mixin/MixinBenchmark*`
Measured client render runs and server tick runs, with warm-up, abort reasons, CSV and text reports, timings for render phases and caches, and per-search records of slow ground paths.

Target checked: `../src/npcs` (only `/flansnpcdistance`)
Not present.

Missing: NPC benchmarking commands and reports.

### Per-pack model toggles — PARTIAL

Reference: `ContentPacks.loadPacksConfig` (category `Content Packs`)
The config can switch off the NPC models of each content pack.

Target checked: `model/FlanModelEntities.registerEntityTypes` (registers every loaded AA gun and driveable), `mixin/NpcModelBrowserMixin` (browsing by pack)
**Present:** models are grouped by pack, and only installed packs contribute.
**Missing:** hiding an installed pack's models from the NPC model list without uninstalling the pack.

---

## Summary

| Subsystem | Feature | Status | Confidence |
| --------- | ------- | ------ | ---------- |
| Vehicle movement | Ground vehicle driving controller | PARTIAL | HIGH |
| Vehicle movement | Ground path debug preview | MISSING | HIGH |
| Vehicle movement | Plane and helicopter flight AI | MISSING | HIGH |
| Vehicle movement | Submarine depth control | PARTIAL | HIGH |
| Combat | Rounds passing the blocks an NPC fires through | PARTIAL | MEDIUM |
| Rendering | Mecha arms following the aim | PARTIAL | MEDIUM |
| Diagnostics | Server-wide target search tuning and profiling | PARTIAL | HIGH |
| Diagnostics | Client and server benchmarks | MISSING | HIGH |
| Configuration | Per-pack model toggles | PARTIAL | LOW |

Totals: 3 MISSING, 6 PARTIAL, 0 UNCERTAIN.

Not applicable to the target: the reference's guard for modern TAP attachment rendering (8.0.1) fixes a 1.7.10 content-pack incompatibility.

## Areas requiring deeper audit

- **In-game verification of the closed findings:** none of the 33 closed findings has been run in a client. Unit tests cover their arithmetic and settings, and a headless server run shows the mixins applied. Still to check in game: the editor pages, the direction of every rotation on real models, wheel, track, propeller and leg motion, hull steering along real paths, terrain profiles, sight through transparent blocks, and combat behaviour.
- **Terrain profile mixin:** the node-evaluator hook behind the Ground and Watercraft profiles was not exercised by the headless run, because its class is not loaded before Custom NPCs' startup crash in that environment.
- **Rounds through transparent blocks:** check which blocks Flan's Mod Ultimate bullets already pass or break before deciding whether the launch point has to move.
- **Hull steering against the reference's controller:** compare a target vehicle with a reference one on straight roads, 90° turns, S-curves and dead ends, to see which of the missing behaviours (reversing, corner slowdown, stuck recovery) matter most.
- **Watercraft footprint:** the reference requires water under a watercraft's whole footprint. The target filters path nodes by the depth at the node and relies on the path search's own footprint test; a wide ship next to a shore needs a look.
- **Rendering performance:** animated models now run the driveable model code, level of detail and caches for every NPC vehicle. Recheck frame times with many moving turrets, wheels and tracks.
- **Large NPCs without a Flan model:** the reference could exempt any large NPC from frustum culling. In 1.20.1 the hitbox grows with the NPC's size, so this was not ported; confirm that very large humanoid NPCs do not vanish at the screen edge.
- **Reference notes after 8.0.5:** the flight, submarine and aircraft-turn commits have no release notes. They were read from source only, so intended behaviour beyond what the code shows may be missing here.
