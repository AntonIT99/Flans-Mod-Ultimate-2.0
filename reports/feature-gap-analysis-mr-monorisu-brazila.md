# Feature Gap Analysis — Mr-Monorisu-Brazila (Labjac/TaP fork) → Flan's Mod Ultimate 2.0

- **Reference:** `C:\Users\alpha\Documents\Minecraft-Development\Mr-Monorisu-Brazila-master`. This is the newest Labjac Flan's fork: Minecraft 1.7.10, "Flan's LabCat Mod — TACZ hitreg experiment", including the Dill-branch features listed in `CHANGELOG_DILL.md`. It bundles the `com.hfr` (HBM/Clowder) mod and the `flanshorizons` source set.
- **Target:** `C:\Users\alpha\Documents\Minecraft-Development\Flans-Mod-Ultimate-2.0`, `master` @ `55b5aed88`.
- **Date:** 2026-10-09. The comparison runs one way only, from the reference to the target. Functionality that only the target has, refactors, and modernisations are not reported.
- **Prior report:** `feature-gap-analysis-krishna-mk6c.md` analysed the superseded Krishna Mk6C fork. Every finding below was re-checked against the current target. Several earlier gaps are now partly closed: vehicle per-seat optics, white-hot vehicle thermal, the optics-HUD rangefinder, HEAT, lifetime limits and voting.

Evidence roots:

- `R/` = reference `../src/main/java/com/flansmod`
- `T/` = target `../src/main/java/com/flansmodultimate`

**Method:** I extracted every `split[0].equals(...)` key from the reference parsers (1,498 keys). I flagged the keys that appear nowhere in target string literals, case-insensitively (709 keys) and grouped them into features. I traced each feature through reference call sites, then searched the target for direct, renamed and newer-mechanism equivalents. A missing key alone was never treated as proof: each finding below also had its behaviour checked. Systems that are not driven by config (packets, client handlers, commands, game types) were compared separately.

---

## Optics, thermal and fire control

### Thermal vision on guns and scope attachments — MISSING

Reference: `R/common/guns/GunType` and `AttachmentType` (`HasThermalVision`, alias `HasThermal`), `R/client/FlansModClient` (`thermalShader`), `config_txt/new_config_reference.txt`
When a player aims down a gun's default scope, or a scope attachment, that has `HasThermalVision`, the generation-3 white-hot thermal shader and the hot-entity overlay switch on. This is separate from `AllowNightVision`/`HasNightVision`, which give vanilla Night Vision.

Target checked: `T/client/render/VehicleThermalRenderer`, `T/client/render/VehicleOpticsClient.thermal()`, `T/common/types/GunType`, `AttachmentType`
Thermal only turns on through `VehicleOpticsClient.thermal()`, which requires an occupied seat with a thermal sight. Neither gun nor attachment types parse a thermal key.

Missing: thermal imaging while aiming hand-held guns and scope attachments.

### Thermal palettes, generations and hot-object coverage — PARTIAL

Reference: `R/client/FlansModClient`, `R/client/particle/ThermalParticleRenderer`, `R/client/ThermalTeamStrobe`, `R/common/driveables/VehicleType` (`ThermalVisionColor`, `ThermalVisionGeneration`/`thermalVisionGen`), and the "Additional Thermal Hot Particles" client config
Packs can choose a white-, green- or red-hot palette and image generation 1–3, which differ in blur, noise and CRT-style motion. Hot overlays cover players, mobs, vehicles, planes, bullets, grenades, explosions, muzzle flashes and configured particles, and the config can add particles from other mods. During minigames a thermal IFF strobe marks team-mates.

Target checked: `T/client/render/VehicleThermalRenderer` ("Depth-tested white-hot FLIR"), `T/common/driveables/VehicleOptics.thermal(int)`
**Present:** per-sight white-hot vehicle FLIR (`ThermalGuis`/`thermalSight`) that renders `LivingEntity` and `Driveable` hot, including distant driveables.
**Missing:** the green- and red-hot palettes, generation-based image quality, hot projectiles, grenades, explosions, muzzle flashes and particles, the extra-hot-particle config, and the team IFF strobe.

### Ballistic fire-control solution — PARTIAL

Reference: `R/common/driveables/DriveableType` (`Rangefinder`, `FCSMode Off|AdjustingSight|AdjustingBarrel`, `FCSMaxRange`, `FCSDropScale`, `FCSHorizontalDrag`/`FCSVerticalDrag`, `FCSBarrelMaxCorrection`, `FCSWeapon`, the `FCS*Pos/Scale/Color/Label/Marker[Sight]` family), `R/client/TickHandlerClient`, `R/client/gui/GuiDriveableController`
The rangefinder lases the nearest block or hitbox and ignores the operator's own vehicle. The FCS then works out a firing solution from the loaded shell's speed, gravity, drag and spread and the muzzle-to-target offset. `AdjustingSight` moves an impact marker to the solution. `AdjustingBarrel` moves the physical barrel within `FCSBarrelMaxCorrection`. Main and coaxial guns get separate solutions, and missiles and manually guided rounds are excluded. The HUD has labels, digital fonts and live editing.

Target checked: `T/client/render/VehicleOpticsHud` (`measureRange`, `requestRange`), `T/common/driveables/OpticsHud` (Range/Speed/Traverse/Compass/Elevation elements, `requireRangeKey`, `maxRange`), `T/client/gui/OpticsHudEditorScreen`, `T/client/distant/DistantRangefinder`
**Present:** manual or automatic range display (it can also measure Distant Horizons terrain), configurable optics-HUD elements, per-sight colour and scale, and a live HUD editor.
**Missing:** any ballistic computation, the adjusting-sight impact marker, adjusting-barrel correction, separate coax solutions, `FCSDebug` ballistic readouts, and the reference's `Rangefinder`/`FCS*` key names. None of the reference's FCS keys parse.

### Tank turret and sight stabilisation — MISSING

Reference: `R/common/driveables/EntitySeat` (`turretStabilization`, `playerLookingStabilized`, `toggleTurretStabilizer`, `stabilizedRenderView`), `R/common/driveables/VehicleType` (`TurretStabilization`), `R/client/KeyInputHandler` (toggle key H), `FCSStabilizerPos/Scale[Sight]`, `config_txt/fcs_stabilizer_configs.txt`
A stabilised seat keeps the gunner's intended aim fixed in world space while the hull turns and pitches. The physical turret catches up at its traverse speed and the sight stays on target. The feature can be toggled and shows a green or red `STA` indicator. Turret and barrel rendering is interpolated for display only, and an F10 diagnostic is available.

Target checked: `T/common/entity/Seat` (`applyClientAimDelta`, `getHullAimYaw`, `usesAbsoluteViewYaw`)
Vehicle seat aim is stored relative to the hull or turret, so it turns with the vehicle. World-yaw aim is used only for a mecha driver's torso (`usesAbsoluteViewYaw`). There is no stabiliser key, toggle or indicator.

Missing: world-space stabilised aim for vehicle turret seats, its toggle key and STA HUD, and the `TurretStabilization` definition key.

### Helicopter seat stabiliser and hover hold — MISSING

