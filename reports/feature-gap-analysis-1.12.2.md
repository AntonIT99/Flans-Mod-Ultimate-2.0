# Feature gap analysis: Flan's Mod 5.10.0 (1.12.2) → Ultimate 2.0

Initial audit: 2026-09-12. Last updated: 2026-10-11. Direction is strictly reference → target.

- Reference: `C:/Users/alpha/Documents/Minecraft-Development/FlansMod`, Flan's Mod 5.10.0 / MC 1.12.2, HEAD `71ba7ed065d906d48f34ca471bbd0172b5192f6b`.
- Target at the previous audit: `C:/Users/alpha/Documents/Minecraft-Development/Flans-Mod-Ultimate-2.0`, Forge 1.20.1, HEAD `c7af2b0fb3cb85711796129fec789c51e96b999e` (clean tree).
- Current target revalidated: HEAD `263d1fe447df1b5cd779ef18ea6fbe37a8ff3d04` plus the current working tree, including ongoing driveable animation changes. Reference HEAD is unchanged.
- Remaining: **0 MISSING, 0 PARTIAL, 0 UNCERTAIN** after implementing the deeper-audit improvements below. This is not a proof of exhaustive parity.
- Completed findings are removed as they are implemented, so this stays a backlog rather than a historical snapshot. The initial audit found 7 MISSING and 4 PARTIAL; all eleven were implemented on 2026-09-13.

## Revalidation (2026-10-11)

Rechecked the prior findings and caveats against current code. The previously flagged Teams extension hooks are now present in `GameType` and wired through `TeamsManager`: round cleanup, allowed spawn teams, login/logout/respawn, base/object attack notifications, non-player entity deaths, and `givePoints`. That stale deeper-audit entry has been removed. The InfoType report separately removes restored aliases and geometry keys from its lexical gap list.

No previously reported gameplay gap was found to have reopened. A subsequent deeper pass on the same date audited Apocalypse structures, mob AI, terrain generation and driveable physics, producing twelve narrow partial findings. Those findings were subsequently addressed by the implementation below. Content-pack Java model classes were excluded at the user's request. The deliberate blast/gravity and interaction-design differences documented in `feature-gap-analysis-1.7.10.md` also apply where the references share that behavior; they are closed design divergences, not claims of exact numeric parity.

## Scope and evidence conventions

This is a static, reference-driven semantic audit. Discovery walked the reference subsystem by subsystem: content loading and type definitions, handheld guns/ammunition/attachments/grenades, shooting, raytracing and penetration, deployed MGs and AA guns, driveables (planes, vehicles, mechas, seats, wheels, collisions, fuel, repair), tools and armor, enchantments, paintjobs, gun/armor boxes, workbenches and item holders, teams (gametypes, maps, rounds, rotations, bases, spawners, flags, loadout pools, ranks, reward boxes, op sticks, commands), client input/HUD/camera/rendering, networking, sounds and particles, world/loot injection, and the bundled sub-mods (apocalypse, and the pack mods).

For each candidate the reference call sites and data flow were traced, then the target was searched for direct, renamed, componentized, data-driven or newer-mechanism equivalents (Lombok accessors, mixins, datapacks, capability APIs and config snapshots included). Commented-out reference implementations (`MovingSoundDriveable`, `CommonProxy.playBlockBreakSound`, spawner chunk-loading) and reference-side dead fields were excluded. Definition-parameter parity is out of scope here — it is covered by `reports/infotype-parameter-gap-analysis-1.12.2.md`.

Evidence paths use these roots:

- `R/` = reference `src/main/java/com/flansmod`.
- `T/` = target `src/main/java/com/flansmodultimate`.
- Resource paths explicitly identify their repository.

`MISSING` means the described capability has no equivalent in the inspected target paths. `PARTIAL` means the broader mechanic exists but the stated behavior does not. Confidence concerns the narrow finding, not full subsystem equivalence. The original findings were implemented separately afterwards and removed from this backlog. The deeper-audit findings were then addressed in code/resources and removed from the open backlog. Intentional modernization choices are recorded below.

