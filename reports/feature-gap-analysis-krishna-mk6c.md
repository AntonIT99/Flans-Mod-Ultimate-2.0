# Feature gap analysis: Flan's Mod Aryan Indian Edition "Krishna Mk6C" → Flans-Mod-Ultimate-2.0

Date: 2026-09-10. Direction is strictly reference → target. Target-only functionality, refactors and modernisations are out of scope and are not reported.

- Reference: `C:/Users/alpha/Documents/Minecraft-Development/Flan's Mod Aryan Indian Edition Krishna Mk6C` — an extracted/decompiled 1.7.10 mod jar (`mcmod.info`: modid `flansmod`, "Flan's LabCat Mod ... fuzed with clowder and HBM NTM"). 2107 `.java` files.
- Target: `C:/Users/alpha/Documents/Minecraft-Development/Flans-Mod-Ultimate-2.0`, branch `master`, working tree at commit `08494f8a` (Fix hitmarkers). 525 `.java` files under `../src/main/java/com/flansmodultimate`.
- Result: **36 MISSING, 8 PARTIAL, 3 UNCERTAIN**. These counts describe the findings below; they are not a measure of port completeness.

## Scope and evidence conventions

Evidence roots:

- `R/` = reference `com/flansmod/`.
- `T/` = target `../src/main/java/com/flansmodultimate`.
- Resource paths are given in full from each repository root.

Discovery was reference-driven and systematic: every reference type class was scanned for its content-pack configuration keys (`split[0].equals("...")`), and the resulting 1553 keys were diffed against every string literal in the target, then each surviving candidate was traced to its reference call sites and checked against plausible target equivalents (renamed, generalised, data-driven or newer-mechanism). Beyond the key diff, the reference's entities, packets (84 classes), GUIs, particles, game types, commands, client handlers, assets and shaders were walked package by package.

`MISSING` means no equivalent capability was found in the inspected target paths. `PARTIAL` means the broader mechanic exists but the stated behaviour does not. `UNCERTAIN` means the reference behaviour is real but a target equivalent may exist under an implementation different enough that this static pass could not settle it. Confidence applies to the narrow finding, not to whole-subsystem parity.

**Bundled foreign mod — excluded.** The reference jar also contains `com/hfr/` (519 `.java` files: Clowder factions, RVI, PON4, schematics, dimensions, nuclear tech) and `com/hbm/`, `cofh/`, `com/LordWeeder/`. These are separate mods fused into the same artifact, not Flan's Mod functionality, and comparing them against a Flan's Mod port would not be meaningful. They are excluded from the findings. The nine `com/flansmod` files that reach into them (`R/common/driveables/EntityDriveable.java:53-55`, `EntityPlane.java:30`, `EntitySeat.java:28`, `EntityVehicle.java:28`, `ItemPlane.java:11`, `ItemVehicle.java:11`, `R/common/guns/EntityBullet.java:50`, `R/common/guns/raytracing/PlayerHitbox.java:610`, `R/client/TickHandlerClient.java:51`) add faction ownership of driveables, an RVI HUD overlay, HBM nuclear-blast bullets, and safezone gating of gore. Those integration points are inherently HFR-dependent and are likewise not reported as gaps.

**Fork-specific joke content — excluded.** Reference config keys such as `InshaAllah`, `goyim`, `catNuke`, `autisticHitDetection`, `evilGolem`, `PumpkinRaid` and `canDabOnEntity`, and the naming of the `R/client/virtualreality/` package, are server-specific gags. Where such a key gates a real mechanic the mechanic is reported on its own merits (see First-person body rendering); the gags themselves are not.

No files in either codebase were modified. No build or in-game test was run; runtime parity is not established by this report.

---

## Weapons and ammunition

### Directional melee and bayonets — MISSING

Reference: `R/common/guns/GunType.java` (`MeleeLeft/Right/Up`, `MeleeLeftTime`, `MeleeLeftDamagePoint`, `MeleeLeftDamageOffset` and the Right/Up equivalents, `MeleeDamaged`, `MeleeHitSound`, `shootMelee`, `canBlock`, `spear`, `lance`, `swordArmorPen`), `R/common/PlayerData.java:104-112` (`meleeProgressLeft/Right/Down`, `meleeLengthLeft/Right/Down`), `R/common/PlayerData.java:627-673` (`doMeleeLeft`, `doMeleeRight`, `doMeleeDown`), `R/common/guns/AttachmentType.java` (`Bayonet`, `BayonetMeleeTime`).
A gun swings in three distinct directions, each with its own animation length, damage point, damage offset and hit sound, plus directional blocking state (`R/common/PlayerData.java:48-51`, `isBlockingLeft/Right/Top/Bottom`). A bayonet attachment adds its own melee timing on top of the host gun.

Target checked: `T/common/types/GunType.java` (`MeleeDamage`, `MeleeDamagePoint`, `MeleeDamageOffset`, `MeleeTime`, `MeleeSound`, `UseCustomMelee`, `UseCustomMeleeWhenShoot`), `T/common/types/AttachmentType.java`, `T/common/item/GunItemHandler.java`. One undirected melee attack with a single timing and damage point; `Shield` exists (`T/common/types/GunType.java:340-347`) but as a passive shield piece, not directional blocking.

Missing: the three-direction melee attack set with per-direction timing and damage geometry, directional blocking state, and bayonet attachment melee timing.

### Ricochet, over-penetration and non-penetration feedback — MISSING

Reference: `R/common/guns/BulletType.java` (`RicochetSound`, `DistantRicochetSound`, `PenetrateSound`, `OverPenSound`, `MinorPenSound`, `BarelyPenPenalty`, `NonPenPenalty`, `OverPenPenalty`, `PenDecay`, `LessOverpen`, `Bouncy`, `BounceSoundRange`), consumed on hit resolution in `R/common/guns/EntityBullet.java`.
A round that fails to penetrate, barely penetrates, or over-penetrates plays a distinct sound (with a separate distant variant) and takes a distinct damage penalty, giving shooter and target audible feedback on the armour outcome.

Target checked: `T/common/guns/penetration/PenetrationCalculator.java`, `PenetrationLoss.java`, `PenetrationResult.java`, `T/common/guns/ShootingHelper.java:322-324` (`handleBounceOrStop`), `T/common/driveables/armor/VehicleArmorResolver.java`, `T/common/types/BulletType.java` (`PenetrationDecay`, `PenetrationDecayDamageEffect`, `Bounciness`). The target models penetration decay, sloped-armour impact angle and a maximum impact angle, and bullets can bounce — but the outcome is silent and carries no penalty tiering.

Missing: the ricochet / penetrate / over-penetrate / minor-penetrate sound cues with distant variants, and the barely-pen, non-pen and over-pen damage penalty tiers.

### Tracer beam rendering — MISSING

Reference: `R/common/guns/BulletType.java` (`TracerBeam`, `TracerBeamColor`, `TracerBeamAlpha`, `TracerBeamLength`, `TracerBeamWidth`, and the `AlternateTracerBeam*` set), `R/client/particle/EntityTracerBeamFX.java`, `R/client/particle/EntityFMTracerRed.java`, `EntityFMTracerGreen.java`.
Bullets can render as a coloured beam parameterised by width and length rather than a sprite trail, with a second beam configuration selectable per loaded ammo.

Target checked: `T/client/particle/FmTracerParticle.java`, `T/client/render/InstantShotTrail.java`, `T/client/render/InstantBulletRenderer.java`, `T/network/client/PacketBulletTrail.java`, `T/common/types/BulletType.java` (`TrailTexture`). Trails are texture-sprite based; no beam geometry, colour, alpha, width or length configuration.

Missing: the configurable tracer-beam renderer and its per-ammo alternate configuration.

### Per-ammo alternate model, texture and trail — MISSING

Reference: `R/common/guns/BulletType.java` (`HasAlternateModel`, `AlternateModel`, `AlternateTexture`, `AlternateBulletLoad`, `AlternateTrailParticles`, `AlternateTrailParticleType`, `AlternateTrailParticleCount`, `AmmoModelType`).
One bullet type can present a second model, texture and trail set, switched by which round is actually chambered.

Target checked: `T/common/types/BulletType.java`, `T/common/types/ShootableType.java` (`TrailParticles`, `TrailParticleType`, `SmokeTrail`), `T/client/render/entity/BulletRenderer.java`, `T/client/model/ModelCache.java`. A single model, texture and trail per bullet type.

Missing: the alternate model/texture/trail variant and the chambered-round switch that selects it.

### Suppression on near-miss — MISSING

Reference: `R/common/guns/raytracing/EnumHitboxType.java` (`NEARBY`), `R/common/guns/raytracing/PlayerHitbox.java:506-551`, `R/common/network/PacketSuppression.java`, `R/common/guns/BulletType.java` (`Suppression`, `suppression`), `R/common/FlansMod.java` (`enableModernSuppression`, `modernSuppressionBulletCrack`, `modernSuppressionScreenShake`, `modernSuppressionVignette`).
A round passing near a player without hitting them registers on a dedicated `NEARBY` hitbox and sends a suppression packet carrying intensity and direction; the client answers with a screen shake, a vignette and a directional bullet-crack sound. A legacy fallback applies blindness instead.

Target checked: `T/common/raytracing/EnumHitboxType.java` (`LEGS, BODY, HEAD, LEFTARM, RIGHTARM, LEFTITEM, RIGHTITEM` — no near-miss box), `T/common/raytracing/PlayerHitbox.java`, `T/common/guns/ShootingHelper.java`, `T/network/client/` (no suppression packet). Every occurrence of "suppress" in the target is `SuppressedShootSound`, i.e. silencer audio.

Missing: near-miss detection and the entire suppression feedback loop (packet, screen shake, vignette, directional crack).

### Beyond-visual-range projectile chunk loading — MISSING

Reference: `R/common/guns/ProjectileChunkManager.java` (ticket purpose `ProjectileBVR`, with per-dimension, per-player and server-wide ticket caps), `R/common/guns/BulletType.java` (`ProjectileChunkLoading`), `R/common/FlansMod.java` (`enableProjectileChunkLoading`, `projectileChunkLoadingDistance`, `projectileChunkLoadingTicks`, `projectileForcedChunksPerProjectile`, `maxProjectileTicketsPerDimension`, `maxProjectileTicketsPerPlayer`, `maxProjectileTicketsServer`).
Long-range missiles and shells force-load the chunks along their flight path so they keep ticking outside loaded terrain, under quota caps that bound the cost.

Target checked: `T/common/entity/Bullet.java`, `T/common/entity/Shootable.java`, `T/config/ModCommonConfig.java` (`bulletTrackingRange` and `bulletRenderDistance` are view and tracking distances, not chunk tickets). No forced-chunk mechanism.

Missing: projectile chunk ticketing and its quota configuration; long-range ordnance stops ticking once it leaves loaded chunks.

### Seeker classes: radar-guided, anti-radiation, self-guided — MISSING