Reference: `R/common/driveables/DriveableType` (`AddHeliStabilizerToSeat`), `R/common/driveables/EntitySeat.isHeliStabilizerEnabled`, `R/common/driveables/EntityPlane.heliHoverMode`, `R/common/driveables/FlightController` (hover-hold block near line 1397), `R/client/TickHandlerClient` ("HOVER ON")
In a helicopter, stabilised gunner and pilot optics keep their world aim within the gun's yaw and pitch limits. The pilot can toggle hover hold with the mode key: the helicopter holds the altitude it had when hold was engaged (vertical-speed command clamped to ±0.22) and damps horizontal drift. The HUD shows the state.

Target checked: `T/common/driveables/physics/HelicopterPhysics` (half-collective hover convention), `T/common/entity/Plane`, `T/common/entity/Seat`
Helicopters hover at mid collective, but nothing holds a target altitude and there is no toggle. Seat stabilisation is absent, as above.

Missing: the altitude-hold hover mode with its HUD text, and per-seat helicopter optics stabilisation.

### Picture-in-picture scopes — MISSING

Reference: `R/client/PictureInPictureEffect`, `R/client/PipOptics`, `R/client/ScopeRenderCompatibility`, `R/client/PipCalibrationCommand`, `GunType`/`AttachmentType` (`PictureInPicture`, `PictureInPictureReticle`, `PictureInPictureReticleScale`)
Scopes draw a magnified second camera view into the physical lens of the scope model, including thermal lenses, instead of zooming the whole screen. The renderer isolates the secondary camera from shader packs (OptiFine, Angelica), and a client command calibrates the lens live and exports config snippets.

Target checked: whole target (`pip`, `lens`, `PictureInPicture` searches); scope zoom in `T/common/types/IScope`/`GunType`
Scopes only use full-screen FOV zoom with overlays.

Missing: rendering the lens from a secondary camera, the PiP reticle and scale keys, and the calibration command.

### Missile and shell camera views — MISSING

Reference: `R/client/EntityCamera` (TV-guided and "magic artillery" views, `bullet.type.TVguided`, `bullet.type.starShell`), `R/common/guns/BulletType` (`TVguided`, `TPVdrone`, `manualSensitivity`), `R/common/driveables/DriveableType` (`hasMagicArtilleryMode`, `artilleryCalculator`, `walterCalculator`)
Firing a TV-guided missile or a TPV drone round moves the player's camera onto the projectile, which they steer from its own viewpoint. Artillery vehicles can switch to an overhead aiming view with range calculators.