Large, well-ported areas that produced no findings are deliberately not enumerated. Notable examples verified as present: mecha upgrade behaviors (including diamond detection, auto-repair, ore multipliers, rocket pack, item vacuum, waste compaction, forced light level), grenade behaviors (proximity triggers, stickiness, deployable bags, smoke/potion effects, heal amounts), AA gun and deployed-MG mechanics, CTF flag handling, smart weapon drops with ammo consolidation, team spawner vehicle/item spawning, dungeon-loot injection, creative paintjob variants, gun attribute modifiers, flashlight attachments, and the Mecha Parts pack contents (folded into the target's Titan pack).

## Deeper-audit implementation (2026-10-11)

The twelve findings from the deeper pass are closed. The implementation preserves the useful 1.12.2 behavior through modern APIs, with the following bounded adaptations:

| Area | Implemented behavior and evidence |
| ---- | --------------------------------- |
| Abandoned portals | `T/apocalyse/common/world/ApocalypsePortalManager.java#createAbandonedPortal` creates a complete obsidian foundation with only one or two powered corners. The wasteland marker calls it rather than constructing an active teleporter. Player completion still uses normal frame validation. Overworld entry and automatic return portals remain usable immediately. |
| Skeleton displays | `ApocalypseWorldgen.java#generateSkeletonDisplay` now calls `ApocalypseLoot.itemHolderLoot`, restoring journals, flesh, empty holders and explosive-ammunition eligibility. |
| Dead trees | `ApocalypseWorldgen.java#generateDeadTree` adds multi-step rising/drifting log branches while retaining the target's taller trunks and occasional webs. |
| Outfits | `T/apocalyse/common/util/ApocalypseLoot.java#equipRandomArmorSet` chooses a sorted content-pack team and applies a random class's armor overrides. Factory stands and survivors share this helper. The no-team fallback still supplies a random armor piece. |
| Survivor scavenging | `T/apocalyse/common/entity/SurvivorEntity.java#finalizeEntitySpawn` enables equipment pickup with the reference's local-difficulty probability. |
| Survivor reload drops | `ApocalypseGunCombat.java#reload` emits the displaced magazine's configured reload item on the survivor path, while retaining consumable reserves. |
| Drone endurance | The drone reload path copies reusable reserve ammunition without consuming it. Loading an old exhausted drone save reconstructs the reserve from its loaded magazine's ammo type. No ammunition is fabricated for survivors. |
| Boss rewards | `SkullBossEntity.java#dropEntityDeathLoot` restores the guaranteed totem, original apple/powder ranges, gunpowder-block rolls and conditional pack-defined Nukranium Gauntlet with the enchantment roll. Death rewards do not fire on arbitrary entity removal. `FlansModBlocks`/`FlansModItems` register the missing gunpowder block; block/item models, loot, recipes and all namespace locales are supplied. |
| Boss presentation | `SkullBossEntity` synchronizes the action and start game time, including save restoration and late tracking. `T/apocalyse/client/render/SkullBossRenderer.java` renders its built-in head/jaw texture with facing, laughter and the 720-degree drone-spawn spin. The boss event darkens the sky; its collision size matches the restored 16-block head. |
| Dry terrain | Target `data/flansmodapocalypse/worldgen/noise_settings/apocalypse.json` disables aquifers and sets sea level to zero, preserving separate acid lakes and modern caves. |
| Terrain bands | `data/flansmodapocalypse/worldgen/density_function/terrain.json` replaces Overworld sloped-cheese terrain with climate-aligned, flat height bands near y=33/47/64/81/106 and smooth transition slopes. Sulphur basins sit near y=33. Biome climate ranges distinguish the rare basin strip instead of allowing it to be shadowed by overlapping common biome ranges. |
| Propeller damage | `T/common/entity/Plane.java#fixedWingPhysics` scales ordinary fallback powered throttle/thrust and motion correction by the intact-propeller ratio through `LegacyPlanePhysics.propellerThrottle`. All-propeller loss stops propulsion; partial loss degrades powered flight progressively. |

The old broad source-audit caveats for dye factories, runways, pillars and mob goals are closed. `EntitySkuller` is unregistered, and the only use of `EntityAIGoSomewhere` is commented out; neither is a missing feature. Built-in Apocalypse boss presentation is covered, while external content-pack Java models remain excluded.

## Intentional implementation differences

- **Aerodynamic lift remains speed-dependent.** Propeller loss now reduces propulsion and powered motion correction, eventually reducing airspeed/lift. It does not directly delete a gliding aircraft's aerodynamic lift. This retains the target's airspeed-based model while restoring progressive damage; the 1.12.2 throttle-based lift formula was not a better aerodynamic implementation.
- **Modern terrain placement and caves remain.** Flat climate bands restore the wasteland silhouette and building sites without introducing a parallel chunk generator. Six modern biomes, smooth transition slopes and modern cave density functions remain; the reference's three-common-biome zoom-layer mosaic and exact old seed layout are not reproduced.
- **Useful target additions remain.** Taller dead-tree trunks/webs, ready-to-use overworld/return portals, boss target reacquisition/nuke action and server authority are retained. Restored sparse skeleton loot and repairable wasteland portals provide their original exploration behavior.

Changes to terrain and generated ruins apply to new chunks. Existing chunks and active portals are preserved, and terrain seams can appear at explored boundaries. Existing drone reserves and entity save keys remain compatible.

## Physics comparison boundaries

The deeper source comparison resolves the old blanket claim that numeric parity can only be judged in play: several formula differences are provable statically. They do not each become feature gaps merely because the implementation was modernized, and 1.7.10 remains the primary legacy authority for this project.

| Path | Reference 1.12.2 | Current target |
| ---- | --------------- | -------------- |
| Fixed-wing control response | Throttle-dependent sensitivity and direct per-tick angular response | `LegacyPlanePhysics` airspeed-dependent sensitivity and smoothed angular response |
| Fixed-wing support | Throttle/propeller/wing-dependent lift, gravity `0.049` per tick before drag | Forward-airspeed-squared/wing-dependent fallback lift, gravity `0.098` before dimension scaling/drag; real-profile path uses aerodynamic forces |
| Helicopter support | Immediate collective lift, gravity `0.098`, common drag multiplier | `HelicopterPhysics` rotor spool and squared RPM lift, gravity `0.05`, tilt/collective translation and braking; real profiles adjust performance |
| Ground motion | Per-wheel acceleration, damping and steering; spring wheel corrections move the chassis | Target-speed/grip fallback or resolved powertrain, sampled suspension support and separate hull collision |
| Legacy override | No alternate real-profile path in reference | Overrides bypass resolved profiles; they do not reconstruct the 1.12.2 integrator |

Evidence: `R/common/driveables/EntityPlane.java` and `EntityVehicle.java` updates; `T/common/entity/Plane.java`, `Vehicle.java`, `Driveable.java` flotation/support/fuel consumers; `T/common/driveables/physics/{LegacyPlanePhysics,HelicopterPhysics,DriveableControlPhysics,SuspensionPhysics,MarineDraftPhysics}.java`. Ground traction, steering, suspension alignment, flotation and fuel use have active target implementations; different coefficients alone are not evidence that those capabilities are absent. The specific propeller-loss omission is reported above. Existing blast and projectile-gravity design divergences remain in the 1.7.10 report.

## Summary

No open MISSING, PARTIAL or UNCERTAIN findings remain from the deeper audit. Closed implementation differences are bounded above; this does not establish exhaustive or runtime parity.

## Areas requiring deeper audit

- **Generated-world execution.** Native density/noise codecs and representative flat-band calculations are validated by focused tests. In-game sampling is still needed to assess transition slopes, cave entrances, structure placement across chunk boundaries, acid basins and deferred props after save/reload.
- **Combat, trajectories and multiplayer.** In-game checks remain for repaired ruins, outfits/scavenging, repeated drone reloads, boss death rewards and late-tracking attack animation. Flight/ground formula differences are documented above; actual takeoff, rotor spool, partial propeller damage, suspension/flotation and authoritative client interpolation still need matched-input play tests.

Content-pack Java model classes were not assessed, as requested.

## Validation of this implementation

- **Full build and tests:** `gradlew build --no-parallel` passed, including packaging/reobfuscation. The suite ran 1,519 tests with no failures and 10 skips. Focused tests decode the native terrain/noise/climate data, check representative band heights and sulfur-biome selection, and check partial/complete propeller loss, reverse throttle and non-finite inputs.
- **Formatting and analysis:** `spotlessApply`/`spotlessCheck` passed with an external init script targeting this task's Java files and report, preserving unrelated work. `sonarlintMain` and `sonarlintTest` passed with the same scoped source selection; tests have no findings, and 36 pre-existing main-source findings remain. IntelliJ inspections completed with the shared profile in an isolated cache: no errors, no warnings in the new renderer or touched tests, and 52 warnings in the touched existing main files (existing nullability/deprecation/style findings and Forge entry-point false positives). The new renderer factory's false unused warning is narrowly suppressed with its Forge call path explained.
- **Resources and diff:** New resource JSON and sorted locale entries were checked. The built mod jar contains the renderer, density function and gunpowder-block resources. Scoped `git diff --check` passed in the main and wiki repositories.
- **Review boundaries:** Source review traces server-owned world/entity mutation, preserved NBT, native synchronized entity data and client-only renderer registration. No in-game world-generation, combat, flight or multiplayer session was run; the deeper-audit follow-ups above remain runtime checks. No content-pack definitions or packaged pack inputs changed. The sibling wiki's `Apocalypse.md` and `Realistic-Vehicle-Physics.md` document the restored behavior and new-chunk boundary.
