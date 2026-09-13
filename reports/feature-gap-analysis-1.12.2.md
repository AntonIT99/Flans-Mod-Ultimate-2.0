# Feature gap analysis: Flan's Mod 5.10.0 (1.12.2) → Ultimate 2.0

Initial audit: 2026-09-12. Last updated: 2026-09-13. Direction is strictly reference → target.

- Reference: `C:/Users/alpha/Documents/Minecraft-Development/FlansMod`, Flan's Mod 5.10.0 / MC 1.12.2, HEAD `71ba7ed065d906d48f34ca471bbd0172b5192f6b`.
- Target: `C:/Users/alpha/Documents/Minecraft-Development/Flans-Mod-Ultimate-2.0`, Forge 1.20.1, HEAD `c7af2b0fb3cb85711796129fec789c51e96b999e` (clean tree).
- Remaining: **1 MISSING, 0 PARTIAL, 0 UNCERTAIN**. These counts describe the findings below, not a percentage of port completeness.
- Completed findings are removed as they are implemented, so this stays a backlog rather than a historical snapshot. The initial audit found 7 MISSING and 4 PARTIAL; the six Apocalypse findings and the four Teams findings were implemented on 2026-09-13.

## Scope and evidence conventions

This is a static, reference-driven semantic audit. Discovery walked the reference subsystem by subsystem: content loading and type definitions, handheld guns/ammunition/attachments/grenades, shooting, raytracing and penetration, deployed MGs and AA guns, driveables (planes, vehicles, mechas, seats, wheels, collisions, fuel, repair), tools and armor, enchantments, paintjobs, gun/armor boxes, workbenches and item holders, teams (gametypes, maps, rounds, rotations, bases, spawners, flags, loadout pools, ranks, reward boxes, op sticks, commands), client input/HUD/camera/rendering, networking, sounds and particles, world/loot injection, and the bundled sub-mods (apocalypse, and the pack mods).

For each candidate the reference call sites and data flow were traced, then the target was searched for direct, renamed, componentized, data-driven or newer-mechanism equivalents (Lombok accessors, mixins, datapacks, capability APIs and config snapshots included). Commented-out reference implementations (`MovingSoundDriveable`, `CommonProxy.playBlockBreakSound`, spawner chunk-loading) and reference-side dead fields were excluded. Definition-parameter parity is out of scope here — it is covered by `reports/infotype-parameter-gap-analysis-1.12.2.md`.

Evidence paths use these roots:

- `R/` = reference `src/main/java/com/flansmod`.
- `T/` = target `src/main/java/com/flansmodultimate`.
- Resource paths explicitly identify their repository.

`MISSING` means the described capability has no equivalent in the inspected target paths. `PARTIAL` means the broader mechanic exists but the stated behavior does not. Confidence concerns the narrow finding, not full subsystem equivalence. The audit itself changed nothing; the Apocalypse and Teams findings were implemented separately afterwards and removed from this backlog.

Large, well-ported areas that produced no findings are deliberately not enumerated. Notable examples verified as present: mecha upgrade behaviors (including diamond detection, auto-repair, ore multipliers, rocket pack, item vacuum, waste compaction, forced light level), grenade behaviors (proximity triggers, stickiness, deployable bags, smoke/potion effects, heal amounts), AA gun and deployed-MG mechanics, CTF flag handling, smart weapon drops with ammo consolidation, team spawner vehicle/item spawning, dungeon-loot injection, creative paintjob variants, gun attribute modifiers, flashlight attachments, and the Mecha Parts pack contents (folded into the target's Titan pack).

## Driveables

### Fluid-bucket refueling — MISSING

Reference: `R/common/FlansHooks.java`, `R/common/driveables/EntityDriveable.java:1210-1224`.
When BuildCraft Energy is present, an oil bucket in a driveable's fuel slot adds 1000 × fuel multiplier and a fuel bucket adds 2000 × fuel multiplier to the tank, leaving an empty bucket behind.

Target checked: `T/common/entity/Driveable.java:3398` (fuel consumption accepts only `PartType.Category.FUEL` part items) and `:3428` plus `T/common/driveables/DriveableData.java:280` (a Forge Energy path for RF-capable items), `T/common/inventory/DriveableInventoryMenu.java`, `T/client/gui/DriveableInventoryScreen.java` fuel page.
The target refuels from fuel parts and charges from `ForgeCapabilities.ENERGY` items. There is no bucket or fluid handling of any kind — no `FluidUtil`, `IFluidHandler` or bucket-item branch — so no liquid fuel can be poured into a tank.

Missing: Refueling a driveable from a liquid-fuel bucket. (BuildCraft itself has no 1.20.1 counterpart; the equivalent modern path would be a Forge fluid-capability branch alongside the existing energy branch.)

## Summary

| Subsystem | Feature | Status | Confidence |
| --------- | ------- | ------ | ---------- |
| Driveables | Fluid-bucket refueling | MISSING | HIGH |

## Areas requiring deeper audit

- **Apocalypse structure interiors.** The target rebuilds the reference's `common/world/buildings/*` generators procedurally rather than porting them. The research lab and the village were compared closely; the dye factory, runway, boss pillar, dead tree, skeleton display and abandoned-portal generators were read at call-site level only, so smaller content differences (block palettes, loot placement, secondary rooms, spawned props) may remain inside each.
- **Apocalypse mob AI.** `SurvivorEntity`, `SkullBossEntity` and `SkullDroneEntity` exist in the target, but their goal sets were not diffed against `EntitySurvivor`, `EntitySkullBoss` and `EntitySkullDrone`, nor against `EntityAIGoSomewhere`. `EntitySkuller` was excluded because it is never registered in the reference.
- **Apocalypse terrain shape.** The implemented dimension keeps the vanilla overworld noise router and supplies its own biomes, climate placement and surface rules on top. That reproduces the reference's biome set, its canyon-low/plateau-high ordering and its red-sand wasteland surface, but not the 1.12.2 chunk provider's exact per-biome base heights, which have no direct equivalent in 1.18+ terrain generation.
- **Driveable flight and ground physics.** The target replaced the reference's per-tick math with a `common/driveables/physics/**` model (`LegacyPlanePhysics`, `AircraftPerformancePhysics`, `SuspensionPhysics`, `MarineDraftPhysics`, …). Every reference `type.*` field consumed by `EntityPlane`/`EntityVehicle` has a target consumer, but numeric handling parity was not established and can only be judged in play.
- **Teams gametype hook surface.** The reference `Gametype` exposes hooks the target's `GameType` still does not (`baseAttacked`, `objectAttacked`, `entityKilled`, `playerJoined`, `playerQuit`, `playerRespawned`, `roundCleanup`, `getTeamsCanSpawnAs`, `givePoints`); `playerDefected` and `playerChoseNewClass` were added with the defection work. In the shipped gametypes most of these bodies are empty and the non-empty ones were traced to target equivalents in `TeamsManager`, but a third-party gametype extending the reference class would have less to override.
- **Content-pack Java model classes.** The reference ships pack models as compiled classes inside the mod jar (`R/modernweapons/**`, `R/titan/**`, `R/nerf/**`); the target compiles pack-supplied Java models at load time (`T/util/JavaModelCompiler.java`, `ContentPackClassLoader`). Coverage of individual legacy model classes and their animation fields was not enumerated here.