Reference: `R/common/guns/BulletType.java` (`radarGuided`, `antiRadiation`, `selfGuided`, `seekerRange`, `missileRadarVisible`, `earlyInfrared`, `infiniteAngle`, `LockOnFuse`, `maxLockOnAngle`).
Distinct seeker behaviours: semi-active radar homing, homing onto radar emitters, fire-and-forget self-guidance with an independent acquisition range, an early-generation infrared seeker with degraded acquisition, and a proximity fuse tied to lock state.

Target checked: `T/common/types/BulletType.java` (`LockOnToDriveables/Vehicles/Planes/Mechas/Players/Livings`, `MaxLockOnAngle`, `LockOnForce`, `TickStartHoming`, `EnableSACLOS`, `MaxDegreeOFSACLOS`, `MaxRangeOfMissile`, `LaserGuidance`, `ManualGuidance`, `FixedTrackDirection`, `GuidedTurnRadius`), `T/common/entity/Bullet.java`, `T/event/BulletLockOnEvent.java`. Lock-on, SACLOS, laser and manual guidance exist, but the seeker itself is one undifferentiated homing model.

Missing: radar-guided, anti-radiation and self-guided seeker classes, an independent seeker acquisition range, seeker-generation modelling, and the lock-tied proximity fuse.

### Proportional navigation guidance — MISSING

Reference: `R/common/guns/ProportionalNavigation.java` (true-PN steering from the line-of-sight rotation rate and closing speed, with gain and max-turn-rate clamps), `R/common/guns/BulletType.java` (`ProportionalNavigation`, `ProNavGain`, `ProNavMaxTurnRate`, `ProNavTerminalGain`, `ProNavTerminalRange`, `ProNavDebugTrail`, `ProNavDebugTrailLength`, `ProNavDebugTrailLifetime`), `R/client/ProNavDebugTrailRenderer.java`, `R/common/network/PacketProNavState.java`.
Missiles lead a manoeuvring target using proportional navigation, with a separate terminal-phase gain inside a configurable terminal range, plus a renderable debug trail for tuning.

Target checked: `T/common/entity/Bullet.java` (homing turns toward the current target position via `GuidedTurnRadius`, `LockOnForce` and `TurningForce`), `T/common/types/BulletType.java`, `T/client/render/` (no trail renderer), `T/network/` (no guidance-state packet). Pure-pursuit homing only.

Missing: proportional-navigation steering with terminal-phase gain switching, and its debug trail visualisation.

### Missile twirl and ground-skim profiles — MISSING

Reference: `R/common/guns/BulletType.java` (`MissileTwirl`, `MissileTwirlSpeed`, `MissileTwirlWobble`, `ATGMTwirl`, `ATGMTwirlSpeed`, `ATGMTwirlWobble`, `MissileGroundSkimTolerance`, `ATGMGroundSkimTolerance`).
Missiles can spiral around the line of sight at a configured rate and wobble amplitude — tuned separately for ATGMs — and can fly a terrain-following profile that holds a tolerance band above the ground.

Target checked: `T/common/entity/Bullet.java`, `T/common/types/BulletType.java` (`GuidedPhaseSpeed`, `GuidedPhaseTurnSpeed`, `HasDeadZone`, `DeadZoneTime`, `ShootForSettingPos`, `ShootForSettingPosHeight`). Phased guidance and a fixed launch-height offset exist; no spiral flight and no ground-following band.

Missing: the twirl/wobble flight profile and ground-skim terrain following.

### Fly-over top attack — PARTIAL

Reference: `R/common/guns/BulletType.java` (`FlyOverTopAttack`, `FlyOverTopAttackAcquireRange`, `FlyOverTopAttackClearance`, `FlyOverTopAttackCorridor`, `FlyOverTopAttackDetonationWindow`, `FlyOverTopAttackTurnRate`), `R/common/network/PacketTopAttackState.java`.
A Javelin-style profile: acquire at range, climb to a clearance altitude, fly a corridor over the target, then detonate downward inside a timing window, with its own turn rate and a state packet so the client can reflect the attack phase.

Target checked: `T/common/types/BulletType.java:144,271` (`IsDoTopAttack`), `T/common/entity/Bullet.java:850-855`. The target's top attack is a check that the missile is more than 2 blocks away in X and Z before biasing the approach downward — no climb, corridor, clearance, detonation window, acquisition range or turn rate, and no client state.

Missing: the parameterised climb / corridor / clearance / detonation-window profile and its client state synchronisation.

### Wire-guided missile and its visible wire — MISSING

Reference: `R/common/guns/EntityGuidanceWire.java` (an entity linking the launcher — or a specific mounted seat anchor — to the projectile, rendered out to 2048 blocks), `R/common/guns/BulletType.java` (`HasLine`, `TVguided`), `R/common/network/PacketManualGuidance.java`.
A wire-guided missile trails a visible command wire anchored to the firing seat or player, giving both the operator and observers a physical cue for the guidance link.

Target checked: `T/network/server/PacketManualGuidance.java` (SACLOS-style command guidance from an origin and look vector — the guidance mechanic itself is present), `T/common/entity/` (no wire entity), `T/client/render/entity/` (no wire renderer).

Missing: the guidance-wire entity, its seat/player anchor and its rendering. The command-guidance mechanic is present.

### FPV drone munitions — MISSING

Reference: `R/common/guns/DroneFlightController.java` (rotor spool, collective, attitude integration, hover latch, controller-loss grace with failsafe hover or self-destruct), `R/common/guns/BulletType.java` (`RotorcraftDrone`, `RotorcraftDroneFlight`, `DroneDropped`, `canDroneDrop`, `TPVdrone`, `joystick`, `manualSensitivity`, `droneSpeed`, plus a ~25-key `Drone*` tuning set: `DroneCollectiveRate`, `DroneLiftMultiplier`, `DroneMaxPitch/Roll/YawRate`, `DroneHoverAltitudeGain`, `DroneHoverHorizontalGain`, `DroneRotorSpoolUp/Down`, `DroneControllerGraceTicks`, `DroneControllerLossBehavior`, `DroneControllerLossHoverTicks`, `DroneControllerLossSelfDestructTicks`, …), reference resources `assets/minecraft/shaders/post/drone_crt.json`, `drone_static.json` and `assets/minecraft/shaders/program/drone_crt.fsh`, `drone_static.fsh`.
A projectile flown as an FPV multirotor: the operator pilots it from its own camera through a CRT/static post-process, it can hover-latch, carry and drop a payload, and it degrades predictably (hover, then self-destruct) when the control link is lost.

Target checked: `T/common/entity/Bullet.java`, `T/common/types/BulletType.java`, `T/client/render/MountedCameraView.java`, `T/mixin/DriveableCameraMixin.java`, `../src/main/resources` (the target ships no shaders at all). The only "drone" in the target is `T/apocalyse/common/entity/SkullDroneEntity.java`, an unrelated Apocalypse mob.

Missing: the entire FPV drone munition — flight controller, control-loss failsafe, payload drop, operator camera and its screen shaders.

### Naval ordnance behaviours — PARTIAL

Reference: `R/common/guns/BulletType.java` (`depthCharge`, `activationDepth`, `navalMine`, `modernTorpedo`, `ASWminRange`, `aftermathFuse`, `smallWater`, `bigWater`).
Depth charges arm at a configured depth; naval mines persist and trigger on proximity; a modern torpedo profile differs from the legacy one; anti-submarine weapons refuse to arm inside a minimum range; water impacts pick a small or large splash effect.

Target checked: `T/common/types/BulletType.java` (`Torpedo`, `DragInWater`), `T/common/entity/Bullet.java`, `T/common/types/ShootableType.java` (`LivingProximityTrigger`, `VehicleProximityTrigger`, `PrimeDelay`, `TriggerDelay`). A torpedo mode and generic proximity triggers exist.

Missing: depth-based arming, the persistent naval mine, the modern-torpedo profile, ASW minimum arming range, and water-impact effect scaling.

### Illumination and chemical shells — MISSING

Reference: `R/common/guns/BulletType.java` (`starShell`, `smokeParticleType`, `smokeDelay`, `smokeParticleCount`, `smokeProtectable`, `gasmaskable`, `stolenSmoke`, `stolenSmokeEffect`), `R/client/particle/EntitySmokeShell.java`, `EntitySmokeShellChlorine.java`, `EntitySmokeShellMustard.java`, `R/common/network/PacketVaccine.java`.
Star shells light an area; smoke shells come in plain, chlorine and mustard variants that apply effects to anyone in the cloud, gated by whether the victim's armour provides gas-mask or smoke protection.

Target checked: `T/common/types/GrenadeType.java` (`SmokeTime`, `SmokeParticles`, `SmokeParticleType`, `SmokeParticlesCount`, `SmokeRadius`), `T/client/particle/SmokeGrenadeParticle.java`, `T/common/types/ArmorType.java` (`SmokeProtection` exists). Smoke grenades and smoke protection are present; smoke as a *shell* payload, illumination rounds and chemical agents are not.

Missing: star-shell illumination, chemical (chlorine and mustard) shell payloads with their gas-mask gating, and the smoke-shell projectile payload path.

### HEAT and HESH shell classes — MISSING

Reference: `R/common/guns/BulletType.java` (`HEAT`, `Hesh`, `ShellType`), `R/common/guns/GrenadeType.java` (`HEAT`, `BulletProof`, `penetratingPower`, `armorPen`).
Shells are classified so that a shaped-charge round penetrates independently of impact velocity, and a squash-head round transfers damage through armour rather than defeating it.

Target checked: `T/common/guns/penetration/PenetrationCalculator.java`, `T/common/driveables/armor/VehicleProjectileDamageResolver.java`, `T/common/types/BulletType.java` (`PenetrationAt100m`, `Penetration`, `PenetratingPower`, `PenetrationDecay`), `T/config/ModCommonConfig.java` (`kineticPenetrationReference`). One kinetic penetration model driven by mass and velocity.

Missing: non-kinetic shell classes (chemical-energy HEAT, spalling HESH) and the shell-type classification that selects them.

### glTF skeletal gun animation — MISSING

Reference: `R/client/model/animation/gltf/GLTFAnimationController.java`, `AnimationResourceHandler.java`, `model/AnimationFile.java`, `model/Animation.java`, `model/Bone.java`, `model/EnumAnimationPart.java`, `R/client/model/animation/AnimationController.java` (456 lines), driven by `R/common/guns/GunType.java` (`UseGLTFAnimation`, `GLTFAnimation`, `TacticalReloadGLTFAnimation`, `UseLeftArmGLTFAnimation`, `UseRightArmGLTFAnimation`) and by `R/common/guns/AttachmentType.java` animation overrides.
Guns can play authored keyframe animations loaded from glTF files, with named bones bound to model parts and per-arm clip selection, as an alternative to procedural animation.

Target checked: `T/common/types/GunAnimationConfig.java` (a large procedural TMT-driven set: slide distance, pump, charge, revolver flip, break action, arm poses, staged reload, …), `T/client/render/item/GunItemRenderer.java`, `../src/main/java/com/flansmod/client/model/GunAnimations.java`. Entirely procedural; no glTF loader, no bone binding, no animation resource handler.

