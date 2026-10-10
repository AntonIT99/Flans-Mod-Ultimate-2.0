# Feature Gap Analysis — Flan's Mod NPC Vehicles → NPC module

Implementation follow-up on Forge 1.20.1: the two Display/sounds gaps below are
now addressed. Wolff's Mod > Display provides independent hurt-flash and death
rotation switches plus the existing hide-killed-body setting. Wolff's Mod >
Sounds and Particles > Vehicle Sounds provides idle (default No), movement
(default Yes), variable pitch (default Yes) and Use Vehicle's Defaults (default
Yes). Selecting defaults resets custom values from the vehicle definition. Both
channel editors are always visible and read-only in defaults mode. Custom mode
reuses the native sound picker with namespace support, range and measured Flan
lengths or repeat overrides. Cancellable server-timed clips switch between idle
and movement independently of footsteps; automatic timing accounts for pitch.
Settings use normal NPC persistence, presets and spawn/update sync. The original
findings below remain the audit snapshot; GUI appearance and audible behavior
still require in-game verification.

- **Reference:** `C:\Users\alpha\Documents\Minecraft-Development\Flans-Mod-NPC-Vehicles` (commit `7999bb16`, version 7.1.0, Forge 1.7.10, CustomNPC+ 1.11.1)
- **Target:** `../src/npcs` of Flan's Mod Ultimate 2.0 (`master` @ `6e9f190a8`, Forge 1.20.1, Custom NPCs 1.20.1 GBPort), together with the public API in `src/main/java/com/flansmodultimate/api` that the module uses
- **Direction:** reference → target only. Target-only features are left out, such as item-driven equipment combat, paintjob browsing, the 64x32 renderer, inherited type properties and editor read-only locks.
- **Scope note:** the reference's ~600 generated `Entity*`/`Model*` classes are per-model data. The target replaces them with `FlanModelEntities`, which registers a model for every loaded AA gun and driveable. That data-driven equivalent is not reported.

Unless noted, reference paths are relative to `../src/main/java/com/wolffsmod` and target paths to `src/npcs/java/com/wolffsmod/npcs/`.

---

## Vehicle NPC simulation and aiming

### Gradual turret/seat aiming with limits and fire alignment — MISSING

Reference: `entity/Seat.setYawAndPitch`, `isRotating`; `entity/EntityFlanDriveableNPC.setDriver`/`setPassenger`/`setDriverAimSpeed`/`setPassengerAimSpeed`; `mixin/MixinEntityAIRangedAttack.updateTask`; `network/FlanEntitySyncPacket`
Each NPC vehicle has a driver seat and numbered passenger seats parsed from `Driver`/`Pilot`/`Passenger` lines. Each seat has yaw/pitch limits, `DriverAimSpeed`/`PassengerAimSpeed` and an offset yaw. Every tick, while the NPC has a target, the turret turns toward it at the configured speed and stays within its limits. Fire is withheld, without using up cooldown or burst state, until the driver seat is aligned. While a target is held, the server sends the turret's yaw/pitch to clients every 2 ticks. A `RotationLocked` ability freezes turret tracking.

Target checked: `combat/NpcRangedAttack.Attack.launch`, `combat/ProjectileAim`, `mixin/EntityAIRangedAttackMixin`, `api/ISeat` (only for real `Driveable` seats)
Target NPCs work out a fresh firing direction for each shot and launch at once. Nothing stores turret yaw/pitch, aim speed or angle limits, nothing waits for the turret to line up, and nothing syncs turret rotation to clients.

Missing: per-NPC seat state, gradual traverse and elevation, seat angle limits and aim speeds, the fire-only-when-aligned rule, passenger seats tracking on their own, and the turret-rotation sync packet.

### Muzzle origin following turret rotation — PARTIAL

Reference: `customnpc/NPCInterfaceUtil.getFiringPosition`, `entity/EntityFlanDriveableNPC.getPrimaryAimOrigin`, `setTurretOrigin`, `mixin/MixinEntityNPCInterface.shootFlanProjectile`
Shoot points on the `turret` part are rotated around `TurretOrigin` by the driver's yaw and pitch. Points with an offset get yaw on the root and yaw plus pitch on the tip. AA barrels are rotated by gun yaw and pitch. Shoot particles use the same rotated point.

