# Feature Gap Analysis — Mr-Monorisu-Brazila (Labjac/TaP fork) → Flan's Mod Ultimate 2.0

- **Reference:** `C:\Users\alpha\Documents\Minecraft-Development\Mr-Monorisu-Brazila-master`. This is the newest Labjac Flan's fork: Minecraft 1.7.10, "Flan's LabCat Mod — TACZ hitreg experiment", including the Dill-branch features listed in `CHANGELOG_DILL.md`. It bundles the `com.hfr` (HBM/Clowder) mod and the `flanshorizons` source set.
- **Target:** `C:\Users\alpha\Documents\Minecraft-Development\Flans-Mod-Ultimate-2.0`, `master` @ `55b5aed88`.
- **Date:** 2026-10-09; completed-port/status refreshes 2026-10-11 and 2026-10-12. The comparison runs one way only, from the reference to the target. Functionality that only the target has, refactors, and modernisations are not reported.

Evidence roots:

- `R/` = reference `../src/main/java/com/flansmod`
- `T/` = target `../src/main/java/com/flansmodultimate`

**Method:** I extracted every `split[0].equals(...)` key from the reference parsers (1,498 keys). I flagged the keys that appear nowhere in target string literals, case-insensitively (709 keys) and grouped them into features. I traced each feature through reference call sites, then searched the target for direct, renamed and newer-mechanism equivalents. A missing key alone was never treated as proof: each finding below also had its behaviour checked. Systems that are not driven by config (packets, client handlers, commands, game types) were compared separately.

---

Targeted refresh (2026-11-04): authored passenger barrel counts/spacing and offset particle lists now use the shared native/NPC muzzle path, and `PassengerZoom` enables optics unless explicitly disabled. Removed the obsolete uncertain seat-weapon finding. Bayonet mode, basic PN guidance and aircraft sirens are now present; their remaining broader feature gaps are retained below. `cannonRecoil` also supplies legacy camera-kick defaults. This is not a full re-audit of other findings.

## Optics, thermal and fire control

### Ballistic fire-control solution — PARTIAL

Reference: `R/common/driveables/DriveableType` (`Rangefinder`, `FCSMode Off|AdjustingSight|AdjustingBarrel`, `FCSMaxRange`, `FCSDropScale`, `FCSHorizontalDrag`/`FCSVerticalDrag`, `FCSBarrelMaxCorrection`, `FCSWeapon`, the `FCS*Pos/Scale/Color/Label/Marker[Sight]` family), `R/client/TickHandlerClient`, `R/client/gui/GuiDriveableController`
The rangefinder lases the nearest block or hitbox and ignores the operator's own vehicle. The FCS then works out a firing solution from the loaded shell's speed, gravity, drag and spread and the muzzle-to-target offset. `AdjustingSight` moves an impact marker to the solution. `AdjustingBarrel` moves the physical barrel within `FCSBarrelMaxCorrection`. Main and coaxial guns get separate solutions, and missiles and manually guided rounds are excluded. The HUD has labels, digital fonts and live editing.

Target checked: `T/client/render/VehicleOpticsHud` (`measureRange`, `requestRange`), `T/common/driveables/OpticsHud` (Range/Speed/Traverse/Compass/Elevation elements, `requireRangeKey`, `maxRange`), `T/client/gui/OpticsHudEditorScreen`, `T/client/distant/DistantRangefinder`
**Present:** manual or automatic range display (it can also measure Distant Horizons terrain), configurable optics-HUD elements, per-sight colour and scale, and a live HUD editor.
**Present (2026-11-22):** `FCSMode AdjustingBarrel` with `FCSMaxRange`, `FCSBarrelMaxCorrection` and `FCSDropScale` (40 Tyrants and Plebeians vehicles). The client lases along the driver's first-person view every five ticks (`FireControlClient`) and sends the range; the server raises each gun and shell shot of the driver's banks to the solution for that shot's own speed, gravity and drag (`FireControlSolution`, `Vehicle.applyFireControl`), so main gun and coax are solved separately. The lased range shows beside the hotbar.
The drawn main gun rises by the main bank's correction, synced from the server every five ticks and laid at 1° per tick; the shot itself is corrected when it leaves the muzzle, so a gun still rising fires on the solution anyway. Corrections stop at the driver seat's pitch limits.
**Missing:** see the fire-control entry under Known gaps of the 2026-11-22 ports.

### Tank turret and sight stabilisation — PARTIAL

Reference: `R/common/driveables/EntitySeat` (`turretStabilization`, `playerLookingStabilized`, `toggleTurretStabilizer`, `stabilizedRenderView`), `R/common/driveables/VehicleType` (`TurretStabilization`), `R/client/KeyInputHandler` (toggle key H), `FCSStabilizerPos/Scale[Sight]`, `config_txt/fcs_stabilizer_configs.txt`
A stabilised seat keeps the gunner's intended aim fixed in world space while the hull turns and pitches. The physical turret catches up at its traverse speed and the sight stays on target. The feature can be toggled and shows a green or red `STA` indicator. Turret and barrel rendering is interpolated for display only, and an F10 diagnostic is available.

Target checked: `T/common/entity/Seat` (`applyClientAimDelta`, `getHullAimYaw`, `usesAbsoluteViewYaw`)
Vehicle seat aim is stored relative to the hull or turret, so it turns with the vehicle. World-yaw aim is used only for a mecha driver's torso (`usesAbsoluteViewYaw`). There is no stabiliser key, toggle or indicator.

**Present (2026-11-22):** `TurretStabilization` (44 Tyrants and Plebeians vehicles) holds the vehicle driver's view in world yaw, reusing the mecha driver's world-yaw view, with the turret following at its traverse speed and within its traverse limits. A client-side toggle key (O by default; the fork uses H, which the target binds to the optics range reset) and a green/red STA label beside the hotbar. The server needs no change: it already turns the turret toward the aim the client requests.
Missing: hull-pitch compensation, stabilised passenger turret seats, the fork's display-only turret interpolation and FCS stabiliser sight settings.

### Helicopter seat stabiliser and hover hold — PARTIAL

Reference: `R/common/driveables/DriveableType` (`AddHeliStabilizerToSeat`), `R/common/driveables/EntitySeat.isHeliStabilizerEnabled`, `R/common/driveables/EntityPlane.heliHoverMode`, `R/common/driveables/FlightController` (hover-hold block near line 1397), `R/client/TickHandlerClient` ("HOVER ON")
In a helicopter, stabilised gunner and pilot optics keep their world aim within the gun's yaw and pitch limits. The pilot can toggle hover hold with the mode key: the helicopter holds the altitude it had when hold was engaged (vertical-speed command clamped to ±0.22) and damps horizontal drift. The HUD shows the state.

Target checked: `T/common/driveables/physics/HelicopterPhysics` (half-collective hover convention), `T/common/entity/Plane`, `T/common/entity/Seat`
Helicopters hover at mid collective, but nothing holds a target altitude and there is no toggle. Seat stabilisation is absent, as above.

**Present (2026-11-22):** the hover hold on the mode key (J) for `Mode HELI` helicopters without folding wings (41 Tyrants and Plebeians helicopters, one of which folds wings and keeps that on the key), with the fork's ±0.22 climb cap, gain and drift damping (`HoverHold`), a synced and saved hold height and a HUD line.
**Present (2026-11-22):** `AddHeliStabilizerToSeat` on gunner seats (13 TaP helicopters): O toggles a sight stabiliser, released by default as in the fork, that holds the sight's world heading within the seat's traverse limits, reusing the turret stabiliser's world-yaw aim and STA label.
Missing: the pilot's stabilised optics (the other 13 TaP seats), and pitch: the fork holds a world point, the target only the heading.

### Picture-in-picture scopes — MISSING

Reference: `R/client/PictureInPictureEffect`, `R/client/PipOptics`, `R/client/ScopeRenderCompatibility`, `R/client/PipCalibrationCommand`, `GunType`/`AttachmentType` (`PictureInPicture`, `PictureInPictureReticle`, `PictureInPictureReticleScale`)
Scopes draw a magnified second camera view into the physical lens of the scope model, including thermal lenses, instead of zooming the whole screen. The renderer isolates the secondary camera from shader packs (OptiFine, Angelica), and a client command calibrates the lens live and exports config snippets.

Target checked: whole target (`pip`, `lens`, `PictureInPicture` searches); scope zoom in `T/common/types/IScope`/`GunType`
Scopes only use full-screen FOV zoom with overlays.

Missing: rendering the lens from a secondary camera, the PiP reticle and scale keys, and the calibration command.

### Missile and shell camera views — PARTIAL

Reference: `R/client/EntityCamera` (TV-guided and "magic artillery" views, `bullet.type.TVguided`, `bullet.type.starShell`), `R/common/guns/BulletType` (`TVguided`, `TPVdrone`, `manualSensitivity`), `R/common/driveables/DriveableType` (`hasMagicArtilleryMode`, `artilleryCalculator`, `walterCalculator`)
Firing a TV-guided missile or a TPV drone round moves the player's camera onto the projectile, which they steer from its own viewpoint. Artillery vehicles can switch to an overhead aiming view with range calculators.