Missing: the glTF animation pipeline (file loading, bone binding, authored clip playback) and the config keys that select clips per gun, attachment and arm.

### Firing screen shake — MISSING

Reference: `R/common/guns/GunType.java` (`HasScreenShake`, `ScreenShakeIntensity`, `ScreenShakeStyle`, `ScreenShakeCameraKick`, `CameraRecoil`), `R/common/driveables/DriveableType.java` (`FancyScreenShake`, `FancyScreenShakeRange`, `FancyScreenShakePrimaryIntensity/Duration`, `FancyScreenShakeCoaxIntensity/Duration`, `PrimaryScreenShake`, `SecondaryScreenShake`), `R/common/network/PacketFancyScreenShake.java`, `PacketShakeRecoil.java`, `R/common/guns/BulletType.java` (`powerShake`, `rangeShake`).
Firing a gun or a vehicle cannon shakes the camera for nearby players, with per-weapon intensity, duration and style and a separate camera kick; explosions shake by power and range.

Target checked: `T/common/guns/GunRecoil.java`, `T/client/input/`, `T/client/render/MountedCameraView.java`, `T/network/client/` (no shake packet), `T/config/ModClientConfig.java` (`realisticRecoil`, `gunRecoilModifier`). Recoil moves the shooter's aim; nothing shakes the camera, and nothing propagates a shake to bystanders.

Missing: camera shake from firing and from nearby heavy weapons, with its range, intensity, duration and style configuration.

### Gun carry limit — MISSING

Reference: `R/common/FlansMod.java` (`gunCarryLimitEnable`, `gunCarryLimit`), enforced against inventory contents; interacts with `R/common/teams/ArmourType.java` pouches (`hasPouch`, `hasGunPouch`, `hasHeavyPouch`, `hasBowPouch`, `hasMagPouch`, `hasOldGunPouch`, `pouchMultiplier`) and `R/common/guns/GunType.java` (`Heavy`, `sidearm`, `GunLength`).
A player may carry only a limited number of guns, weighted by weapon class, with armour pouches raising the allowance.

Target checked: `T/config/ModCommonConfig.java`, `T/common/item/GunItem.java`, `T/util/InventoryHelper.java`, `T/common/types/ArmorType.java`. No carry limit and no weapon-class weighting.

Missing: the gun carry limit, weapon-class weighting, and the armour-pouch allowance that modifies it.

### Gun tier economy — MISSING

Reference: `R/common/guns/GunType.java:42-44,414-420` (`Tier` derives `expValue = (7 + tier) * 135 - 992`, floored at 5, and `repairCost = expValue`); the same keys appear on `R/common/guns/ShootableType.java` and `R/common/driveables/DriveableType.java`.
A single tier number on a weapon derives both its XP/score value and its repair cost, so pack authors set relative weapon worth once.

Target checked: `T/common/types/GunType.java`, `T/common/types/InfoType.java`, `T/common/teams/LoadoutPool.java` (`getExperienceForKill`, `getExperienceForKillstreakBonus` — XP is per kill and per streak, not per weapon), `T/common/types/RewardBox.java`.

Missing: the per-weapon tier value and the XP and repair-cost economy derived from it.

### Ammunition evolution rounds — MISSING

Reference: `R/common/guns/BulletType.java:32` (`Evolution`, `PreEvolution`), `R/common/guns/EntityBullet.java:1046-1053`.
A round that hits a vehicle matching `PreEvolution` converts it into the `Evolution` vehicle type — an upgrade or field-modification munition.

Target checked: `T/common/entity/Bullet.java`, `T/common/guns/ShootingHelper.java`, `T/common/driveables/armor/VehicleProjectileDamageResolver.java`. Bullet-vehicle interaction is damage-only.

Missing: the round that transforms a struck vehicle into another vehicle type.

### Muzzle and chamber smoke models — PARTIAL

Reference: `R/common/guns/GunType.java` (`MuzzleSmokeModel`, `MuzzleSmokeTexture`, `ChamberSmokeModel`, `ChamberSmokeTexture`, `muzzleParticleHave`, `muzzleParticle`, `muzzleParticleCount`, `muzzleOffset`).
Separate modelled smoke wisps at the muzzle and at the ejection port after firing, alongside the muzzle flash.

Target checked: `T/common/types/GunType.java` (`MuzzleFlashModel`, `FlashModel`, `FlashTexture`, `MuzzleFlashParticle`, `MuzzleFlashParticleSize`, `MuzzleFlashParticleHandOffset`, `MuzzleFlashParticleShoulderOffset`, `CasingModel`, `CasingTexture`), `T/client/particle/FmMuzzleFlashParticle.java`, `T/client/particle/FmSmokeParticle.java`. Muzzle flash models and particles and casing models are present.

Missing: the dedicated muzzle-smoke and chamber-smoke model and texture channels.

---

## Armour and the player damage model

### Per-zone armour coefficients and extended hitboxes — MISSING

Reference: `R/common/guns/raytracing/EnumHitboxType.java` (`BODY, NAPE, LEFTARM, RIGHTARM, LEFTITEM, RIGHTITEM, LEGS, FACE, BACK, CRANIUM, NEARBY`), `R/common/teams/ArmourType.java:38-44` (`armArmor`, `legArmor`, `headArmor`, `napeArmor`, `faceArmor`, `bodyArmor`, `backArmor`), `R/common/guns/BulletType.java` and `ShootableType.java` (`HeadPen`, `BodyPen`, `ArmPen`, `BodyArmorPen`, `DynamicBodyArmorPen`, `ArmorPen`).
The head is split into cranium, face and nape and the torso into front and back, each with its own armour coefficient; rounds carry matching per-zone penetration values, so a shot to the back of the helmet resolves differently from one to the face.

Target checked: `T/common/raytracing/EnumHitboxType.java` (7 boxes: `LEGS, BODY, HEAD, LEFTARM, RIGHTARM, LEFTITEM, RIGHTITEM`), `T/common/raytracing/PlayerHitbox.java:174-197` (flat per-zone damage multipliers from config: `headshotDamageModifier`, `chestshotDamageModifier`, `legshotModifier`, `armshotDamageModifier`), `T/common/types/ArmorType.java` (`DamageReduction`, `BulletDefence`, `PenetrationResistance` — one whole-piece value).

Missing: cranium/face/nape and front/back torso discrimination, per-zone armour coefficients on armour types, and per-zone penetration values on ammunition.

### Reserve armour plates — MISSING

Reference: `R/common/teams/ArmourType.java:45-52` (`ReserveHeadArmor`, `ReserveNapeArmor`, `ReserveFaceArmor`, `ReserveBodyArmor`, `ReserveBackArmor`, `ReserveArmArmor`, `ReserveLegArmor`, `backupDefence`).
A second armour layer behind the primary one: once the primary coefficient is defeated at a zone, the reserve value and backup defence still apply, modelling plate carriers with backing plates.

Target checked: `T/common/types/ArmorType.java`, `T/common/item/CustomArmorItem.java`, `T/common/item/CustomArmorMaterial.java`, `T/common/raytracing/PlayerHitbox.java`. Single-layer armour only.

Missing: the reserve armour layer and its backup-defence fallback.

### Energy shields on armour — MISSING

Reference: `R/common/teams/ArmourType.java:29,34-37,56-59` (`energyShield`, `forceField`, `maxDamage` as `damageLimit`, `rechargeTimer`, `rechargeDelay`, `warningSoundTimer`, `warningSound`, `rechargeSound`, `ShieldHit`, `ShieldKill`), with state in `R/common/PlayerData.java:141-144` (`shieldTimer`, `rechargeTimer`, `SoundTimer`, `shieldHit`).
A regenerating shield absorbs damage up to a limit, delays then recharges, and plays distinct hit, break and recharge sounds plus a low-shield warning tone.

Target checked: `T/common/types/ArmorType.java`, `T/common/item/CustomArmorItem.java`; `T/common/types/MechaItemType.java` has `EnergyShield`, but only as a mecha upgrade, not as player armour.

Missing: the player-armour energy shield with its damage limit, recharge delay and timer, and its four-sound feedback set.

### Bleeding and field medicine — MISSING

Reference: `R/common/PlayerData.java:121-126` (`blood`, `minorBleed`, `Bleed`, `hemorrhaging`, `timer`, `timerSlow`), `R/common/guns/raytracing/PlayerHitbox.java:611-612` (`data.minorBleed += hitDamage * bullet.type.bleedMultiplier`, gated on `TeamsManager.bleeding`), `R/common/guns/BulletType.java` (`BleedMultiplier`, `bleeding`), `R/common/tools/ToolType.java` (`bandAid`, `superBandAid`, `needle`, `surgery`, `transfusion`, `HealStrength`), `R/common/teams/CommandTeams.java` (`bleeding` toggle), and blood particles `R/client/particle/Entityblood.java`, `EntityGroundBlood.java`.
Wounds cause graded bleeding that drains a blood pool over time; different medical items treat different severities (bandage for minor bleeds, needle, transfusion or surgery for worse), and blood pools render at the wound site.

Target checked: `T/common/PlayerData.java`, `T/common/types/ToolType.java` (`Heal`, `HealPlayers`, `HealAmount`, `Food`, `Foodness` — instant healing only), `T/client/render/ClientHudOverlays.java:391` and `T/client/ModClient.java:171-174` (a red damage flash overlay), `T/common/teams/TeamsManager.java`. No bleeding state, no blood pool, no graded treatment.

Missing: the blood and bleeding state machine, its match toggle, graded medical items, and blood particles.

### Armour pouches and weapon mounts — MISSING

Reference: `R/common/teams/ArmourType.java:60-75` (`pouchMultiplier`, `hasPouch`, `hasOldGunPouch`, `hasGunPouch`, `hasHeavyPouch`, `hasBowPouch`, `hasMagPouch`, `hasFrontMount` with `frontMountY1/Z1/Y2/Z2/Rotation`, `hasTopMount` with `topMountY/Z/Rotation`), `R/common/PlayerData.java:92` (`pouchMultiplier`).
Armour advertises pouches that raise carry allowance and front/top mount points with explicit geometry, so a slung weapon renders on the wearer's chest or back at authored coordinates.

Target checked: `T/common/types/ArmorType.java`, `T/client/render/CustomArmorLayer.java`, `T/mixin/HumanoidArmorLayerMixin.java`, `../src/main/java/com/flansmod/client/model/ModelCustomArmour.java`. Armour renders a custom model but exposes no mount points and no pouch capacity.

Missing: pouch capacity classes and the front/top weapon mount geometry with its rendering.

---

## Vehicles, aircraft and mechas

### Destroyed-vehicle wrecks — MISSING

Reference: `R/common/driveables/EntityDriveableWreck.java`, `R/common/driveables/EntityDriveable.java` (`captureWreckPartSnapshot`, `spawnWreckIfConfigured`), `R/common/driveables/DriveableType.java` (`LeaveWreck`, `WreckModel`, `WreckTexture`, `WreckLifetime`, `WreckSmoke`, `WreckRendersFancyTracks`), `R/common/FlansMod.java` (`enableDriveableWrecks`).
A destroyed vehicle leaves a persistent burning hulk entity — its own model and texture, a smoke plume, a configured lifetime, and a snapshot of which parts had already been blown off.