Target checked: `T/network/server/PacketManualGuidance`, `T/common/entity/Bullet` (manual and laser guidance by the shooter's look ray), `T/client/ModClient` (`setCameraEntity` only restores the player)
Manual guidance steers along the shooter's line of sight, and the camera stays with the shooter.

Missing: projectile-mounted cameras (TV-guided and TPV drone views), the steering sensitivity setting, and the artillery camera and calculator modes.

---

## Weapons and recoil

### Infantry firing screen shake — MISSING

Reference: `R/common/guns/GunType` (`HasScreenShake`, `ScreenShakeStyle Clean|Sustained`, `ScreenShakeIntensity`, `CameraRecoil`, `ScreenShakeCameraKick`), `R/common/network/PacketFancyScreenShake`, `PacketShakeRecoil`, `R/common/guns/BulletType` (`powerShake`, `rangeShake`)
Firing a gun shakes the camera in Polati style. The shake is either clean or driven by sustained recoil, its intensity is set per gun, and there is an optional camera kick. Explosions also shake nearby players' cameras, scaled by power and range.

Target checked: `T/client/render/VehicleScreenShake`, `T/network/client/PacketDriveableScreenShake`, `T/config/ModClientConfig.vehicleScreenShake`, `T/client/render/item/GunItemRenderer` (`animShakeDistance` only moves the gun model)
**Present:** camera shake for driveable main and coax guns (`FancyScreenShake*`).

Missing: camera shake when infantry guns fire, and camera shake from explosions.

### Battlefield and sustained recoil model — MISSING

Reference: `R/common/guns/GunType` (`recoilElevator`, `sustainedelevator`, `firstShotRecoil`, `sustainedRecoilPitch`, `sustainedRecoilYaw`, `dillElevator`, `dillZoomModifier`)
This recoil model gives the first shot its own kick and makes recoil grow over a burst along a separate sustained curve, with an elevator term and a zoom modifier.

Target checked: `T/common/types/GunType` (`Recoil`, `RecoilYaw`, `RandomRecoil*`, `DecreaseRecoil*`, stance multipliers, `FancyRecoil`)
Both legacy recoil and FancyRecoil exist. The first-shot and sustained parameters do not.

Missing: first-shot recoil, sustained per-burst recoil growth, and the elevator and zoom modifiers.

### Directional melee and bayonets — MISSING

Reference: `R/common/guns/GunType` (`meleeLeft/Right/Up`, `Melee{Left,Right,Up}{Time,DamagePoint,DamageOffset}`, `AddLeftNode`/`AddRightNode`/`AddUpNode`, `spear`, `lance`, `canBlock`, `shootMelee`, `swordArmorPen`), `R/common/guns/AttachmentType` (`Bayonet`, `BayonetMeleeTime`), `R/common/network/PacketBlockerino`, `PacketBolterino`
A melee swing goes left, right or up depending on input. Each direction has its own path, timing and damage point. Some weapons can block, and a bayonet attachment turns a gun into a stabbing weapon.

Target checked: `T/common/types/GunType` (`MeleeDamage`, single `AddNode` melee path), `T/network/client/PacketGunMeleeClient`
There is one melee path only.

Missing: directional swings, blocking, bayonet attachments, and spear/lance behaviour.

### Ricochet, over-penetration and penetration feedback — PARTIAL

Reference: `R/common/guns/BulletType` (`ricochetSound`, `distantRicochetSound`, `overPenSound`, `penetrateSound`, `minorPenSound`, `overPenPenalty`, `lessOverpen`, `penDecay`), `R/common/driveables/DriveablePart` (lines 615 and 690)
Shells hitting armour play distinct sounds for a ricochet, a minor penetration, a full penetration and an over-penetration. An over-penetrating round does reduced damage (`overPenPenalty`), and penetration falls off with flight time (`penDecay`).

Target checked: `T/common/guns/ShootingHelper` (bounce/ricochet branch at line 349), `T/common/types/BulletType` (`PenetrationAt100m`, `PenetrationDecay`, `HEAT`)
**Present:** projectiles can bounce or ricochet, and penetration decays with distance.
**Missing:** per-outcome hit sounds, the damage penalty for over-penetration, and time-based penetration decay.

### Tracer beams and mixed ammunition belts — MISSING

Reference: `R/common/guns/BulletType` (`TracerBeam`, `TracerBeamColor/Length/Width/Alpha`, `AlternateBulletLoad`, `HasAlternateModel`, `AlternateModel`, `AlternateTexture`, `AlternateTracerBeam*`, `AlternateTrailParticles/Type/Count`), `R/client/particle/EntityTracerBeamFX`, `R/common/guns/EntityBullet` (synced alternate-round state)
A round can draw a 3D tracer beam with configurable colour, size and alpha. Every N-th round of an ammunition type can be an "alternate" round with its own model, texture, beam and trail particles, for example one tracer every five rounds. This works for infantry guns, vehicle guns, coax and passenger guns, and other clients see it.

Target checked: `T/client/particle/FmTracerParticle`, `T/common/entity/Bullet`, `T/common/types/BulletType`
Only sprite tracer particles exist.

Missing: the 3D tracer beam renderer and alternate-round belt sequencing with its model, texture, beam and particle overrides.

### Hold-to-throw grenades — MISSING

Reference: `R/common/guns/GrenadeType` (`HoldToThrow`, `OverhandThrowSpeedMultiplier`, `UnderhandThrowSpeedMultiplier`, `GrenadeGroundFriction`, `GrenadeAirDrag`, `GrenadeStopSpeed`, `CringeExplodeOnImpact`/`CringeDetonateOnImpact`, `Description`), `R/common/network/PacketGrenadeThrow`
With `HoldToThrow`, holding and releasing left mouse throws overhand and right mouse tosses underhand. The grenade is used up and spawned only on release, and the fuse starts at release. The throw packet is validated on the server. Each grenade sets its own rolling friction, air drag, stop speed and impact detonation.

Target checked: `T/common/types/GrenadeType` (`ThrowDelay`, `CanThrow`, `Sticky*`, `SpinWhenThrown`, `FlashBang*`, …), `T/common/entity/Grenade`
Grenades are thrown immediately with a single throw arc.

Missing: hold-and-release throwing, the underhand toss, throw-speed multipliers, separate ground friction, drag and stop-speed physics, and the impact-detonation aliases.

### Motion-sensor grenades — MISSING

Reference: `R/common/guns/GrenadeType` (`MotionSensor`, `MotionSensorRange`, `MotionSound`, `MotionSoundRange`, `MotionTime`)
A deployed grenade detects movement within range, plays a warning sound and triggers on what it detects.

Target checked: `T/common/types/GrenadeType`, `T/common/entity/Grenade`
Missing: motion-sensor detection and triggering.

### Suppression on near miss — PARTIAL

Reference: `R/common/guns/BulletType.suppression`, `R/common/network/PacketSuppression`, `FlyBySound`
Rounds passing close to a player suppress them (a screen effect) and play a per-ammunition fly-by sound.

Target checked: `T/common/entity/Bullet.playFlybyIfClose` (a generic fly-by sound within 5 blocks), the target network packets
**Present:** a generic bullet fly-by sound.
Missing: the suppression effect and per-ammunition `FlyBySound`.

### Projectile chunk loading — MISSING

Reference: `R/common/guns/ProjectileChunkManager`, `ShootableType.ProjectileChunkLoading`
Long-range projectiles take chunk tickets so they keep simulating beyond players' loaded areas.

Target checked: `T/platform/world/ChunkTicketPlatform` (used only by `TeamsManager`)
Missing: chunk tickets for projectiles in flight.

### Seekers: self-guided, radar-guided, anti-radiation and homing torpedoes — MISSING

Reference: `R/common/guns/EntityBullet` (around lines 418–500 and 656–690), `BulletType` (`selfGuided`, `seekerRange`, `radarGuided`, `antiRadiation`, `modernTorpedo`, `ASWminRange`, `scoutBullet`, `infiniteAngle`, `LockOnFuse`)
Fire-and-forget missiles find their own target after launch. Anti-radiation missiles home on radar emitters. Radar-guided missiles depend on the launcher. Modern torpedoes acquire a target only once they are in the water, can change depth, and use a seeker range scaled by the target's noise.

Target checked: `T/common/types/BulletType` (`LockOnTo*`, `MaxLockOnAngle`, `EnableSACLOS`, `ManualGuidance`, `LaserGuidance`, `Torpedo`)
Missiles lock on at the launcher, or are steered by SACLOS, manual or laser guidance. Legacy torpedoes run straight.

Missing: post-launch seeker acquisition, anti-radiation and radar-guided seekers, and torpedo homing and depth control.

### Proportional-navigation guidance — MISSING

Reference: `R/common/guns/ProportionalNavigation`, `R/common/network/PacketProNavState`, `R/client/ProNavDebugTrailRenderer`, `BulletType` (`ProportionalNavigation`, `ProNavGain`, `ProNavMaxTurnRate`, `ProNavTerminalRange`, `ProNavTerminalGain`, `ProNavDebugTrail*`), `config_txt/proportional_navigation.txt`
This is opt-in interception guidance for MISSILE rounds. PN commands the turn rate (gain clamped to 0–10, maximum turn per tick clamped to 0–90) and blends to a terminal gain near the target. The server is authoritative, and players tracking the missile receive a motion stream. There is an optional debug flight-path trail.

Target checked: `T/common/entity/Bullet` homing (pursuit using `TurningForce`/`LockOnForce`)
Missing: PN guidance, its motion sync, and the debug trail.

### Missile twirl and ground-skim profiles — MISSING

Reference: `BulletType` (`ATGMTwirl`, `ATGMTwirlSpeed`, `ATGMTwirlWobble`, `MissileTwirl*`, `ATGMGroundSkimTolerance`, `MissileGroundSkimTolerance`)
ATGMs and missiles spin around their flight axis with a wobble, and can fly close to the ground within a set tolerance.

Target checked: `T/common/entity/Bullet`
Missing: the visual and flight twirl, and the ground-skim height profile.

### Fly-over top attack — PARTIAL

Reference: `BulletType` (`FlyOverTopAttack`, `…AcquireRange`, `…Clearance`, `…Corridor`, `…DetonationWindow`, `…TurnRate`), `R/common/network/PacketTopAttackState`
The missile flies over the target at a set clearance and fires downward when the target passes through its corridor, as TOW-2B and NLAW do.

Target checked: `T/common/types/BulletType.isDoTopAttack` (`IsDoTopAttack`), `T/common/entity/Bullet` (climb-then-dive profile)
**Present:** Javelin-style dive top attack.
**Missing:** the fly-over-and-shoot-down mode and its parameters.

### Wire-guided missiles and the visible wire — MISSING

Reference: `R/common/guns/EntityGuidanceWire`, `BulletType.HasLine`
Wire-guided missiles draw a wire from the launcher to the missile, and guidance depends on it.

Target checked: `T/common/entity/Driveable.hasLineOfSight` (an unrelated LOS check)
Missing: the guidance-wire entity and its rendering.

### Naval ordnance: depth charges — MISSING

Reference: `R/common/guns/EntityBullet` (around line 1667; `type.depthCharge`, `activationDepth`, `TeamsManager.seaLevel`)
A depth charge sinks and detonates at its activation depth below the configured sea level.

Target checked: `T/common/types/BulletType` (`Torpedo`), `T/common/entity/Bullet`
Missing: depth-triggered detonation and a sea-level setting. (`navalMine` is a ghost key in the reference: it is parsed but never used.)

### Illumination (star) shells — MISSING

Reference: `BulletType.starShell`, `R/client/FlansModClient` (around line 1118), `R/client/EntityCamera`
Star shells light up the area below them while they descend.

Target checked: `T/client/ModClient` dynamic lights (`HasDynamicLight` projectiles at a flat level)
Missing: a dedicated slow-falling illumination round.

### HESH shell class — MISSING

Reference: `BulletType.Hesh`, `R/common/driveables/EntityDriveable` (around line 4606)
HESH rounds, like HEAT, cannot go through armour types that kinetic rounds can over-penetrate.

Target checked: `T/common/types/BulletType.heat` ("HEAT" key and armour-face protection against HEAT)
**Note:** HEAT is present.
Missing: the HESH flag and its penetration rule.

### Soft-target and vanilla-damage modifiers — MISSING

Reference: `BulletType` (`dynamicDamage`, `dynamicBulletDelay`, `vanillaDamage`), `R/common/guns/raytracing/PlayerHitbox` (line 604), `R/common/guns/ItemBullet` ("Soft-Target Damage")
Damage against living targets has its own multiplier, which the tooltip shows. `vanillaDamage` controls whether vanilla damage sources can hurt driveables.

Target checked: `T/common/types/BulletType`, `T/common/types/ShootableType`
Missing: the soft-target damage multiplier and the per-driveable vanilla-damage gate.

### glTF skeletal gun animation — MISSING

Reference: `GunType` (`UseGLTFAnimation`, `GLTFAnimation`, `Use{Left,Right}ArmGLTFAnimation`, `TacticalReloadGLTFAnimation`, `Start/End/LoopReloadAnimation`, `ShootAnimation`, `AimingShootAnimation`, `SwitchAnimation`, `Secondary*Animation`, `ActionAnimation`)
Guns can be animated from glTF clips: reload start, loop and end, tactical reload, shooting, switching and arm animations.

Target checked: `T/common/types/GunAnimationConfig` (legacy model animation, staged-reload rendering)
Missing: glTF animation loading and playback.

### Tactical reload timing and secondary-mode switch animations — MISSING

Reference: `GunType` (`TacticalReloadTime`, `StagedReloadTime`, `ForceStagedReload`, `SecondarySwitchTime`, `SecondaryUnSwitchTime`, `RunPosTime`, `RunCrouchTime`), `AttachmentType` (`RefundAmmoOnSwitch`, `UnSwitchAnimationOnEmpty`, `RenderOnlyOnMode`, `AimingTimeMultiplier`)
A reload with a round still chambered is faster. Switching to and from secondary mode takes time and plays an animation. Attachments can refund ammo when switched and render only in a chosen mode.

Target checked: `T/common/types/GunAnimationConfig` (staged-reload render keys), `T/network/server/PacketGunSecondaryMode`, `PacketGunSwitchDelay`
Missing: the shorter tactical reload, timed secondary switching, ammo refund on switch, and mode-gated attachment rendering.

### Gun carry limits and gun tiers — MISSING

Reference: `GunType` (`Heavy`, `sidearm`, `OldGun`, `RepeatingGun`, `labigunLimit`, `hasLabigunDelay`, `Tier`), `ArmourType` pouches (`hasGunPouch`, `hasHeavyPouch`, `hasOldGunPouch`, …)
Players may carry only a limited number of guns per class (heavy, sidearm, old). Armour pouches raise those limits, and tiers drive the gun economy.

Target checked: `T/common/item/GunItem`, `T/common/types/GunType`, `ArmorType`
Missing: carry-limit enforcement, pouches and the tier field.

### Ammunition evolution — MISSING

Reference: `BulletType` (`Evolution`, `PreEvolution`)
Ammunition items can be upgraded into a linked type.

Target checked: `T/common/types/BulletType`
Missing: the evolution chain.

### Muzzle and chamber smoke models — PARTIAL

Reference: `GunType` (`MuzzleSmokeModel`/`Texture`, `ChamberSmokeModel`/`Texture`, `muzzleParticle`, `muzzleParticleCount`, `muzzleOffset`)
Guns can draw 3D smoke models at the muzzle and chamber, and spawn named particles at the muzzle.

Target checked: `T/network/client/PacketGunMuzzleFlash`, `T/client/render/item/GunItemRenderer`, the flash-model support in `GunType`
**Present:** muzzle flash models and particles.
**Missing:** the muzzle and chamber smoke model and texture keys.

---

## Armour and the player damage model

### Per-zone armour and armour penetration — MISSING

Reference: `R/common/teams/ArmourType` (`headArmor`, `faceArmor`, `neckArmor`, `napeArmor`, `chestArmor`, `backArmor`, `armArmor`, `legArmor`, `bodyArmor`, …), `ShootableType` (`headPen`, `bodyPen`, `armPen`, `bodyArmorPen`, `dynamicBodyArmorPen`), `R/common/guns/raytracing/PlayerHitbox`
Each armour piece protects specific hitbox zones, and each round has its own penetration per zone.

Target checked: `T/common/types/ArmorType`, `T/common/guns/ShootingHelper`
Missing: zone coefficients and per-zone penetration.

### Reserve armour plates and energy shields — MISSING

Reference: `ArmourType` (`Reserve{Head,Face,Body,Arm,Leg,Back,Nape}Armor`, `forceField`, `rechargeDelay`, `rechargeTimer`, `RechargeSound`, `ShieldKillSound`, `WarningSound`, `damageLimit`, `BackupDefence`), `BulletType.SwordEnergy`
Armour carries reserve plates that absorb damage first. Shields recharge after a delay, warn when low and play a sound when broken.

Target checked: `T/common/types/ArmorType`, `T/common/types/MechaItemType` (mecha force field only)
Missing: player armour plates and player energy shields.

### Bleeding and field medicine — MISSING

Reference: `R/common/tools/ToolType` (`bandAid`, `superBandAid`, `surgery`, `transfusion`, `needle`, `HealStrength`), `ShootableType.bleedMultiplier`, `R/common/network/PacketVaccine`, `CommandTeams bleeding`
Wounds make players bleed, and medical tools stop the bleeding or heal.

Target checked: `T/common/types/ToolType`, `T/common/types/GrenadeType.HealAmount`
Missing: the bleeding state and the medical tool classes.

### Armour weapon mounts, pouches and attributes — MISSING

Reference: `ArmourType` (`hasTopMount`, `hasFrontMount`, `topMount*`, `frontMount*`, `pouchMultiplier`, `reloadMultiplier`, `gasmaskable`/`smokeProtectable`)
Armour can show stowed guns, speed up reloads and protect against gas or smoke.

Target checked: `T/common/types/ArmorType`
Missing: weapon mounts, the reload multiplier and gas or smoke protection.

---

## Vehicles, aircraft and mechas

### Destroyed-vehicle wrecks — MISSING

Reference: `R/common/driveables/EntityDriveableWreck`, `DriveableType` (`LeaveWreck`, `WreckModel`, `WreckTexture`, `WreckSmoke`, `WreckLifetime`, `WreckRendersFancyTracks`)
A destroyed vehicle leaves a smoking wreck entity, with its own model and texture, that lasts for a set time.

Target checked: `T/common/driveables/DriveableCrashExplosion`, `T/common/entity/Driveable` (death sequence)
Missing: a persistent wreck entity.

### Ammo-rack and weak-spot cook-off — PARTIAL

Reference: `R/common/driveables/EntityVehicle` (lines 245–275 and 1727–1830; `getAmmoRackCookoffDamage`, `killDriverForAmmoRack`, `spawnFancyAmmoRackCookoff`), `VehicleType` (`WeakspotCookTime`, `fancyAmmoRackCookoff`), `PlaneType.OilCookTime`
When a `turretWeak`/`weakSpot` hitbox is destroyed while the turret or core still has health, the driver is killed once. The turret or core then burns down over `WeakspotCookTime`, with small explosions and a flare column. With `fancyAmmoRackCookoff`, a full flame, smoke and debris column plays.

Target checked: `T/common/driveables/EnumDriveablePart` (`TURRET_WEAK`, `TURRET_WEAK_2`, `WEAK_SPOT`…`WEAK_SPOT_3`)
**Present:** the weak-spot part names parse as damageable parts.
**Missing:** the cook-off sequence (killing the driver, timed turret or core burn-down, effects), plus `WeakspotCookTime` and `fancyAmmoRackCookoff`.

### Vehicle weapon overheating — MISSING

Reference: `R/common/driveables/EntityDriveable` (line 2198; `overheatLimit`, `overheatPenalty`, `overheatSound`, `coolingBonus`)
Sustained fire builds heat. Past the limit the gun is locked out for a penalty time.

Target checked: `T/common/entity/Driveable`
Missing: weapon heat.

### Radar, stealth and target designation — MISSING

Reference: `DriveableType` (`hasRadar`, `hasPlaneRadar`, `digitalRadar`, `radarRange`, `radarRefreshDelay`, `radarPositionOffset`, `RadarDetectableAltitude`, `radarVisible`, `OnRadar`, `Stealth`, `radarDetectionRangeMultiplier`), `BulletType.missileRadarVisible`
Vehicles with radar see contacts on a radar HUD. Stealth, altitude and terrain reduce how far a vehicle can be detected.

Target checked: `T/common/driveables/EnumDriveablePart` (radar hitbox parts only), `T/client/distant/DistantContactsClient`
Missing: a radar HUD and detection model.

### Active protection systems and CIWS — MISSING

Reference: `DriveableType` (`hasAPS`, `APSdelayMax`), `VehicleType` (`HasPassiveAPS`, `PassiveAPSCanForceAim`), `BulletType` (`BypassPassiveAPS`, `PassiveAPSECCM`, `CIWSable`, `CIWSer`, `ciwsBullet`, `APSsound`), `R/common/network/PacketPassiveAPS`, `R/client/particle/EntityAPSGrenade`
Hard-kill APS destroys incoming missiles and then needs time to reload. Soft-kill APS can throw off a missile's aim. CIWS rounds shoot down munitions marked `CIWSable`.

Target checked: whole target
Missing: all of these.

### Remote-controlled drones and quadcopters — MISSING

Reference: `R/common/driveables/EntityRemoteDronePlane`, `ItemRemoteDroneController`, `QuadcopterPhysics`, `RemoteDroneChunkManager`, `R/client/RemoteDroneClientControl`, `PacketRemoteDrone{Action,Control,WeaponState}`, `PlaneType` (`RemoteDrone*`, `Quadcopter*`, `DroneFlightMode`), `BulletType` (`RotorcraftDrone*`, `Drone*`, `TPVdrone`, `DroneDropped`), `config_txt/quadcopter_flight.txt`
A controller item flies a real aircraft remotely while the operator stays on foot. It has an FPV feed with static, a choice of quadcopter, helicopter or plane flight models, and a defined behaviour on signal loss (hover, return or self-destruct). FPV munition drones and droppable munitions are also supported.

Target checked: `T/apocalyse/common/entity/SkullDroneEntity` (a hostile mob), the target tool types
Missing: remote piloting, the drone flight model and FPV drone munitions.

### Troop transport — MISSING

Reference: `DriveableType` (`transport`, `troopType`, `troopCapacity`, `deployTroopSound`, `remountTroopSound`, `mobileInfantry`)
Vehicles carry and deploy NPC troops.

Target checked: `T/common/entity/Driveable`
Missing: troop transport.

### Submarine diving and crew oxygen — MISSING

Reference: `VehicleType` (`canDive`, `DiveSpeed`, `SurfaceSpeed`, `seaLevel`), `DriveableType` (`oxygen`, `maxOxygen`, `unlimitedOxygen`), `R/common/driveables/EntityVehicle` (around line 2170), `R/client/TickHandlerClient` (oxygen HUD)
Submarines control their depth and use up oxygen while submerged, which the HUD shows.

Target checked: `T/common/entity/Driveable` (`MaxDepth`, `WorksUnderWater`), `T/common/driveables/physics/EpicShipPhysics`
Missing: controlled diving and surfacing, and the oxygen budget.

### Submarine-launched ballistic missiles and VLS timing — MISSING

Reference: `DriveableType` (`slbmDelay`, `slbmFlightType`, `slbmRange`, `slbmStrength`, `slbmWarheadType`), `BulletType.VLSTime`
Ballistic missiles launched from submarines follow a configured flight profile.

Target checked: `T/common/types/BulletType` (`VLS`, `HasDeadZone`)
Missing: SLBM launch profiles and `VLSTime`.

### Hardpoints, ordnance weight and carrier operations — MISSING

Reference: `DriveableType` (`hardpoint`, `weightLimit`, `parkingSpot`, `helipad`), `PlaneType` (`carrierLandable`, `helipadLandable`, `carrierWingFlip`, `needsGear`, `parasitePlane`), `BulletType.missileWeight`, `R/common/network/PacketMissileWeight`, `R/common/guns/ItemBullet` (weight tooltip)
The weight of loaded ordnance affects aircraft performance. Aircraft can dock on carriers or helipads within a weight limit, and parasite planes are supported.

Target checked: `T/common/entity/Plane`, `T/common/types/PlaneType`
Missing: ordnance weight, carrier and helipad docking, and parasite aircraft.

### Animated wing flaps and wheel covers — MISSING

Reference: `PlaneType` (`WingFlapPosition1/2`, `WingFlapRotation1/2`, `WingFlapRate`, `WingFlapRotRate`, `WheelCover*`, `GearCoverPriority`)
Extra animation channels move flaps and gear-bay covers between two poses.

Target checked: `T/common/types/PlaneType` (wings, gear and doors)
Missing: the flap and wheel-cover channels.

### Afterburner — MISSING

Reference: `R/common/driveables/EntityPlane` (lines 537, 2086 and 3039; `hasAfterBurner`, `afterBurnFuelPenalty`, `afterburnOffBonus`, `AfterburnWing`/`AfterburnWingFlipped`, `particleAfterBurn`, `afterBurnName`)
The pilot toggles an afterburner, which adds thrust, burns extra fuel, can sweep the wings and emits afterburner particles.

Target checked: `T/common/entity/Plane` (afterburn particles only for Valkyrie foot exhaust)
Missing: the afterburner toggle and its thrust and fuel effects.

### Altitude-dependent aircraft performance and Labjac flight tuning — MISSING

Reference: `PlaneType` (`flightCeiling`, `highAltMax`, `highAltMaxDry`, `maxSpeedDry`, `cruiseSpeed`, `climbRate`, `maxG`, `turnTime`, `pitch/roll/yaw{Bonus,Boost,Stall}`, `stallSuffering`, `planeDiveFactor`, `heliSpeedLimit`), `VehicleType.maxAltitude`
Aircraft have a service ceiling, an altitude-dependent top speed, G and turn limits, and stall penalties on each axis.

Target checked: `T/common/driveables/physics/AircraftPerformancePhysics` (constant `AIR_DENSITY`, soft stall)
Missing: an altitude or ceiling model, and those tuning keys.

### Helicopter and aircraft crash presentation — PARTIAL

Reference: `PlaneType` (`HeliCrashEffects`, `HeliCrashSmokeHealth`, `HeliCrashSpin`, `HeliCrashSpinStrength`, `HeliCrashSound`, `PlaneCrashSound`, `CrashSoundRange`), `R/common/driveables/FlightController` (`isHeliCrashing`, descent clamp −0.16), the tail-rotor hiding in `RenderPlane`
A crashing helicopter trails smoke and fire from the engine and airframe, spins, descends more slowly, hides its tail rotor and plays a crash sound that stops on impact.

Target checked: `T/common/entity/Plane` (`SpinWithoutTail`, `PlaneCrashDamage`), `T/common/driveables/DriveableCrashExplosion`
**Present:** spin without a tail, and the crash explosion and fire.
**Missing:** the crash smoke and fire threshold, spin strength, slower crash descent, tail-rotor hiding, and crash sounds.

### Distant and situational vehicle audio — MISSING

Reference: `DriveableType` (`farSound`, `farSoundRange`, `DistantSoundPrimary`/`Secondary`, `DistantFlareSound*`, `TracksSound*`, `StukaSound*`, `sonicBoomSound`, `loudCannon`, `LegacyEngineSounds`), `VehicleType` (`DriftSound*`), `ShootableType` (`DistantDetonateSound*`), `R/client/CustomSoundLoop`
Vehicles play distant-fire and distant-detonation sounds, track and drift loops, Stuka dive sirens and sonic booms, with custom sound ranges.

Target checked: `T/common/types/GunType` (distant gun sounds for infantry and deployed guns)
**Note:** distant gun sounds exist for guns only.
Missing: distant sounds for driveable weapons and detonations, and track, drift, dive-siren and sonic-boom audio.

### Door-state passenger visibility — PARTIAL

Reference: `DriveableType` (`SetPlayerInvisibleOnDoorClose`/`Open`, `SetPassengerInvisible`, `invisiblePassenger`, `SetDriverInvincible`)
Whether occupants are visible depends on whether the doors are open, and the driver can be made invincible.

Target checked: `T/common/types/DriveableType` (`SetPlayerInvisible`)
**Present:** an unconditional invisibility flag.
**Missing:** visibility tied to door state, and the driver-invincible flag.

### Seat weapon groups and passenger particles — UNCERTAIN

Reference: `DriveableType` (`SeatGuns`, `SeatGunSpread`, `barrels`, `barrelSpread`, `barrelOffset`, `PassengerShootParticles`, `PassengerHasParticles`), the F10 passenger shoot-point debug
Passenger seats can fire several barrels with spread, with muzzle-particle origins per barrel.

Target checked: `T/network/client/PacketDriveablePassengerFired`, `T/client/render/DriveableMuzzleFlashes`, `temp/secondary-passenger-muzzle-particles.md`
Passenger muzzle effects exist. I could not confirm multi-barrel seat guns or barrel-spread semantics.

Missing (unconfirmed): multi-barrel passenger seat guns.

### Crew damage and part-loss effects — UNCERTAIN

Reference: `DriveableType` (`damageVsCrew`, `crewEngine`, `canPanic`, `engineLoss`, `wingLoss`, `tailLoss`, `hijackablePilot`), `GrenadeType` (`EngineGore`/`WingGore`/`TailGore`)
Hits can wound crew, and losing the engine, wings or tail changes how the aircraft handles.

Target checked: `T/common/driveables/VehicleHealthScaler`, `T/common/entity/Driveable` part logic
Part destruction exists. I did not confirm a crew-damage model or per-part loss scaling.

Missing (unconfirmed): crew wounding and part-loss handling penalties.

### Vehicle-launched smoke and turret grenades — MISSING

Reference: `DriveableType` (`autoSmoke`, `turretGrenade`, `turretTossTrue`, `smokeDelay`), `R/common/network/PacketVehicleGrenade`, `R/client/particle/EntitySmokeShell*`
Vehicles fire smoke grenades, automatically or on a key, from turret launchers.

Target checked: `T/client/particle/SmokeGrenadeParticle`, `T/network/client/PacketSmokeShell`
**Note:** smoke shell particles exist.
Missing: vehicle-mounted grenade launchers and automatic smoke.

### Mecha morale — MISSING

Reference: `MechaType` (`morale`, `panicSound`, `panicTime`, `runAmokSound`, `unpunchable`)
Mecha pilots can panic or run amok.

Target checked: `T/common/types/MechaType`
Missing: the morale system.

### Vehicle repair costs and new driving model — UNCERTAIN

Reference: `VehicleType` (`NewRepairSystem`, `canRepair`, `NewDrivingModel`/`nuDrivingModel`, `raceCar`, `driftMultiplier`, `terrainPenalty`), `ShootableType`/`GunType.repairCost`
Repairs have a cost, and an alternative driving model includes drift.

Target checked: `T/common/entity/Driveable` (repair), `T/common/driveables/physics/*` (target vehicle physics)
The target has its own physics stack, so the two may be partly equivalent. The repair-cost economy is absent.

Missing (unconfirmed): repair costs, plus drift and terrain penalties as configurable keys.

---

## Teams and game modes

### Conquest, Domination and Assault game types — MISSING

Reference: `R/common/teams/GameTypeConquest`, `GameTypeDomination`, `GameTypeAssault`, `capturepoint/`, `ITeamCapturePoint`, `TeamObjectiveSnapshot`, `TeamsHudSnapshot`, `PacketCapturePointEdit`
These are objective modes with capture points, ticket bleed and assault phases.

Target checked: `T/common/teams/GameTypes` (DM, TDM, CTF, Zombies)
Missing: these three modes and capture points.

### Deployment layouts and spawn selection — MISSING

Reference: `R/common/teams/DeployLayoutEntry`, `PacketDeployLayoutSave`, `PacketConquestSpawnSelect`, `DriveableType.DeploymentPoint`
Players pick where to spawn, including vehicles used as deployment points, and admins edit and save the layouts.

Target checked: `T/common/teams/TeamsManager`, `T/network/server/PacketTeamsAction`
Missing: spawn selection and deployment layouts.

### Class limits, prestige and victory presentation — MISSING

Reference: `PlayerClass` (`ClassLimit`, `ClassLimitPercent[age]`, `RequiredPrestigeLevel`), `CommandRanks` (`prestige`, `setRank`), `Team` (`VictorySound`, `DefeatSound`, `HasVictorySound`, `VictoryFlagTexture`), `PacketRankUp`, `PacketRoundFinished`
Each class has a player cap and can require a prestige level. Teams have victory and defeat sounds and a flag screen.

Target checked: `T/common/types/PlayerClass`, `T/common/types/Team`, `T/client/gui/TeamsRankIcon`
Missing: class caps, prestige gating, and the victory and defeat presentation.

### Map and rotation administration and spawn-rate limits — PARTIAL

Reference: `R/common/teams/CommandTeams` (`addMap`, `addMapWithBounds`, `removeMap`, `renameMap`, `setMapImage`, `addToRotation`/`removeFromRotation`/`listRotation`, `planeRate`/`vehicleRate`/`seatRate`, `raiding`, `pacifism`, `seaLevel`, `shake`, `rviForce`/`rviRate`, `movieProp`)
Admins manage maps and rotation from commands, set per-type spawn-rate limits, and toggle raiding, pacifism and other match rules.

Target checked: `T/common/command/TeamsCommand` (`add`, `remove`, `maps`, `rounds`, `useRotation`, `setvariable` with lifetime keys)
**Present:** adding and removing maps and rounds, rotation, lifetime limits and voting.
**Missing:** map bounds, renaming and images, spawn-rate limits, and the raiding, pacifism, sea-level, shake and RVI rule toggles.

---

## AI and NPCs

### Armed bot NPCs — PARTIAL

Reference: `R/common/entity/EntityFlansBot`, `FlansBotType` (`IQlevel`, `coverage`, `evil`, `CommonGun`/`RareGun`/`SuperRareGun`, `maxHealth`, `movementSpeed`), `ItemFlansBot`, `EntityAIFollowOwner`
Bot items spawn owned or hostile soldiers, which draw guns from rarity tables, use cover according to their IQ and follow their owner.

Target checked: `src/npcs/java/com/wolffsmod/npcs/combat/*` (ranged Flan's weapon attacks for NPCs), `T/apocalyse/common/entity/SurvivorEntity`
**Present:** NPCs that can use Flan's weapons.
**Missing:** the content-pack bot type, owner following, and the hostile and rarity loadout model.

### AI target aircraft — PARTIAL

Reference: `R/common/entity/EntityPlaneTarget`, `PlaneTargetType` (`Maneuver`, `ManeuverPeriod`, `Throttle`, `ControlStrength`, `PaintjobID`), `CommandPlaneTarget`, `EntityFlyByPlane`
A command spawns AI aircraft that fly manoeuvres for target practice.

Target checked: `T/apocalyse/common/entity/FlyByPlaneEntity` (an apocalypse-event autopilot fly-over)
**Present:** an autopilot flyby plane in the apocalypse feature.
**Missing:** the content-pack target-aircraft type, manoeuvre patterns and the spawn command.

---

## Client and networking

### First-person body rendering — MISSING

Reference: `R/client/virtualreality/AngelicaFirstPersonBodyCompat` and related, body hiding in vehicle optics
The player's own body is drawn in first person, and hidden while looking through vehicle optics.

Target checked: whole target
Missing: first-person body rendering.

### Iron-sight alignment tool — MISSING

Reference: `R/client/IronSightCalibrationCommand`, `GunType` (`IronSightOffset`, `IronSightPitch`, `ScopeAlignment`, `Xoffset`/`Zoffset`)
A client command adjusts first-person iron-sight alignment live, and per-gun keys store the offsets.

Target checked: `T/client/render/item/GunItemRenderer`
Missing: the alignment tool and the offset keys.

### Client performance governor and shot prediction — PARTIAL

Reference: `R/client/ClientPerformanceGovernor` (freezes fancy track links and limits effects under load), `R/client/ClientShotPrediction` (short-lived visual hit hints)
The client lowers visual load under pressure and shows predicted shot impacts before the server confirms them.

Target checked: `T/client/render/entity/DriveableImpostorCache`, track-link LOD, `T/common/driveables/DriveablePrediction` (vehicle movement prediction)
**Present:** LOD and impostor rendering for driveables, and vehicle movement prediction.
**Missing:** a load-driven governor for effects, and visual prediction of infantry shot impacts.

### Player-list ping display — PARTIAL

Reference: `R/client/ping/RenderPingHandler`
The tab list is replaced with one that shows numeric ping.

Target checked: `T/common/command/TeamsCommand ping` (`listPings`)
**Present:** a ping list from a command.
**Missing:** numeric ping in the tab overlay.

### Offhand gun selection — UNCERTAIN

Reference: `R/common/network/PacketSelectOffHandGun`, `PacketOffHandGunInfo`
Players pick a gun to dual-wield in the offhand.

Target checked: `T/client/input/KeyInputHandler`, `T/common/item/GunItem` (offhand handling)
The target uses the native offhand slot. I could not confirm whether the reference's selection workflow has an equivalent.

---

## Third-party integration and bundled mods

### Baris tech-tree and infrared/laser integration — MISSING

Reference: `R/common/BarisTechTreeHandler`, `AttachmentType` (`barisInfrared`, `barisLaser`), `BulletType.earlyInfrared`
This exports items to the Baris tech tree and adds infrared and laser attachment hooks.

Target checked: whole target
Missing: the integration.

### JourneyMap capture-point overlay — MISSING

Reference: `R/client/teams/JourneyMapCapturePointHook`, `R/client/JourneyMapPerformanceFix`
Capture points appear on the JourneyMap minimap.

Target checked: whole target (there are no capture points)
Missing: the minimap overlay.

### Flan's Horizons distant terrain — UNCERTAIN

Reference: `src/horizons/java/com/flanshorizons/*` (server terrain export, terrain pack datasets, chunked catalog and blob transfer, client GPU terrain LOD, fog and weather)
A companion mod exports terrain on the server, sends it to clients and renders distant terrain.

Target checked: `T/client/distant/*` (`DistantHorizonsClient`, `DhDistantTerrain`, `DistantBoxRenderer`, distant contacts)
The target delegates terrain LOD to the Distant Horizons mod and adds distant-vehicle rendering. The aim is equivalent, but the self-hosted export and transfer pipeline is not.

### Bundled HFR/Clowder faction and industry mod — MISSING

Reference: `src/main/java/com/hfr/*` (machines, missiles, factions/clowder, dimensions, schematics, RBMK and power, markets)
The reference jar also ships a separate faction and industry mod.

Target checked: whole target
This is not part of Flan's gameplay. I list it once for completeness and did not audit it mechanic by mechanic.

---

## Summary

| Subsystem | Feature | Status | Confidence |
| --------- | ------- | ------ | ---------- |
| Optics | Thermal on guns and attachments | MISSING | HIGH |
| Optics | Thermal palettes, generations and hot-object coverage | PARTIAL | HIGH |
| Optics | Ballistic fire-control solution | PARTIAL | HIGH |
| Optics | Turret and sight stabilisation | MISSING | HIGH |
| Optics | Helicopter seat stabiliser and hover hold | MISSING | HIGH |
| Optics | Picture-in-picture scopes | MISSING | HIGH |
| Optics | Missile and shell camera views | MISSING | HIGH |
| Weapons | Infantry firing and explosion screen shake | MISSING | HIGH |
| Weapons | Battlefield and sustained recoil | MISSING | HIGH |
| Weapons | Directional melee and bayonets | MISSING | HIGH |
| Weapons | Ricochet and penetration feedback | PARTIAL | MEDIUM |
| Weapons | Tracer beams and mixed belts | MISSING | HIGH |
| Weapons | Hold-to-throw grenades | MISSING | HIGH |
| Weapons | Motion-sensor grenades | MISSING | HIGH |
| Weapons | Suppression on near miss | PARTIAL | HIGH |
| Weapons | Projectile chunk loading | MISSING | HIGH |
| Weapons | Self-guided, radar and ARM seekers, homing torpedoes | MISSING | HIGH |
| Weapons | Proportional navigation | MISSING | HIGH |
| Weapons | Missile twirl and ground skim | MISSING | HIGH |
| Weapons | Fly-over top attack | PARTIAL | HIGH |
| Weapons | Wire-guided missile and wire | MISSING | HIGH |
| Weapons | Depth charges | MISSING | HIGH |
| Weapons | Illumination star shells | MISSING | HIGH |
| Weapons | HESH | MISSING | MEDIUM |
| Weapons | Soft-target and vanilla-damage modifiers | MISSING | MEDIUM |
| Weapons | glTF gun animation | MISSING | HIGH |
| Weapons | Tactical reload and secondary switch timing | MISSING | MEDIUM |
| Weapons | Gun carry limits and tiers | MISSING | HIGH |
| Weapons | Ammunition evolution | MISSING | MEDIUM |
| Weapons | Muzzle and chamber smoke models | PARTIAL | MEDIUM |
| Armour | Per-zone armour and penetration | MISSING | HIGH |
| Armour | Reserve plates and energy shields | MISSING | HIGH |
| Armour | Bleeding and field medicine | MISSING | HIGH |
| Armour | Weapon mounts, pouches and attributes | MISSING | HIGH |
| Vehicles | Destroyed-vehicle wrecks | MISSING | HIGH |
| Vehicles | Ammo-rack and weak-spot cook-off | PARTIAL | HIGH |
| Vehicles | Weapon overheating | MISSING | HIGH |
| Vehicles | Radar, stealth and designation | MISSING | HIGH |
| Vehicles | APS and CIWS | MISSING | HIGH |
| Vehicles | Remote drones and quadcopters | MISSING | HIGH |
| Vehicles | Troop transport | MISSING | HIGH |
| Vehicles | Submarine diving and oxygen | MISSING | HIGH |
| Vehicles | SLBM and VLS timing | MISSING | MEDIUM |
| Vehicles | Hardpoints, ordnance weight and carriers | MISSING | HIGH |
| Vehicles | Wing-flap and wheel-cover animation | MISSING | HIGH |
| Vehicles | Afterburner | MISSING | HIGH |
| Vehicles | Altitude performance and flight tuning | MISSING | MEDIUM |
| Vehicles | Helicopter and aircraft crash presentation | PARTIAL | HIGH |
| Vehicles | Distant and situational vehicle audio | MISSING | HIGH |
| Vehicles | Door-state passenger visibility | PARTIAL | HIGH |
| Vehicles | Vehicle smoke and turret grenades | MISSING | HIGH |
| Mechas | Mecha morale | MISSING | HIGH |
| Vehicles | Multi-barrel seat guns | UNCERTAIN | LOW |
| Vehicles | Crew damage and part-loss effects | UNCERTAIN | LOW |
| Vehicles | Repair costs and alternative driving model | UNCERTAIN | LOW |
| Teams | Conquest, Domination and Assault | MISSING | HIGH |
| Teams | Deployment layouts and spawn selection | MISSING | HIGH |
| Teams | Class limits, prestige and victory presentation | MISSING | HIGH |
| Teams | Map administration and spawn-rate limits | PARTIAL | HIGH |
| AI | Armed bot NPCs | PARTIAL | MEDIUM |
| AI | AI target aircraft | PARTIAL | MEDIUM |
| Client | First-person body rendering | MISSING | HIGH |
| Client | Iron-sight alignment tool | MISSING | HIGH |
| Client | Performance governor and shot prediction | PARTIAL | MEDIUM |
| Client | Tab-list ping display | PARTIAL | MEDIUM |
| Client | Offhand gun selection | UNCERTAIN | LOW |
| Integration | Baris tech tree and IR/laser | MISSING | HIGH |
| Integration | JourneyMap capture-point overlay | MISSING | HIGH |
| Integration | Flan's Horizons distant terrain | UNCERTAIN | LOW |
| Bundled | HFR/Clowder faction and industry mod | MISSING | HIGH |

**Totals:** 51 MISSING, 14 PARTIAL, 5 UNCERTAIN.

**Follow-up (same day):** these opt-in features were implemented after this analysis. The findings above describe the target before they were added.

- Infantry firing screen shake: `HasScreenShake`, `ScreenShakeStyle`, `ScreenShakeIntensity`, `CameraRecoil` and `ScreenShakeCameraKick`, plus the `gunScreenShake` client setting. Explosion shake is still missing.
- Tracer beams and mixed belts: `TracerBeam*`, `HasAlternateModel`, `AlternateBulletLoad`, `AlternateModel`, `AlternateTexture`, `AlternateTracerBeam*` and `AlternateTrailParticle*`.
- Hold-to-throw grenades: `HoldToThrow`, the throw-speed multipliers, `GrenadeGroundFriction`, `GrenadeAirDrag`, `GrenadeStopSpeed`, and the `Cringe*OnImpact` aliases.
- Thermal on gun and attachment scopes (`HasThermalVision`/`HasThermal`), and the vehicle `ThermalVisionColor` and `ThermalVisionGeneration` settings.
- Thermal hot coverage: flying rounds, grenades, and hot particles (explosions, fire, muzzle flashes, sparks, tracers, exhaust, Flan's blast smoke), plus the `additionalThermalHotParticles` client setting and the Teams IFF strobe. The thermal finding is now fully covered.

## Areas requiring deeper audit

- **Passenger and seat weapons.** The target has passenger fire packets and muzzle-flash handling. The reference's `SeatGuns`/`barrels`/`barrelSpread` semantics need a line-by-line comparison against `T/common/entity/Driveable` seat firing.
- **Crew and part-loss model.** The reference's `damageVsCrew`, `crewEngine` and `engineLoss`/`wingLoss`/`tailLoss` may partly overlap with the target's part-health scaling (`VehicleHealthScaler`) and its aircraft physics. I did not confirm this.
- **Vehicle driving model.** The target's physics stack (`T/common/driveables/physics`) is substantially different. The Labjac `NewDrivingModel`/drift/terrain-penalty behaviour needs a behavioural comparison rather than a key comparison.
- **Flan's Horizons versus Distant Horizons.** The two have equivalent aims through different mechanisms. Whether server-authoritative terrain distribution matters for this project is a product decision.
- **HFR bundle.** It is intentionally not audited.
- **Key-extraction blind spots.** Discovery relied on `split[0]` keys plus a sweep of client and network classes. Reference behaviour that is hard-coded, with no key, packet or class, could be missing from this list. The likeliest places are `R/common/driveables/EntityDriveable` (more than 5,000 lines), `EntityPlane` and `EntityBullet`.