Target checked: `combat/NpcRangedAttack.Attack.modelOrigin`, `api/FlansProjectiles.getMuzzles`
**Present:** model muzzles, alternate barrels, AA barrels, model scale and resting height.
**Missing:** muzzle offsets are only rotated by the body yaw (`yRot(180 - yBodyRot)`), so shots and particles leave from the barrel's rest pose rather than where the turret or gun is pointing.

### Advanced ballistic target leading — PARTIAL

Reference: `customnpc/FlanBallisticAim` (`MotionTracker`, `MotionEstimate`, `solve`), called from `Seat.setYawAndPitch`
The aim solver tracks the target's server-side motion every tick and smooths its velocity over recent samples. It seeds a newly acquired target from one tick of movement and adds a conservative acceleration estimate. It iterates flight time up to four times, aims at a point scaled to the target's body size, and throws out stale history, teleports and correction spikes. Lead is capped at 100 ticks of flight, 96 blocks horizontally and 48 vertically. It re-solves every 3 ticks and falls back to direct line of sight.

Target checked: `combat/ProjectileAim.direction`, `NpcRangedAttack.Attack` (`targetVelocity = target.getDeltaMovement()`, aim point at half the target's height)
**Present:** a gravity- and drag-aware intercept against constant velocity, with a high/low arc choice.
**Missing:** velocity smoothing and history, the acceleration term, spike and teleport rejection, safety caps on lead, the body-size aim point, and solver caching.

### Hull facing its movement direction and slowing when misaligned — MISSING

Reference: `mixin/MixinEntityCustomNpc.func_110146_f` (`EntityNPCBodyHelper`), `mixin/MixinEntityNPCInterface.wolffsmod$limitMisalignedVehicleTravel`, `ConfigDriveable.setTurnSpeed` (ALPHA_24)
A non-plane vehicle's hull turns gradually toward its actual direction of travel instead of its look/combat yaw, so the turret can stay on the target. When the hull is more than 20° off its heading, speed drops progressively, down to 15% at 90° or more, so vehicles don't drive sideways or backwards at full speed. Planes are left out.

Target checked: `mixin/EntityNPCInterfaceMixin`, `combat/NpcEquipmentRangedGoal.aim`, `properties/NpcTypeMapping` (only `WALKING_SPEED`)
Target NPC vehicles use Custom NPCs' humanoid body rotation unchanged. `NpcEquipmentRangedGoal.aim` even snaps body yaw to the target.

Missing: a hull aligned to travel, turn-rate-limited hull rotation, and throttling when misaligned.

### Riders seated at the driver position — MISSING

Reference: `entity/EntityFlanDriveableNPC.updateRiderPosition`, `getDriverPosition`; `mixin/MixinEntityCustomNpc.updateRiderPosition`
A player or entity riding an NPC vehicle sits at the definition's `Driver` position plus `RotatedDriverOffset`, rotated with the turret and scaled with the NPC's model size.

Target checked: `mixin/EntityNPCInterfaceMixin`, `model/FlanModelEntity`; searched `../src/npcs` for passenger/rider positioning
No rider positioning. Riders use Custom NPCs' default mount point.

Missing: rider placement at the driver seat.

### NPC eye height at the primary shoot point — PARTIAL

Reference: `mixin/MixinEntityCustomNpc.getEyeHeight`, `entity/EntityFlanDriveableNPC.getEyeHeight`
A vehicle NPC's eye height is the Y of its first primary shoot point (or driver seat) plus `YOffset`. Line-of-sight and target checks therefore start from the gun.

Target checked: `combat/NpcRangedAttack` (projectiles start at model muzzles), `model/FlanModelShape` (hitbox only)
**Present:** projectiles leave from the model muzzles.
**Missing:** the NPC's eye height, which Custom NPCs uses for sensing and line of sight, is still derived from the generic hitbox, not the gun position.

### Persistent driveable state flags — MISSING

Reference: `entity/EntityFlanDriveableNPC.read/writeEntityToNBT` (`DoorsOpen`, `forceMaxThrottle`), `entity/EntityFlanPlaneNPC` (`PropellerOn`, `GearUp`, `VarWings`)
Per-NPC NBT flags select doors open or closed, landing gear up or down, variable-wing position and propeller on or off. They can also force full throttle so wheels and tracks animate. The model renderer reads all of them.

Target checked: `model/FlanModelEntity` (only `FlanPaintjob`), `api/client/FlansModelPreviews` (Javadoc: doors are closed and gear is down)
Only the paintjob is stored.

Missing: storing and rendering door, gear, wing, propeller and throttle states.

### Summoning a model entity creates an NPC — MISSING

Reference: `ModEntityRegistry.createEntity` (`registerModEntity`), `entity/EntityFlanDriveableNPC.onUpdate`
Every vehicle model is a summonable entity. Once spawned, it replaces itself with an `EntityCustomNpc` that uses that model, texture and frustum flag, so `/summon` or spawners can place ready-made vehicle NPCs.

Target checked: `model/FlanModelEntityType` (Javadoc: "It cannot be summoned"), `model/FlanModelEntities`
Model types can only be picked in the Custom NPCs model menu.

Missing: creating a vehicle NPC by spawning or summoning the model type.

---

## Vehicle model rendering and animation

### Animated vehicle models — MISSING

Reference: `model/ModelFlanVehicle` (`renderTurretAndBarrel`, `renderWheels`, track frame selection, `renderFancyTracks`, `renderDrillHead`, door/steering parts, passenger `gunModel` yaw/pitch/recoil), `entity/EntityFlanVehicleNPC.updateNpc` (`wheelsAngle`, `harvesterAngle`, fancy-track links, `throttle` from movement speed)
Turret and barrel follow the driver seat's yaw and pitch. The animated barrel recoils. Wheels and alternate track frames advance with movement, fancy track links run along their link points (`LeftLinkPoint`, `RightLinkPoint`, `TrackLinkLength`, `FixTrackLink`), drill heads spin, and door models follow `DoorsOpen`. Passenger guns turn with their seats, including turret-mounted guns, scaled by `VehicleGunModelScale`.

Target checked: `client/FlanModelEntityModel.setupAnim` ("Static preview for now"), `platform/render/NpcRenderBuffers.renderWorld`, `api/client/FlansModelPreviews.renderWorld` (Javadoc: "turrets and guns face forward, doors are closed and gear is down")
The world model is a static rest pose. It gets caching and level-of-detail but no animation.

Missing: turret and barrel aiming, recoil, wheel and track motion, fancy track links, drill and harvester rotation, door states, and passenger-gun aiming.

### Animated plane models — MISSING

Reference: `model/ModelFlanPlane`, `entity/EntityFlanPlaneNPC.updateNpc` (`propSpeed`, `propAngle`)
Propellers and rotors spin with throttle while the engine is on. Landing gear (skids and wheels) is drawn or hidden by `GearUp`, variable wings switch position, doors follow `DoorsOpen`, and passenger guns aim.

Target checked: same as above
Static rest pose only.

Missing: propeller and rotor spin, gear retraction, variable wings, door state and passenger-gun aiming on planes.

### Mecha walking animation — MISSING

Reference: `entity/EntityFlanMechaNPC` (`LegNode`, `readLegNodes`, `moveLegParts`, `legSwing`, `legPosition`, arm origins, hand modifiers), `model/ModelFlanMecha`
Leg-node keyframes (`LegNode`, `LegAnimSpeed`, `LegSwingLimit`, `LegSwingTime`) move the upper and lower legs and feet of both sides while the mecha walks. Arm origins and limits position the arms.

Target checked: same as above (`FlansModelPreviews` keeps mechas as static geometry)
Static rest pose only.

Missing: leg-node walk cycles and arm posing for mecha NPCs.

### Animated AA gun models — MISSING

Reference: `model/ModelFlanAAGun`, `entity/EntityFlanAAGunNPC` (`barrelRecoil`, `numBarrels`, `Recoil`)
The gun, gunsight, barrels and ammo turn with the driver seat's yaw and pitch, and each barrel recoils on its own.

Target checked: same as above
Static rest pose only.

Missing: AA gun traverse and elevation, and per-barrel recoil.

### Frustum-culling exemption for vehicle and large NPCs — MISSING

Reference: `mixin/MixinEntityCustomNpc.onUpdateHitbox`, `WolffNPCMod` options `Ignore frustum check for NPC vehicles`, `Ignore frustum check for large entities`, `Large NPC entity size`
NPC vehicles and large NPCs skip frustum culling (each case configurable) because their models reach far past the small entity hitbox.

Target checked: `model/FlanModelShape` (hitbox width capped at 4 blocks), `client/FlanModelBounds` (used only for the nameplate and menu preview), `../main/java/com/flansmodultimate/mixin/EntityRendererMixin.flansmodultimateDisableCulling` (only for held items with culling disabled); Custom NPCs 1.20.1 `EntityNPCInterface` has no culling-box override
Nothing widens the culling box or disables culling for Flan-model NPCs.

Missing: a culling exemption or expanded culling bounds, so long ships and large vehicles don't disappear while partly on screen.

### Flan armour following Custom NPC part scaling and custom poses — UNCERTAIN

Reference: `mixin/MixinModelCustomArmour` (`applyCustomNpcFullModelTransform`, `applyCustomAnimationPartTransforms`, `renderAnimatedTurbo`, per-pose methods for dancing, crawling, hugging, waving, crying and puppet)
Flan armour on NPCs takes the NPC's overall size, per-part scales and custom-animation part transforms, and copies every Custom NPC pose.

Target checked: `../main/java/com/flansmodultimate/client/render/layer/CustomArmorLayer` (`copyPropertiesTo`), `mixin/NpcEquipmentAnimationMixin`
Armour copies the parent model's part rotations and positions, so standard poses probably carry over. `copyPropertiesTo` does not copy per-part scale, and it wasn't verified whether Custom NPCs 1.20.1 applies part scaling and puppet/custom animations through those part poses.

Missing: possibly per-part scale and some custom-animation transforms on Flan armour. Needs an in-game check.

---

## Combat and damage

### Flan damage against vehicles and planes for NPC vehicles — MISSING

Reference: `mixin/MixinEntityNPCInterface.attackEntityFrom` (`isFlanDriveable`, `isFlanPlane`, `damageVsVehicles`/`damageVsPlanes`)
When a Flan projectile hits an NPC that looks like a vehicle or plane, its damage-vs-living is swapped for the ammunition's `DamageVsVehicles` or `DamageVsPlanes`. Anti-tank and anti-air ammunition therefore behave correctly against NPC vehicles.

Target checked: `../main/java/com/flansmodultimate/common/types/DamageStats.getDamageAgainstEntity` (decides by entity class: `Plane`, `Driveable`, `LivingEntity`), `mixin/EntityNPCInterfaceMixin`; no API hook found
NPC vehicles are `LivingEntity`s, so they always take `damageVsLiving`.

Missing: vehicle and plane damage profiles for Flan-model NPCs, preferably through an API hook in `DamageStats`.

### Disable All Damage and minimum hit thresholds — MISSING

Reference: `mixin/MixinResistances` (`disableDamage`, `ArrowVulnerability`/`MeleeVulnerability`/`ExplosionVulnerability`), `mixin/MixinSubGuiNpcResistanceProperties` (ALPHA_29)
A per-NPC switch makes the NPC immune to all damage. Separate arrow/projectile, melee and explosion minimums drop any hit below the threshold (for example, armour only hurt by large calibres). Both are edited in the resistance menu, with hover text.

Target checked: `mixin/NpcArmorControlsMixin` (read-only sliders only); `Resistances` and `DataStats` in the Custom NPCs 1.20.1 jar (no `disableDamage` or threshold fields)
Not present.

Missing: total invulnerability, per-type minimum-damage thresholds, their NBT, and the editor controls.

### Flan melee counted as melee by NPC resistances — PARTIAL

Reference: `mixin/MixinResistances.applyResistance` (treats `EntityDamageSourceFlans.melee` as melee, `isProjectile()` as arrow, `isExplosion()` as explosion)
Flan gun-butt and knife hits use the NPC's melee resistance.

Target checked: `mixin/EntityNPCInterfaceMixin.wolffsmodnpcsArmorNotEditor`; Custom NPCs 1.20.1 `Resistances.applyResistance`; `../main/resources/data/flansmodultimate/damage_type/melee.json` (`message_id: melee`)
**Present:** Flan projectiles (via `is_projectile`) and explosions (`message_id: explosion`) hit the projectile and explosion resistances when armour authority is off.
**Missing:** Custom NPCs only treats `player`, `mob` or `npc` as melee, so Flan's `melee` damage type skips the melee resistance.

### AA reload cycle and vehicle/AA reload sounds — PARTIAL

Reference: `mixin/MixinDataInventory.setDriveableStats` (AA `reloadTime` → min/max delay, `shootDelay` → fire rate), `mixin/MixinEntityNPCInterface.reloadGuns` (`DriveableType.shootReloadSound`, `AAGunType.reloadSound`)
AA guns fire bursts at `ShootDelay`, pause for `ReloadTime` and play the reload sound. Vehicles play `ShootReloadSound` after a burst.

Target checked: `../main/java/com/flansmodultimate/common/types/EntityTypePropertySupport` (only `aa.getShootDelay()`), `properties/NpcTypeMapping.timing`, `mixin/EntityAIRangedAttackMixin` (fires continuously at the type's shot delay)
**Present:** native shot cadence.
**Missing:** the AA reload pause and the vehicle and AA reload sounds on NPC models.

### Vehicle knockback immunity — MISSING

Reference: `mixin/MixinDataInventory.setDriveableStats` (`resistances.knockback = 2F`)
Turning on vehicle stats makes the NPC immune to knockback.

Target checked: `properties/NpcTypeProperty` (no knockback entry), `mixin/EntityNPCInterfaceMixin.wolffsmodnpcsNativeKnockback` (forces full knockback, 1F, while armour authority is on)
NPC vehicles are knocked back like humanoids.

Missing: knockback resistance for vehicle, plane, mecha and AA models.

### Off-hand and dual-wielded Flan guns — MISSING

Reference: `mixin/MixinEntityNPCInterface.getGuns`, `animateFlanGun*` (main and off hand), `mixin/MixinDataInventory.setRangedStats` (stats combined over both guns), `mixin/MixinEntityAIAnimation` (`HUG` pose with an off-hand gun)
NPCs can carry a Flan gun in each hand. Both animate, stats are combined over both, and an off-hand gun gives a two-handed aiming pose.

Target checked: `combat/NpcEquipmentRangedGoal` (`MAIN_HAND` only), `combat/NpcItemAttacks.ranged`, `mixin/EntityNPCInterfaceMixin.wolffsmodnpcsEquipmentTick` (off hand only used for shields)
Only the main-hand weapon fires.

Missing: firing and stats for off-hand guns, and the pose for an off-hand gun.

### Gun model shoot, reload and melee animations on NPCs — PARTIAL

Reference: `network/FlanAnimPacket` (`FLAN_SHOOT`, `FLAN_RELOAD`, `FLAN_MELEE`), `mixin/MixinEntityNPCInterface.animateFlanGunShoot`/`Reload`/`Melee`, `flansmod/FlanUtils.doShootAnimation`/`doReloadAnimation`, `customnpc/SubGuiFlanAnimations` (Disabled / Only Shoot / Only Reload / Shoot & Reload, melee yes/no)
NPC-held Flan guns play their model animations: recoil, pump, hammer, casing ejection, reload and melee. Each part can be switched on or off per NPC, and reload sound and animation are tied together.

Target checked: `mixin/NpcEquipmentAnimationMixin`, `../main/java/com/flansmodultimate/common/guns/GunArmPoses.onShotFired` (`PacketGunShotPose` → `recordShot`, arm pose only), `../main/java/com/flansmodultimate/common/guns/EquipmentSupport.fire`; `GunAnimations.doShoot`/`doReload`/`doMelee` are only called for the local player and driveable guns
**Present:** arm and aim poses, muzzle-flash particles, firing and reload sounds, and the `WEAPON_ANIMATIONS` switch.
**Missing:** gun-model shoot, reload and melee animations for NPC holders, and separate shoot, reload and melee animation modes.

### Editable stats after import (one-shot import) — PARTIAL

Reference: `mixin/MixinGuiNPCInv` (Melee, Ranged, Armor and Vehicle toggles), `mixin/MixinDataInventory.import*` (ALPHA_28)
Turning a category from No to Yes copies equipment or vehicle stats into the NPC's normal stats once. After that they are ordinary editable values that persist and drive firing, so a builder can import a tank's stats and then adjust its damage, accuracy, speed or sound per NPC.

Target checked: `properties/NpcTypeProperties`, `NpcPropertyOverrides`, `client/NpcTypeControls`, `client/NpcRangedControls`, `client/NpcEquipmentControls`
**Present:** stats inherited live from the model and equipment, shown read-only in the editor.
**Missing:** per-NPC overrides on top of imported values. Inherited fields are locked, and the only escape is turning off a whole feature group (`TYPE_PROPERTIES`, `WEAPON_STATS`, `FLAN_SOUNDS`).

### Vanilla throwables from the projectile slot — PARTIAL

Reference: `mixin/MixinEntityNPCInterface.shootProjectile`/`shootThrowable`
With a potion, experience bottle, egg or firework in the NPC's projectile slot, the NPC spawns the real vanilla entity (splash effects, XP, chickens, fireworks), using Custom NPCs accuracy and an indirect arc.

Target checked: `combat/NpcItemAttacks.throwable`/`projectile` (held-item path only: potion, egg, snowball, ender pearl, trident; firework only as crossbow ammunition), `mixin/EntityNPCInterfaceMixin.wolffsmodnpcsFireFlanRound` (non-Flan projectile-slot items go back to Custom NPCs)
**Present:** native throwables when held as the weapon.
**Missing:** native entities for projectile-slot items, and any support for experience bottles or standalone fireworks.

### Stat import for Custom NPCs' own weapons — UNCERTAIN

Reference: `mixin/MixinDataInventory.setRangedStats` (CustomNPC guns and bullets, machine gun, crossbow, slingshot, musket, staff, throwing weapons, kunai)
Turning on ranged stats also fills in damage, speed, physics, trail and sound for CustomNPC+'s own ranged weapons.

Target checked: `combat/NpcEquipment.ranged`, `NpcWeaponAdapter`
The target only knows vanilla and Flan items plus the opt-in `NpcWeaponAdapter`. It wasn't verified whether Custom NPCs 1.20.1 still ships these weapon items or handles them natively.

Missing: possibly stat defaults for Custom NPCs' own weapons.

---

## AI, targeting and navigation

### Terrain mobility profiles — MISSING

Reference: `customnpc/VehicleMobilityProfile`, `TerrainMobilityRules`, `SubGuiVehicleMobility`, `mixin/MixinDataAI`, `MixinPathFinderTerrainMobility`, `MixinEntityNPCInterface.wolffsmod$profileSpeed`/`wolffsmod$applyTerrainSpeed`, `MixinSubGuiNpcMovement` (ALPHA_23)
Each NPC has a terrain profile: Legacy, Ground (maximum water depth), Watercraft (needs water under its whole footprint, minimum depth) or Amphibious. Land and water speeds are set in blocks per second. Path nodes are filtered server-side and water speed is controlled. Watercraft and amphibious profiles turn on swimming. It's edited under AI → Movement → Terrain... and saved in AI NBT.

Target checked: `mixin/*`, `properties/NpcTypeMapping` (only `worksUnderWater` → `CAN_DROWN`, `walkingSpeed`)
Not present. Ships and tanks path like humanoids.

Missing: terrain profiles, depth-limited path filtering, separate land and water speeds, and the editor.

### Line of sight through transparent blocks — MISSING

Reference: `customnpc/TransparentBlockLos`, `SubGuiTransparentBlockLos`, `mixin/MixinEntitySensesTransparentBlocks`, `MixinDataDisplay` (`WolffTransparentBlockLosMode`/`VisionLimit`/`FireLimit`), `MixinEntityAIRangedAttack` (`canFire`, `projectileOrigin`) (7.1.0)
Each NPC has a mode: Normal, See Through, or See + Fire Through. Separate limits from 0 to 64 set how many plants, leaves, webs, fences, glass, panes, bars, vines or ladders vision and fire may cross. The firing bypass only applies to Flan projectiles from NPC ranged attacks.

Target checked: `../src/npcs` and `src/main/java` searched; `combat/NpcEquipmentRangedGoal` uses `getSensing().hasLineOfSight`
Not present.

Missing: per-NPC transparent-block vision and firing rules, and their editor.

### NPC range limits and client sync — PARTIAL

Reference: `config/RangeConfig` (`MaximumNPCCombatRange`, `NPCViewDistance`, `NPCNavigationRange`, 16–512), `network/RangeConfigPacket`/`RangeConfigSyncHandler`, `mixin/MixinDataStats` (clamped on load), `MixinConfigMainRange128`, `MixinGuiNpcStatsRange128`, `MixinSubGuiNpcRangePropertiesRange128`, `MixinCustomNpcsTrackingRange128`, `MixinCombatHandlerTacticalRange128`, `MixinEntityAIAttackTargetTacticalRange128`
One server-side setting caps aggro and ranged-fire range at up to 512 blocks. It is synced to clients so editor field limits match, saved values are clamped to it on load, and it also sets CNPC tracking and navigation range.

Target checked: `../main/java/com/flansmodultimate/config/ModCommonConfig.flanNpcTrackingRange` (16–512), `ModClientConfig.flanNpcRenderDistanceMultiplier`, `NpcDistanceCommands`, `properties/NpcTypeMapping` (`RANGE` capped at 256); Custom NPCs 1.20.1 GUI (aggro 1–512, ranged range 1–256), `CustomNpcs.NpcNavRange`
**Present:** a configurable tracking range for Flan NPCs, a render-distance multiplier, native aggro up to 512, and navigation range through Custom NPCs' own config.
**Missing:** ranged firing range beyond 256, a server-enforced combat-range cap with client sync of the editor limits, and clamping on load.

### Target retention for tactical variants — UNCERTAIN

Reference: `customnpc/TacticalRangeHelper`, `mixin/MixinEntityAIRangedAttack.shouldExecute`
Surround, Hit & Run, Ambush, Stalk and Dodge NPCs keep a long-range target for an extra margin while their manoeuvre carries them beyond aggro range.

Target checked: Custom NPCs 1.20.1 `EntityAIRangedAttack.canUse` (keeps the target within `max(aggroRange, ranged range)`)
Target ranged AI has a different structure. It wasn't verified whether 1.20.1 tactical goals drop targets the same way.

Missing: possibly the extra margin per tactical variant.

### Ticking NPCs at the edge of loaded chunks — UNCERTAIN

Reference: `customnpc/CustomNpcTickActivation` (`EntityEvent.CanUpdate` within `NPCViewDistance`) (ALPHA_35)
Loaded NPCs inside the view distance keep ticking even without the 32-block loaded margin, so visible far-away battles don't freeze.

Target checked: `../src/npcs`, `src/main/java` (no equivalent)
In 1.20.1, entities only tick in entity-ticking chunks (simulation distance), so the mechanism differs. NPCs that are tracked up to 512 blocks but outside simulation distance probably still freeze.

Missing: possibly any way to keep visible but distant NPCs simulating.

### Progressive target search — MISSING

Reference: `mixin/MixinEntityAIClosestTargetProgressive`, `customnpc/ProgressiveTargetSelector`, `TargetSearchProfiler`, `config/TargetSearchConfig` (ALPHA_13, ALPHA_34)
Target acquisition searches distance bands (0–32, 32–64, 64–128, 128–256, 256–384, up to 512) on configurable intervals, staggered per entity. Outer bands scan shells only. The 0–32 band runs on a shared cadence so formations react together. Optional profiling logs counters.

Target checked: `../src/npcs` (no target-selection hooks)
Not present. Custom NPCs' default acquisition applies.

Missing: banded and staggered acquisition, synchronized nearby acquisition, and its config and profiling.

### Ground path search performance — MISSING

Reference: `customnpc/PathBlockCache`, `mixin/MixinNPCPathBlockCache`, `customnpc/AdaptivePathSearchRange`, `PartialPursuitReuse`, `PursuitReusePolicy`, `mixin/MixinEntityAIRangedAttack` (staggered 10-tick pursuit retry) (ALPHA_19, 21, 25)
Block reads are cached within a single search. The search radius adapts (destination distance plus a detour margin, minimum 32). An expensive partial pursuit path is reused for a short time, and pursuit retries are staggered. All are configurable under `NPC Path Performance`.

Target checked: `../src/npcs`, `combat/NpcEquipmentRangedGoal.tick` (calls `moveTo` every tick while out of range or line of sight)
Not present.

Missing: path read caching, adaptive search radius, partial-path reuse and retry staggering.

---

## Display, GUI and sounds

### Hurt-effect toggle — MISSING

Reference: `mixin/MixinDataDisplay` (`DisplayHurt`), `mixin/MixinEntityNPCInterface.removeHurtEffectWhenTurnedOff`/`beforeDeathUpdate`, `mixin/MixinGuiNpcDisplay` (Display Hurt Effect button)
A per-NPC switch turns off the red hurt flash and, together with Hide Body When Killed, the death animation. Vehicles then don't turn red or tip over.

Target checked: `../src/npcs`; Custom NPCs 1.20.1 `DataDisplay` and `DataStats` (only `hideKilledBody`, no hurt-effect option)
Not present.

Missing: the hurt-flash switch and its editor button.

### Engine sound with range and loop length — PARTIAL

Reference: `mixin/MixinEntityNPCInterface.func_145780_a` (step-sound override: `PacketPlaySound` with `engineSoundRange`, re-armed by `startSoundLength`)
Moving vehicle NPCs play their engine sound over its authored range, no more often than the sound's length.

Target checked: `properties/NpcTypeMapping.sounds` (`Sound.STEP` → `NpcTypeProperty.STEP_SOUND`, Custom NPCs' step sound)
**Present:** the engine sound plays as the NPC's step sound.
**Missing:** the authored sound range and the gate that waits for the sound to finish. Custom NPCs uses its own step cadence and volume.

---

## Diagnostics and configuration

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
| Vehicle simulation | Gradual turret/seat aiming, limits, fire alignment, rotation sync | MISSING | HIGH |
| Vehicle simulation | Muzzle origin following turret rotation | PARTIAL | HIGH |
| Vehicle simulation | Advanced ballistic target leading | PARTIAL | MEDIUM |
| Vehicle simulation | Hull facing travel direction, misalignment throttle | MISSING | HIGH |
| Vehicle simulation | Riders at driver seat | MISSING | MEDIUM |
| Vehicle simulation | Eye height at primary shoot point | PARTIAL | LOW |
| Vehicle simulation | Persistent door/gear/wing/propeller/throttle flags | MISSING | MEDIUM |
| Vehicle simulation | Summoning a model entity creates an NPC | MISSING | MEDIUM |
| Rendering | Animated vehicle models | MISSING | HIGH |
| Rendering | Animated plane models | MISSING | HIGH |
| Rendering | Mecha walking animation | MISSING | HIGH |
| Rendering | Animated AA gun models | MISSING | HIGH |
| Rendering | Frustum-culling exemption | MISSING | MEDIUM |
| Rendering | Flan armour part scale and custom poses | UNCERTAIN | LOW |
| Combat | Damage vs vehicles/planes for NPC vehicles | MISSING | HIGH |
| Combat | Disable All Damage and minimum hit thresholds | MISSING | HIGH |
| Combat | Flan melee counted as melee by resistances | PARTIAL | MEDIUM |
| Combat | AA reload cycle and reload sounds | PARTIAL | MEDIUM |
| Combat | Vehicle knockback immunity | MISSING | MEDIUM |
| Combat | Off-hand and dual-wielded Flan guns | MISSING | MEDIUM |
| Combat | Gun model shoot/reload/melee animations | PARTIAL | HIGH |
| Combat | Editable stats after import | PARTIAL | MEDIUM |
| Combat | Vanilla throwables from projectile slot | PARTIAL | LOW |
| Combat | Stat import for Custom NPCs' own weapons | UNCERTAIN | LOW |
| AI/navigation | Terrain mobility profiles | MISSING | HIGH |
| AI/navigation | Line of sight through transparent blocks | MISSING | HIGH |
| AI/navigation | NPC range limits and client sync | PARTIAL | MEDIUM |
| AI/navigation | Target retention for tactical variants | UNCERTAIN | LOW |
| AI/navigation | Ticking at the edge of loaded chunks | UNCERTAIN | LOW |
| AI/navigation | Progressive target search | MISSING | MEDIUM |
| AI/navigation | Ground path search performance | MISSING | MEDIUM |
| Display/sounds | Hurt-effect toggle | MISSING | HIGH |
| Display/sounds | Engine sound with range and loop length | PARTIAL | MEDIUM |
| Diagnostics | Client and server benchmarks | MISSING | HIGH |
| Configuration | Per-pack model toggles | PARTIAL | LOW |

Totals: 20 MISSING, 11 PARTIAL, 4 UNCERTAIN.

## Areas requiring deeper audit

- **Flan armour on NPCs:** test per-part scaling, puppet poses and custom animations in game. Check what `CustomArmorLayer.copyPropertiesTo` leaves out compared with the reference's `applyCustomNpcFullModelTransform`.
- **Culling of large NPC vehicles:** decompile `RenderCustomNpc`/`RenderNPCInterface` in full and look for a culling-box override, then test a long ship at the edge of the screen.
- **Tactical variants and ticking at range:** the AI and chunk-ticking models in 1.20.1 differ enough that both findings need in-game reproduction before porting.
- **Custom NPCs' own weapons:** check which CNPC weapon items Custom NPCs 1.20.1 still ships and what stats it gives them.
- **Rendering performance:** the reference's 1.7.10 display-list cache, group culling and track-frame selection were not reported, because the target has its own caching, level-of-detail and impostor path (`FlansModelPreviews.renderWorld`). Once animated models are added (see Rendering), recheck that caching still works with moving turrets, wheels and tracks.
- **Advanced target leading:** comparing hit rates against strafing and turning aircraft would show whether the missing smoothing, acceleration and caps matter in practice.