Target checked: `T/common/entity/Driveable.java`, `T/common/driveables/DriveableCrashExplosion.java`, `T/network/client/PacketDriveableCrashFireball.java`. Occurrences of "wreck" in the target are prose in comments only; destruction produces an explosion and removes the entity.

Missing: the wreck entity, its part-state snapshot, its model/texture/smoke/lifetime configuration and the global toggle.

### Vehicle weapon overheating — MISSING

Reference: `R/common/driveables/EntityDriveable.java:1213-1225,1830-1850,2910`, `R/common/driveables/DriveableType.java`, `VehicleType.java` and `PlaneType.java` (`OverheatLimit`, `OverheatPenalty`, `OverheatSound`, `coolingBonus`), with heat state on `DriveableData` (`overheat`, `overheatSuffer`).
Each shot adds heat; passing the limit locks the weapon out for a penalty period and plays an overheat sound; heat bleeds off continuously at a rate improved by a cooling bonus.

Target checked: `T/common/entity/Driveable.java`, `T/common/entity/Vehicle.java`, `T/common/driveables/DriveableData.java`, `T/common/types/DriveableType.java`, `VehicleType.java`, `PlaneType.java`. No occurrence of "overheat" anywhere in the target; sustained fire is limited only by reload timing.

Missing: the heat accumulation, limit, penalty and cooling cycle, and its sound.

### Vehicle radar and target designation — MISSING

Reference: `R/common/driveables/DriveableType.java` (`hasRadar`, `hasPlaneRadar`, `digitalRadar`, `radarRange`, `radarRefreshDelay`, `radarDetectionRangeMultiplier`, `radarPositionOffset`, `radarVisible`, `RadarDetectableAltitude`), `R/common/network/PacketTargetSwitch.java` (cycles two designated target entity ids for a driveable), `R/common/network/PacketCurrentMissile.java` (syncs the selected missile stack to the HUD), `R/common/network/PacketMissileWeight.java`.
A radar-equipped vehicle sweeps at a configured range and refresh rate, low-flying aircraft drop below detection altitude, and the crew cycles designated targets and sees which round is currently selected.

Target checked: `T/common/types/DriveableType.java:186,697` (`OnRadar` — a passive "can be seen" flag only), `T/common/driveables/EnumDriveablePart.java:61-64` (`RADAR_1..4` are destructible parts), `T/client/render/ClientHudOverlays.java` (`renderAAGunHud`, ordnance lines), `T/network/`. No sweep, no detection range, no designation, no selected-round sync.

Missing: the active radar sweep with range, refresh and altitude gating; target designation and cycling; and selected-ordnance HUD synchronisation.

### Thermal vision and night sights — MISSING

Reference: reference resources `assets/minecraft/shaders/post/thermal.json`, `thermal_gen1.json`, `thermal_gen3.json`, `thermal_green*.json`, `thermal_red*.json` with matching `shaders/program/*.fsh` and `.vsh`; `R/common/driveables/DriveableType.java` and `VehicleType.java` (`HasThermal`, `HasThermalVision`, `ThermalGuis`, `ThermalVisionColor`, `ThermalVisionGeneration`, `thermalSight`, `NightSight`, `nightScope`, `SeatThermalGuis`, `PassengerThermalGuis`), `R/common/guns/GunType.java` and `AttachmentType.java` (`HasThermal`, `HasThermalVision`), `R/client/ThermalTeamStrobe.java` (friendly vehicles strobe on a 20-tick cycle so gunners can tell them apart), `R/client/particle/ThermalParticleRenderer.java`.
Thermal optics render the world through a post-process shader in one of three generations and three palettes, friendlies strobe an IFF pattern, particles get a thermal render pass, and night sights are a separate optic class.

Target checked: `T/common/types/AttachmentType.java` (`HasNightVision`), `T/common/types/GunType.java` (`AllowNightVision`), `T/common/types/ArmorType.java` (`NightVision`) — night vision exists as the vanilla effect; `T/client/render/CustomRenderType.java`, `T/mixin/ForceDarkLightMixin.java`; `../src/main/resources` contains no shaders at all.

Missing: thermal imaging entirely — post-process shaders, generations and palettes, the friendly IFF strobe, the thermal particle pass, and dedicated night-sight optics.

### Per-seat optics and gunsights — MISSING

Reference: `R/common/driveables/DriveableType.java` (`Gunsight`, `GunsightPos`, `HasGunsightPos`, `GunsightZoom`, `SeatGunsight`, `SeatGunsights`, `SeatGunsightZoom`, `SeatGunsightZooms`, `SeatOverlay`, `SeatHasScope`, `SeatAutoScope`, `SeatNightSight`, `SeatOpticsCamera`, `SeatOpticsMode`, `PilotOptics`, `PilotOpticsCamera`, `DriverOptics`, `DriverOpticsCamera`, `GunnerOptic`, `GunnerOpticCamera`, `PassengerGunsight`, `PassengerGunsightZoom`, `PassengerZoom`, `PassengerHasScope`, `PassengerAutoScope`, `HasScope`, `hasScope`, `passengerZoom`), `R/client/EntityCamera.java` (`getScopeCamera`, stabilised render pose).
Every crew station can have its own sight: an overlay texture, one or more zoom levels, an optic camera anchored at authored coordinates, an auto-scope-on-mount behaviour and a mode switch.

Target checked: `T/common/guns/ScopeZoom.java` and `T/common/types/IScope.java` (used only by `T/common/item/GunItem.java`), `T/common/driveables/SeatInfo.java`, `T/common/entity/Seat.java`, `T/client/render/MountedCameraView.java`, `T/client/render/ClientHudOverlays.java:419` (`renderScopeOverlay` is driven from hand-held guns). No zoom, sight overlay or optic camera on any seat.

Missing: seat-level optics — gunsight overlays, zoom levels, optic cameras, auto-scope, and optic mode switching for driver, pilot, gunner and passenger stations.

### Fire-control system HUD — MISSING

Reference: `R/common/driveables/DriveableType.java` — a ~45-key `FCS*` block (`FCSMode`, `FCSWeapon`, `FCSTool`, `FCSMaxRange`, `FCSDropScale`, `FCSHorizontalDrag`, `FCSVerticalDrag`, `FCSBarrelMaxCorrection`, `FCSMarker`, `FCSMarkerOffset`, `FCSMarkerScale`, `FCSMarkerSize`, `FCSRangePos`, `FCSRangeScale`, `FCSStabilizerPos`, `FCSLabel`, `FCSLabelPos`, `FCSColor`, `FCSRequireRangeKey`, `FCSEditMode`, `FCSDebug`, plus `*Sight` variants for the optic view) — with `Rangefinder`, `PilotOpticsHUD`, `PilotOpticsHUDMaxRange`, `PilotOpticsHUDRequireRangeKey`, `PilotOpticsHUDEdit`, `artilleryCalculator`, `HasMagicArtilleryMode`, `walterCalculator`, `walterMortar`.
A ballistic computer overlays a drop marker, a measured range readout and a stabiliser indicator on the gunner's sight, laid out at authored positions and scales, with a rangefinder key, an in-game edit mode for positioning and a debug view; artillery pieces additionally get an indirect-fire solution.

Target checked: `T/client/render/ClientHudOverlays.java` (hit markers, scope overlay, ammo, digital ammo, team info, kill messages, AA gun HUD, vehicle debug), `T/common/types/DriveableType.java`, `T/common/item/DriveableWeaponTooltip.java`. Nothing computes or displays a firing solution.

Missing: the fire-control HUD (drop marker, range readout, stabiliser indicator, layout, edit and debug modes) and the artillery firing-solution calculator.

### Turret stabilisation — MISSING

Reference: `R/common/driveables/VehicleType.java` (`TurretStabilization`), `R/common/driveables/DriveableType.java` (`Stabilizer`, `AddHeliStabilizerToSeat`, `FCSStabilizerPos`), `R/client/EntityCamera.java` (stabilised render pose).
A stabilised turret holds its aim point while the hull pitches and rolls, and the sight view is stabilised to match.

Target checked: `T/common/entity/Seat.java`, `T/common/driveables/SeatInfo.java`, `T/common/entity/Driveable.java`, `T/mixin/MountedRiderTurnMixin.java`, `T/client/render/MountedCameraView.java`. No occurrence of "stabiliz" in the target; turret aim is relative to the hull.

Missing: turret and sight stabilisation against hull motion.

### Active protection systems and CIWS — MISSING

Reference: hard-kill — `R/common/driveables/DriveableType.java` (`hasAPS`, `APSdelayMax`, `APSsound`), `R/common/driveables/EntityDriveable.java:250,2805-2809` (`APSchecker`, `APSdelay`, `APSmax`), `R/client/particle/EntityAPSGrenade.java`; soft-kill — `R/common/driveables/VehicleType.java` (`HasPassiveAPS`, `PassiveAPSCanForceAim`), `R/common/driveables/EntityDriveable.java:2844-2871` (`triggerPassiveAPSResponse`, `applyPassiveAPSAim`, `isPassiveAPSTargetFacing`), `R/common/network/PacketPassiveAPS.java`; counter-countermeasures — `R/common/guns/BulletType.java` (`BypassPassiveAPS`, `PassiveAPSECCM`); CIWS — `R/common/guns/BulletType.java` (`CIWSer`, `CIWSable`, `ciwsBullet`), `R/common/guns/EntityBullet.java:890,1117` (a CIWS round intercepts hostile projectiles in flight).
Vehicles intercept incoming missiles: a hard-kill launcher fires a countermeasure grenade on a recharge cycle, a passive system detects the launch point and slews the turret toward it, and CIWS rounds shoot down other projectiles. Ammunition can be built to defeat both.

Target checked: `T/common/entity/Vehicle.java:236-242` (flare and smoke countermeasures against lock-on — present), `T/common/types/DriveableType.java` (`HasFlare`, `FlareDelay`, `TimeFlareUsing`), `T/common/entity/Bullet.java`, `T/common/types/BulletType.java`. Decoy countermeasures exist; nothing intercepts a projectile and nothing reacts to a launch event.

Missing: hard-kill APS, passive APS launch detection and auto-slew, APS ECCM and bypass on ammunition, and CIWS projectile interception.

### Remote-controlled drone aircraft — MISSING

