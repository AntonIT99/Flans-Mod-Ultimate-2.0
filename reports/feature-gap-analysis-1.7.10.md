# Feature gap analysis: Ultimate 1.7.10 → Ultimate 2.0

Initial audit: 2026-09-10. Last updated: 2026-10-02. Direction is strictly reference → target.

- Reference: `C:/Users/alpha/Documents/Minecraft-Development/Flans-Mod-Ultimate-1.7.10`, HEAD `9b669b12149c3c9fa3a69f1a4ac5d8a55367506d` (unchanged since the initial audit).
- Target initially audited: HEAD `08494f8a4972c249feb4e7d8522e981547439c4e`; previous revalidation: `e0b68dc60fc7060324ee4293678183aca93650b7`.
- Target revalidated: HEAD `767d2dc61b42f488ad423e704d1c4a8981e6a481` (166 commits after the 2026-09-21 update). The findings of that revalidation were then implemented in the working tree on the same day.
- Completed findings are removed as they are implemented, so the report remains a backlog rather than a historical snapshot.
- Remaining: **0 MISSING, 0 PARTIAL, 0 UNCERTAIN**. The deeper audit of 2026-10-02 found three more differences: two are recorded as deliberate divergences and one was fixed. These counts describe outstanding findings, not a percentage of port completeness.
- Findings closed by a deliberately different design, rather than by reproducing the reference, are recorded under "Deliberate divergences" instead of being deleted.

## What changed in this revalidation

The 2026-09-21 revision listed no outstanding findings. This pass re-ran discovery from the reference instead of only rechecking the earlier backlog, and found 2 missing and 9 partial features. Ten were in areas the earlier pass had not opened up far enough (Teams administration, death drops and pickups, gameplay settings from `FlansMod.java`, HUD options); one, the `OneHanded` default, came from commit `4b3037279` and is now recorded as a deliberate divergence.

The rewritten blast/fragmentation, gravity/drag, steering and HEAT systems were checked for missing mechanics. Their changes in numbers are listed under "Areas requiring deeper audit" rather than counted as gaps, because they are deliberate model replacements.

### Closed on 2026-10-02

| Finding | Closed by |
| --- | --- |
| Per-rule Teams administration commands | `T/common/command/TeamsCommand.java#addRuleCommands`: every legacy rule under its legacy spelling and aliases (`overrideHunger`/`noHunger`, `bombs`/`allowBombs`, `shells`, `bullets`/`bulletsEnabled`, `canBreakGuns`, `canBreakGlass`, `survivalCanBreakVehicles`, `survivalCanPlaceVehicles`, `armourDrops`/`armorDrops`, `weaponDrops <on\|off\|smart>`, `vehiclesBreakBlocks`, the five `*Life` timers), plus `useRotation`, `autobalance`, `setRound`, `goToMap`, `getSticks`/`getOpSticks` and `ping`. `T/common/teams/TeamsManager.java#queueNextRound` keeps the operator's next-round choice ahead of rotation and voting, and `setCurrentScoreLimit` gives every game type the `scorelimit` variable. Game-type variables are now saved after `setvariable`. |
| Lag-compensation tuning | `bulletSnapshotMin`/`bulletSnapshotDivisor` in the common config (`Teams Settings`), read by `TeamsManager#getBulletSnapshotMin`/`Divisor`; `/teams bltss <min> <divisor>` persists them, `/teams bltss`, `/teams showbltss` and `/teams admin bltss` show them. |
| Death drops under `weaponDrops`/`armourDrops` | `T/common/teams/TeamsDeathDrops.java`, called from `CommonEventHandler#onLivingDrops`. Smart drops spawn one `GunItemEntity` bundle per gun type carrying the matching ammunition, as 1.7.10 did. |
| Spectator pickup and team item spawners | `CommonEventHandler#canPickUp` blocks spectators during a round. `T/common/entity/TeamItemEntity.java` circles the spawner client-side, is indestructible, never despawns or merges, and only the team owning the spawner's base can take it (`TeamSpawnerBlockEntity#getOwningTeam`). |
| Post-respawn slot 0 and automatic reload | `TeamsManager#respawnPlayer` selects slot 0 and schedules `PlayerData#tickRespawnReload` five ticks later; that reload uses the existing instant first-reload rule. |
| `ShootWithOpenDoor` semantics and weapon-arming HUD | `T/common/types/VehicleType.java#doorAllowsFiring`, used by `Vehicle#canFireWeaponBank`; `ClientHudOverlays` shows `Weapon: READY/DISABLED [door key]`. |
| Player maximum health | `maxPlayerHealth` (`General Settings`), applied by `CommonEventHandler#applyMaxHealth` on login and on respawn. |
| Gun development mode | `gunDevMode` (`Gun Settings`) and the restored `GunReloadEvent#needsAmmo`: either loads the default ammunition without consuming any (`GunReloader#loadDefaultAmmo`). |
| Kill message toggles and distance | `enableKillMessages` and `showDistanceInKillMessage` (`General Settings`); `FlanDamageSources.FlanDamageSource#getLocalizedDeathMessage` appends the distance, and `CommonEventHandler#sendKillMessage` honours the switch. |
| Hit-marker and tooltip display options | Client `showHitMarker`, `hitMarkerRed/Green/Blue/Alpha` (`ClientHudOverlays#renderHitMarker`) and `showDetailedItemDescriptions` (`IFlanItem#showDetailedDescriptions`). |

