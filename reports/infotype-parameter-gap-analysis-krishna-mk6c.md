# InfoType Parameter Gap Analysis

| | |
|---|---|
| **Target** | `C:\Users\alpha\Documents\Minecraft-Development\Flans-Mod-Ultimate-2.0` (Flan's Mod Ultimate 2.0, `com.flansmodultimate`) |
| **Reference** | `C:\Users\alpha\Documents\Minecraft-Development\Flan's Mod Aryan Indian Edition Krishna Mk6C` (deobfuscated 1.7.10 fork, mcmod id `flansmod`, name "Flan's LabCat Mod") |
| **Target inventory** | 25 concrete/abstract `InfoType` classes, 4030 `.txt` definitions, 1041 distinct accepted keys |
| **Reference inventory** | 22 `InfoType` classes, **0** bundled `.txt` definitions, 1427 distinct accepted keys |
| **Analysis date** | 2026-09-10 |

## How the inventories were built

**Target.** `InfoType` (`../src/main/java/com/flansmodultimate/common/types/InfoType.java`) reads through `TypeFile`, which lower-cases every leading token into a `configMap` (`TypeFile.java:39-42`) — so all target dispatch is case-insensitive by construction. Keys enter the parser as literal first arguments to `TypeReaderUtils` helpers (`readValue`, `readValues`, `readResource`, `readIntValues`, `readSound`, `readValuesInLines`, …), to `TypeFile.hasConfigLine` / `hasAnyConfigLine` / `getConfigLines`, and to the local helpers `forEachLine`, `aliasInt`, `aliasFloat`, `aliasSound`, `scaledVector`, `modelVector`, `addEffects`, `resolveShootDelay`. Four delegated parsers contribute keys to the types that invoke them and were folded into their callers' effective key sets:

| Delegate | Contributes to |
|---|---|
| `common/driveables/armor/VehicleArmorSpecReader.java` | `DriveableType` |
| `common/driveables/physics/RealWorldSpecReader.java` | `DriveableType`, `AAGunType` |
| `common/guns/AmmoOverrides.java` | `InfoType` (all types) |
| `common/guns/RemovedAmmo.java` | `GunType`, `AAGunType`, `DriveableType` |
| `common/types/GunAnimationConfig.java` | `GunType` |

Those delegates hoist their key names into `static final String` constants (e.g. `RealWorldSpecReader.KEY_MASS = "RealMassKg"`), which the inventory resolves. `TypeFile.addCategoryConfigMap` injects the `properties` of `src/main/resources/config/*_categories.json` as additional config lines; all 79 distinct category property names resolve to keys already accepted by a parser, so the category system adds values, not new key names.

**Reference.** The fork is decompiled 1.7.10 Flan's Mod: `InfoType.read(String[] split, TypeFile)` dispatches on `split[0].equals("Key")` chains (`com/flansmod/common/types/InfoType.java:74-125`), with subclasses overriding `read`. The only other dispatch form is `DriveableType.isSeatOpticsHudConfig(String key)` / `readSeatOpticsHudConfig` (`com/flansmod/common/driveables/DriveableType.java:2002-2073`), which uses `key.equals(...)` and five `key.startsWith(...)` prefix families on the same `split[0]`. No `switch`, annotation, or reflection-driven key registration exists in the reference. **The reference ships no content packs** (its only `.txt` files are licences and `assets/lwep/texts/map.txt`), so its key set is parser-derived only and reference `GHOST_UNPARSED` cannot be determined.

---

## Target ghost parameters

### GHOST_PARSED — value is stored but never consumed

Each of the following is accepted by a target parser and assigned to a field that is **never read anywhere in `../src`** (main, test, or any of the five bundled pack source sets). Field name and Lombok accessors (`getX`/`isX`/`setX`) were all searched; total identifier occurrences are the declaration plus the assignment only.

#### DriveableType

- **`ExitSoundLength`** — `GHOST_PARSED`. `exitSoundLength = readSoundLength("ExitSoundLength", exitSoundLength, file);` (`common/types/DriveableType.java:710`) into `protected int exitSoundLength = 50` (`:197`). No reader; the paired `exitSound` string is consumed, the length is not.
- **`OnRadar`** — `GHOST_PARSED`. `onRadar = readValue("OnRadar", onRadar, file);` (`DriveableType.java:697`) into `protected boolean onRadar` (`:186`). The target has no radar subsystem at all: apart from `EnumDriveablePart`'s unrelated part name, `DriveableType.java` is the only file in `../src/main/java` that mentions "Radar".

### GHOST_UNPARSED — key occurs in target `.txt` definitions with no parser

These names appear as the leading token of a definition line in the target's own bundled content packs, but no parser for the owning type accepts them. Grouped by owning type; the `.txt` path is one representative occurrence.

#### PlaneType / VehicleType (legacy 1.7.10 driveable keys)

None of these literals occur anywhere in target Java:

| Key | Types | Representative definition |
|---|---|---|
| `Bounciness` | PlaneType, VehicleType | `src/manuspacks/.../planes/DropshipMk3_A_Desert.txt:32` |
| `MomentOfInertia` | PlaneType, VehicleType | `src/warfare44pack/.../vehicles/44_GMCTruck.txt:22` |
| `ClutchSteer` | VehicleType | `src/warfare44pack/.../vehicles/44_Marder2.txt:17` |
| `FlipLinkFix` | VehicleType | `src/warfare44pack/.../vehicles/44_ChiHa.txt:343` |
| `FloatOnLand` | VehicleType | `src/warfare44pack/.../vehicles/44_M4A1Sherman.txt:308` |
| `HasSmoke` | VehicleType | `src/warfare44pack/.../vehicles/44_PantherCommand.txt:292` |
| `RecoilDistance` | VehicleType | `src/warfare44pack/.../vehicles/44_Wespe.txt:265` |
| `SoundsPlaceTimePrimary` | VehicleType | `src/warfare44pack/.../vehicles/44_GMCTruck.txt:60` |
| `TurretRotationSpeed` | VehicleType | `src/officialpacks/.../ww2/definitions/vehicles/B1.txt:50` |

Note `Bounciness` *is* parsed by `ShootableType` (`:319`) and consulted by `GrenadeType` (`:148`), but not by `DriveableType` or its subclasses.

### Malformed definition lines — not parameters

The line-token inventory also flagged the following, which are content-authoring defects rather than parameters, and are excluded from the ghost counts: `B`, `LPA`, `MAM`, `MMM` (recipe-shape rows under a `//Recipe` comment with no `Recipe` keyword — `src/manuspacks/.../aaguns/WW2_AAGun_Flak20mmVierling_1A.txt:17-20`); `WW2_Missle_USAHVAR` (a bare ammo shortname missing its `AddAmmo` prefix — `src/manuspacks/.../planes/WW2_Plane_F4U_1A.txt:48`); `Add` (from `Add Dye 1 blue`, `src/officialpacks/.../mechas/AlphaTitan.txt:48`); `add` and `Dye` (uncommented prose — `src/warfare44pack/.../guns/44_20mmCannon.txt:28`, `src/manuspacks/.../vehicles/Boxer4.txt:73`).

---

## Reference ghost parameters

49 reference keys are accepted by a `split[0].equals(...)` branch and assigned to a field whose only two occurrences in the entire 2107-file reference corpus are its declaration and that assignment — no getter, no reader, no serialization. Representative verification: `GunType.showDamage` occurs only at `com/flansmod/common/guns/GunType.java:134` (declaration) and `:406` (`this.showDamage = Boolean.parseBoolean(split[1]);`); the same holds for every row below.

| Reference type | `GHOST_PARSED` keys | Destination field(s) |
|---|---|---|
| `ArmourType` | `FireResistance` | `fireResistance` |
| `AttachmentType` | `FlashlightStrength` | `flashlightStrength` |
| `BulletType` | `Bouncy`, `DroneHoverSignalThreshold`, `DroneNeutralForwardInput`, `DroneSideDrag`, `LockOnFuse` | `Bouncerino`, `droneHoverSignalThreshold`, `droneNeutralForwardInput`, `droneSideDrag`, `lockOnFuse` |
| `DriveableType` | `CollectHarvest`, `DropHarvest`, `EnableReloadTime`, `GunLength`, `Harvester`, `OnRadar`, `RadarDetectableAltitude`, `RecoilDistance`, `Stealth`, `energyGainRate` | `collectHarvest`, `dropHarvest`, `enableReloadTime`, `gunLength`, `harvestBlocks`, `onRadar`, `radarDetectableAltitude`, `recoilDist`, `stealth`, `energyRate` |
| `GrenadeType` | `MotionSensor`, `MotionSensorRange`, `MotionSound`, `MotionSoundRange` | `motionSensor`, `motionSensorRange`, `motionSound`, `motionSoundRange` |
| `GunType` | `AllowNightVision`, `CanShootUnderwater`, `RandomRecoilRange`, `RandomRecoilYawRange`, `ShowAccuracy`, `ShowAttachments`, `ShowDamage`, `ShowRecoil`, `ShowReloadTime` | `allowNightVision`, `canShootUnderwater`, `rndRecoilPitchRange`, `rndRecoilYawRange`, `showSpread`, `showAttachments`, `showDamage`, `showRecoil`, `showReloadTime` |
| `MechaItemType` | `FlameBurst` | `flameBurst` |
| `PlaneType` | `HasWing`, `ShootDelay`, `diveBonus`, `highAltMax`, `highAltMaxDry`, `maxSpeedDry`, `stallSuffering` | `hasWing`, `planeShootDelay`, `deathDiveSpeedLimit`, `speedLimitHigh`, `speedLimitHighDry`, `speedLimitDry`, `stallSpeedLimit` |
| `VehicleType` | `Door2Position1`, `Door2Position2`, `Door2Rate`, `Door2RotRate`, `Door2Rotation1`, `Door2Rotation2`, `DriftSound`, `DriftSoundLength`, `FlipLinkFix`, `FourWheelDrive`, `ShootDelay` | `door2Pos1`, `door2Pos2`, `door2Rate`, `door2RotRate`, `door2Rot1`, `door2Rot2`, `driftSound`, `driftSoundLength`, `flipLinkFix`, `fourWheelDrive`, `vehicleShootDelay` |

Two of these are worth calling out because they also appear on the target side: `OnRadar` is `GHOST_PARSED` in **both** codebases, and `RecoilDistance` is `GHOST_PARSED` in the reference while being `GHOST_UNPARSED` in the target — in neither mod does a definition setting them change behaviour.

The reference ships no content packs, so **reference `GHOST_UNPARSED` is not determinable** from this repository.

---

## Reference parameters absent from target

All 743 reference parameter names — drawn from the reference's parser branches, since the reference has no bundled definitions — that have **no case-insensitive match** in the target's combined key set (target parser names and aliases, including inherited and delegated parsers, plus every leading token observed in the target's 4030 bundled `.txt` definitions).

This list is **lexical parser/definition compatibility only**. Several entries have a semantically similar target behaviour under a different name (`VLSTime` ↔ target `DeadZoneTime`, `showReload` ↔ target `ShowReloadTime`, `RecoilDistance` ↔ target `RecoilDist`-family keys) — those still count as absent because the target does not accept the reference spelling as an alias. Conversely, no target-only keys appear here.

Two reference type classes have **no counterpart at all** in the target's `EnumType` registry, so every one of their keys is unreachable: `FlansBotType` (folder `bots`) and `PlaneTargetType` (folder `planeTargets`). The target's registry (`common/types/EnumType.java`) has no equivalent entries.

Five of the `DriveableType` entries are **prefix families**, not exact key names — `SeatOpticsCompass`, `SeatOpticsElevation`, `SeatOpticsRange`, `SeatOpticsSpeed`, `SeatOpticsTraverse` are matched with `key.startsWith(...)` at `com/flansmod/common/driveables/DriveableType.java:2017-2021`, so the reference additionally accepts any key beginning with those strings.

### DriveableType (inherited by MechaType, PlaneType, VehicleType) — 253 keys

Evidence: parsed by the reference's line dispatcher, e.g. `} else if (split[0].equals("AddHeliStabilizerToSeat")) {` at line 1484 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `AddHeliStabilizerToSeat` | `alwaysShowTurret` | `APSdelayMax` | `artilleryCalculator` |
| `autisticHitDetection` | `autoSmoke` | `barrelOffset` | `barrels` |
| `barrelSpread` | `bigDeath` | `CameraLerp` | `cannonRecoil` |
| `cannonRecoilCoax` | `canPanic` | `canSmallArms` | `canStab` |
| `carrier` | `centralControl` | `coaxRecoil` | `crewEngine` |
| `damageVsCrew` | `Death` | `DeploymentPoint` | `deployTroopSound` |
| `digitalRadar` | `DistantFlareSound` | `DistantFlareSoundRange` | `DistantSoundPrimary` |
| `DistantSoundSecondary` | `DriverOptics` | `DriverOpticsCamera` | `earRape` |
| `EnableReloadTime` | `energyGainRate` | `energyLossRate` | `engineLoss` |
| `enterable` | `evilGolem` | `evilGravity` | `evilRange` |
| `evilSpread` | `exitTimer` | `explosionPush` | `explosionResistance` |
| `FancyScreenShake` | `FancyScreenShakeCoaxDuration` | `FancyScreenShakeCoaxIntensity` | `FancyScreenShakePrimaryDuration` |
| `FancyScreenShakePrimaryIntensity` | `FancyScreenShakeRange` | `fancyShip` | `farSound` |
| `farSoundRange` | `FCSBarrelMaxCorrection` | `FCSColor` | `FCSColorSight` |
| `FCSDebug` | `FCSDebugPos` | `FCSDebugPosSight` | `FCSDebugScale` |
| `FCSDebugScaleSight` | `FCSDropScale` | `FCSEditMode` | `FCSHorizontalDrag` |
| `FCSLabel` | `FCSLabelPos` | `FCSLabelPosSight` | `FCSLabelScale` |
| `FCSLabelScaleSight` | `FCSMarker` | `FCSMarkerOffset` | `FCSMarkerOffsetSight` |
| `FCSMarkerScale` | `FCSMarkerScaleSight` | `FCSMarkerSize` | `FCSMaxRange` |
| `FCSMode` | `FCSRangePos` | `FCSRangePosSight` | `FCSRangeScale` |
| `FCSRangeScaleSight` | `FCSRequireRangeKey` | `FCSStabilizerPos` | `FCSStabilizerPosSight` |
| `FCSStabilizerScale` | `FCSStabilizerScaleSight` | `FCSStabPos` | `FCSStabPosSight` |
| `FCSStabScale` | `FCSStabScaleSight` | `FCSTool` | `FCSVerticalDrag` |
| `FCSWeapon` | `fuelTimer` | `GunLength` | `GunnerOptic` |
| `GunnerOpticCamera` | `Gunsight` | `GunsightPos` | `gunsightZoom` |
| `hardpoint` | `hasAfterBurner` | `hasAPS` | `HasGunsightPos` |
| `hasHUD` | `hasMagicArtilleryMode` | `hasPlaneRadar` | `hasRadar` |
| `hasScope` | `heliGUI` | `heliGuiSeat` | `helipad` |
| `hijackablePilot` | `hudColorB` | `hudColorG` | `hudColorR` |
| `InshaAllah` | `invincible` | `invisiblePassenger` | `isExplosionWhenDestroyedRadius` |
| `kamikazeBonus` | `labjacFuel` | `LaunchDelay` | `LaunchDelayPrimary` |
| `LaunchDelaySecondary` | `LeaveWreck` | `LegacyEngineSounds` | `lessOverpen` |
| `loudCannon` | `maxOxygen` | `MechStomp` | `missilePrimaryAuto` |
| `missileSecondaryAuto` | `mobileInfantry` | `ModelShitty` | `needsThrottle` |
| `nightScope` | `NightSight` | `nuclearDeath` | `OpticsCameraSeat` |
| `OpticsModeSeat` | `overheatSound` | `oxygen` | `parkingSpot` |
| `particleAfterBurn` | `particleTrailBurner` | `PassengerAutoScope` | `PassengerGunsight` |
| `PassengerGunsights` | `PassengerGunsightZoom` | `PassengerHasParticles` | `PassengerHasScope` |
| `PassengerNightSight` | `PassengerShootParticles` | `PassengerThermalGuis` | `passengerZoom` |
| `PilotOptics` | `PilotOpticsCamera` | `PilotOpticsHUD` | `PilotOpticsHUDEdit` |
| `PilotOpticsHUDMaxRange` | `PilotOpticsHUDRequireRangeKey` | `placeableOnPumpkin` | `planeCoaxPrimary` |
| `planeCoaxSecondary` | `primaryDoor` | `primaryRecoil` | `primaryRecoilStrength` |
| `primaryScreenShake` | `projectileMass` | `RadarDetectableAltitude` | `radarDetectionRangeMultiplier` |
| `radarPositionOffset` | `radarRange` | `radarRefreshDelay` | `radarVisible` |
| `Rangefinder` | `remountTroopSound` | `rocketThrottle` | `SeatAutoScope` |
| `SeatGuns` | `SeatGunsight` | `SeatGunsightZoom` | `SeatGunsightZooms` |
| `SeatGunSpread` | `SeatHasScope` | `SeatNightSight` | `SeatOpticsCamera` |
| `SeatOpticsCompass` | `SeatOpticsCompassMarker` | `SeatOpticsCompassMarkerOffset` | `SeatOpticsCompassMarkerSight` |
| `SeatOpticsCompassMarkerSize` | `SeatOpticsElevation` | `SeatOpticsElevationIndicator` | `SeatOpticsHUD` |
| `SeatOpticsHUDColor` | `SeatOpticsHUDColorSight` | `SeatOpticsHUDEdit` | `SeatOpticsHUDInherit` |
| `SeatOpticsHUDMaxRange` | `SeatOpticsHUDRequireRangeKey` | `SeatOpticsHUDScale` | `SeatOpticsHUDScaleSight` |
| `SeatOpticsMode` | `SeatOpticsRange` | `SeatOpticsSpeed` | `SeatOpticsTraverse` |
| `SeatOverlay` | `SeatThermalGuis` | `secondaryDoor` | `secondaryRecoil` |
| `secondaryScreenShake` | `SetDriverInvincible` | `SetPassengerInvisible` | `SetPlayerInvisibleOnDoorClose` |
| `SetPlayerInvisibleOnDoorOpen` | `showReload` | `showTurretIndicator` | `slbmDelay` |
| `slbmFlightType` | `slbmRange` | `slbmStrength` | `slbmWarheadType` |
| `solid` | `sonicBoomSound` | `Stabilizer` | `Stealth` |
| `StukaSound` | `StukaSoundLength` | `StukaSpeed` | `tailLoss` |
| `TextureShitty` | `ThermalGuis` | `ThrottleModifier` | `TracksSound` |
| `TracksSoundRange` | `transport` | `troopCapacity` | `troopType` |
| `turretGrenade` | `turretTossTrue` | `unlimitedOxygen` | `vanillaDamage` |
| `walterCalculator` | `walterMortar` | `weightLimit` | `wingLoss` |
| `WreckLifetime` | `WreckModel` | `WreckRendersFancyTracks` | `WreckSmoke` |
| `WreckTexture` |  |  |  |

### BulletType — 139 keys

Evidence: parsed by the reference's line dispatcher, e.g. `} else if (split[0].equals("activationDepth")) {` at line 470 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `activationDepth` | `aftermathFuse` | `AimTimeMultiplier` | `AlternateBulletLoad` |
| `AlternateModel` | `AlternateTexture` | `AlternateTracerBeam` | `AlternateTracerBeamAlpha` |
| `AlternateTracerBeamColor` | `AlternateTracerBeamLength` | `AlternateTracerBeamWidth` | `AlternateTrailParticleCount` |
| `AlternateTrailParticles` | `AlternateTrailParticleType` | `AmmoModelType` | `angelOfDeath` |
| `angelSpeed` | `antiRadiation` | `APSsound` | `ASWminRange` |
| `ATGMGroundSkimTolerance` | `ATGMTwirl` | `ATGMTwirlSpeed` | `ATGMTwirlWobble` |
| `barelyPenPenalty` | `bigWater` | `Bouncy` | `bulletSmokeTime` |
| `BypassPassiveAPS` | `catNuke` | `CIWSable` | `ciwsBullet` |
| `CIWSer` | `depthCharge` | `distantRicochetSound` | `DroneCollectiveRate` |
| `DroneControllerGraceTicks` | `DroneControllerLossBehavior` | `DroneControllerLossHoverTicks` | `DroneControllerLossSelfDestructTicks` |
| `DroneDropped` | `DroneForwardDrag` | `DroneGravity` | `DroneHorizontalAcceleration` |
| `DroneHoverAltitudeGain` | `DroneHoverHorizontalGain` | `DroneHoverSignalThreshold` | `DroneLiftMultiplier` |
| `DroneMaxClimbSpeed` | `DroneMaxDescentSpeed` | `DroneMaxHorizontalSpeed` | `DroneMaxPitch` |
| `DroneMaxRoll` | `DroneMaxYawRate` | `DroneNeutralForwardInput` | `DronePitchResponse` |
| `DroneRollResponse` | `DroneRotorSpoolDown` | `DroneRotorSpoolUp` | `DroneSideDrag` |
| `droneSpeed` | `DroneVerticalResponse` | `DroneYawResponse` | `dynamicBulletDelay` |
| `dynamicDamage` | `DynamicHitSoundEnable` | `earlyInfrared` | `Evolution` |
| `FlyBySound` | `FlyOverTopAttack` | `FlyOverTopAttackAcquireRange` | `FlyOverTopAttackClearance` |
| `FlyOverTopAttackCorridor` | `FlyOverTopAttackDetonationWindow` | `FlyOverTopAttackTurnRate` | `gasmaskable` |
| `Ghost` | `grenadeBounce` | `HasAlternateModel` | `hasLauncherModel` |
| `HasLine` | `Hesh` | `infiniteAngle` | `jamiogravity` |
| `joystick` | `LockOnFuse` | `manualSensitivity` | `minorPenSound` |
| `MissileGroundSkimTolerance` | `MissileTwirl` | `MissileTwirlSpeed` | `MissileTwirlWobble` |
| `missileWeight` | `missNoise` | `modernTorpedo` | `moonstone` |
| `navalMine` | `nonPenPenalty` | `overPenPenalty` | `overPenSound` |
| `PassiveAPSECCM` | `penDecay` | `penetrateSound` | `powerShake` |
| `PreEvolution` | `ProNavDebugTrail` | `ProNavDebugTrailLength` | `ProNavDebugTrailLifetime` |
| `ProNavGain` | `ProNavMaxTurnRate` | `ProNavTerminalGain` | `ProNavTerminalRange` |
| `ProportionalNavigation` | `radarGuided` | `rangeShake` | `ricochetSound` |
| `RotorcraftDrone` | `RotorcraftDroneFlight` | `scoutBullet` | `seekerRange` |
| `selfGuided` | `ShellType` | `shrapnelAngel` | `smallWater` |
| `smokeDelay` | `smokeProtectable` | `starShell` | `stolenSmoke` |
| `stolenSmokeEffect` | `suppression` | `SwitchDelayMultiplier` | `TPVdrone` |
| `TracerBeam` | `TracerBeamAlpha` | `TracerBeamColor` | `TracerBeamLength` |
| `TracerBeamWidth` | `TVguided` | `VLSTime` |  |

### GunType — 80 keys

Evidence: parsed by the reference's line dispatcher, e.g. `} else if (split[0].equals("ActionAnimation")) {` at line 662 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `ActionAnimation` | `ActionEndSound` | `AddLeftNode` | `AddRightNode` |
| `AddUpNode` | `AimingShootAnimation` | `AimTimeConstant` | `Bow` |
| `CameraRecoil` | `canBlock` | `ChamberSmokeModel` | `ChamberSmokeTexture` |
| `dillElevator` | `dillZoomModifier` | `EndReloadAnimation` | `firstShotRecoil` |
| `GLTFAnimation` | `hasLabigunDelay` | `HasScreenShake` | `Heavy` |
| `IronSightOffset` | `IronSightPitch` | `labigunLimit` | `lance` |
| `LoopReloadAnimation` | `match` | `MeleeDamaged` | `MeleeHitSound` |
| `meleeLeft` | `MeleeLeftDamageOffset` | `MeleeLeftDamagePoint` | `MeleeLeftTime` |
| `meleeRight` | `MeleeRightDamageOffset` | `MeleeRightDamagePoint` | `MeleeRightTime` |
| `meleeUp` | `MeleeUpDamageOffset` | `MeleeUpDamagePoint` | `MeleeUpTime` |
| `muzzleOffset` | `muzzleParticle` | `muzzleParticleCount` | `muzzleParticleHave` |
| `MuzzleSmokeModel` | `MuzzleSmokeTexture` | `OldGun` | `recoilElevator` |
| `RepeatingGun` | `RPM` | `RunCrouchTime` | `RunPosTime` |
| `ScopeAlignment` | `ScreenShakeCameraKick` | `ScreenShakeIntensity` | `ScreenShakeStyle` |
| `SecondaryReloadAnimation` | `SecondarySwitchAnimation` | `SecondarySwitchTime` | `SecondaryUnSwitchAnimation` |
| `SecondaryUnSwitchTime` | `ShootAnimation` | `shootMelee` | `sidearm` |
| `spear` | `StagedReloadTime` | `StartReloadAnimation` | `sustainedelevator` |
| `sustainedRecoilPitch` | `sustainedRecoilYaw` | `SwitchAnimation` | `swordArmorPen` |
| `TacticalReloadGLTFAnimation` | `TacticalReloadTime` | `UseGLTFAnimation` | `UseLeftArmGLTFAnimation` |
| `UseLoopReloadAnimation` | `UseRightArmGLTFAnimation` | `Xoffset` | `Zoffset` |

### PlaneType — 78 keys

Evidence: parsed by the reference's line dispatcher, e.g. `if (split[0].equals("accelBonus")) {` at line 354 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `accelBonus` | `afterBurnFuelPenalty` | `afterburnOffBonus` | `AfterburnWing` |
| `AfterburnWingFlipped` | `area` | `carrierLandable` | `carrierWingFlip` |
| `climbRate` | `CrashSoundRange` | `cruiseSpeed` | `diveBonus` |
| `DroneFlightMode` | `flightCeiling` | `GearCoverPriority` | `gravityMultiplier` |
| `gunRecoil` | `HeliCrashEffects` | `HeliCrashSmokeHealth` | `HeliCrashSound` |
| `HeliCrashSpin` | `HeliCrashSpinStrength` | `helipadLandable` | `heliSpeedLimit` |
| `highAltMax` | `highAltMaxDry` | `maxG` | `maxSpeedDry` |
| `missileElevation` | `missileForward` | `missileVisible` | `missileWingSpan` |
| `needsGear` | `OilCookTime` | `parasitePlane` | `pitchBonus` |
| `pitchBoost` | `pitchStall` | `PlaneCrashSound` | `planeDiveFactor` |
| `QuadcopterAttitudeResponse` | `QuadcopterControlExpo` | `QuadcopterLinearDrag` | `QuadcopterMassKg` |
| `QuadcopterMaxClimbMps` | `QuadcopterMaxSpeedKmh` | `QuadcopterMaxTiltDegrees` | `QuadcopterMouseYawRange` |
| `QuadcopterNeutralBrake` | `QuadcopterYawRate` | `RemoteDrone` | `RemoteDroneBreakBlocks` |
| `RemoteDroneFlightMode` | `RemoteDroneFlightModel` | `RemoteDronePilotHUD` | `RemoteDroneStaticSound` |
| `rollBonus` | `rollBoost` | `rollStall` | `stallSuffering` |
| `StukaSoundRange` | `swapInitialWing` | `turnTime` | `WheelCoverPosition1` |
| `WheelCoverPosition2` | `WheelCoverRate` | `WheelCoverRotation1` | `WheelCoverRotation2` |
| `WheelCoverRotRate` | `WingFlapPosition1` | `WingFlapPosition2` | `WingFlapRate` |
| `WingFlapRotation1` | `WingFlapRotation2` | `WingFlapRotRate` | `yawBonus` |
| `yawBoost` | `yawStall` |  |  |

### ArmourType — 48 keys

Evidence: parsed by the reference's line dispatcher, e.g. `if (split[0].equals("armArmor") \|\| split[0].equals("gauntletArmor")) {` at line 174 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `armArmor` | `backArmor` | `BackupDefence` | `bodyArmor` |
| `chestArmor` | `damageLimit` | `faceArmor` | `forceField` |
| `frontMountRotation` | `frontMountY1` | `frontMountY2` | `frontMountZ1` |
| `frontMountZ2` | `gauntletArmor` | `hasBowPouch` | `hasFrontMount` |
| `hasGunPouch` | `hasHeavyPouch` | `hasMagPouch` | `hasOldGunPouch` |
| `hasPouch` | `hasTopMount` | `headArmor` | `helmetArmor` |
| `legArmor` | `leggingArmor` | `maskArmor` | `napeArmor` |
| `neckArmor` | `pouchMultiplier` | `rearArmor` | `rechargeDelay` |
| `RechargeSound` | `rechargeTimer` | `reloadMultiplier` | `ReserveArmArmor` |
| `ReserveBackArmor` | `ReserveBodyArmor` | `ReserveFaceArmor` | `ReserveHeadArmor` |
| `ReserveLegArmor` | `ReserveNapeArmor` | `ShieldKillSound` | `topMountRotation` |
| `topMountY` | `topMountZ` | `WarningSound` | `WarningSoundTimer` |

### VehicleType — 33 keys

Evidence: parsed by the reference's line dispatcher, e.g. `if (split[0].equals("AccelerationSpeed")) {` at line 190 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `AccelerationSpeed` | `airship` | `animationMultiplier` | `BoostLimit` |
| `BrakeMultiplier` | `canDabOnEntity` | `canDive` | `canRepair` |
| `DecelerationSpeed` | `DiveSpeed` | `driftMultiplier` | `DriftSound` |
| `DriftSoundLength` | `epicShip` | `fancyAmmoRackCookoff` | `HasPassiveAPS` |
| `maxAltitude` | `needsTurret` | `NewDrivingModel` | `NewRepairSystem` |
| `nuDrivingModel` | `PassiveAPSCanForceAim` | `raceCar` | `seaLevel` |
| `SurfaceSpeed` | `switchSides` | `terrainPenalty` | `thermalSight` |
| `ThermalVisionColor` | `thermalVisionGen` | `ThermalVisionGeneration` | `TurretStabilization` |
| `WeakspotCookTime` |  |  |  |

### ShootableType (inherited by BulletType, GrenadeType) — 23 keys

Evidence: parsed by the reference's line dispatcher, e.g. `} else if (split[0].equals("armPen") \|\| split[0].equals("ArmPen")) {` at line 133 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `armPen` | `bleedMultiplier` | `bodyArmorPen` | `bodyPen` |
| `canDroneDrop` | `classicExplosion` | `classicExplosionRadius` | `CringeDetonateOnImpact` |
| `CringeExplodeOnImpact` | `DetonationSoundRange` | `DistantDetonateSound` | `DistantDetonateSoundRange` |
| `dynamicBodyArmorPen` | `headPen` | `launcherMesh` | `LauncherSkin` |
| `missileRadarVisible` | `Noise` | `NoiseLength` | `ProjectileChunkLoading` |
| `smokeParticleCount` | `SwordEnergy` | `wingVisible` |  |

### GrenadeType — 20 keys

Evidence: parsed by the reference's line dispatcher, e.g. `} else if (split[0].equals("BulletProof")) {` at line 148 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `BulletProof` | `EngineGore` | `GrenadeAirDrag` | `GrenadeGroundFriction` |
| `GrenadeStopSpeed` | `HoldToThrow` | `MotionSensor` | `MotionSensorRange` |
| `MotionSound` | `MotionSoundRange` | `MotionTime` | `OverhandThrowSpeedMultiplier` |
| `PenetratesEntities` | `PumpkinRaid` | `RandomDirection` | `smokerino` |
| `TailGore` | `UnderhandThrowSpeedMultiplier` | `vehicleReference` | `WingGore` |

### AttachmentType — 14 keys

Evidence: parsed by the reference's line dispatcher, e.g. `} else if (split[0].equals("AimingTimeMultiplier")) {` at line 172 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `AimingTimeMultiplier` | `barisInfrared` | `barisLaser` | `Bayonet` |
| `BayonetMeleeTime` | `ForceStagedReload` | `HasSecondaryModeAnimations` | `RefundAmmoOnSwitch` |
| `RenderOnlyOnMode` | `RunCrouchTimeMultiplier` | `RunPosTimeMultiplier` | `UnSwitchAnimationOnEmpty` |
| `UseSecondarySwitchAnimation` | `UseSecondaryUnSwitchAnimation` |  |  |

### FlansBotType — 10 keys

Evidence: parsed by the reference's line dispatcher, e.g. `if (split[0].equals("armor")) {` at line 77 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `armor` | `CommonGun` | `coverage` | `evil` |
| `IQlevel` | `knockbackResistance` | `maxHealth` | `movementSpeed` |
| `RareGun` | `SuperRareGun` |  |  |

### ToolType — 10 keys

Evidence: parsed by the reference's line dispatcher, e.g. `} else if (split[0].equals("bandAid")) {` at line 60 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `bandAid` | `EUPerCharge` | `HealStrength` | `needle` |
| `RemoteDroneController` | `summonItem` | `superBandAid` | `surgery` |
| `ToolUses` | `transfusion` |  |  |

### PlaneTargetType — 7 keys

Evidence: parsed by the reference's line dispatcher, e.g. `} else if (split[0].equals("ControlStrength")) {` at line 53 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `ControlStrength` | `Maneuver` | `ManeuverPeriod` | `PaintjobID` |
| `Plane` | `PlaneType` | `Throttle` |  |

### MechaType — 6 keys

Evidence: parsed by the reference's line dispatcher, e.g. `} else if (split[0].equals("AllowAllGuns") \|\| split[0].equals("AcceptAllGuns")) {` at line 118 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `AcceptAllGuns` | `AllowAllGuns` | `morale` | `panicSound` |
| `panicTime` | `runAmokSound` |  |  |

### PlayerClass — 4 keys

Evidence: parsed by the reference's line dispatcher, e.g. `if (split[0].equals("ClassLimit")) {` at line 55 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `ClassLimit` | `ClassLimitPercent` | `ClassLimitPercentage` | `RequiredPrestigeLevel` |

### Team — 4 keys

Evidence: parsed by the reference's line dispatcher, e.g. `if (split[0].equals("DefeatSound")) {` at line 205 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `DefeatSound` | `HasVictorySound` | `VictoryFlagTexture` | `VictorySound` |

### BulletType, GrenadeType — 3 keys

Evidence: parsed by the reference's line dispatcher, e.g. `} else if (split[0].equals("armorPen") \|\| split[0].equals("ArmorPen")) {` at line 418 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `armorPen` | `BounceSoundRange` | `HEAT` |  |

### PlaneType, VehicleType — 3 keys

Evidence: parsed by the reference's line dispatcher, e.g. `if (split[0].equals("coolingBonus")) {` at line 953 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `coolingBonus` | `overheatLimit` | `overheatPenalty` |  |

### Shared across 8 type classes (BulletType, DriveableType, GrenadeType, GunType, MechaType, PlaneType, ShootableType, VehicleType) — 3 keys

Evidence: parsed by the reference's line dispatcher, e.g. `} else if (split[0].equals("expValue")) {` at line 823 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `expValue` | `repairCost` | `Tier` |  |

### AttachmentType, GunType — 2 keys

Evidence: parsed by the reference's line dispatcher, e.g. `} else if (split[0].equals("HasThermalVision") \|\| split[0].equals("HasThermal")) {` at line 199 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `HasThermal` | `HasThermalVision` |  |  |

### ArmourType, GunType — 1 key

Evidence: parsed by the reference's line dispatcher, e.g. `} else if (split[0].equals("ShieldHitSound")) {` at line 487 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `ShieldHitSound` |  |  |  |

### DriveableType (inherited by MechaType, PlaneType, VehicleType) — 1 key

Evidence: parsed by the reference's line dispatcher at `com/flansmod/common/driveables/DriveableType.java:833` — `if (split[0].equals("afterBurnName") || split[0].equals("AfterBurnName")) {` — which accepts both spellings. The name is also mentioned in `com/flansmod/common/types/InfoType.java:79` as `split[0].equals("Name") && !split[0].equals("afterBurnName")`, an unreachable guard (see *Uncertain parameters*); the `DriveableType` branch is the real parser. The target accepts neither spelling anywhere.

| | | | |
|---|---|---|---|
| `afterBurnName` |  |  |  |

### MechaType, PlaneType, VehicleType — 1 key

Evidence: parsed by the reference's line dispatcher, e.g. `if (split[0].equals("unpunchable")) {` at line 390 of the owning class; no target parser or definition uses any of these names (case-insensitive).

| | | | |
|---|---|---|---|
| `unpunchable` |  |  |  |

---

## Uncertain parameters

- **`InfoType.recipe` / `recipeLine` shape rows (both codebases)** — `UNCERTAIN`. `Recipe` consumes the three following lines as raw shape strings (target `common/types/InfoType.java:219-236`; reference `com/flansmod/common/types/InfoType.java:101-118`). The inventory skips those three lines, but a definition whose recipe block is shorter or is interrupted by comments can shift the skip window and cause a real parameter to be mis-attributed, or a shape row to be counted as a key. The `B`/`LPA`/`MAM`/`MMM` rows listed under *Malformed definition lines* are the observable symptom.
- **Target `ArmorBoxType` `AddArmour`/`AddArmor` blocks** — `UNCERTAIN`. `common/types/ArmorBoxType.java:39` consumes four following lines; the same window-shift risk applies, and no unparsed keys were observed for this type, which may mean full coverage or may mean over-skipping.
- **Reference `key.contains("Pos")` / `contains("Scale")` / `contains("Texture")` / `contains("TextureSize")`** — `UNCERTAIN`. Inside `readSeatOpticsHudConfig` (`DriveableType.java:2029-2073`) these substring tests further decompose keys already matched by the `SeatOptics*` prefixes. The exact set of full key names the reference accepts through this path cannot be enumerated lexically; the five prefix families are reported instead.
- **Target model/GUI class references** — `UNCERTAIN`. `InfoType.readClient` resolves `Model`/model-class names through `findModelClass` and a runtime class loader (`util/ClassLoaderUtils.java`, `util/JavaModelCompiler.java`). Key *names* there are literal and covered, but any behaviour keyed on a resolved class name is outside a lexical audit.
- **Reference `afterBurnName` guard** — `UNCERTAIN` only as an *attribution* detail, not as a finding. `com/flansmod/common/types/InfoType.java:79` guards the `Name` branch with `split[0].equals("Name") && !split[0].equals("afterBurnName")`, which is unreachable dead logic (a token cannot equal both), so `InfoType` does not in fact own this key. The real parser is `DriveableType.java:833`, which accepts both `afterBurnName` and `AfterBurnName`; the key is listed under `DriveableType` in the absent-from-target section, and the target accepts neither spelling (HIGH confidence on the gap itself).

---

## Summary

### Target ghost findings

| Type | Parameter | Status | Confidence |
|---|---|---|---|
| `DriveableType` | `ExitSoundLength` | GHOST_PARSED | HIGH |
| `DriveableType` | `OnRadar` | GHOST_PARSED | HIGH |
| `PlaneType` | `Bounciness`, `MomentOfInertia`, `NumWheels` | GHOST_UNPARSED | HIGH |
| `VehicleType` | `Bounciness`, `ClutchSteer`, `FlipLinkFix`, `FloatOnLand`, `HasSmoke`, `MomentOfInertia`, `NumWheels`, `RecoilDistance`, `SoundsPlaceTimePrimary`, `TurretRotationSpeed`, `backRightWheel`, `frontRightWheel` | GHOST_UNPARSED | HIGH |

**Totals — target: 7 `GHOST_PARSED`, 41 `GHOST_UNPARSED` (distinct type/key pairs), 5 `UNCERTAIN` areas.**

### Reference ghost findings

| Type | Count | Confidence |
|---|---|---|
| `DriveableType` | 10 | HIGH |
| `VehicleType` | 11 | HIGH |
| `GunType` | 9 | HIGH |
| `PlaneType` | 7 | HIGH |
| `BulletType` | 5 | HIGH |
| `GrenadeType` | 4 | HIGH |
| `ArmourType`, `AttachmentType`, `MechaItemType` | 1 each | HIGH |

**Total — reference: 49 `GHOST_PARSED`; `GHOST_UNPARSED` not determinable (no bundled definitions).**

### Reference-only keys

| Owning reference class(es) | Absent from target | Confidence |
|---|---|---|
| `DriveableType` (+ `MechaType`, `PlaneType`, `VehicleType`) | 254 (253 + `afterBurnName`) | HIGH |
| `BulletType` | 139 | HIGH |
| `GunType` | 80 | HIGH |
| `PlaneType` | 78 | HIGH |
| `ArmourType` | 48 | HIGH |
| `VehicleType` | 33 | HIGH |
| `ShootableType` (+ `BulletType`, `GrenadeType`) | 23 | HIGH |
| `GrenadeType` | 20 | HIGH |
| `AttachmentType` | 14 | HIGH |
| `FlansBotType` (no target type) | 10 | HIGH |
| `ToolType` | 10 | HIGH |
| `PlaneTargetType` (no target type) | 7 | HIGH |
| `MechaType` | 6 | HIGH |
| `PlayerClass` | 4 | HIGH |
| `Team` | 4 | HIGH |
| multi-class groups (`Tier`/`repairCost`/`expValue`, `coolingBonus`/`overheatLimit`/`overheatPenalty`, `armorPen`/`BounceSoundRange`/`HEAT`, `HasThermal`/`HasThermalVision`, `ShieldHitSound`, `unpunchable`) | 13 | HIGH |

**Total — 743 reference parameter names absent from the target.**

---

## Areas requiring deeper audit

- **Recipe- and block-consuming keys** (`Recipe`, `ShapelessRecipe`, `AddArmour`/`AddArmor`) advance the file cursor by a fixed number of lines in both codebases. The inventory replicates that skip, but malformed content — which demonstrably exists in this repository — desynchronises it. A parser-accurate replay of each `.txt` through the real `TypeFile` would settle the handful of odd tokens listed under *Malformed definition lines*.
- **Reference `SeatOptics*` prefix dispatch** (`DriveableType.java:2002-2073`) accepts an open-ended key space through `startsWith` plus `contains("Pos"|"Scale"|"Texture"|"TextureSize")`. The five prefixes are reported, but the concrete full key names a content pack would write cannot be recovered without reference content packs.
- **Reference content packs are absent.** The reference key set is parser-only. If the fork's packs are available separately, re-running the inventory against them would reveal reference `GHOST_UNPARSED` keys and would confirm which of the 743 absent keys content actually uses.
- **Dead-code consumers.** The ghost classification proves a parsed field has no *reader*. It does not prove that a field with a reader reaches observable behaviour — a field read only inside an unreachable branch would be classified `ACTIVE` here. Types with large alias families (`DriveableType`, `GunType`) are the likeliest place for such second-order ghosts.
- **Reflection and cross-module access.** The target loads model classes dynamically (`util/ClassLoaderUtils.java`, `util/DeobfClassVisitor.java`, `util/TransformClassVisitor.java`). No InfoType *field* is accessed reflectively in the code reviewed, but a transformer-injected consumer would not be visible to a source-level search.