Reference: `R/common/driveables/EntityRemoteDronePlane.java`, `R/common/driveables/ItemRemoteDroneController.java` (link a drone by right-click, store its UUID and entity id on the controller, toggle the remote view), `R/common/driveables/RemoteDroneChunkManager.java` (554 lines: chunk tickets, an entity-tracker extension, a 1,000,000-block remote tracking range, watch budgeting), `R/common/driveables/QuadcopterPhysics.java`, `R/common/driveables/EnumRemoteDroneFlightMode.java` (`INHERIT/PLANE/HELICOPTER/QUADCOPTER`), `R/common/driveables/FlightController.java`, `R/common/driveables/PlaneType.java` (`RemoteDrone`, `RemoteDroneController`, `RemoteDroneFlightMode`, `RemoteDroneFlightModel`, `RemoteDronePilotHUD`, `RemoteDroneStaticSound`, `RemoteDroneBreakBlocks`, plus the `Quadcopter*` tuning set), `R/common/tools/ToolType.java` (`RemoteDroneController`), `R/client/RemoteDroneClientControl.java`, `R/common/network/PacketRemoteDroneControl.java`, `PacketRemoteDroneAction.java`, `PacketRemoteDroneWeaponState.java`.
An unmanned aircraft flown from a hand-held controller: the operator's view moves to the drone, its chunks stream in wherever it goes, and it flies under a plane, helicopter or quadcopter model with a dedicated pilot HUD and static-noise audio.

Target checked: `T/common/entity/Plane.java`, `T/common/driveables/LegacyPlanePhysics.java`, `T/common/driveables/physics/AircraftPerformancePhysics.java`, `T/common/types/PlaneType.java`, `T/common/types/ToolType.java`, `T/api/IControllable.java`, `T/network/`. No remote-control entity, item, chunk manager, flight mode or packets.

Missing: the entire remote drone aircraft subsystem.

### Troop transport — MISSING

Reference: `R/common/driveables/DriveableType.java` (`transport`, `troopCapacity`, `troopType`, `deployTroopSound`, `remountTroopSound`, `mobileInfantry`), `R/common/PlayerData.java:80` (`activeTroops`).
A transport carries a configured number of NPC troops of a given type as abstract cargo rather than as seated entities, with deploy and remount sounds.

Target checked: `T/common/types/DriveableType.java` (`NumPassengers`, `Passengers`, `CargoSlots` — seats and item cargo), `T/common/entity/Seat.java`, `T/common/PlayerData.java`. No troop-capacity concept.

Missing: abstract troop cargo, its capacity and type configuration, and deploy/remount handling.

### Crew oxygen and submarine depth — MISSING

Reference: `R/common/driveables/DriveableType.java` (`oxygen`, `maxOxygen`, `unlimitedOxygen`), `R/common/driveables/VehicleType.java` (`canDive`, `submarine`, `activationDepth`, `seaLevel`).
A sealed vehicle carries a finite oxygen supply that depletes while submerged, and submarines dive and operate at depth against a configured sea level.

Target checked: `T/common/types/DriveableType.java` (`MaxDepth`, `WorksUnderwater`, `FloatOnWater`, `Buoyancy`, `FloatOffset`), `T/common/driveables/physics/MarineDraftPhysics.java`, `T/common/types/ArmorType.java` (`Submarine` — a player armour effect). Buoyancy, draft and a maximum operating depth exist; no oxygen supply and no dive control.

Missing: crew oxygen depletion and limit, and submarine dive control.

### Vehicle animation channels — MISSING

Reference: `R/common/driveables/PlaneType.java` (`WheelCoverPosition1/2`, `WheelCoverRotation1/2`, `WheelCoverRate`, `WheelCoverRotRate`, `GearCoverPriority`, `WingFlapPosition1/2`, `WingFlapRotation1/2`, `WingFlapRate`, `WingFlapRotRate`, `AfterburnWing`, `AfterburnWingFlipped`, `swapInitialWing`, `carrierWingFlip`), `R/common/network/PacketPlaneAnimator.java`, `PacketAnimatedBarrelRecoil.java`.
Landing-gear bay covers, wing-flap surfaces and carrier wing folding animate between authored positions and rotations at authored rates, sequenced against gear state; gun barrels animate their recoil stroke, synchronised over the network.

Target checked: `T/common/types/PlaneType.java` (`HasGear`, `HasLandingGear`, `HasWing`, `FoldWingForLand`, `AutoDeployLandingGearNearGround`, `AutoOpenDoorsNearGround`), `T/common/entity/Plane.java:94,176,295-297`, `T/network/client/PacketDriveableRenderState.java`, `T/common/driveables/Propeller.java`. Gear, doors and wing folding toggle as states; there are no intermediate keyframes, rates or bay-cover channels, and no barrel recoil animation.

Missing: gear-cover and wing-flap animation channels with their positions, rotations and rates; gear-cover sequencing priority; and animated barrel recoil.

### Altitude-dependent aircraft performance — MISSING

Reference: `R/common/driveables/PlaneType.java` (`flightCeiling`, `maxAltitude`, `highAltMax`, `highAltMaxDry`, `maxSpeedDry`, `HasAfterBurner`, `afterBurnFuelPenalty`, `afterburnOffBonus`, `AfterBurnName`, `climbRate`, `cruiseSpeed`, `diveBonus`, `maxG`, `gravityMultiplier`, `area`, `mass`).
Aircraft have a service ceiling and separate dry and afterburner top speeds at low and high altitude, with an afterburner that costs extra fuel and leaves a performance bonus when disengaged.

Target checked: `T/common/driveables/physics/AircraftPerformancePhysics.java` (a real lift and drag model — `WingArea`, `Mass`, `MaxThrust`, `Supersonic`, stall speed, `stallRecoveryPitchDegrees`), `T/common/driveables/physics/VehiclePhysicsUnits.java:AIR_DENSITY` (a single constant, used at `AircraftPerformancePhysics.java:91,194`), `T/common/types/PlaneType.java`. The aerodynamic model is materially better than the reference's, but air density does not vary with altitude, so there is no ceiling and no high-altitude regime.

Missing: altitude-varying air density and the service ceiling, high-altitude speed regimes, and the dry/afterburner speed split with its fuel cost.

### Landing-surface constraints — MISSING

Reference: `R/common/driveables/PlaneType.java` (`carrierLandable`, `helipadLandable`, `takeoffSpeed`, `parasitePlane`), `R/common/driveables/DriveableType.java` (`helipad`, `parkingSpot`, `NeedsGear`, `needsGear`).
An aircraft can be restricted to landing on carrier decks or helipads, requires gear down to land, and parasite aircraft attach to a carrier aircraft.

Target checked: `T/common/types/PlaneType.java` (`TakeoffSpeed`, `HasGear`, `FoldWingForLand`), `T/common/types/DriveableType.java` (`PlaceableOnLand`, `PlaceableOnWater`, `PlaceableOnSponge` — placement restrictions, not landing restrictions), `T/common/entity/Plane.java`. Takeoff speed is present.

Missing: carrier and helipad landing restrictions, parking spots, gear-required-to-land enforcement, and parasite aircraft attachment.

### Distant and situational vehicle audio — MISSING

Reference: `R/common/driveables/DriveableType.java` (`DistantSoundPrimary`, `DistantSoundSecondary`, `DistantFlareSound`, `DistantFlareSoundRange`, `farSound`, `farSoundRange`, `TracksSound`, `TracksSoundRange`, `DriftSound`, `DriftSoundLength`, `sonicBoomSound`, `loudCannon`, `earRape`, `LegacyEngineSounds`, `CrashSoundRange`, `DetonationSoundRange`), `R/common/driveables/PlaneType.java` (`StukaSound`, `StukaSoundLength`, `StukaSoundRange`, `StukaSpeed`, `PlaneCrashSound`, `HeliCrashSound`), `R/common/guns/BulletType.java` (`FlyBySound`, `DistantDetonateSound`, `DistantDetonateSoundRange`), `R/client/CustomSoundLoop.java`.
Weapons and vehicles play a different sample at distance from the one heard up close; tracks, drifting, sonic booms, dive-siren (Stuka) audio above a speed threshold, and projectile fly-by cracks are all separate cues with their own ranges.

Target checked: `T/common/types/GunType.java` (`DistantShootSound`, `DistantSound`, `DistantSoundRange` — present for hand guns), `T/common/types/DriveableType.java` (`EngineSound`, `IdleSound`, `StartSound`, `BackSound`, `ShootSoundPrimary/Secondary`, `LockOnSound`, `FlareSound`, `ExitSound`, each with a `*SoundRange`), `T/client/SoundHelper.java`, `T/network/client/PacketPlaySound.java`. Distant variants exist for guns only.

Missing: distant sound variants for vehicle weapons and flares, track / drift / sonic-boom / dive-siren audio, and projectile fly-by cracks.

### Helicopter crash behaviour — PARTIAL

Reference: `R/common/driveables/PlaneType.java` (`HeliCrashEffects`, `HeliCrashSpin`, `HeliCrashSpinStrength`, `HeliCrashSmokeHealth`, `HeliCrashSound`, `heliSpeedLimit`, `OilCookTime`), `R/common/driveables/DriveableType.java` (`EngineGore`, `TailGore`, `WingGore`, `tailLoss`, `wingLoss`, `engineLoss`).
A damaged helicopter enters an uncontrolled spin whose strength scales with damage, starts smoking below a health threshold, and plays a dedicated crash sound; losing a tail, wing or engine has its own visible consequence.

Target checked: `T/common/driveables/PlaneCrashDamage.java`, `T/common/driveables/DriveableCrashExplosion.java`, `T/network/client/PacketDriveableCrashFireball.java`, `T/common/entity/Plane.java` with `SpinWithoutTail` in `T/common/types/PlaneType.java`, `T/common/driveables/DriveableImpactDamage.java`. Crash damage, a crash fireball and tail-loss spin are present.

Missing: damage-scaled spin strength, the smoke-below-health-threshold state, the dedicated helicopter crash sound, and per-part loss effects for wing and engine.

### Aircraft attitude HUD — MISSING

Reference: `R/client/PlaneHUD.java` (424 lines: roll, pitch and yaw drawn as a flight instrument overlay), `R/common/driveables/DriveableType.java` (`hasHUD`, `HeliGUI`, `heliGUI`, `heliGuiSeat`, `hudColorR/G/B`).
Pilots get an attitude and heading instrument overlay, with helicopter-specific variants and a configurable HUD colour.

Target checked: `T/client/render/ClientHudOverlays.java` (`renderAAGunHud`, `renderVehicleDebug`, ordnance lines), `T/common/item/DriveablePhysicsTooltip.java`. A debug readout and an AA-gun HUD exist; no pilot instrument overlay.

Missing: the aircraft attitude and heading HUD, its helicopter variant and its colour configuration.

### Seat mount desynchronisation recovery — MISSING

Reference: `R/common/network/PacketSeatCheck.java`, `PacketSeatKickDetected.java:1-10` (the client reports the seat and driveable ids it believes it is riding), `PacketSeatMountConfirm.java`, `PacketForceSeatMount.java`, `PacketSeatUpdates.java`, `PacketDriveableResyncRequest.java`.
The client continuously validates that it is still mounted in the seat the server thinks it is; on a mismatch it reports the discrepancy and the server force-remounts it, and either side can request a full driveable resync.

Target checked: `T/common/entity/Seat.java`, `T/common/driveables/SeatCycle.java`, `T/network/server/PacketRequestDismount.java`, `T/network/client/PacketDriveableRenderState.java`. Mounting and dismounting are handled, but nothing detects or repairs a mount desync.

Missing: seat-state validation, mismatch reporting, forced remount and driveable resync requests.

### Submarine-launched ballistic missiles — MISSING