While implementing smart drops, `GunItemEntity` turned out to have no client renderer registered; it now uses the vanilla item renderer, as does `TeamItemEntity`.

## Findings table

No outstanding findings. The [deeper audit](#deeper-audit-2026-10-02) found three differences: the explosion damage curve and the ballistics are deliberate (see "Deliberate divergences"), and the driveable terrain collision was fixed.

## Scope and evidence conventions

This is a static, reference-driven semantic audit. Discovery covered reference types and configuration keys, weapons and ammunition, grenades, deployable/AA guns and targeting, tools and armor, driveables/aircraft/mechas, controls, rendering and effects, teams/game modes, commands, inventories/crafting, persistence, and networking. This pass also lexically diffed every key the reference parsers and commands read against the target's string literals, then traced each name with no match to see whether it had an active consumer. Commented-out implementations and unused reference declarations were excluded. These include `gunCarryLimit`, `OnRadar`, `MotionSensor*`, `LockOnFuse`, `PenetratesEntities` and `EUPerCharge`.

Evidence paths use these roots:

- `R/` = reference `src/main/java/com/flansmod`.
- `T/` = target `src/main/java/com/flansmodultimate`.
- `TL/` = target legacy-compatibility package `src/main/java/com/flansmod`.

`MISSING` means the described capability has no equivalent in the inspected target paths. `PARTIAL` means the broader mechanic exists but the stated option or secondary behavior does not. Confidence concerns the narrow finding, not full subsystem equivalence. The audit itself changed nothing; the implementation that closed its findings is listed below. Unit tests and a full build were run for that implementation, but no in-game test was, so runtime parity is not established by this report.


## Deliberate divergences

Reference behavior settled by a different design decision rather than reproduced. These are closed, not outstanding.

### Blocking block interactions while armed: one client preference instead of two server booleans

Reference: `R/common/guns/ItemGun.java#onPlayerInteract`, registered through the first gun item in `R/common/FlansMod.java`; `holdingGunsDisablesChests` (default on) and `holdingGunsDisablesAll` (default off).
Right-click block interaction can be canceled for inventory blocks specifically, or for all blocks, while a gun is held.

Target: `T/config/EnumGunBlockInteraction.java`, `T/config/ModClientConfig.java` (`gunBlockInteraction`, Input Settings), `T/event/handler/ClientEventHandler.java#isBlockUseSuppressed`.
One client option with three values replaces the two booleans: `ALLOW`, `NO_CONTAINERS` (default) and `NONE`. Containers are detected with `state.getMenuProvider(level, pos)`, the same test `GunItem#doesSneakBypassUse` already uses. Under `NO_CONTAINERS` sneaking opens the container anyway, which turns that existing sneak bypass into a deliberate escape hatch; `NONE` has no way through.

Why the reference design was not reproduced:

- The two booleans overlap. `holdingGunsDisablesAll` subsumes `holdingGunsDisablesChests`, so two of the four combinations mean the same thing. Three named values say the same with no invalid state.
- It is a personal preference, not a rule of play. Giving up a block interaction costs other players nothing, so it belongs in the client config and the options screen rather than in server configuration, and needs no synchronization or packet.
- The gap was never the mechanism, but the default. Before this, aiming at a chest opened it whether or not the player sneaked; the reference's on-by-default chest rule existed to prevent exactly that, and `NO_CONTAINERS` restores that default.

Implementation note: suppression is applied to the vanilla use-item event, not to the configured aim button, so it holds whichever buttons the player has bound and aiming is unaffected since it is read from the key itself.

Not carried over: two separate booleans, server-side configuration of this behavior, and the reference's lack of any sneak bypass under the chest rule.

### Content-definition mismatch: warn per pack instead of kicking

Reference: `R/common/sync/Sync.java`, `R/common/sync/SyncEventHandler.java#playerJoined`, `R/common/network/PacketHashSend.java`, `kickNonMatchingHashes`.
The reference hashes all normalized definition text into one aggregate hash. On dedicated-server login the server sends it, the client replies with its own, and with `kickNonMatchingHashes` enabled the server disconnects a mismatching client.

Target: `T/common/sync/ContentFingerprint.java`, `T/common/sync/EnumContentMismatch.java`, `T/network/client/PacketContentFingerprint.java`, `T/ContentManager.java#registerConfigs`, `T/event/handler/CommonEventHandler.java#onPlayerLogin`.
The target fingerprints definition text per content pack (SHA-256, normalized to drop blank lines, comments and indentation) and sends the server's fingerprints to each player on login. The client compares them with its own and names the packs that differ, are missing, or are unknown to the server. Nobody is disconnected and the client sends nothing back.

Why the reference design was not reproduced:

- The reference check cannot stop a cheater. The client simply returns a string, so a modified client echoes the server's hash and passes.
- A missing pack is already caught upstream. Forge's registry synchronization refuses a login when the client lacks registered items, which is the failure a kick would otherwise cover.
- What remains is packs with the same shortnames but different numbers. Since the mod is server-authoritative, that costs the player accurate information (displayed statistics, zoom, animation timing) rather than fairness, which makes it a diagnosis problem. Naming the offending pack solves it; disconnecting does not.
- One-way exchange keeps client-supplied data out of server decisions, unlike the reference, whose server acts on a value the client chose.

Cost: one SHA-256 pass over definition text during the existing load, and one small packet per login. Nothing runs per tick.

Not carried over: `kickNonMatchingHashes` and its enforcement, and the aggregate whole-installation hash. Per-pack fingerprints replace it, since an aggregate can only say that something differs.

### Two-handed guns: dual wielding by default

Reference: `R/common/guns/GunType.java` (`oneHanded = false`).
A gun is two-handed unless its definition says `OneHanded true`, and two-handed guns cannot be dual-wielded.

Target: `T/common/types/GunType.java` (`oneHanded = true`, commit `4b3037279`), `T/common/item/GunItemHandler.java#gunCanBeHandled`, `disableDualWielding` in the common config.
Guns are one-handed unless their definition says `OneHanded false`. The two-handed restriction itself is unchanged and applies to every gun that opts out.

Why the reference default was not kept: Minecraft 1.20.1 has a native off hand, and this port encourages dual wielding with it. A server can still forbid dual wielding for every gun with `disableDualWielding`.

Not carried over: the two-handed default for definitions without a `OneHanded` line.

### Ad-hoc game types: rounds are the unit of play

Reference: `R/common/teams/CommandTeams.java` (`setGametype`, `setTeams`).
An operator could start a game type and pick its teams directly, without a configured round.

Target: `/teams admin round add <map> <gametype> <teams> <minutes> <score>` followed by `/teams admin start <index>` or `/teams goToMap <index>`.

Why it was not reproduced: the Teams runtime is built around saved rounds that tie a game type to a map, its bases, its chunk tickets, and time and score limits. An ad-hoc game type without a map would bypass all of that, while adding a round and starting it gives the same result in two commands. The reference's global `/teams autobalance` toggle was also never read in 1.7.10; the restored command switches the current game type's `autobalance` variable instead, which is what the reference's TDM and CTF used.

Not carried over: `setGametype` and `setTeams`.

### Legacy explosion damage: the new blast model for every explosive

Reference: `R/common/guns/FlansModExplosion.java#doExplosionA` (vanilla 1.7.10 `Explosion`), called from `R/common/guns/EntityBullet.java`, `EntityGrenade.java` and `R/common/driveables/EntityDriveable.java`.
Entities within **twice** the `ExplosionRadius` R are hurt. With `x = (1 − distance / 2R) · exposure`, the damage is `(x² + x) / 2 · 16R + 1`, then multiplied by `ExplosionDamageVsPlayer/Living/Plane/Vehicle`, which are multipliers defaulting to 1. Wheels and seats take `vehicleWheelSeatExplosionModifier` on top.

Target checked: `T/common/types/ShootableType.java#read` (`explosionBlastDamage.scale(8F * explosionRadius + 1F)`), `#getExplosionStats`, `T/common/explosions/FlanExplosion.java` (`Stats`, entity loop, `getBlastDamage`), `T/common/explosions/ExplosionScaling.java#blastFalloff`, `T/common/driveables/armor/ExplosionVehicleDamageResolver.java#legacyTntEquivalentKg`.
A definition without `ExplosiveMassTNTg/Kg` keeps the legacy keys, but they feed a different curve. The peak is `(8R + 1) · multiplier`, half the reference's `16R + 1`. Reach is `max(BlastRadius, R)`, half the reference's 2R. The falloff `sqrt((1 − d/R)² / (1 + (2.5·d/R)³))` drops much faster. Vehicles are resolved part by part with an armour pressure model, from a TNT equivalent recovered from R and `ExplosionPower`, instead of the reference formula times `ExplosionDamageVsVehicle`.

Damage to a fully exposed player with multipliers at 1, 1.7.10 / target:

| Radius | Centre | R/2 | R | 1.5R | 2R |
| --- | --- | --- | --- | --- | --- |
| R = 2 | 33 / 17 | 22 / 5 | 13 / 0 | 6 / 0 | 1 / 0 |
| R = 4 | 65 / 33 | 43 / 10 | 25 / 0 | 11 / 0 | 1 / 0 |
| R = 8 | 129 / 65 | 85 / 19 | 49 / 0 | 21 / 0 | 1 / 0 |

Built-in content is barely affected: 184 of the 221 explosive bullets and grenades get an explosive mass from the category files or their own definitions and use the new model as intended. Of the other 37, 32 are AP shells with a cosmetic radius of 1 or 2 (27 Warfare 44, 3 Official Packs, 2 Wolff Germany). Five use real legacy blasts: the Manus Packs `ammopowerpackxx9` (R 27), `wh40k_ammo_warhoundplasmablastgun` (R 27) and `wh40k_ammo_warhoundturbolaserdestructor` (R 31), and the Official Packs `grenadelauncherincendiaryammo` and `mg151ammo`. Third-party legacy packs are affected in full: their grenades, rockets and HE shells do about half the damage at the centre and nothing past their radius. `forceLegacyPlanePhysics` and `forceLegacyVehiclePhysics` restore legacy movement, but nothing restores this curve.

Why the reference curve was not kept: the legacy formula comes from vanilla 1.7.10, whose `Explosion` doubled the authored size before hurting entities, so an `ExplosionRadius` of 4 hurt players 8 blocks away. The target aligns legacy definitions with the modern blast model instead: the authored radius is the radius at which the blast stops hurting, the peak scales with it, and the falloff matches the one used for explosives declared by mass. The input radius therefore means what it says.

Not carried over: the 2R reach, the `16R + 1` peak and the shallow falloff of 1.7.10 for definitions without an explosive mass. A pack that wants the old peak and reach (with the steeper falloff) can declare `BlastRadius` as twice its `ExplosionRadius` and `ExplosionDamage 2`, since legacy damage values are multiplied by `8R + 1`; or it can declare an explosive mass.

### Projectile gravity and drag: realistic values for every projectile

Reference: `R/common/guns/EntityBullet.java#onUpdate` (`drag = DragInAir`, `motionY -= 0.02F * FallSpeed`), `R/common/guns/EntityGrenade.java#onUpdate` (`motionY -= 9.81 / 400 * FallSpeed`, no air drag). The 1.12.2 sources use the same values.

Target checked: `T/common/types/ShootableType.java` (`FALL_SPEED_COEFFICIENT = 9.81 / 400`, `AIR_DEFAULT_DRAG`), `T/common/entity/Bullet.java#applyDragAndGravity`, `T/common/entity/Shootable.java#applyDragAndGravity`, `T/common/entity/ProjectileDrag.java`; commits `4b86cf558` ("Fix fall speed") and `181611090` (gravity and drag rework).
Bullets now fall with 9.81/400 ≈ 0.0245 blocks/tick² instead of 0.02, 23 % more. Grenades keep the 1.7.10 gravity but are also slowed by the air drag of 0.99 per tick (0.8 in water, 0.6 in lava), which neither legacy version applied to them. Simulated with the mods' own update order:

| Projectile | 1.7.10 | Target |
| --- | --- | --- |
| Bullet, 5 blocks/tick, drop at 100 blocks | 4.9 | 6.0 |
| Bullet, 2 blocks/tick, drop at 100 blocks | 40.0 | 49.0 |
| Bullet, 2 blocks/tick at 45°, range | 97 | 87 |
| Grenade `ThrowSpeed 1` at 45°, range | 9.8 | 8.1 |
| Grenade `ThrowSpeed 2` at 45°, range | 40.1 | 28.3 |

Built-in content compensates through its categories: 682 bullet entries set `FallSpeed` and 34 grenade entries set `ThrowSpeedMs` for the new coefficients. Third-party legacy packs do not: their guns shoot lower and their grenades and grenade launchers fall short, by up to about 30 % for strong throws.

Why the reference values were not kept: 0.02 blocks/tick² was an arbitrary constant. 9.81/400 is Earth's gravity at one block per metre and 20 ticks per second, and it applies to every Flan's projectile alike. Bullets (`Bullet#applyDragAndGravity`) and grenades (`Shootable#applyDragAndGravity`) use the same `FALL_SPEED_COEFFICIENT · FallSpeed`, the same 0.99 air, 0.8 water and 0.6 lava drag through `ProjectileDrag`, and the same dimension factors through `ModPhysics`, which `FlansModApi#getGravityFactor`/`setGravityFactor` and `getDragFactor`/`setDragFactor` read and change. A grenade therefore flies exactly like a bullet with the same speed and `FallSpeed`.

Thrown guns (`T/common/entity/ThrownGun.java`, javelins and the like) follow it too since 2026-10-02. As vanilla arrows they used to fall with 0.05 blocks/tick², twice the shared value, which made a throw authored in real metres per second (`ThrowSpeedMs`) fall short. They now undo the arrow gravity and apply the shared coefficient through `ModPhysics`.

Not carried over: the 0.02 bullet gravity and drag-free grenade flight of 1.7.10 for definitions that do not author `FallSpeed` or `ThrowSpeedMs` for the new values.

## Deeper audit (2026-10-02)

The previous revision left five areas whose implementations differ too much for a name-level comparison: explosion damage, ballistics, driveable collision, mecha leg animation and multiplayer synchronization. Each was traced through both codebases and, where it is numeric, checked by reproducing both formulas. Figures assume the default common config (`newDamageSystemBlastFalloffSharpness` 2.5, gravity and drag factors 1).

### World collision at a driveable's extremities — fixed

Reference: `R/common/driveables/EntityWheel.java` (`setSize(1F, 1F)`, `moveEntity`), `R/common/driveables/EntityVehicle.java#onUpdate` and `EntityPlane.java#onUpdate` (each wheel moved through the world, the body derived from the wheels), `R/common/driveables/EntityDriveable.java` (`seatCollisions` ray from each occupied seat to the wheel midpoint, setting `collisionHardness`, which pushes the vehicle back when it exceeds 0.2).
Every wheel is a 1×1 entity that collides with terrain on its own, so a vehicle's corners stop at walls. With `seatCollisions` on (the default), an occupied seat whose path back to the wheels crosses a solid block also stops and repels the vehicle, which kept long vehicles and their crews out of walls.

Target checked: `T/common/entity/Driveable.java#moveWithCollisions`, `#getDimensions`, `#sweepCollisionPointImpacts`, `#handleCollisionConsequences`, the wheel probes in `#applyWheelContactPhysics`, `T/common/entity/Wheel.java` and `Seat.java` (`noPhysics = true`), `T/common/driveables/DriveableCollisionHelper.java`, `DriveableCollisionWorld.java`.
Before the fix, only the driveable's own bounding box collided with terrain horizontally, through vanilla `move`. That box is the core part's footprint clamped to at most 4 × 6 blocks. Wheels and seats are physics-free proxies, the suspension probes only look vertically for ground, and the authored collision points only damage parts after the move. The shaped hulls (`DriveableCollisionHelper`) handle entities against the vehicle, not the vehicle against terrain.

On a vehicle longer or wider than about 4 blocks (most tanks, trucks and aircraft), the nose, tail, wings and outer seats could therefore pass into walls until the centre box touched them, including an occupied driver seat at the front.

Fixed in `T/common/driveables/DriveableTerrainProbes.java` and `T/common/entity/Driveable.java#moveWithCollisions`. Each move is now limited by small terrain boxes at every intact wheel and at every occupied seat, as 1.7.10's wheel entities and `seatCollisions` were. The boxes start above the driveable's step height (at least 0.6 blocks), so ledges it can climb never stop it. Each axis travels the shortest distance any box allows, so a driveable slides along walls instead of sticking. A turn that would swing a wheel or crewed seat into terrain is cancelled for that tick (`#guardTurnAgainstTerrain`). Boxes already inside terrain are ignored, so a driveable can always drive or turn out. A stop counts as a horizontal collision, and impact damage and the collision-point sweep use the speed that was requested. Aircraft use the probes only while on the ground (`Plane#usesTerrainProbes`); in flight, terrain is still struck through their collision points. This goes further than 1.7.10 in two ways: rotation is guarded too, and stopped crews are not shoved back. It still needs an in-game check with a long tank and a truck driven into walls and turned against them.

### Checked and found equivalent

- **Mecha simple-leg animation.** 1.7.10 steps `legSwing` towards `1 / (LegSwingLimit − 1)` while walking and divides it by `LegSwingLimit` every tick, then draws `sin(ticks / LegSwingTime) · legSwing` radians. `TL/client/model/ModelMecha.java` uses the same amplitude and period from `Driveable.tickCount`. The only differences are cosmetic: the amplitude is capped at 70°, which only matters for `LegSwingLimit` below about 1.82, and it follows the walking throttle instead of ramping up and down over a few ticks.
- **Seated players across a reconnect.** Neither version keeps a player mounted across a logout: 1.7.10 `EntitySeat` is never written to NBT, and target seats are unsaved proxies rebuilt by `Driveable#ensureProxyEntities`.
- **Late entity tracking.** Driveable part health and states reach players who start tracking a driveable later (`CommonEventHandler#onStartTracking` → `Driveable#sendPartStateTo`). Seat links travel in synced entity data (`Seat.DATA_SEAT_INDEX`, `DATA_PARENT_ID`), and passengers through vanilla pairing.

### Still requiring in-game validation

- Multiplayer timing: reconnecting in the middle of a reload, round transitions with players in vehicles, and persisted rounds across a server restart. Source review found no gap, but cannot prove that timing-dependent behaviour matches.
- Articulated vehicles and custom model transforms: visual equivalence of legacy model animation was not established by this audit.