Target checked: `T/network/server/PacketManualGuidance`, `T/common/entity/Bullet` (manual and laser guidance by the shooter's look ray), `T/client/ModClient` (`setCameraEntity` only restores the player)
Manual guidance steers along the shooter's line of sight, and the camera stays with the shooter.

**Present (2026-11-22):** the TV-guided view (`TVguided`, 7 Tyrants and Plebeians rounds): a player firing one from a driveable seat watches from the round, looking along its flight, until it is gone or they leave the seat. Steering keeps the operator's line of sight, as the target's manual guidance does.
**Present (2026-11-22):** the magic artillery view (`hasMagicArtilleryMode`, 35 TaP vehicles): F6 (the fork's U is the teams menu here) switches on a camera that follows, in third person, the newest shell, missile or bomb fired by anyone aboard within 200 blocks across and 500 up or down, until it is gone. Client side, like the fork's camera.
Missing: TPV drone views, the steering sensitivity setting, and the range calculators (`artilleryCalculator`, `walterCalculator`).

---

## Weapons and recoil

### Battlefield and sustained recoil model — MISSING

Reference: `R/common/guns/GunType` (`recoilElevator`, `sustainedelevator`, `firstShotRecoil`, `sustainedRecoilPitch`, `sustainedRecoilYaw`, `dillElevator`, `dillZoomModifier`)
This recoil model gives the first shot its own kick and makes recoil grow over a burst along a separate sustained curve, with an elevator term and a zoom modifier.

Target checked: `T/common/types/GunType` (`Recoil`, `RecoilYaw`, `RandomRecoil*`, `DecreaseRecoil*`, stance multipliers, `FancyRecoil`)
Both legacy recoil and FancyRecoil exist. The first-shot and sustained parameters do not.

Missing: sustained per-burst recoil growth (`sustainedRecoilPitch` / `sustainedRecoilYaw`).
**Dormant in the reference (checked 2026-11-22):** `firstShotRecoil`, `recoilElevator`, `sustainedelevator`, `dillElevator` and `dillZoomModifier` are parsed into fields that nothing reads, so packs authoring them (Tyrants and Plebeians: 100–220 guns each) lose nothing in the target.

### Directional melee and bayonets — PARTIAL

Reference: `R/common/guns/GunType` (`meleeLeft/Right/Up`, `Melee{Left,Right,Up}{Time,DamagePoint,DamageOffset}`, `AddLeftNode`/`AddRightNode`/`AddUpNode`, `spear`, `lance`, `canBlock`, `shootMelee`, `swordArmorPen`), `R/common/guns/AttachmentType` (`Bayonet`, `BayonetMeleeTime`), `R/common/network/PacketBlockerino`, `PacketBolterino`
A melee swing goes left, right or up depending on input. Each direction has its own path, timing and damage point. Some weapons can block, and a bayonet attachment turns a gun into a stabbing weapon.

Target checked: `T/common/types/GunType` (`MeleeDamage`, `AddNode`, `readDirectionalMeleeSwing`, `CanBlock`), `T/network/client/PacketGunMeleeClient`
**Rechecked 2026-11-22:** directional `MeleeLeft` / `MeleeRight` / `MeleeUp` swings with their own nodes, time and damage points, `ShootMelee`, `CanBlock` blocking and `SwordArmorPen` were ported in `0c1dee2de` (2026-11-10).

Present: grip `Bayonet` mode uses the existing secondary-mode toggle, `AddNode` stabbing animation and attachment `BayonetMeleeTime`. The grip's short-lived `SecondaryAmmo` supplies damage through the shared projectile path with virtual ammunition; cartridges and magazines are not consumed.
Missing: spear and lance behaviour (`spear`, `lance`; TaP sets `spear true` on one gun).

### Ricochet, over-penetration and penetration feedback — PARTIAL

Reference: `R/common/guns/BulletType` (`ricochetSound`, `distantRicochetSound`, `overPenSound`, `penetrateSound`, `minorPenSound`, `overPenPenalty`, `lessOverpen`, `penDecay`), `R/common/driveables/DriveablePart` (lines 615 and 690)
Shells hitting armour play distinct sounds for a ricochet, a minor penetration, a full penetration and an over-penetration. An over-penetrating round does reduced damage (`overPenPenalty`), and penetration falls off with flight time (`penDecay`).

Target checked: `T/common/guns/ShootingHelper` (bounce/ricochet branch at line 349), `T/common/types/BulletType` (`PenetrationAt100m`, `PenetrationDecay`, `HEAT`)
**Present (verified 2026-11-04):** projectiles can bounce or ricochet, penetration decays with distance, and `ricochetSound` / `minorPenSound` / `penetrateSound` / `overPenSound` select feedback from the current armor result for real driveables and NPC model bodies. `bounceSoundRange` controls ricochet reach.
**Missing:** distant ricochet audio, the damage penalty for over-penetration, and time-based penetration decay. Audio does not change penetration or damage; blocked hits above 90% of the requirement use minor feedback and successful hits above 3x use over-penetration feedback.

### Motion-sensor grenades — UNCERTAIN

Reference: `R/common/guns/GrenadeType` (`MotionSensor`, `MotionSensorRange`, `MotionSound`, `MotionSoundRange`, `MotionTime`), `R/common/guns/EntityGrenade.motionTime`
The parser accepts these fields, but the current reference only decrements the entity's motion timer. No consumer of the sensor flag, range or sound was found in reference Java code.

Target checked: `T/common/types/GrenadeType`, `T/common/entity/Grenade`
No matching sensor mechanic exists, but parsed reference fields alone do not establish a feature to port.

Missing: unconfirmed; do not treat this as an active gameplay gap without evidence of reference detection and triggering.

### Seekers: self-guided, radar-guided, anti-radiation and homing torpedoes — PARTIAL

Reference: `R/common/guns/EntityBullet` (around lines 418–500 and 656–690), `BulletType` (`selfGuided`, `seekerRange`, `radarGuided`, `antiRadiation`, `modernTorpedo`, `ASWminRange`, `scoutBullet`, `infiniteAngle`, `LockOnFuse`)
Fire-and-forget missiles find their own target after launch. Anti-radiation missiles home on radar emitters. Radar-guided missiles depend on the launcher. Modern torpedoes acquire a target only once they are in the water, can change depth, and use a seeker range scaled by the target's noise.

Target checked: `T/common/types/BulletType` (`LockOnTo*`, `MaxLockOnAngle`, `EnableSACLOS`, `ManualGuidance`, `LaserGuidance`, `Torpedo`)
Missiles lock on at the launcher, or are steered by SACLOS, manual or laser guidance. Legacy torpedoes run straight.

**Present (2026-11-22):** `modernTorpedo` acoustic homing with `ASWminRange` / `seekerRange` (13 Tyrants and Plebeians rounds) and `antiRadiation` (2), see the eighth batch under Pack-driven ports. `selfGuided`, `seekerRange` outside torpedoes and `radarGuided` are not authored by TaP.
Missing: delayed post-launch acquisition for other missiles (`selfGuided`, `lockonDelay`), radar-guided seekers, `infiniteAngle`, `scoutBullet` and `LockOnFuse`.

### Proportional-navigation guidance — PARTIAL

Reference: `R/common/guns/ProportionalNavigation`, `R/common/network/PacketProNavState`, `R/client/ProNavDebugTrailRenderer`, `BulletType` (`ProportionalNavigation`, `ProNavGain`, `ProNavMaxTurnRate`, `ProNavTerminalRange`, `ProNavTerminalGain`, `ProNavDebugTrail*`), `config_txt/proportional_navigation.txt`
This is opt-in interception guidance for MISSILE rounds. PN commands the turn rate (gain clamped to 0–10, maximum turn per tick clamped to 0–90) and blends to a terminal gain near the target. The server is authoritative, and players tracking the missile receive a motion stream. There is an optional debug flight-path trail.

Target checked: `T/common/entity/Bullet` homing (pursuit using `TurningForce`/`LockOnForce`)
Present: server-authoritative `ProportionalNavigation` / `ProNavGain` steering, preserving speed and the existing seeker, delay and `LockOnForce` turn budget. PN projectiles accept vanilla tracked velocity corrections; the existing entity tracking carries motion to clients.
**Present (2026-11-22):** `ProNavMaxTurnRate`, `ProNavTerminalRange` and `ProNavTerminalGain` (21 Tyrants and Plebeians missiles). An unset turn rate keeps the `LockOnForce` budget instead of the fork's 6° default.
Missing: the optional debug trail.

### Fly-over top attack — PARTIAL

Reference: `BulletType` (`FlyOverTopAttack`, `…AcquireRange`, `…Clearance`, `…Corridor`, `…DetonationWindow`, `…TurnRate`), `R/common/network/PacketTopAttackState`
The missile flies over the target at a set clearance and fires downward when the target passes through its corridor, as TOW-2B and NLAW do.

Target checked: `T/common/types/BulletType.isDoTopAttack` (`IsDoTopAttack`), `T/common/entity/Bullet` (climb-then-dive profile)
**Present:** Javelin-style dive top attack.
**Missing:** the fly-over-and-shoot-down mode and its parameters.

### Wire-guided missiles and the visible wire — PARTIAL

Reference: `R/common/guns/EntityGuidanceWire`, `BulletType.HasLine`
Wire-guided missiles draw a wire from the launcher to the missile, and guidance depends on it.

Target checked: `T/common/entity/Driveable.hasLineOfSight` (an unrelated LOS check)
**Present (2026-11-22):** `HasLine` draws a client-side wire along the recorded flight path that sags to loaded terrain and fades after the missile is gone (`GuidanceWireRenderer`, 17 Tyrants and Plebeians missiles). Guidance never depended on the wire in the reference either.
Missing: the separately tracked wire entity (a wire appears only while the client tracks the missile, anchored at its first seen position rather than the launcher), the launch curl and the rear beacon.

### Illumination (star) shells — DORMANT

Reference: `BulletType.starShell`, `R/client/FlansModClient` (around line 1118), `R/client/EntityCamera`
Star shells light up the area below them while they descend.

Target checked: `T/client/ModClient` dynamic lights (`HasDynamicLight` projectiles at a flat level)
**Dormant in the reference (checked 2026-11-22):** `starShell` only raises a client counter that sets `starStruck`, which nothing reads, and admits the round to the artillery shell camera. There is no lighting effect to port.

### Soft-target and vanilla-damage modifiers — PARTIAL

Reference: `BulletType` (`dynamicBodyarmorPen`, `vanillaDamage`), `R/common/guns/raytracing/PlayerHitbox` (line 172), `EntityVehicle`/`EntityPlane`/`EntityMecha.attackEntityFrom`
After `dynamicBulletDelay` a round also switches to a separate body-armour penetration. `vanillaDamage false` (the fork default) makes crewed driveables ignore direct player melee.

Target checked: `T/common/guns/LongRangeDamage`, `T/common/entity/Driveable.hurt`
**Note:** `DynamicBulletDelay` / `DynamicDamage` are present (see Projectile ports, 2026-11-21).
Missing: long-range body-armour penetration, which has no target counterpart because the target has no Labjac body-armour model, and the per-driveable melee gate; target driveables always take player melee damage, as in 1.7.10.
### glTF skeletal gun animation — MISSING

Reference: `GunType` (`UseGLTFAnimation`, `GLTFAnimation`, `Use{Left,Right}ArmGLTFAnimation`, `TacticalReloadGLTFAnimation`, `Start/End/LoopReloadAnimation`, `ShootAnimation`, `AimingShootAnimation`, `SwitchAnimation`, `Secondary*Animation`, `ActionAnimation`)
Guns can be animated from glTF clips: reload start, loop and end, tactical reload, shooting, switching and arm animations.

Target checked: `T/common/types/GunAnimationConfig` (legacy model animation, staged-reload rendering)
Missing: glTF animation loading and playback.

### Tactical reload timing and secondary-mode switching — PARTIAL

Reference: `GunType` (`TacticalReloadTime`, `StagedReloadTime`, `ForceStagedReload`, `SecondarySwitchTime`, `SecondaryUnSwitchTime`, `RunPosTime`, `RunCrouchTime`), `AttachmentType` (`RefundAmmoOnSwitch`, `UnSwitchAnimationOnEmpty`, `RenderOnlyOnMode`, `AimingTimeMultiplier`).

**Present (2026-10-12):** tactical reload selection from the actual loaded ammunition before scheduling; per-gun explicit timing and configurable default magazine multiplier; independent server-authoritative secondary enter/exit delays, a cosmetic switching movement, and grip/ammo visibility gated by secondary mode while retained during transitions. The normal attachment, ammunition and enchantment reload multipliers still apply; secondary attachment reload timing stays independent.

**Missing:** glTF switch clips, ammo refund, empty-secondary unswitch-animation policy, the fork's forced/staged reload behavior and run/crouch timing keys. Existing legacy staged-reload rendering remains available but is not the fork's full staged-reload mechanic.

### Gun carry limits and gun tiers — PARTIAL

Reference: `GunType` (`Heavy`, `sidearm`, `OldGun`, `RepeatingGun`, `labigunLimit`, `hasLabigunDelay`, `Tier`), `ArmourType` pouches (`hasGunPouch`, `hasHeavyPouch`, `hasOldGunPouch`, …)
**Corrected 2026-11-22:** the current reference has no carry limits. `sidearm` decides which mouse button and timer fire the gun, `labigunLimit` with `hasLabigunDelay` caps a firing timer, and `OldGun` only matches `hasOldGunPouch` for reload pouches. `Tier` is shown in every item's tooltip and sets a base `expValue` for the fork's experience bonus when a lower-tier weapon kills a higher-tier vehicle.

Target checked: `T/common/item/GunItem`, `T/common/types/GunType`, `ArmorType`
**Present (2026-11-22):** `Tier` on any definition (2047 Tyrants and Plebeians items) as a tooltip line; reload-duration pouches were implemented earlier.
Missing: the tier-gap experience bonus (the target has no experience economy), the fork's `sidearm` firing input and the `labigunLimit` firing timer.

### Ammunition evolution — MISSING

Reference: `BulletType` (`Evolution`, `PreEvolution`)
Ammunition items can be upgraded into a linked type.

Target checked: `T/common/types/BulletType`
Missing: the evolution chain.

### Muzzle and chamber smoke models — PARTIAL (smoke models present)

Reference: `GunType` (`MuzzleSmokeModel`/`Texture`, `ChamberSmokeModel`/`Texture`, `muzzleParticle`, `muzzleParticleCount`, `muzzleOffset`)
Guns can draw 3D smoke models at the muzzle and chamber, and spawn named particles at the muzzle.

Target checked: `T/network/client/PacketGunMuzzleFlash`, `T/client/render/item/GunItemRenderer`, the flash-model support in `GunType`
**Present:** muzzle flash models and particles.
**Corrected 2026-11-22:** the muzzle and chamber smoke model and texture keys were ported with the keyframe gun animations in `4146dc0fa` (`GunType`, `FirstPersonGunEffects`), including the fork's swapped key meaning that TaP's 50 guns rely on.
**Missing:** not re-checked beyond the smoke models.

---

## Armour and the player damage model

### Per-zone armour and armour penetration — PARTIAL

Reference: `R/common/teams/ArmourType` (`headArmor`, `faceArmor`, `neckArmor`, `napeArmor`, `chestArmor`, `backArmor`, `armArmor`, `legArmor`, `bodyArmor`, …), `ShootableType` (`headPen`, `bodyPen`, `armPen`, `bodyArmorPen`, `dynamicBodyArmorPen`), `R/common/guns/raytracing/PlayerHitbox`
Each armour piece protects specific hitbox zones, and each round has its own penetration per zone.

Target checked: `T/common/types/ArmorType`, `T/common/guns/ShootingHelper`
**Present (2026-11-22):** zone ratings (`headArmor`/`faceArmor`/`neckArmor`/`bodyArmor`/`backArmor`/`armArmor`/`legArmor` and aliases) on any worn piece, round `bodyArmorPen` (and `armPen`/`headPen`/`bodyPen`, which the reference folds into one value), long-range `dynamicBodyArmorPen`, `nonPenPenalty` / `barelyPenPenalty` (`ZoneArmor`, `PlayerHitbox`). The target's hitboxes have no separate face, nape or back boxes: hits on the head below eye level count as face or nape by direction, and body hits from behind as back. A missing `dynamicBodyArmorPen` keeps the round's own value instead of the fork's fixed 250.
Missing: the reference's separate zone damage multipliers (nape 3.5×, cranium 2×, face 1.5×) beyond the target's configured body-part modifiers, and grenade zone penetration.

### Reserve armour plates and energy shields — MISSING

Reference: `ArmourType` (`Reserve{Head,Face,Body,Arm,Leg,Back,Nape}Armor`, `forceField`, `rechargeDelay`, `rechargeTimer`, `RechargeSound`, `ShieldKillSound`, `WarningSound`, `damageLimit`, `BackupDefence`), `BulletType.SwordEnergy`
Armour carries reserve plates that absorb damage first. Shields recharge after a delay, warn when low and play a sound when broken.

Target checked: `T/common/types/ArmorType`, `T/common/types/MechaItemType` (mecha force field only)
Missing: player armour plates and player energy shields.

### Bleeding and field medicine — PARTIAL

Reference: `R/common/tools/ToolType` (`bandAid`, `superBandAid`, `surgery`, `transfusion`, `needle`, `HealStrength`), `ShootableType.bleedMultiplier`, `R/common/network/PacketVaccine`, `CommandTeams bleeding`
Wounds make players bleed, and medical tools stop the bleeding or heal.

Target checked: `T/common/types/ToolType`, `T/common/types/GrenadeType.HealAmount`
**Present (verified 2026-11-04):** native healing tools honor an authored `HealStrength` as regeneration amplifier and `HealAmount` as duration in ticks, preserving instant healing when the strength field is absent.
Missing: the bleeding state and the bleeding-specific medical tool classes.

### Armour weapon mounts, pouches and attributes — PARTIAL

Reference: `ArmourType` (`hasTopMount`, `hasFrontMount`, `topMount*`, `frontMount*`, `pouchMultiplier`, `reloadMultiplier`, `gasmaskable`/`smokeProtectable`)
Armour can show stowed guns, speed up reloads and protect against gas or smoke.

Target checked: `T/common/types/ArmorType`
**Present (verified 2026-11-04):** `reloadMultiplier` / `pouchMultiplier`, general and weapon-specific pouch flags, applied to player and NPC held-gun reloads. The fastest matching worn pouch wins without stacking, instead of relying on armor-slot order.
Missing: weapon mounts and gas or smoke protection.

---

## Vehicles, aircraft and mechas

### Destroyed-vehicle wrecks — PARTIAL

Reference: `R/common/driveables/EntityDriveableWreck`, `DriveableType` (`LeaveWreck`, `WreckModel`, `WreckTexture`, `WreckSmoke`, `WreckLifetime`, `WreckRendersFancyTracks`)
A destroyed vehicle leaves a smoking wreck entity, with its own model and texture, that lasts for a set time.

Target checked: `T/common/entity/Driveable` (`becomeWreck`, wreck physics, burn-out, blast break-up)
**Present (rechecked 2026-11-22):** destroyed driveables stay as burning, collidable wrecks for the server-configured lifetime; `NoWreck` and now `LeaveWreck false` opt out.
**Present (2026-11-22):** per-type `WreckLifetime` within the server's wreck lifetime and `WreckSmoke false`.
Missing: custom `WreckModel` / `WreckTexture` (the target renders the damaged original model); TaP authors `None` for both.

### Ammo-rack and weak-spot cook-off — PARTIAL

Reference: `R/common/driveables/EntityVehicle` (lines 245–275 and 1727–1830; `getAmmoRackCookoffDamage`, `killDriverForAmmoRack`, `spawnFancyAmmoRackCookoff`), `VehicleType` (`WeakspotCookTime`, `fancyAmmoRackCookoff`), `PlaneType.OilCookTime`
When a `turretWeak`/`weakSpot` hitbox is destroyed while the turret or core still has health, the driver is killed once. The turret or core then burns down over `WeakspotCookTime`, with small explosions and a flare column. With `fancyAmmoRackCookoff`, a full flame, smoke and debris column plays.

Target checked: `T/common/driveables/EnumDriveablePart` (`TURRET_WEAK`, `TURRET_WEAK_2`, `WEAK_SPOT`…`WEAK_SPOT_3`)
**Present:** the weak-spot part names parse as damageable parts.
**Present (2026-11-22):** the cook-off sequence (`AmmoRackCookOff`, `Vehicle`): a destroyed hull or turret weak spot kills the driver once, then burns the core or turret down every five ticks over twice `WeakspotCookTime` (121 Tyrants and Plebeians vehicles, 77 of them with weak-spot parts). The core's or turret's own destruction then runs the target's normal death and cook-off effects. Effects are vanilla flame, lava, smoke and explosion particles instead of the fork's flak flares, and there is no vanilla explosion.
**Missing:** `fancyAmmoRackCookoff`, the fork's turret-death hull cook for `needsTurret` tanks, and troop survivors escaping a burning transport.

### Radar, stealth and target designation — PARTIAL

Reference: `DriveableType` (`hasRadar`, `hasPlaneRadar`, `digitalRadar`, `radarRange`, `radarRefreshDelay`, `radarPositionOffset`, `RadarDetectableAltitude`, `radarVisible`, `OnRadar`, `Stealth`, `radarDetectionRangeMultiplier`), `BulletType.missileRadarVisible`
Vehicles with radar see contacts on a radar HUD. Stealth, altitude and terrain reduce how far a vehicle can be detected.

Target checked: `T/common/driveables/EnumDriveablePart` (radar hitbox parts only), `T/client/distant/DistantContactsClient`
**Checked 2026-11-22:** in the current reference the radar has no contact display or detection model. `radarRange`, `radarRefreshDelay`, `radarPositionOffset`, `digitalRadar`, `radarVisible` and `radarDetectionRangeMultiplier` are copied onto the entity and never read (196 Tyrants and Plebeians driveables author them). The live part is the active/passive radar toggle (`hasRadar`, `hasPlaneRadar`, the F8 key) and its HUD line, which `antiRadiation` missiles (2 TaP rounds) use to lock onto vehicles radiating with an active radar.
**Present (2026-11-22):** the active/passive toggle for `hasRadar` and `hasPlaneRadar` driveables with a HUD line, and anti-radiation lock-on; see the eighth batch under Pack-driven ports.
Missing: stealth (`Stealth`, `RadarDetectableAltitude`, `OnRadar`), not re-checked.

### Active protection systems and CIWS — PARTIAL

Reference: `DriveableType` (`hasAPS`, `APSdelayMax`), `VehicleType` (`HasPassiveAPS`, `PassiveAPSCanForceAim`), `BulletType` (`BypassPassiveAPS`, `PassiveAPSECCM`, `CIWSable`, `CIWSer`, `ciwsBullet`, `APSsound`), `R/common/network/PacketPassiveAPS`, `R/client/particle/EntityAPSGrenade`
Hard-kill APS destroys incoming missiles and then needs time to reload. Soft-kill APS can throw off a missile's aim. CIWS rounds shoot down munitions marked `CIWSable`.

Target checked: whole target
**Checked 2026-11-22:** the hard-kill `hasAPS` only starts the `APSdelayMax` timer, plays `APSsound` and shows a HUD readout; it never stops the round (7 Tyrants and Plebeians vehicles). `CIWSable` lets a round collide with other projectiles, which take no damage from it, and `ciwsBullet` / `CIWSer` are parsed but never read (141 and 48 TaP rounds). Neither has a gameplay effect to port.
**Present (2026-11-22):** the passive soft-kill APS (`HasPassiveAPS`, `PassiveAPSCanForceAim`, `BypassPassiveAPS` / `PassiveAPSECCM`; 2 TaP vehicles, 71 rounds), see the ninth batch under Pack-driven ports.
Missing: the forced-aim turret slew toward the launcher and the smoke it discharges.

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

### Submarine diving and crew oxygen — PARTIAL

Reference: `VehicleType` (`canDive`, `DiveSpeed`, `SurfaceSpeed`, `seaLevel`), `DriveableType` (`oxygen`, `maxOxygen`, `unlimitedOxygen`), `R/common/driveables/EntityVehicle` (around line 2170), `R/client/TickHandlerClient` (oxygen HUD)
Submarines control their depth and use up oxygen while submerged, which the HUD shows.

Target checked: `T/common/entity/Driveable` (`MaxDepth`, `WorksUnderWater`), `T/common/driveables/physics/EpicShipPhysics`
**Present (2026-11-22):** `CanDive`, `DiveSpeed`, `SurfaceSpeed`, `MaxOxygen`, `UnlimitedOxygen` and `MaxDepth` as crush depth (32 Tyrants and Plebeians submarines), with the fork's depth-rate model, oxygen budget, forced surfacing, 10-per-tick crush damage and a depth and oxygen readout (`SubmarineDiving`, `Vehicle`). Dive and Surface are new vehicle keys (V and G) and a new `SURFACE` input bit; the brake damps the rate.
Missing: see the submarine entry under Known gaps of the 2026-11-22 ports.

### Submarine-launched ballistic missiles and VLS timing — MISSING

Reference: `DriveableType` (`slbmDelay`, `slbmFlightType`, `slbmRange`, `slbmStrength`, `slbmWarheadType`), `BulletType.VLSTime`
Ballistic missiles launched from submarines follow a configured flight profile.

Target checked: `T/common/types/BulletType` (`VLS`, `HasDeadZone`)
**Checked 2026-11-22:** the `slbm*` keys (17 Tyrants and Plebeians submarines) are copied onto the entity and never read; the fork has no SLBM launch profile to port.
Missing: `VLSTime`, not re-checked.

### Hardpoints, ordnance weight and carrier operations — MISSING

Reference: `DriveableType` (`hardpoint`, `weightLimit`, `parkingSpot`, `helipad`), `PlaneType` (`carrierLandable`, `helipadLandable`, `carrierWingFlip`, `needsGear`, `parasitePlane`), `BulletType.missileWeight`, `R/common/network/PacketMissileWeight`, `R/common/guns/ItemBullet` (weight tooltip)
The weight of loaded ordnance affects aircraft performance. Aircraft can dock on carriers or helipads within a weight limit, and parasite planes are supported.

Target checked: `T/common/entity/Plane`, `T/common/types/PlaneType`
Missing: ordnance weight, carrier and helipad docking, and parasite aircraft.

### Altitude-dependent aircraft performance and Labjac flight tuning — PARTIAL

Reference: `PlaneType` (`flightCeiling`, `highAltMax`, `highAltMaxDry`, `maxSpeedDry`, `cruiseSpeed`, `climbRate`, `maxG`, `turnTime`, `pitch/roll/yaw{Bonus,Boost,Stall}`, `stallSuffering`, `planeDiveFactor`, `heliSpeedLimit`), `VehicleType.maxAltitude`
Aircraft have a service ceiling, an altitude-dependent top speed, G and turn limits, and stall penalties on each axis.

Target checked: `T/common/driveables/physics/AircraftPerformancePhysics` (constant `AIR_DENSITY`, soft stall)
**Present (2026-11-22):** the `flightCeiling` service ceiling and its HUD readout (280 Tyrants and Plebeians aircraft); see the eighth batch under Pack-driven ports.
Missing: altitude-dependent top speed and the other tuning keys.

### Helicopter and aircraft crash presentation — PARTIAL

Reference: `PlaneType` (`HeliCrashEffects`, `HeliCrashSmokeHealth`, `HeliCrashSpin`, `HeliCrashSpinStrength`, `HeliCrashSound`, `PlaneCrashSound`, `CrashSoundRange`), `R/common/driveables/FlightController` (`isHeliCrashing`, descent clamp −0.16), the tail-rotor hiding in `RenderPlane`
A crashing helicopter trails smoke and fire from the engine and airframe, spins, descends more slowly, hides its tail rotor and plays a crash sound that stops on impact.

Target checked: `T/common/entity/Plane` (`SpinWithoutTail`, `PlaneCrashDamage`), `T/common/driveables/DriveableCrashExplosion`
**Present:** spin without a tail, and the crash explosion and fire.
**Missing:** the crash smoke and fire threshold, spin strength, slower crash descent, tail-rotor hiding, and crash sounds.

### Distant and situational vehicle audio — PARTIAL

Reference: `DriveableType` (`farSound`, `farSoundRange`, `DistantSoundPrimary`/`Secondary`, `DistantFlareSound*`, `TracksSound*`, `StukaSound*`, `sonicBoomSound`, `loudCannon`, `LegacyEngineSounds`), `VehicleType` (`DriftSound*`), `ShootableType` (`DistantDetonateSound*`), `R/client/CustomSoundLoop`
Vehicles play distant-fire and distant-detonation sounds, track and drift loops, Stuka dive sirens and sonic booms, with custom sound ranges.

Target checked: `T/common/types/GunType` (distant gun sounds for infantry and deployed guns)
**Present (verified 2026-11-04):** infantry and deployed-gun distant sounds, plus driveable-bank `DistantSoundPrimary` / `DistantSoundSecondary`, `DistantSoundRange`, and `farSound` / `farSoundRange` aliases.
**Present (additional verification 2026-11-04):** shootable `DistantDetonateSound` / `DistantDetonateSoundRange`, sent on the server to listeners beyond near explosion audio reach.
Present: aircraft speed-triggered `StukaSound`, `StukaSpeed` (mph), `StukaSoundLength` and `StukaSoundRange`.
Missing: track, drift and sonic-boom audio.

### Door-state passenger visibility — PARTIAL

Reference: `DriveableType` (`SetPlayerInvisibleOnDoorClose`/`Open`, `SetPassengerInvisible`, `invisiblePassenger`, `SetDriverInvincible`)
Whether occupants are visible depends on whether the doors are open, and the driver can be made invincible.

Target checked: `T/common/types/DriveableType` (`SetPlayerInvisible`)
**Present (verified 2026-11-04):** unconditional hiding and `SetPlayerInvisibleOnDoorOpen` / `SetPlayerInvisibleOnDoorClose`, shared with NPC vehicle crews through the API and their model door state.
Per-seat `invisiblePassenger` / `SetPassengerInvisible` definition support is also present and inherited by NPC vehicle crews; explicit NPC hidden-seat choices remain additional hiding rules.
**Missing:** the driver-invincible flag.

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

### Mecha morale — PARTIAL

Reference: `MechaType` (`morale`, `panicSound`, `panicTime`, `runAmokSound`, `unpunchable`)
Mecha pilots can panic or run amok.

Target checked: `T/common/types/MechaType`
**Present (2026-11-22):** `canPanic`, `morale`, `panicTime`, `panicSound` and `runAmokSound` (4 Tyrants and Plebeians mounts: horse, camels, elephant) with the fork's once-a-second model: core damage costs morale, which recovers one a second; at none the mount panics, bolts forward along its legs with random swerves whatever its rider does, its timer running down two a second and growing by a quarter of further damage (`MechaMorale`, `Mecha`). Morale or PANIC! shows beside the hotbar.
Missing: `unpunchable`; the fork also stops a panicking mount's rider from turning it with the view, which the target gets by steering along the legs instead.

### Vehicle repair costs and new driving model — UNCERTAIN

Reference: `VehicleType` (`NewRepairSystem`, `canRepair`, `NewDrivingModel`/`nuDrivingModel`, `raceCar`, `driftMultiplier`, `terrainPenalty`), `ShootableType`/`GunType.repairCost`
Repairs have a cost, and an alternative driving model includes drift.

Target checked: `T/common/entity/Driveable` (repair), `T/common/driveables/physics/*` (target vehicle physics)
The target has its own physics stack, so the two may be partly equivalent. The repair-cost economy is absent.

Missing (unconfirmed): repair costs, plus drift and terrain penalties as configurable keys.

---

## Teams and game modes

Conquest, Domination and Assault, deployment layouts and spawn selection, class limits, prestige and the victory presentation were ported on 2026-10-13; see [Teams ports (2026-10-13)](#teams-ports-2026-10-13).

### Map and rotation administration and spawn-rate limits — PARTIAL

Reference: `R/common/teams/CommandTeams` (`planeRate`/`vehicleRate`/`seatRate`, `rviForce`/`rviRate`, `raiding`)
The reference lets admins throttle how often planes, vehicles and seats send their position packets, and how often and how close the long-range vehicle indicator packets are sent.

Target checked: `T/common/command/TeamsCommand`, `T/FlansModEntities` (driveables registered with `updateInterval(1)`), `T/common/distant/DistantSync` (`distantContactUpdateInterval`)
**Present (2026-10-13):** map bounds, renaming and deployment images, the legacy map and rotation spellings, and the pacifism, movie-prop, sea-level and shake commands.
**Missing:** `planeRate`, `vehicleRate` and `seatRate`. The target syncs driveables through the vanilla entity tracker, whose interval is fixed when the entity type is registered, so there is no per-type packet rate to expose. `rviForce`/`rviRate` have no counterpart either; the nearest target mechanism, distant contacts, already has its own `distantContactUpdateInterval` config. `raiding` is parsed and saved by the reference but nothing reads it, so there is no behaviour to port.

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

### Iron-sight alignment tool — PARTIAL

Reference: `R/client/IronSightCalibrationCommand`, `GunType` (`IronSightOffset`, `IronSightPitch`, `ScopeAlignment`, `Xoffset`/`Zoffset`)
A client command adjusts first-person iron-sight alignment live, and per-gun keys store the offsets.

Target checked: `T/client/render/item/GunItemRenderer`, `T/client/debug/AimOffsetDebugCommand`
**Corrected 2026-11-22:** `IronSightOffset` and `IronSightPitch` (33 Tyrants and Plebeians guns) are read and applied in the first-person frame, and `/flansdebug aimoffset` previews aim offsets live, with a Tyrants and Plebeians first-person frame (`/flansdebug viewFrame tap`).
Missing: per-optic `ScopeAlignment` (1 TaP gun) and the fork's calibration command that exports the offsets as config lines.

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

Target checked: whole target; capture points exist since 2026-10-13 (`T/common/block/entity/CapturePointBlockEntity`), with an on-screen compass cue but no minimap integration
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
| Optics | Ballistic fire-control solution | PARTIAL | HIGH |
| Optics | Turret and sight stabilisation | PARTIAL | HIGH |
| Optics | Helicopter seat stabiliser and hover hold | PARTIAL | HIGH |
| Optics | Picture-in-picture scopes | MISSING | HIGH |
| Optics | Missile and shell camera views | PARTIAL | HIGH |
| Weapons | Sustained recoil | MISSING | HIGH |
| Weapons | Directional melee and bayonets | PARTIAL | HIGH |
| Weapons | Ricochet and penetration feedback | PARTIAL | MEDIUM |
| Weapons | Motion-sensor grenades | UNCERTAIN | LOW |
| Weapons | Self-guided, radar and ARM seekers, homing torpedoes | PARTIAL | HIGH |
| Weapons | Proportional navigation | PARTIAL | HIGH |
| Weapons | Fly-over top attack | PARTIAL | HIGH |
| Weapons | Wire-guided missile and wire | PARTIAL | HIGH |
| Weapons | Illumination star shells | DORMANT | HIGH |
| Weapons | Long-range armour penetration and vanilla-damage gate | PARTIAL | MEDIUM |
| Weapons | glTF gun animation | MISSING | HIGH |
| Weapons | Tactical reload and secondary switch timing | PARTIAL | HIGH |
| Weapons | Gun carry limits and tiers | PARTIAL | HIGH |
| Weapons | Ammunition evolution | MISSING | MEDIUM |
| Weapons | Muzzle and chamber smoke models | PARTIAL | MEDIUM |
| Armour | Per-zone armour and penetration | PARTIAL | HIGH |
| Armour | Reserve plates and energy shields | MISSING | HIGH |
| Armour | Bleeding and field medicine | PARTIAL | HIGH |
| Armour | Weapon mounts, pouches and attributes | PARTIAL | HIGH |
| Vehicles | Destroyed-vehicle wrecks | PARTIAL | HIGH |
| Vehicles | Ammo-rack and weak-spot cook-off | PARTIAL | HIGH |
| Vehicles | Radar, stealth and designation | PARTIAL | HIGH |
| Vehicles | APS and CIWS | PARTIAL | HIGH |
| Vehicles | Remote drones and quadcopters | MISSING | HIGH |
| Vehicles | Troop transport | MISSING | HIGH |
| Vehicles | Submarine diving and oxygen | PARTIAL | HIGH |
| Vehicles | SLBM and VLS timing | MISSING | MEDIUM |
| Vehicles | Hardpoints, ordnance weight and carriers | MISSING | HIGH |
| Vehicles | Altitude performance and flight tuning | PARTIAL | MEDIUM |
| Vehicles | Helicopter and aircraft crash presentation | PARTIAL | HIGH |
| Vehicles | Distant and situational vehicle audio | PARTIAL | HIGH |
| Vehicles | Door-state passenger visibility | PARTIAL | HIGH |
| Vehicles | Vehicle smoke and turret grenades | MISSING | HIGH |
| Mechas | Mecha morale | PARTIAL | HIGH |
| Vehicles | Multi-barrel seat guns | UNCERTAIN | LOW |
| Vehicles | Crew damage and part-loss effects | UNCERTAIN | LOW |
| Vehicles | Repair costs and alternative driving model | UNCERTAIN | LOW |
| Teams | Map administration and spawn-rate limits | PARTIAL | HIGH |
| AI | Armed bot NPCs | PARTIAL | MEDIUM |
| AI | AI target aircraft | PARTIAL | MEDIUM |
| Client | First-person body rendering | MISSING | HIGH |
| Client | Iron-sight alignment tool | PARTIAL | HIGH |
| Client | Performance governor and shot prediction | PARTIAL | MEDIUM |
| Client | Tab-list ping display | PARTIAL | MEDIUM |
| Client | Offhand gun selection | UNCERTAIN | LOW |
| Integration | Baris tech tree and IR/laser | MISSING | HIGH |
| Integration | JourneyMap capture-point overlay | MISSING | HIGH |
| Integration | Flan's Horizons distant terrain | UNCERTAIN | LOW |
| Bundled | HFR/Clowder faction and industry mod | MISSING | HIGH |

**Current tracked gaps:** 14 MISSING, 33 PARTIAL, 6 UNCERTAIN. Remaining rows retain their original audit classifications unless noted; this update is not a fresh audit of every subsystem.

## Completed ports (verified 2026-10-11)

These findings were removed from the active gap sections and summary table after checking their current target implementations.

- **Gun and attachment thermal scopes:** `HasThermalVision` / `HasThermal`, `IScope`, `VehicleThermalRenderer`.
- **Thermal palettes, generations and hot coverage:** `ThermalVisionColor`, `ThermalVisionGeneration`, hot rounds, grenades, effects and configured particles (`ThermalHotParticles`), plus team IFF (`ThermalTeamStrobe`).
- **Infantry and explosion screen shake:** existing `GunScreenShake` and the new enabled-by-default shared shootable `powerShake` / `rangeShake` keys, `ShootingHelper.doExplosion`, `PacketCombatFeedback`, `CombatScreenEffects`. Explosion recipients use the authored radius and uniform strength, matching the reference (defaults: strength 10, radius 32 blocks; explicit zero disables); clients control strength and duration independently.
- **Tracer beams and mixed belts:** `BulletType.TracerBeam`, alternate round/model/texture/trail settings, shared fired-shot sequencing and synchronized projectile state.
- **Hold-to-throw grenades:** hold/release overhand and underhand throws, server throw validation, throw multipliers, rolling/drag/stop controls and impact aliases.
- **Near-miss suppression and ammunition fly-by sounds:** `NearMissHandler` checks travelled flying and hitscan segments, stops at terminal impacts, excludes the shooter, direct hits and obstructed players, and sends at most one sound and suppression impulse per shot/player. `FlyBySound` / `missNoise` use registered pack sounds; absent values keep the generic sound and explicit `None` or blank disables it. Suppression is enabled globally by default; pack strength/range, server multipliers/rider policy and client roll/vignette/recovery controls are supported. Triggers accept players, NPCs and other source entities, including ownerless mod projectiles. Render effects leave aim and accuracy unchanged.

### Validation of the 2026-10-11 combat-feedback port

- 14 focused tests passed: travelled-segment geometry, impact endpoint bounds, intensity/range controls, effect recovery and ceilings, legacy parser defaults/aliases, packet/config round trips, and server-side class loading with Minecraft client/rendering classes blocked.
- Scoped `spotlessApply` / `spotlessCheck` and main/wiki diff whitespace checks passed. `sonarlintMain` and `sonarlintTest` completed; new findings were fixed, while existing findings outside the changed methods remain.
- Full `build` failed in the independent Wolff flag pack because shared model classes could not be resolved. A sequential retry failed in the concurrently edited `SkullBossEntity` boss-bar initialization (`BossEvent` versus `ServerBossEvent`). An earlier normal test compilation also encountered an unrelated `ExternalProjectileSupportTest` API mismatch. These unrelated changes were preserved.
- IntelliJ inspections ran once and reached the ten-minute limit before all directories in the mixed checkout were inspected. Available results were reviewed and introduced warnings corrected; full IDE coverage is incomplete.
- In-game audio, visual presentation and multiplayer runtime behavior have not been exercised. The renderer adapts the legacy effects to cosmetic modern camera offsets and configurable real-time envelopes; numerical pixel-for-pixel parity with the legacy FOV/shake renderer is not claimed.

## Ammunition ports (2026-10-12)

- **Depth charges:** checked `BulletType`, `Bullet`, and all shared firing paths before implementation; only torpedo motion existed. Legacy `depthCharge` and `activationDepth` now select sinking motion and server detonation strictly below the authored depth in water. `DepthChargeSeaLevel` optionally overrides the dimension sea level. Depth charges always use projectile entities, including when a weapon would otherwise use hitscan. `FallSpeed`, air/water drag, prime delay, fuse, lifetime and smoke behavior remain available. No shooter-type restriction is imposed.
- **HESH:** checked `Driveable.bulletHit`, `ModelArmourDamage`, and `BulletType`; HEAT already existed, HESH did not. `Hesh` now stops the round at the first vehicle surface, including an NPC or other entity using the driveable armour adapter. Unlike HEAT it retains the existing kinetic armour and direct-damage channel. This adapts the reference auxiliary-armour stopping rule to the target's existing first-surface warhead policy. It does not add a new squash-head spalling or damage model.
- These are ammunition-specific traits, active whenever authored; ordinary ammunition is unaffected. No bundled ammunition definitions were converted automatically. The reference repository contained no matching text definitions to exercise, so parser fixtures use its actual directive spellings.

### Validation of the 2026-10-12 ammunition port

- 30 focused tests passed: eight new depth-charge/parser tests plus existing HEAT, vehicle-armour and damage-channel regression tests.
- Full `build` succeeded, including the main mod, API, NPC, built-in and independent pack artifacts. The full suite reported 1,535 tests: 1,525 passed, 10 skipped, no failures or errors. This includes the existing dedicated-server class-loading tests.
- Scoped `spotlessApply` / `spotlessCheck`, `sonarlintMain`, `sonarlintTest`, IntelliJ inspection and main/wiki `git diff --check` completed. No new analysis findings were introduced. Existing SonarLint complexity/parameter/loop findings (45 in the touched main classes) and 39 pre-existing IntelliJ warnings remain outside this change's new code; the existing `Driveable.bulletHit` complexity warning is retained rather than refactoring the full damage pipeline during this port.
- In-game water movement, actual depth detonation, NPC firing and visual/audio presentation have not been exercised. Tests cover deterministic motion, activation boundaries, content parsing and existing armour/damage calculations, not a running Minecraft world.

### Category follow-up (2026-10-12)

- Audited all category labels and their existing aliases for HESH/HEP and depth-charge identities. Updated four existing `bullet_categories.json` categories, covering eight existing short names: L31A7 receives `HESH true`; aerial, generic and Type 3 depth charges receive `depthCharge true` and `Fuse 0`. No new categories, membership, ammunition groups, masses, filler, velocity or penetration figures were authored.
- Identity evidence: official Modern Warfare English localization and `Shell120mmL31A7.txt`; English localization and bullet definitions inside the read-only installed TaP OHIO and Rainfire Battle Squadrons ZIPs. Names like `HEshell` are ordinary HE, not HESH, and were excluded. Available TaP charges explicitly author 20/40 m aerial and 25/50/75/100 m ship-launched settings; categories preserve those fields rather than inferring depths from short-name suffixes. Type 3 has no depth field and retains the 20-block gameplay fallback, not a claimed historical setting. Historical nomenclature for that pack's Type 3 remains ambiguous: the [US Navy submarine war-damage report, section III](https://www.ibiblio.org/hyperwar/USN/rep/WDR/WDR58/WDR58-3.html) distinguishes Type 3 pistols from Type 95/Type 2 charges. No new historical values were inferred from it.
- The installed charges used positive 100–300 tick legacy self-destruct fuses, which can pre-empt the port's drag-limited sink. Category `Fuse 0` allows depth activation. Added `DepthChargeLifetime` (2400 ticks by default; configurable 1–72000) because a 100 m setting needs more than the ordinary 600-tick bullet ceiling. Ordinary bullet lifetimes are unchanged. This is a gameplay safety ceiling, not a historical fuse delay.

- Category follow-up validation: strict JSON, preserved order/membership/statistics and cross-category property checks passed. Ten focused tests passed, including lifetime bounds and 100 m sinking beyond the old 600-tick ceiling. Full `build` passed with 1,527 tests passed and 10 skipped; scoped formatting, main/test SonarLint, IntelliJ and diff whitespace checks completed with no new issues. Existing analysis warnings remain as documented above. In-game depth-charge behavior remains untested.

## Weapon-handling ports (2026-10-12)

- Checked current `GunType`, `GunItem.getActualReloadTime`, reload scheduling, NPC equipment properties, secondary-mode packets, `AttachmentType` and `GunItemRenderer` before implementation. Reloads previously had one duration; secondary mode changed immediately; grips ignored `RenderOnlyOnMode`.
- `TacticalReloadTime` sets an explicit live-ammo duration. The default `-1` derives 80% of the ordinary duration through `TacticalReloadMultiplier`; `0` disables the bonus. Automatic eligibility is limited to a single ammo slot with live multi-round ammunition; internal and single-round loading retains ordinary timing unless an explicit positive tactical time is authored. The default reduction is a configurable gameplay assignment, not a historically sourced speed. The shared equipment property path exposes the timing to NPC/other holders; the ordinary secondary-grip reload duration remains independent.
- `SecondarySwitchTime` / `SecondaryUnSwitchTime` default to eight ticks and are independently configurable (zero disables). The server validates the held gun, life/spectator state, cooldown and pending reload, changes the mode and acknowledges the server duration. Per-hand transient switch state blocks firing and reload attempts and cancels the old burst queue; changing the ordinary shoot timer cannot shorten the switch. The client plays a cosmetic dip, independently disabled with `SecondarySwitchMovement false`. Protocol 23 rejects peers using the older acknowledgement shape. This is the player secondary-mode control path; arbitrary mod code that directly edits gun NBT does not initiate a timed switch.
- `RenderOnlyOnMode` applies to grip attachments and their ammunition meshes. Hidden primary-mode grips stay visible throughout the switching interval. Existing physical launchers remain visible unless their packs explicitly request mode gating; no attachment category was authored to hide a real mounted launcher.
- Category integration: eleven reviewed belt-fed categories covering 35 existing aliases receive `TacticalReloadMultiplier 1.0`, preserving their authored reload duration instead of granting the new magazine bonus. Source definitions and pack-local English localization were checked for the aliases. Existing category ordering, membership, ballistics and cadence remain unchanged. These are gameplay compatibility exceptions, not new historical reload-time estimates. The touched MG 42 entry at 1200 RPM was rechecked against the later [US War Department report, Tactical and Technical Trends no. 31](https://www.lonesentry.com/articles/ttt/mg42.html), which gives 1100–1350 RPM; its existing cadence was retained. No new categories or coverage were added.

### Weapon-handling validation

- Full `build` passed with 1,571 tests: 1,561 passed, 10 skipped, no failures/errors. The 15 focused timing, parser, packet, presentation, cooldown-lifecycle and expanded server-safety checks passed within that suite. Pack artifacts were rebuilt without changing their source definitions.
- Fractional reload completion now clears its flag when the countdown is ready, through the existing `ShotCooldown` rule. Firing retains its negative fractional remainder, including several shots per tick, and a zero-duration mode change preserves that remainder. Dedicated cooldown tests cover these interactions alongside switch expiry and a timer reset during switching.
- Scoped `spotlessApply` / `spotlessCheck`, strict category/property checks, main/test SonarLint and main/wiki whitespace checks passed. Existing main-source complexity/unused warnings outside the new behavior remain. IntelliJ ran once; five constant-input assertions were given narrowly explained suppressions. The subsequent cooldown refactor was compiled, tested and analyzed with SonarLint but not re-inspected by IntelliJ.
- Actual Minecraft reloads, secondary controls, first/third-person presentation and multiplayer latency have not been exercised. The cosmetic switch movement is an adaptation to the current renderer, not a port of the fork's glTF clips. Remaining switch/refund/staged-animation gaps are retained above.

## Teams ports (2026-10-13)

- Checked `T/common/teams` (game types, manager, maps), `T/common/entity/Flagpole`, `TeamSpawnerBlockEntity`, `PacketTeamsState` and the Teams screens before implementing. Bases were flagpole entities only; the manager, the base editor and the operator stick were generalised to `ITeamBase` so a block entity can be a base.
- **Capture points:** `teams_capture_point` block and block entity (`ITeamCapturePoint`), with name, letter A–E, capture radius, default owner and map set in an operator-stick editor. The flag climbs the legacy flagpole model in the holder's colour, or flies the team's `VictoryFlagTexture`. Capture state is saved with the block and stamped with the round it belongs to, so a point that loads late or a server that restarts mid-round keeps its progress; the reference resets it.
- **Domination, Conquest, Assault** (`dom`, `con`, `aslt`): the reference capture arithmetic is in `CaptureRules`, covered by unit tests. Scoring, ticket bleed, kill tickets, Assault reinforcements, stage time, defender deploy delays and all game-type variables follow the reference defaults. Deviations: Domination undoes a rival's unfinished capture of a neutral point before starting its own, as Conquest does, instead of inheriting it; a map without capture points no longer hands Assault attackers an instant win; players standing on a captured point also get five score, so the scoreboard and MVP mean something; XP follows the target's scale (25 per capture). The reference removes every driveable in every world at round end; the target's other game types do not, and neither do these.
- **Objective HUD** (`TeamsObjectiveHud`) replaces the plain Teams banner in these modes: ticket or reinforcement bars, clock, one square per point, and the Conquest/Assault compass cue. The capture tick sounds of the reference HUD were not ported.
- **Deployment map** (`TeamsDeployScreen`): replaces the death screen in Conquest and Assault, with headquarters, held points, Assault front-line rules, the deploy delay, a class button and the operator edit mode (drag nodes, crop the image, save). Layout, image and crop are stored per map. The delay is a client-side countdown as in the reference; the server validates only where a player may spawn. No map image is bundled: maps without one are drawn as a plain chart.
- **Class limits and prestige:** `ClassLimit`, `ClassLimitPercent[age]`, `RequiredPrestigeLevel`; enforced on the server, shown in the class menu. `/teams prestige`, `/ranks`, `/teams setRank`. Rank-up popup (`PacketRankUp`), also sent without a loadout pool.
- **Victory presentation:** the winner is decided per game type when a round ends; `HasVictorySound`, `VictorySound`, `DefeatSound`, `VictoryFlagTexture`. The reference's Victory, Defeat, Draw and LevelUp sound files and its deployment image were not copied into the target; the names resolve against pack sounds, with vanilla sounds standing in for victory and rank-up.
- **Rules and administration:** map bounds with the 20-second out-of-bounds kill, `renameMap`, `setMapImage`, rotation aliases, `pacifism`, `movieProp`, `seaLevel` (applies to depth charges, the only target consumer of a sea level outside each dimension's own) and `shake`. Movie-prop mode does not retract plane landing gear.
- `DriveableType.DeploymentPoint`, listed under deployment in the original audit, is where a troop transport unloads its soldiers. It belongs to the troop-transport gap and is not part of Teams.

### Validation of the 2026-10-13 Teams port

- Full `build` passed with 1,640 tests: 1,630 passed, 10 skipped, no failures or errors. The 34 new tests cover the capture arithmetic of all three modes, the Assault front line, class-limit capacity, prestige, map persistence (bounds, deployment image, crop, layout, old saves), the objective snapshot round trip, and loading the new packets and rules with Minecraft client classes blocked.
- Scoped `spotlessApply` / `spotlessCheck`, `sonarlintMain`, `sonarlintTest` and `git diff --check` completed. New SonarLint findings were fixed; the four that remain in touched files are complexity warnings on methods that predate this port (`TeamsManager.tick`, `applyLoadout`, `loadRuntime`, `TeamsCommand.addRuleCommands`).
- IntelliJ inspections ran on `common/teams` and `client/gui` only; findings in new code were fixed. The remaining packages touched by this port (`network`, `common/block`, `client/render`, `client/teams`, `common/command`) were not inspected.
- Network protocol bumped to 24 because `PacketTeamsState` changed shape.
- Nothing was exercised in a running game. Capturing, the objective HUD, the deployment map and its edit mode, the capture point editor and renderer, respawning at a chosen point, map bounds and multiplayer behaviour are untested in play, as is a dedicated server.

## Areas requiring deeper audit

- **Motion sensors.** Reference parser fields appear dormant; an active implementation has not been established.

- **Crew and part-loss model.** The reference's `damageVsCrew`, `crewEngine` and `engineLoss`/`wingLoss`/`tailLoss` may partly overlap with the target's part-health scaling (`VehicleHealthScaler`) and its aircraft physics. I did not confirm this.
- **Vehicle driving model.** The target's physics stack (`T/common/driveables/physics`) is substantially different. The Labjac `NewDrivingModel`/drift/terrain-penalty behaviour needs a behavioural comparison rather than a key comparison.
- **Flan's Horizons versus Distant Horizons.** The two have equivalent aims through different mechanisms. Whether server-authoritative terrain distribution matters for this project is a product decision.
- **HFR bundle.** It is intentionally not audited.
- **Key-extraction blind spots.** Discovery relied on `split[0]` keys plus a sweep of client and network classes. Reference behaviour that is hard-coded, with no key, packet or class, could be missing from this list. The likeliest places are `R/common/driveables/EntityDriveable` (more than 5,000 lines), `EntityPlane` and `EntityBullet`.

## Targeted refresh (2026-11-04)

Vehicle weapon overheating is implemented: opt-in `OverheatLimit`, `OverheatPenalty`, `CoolingBonus`, `OverheatSound`, shared mounted-weapon heat, server-enforced cooling and lockouts, and synchronized driver HUD feedback. Main-mod commit `55168e36d`. Door-dependent occupant visibility is implemented in `5130026f8`; gun equip and distant bank sounds in `3ca23865e`. These are targeted corrections; other older findings have not all been re-audited.

Compatibility follow-up (2026-11-04): gun `IronSightOffset` (raw legacy render translation) and `IronSightPitch` (aim-blended rotation) now affect first-person rendering only. Pouch, seat, and armor-audio findings above reflect their implementation, not a full re-audit of the remaining reference features.

Additional rate/audio compatibility (2026-11-04): gun `RPM` aliases canonical/category `RoundsPerMin` at lower priority, `Descriptions` aliases `Description`, and native player bolt/pump closing supports optional `ActionEndSound` with server-validated per-shot/reload permissions. This is a timing adaptation to native animations, not glTF clip playback.

Impact/scatter follow-up (2026-11-04): ammunition `DynamicHitSoundEnable` can layer material feedback with the authored hit sound, with explicit false and unset fallback handled separately. Grenade `RandomDirection` chooses the fork-style scatter direction and speed on the server; spawn and saved orientation now include roll. Shared impact metadata carries both sound layers for native NPC equipment.

## Projectile ports (2026-11-21)

- **Long-range soft-target damage:** `DynamicBulletDelay` / `DynamicDamage` (`LongRangeDamage`, `ShootingHelper.getDamage`). After the delay, the long-range value replaces damage against living targets other than driveables and their stand-ins. Raytraced shots use shooter distance over muzzle velocity as flight time. Mass-based rounds are scaled by `DynamicDamage` over their authored damage against the target, since kinetic damage uses muzzle velocity and has no falloff of its own. The fork's delay of 9000 or more counts as unused. Ammunition tooltips show the long-range value or factor.
- **Missile twirl and ground skim:** `MissileTwirl*` / `ATGMTwirl*` and `*GroundSkimTolerance` (`MissileFlightProfile`, `BulletRenderer`, `Bullet.trySkimGround`), on by default for guided rounds as in the fork. Skim surfaces are block tags and blocks standing in for the 1.7.10 grass/ground/sand/clay/snow materials, plus gravel, farmland and paths.
- **Projectile chunk loading:** `ProjectileChunkLoading` (`ProjectileChunkLoader`). Instead of persistent Forge tickets, the round renews short-lived vanilla region tickets along the next ten ticks of its path; nothing is saved and nothing outlives it. Server settings cap rounds per player and per server and the distance from launch; rounds over a cap fly normally instead of being refused at launch as in the fork. There is no per-dimension cap and no check that chunks are already generated, so a round flying into unexplored terrain generates it.
- **Stale findings removed:** the afterburner (`Plane`, `PlaneType`; the fork's `afterburnOffBonus` is parsed but never used) and wing-flap and wheel-cover animation (`PlaneType`, `ModelPlane`) already existed.
- Validation: Analysis profile, with scoped Spotless, compilation, parser and behaviour tests and SonarLint; no new findings remain. Not exercised in a running game.

## Pack-driven ports (2026-11-22)

Selected from the Labjac keys that the Tyrants and Plebeians Redemption pack (`run/flan`) authors and the target ignored, after checking each against reference call sites; keys whose reference fields are never read were classified dormant instead.

- **Per-zone body armour:** see the armour section above. TaP authors zone ratings on about 440 armour files and penetration on about 300 rounds.
- **Driveable death explosions:** `isExplosionWhenDestroyedRadius` (290 TaP driveables) sets the death-explosion radius at the lowest priority of its aliases and, as in the reference, a positive value turns the explosion on.
- **Explosion resistance:** `explosionResistance` multiplies the blast damage a driveable's parts take (`FlanExplosion`).
- **Seat invincibility:** `invincible <seat> <bool>` and `SetDriverInvincible` cancel damage to that seat's rider except damage that bypasses invulnerability; destroying the driveable still kills them.
- **Missile guidance (second batch):** `ProNavMaxTurnRate` / `ProNavTerminalRange` / `ProNavTerminalGain` (21 TaP missiles) and the `HasLine` guidance wire (17); see the weapons section. `starShell` (3) is dormant in the reference.
- **Ammo-rack cook-off (third batch):** `WeakspotCookTime`; see the vehicles section. `alwaysShowTurret` (167 TaP vehicles) needs nothing: the fork hides the turret from a first-person driver unless it is set, and the target never hides it. Vehicle `hasSmoke` (274) is not read by the fork's driveable code.
- **Turret stabiliser (fourth batch):** `TurretStabilization`; see the optics section. Also checked and left alone: `bleedMultiplier` (198 rounds) needs the fork's bleeding state, which is off by default there, and TaP ships none of its bandage, surgery or transfusion tools; `HeliCrashEffects` / `HeliCrashSpin` (20 helicopters) would overlap the target's own crash-mode physics; `labjacFuel` (265 aircraft) duplicates the target's fuel system.
- **Fire control (fifth batch):** `FCSMode AdjustingBarrel`; see the optics section. Also checked: `CIWSable` (141 rounds) only lets a round collide with other projectiles, which take no damage, and `ciwsBullet` / `CIWSer` are never read; `hasAPS` (7 vehicles true) only starts a reload timer, a sound and a HUD readout without stopping the hit. Neither has a gameplay effect to port.
- **Submarines (sixth batch):** `CanDive` and its oxygen and depth keys; see the vehicles section. The radar keys were checked and found mostly dormant in the reference; see the radar section.
- **Tier and explosion push (seventh batch):** `Tier` as a tooltip line on every type, and `explosionPush` (8 TaP submarines) scaling how hard blasts shove a driveable against the fork's default. Also checked: the `slbm*` keys are dormant; directional melee and blocking were already ported; `sidearm` / `labigunLimit` are firing-input keys, not carry limits; the `seatOptics*` HUD keys parse by prefix and already work.
- **Seekers, radar and ceiling (eighth batch):** `modernTorpedo` (13 TaP rounds) runs as a torpedo that keeps its depth instead of settling, and on its first tick in water listens once: the usual lock along its heading applies first, as in the fork, then the nearest driveable it hears replaces it, heard within `ASWminRange` (5) plus `seekerRange` (250) times its throttle, half for astern, never the launcher's own mount (`PassiveSeekers`, `Bullet.listenForTarget`). Locked, it is steered by the seeker alone with no boresight limit. `hasRadar` (114 TaP vehicles) and `hasPlaneRadar` (78 aircraft) give the driver an active/passive toggle (F7, a new `TOGGLE_RADAR` input bit, a saved driveable flag) and a HUD line; radars start active as in the fork. `antiRadiation` rounds (2) lock on to the nearest emitting driveable their lock flags allow, preferring it to the heading lock, and keep it past their seeker cone. `flightCeiling` (280 aircraft) pitches a nose-up aircraft down a degree a tick within 10 blocks of it, and the plane HUD shows the ceiling beside the altitude (`FlightCeiling`). Also checked: `turretTossTrue` (139 vehicles) throws the turret as a damaging grenade with the vehicle's turret model; the target already throws destroyed turrets off as cosmetic pieces (`DestructionSequences.partsLost`), so only the grenade's blast and impact damage are missing. `damageVsCrew` and `SetupCrewedPart` crews (159 / 211) drive the fork's ship rate of fire and flooding repair, where the target deliberately counts real seat occupants (`DriveableRepairs`).
- **Death blasts, wrecks, hover hold, soft-kill APS and TV guidance (ninth batch):** `Death` (153 TaP driveables), `bigDeath` (144) and `nuclearDeath` (12) play the fork's death sounds (`explcls6`, `battleshipgun`, `yamatogun`, which TaP ships) out to 250 blocks, its screen shakes (88/69, 120/80, 250/120 strength and blocks) and a fiery visual-only explosion (`DeathBlast`); damage still comes from the death explosion alone, as in the fork. `WreckLifetime` (913, all 2000 ticks) shortens the server's wreck lifetime, and `WreckSmoke false` leaves a wreck that neither burns nor smokes. Helicopters get the hover hold (see the optics section). `HasPassiveAPS` vehicles jam SACLOS and manually guided missiles that do not bypass them: every second tick from the third, a missile within 80 blocks, beyond 10, flying within about 70° of the vehicle and passing within its hitbox reach plus 4 blocks, with a clear line and from the hull's front half (any side with `PassiveAPSCanForceAim`), drops its guidance and corkscrews away by a shared seed (`PassiveAps`, `PacketPassiveAps`). `TVguided` rounds show their flight to the operator (see the optics section).
- **Morale, shell camera and scout rounds (tenth batch):** mount morale (see the mechas section), the magic artillery shell camera (see the optics section), and `scoutBullet` (7 TaP rounds), which tells the player who fired it the round's X, Y and Z and its distance from them in chat when it detonates or dies.
- **Fighter HUD and gunner stabiliser (eleventh batch):** `hasHUD` with `hudColorR/G/B` (90 TaP aircraft seats) draws the fork's fighter head-up display in first person: heading tape with large cardinal points, a pitch ladder every ten degrees that turns with the bank, a bank scale, and speed (in the HUD's speed unit) and altitude (above sea level) tapes with boxed values and the throttle, laid out in the fork's units of 353 to the screen height (`FighterHud`). Also the gunner-seat sight stabiliser (see the optics section). Checked and left alone: `swapInitialWing` (12 planes) only sets an F-14 sweep animation state these piston aircraft do not use; `RecoilDistance` and driveable `maxLockOnAngle` are parsed by the fork's driveables but never read there.
- **Not ported:** vehicle `armorPen` / `penDecay` use the fork's unitless armour integers, which conflict with the target's millimetre armour model; `AccelerationSpeed` / `DecelerationSpeed` / `BrakeMultiplier` need the driving-model comparison listed under deeper audit.
- Validation: Focused profile, with scoped Spotless, compilation and parser/behaviour tests. Not exercised in a running game.

### Known gaps of the 2026-11-22 ports

What each port leaves out compared with the reference, and where it behaves differently. None of these ports has been exercised in a running game.

- **Per-zone body armour:** no separate zone damage multipliers (nape 3.5×, cranium 2×, face 1.5×) beyond the configured body-part modifiers; no grenade zone penetration. Face, nape and back are inferred from where and from which side the head or body is hit, since the target's hitboxes have no boxes of their own for them.
- **Destroyed-vehicle wrecks:** no custom `WreckModel` / `WreckTexture` (TaP sets both to `None`).
- **Proportional navigation:** no debug flight-path trail (`ProNavDebugTrail*`). An unset `ProNavMaxTurnRate` keeps the `LockOnForce` turn budget, where the fork defaults to 6° per tick.
- **Guidance wire (`HasLine`):** client-side only, so a wire exists only while that client tracks the missile and starts where the client first saw it rather than at the launcher. No launch curl, no rear beacon, and no separately tracked wire entity that outlives the missile's tracking range.
- **Ammo-rack cook-off:** no `fancyAmmoRackCookoff` column; no hull cook-off after the turret is lost on `needsTurret` tanks; no troops escaping a burning transport. Flames, smoke and blasts are vanilla particles and sound instead of the fork's flak flares, and there is no vanilla explosion, so nothing nearby is damaged. The driver-killed flag is not saved, so a vehicle reloaded with a destroyed rack can kill one more driver.
- **Turret stabiliser:** yaw only, without hull-pitch compensation; driver seat only, no stabilised passenger turrets; no display-only turret interpolation and no FCS stabiliser sight settings. The toggle is client-side and per seat entity, defaults to engaged and is bound to O (the fork's H resets the optics range here).
- **Adjusting-barrel fire control:** lases loaded terrain and entities only, not Distant Horizons terrain, and only in first person; the server trusts the client's range, capped at `FCSMaxRange`, because it cannot see far or unloaded terrain itself. Coax and other banks have their shots corrected but only the main bank raises the drawn barrel, and the shoot origin is not moved with it. No `AdjustingSight` impact marker (no TaP vehicle uses it; one authors the typo `AdjustingBarrek`, which the fork ignores too), no `FCSDebug` readouts, no `Rangefinder` key and no `FCS*Pos/Scale/Color/Label` HUD family. The drawn barrel's direction follows the aim-pitch convention (positive down) and has not been checked in game.
- **Submarine diving:** depth is measured from the hull's position to the water surface above it, not from the fork's fixed `TeamsManager.seaLevel`, and counts up to 160 blocks of water. The depth rate is not saved and is stepped separately on the server and the predicting client. There are no `seaLevel` server setting, airship lift (`airship`, `maxAltitude`) or SLBM launch timing, which TaP also authors. `MaxDepth` also keeps its target meaning, so a diving hull's engine floods below its crush depth. The crush damage bypasses armour and hits only the core.
- **Tier and explosion push:** the tier is display only, without the fork's tier-gap experience bonus; a missing `Tier` shows nothing rather than the fork's "Reserve Tier". `explosionPush` scales the target's own knockback, which pushes every driveable as hard as a player, so it is relative to the fork's default of 0.25 rather than its absolute value; unauthored driveables, epic ships included, keep the target's push instead of the fork's 0.25 and 0.
- **Seekers, radar and ceiling:** a modern torpedo hears only Flan's driveables, whose throttle is their noise, not vehicles of other mods. The fork's lock scan covers all loaded entities; the target's covers the server lock-on range, also for anti-radiation rounds. The torpedo listens on the client as well and takes the server's tracked motion, so a client that hears differently is corrected. An anti-radiation round keeps a lock made before its target goes passive, as in the fork, and aircraft radars emit too, where the fork's seeker reads only `hasRadar`; no TaP anti-radiation round locks on to planes. The flight ceiling counts from sea level like the HUD altitude rather than from Y 0, unauthored aircraft have none instead of the fork's 800, and the push never takes the nose past level. No radar contact display, stealth or SLBM keys.
- **Death blasts, wrecks, hover hold, soft-kill APS and TV guidance:** the fork's tank, ship and nuke flash particles are drawn with the target's explosion visuals. The wreck lifetime is capped by the server's `driveableWreckLifetime`, so a pack cannot make wrecks outlast what the server allows, and an unauthored or 0 lifetime keeps the server's where the fork's 0 means a permanent wreck. The hover hold needs a pilot, a running engine and intact blades, and is not on the mode key of helicopters whose wings fold. The soft-kill APS covers all sides with `PassiveAPSCanForceAim` instead of slewing the turret toward the launcher and waiting until it faces within 8°, and discharges no smoke; its 20-tick response cooldown is not modelled. The TV-guided camera keeps the player's first- or third-person setting; seat HUD overlays still draw over it.
- **Morale, shell camera and scout rounds:** morale and the panic timer are not saved and start full for a reloaded mount, as in the fork; the panicking mount needs its engine and hips like any walk, and nothing stops its rider dismounting. The shell camera's on/off state is per player, not per driveable, and is dropped on leaving the driveable; it forces third person while it follows a round and restores the previous view after. A scout round reports once, in one chat line instead of the fork's five.
- **Fighter HUD and gunner stabiliser:** the HUD reads the composed camera view rather than the fork's blend of airframe and head angles, hides while the seat's optics or third person are in use, labels every ten degrees of heading instead of every five, and draws no flight path marker. The sight stabiliser holds heading only and ignores the seat's own aim frame, so a gunner seat on a turning mount is not compensated for it.
- **Classified as needing nothing:** `firstShotRecoil` and the other dormant recoil keys, `starShell`, `alwaysShowTurret`, vehicle `hasSmoke`, the hard-kill `hasAPS`, `CIWSable` / `ciwsBullet` / `CIWSer`.
- **Checked and left alone:** `HeliCrashEffects` / `HeliCrashSpin` were re-checked on 2026-11-22: the fork's doomed-helicopter pitch-and-roll, extra sink and flame and smoke overlap the target's realistic aircraft damage, autorotation and wreck crash modes. `turretTossTrue` (cosmetic turret throw-off already present), `damageVsCrew` / `SetupCrewedPart` crews (the target counts seat occupants), `bleedMultiplier` (no TaP medical tools), `HeliCrashEffects` / `HeliCrashSpin` (overlap the target's crash physics), `labjacFuel` (duplicates the target's fuel), vehicle `armorPen` / `penDecay` (conflict with millimetre armour) and `AccelerationSpeed` / `DecelerationSpeed` / `BrakeMultiplier` (need the driving-model comparison).
- **Large TaP-used gaps not started:** troop transport and the content-pack bots it carries (50 vehicles, 33 bots), carrier operations and `weightLimit` (26), and the tier experience economy.