Reference: `R/common/driveables/DriveableType.java` (`slbmDelay`, `slbmFlightType`, `slbmRange`, `slbmStrength`, `slbmWarheadType`).
A strategic launch system on a vessel: a launch delay, a selectable flight profile, a range, a yield and a warhead type.

Target checked: `T/common/types/DriveableType.java` (`MissileSlots`, `ShellSlots`, `BombSlots`, `MineSlots`), `T/common/types/BulletType.java` (`VLS`). Vertical launch of ordinary missiles exists.

Missing: the strategic launch system with its flight profile, range, yield and warhead selection.

### Hardpoints and ordnance weight — MISSING

Reference: `R/common/driveables/DriveableType.java` (`hardpoint`, `weightLimit`), `R/common/guns/BulletType.java` (`missileWeight`, `missileWingSpan`, `missileVisible`, `missileElevation`, `missileForward`), `R/common/driveables/PlaneType.java` (`missileVisible`, `missileWingSpan`), `R/common/network/PacketMissileWeight.java`.
Aircraft carry ordnance on discrete hardpoints against a total weight limit; carried missiles render on the pylons at authored positions and their weight is synchronised so performance reflects the loadout.

Target checked: `T/common/types/DriveableType.java` (`MissileSlots`, `BombSlots`, `ShellSlots`, `MineSlots` — flat inventory counts), `T/common/driveables/DriveableData.java`, `T/client/render/entity/DriveableRenderer.java`. Ordnance is inventory, not externally carried mass.

Missing: hardpoint positions with rendered stores, the total weight limit, and the loadout's effect on performance.

### Door-state player visibility — PARTIAL

Reference: `R/common/driveables/DriveableType.java` (`SetPlayerInvisibleOnDoorOpen`, `SetPlayerInvisibleOnDoorClose`, `SetPassengerInvisible`, `invisiblePassenger`, `primaryDoor`, `secondaryDoor`, `SetDriverInvincible`, `Invincible`, `invincible`).
Occupant visibility is tied to door state — a closed hatch hides the crew, an open one reveals them — with separate driver and passenger control and an invincibility flag while enclosed.

Target checked: `T/common/types/DriveableType.java` (`SetPlayerInvisible`), `T/common/types/PlaneType.java` (`HasDoor`, `FlyWithOpenDoor`, `AutoOpenDoorsNearGround`), `T/common/types/VehicleType.java` (`HasDoor`, `ShootWithOpenDoor`). Doors and a static occupant-invisibility flag both exist, but they are independent of each other.

Missing: coupling occupant visibility to door state, the separate driver and passenger split, and crew invincibility while enclosed.

### Ground vehicle terrain handling — PARTIAL

Reference: `R/common/driveables/VehicleType.java` (`TerrainPenalty`, `terrainPenalty`, `raceCar`, `NewDrivingModel`, `nuDrivingModel`, `SurfaceSpeed`, `AccelerationSpeed`, `DecelerationSpeed`, `BoostLimit`, `BrakeMultiplier`, `DiveSpeed`).
Vehicles lose speed on rough ground by a configured penalty, and a race-car handling profile is selectable alongside the standard and "new" driving models.

Target checked: `T/common/driveables/physics/GroundPropulsionPhysics.java`, `GroundSlopePhysics.java`, `T/common/driveables/SuspensionPhysics.java`, `T/common/driveables/DriveableControlPhysics.java`, `T/common/types/VehicleType.java` (`UseRealisticAcceleration`, `BrakingModifier`, `ThrottleDecay`, `FourWheelDrive`, `Tank`, `TankMode`), `T/config/ModCommonConfig.java` (`forceLegacyVehiclePhysics`). Slope, suspension, drive type and a realistic acceleration model are present and more capable than the reference's; drift exists.

Missing: a surface-roughness speed penalty, and the race-car handling profile.

### Mecha morale — MISSING

Reference: `R/common/driveables/mechas/MechaType.java` (`morale`, `panicSound`, `panicTime`, `runAmokSound`, `unpunchable`, `AcceptAllGuns`, `AllowAllGuns`), `R/common/driveables/DriveableType.java` (`canPanic`).
A mecha (or crewed vehicle) has a morale value; when it breaks, the machine panics for a configured time with its own sound and can run amok out of the pilot's control.

Target checked: `T/common/driveables/MechaPhysics.java`, `T/common/entity/Mecha.java`, `T/common/types/MechaType.java`, `T/common/driveables/EnumMechaItemType.java`. No morale, panic or amok state.

Missing: the morale / panic / run-amok state machine and its audio.

---

## Teams and game modes

### Domination, Conquest and Assault game types — MISSING

Reference: `R/common/teams/GameTypeDomination.java` (341 lines: `captureTicks`, `ticketDrainInterval`, `killTicketValue`), `R/common/teams/GameTypeConquest.java` (475 lines: adds `deployDelaySeconds`, `preCaptureEndPoints`), `R/common/teams/GameTypeAssault.java` (517 lines: adds `reinforcementRefill`, `stageTimeMinutes`, `baseDefenderDeployDelaySeconds`, `defenderDeployDelayPerLostObjectiveSeconds`), each with `createHudSnapshot`, `roundStart`, `tick`, `roundEnd`, `roundCleanup` and `teamHasWon`; supported by `R/common/teams/TeamObjectiveSnapshot.java` and `TeamsHudSnapshot.java`.
Three ticket-based objective modes: hold points to drain the enemy ticket pool (Domination), a Conquest variant with deploy delays and optional pre-captured end points, and a staged Assault where attackers push a sequence of objectives against a reinforcement pool on a stage timer.

Target checked: `T/common/teams/GameTypes.java` (`DEATHMATCH`, `TEAM_DEATHMATCH`, `CAPTURE_THE_FLAG`, `ZOMBIES`), `T/common/teams/GameType.java`, `T/common/teams/GameTypeCTF.java`, `T/common/teams/TeamsRound.java`.

Missing: all three game types, ticket and reinforcement scoring, and their objective HUD snapshots.

### Capture points — MISSING

Reference: `R/common/teams/capturepoint/BlockCapturePoint.java`, `TileEntityCapturePoint.java`, `CapturePointController.java` (126 lines: `scan` returns per-team occupant counts, contested state and the dominant team), `R/common/teams/ITeamCapturePoint.java`, `R/common/network/PacketCapturePointEdit.java`, `R/client/gui/teams/GuiCapturePointEditor.java`, `R/client/gui/teams/GuiDominationHud.java`, `R/client/model/RenderCapturePoint.java`, `R/client/teams/JourneyMapCapturePointHook.java` (neutral, contested and capturing colours on the minimap).
A placeable capture point block scans the players standing on it, resolves contested versus dominant ownership, progresses capture over `captureTicks`, renders its state in the world and on the minimap, and is configured through an in-game editor.

Target checked: `T/common/block/TeamSpawnerBlock.java`, `T/common/block/entity/TeamSpawnerBlockEntity.java`, `T/common/teams/ITeamBase.java`, `T/common/teams/ITeamObject.java`, `T/common/entity/Flag.java`, `T/common/entity/Flagpole.java`, `T/client/gui/TeamsBaseEditScreen.java`. Bases, spawners and CTF flags exist; no capture point.

Missing: the capture-point block, its tile entity, the capture scan and progress logic, its editor, its world and minimap rendering, and its HUD.

### Deployment and spawn selection — MISSING

Reference: `R/client/gui/teams/GuiConquestDeploy.java` (1026 lines), `R/common/network/PacketConquestSpawnSelect.java`, `R/common/teams/DeployLayoutEntry.java`, `R/common/network/PacketDeployLayoutSave.java`.
On death the player gets a deployment map and chooses which friendly objective to spawn at, subject to the mode's deploy delay; the layout of the map's spawn markers is authored and saved per map.

Target checked: `T/client/gui/TeamsChooseLoadoutScreen.java`, `T/client/gui/TeamsSelectScreen.java`, `T/client/gui/TeamsLoadoutHubScreen.java`, `T/common/teams/TeamsMap.java`, `T/network/server/PacketLoadoutAction.java`. Team and loadout selection exist; spawn location is not chosen by the player.

Missing: the deployment map screen, spawn-point selection with its deploy delay, and the authored and saved deploy layout.

### Rank-up feedback and class gating — PARTIAL

Reference: `R/client/gui/teams/GuiRankUpPopup.java`, `R/common/network/PacketRankUp.java`, `R/common/teams/CommandRanks.java` (`setRank`, `resetRank`, `prestige`), `R/common/teams/PlayerClass.java` (`RequiredPrestigeLevel`, `ClassLimit`, `ClassLimitPercent`, `ClassLimitPercentage`).
Ranking up shows an animated popup; classes can require a prestige level and can be capped to an absolute count or a percentage of the team.

Target checked: `T/client/gui/TeamsRankIcon.java:17-23` (draws rank and prestige icons — both exist as state), `T/common/teams/PlayerStats.java`, `T/common/types/PlayerClass.java` (`UnlockLevel` only), `T/common/command/TeamsCommand.java` (`resetrank`, `xp`, `xpmultiplier`), `T/network/client/PacketTeamsState.java`.

Missing: the rank-up popup and its packet, the prestige requirement on classes, and absolute and percentage class limits.

### Map rotation administration — PARTIAL

Reference: `R/common/teams/CommandTeams.java` (`addMap`, `addMapWithBounds`, `removeMap`, `renameMap`, `setMapName`, `setMapImage`, `goToMap`, `nextMap`, `addMapToRotation`, `removeMapFromRotation`, `addToRotation`, `removeFromRotation`, `addRotation`, `removeRotation`, `listRotation`, `useRotation`, `roundsGenerator`, `votingTime`, `scoreDisplayTime`, `autobalancetime`).
Operators build named maps with world bounds and a map image, assemble them into named rotations, jump between maps, and tune voting, score and autobalance timings from chat.

Target checked: `T/common/command/TeamsCommand.java` (`map`, `maps`, `round`, `rounds`, `nextRound`, `listMaps`, `listRounds`, `arena`, `setvariable`), `T/common/teams/TeamsManager.java:57,164,364-416` (`NBT_ROTATION_INDEX`, `rotationIndex`, `startRound((rotationIndex + 1) % rounds.size())`), `T/common/teams/TeamsMap.java`, `T/common/teams/TeamsSavedData.java`. A single round rotation with an index exists, as do map and round commands.

Missing: named multi-rotation management, map bounds and map images, direct map jumping, and the voting / score-display / autobalance timing commands.

### Per-entity lifetime and spawn-rate limits — MISSING

Reference: `R/common/teams/CommandTeams.java` and `TeamsManager.java` (`planeLife`, `vehicleLife`, `mgLife`, `aaLife`, `mechaLife`, `planeRate`, `vehicleRate`, `seatRate`, `rviRate`).
Match rules cap how long a spawned plane, vehicle, deployable MG, AA gun or mecha survives, and how often each may be spawned — the standard anti-spam controls for a public server.

Target checked: `T/common/teams/TeamsManager.java` (`explosionsBreakBlocks`, `driveablesBreakBlocks`, `vehiclesNeedFuel`, `armourDrops`, `bombsEnabled`, `shellsEnabled`, `bulletsEnabled`, `forceAdventureMode`, `overrideHunger`, `canBreakGuns`, `weaponDrops`, `survivalCanBreakVehicles`, `survivalCanPlaceVehicles`), `T/common/command/TeamsCommand.java`.

Missing: per-entity-class lifetime caps and spawn-rate throttles.

### Match rule modes — MISSING

Reference: `R/common/teams/CommandTeams.java` (`pacifism`, `raiding`, `movieProp`, `bltss`, `autoBLTSS`, `showbltss`, `bleeding`, `noHunger`, `canBreakGlass`, `vehiclesCanZoom`, `shake`, `ping`, `seaLevel`).
Server-wide match modes: a no-damage pacifism mode, a raiding mode, a prop/cinematic mode, and toggles for bleeding, glass breaking, vehicle zoom, screen shake and the ping display.

Target checked: `T/common/teams/TeamsManager.java`, `T/common/command/TeamsCommand.java` (`explosions`, `forceAdventure`, `fuelNeeded`, `arena`, `survival`, `kit`), `T/config/ModCommonConfig.java` (`shootablesCanBreakGlass` exists as a config option, not as a match rule).

Missing: the pacifism, raiding and prop modes, and the per-match toggles for bleeding, vehicle zoom, screen shake and ping display.

### Team victory presentation — MISSING

Reference: `R/common/teams/Team.java` (`VictorySound`, `HasVictorySound`, `DefeatSound`, `VictoryFlagTexture`).
Each team has its own victory and defeat stingers and a victory flag texture shown at round end.

Target checked: `T/common/types/Team.java` (`TextColour`, `AllowedForRoundsGenerator`), `T/client/gui/TeamsMissionResultsScreen.java`, `T/common/teams/TeamsRound.java`. A results screen exists.

Missing: per-team victory and defeat audio, and the victory flag texture.

---

## AI and NPCs

### Armed NPC bots — MISSING

Reference: `R/common/entity/EntityFlansBot.java` (785 lines: an `EntityMob` implementing `IRangedAttackMob`, with `shootDelay`, `hesitation`, `minigunSpeed`, reload state, warmup sounds and owner following), `R/common/entity/FlansBotType.java` (equips hat, chest, legs and shoes, picks from `CommonGun`/`RareGun`/`SuperRareGun` tiers, `IQlevel`, `evil`, `coverage`, `maxHealth`, `movementSpeed`, `knockbackResistance`), `R/common/entity/EntityAIArrowAttackFlan.java`, `R/common/entity/EntityAIFollowOwner.java`, `R/common/entity/ItemFlansBot.java`, `R/common/entity/DamageSourceFlansBot.java`.
Spawnable NPCs that wear Flan's armour, carry Flan's guns drawn from rarity tiers, use the mod's own gun mechanics (warmup, reload, minigun spin-up), aim with a configurable competence level, and can be commanded to follow their owner.

Target checked: `T/common/entity/` (`AAGun`, `Bullet`, `DeployedGun`, `Driveable`, `Flag`, `Flagpole`, `Grenade`, `GunItemEntity`, `Mecha`, `Parachute`, `Plane`, `Seat`, `Shootable`, `Vehicle`, `Wheel`), `T/apocalyse/common/entity/SurvivorEntity.java` (an Apocalypse-mode mob, not a configurable gun-carrying bot), `T/common/teams/GameTypeZombies.java`. No `FlansBot` equivalent.

Missing: the configurable armed NPC, its type definition with gun rarity tiers, its ranged-attack and follow-owner AI, its spawn item, and its damage source.

### AI target aircraft — MISSING

Reference: `R/common/entity/EntityPlaneTarget.java` (225 lines), `R/common/entity/EntityFlyByPlane.java`, `R/common/entity/PlaneTargetType.java` (`Plane`/`PlaneType` shortname, `Maneuver` — e.g. `straight` — `ManeuverPeriod`, `Throttle`, `ControlStrength`, `PaintjobID`), `R/common/teams/CommandPlaneTarget.java`.
Spawnable AI aircraft that fly authored manoeuvre patterns on a period, used as AA gunnery targets and as scenery fly-bys, summoned by command.

Target checked: `T/common/entity/Plane.java`, `T/common/types/PlaneType.java`, `T/common/command/FlanEntityCommand.java`, `T/common/types/AAGunType.java` (`TargetPlanes`, `TargetDriveables` — AA guns can target planes, but nothing flies itself).

Missing: the AI target aircraft, its manoeuvre-pattern type definition, and its spawn command.

---

## Networking, integrity and client behaviour

### Content pack hash verification — MISSING

Reference: `R/common/sync/Sync.java` (SHA-512 hashes of loaded content, cached), `R/common/sync/SyncEventHandler.java`, `R/common/network/PacketHashSend.java`, `PacketChecker.java`, `R/common/FlansMod.java` (`kickNonMatchingHashes`).
On join, the client sends hashes of its loaded content packs; a server configured to enforce this kicks clients whose packs do not match, preventing desync and stat tampering.

Target checked: `T/network/client/PacketSyncCommonConfig.java`, `T/config/ModCommonConfigSync.java`, `T/config/ModCommonConfig.java` (`validateContentReferencesOnWorldLoad` — a local integrity check of content references, not a client/server comparison), `T/ContentManager.java`, `T/network/PacketHandler.java`. Config is synchronised; content packs are not verified between client and server.

Missing: content-pack hashing, the hash exchange packets, and the kick-on-mismatch enforcement.

### Ping display — MISSING

Reference: `R/client/ping/RenderPingHandler.java`, `R/common/teams/CommandTeams.java` (`ping` toggle).
Player ping is rendered in-world or on the HUD, toggleable by the server.

Target checked: `T/client/render/ClientHudOverlays.java`, `T/client/render/KillMessageFeed.java`, `T/common/command/TeamsCommand.java`. Matches for "ping" in the target are all unrelated (`mapping`, `Shipping`, `stopping`).

Missing: the ping display and its server toggle.

### First-person body rendering — MISSING

Reference: `R/client/virtualreality/SexDoll.java` (a `Render` subclass drawing the player's own biped limbs from the first-person camera), `R/client/virtualreality/AngelicaFirstPersonBodyCompat.java:1-25` (renders the body on `RenderWorldLastEvent` under the Angelica shader mod, with a separate pass when thermal vision is active), `R/common/FlansMod.java` (`firstPersonBodyEnable`, `armsEnable`).
The player sees their own legs and torso in first person, with a compatibility path for shader mods and an extra pass so the body appears under thermal optics. (The package and class names are fork gags; the functionality is a first-person body renderer.)

Target checked: `T/client/render/` (`CustomArmorLayer.java`, `PlayerSkinOverrides.java`, `LegacyTransformApplier.java`), `T/mixin/SeatedPlayerRendererMixin.java`, `T/client/render/item/GunItemRenderer.java:87,140` (first-person handling is gun-model posing only), `T/config/ModCommonConfig.java` (`enableArms` covers the gun's arm models, not a body).

Missing: first-person body rendering and its shader-mod compatibility path.

### Client projectile render governor — PARTIAL

Reference: `R/client/ClientPerformanceGovernor.java` (samples FPS, culls non-critical projectiles below `clientProjectileCullFps`, batches fancy tracks below `clientFancyTrackBatchFps` and resumes above `clientFancyTrackResumeFps`, with an emergency threshold), `R/common/FlansMod.java` (`enableClientProjectileRenderGovernor`, `enableLowFpsFancyTrackBatching`, `clientFancyTrackDetailDistance`, `optimizationFpsFloor`), `R/client/JourneyMapPerformanceFix.java`.
The client measures its own frame rate and adaptively degrades projectile and vehicle-track rendering to recover it, while preserving "critical" projectiles.

Target checked: `T/client/render/entity/DriveableImpostorCache.java`, `T/config/ModCommonConfig.java` (`enableDriveableLod`, `driveableImpostor*`, `bulletRenderDistance`, `particleRenderDistance`, `distantParticleDensity`, `fullParticleDensityDistance`, `maxFlansParticlesPerTick`). The target has a comparable and arguably better *static* budget: distance-based LOD, driveable impostors and particle density falloff.

Missing: the adaptive, FPS-feedback-driven element — nothing measures frame rate or degrades further when the client is struggling.

### Iron sight calibration command — MISSING

Reference: `R/client/IronSightCalibrationCommand.java` (`/flansironsight <on|off|toggle>`), `R/common/guns/GunType.java` (`IronSightOffset`, `IronSightPitch`, `ScopeAlignment`, `Xoffset`, `Yoffset`, `Zoffset`, `dillZoomModifier`, `dillElevator`).
A client command toggles a calibration mode for positioning iron sights, with the resulting offsets stored on the gun type — a pack-authoring aid.

Target checked: `T/common/command/` (`DefaultAmmoCommand`, `DigitalAmmoCommand`, `FMParticleCommand`, `FlanEntityCommand`, `TeamsCommand`, `VehiclePhysicsCommand`), `T/common/types/GunAnimationConfig.java` (`animGunOffset`, `animScopeAttachPoint` — authored offsets exist, but no in-game calibration mode).

Missing: the iron-sight calibration command and the runtime sight-alignment adjustment it drives.

### Effect particles — PARTIAL

Reference: `R/client/particle/` — `Entityblood.java`, `EntityGroundBlood.java`, `EntityLocked.java` (lock-on indicator), `EntityFMNuke.java`, `EntitytankDeath.java`, `EntityshipDeath.java`, `EntityShipSmoke.java`, `EntityWaterSmoke.java`, `EntityWaterSmokeMini.java`, `EntityOverKill.java`, `EntityShellCasing.java`, `EntityNuFlash.java`, `EntityNuMuzzle.java`, `EntityNuSpark.java`.
A wider effect vocabulary: blood spray and persistent ground pools, an on-screen lock-on marker, a nuclear detonation effect, distinct tank and ship death effects, water and ship smoke, and an overkill indicator.

Target checked: `T/common/FlanParticles.java` and `T/client/particle/` (`AfterburnParticle`, `BigSmokeParticle`, `Debris1Particle`, `FlareParticle`, `FlashParticle`, `FmFlameParticle`, `FmMuzzleFlashParticle`, `FmSmokeParticle`, `FmTracerParticle`, `LegacyBlockParticle`, `LegacyExplodeParticle`, `LegacyItemParticle`, `ParticleBase`, `RocketExhaustParticle`, `SmokeBurstParticle`, `SmokeGrenadeParticle`); casings are model-based via `CasingModel` and `CasingTexture`, and a blood *screen overlay* exists at `T/client/render/ClientHudOverlays.java:391`.

Missing: blood spray and ground-pool particles, the lock-on marker particle, the nuclear detonation effect, tank and ship death effects, water and ship smoke, and the overkill indicator.

---

## Third-party integration

### Baris tech-tree export — MISSING

Reference: `R/common/BarisTechTreeHandler.java` (102 lines; Gson serialisation of Flan's content into a tech tree for mod id `barismodremaster`, with a `@Skip` exclusion strategy), plus `R/common/guns/AttachmentType.java` (`barisInfrared`, `barisLaser`).
Flan's content is exported as tech-tree data for the Baris mod, and attachments declare Baris-specific infrared and laser capabilities.

Target checked: `T/` (no tech-tree handler), `T/ContentManager.java`, `T/common/recipe/RecipeJsonGenerator.java`, `T/common/types/AttachmentType.java`.

Missing: the tech-tree export and the Baris attachment capability flags.

### Minimap capture-point overlay — MISSING

Reference: `R/client/teams/JourneyMapCapturePointHook.java` (253 lines; draws capture points on the JourneyMap minimap with neutral, contested and capturing colours), `R/client/JourneyMapPerformanceFix.java`.
Objective state is mirrored onto the minimap so players can read the battle at a glance.

Target checked: `T/client/`, `T/client/teams/TeamsClientState.java`, `T/client/gui/`. No minimap integration.

Missing: the minimap objective overlay. This depends on the capture-point subsystem, which is also absent.

---

## Uncertain

### Vehicle crew damage model — UNCERTAIN

Reference: `R/common/driveables/DriveableType.java` (`damageVsCrew`, `crewEngine`), `R/common/driveables/EntityDriveable.java:2810` (`this.damageVsCrew = type.damageVsCrew`).
Rounds that penetrate can damage the crew inside rather than the vehicle, and a crewed engine behaves differently from an unmanned one.

Target checked: `T/common/types/DriveableType.java` (`SetupCrewedPart`, `SetuCrewedpPart`, `SetupArmoredPart`, `SetupCompositeArmoredPart` are parsed), `T/common/driveables/armor/VehicleProjectileDamageResolver.java`, `T/common/driveables/DriveablePart.java`, `T/common/entity/Seat.java`. Crewed parts exist in the target's part setup and a penetrating hit resolves against armour, but whether occupant damage on penetration is modelled could not be established from the resolver alone.

Missing: possibly the crew-damage-on-penetration coefficient. Needs a read of the target's full damage resolution path from `VehicleProjectileDamageResolver` through `Seat` occupant handling.

### Dynamic light sources beyond projectiles — UNCERTAIN

Reference: `R/client/IDynamicLightSource.java` (a general interface: `getAttachmentEntity()`, `getLightLevel()`), implemented by projectiles and, given the interface's generality, potentially by vehicles and flares.
Any entity can advertise a light level to the dynamic lighting integration.

Target checked: `T/client/ModClient.java:551`, `T/common/types/ShootableType.java:127,321` (`HasLight`, `HasDynamicLight`). Dynamic lighting exists for shootables specifically.

Missing: possibly dynamic light emission from vehicles, flares and other non-projectile entities. Which reference classes implement the interface was not enumerated.

### Reload staging depth — UNCERTAIN

Reference: `R/common/guns/GunType.java` (`StagedReloadTime`, `TacticalReloadTime`, `ForceStagedReload`, `LoopReloadAnimation`, `UseLoopReloadAnimation`, `StartReloadAnimation`, `EndReloadAnimation`, `SecondaryReloadAnimation`, `ActionAnimation`, `ActionEndSound`), `R/common/guns/AttachmentType.java` (`ForceStagedReload`, `ReloadTimeMultiplier`).
Reloads split into start, loop and end phases with separate timings, a tactical (round-still-chambered) variant, and an attachment that can force staged behaviour.

Target checked: `T/common/guns/reload/GunReloader.java`, `ReloadPlan.java`, `PendingReload.java`, `T/common/types/GunAnimationConfig.java` (`animStagedReload`, `animStagedLeftArmReloadPos/Rot`, `animStagedRightArmReloadPos/Rot`, `animStagedRotateClipHorizontal/Vertical`, `animStagedTiltClip`, `animStagedTranslateClip`, `animNumBulletsInReloadAnimation`, `animLoadClipTime`, `animUnloadClipTime`). Staged reloading and its animation set clearly exist and look at least as capable as the reference's.

Missing: possibly the distinct tactical-reload timing and the attachment-forced staged reload. Needs a read of `GunReloader`'s timing selection against a chambered round.

---

## Summary

| Subsystem | Feature | Status | Confidence |
| --------- | ------- | ------ | ---------- |
| Weapons | Directional melee and bayonets | MISSING | HIGH |
| Weapons | Ricochet / over-pen / non-pen feedback | MISSING | HIGH |
| Weapons | Tracer beam rendering | MISSING | HIGH |
| Weapons | Per-ammo alternate model/texture/trail | MISSING | HIGH |
| Weapons | Suppression on near-miss | MISSING | HIGH |
| Weapons | Beyond-visual-range projectile chunk loading | MISSING | HIGH |
| Weapons | Radar-guided / anti-radiation / self-guided seekers | MISSING | HIGH |
| Weapons | Proportional navigation guidance | MISSING | HIGH |
| Weapons | Missile twirl and ground-skim profiles | MISSING | HIGH |
| Weapons | Fly-over top attack | PARTIAL | HIGH |
| Weapons | Wire-guided missile and visible wire | MISSING | HIGH |
| Weapons | FPV drone munitions | MISSING | HIGH |
| Weapons | Naval ordnance behaviours | PARTIAL | HIGH |
| Weapons | Illumination and chemical shells | MISSING | HIGH |
| Weapons | HEAT and HESH shell classes | MISSING | MEDIUM |
| Weapons | glTF skeletal gun animation | MISSING | HIGH |
| Weapons | Firing screen shake | MISSING | HIGH |
| Weapons | Gun carry limit | MISSING | HIGH |
| Weapons | Gun tier economy | MISSING | HIGH |
| Weapons | Ammunition evolution rounds | MISSING | MEDIUM |
| Weapons | Muzzle and chamber smoke models | PARTIAL | MEDIUM |
| Armour | Per-zone armour coefficients and extended hitboxes | MISSING | HIGH |
| Armour | Reserve armour plates | MISSING | HIGH |
| Armour | Energy shields on armour | MISSING | HIGH |
| Armour | Bleeding and field medicine | MISSING | HIGH |
| Armour | Armour pouches and weapon mounts | MISSING | HIGH |
| Vehicles | Destroyed-vehicle wrecks | MISSING | HIGH |
| Vehicles | Vehicle weapon overheating | MISSING | HIGH |
| Vehicles | Vehicle radar and target designation | MISSING | HIGH |
| Vehicles | Thermal vision and night sights | MISSING | HIGH |
| Vehicles | Per-seat optics and gunsights | MISSING | HIGH |
| Vehicles | Fire-control system HUD | MISSING | HIGH |
| Vehicles | Turret stabilisation | MISSING | HIGH |
| Vehicles | Active protection systems and CIWS | MISSING | HIGH |
| Vehicles | Remote-controlled drone aircraft | MISSING | HIGH |
| Vehicles | Troop transport | MISSING | HIGH |
| Vehicles | Crew oxygen and submarine depth | MISSING | MEDIUM |
| Vehicles | Vehicle animation channels | MISSING | HIGH |
| Vehicles | Altitude-dependent aircraft performance | MISSING | MEDIUM |
| Vehicles | Landing-surface constraints | MISSING | HIGH |
| Vehicles | Distant and situational vehicle audio | MISSING | HIGH |
| Vehicles | Helicopter crash behaviour | PARTIAL | MEDIUM |
| Vehicles | Aircraft attitude HUD | MISSING | HIGH |
| Vehicles | Seat mount desynchronisation recovery | MISSING | HIGH |
| Vehicles | Submarine-launched ballistic missiles | MISSING | MEDIUM |
| Vehicles | Hardpoints and ordnance weight | MISSING | HIGH |
| Vehicles | Door-state player visibility | PARTIAL | MEDIUM |
| Vehicles | Ground vehicle terrain handling | PARTIAL | MEDIUM |
| Mechas | Mecha morale | MISSING | HIGH |
| Teams | Domination, Conquest and Assault game types | MISSING | HIGH |
| Teams | Capture points | MISSING | HIGH |
| Teams | Deployment and spawn selection | MISSING | HIGH |
| Teams | Rank-up feedback and class gating | PARTIAL | HIGH |
| Teams | Map rotation administration | PARTIAL | HIGH |
| Teams | Per-entity lifetime and spawn-rate limits | MISSING | HIGH |
| Teams | Match rule modes | MISSING | MEDIUM |
| Teams | Team victory presentation | MISSING | HIGH |
| AI | Armed NPC bots | MISSING | HIGH |
| AI | AI target aircraft | MISSING | HIGH |
| Networking | Content pack hash verification | MISSING | HIGH |
| Client | Ping display | MISSING | HIGH |
| Client | First-person body rendering | MISSING | HIGH |
| Client | Client projectile render governor | PARTIAL | MEDIUM |
| Client | Iron sight calibration command | MISSING | MEDIUM |
| Client | Effect particles | PARTIAL | HIGH |
| Integration | Baris tech-tree export | MISSING | HIGH |
| Integration | Minimap capture-point overlay | MISSING | HIGH |
| Vehicles | Vehicle crew damage model | UNCERTAIN | LOW |
| Client | Dynamic light sources beyond projectiles | UNCERTAIN | LOW |
| Weapons | Reload staging depth | UNCERTAIN | LOW |

## Areas requiring deeper audit

- **Vehicle damage resolution.** `T/common/driveables/armor/` is a substantially different and more principled implementation than the reference's flat coefficients (armour plates, facings, impact angle, health scaling). Confirming whether crew damage, spalling and part-loss consequences are represented needs that path read end to end, not the key-level comparison used here.
- **Reload and animation timing.** `T/common/types/GunAnimationConfig.java` is far richer than the reference's animation set, so absent reference keys are usually generalised rather than missing. Only `TacticalReloadTime` and `ForceStagedReload` looked genuinely unrepresented, and that was not settled.
- **Aircraft flight model.** `T/common/driveables/physics/AircraftPerformancePhysics.java` derives lift, drag and stall from mass, wing area and thrust, replacing roughly 25 reference tuning keys (`pitchBoost`, `rollStall`, `yawBonus`, `stallSuffering`, `accelBonus`, `diveBonus`, `turnTime`, …). Those keys are deliberately not reported as gaps. Whether the derived model reproduces the reference's *feel* per airframe is a tuning question this static pass cannot answer.
- **Sound coverage.** The reference ships 168 `.ogg` files and a `sounds.json`; a per-event audit of which cues have no target trigger would likely surface more gaps than the config-key diff found.
- **Content-pack compatibility.** Roughly 860 reference configuration keys have no literal match anywhere in the target. Most are fork-specific, HFR-dependent, or superseded by target mechanisms, but a pack authored for this reference would silently lose behaviour on load. If loading Krishna Mk6C packs is a goal, a key-by-key triage — not just the feature-level triage done here — is warranted.
- **Bundled HFR mod.** `com/hfr/` (519 files) was excluded by scope. If any of its functionality is wanted — Clowder factions, RVI, or the territory layer it shares with the Flan's teams code — it needs its own analysis.
