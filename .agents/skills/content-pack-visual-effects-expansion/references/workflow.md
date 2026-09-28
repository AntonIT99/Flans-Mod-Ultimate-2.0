# Visual Effects Expansion Workflow

Paths are relative to the repository root. `<pack>` is the resolved folder
`src/<set>/resources/flans_content/<pack>`, and `<set>` is its source set
(`manuspacks`, `warfare44pack`, `wolffromepack`, ...).

## 1. Inventory

1. List the pack's definitions by folder (`guns`, `bullets`, `grenades`,
   `attachments`, `vehicles`, `planes`, `aaguns`, `mechas`) and record, per file:
   `ShortName`, `Model`, `DeployedModel`, `ModelScale`, and every effect key already
   present (§2). Record the file's encoding and line endings before touching it.
2. Guns: collect the pack's model packages from the `Model` values and, for every
   `Deployable True` gun, from its `DeployedModel`. Then run the muzzle report for
   each package, writing to the scratchpad:

   ```bash
   ./gradlew muzzleReport -PmodelSourceSet=<set> -PmodelPackage=com.flansmod.client.model.<Package> -PreportFile=<scratchpad>/<pack>-<Package>.tsv
   ```

   Pass the package exactly as declared, since casing matters. Use its gun table
   for hand-held models: the measured muzzle, the barrel face size, the barrel
   attach point and a suggested flash point. Use its deployable table for
   `ModelMG` models: the renderer uses that same automatic front-face measurement
   as the flash position. The report constructs each model the way the client does
   (`translateAll`, `flipAll`). Rotated parts are left out and flagged in `notes`.
   Don't use its driveable table for placement. It measures seat guns at
   `VehicleGunModelScale` 1 and ignores `ModelScale`.
3. Driveables and AA guns: run the shoot-point sync for the pack, report only:

   ```bash
   ./gradlew shootPointSync "-Pfilter=<set>: <pack>/" -PreportFile=<scratchpad>/<pack>-shootpoints.md
   ```

   The filter is a case-insensitive substring of `<set>: <pack>/definitions/<folder>/<file>`.
   The task measures every vehicle, plane, mecha and AA gun model the same way
   `/flandebug shootpoint apply` does, including `ModelScale` and
   `VehicleGunModelScale`. It compares the result with each definition's primary shoot point,
   seat `GunOrigin` and AA gun `Barrel` lines. It never fails the build, and it writes
   nothing without `-Pwrite`. The console line gives the counts. A pack is synced
   when it reports `0 to update, 0 to add`. The report lists, per value, the action
   (`unchanged`, `update`, `add`, `skipped`), the authored and measured points in
   type-file pixels, and a **Check first** table of values scored 15 or more.
   Those are the values where the measurement itself may be wrong, or where the
   tool could not place the point. §5.1, §5.2 and §5.5 turn this into placement
   decisions.
4. Note which flash asset the pack already uses (`FlashModel`/`FlashTexture` pairs,
   `MuzzleFlashModel`), which driveables already opt into `FlashModel DefaultFlash`,
   and the pack's particle conventions: shoot-particle sets, emitter sets, trail
   types.
5. Identify reference families (§3.4). Warfare 44 sources live in
   `src/warfare44pack`. Decompile Tyrants and Plebeians models through the
   `mod-class-decompilation` skill (slug `tap-hero-shooter-september`, mappings
   `1.7.10`) and read them from `decompiled/`.

## 2. Effect catalogue

Parsers are the source of truth. When a key or its semantics look different from
this table, trust `GunType`, `ShootableType`, `BulletType`, `GrenadeType`,
`DriveableType`, `VehicleType`, `AttachmentType` and `GunAnimationConfig`. Valid
particle names are the constants in `FlanParticles` as mapped by
`ParticleHelper`: legacy vanilla names like `largesmoke`, `explode`, `cloud` and
`crit`, and mod names like `flansmod.fmflame`, `flansmod.fmsmoke`,
`flansmod.fmtracer`, `flansmod.fmtracerred`, `flansmod.fmtracergreen` and
`flansmod.rocketexhaust`.

| Owner | Key | Syntax and meaning |
|---|---|---|
| Gun | `FlashModel` / `FlashTexture` | Three-frame flash model drawn for 2 ticks after a shot. Hand-held rendering places it at `muzzleFlashPoint` (+ `defaultBarrelFlashPoint` without a barrel attachment); deployable rendering places and aims it at the measured `DeployedModel` muzzle. Both scale it by `flashScale`. `FlashModel DefaultFlash` is the built-in, W44-derived flash and needs no texture line. |
| Gun | `animMuzzleFlashPoint x y z` | Overrides the hand-held model's `muzzleFlashPoint`, in blocks of model space, divided by the flash scale (§3.2). It does not move the automatically measured deployable flash. |
| Gun | `animFlashScale s` | Overrides the model's `flashScale` for both hand-held and deployable flashes. Also scales the hand-held flash point. |
| Gun | `animDefaultBarrelFlashPoint x y z` | Offset added when no barrel attachment is fitted. Rarely needed, see §3.2. |
| Gun | `MuzzleFlashModel` | Alternative single-frame model. Hand-held rendering places it at `muzzleFlashPoint`, else `barrelAttachPoint`; deployable rendering places and aims it at the measured `DeployedModel` muzzle. Do not add it when a `FlashModel` is present or added; the renderers treat them as alternatives. |
| Gun | `ShowMuzzleFlashParticle`, `MuzzleFlashParticle*` | World particle at the shooter's hand, positioned from player offsets, not the model. Off unless the server config enables it. Only ever write `ShowMuzzleFlashParticle False`. |
| Bullet, grenade | `TrailParticles` / `SmokeTrail` (bool), `TrailParticleType` | Particle trail while in flight. |
| Bullet, grenade | `ExplodeParticles` / `NumExplodeParticles`, `ExplodeParticleType` | Particles on detonation. |
| Bullet, grenade | `FlareParticleCount`, `DebrisParticleCount` | Explosion flare and debris counts. The engine multiplies them by charge intensity (`ExplosionVisuals`). |
| Bullet | `FlakParticles n`, `FlakParticleType` | Airburst cloud, for flak and proximity rounds only. |
| Bullet | `BoostParticle` | Rocket and missile boost-phase particle. |
| Driveable | `FlashModel DefaultFlash` | Type-wide muzzle-flash opt-in. Each successful `GUN` or `SHELL` bank shot flashes only its fired shoot point/barrel; passenger guns flash only their fired `GunOrigin` barrel. Several muzzles can flash together. A mounted gun's own `FlashModel` or `MuzzleFlashModel` replaces the default at that muzzle. Driveables accept only `DefaultFlash`, with no `FlashTexture` or `anim*` placement lines. |
| Driveable | `ShootParticlesPrimary` / `ShootParticlesSecondary name x y z` | Spawned once per fired shoot point at that point's shoot origin (root plus offset), the same point the projectile leaves from. The particles are therefore exactly as well placed as the shoot point (§5.2). `x y z` is a direction and speed in the model basis: +x forward, y up. |
| Driveable | `AddEmitter` / `AddParticle name rate [ox,oy,oz] [ex,ey,ez] [vx,vy,vz] minThrottle maxThrottle minHealth maxHealth part` | Continuous emitter. The origin, extents and velocity are in type-file pixels. It emits every `rate` ticks while throttle and part-health fraction are inside the bounds. It runs only while the engine is on (vehicles and planes) and within the local emitter range. Turret and barrel parts follow the turret. |
| Driveable | `EmittersRequireOccupant` | Emitters only while occupied. |
| AA gun | `Barrel index x y z` | Projectile origin in type-file pixels for that barrel. `shootPointSync` compares and can synchronize these lines with the measured `ModelAAGun` muzzles. This is placement data, not a visual-effect key. |
| AA gun | no muzzle-effect key | `AAGunType` currently parses neither `FlashModel` nor `ShootParticles*`, and `AAGunRenderer` draws neither. Audit its barrels and ammunition visuals, then report a muzzle-effect proposal under the engine limitation (§5.5). |
| Attachment | `DisableMuzzleFlash` / `DisableFlash` | Suppresses both flash models and the world particle while fitted. |

Engine limitations. Report these; don't work around them:
- deployed guns (`Deployable True`, drawn by `ModelMG`) can draw `FlashModel` or
  `MuzzleFlashModel` at their measured muzzle, but still have no world
  muzzle-flash particle path;
- driveable passenger guns can draw a flash at `GunOrigin`, but still get no
  `ShootParticles*` path;
- `FlashModel DefaultFlash` is one driveable-wide switch, not a per-bank or
  per-seat setting; it cannot safely represent mixed eligible and ineligible
  gun/shell muzzles;
- AA guns still have no flash-model or `ShootParticles*` parser/render path; adding
  either key to an AA-gun definition would be inert;
- only the primary and secondary banks of a driveable get `ShootParticles*`.

## 3. Hand-held guns

### 3.1 Eligibility

| Weapon | Front flash | Notes |
|---|---|---|
| Pistols, revolvers, SMGs, rifles, carbines, shotguns, LMG/GPMG, HMG, anti-materiel rifles, handheld autocannon | Yes | Default case. |
| Integrally suppressed (Welrod, De Lisle, Sten Mk II(S), VSS, AS Val, MP5SD) | No | Also write `ShowMuzzleFlashParticle False`. |
| Rocket launchers, recoilless rifles, Panzerfaust-type | No front flash | The flash is at the rear or it's a launch plume. Never place the front flash. Report the item as a backblast candidate. |
| Grenade launchers (M79, rifle-grenade cups), mortars | No, or very small | Low-pressure launch. Skip unless the pack's style gives them one. |
| Flamethrowers | No | Their stream is their effect. |
| Melee, bows, crossbows, throwables, air or gas guns, tools | No | Write `ShowMuzzleFlashParticle False` only for guns that could otherwise show one. |
| Energy or fictional weapons | Case by case | Only with a pack-consistent asset. Never apply the firearm flash to a laser or plasma weapon by default. Report it for the user to decide. |

A gun whose definition already has `FlashModel` or `MuzzleFlashModel` needs no new
flash. Check that its position agrees with the measurement and report
disagreements as suspicious; don't change it. The model field `hasFlash` does not
drive rendering, so it is not evidence either way.

Apply this table to every `GunType` regardless of where it is used. A pilot gun,
passenger gun or deployable gun does not stop being an eligible small arm merely
because the player is not holding it. Evaluate each supported rendering context
independently when one definition supplies more than one model.

### 3.2 Position

The flash frames are discs centred on their origin (a unit test pins this for
`ModelDefaultFlash`). The renderer applies `ModelScale`, then `flashScale`, then
translates by `muzzleFlashPoint`. Therefore:

```text
animMuzzleFlashPoint = muzzlePx / 16 / flashScale     (per axis)
```

`muzzlePx` is the report's `muzzlePx`. The report's `suggestedAnimMuzzleFlashPoint`
assumes the model's current flash scale. After choosing a new `animFlashScale` (§3.3),
recompute the point with the formula. Round to 5 decimals.

Accept the measurement only when all of these hold:
- the status is `measured-agrees`, or `measured-no-attach-point` with a
  `muzzleSourceGroup` that is plainly the barrel (`gunModel`, `slideModel`,
  `breakActionModel`, or `defaultBarrelModel`);
- `notes` report no rotated parts reaching further forward. If they do, read the
  model source: a folded bayonet or bipod can be ignored, but a rotated barrel
  cannot be measured and must be skipped;
- the muzzle is at the front of the model (x clearly positive and the largest
  extent) and on a plausible bore line (y within the barrel region seen in the
  source).

`measured-attach-point-disagrees` means the measurement and the author's barrel attach
point are more than 2 px apart. Read the model source to decide which is the
barrel. Many authors park `barrelAttachPoint` somewhere unrelated. If it's still
unclear, skip the gun and report it.

When `defaultBarrelModel` reaches past the body, put the flash at the default
barrel's tip through `animMuzzleFlashPoint`, as Warfare 44 does. Fitted barrel
attachments then decide via their own model offsets. Leave
`animDefaultBarrelFlashPoint` unused unless the gun already relies on it.

The model's own `muzzleFlashPoint`, when declared (for example
`declaredMuzzleFlashPoint` in the report), takes precedence over any measurement.
It only needs a flash asset.

### 3.3 Size

The rendered flash diameter in blocks is `9 × flashScale × ModelScale / 16`
(`ModelScale` comes from the gun definition, default 1). Choose `animFlashScale`
so that the diameter hits the class target. These targets are calibrated on
Warfare 44's hand-tuned guns:

| Class | Target diameter (blocks) |
|---|---|
| Pistol, revolver, machine pistol | 0.25 (0.20–0.35) |
| SMG, pistol-calibre carbine | 0.35 (0.30–0.50) |
| Intermediate-cartridge rifle and carbine (StG 44, SKS, M1 Carbine, AK) | 0.42 |
| Full-power rifle, battle rifle, shotgun | 0.56 |
| LMG / GPMG (bipod) | 0.45–0.56 |
| MMG / HMG on tripod (.30 cal, M2, Maxim) | 0.70 |
| Anti-materiel rifle, 20 mm handheld | 0.80 |

```text
animFlashScale = targetDiameter × 16 / (9 × ModelScale)
```

Shorten the flash toward the low end for long barrels and effective flash hiders,
and push it higher for short barrels such as carbines, snub revolvers and sawn-offs.
Round to 2 decimals. If the pack already uses the same flash asset, match its sizes
for the same class rather than the table.

### 3.4 Same-model families

Packs often reuse a more detailed or a simpler version of another pack's model,
such as Warfare 44 versus the official WW2 pack. A reference position may be reused
only when:
- both models are the same weapon, by name, definition and look;
- both muzzle measurements come from the report (or a decompiled source, which
  you then measure or read as a declared `muzzleFlashPoint`), and the barrel
  lengths agree within 10 %, or differ by a known uniform scale that you apply;
- you convert through world size, not raw numbers. Different `ModelScale`s and
  flash scales make raw `muzzleFlashPoint` values incomparable.

Record the family evidence in the report. A family match that needed scaling is
always listed as suspicious.

### 3.5 Placement in the file

Put `FlashModel` (and `FlashTexture` when reusing a pack asset) next to `Model`
and `Texture`. Put `anim*` lines together with any existing `anim*` lines, or else
right after the flash lines. Never reorder or reformat unrelated lines. Follow
`content-pack-definition-sync` for encoding, line endings and trailing newlines.

### 3.6 Deployable guns

For every realistic firearm with `Deployable True`, resolve `DeployedModel` and
use its row in the muzzle report's deployable table. Add `FlashModel DefaultFlash`
when the definition has neither `FlashModel` nor `MuzzleFlashModel`, but only when
the row is `measured` and source inspection confirms the reported point is the bore
at the correct end of the gun. An existing `MuzzleFlashModel` also renders on the
deployable. The automatic `ModelMG` measurement follows pitch and yaw in game, so
do not add or derive `animMuzzleFlashPoint` for the deployed view.

Apply the eligibility rules in §3.1 without exception: no firearm flash for
integrally suppressed deployables, launch tubes, recoilless weapons, air guns, or
energy weapons unless the pack supplies a suitable effect. A deployable definition
that also has a hand-held `Model` must satisfy both models' evidence requirements
before one shared `FlashModel` line is added.

Size the deployed flash with the §3.3 formula, using the definition's `ModelScale`.
For tripod MMG/HMGs use the 0.70-block target; scale upward only for a verified
larger-calibre weapon. Add or retain `animFlashScale` only after checking its
hand-held rendering too, because the same value controls both views. A
deployable-only definition can be sized from the deployed model alone.

The deployed renderer does not spawn `ShowMuzzleFlashParticle`; leave that setting
under the server default exactly as for hand-held guns. List ambiguous automatic
measurements under `no reliable position` and on the suspicious list rather than
guessing.

## 4. Attachments

- Suppressors and silencers lacking `DisableMuzzleFlash True`: add it. It is visual
  only.
- Flash hiders, compensators and muzzle brakes: no key change. Report ones whose
  attachment model visibly extends the muzzle as suspicious for flash position.

## 5. Driveables and AA guns

### 5.1 Muzzle flashes

Small-arms flash coverage is exhaustive. Resolve every driveable's primary and
secondary weapon types, every gun named by `ShootPointPrimary`,
`ShootPointSecondary`, `AddGun` or `PilotGun`, and every passenger gun. Cross-check
the referenced `GunType` definitions under §3 so the same eligible weapon also
gets a flash when hand-held or deployed.

Add `FlashModel DefaultFlash` beside the driveable's `Model` and `Texture` when all
of these hold:

- the driveable has at least one eligible small arm in a `GUN` bank, pilot mount,
  or passenger seat;
- every `GUN` or `SHELL` bank and passenger gun that the type-wide switch will
  render is eligible for a front muzzle flash; missile, bomb and mine banks are
  unaffected and do not block it;
- every affected shoot point and passenger `GunOrigin` has reliable position
  evidence as described below.

This is one switch for the complete driveable. If an ordinary machine gun shares
the driveable with an integrally suppressed gun, launch tube, or another muzzle
that must not show a front flash, skip the line and report `mixed flash eligibility`.
Do not invent a per-bank key or try to suppress one mount with
`ShowMuzzleFlashParticle False`; that key controls the optional world particle,
not the flash model.

Use the `shootPointSync` report and the §5.2 decision table for position evidence.
Primary and secondary banks flash at the exact fired `ShootPoint` and measured
multi-barrel offset. Passenger guns flash at their fired `GunOrigin` barrel. A
synced, low-risk measured row is acceptable. An unmeasured primary/secondary point
or passenger origin must pass the same authored-only model check as shoot
particles and goes on the suspicious list. Pending `update` or `add` rows must be
synced in apply mode before adding the flash line. A Check first, skipped or failed
row without independently verified authored placement is `no reliable position`.

A referenced mounted gun's own `FlashModel`/`FlashTexture` or
`MuzzleFlashModel` is used at that muzzle; otherwise the driveable's built-in
default is used. Still add or preserve the gun definition's own eligible flash per
§3 for its hand-held and deployable contexts. Do not add `FlashTexture`,
`animMuzzleFlashPoint` or `animFlashScale` to a driveable: those are gun-only keys.
AA-gun definitions are outside this flash path and must not receive the driveable
key. Audit them separately under §5.5.

### 5.2 Shoot particles

Vehicle-cannon coverage is exhaustive: audit every primary and secondary bank
that fires an autocannon, tank gun, anti-tank gun, howitzer or other conventional
cannon. Each supported bank should have a calibre-appropriate layered muzzle blast,
either already present or added here. A small-arms bank may also receive the lighter
sets below when appropriate. Shoot particles are drawn at the shoot point, so place
them only on a point whose position is known. Never measure or estimate a driveable
muzzle by hand. Decide per bank from the `shootPointSync` report (§1.3):

| Report state for the bank | Position evidence | Shoot particles |
|---|---|---|
| `primary` `unchanged`, not in Check first | Measured main-gun muzzle | Add |
| `primary` `update`, or a `GunOrigin`/`Barrel` `add` | The pack is not synced yet | Apply mode: sync first (below), then add. Audit mode: propose the sync and the particles together. |
| `primary` `skipped` (several points, or a point that mounts its own gun) | Not measured; authored only | Add only when the authored points pass the authored-only check below. Otherwise skip: `no reliable position`. |
| Secondary bank, `AddGun`, or listed under "Nothing to measure" | Not measured; the tool never moves these | Authored-only check |
| Any row in Check first (score 15 or more) | Measurement or placement in doubt | Skip and list as suspicious |
| Definition missing from the report, or its model `skipped`/`failed` | None | Skip: `no reliable position` |

**Syncing.** In apply mode, when the report has pending `update` or `add` rows,
run the task with `-Pwrite` and the same filter before adding any particle. Then
run it again without `-Pwrite` and confirm `0 to update, 0 to add`. This is the one
permitted change to existing position lines, and only the task makes it. Never edit
`ShootPointPrimary`, `BarrelPosition`, `GunOrigin` or `Barrel` values by hand. Report
the synced lines apart from the added effects, because they move projectile origins
as well. Audit mode never writes.

**Authored-only check.** The authored point must sit where the weapon visibly is:
on a gun port, a wing leading edge, a pod or a turret face that the model source
shows. It must not sit on the turret pivot or inside the hull. Planes' wing guns are
usually not modelled, so this check is their normal path. Anything placed this way
goes on the suspicious list, and `/flandebug shootpoint list` can confirm it in game.

Seat guns (`GunOrigin`) are synced by the same task, but the engine gives them no
`ShootParticles*` path (§2 limitations); their flashes are handled by §5.1. Their
rows matter here only as a check on the model: a gun that measures far from its
gunner seat, or across the hull, points to a model or seat problem worth reporting.

Classify existing cannon particles into three visual layers:

- **pressure flash**: `largeexplode` or `explode` at the muzzle;
- **hot gas and flame**: `flansmod.fmflame`, normally pushed along the bore or
  sideways through a muzzle brake;
- **smoke and dust**: `largesmoke`, `smoke` or a restrained `cloud` ring.

A cannon set is complete when it has a short bright core and a smoke body suited to
its calibre; large guns should also communicate blast pressure. Preserve complete
sets. When a set is sparse, retain every authored line and add only its missing
layer or layers. Do not paste a second full preset on top of existing particles,
and do not add near-duplicate lines merely to increase density. Record the original
and supplemented layers in the report.

Treat the table as a palette and scale reference, not a set of mandatory recipes.
Creative combinations are welcome: mix a pressure flash, staggered flame jets,
sparks (`crit`), smoke and dust (`cloud`) when the weapon supports that look. Give
each particle a job and shape its velocity into a coherent blast: a narrow forward
cone for an unbraked barrel, paired side jets for a muzzle brake, a wider cloud for
a short howitzer, and a lighter, faster-clearing effect for rapid fire. Alternate
slightly asymmetric vectors to avoid a flat ring while keeping the overall blast
centred on the bore. Use only names verified in `ParticleHelper`, keep the total no
denser than a comparable complete set already in the pack, and avoid explosion
particles on every autocannon round when flame and smoke communicate the shot more
cleanly.

For the extensive source analysis, supported particle palette, geometry rules and
ready-to-adapt configurations from rifle-calibre MGs through siege guns, read
[muzzle-blast-proposals.md](muzzle-blast-proposals.md). Use its fan-out calculation
for every multi-point or multi-barrel bank before applying the budget.

Use the pack's own complete set for the same weapon class when one exists.
Otherwise build from these examples. Rows marked W44 are Warfare 44's own sets;
the others are proposals derived from them, to scale with care:

| Weapon class | Set per shot point (direction = forward +x) | Source |
|---|---|---|
| Rifle-calibre MG, aircraft MG | ring of 5 `crit 0.4 ±0.1` + 1 `flansmod.fmflame 0 0 0` | W44 planes |
| HMG / 20–40 mm autocannon | 2 `flansmod.fmflame` along the bore (`0.5` and `0.25`) + 2–4 `smoke 0.3` spread narrowly around it | W44 (flames), proposed (smoke) |
| Tank or AT gun, 76 mm and up; howitzer | `largeexplode 0 0 0` + `explode 1.5 0 0` + a 6–8 particle `flansmod.fmflame` burst + ring of 6–8 `largesmoke 0.5`; add a restrained `cloud 0.05` ring only where the pack uses it for heavy blast | W44 tanks |
| Tank or AT gun, 37–75 mm | `largeexplode 0 0 0` or `explode 1.0 0 0` + 4–6 `flansmod.fmflame` + 4–6 `largesmoke`; omit the heavy `cloud` ring | proposed from W44 |
| Muzzle-braked guns | bias the ring sideways (larger ±z) rather than forward | proposed |
| Recoilless or rocket primary | launch plume `flansmod.rocketexhaust` or `explode -1.5 0 0` backwards; never a forward flash | proposed |

Mirror the direction (`-x`) only when the pack's model faces backwards (see
existing reversed sets in the pack before assuming). Recoilless rifles and rocket
launchers use the launch-plume row rather than the conventional cannon requirement.

### 5.3 Engine exhaust

Add an exhaust emitter only when the exhaust location is known from the model
source (a part commented or grouped as exhaust, or clearly shaped as pipes or
mufflers), from a same-family reference, or from pack-consistent evidence. Follow
the Warfare 44 throttle bands:

- idle `explode 1 [pos] [1,1,1] [0,0.2,0] 0.05 0.2 0 1 <part>`;
- cruise `explode 1 ... [0,0.7,0] 0.2 0.5 0 1`;
- load `explode 1 ... [0,0.7,0] 0.5 2 0 1`;
- full-throttle `largesmoke 3 ... [0,0.5,0] 0.8 2 0 1`.

Diesel engines (most Soviet and Japanese tanks, late trucks) get one more
`largesmoke` at load. Petrol engines stay lighter. Jets get
`flansmod.afterburn` at high throttle only where the pack has afterburners.
Exhaust on a part the engine isn't in (a turret, say) is a bug.

### 5.4 Damage smoke and fire

For each vehicle or plane lacking damage emitters, add the Warfare 44 ladder over
the engine compartment:

- `smoke 2 [pos] [7,1,7] [0,0.5,0] -1 1 0 0.75 core`;
- `largesmoke 3 ... [0,1,0] -1 1 0 0.5 core`;
- `largesmoke 1 ... [0,1.5,0] -1 1 0 0.25 core`;
- `flame 1 ... [5,1,5] [0,1,0] -1 1 0 0.25 core`.

For planes, use each engine or nacelle (and a wing root for a single-engine
fighter's fuel fire only if the pack's style does). Engine location must be
evidenced by the model, the definition's collision or engine hints, or a
historically certain layout. Most tanks are rear-engined. Many APCs and IFVs
(M113, BMP family), the Merkava, and nearly all trucks and cars are
front-engined, and some light tanks mount the engine to one side. Verify each
vehicle; never assume the layout from its class. Being diffuse, damage smoke tolerates about ±4 px.
Exhaust does not.

Remember emitters only run with the engine on. Don't add emitters to towed guns or
static emplacements without an engine.

### 5.5 AA guns

Audit every file under `definitions/aaguns`, including manned mounts, sentries,
anti-aircraft autocannons, heavy dual-purpose guns, missile launchers and fictional
turrets. Record `Model`, `NumBarrels`, every `Barrel`, `FireAlternately`, cadence,
ammo entries and ammo groups.

**Placement.** Use the AA-gun rows from `shootPointSync`. In apply mode, synchronize
pending `Barrel` updates or additions with `-Pwrite`, then rerun read-only and
require `0 to update, 0 to add`. Never edit a `Barrel` line by hand. Put every
Check first, skipped or failed row on the suspicious list. Also report a barrel
count that does not match usable model geometry; the parser clamps `NumBarrels` to
at least one, so an authored zero is not a visual exemption.

**Current muzzle limitation.** `AAGunType` and `AAGunRenderer` have no supported
flash-model or shoot-particle path. Do not add `FlashModel`, `MuzzleFlashModel`,
`ShootParticlesPrimary`, `ShootParticlesSecondary` or invented AA equivalents to
an AA-gun definition. Instead, select or compose the effect the weapon should use
from [muzzle-blast-proposals.md](muzzle-blast-proposals.md) and include it in the
report as an `engine limitation` proposal. State its intended layers, per-barrel
line count, and worst-case volley count.

`FireAlternately True` fires one barrel from the rotating index per volley.
`FireAlternately False` can fire every loaded barrel in the same volley. Use that
fan-out when selecting the future proposal: a quad 20 mm mount needs a much smaller
per-barrel effect than a single 40 mm gun even when their individual calibres are
similar.

**Actionable ammunition visuals.** Resolve every direct `Ammo` entry and every
round admitted through `UseAmmoGroup`, then audit those `BulletType` definitions
under §6:

- ordinary ball or AP receives no invented impact explosion;
- historically or fictionally appropriate tracer belts may use the pack's tracer
  colour, with `flansmod.fmtracer` as the neutral fallback;
- autocannon HE gets a restrained trail and small impact effect consistent with
  its filler;
- timed, proximity or dedicated flak rounds may receive `FlakParticles` and a
  smoke trail;
- guided AA missiles use rocket exhaust or smoke and `BoostParticle` where their
  motor has a boost phase;
- fictional turrets follow the pack's established energy palette rather than a
  conventional firearm effect.

Do not add every eligible effect to a shared round merely because one AA gun uses
it. First inventory every gun, vehicle and aircraft that accepts that ammunition;
the projectile visual follows the round in all contexts. Report shared-ammo impact
as suspicious when the new visual may be inappropriate for another weapon.

## 6. Projectiles and grenades

Match the pack's own conventions first. Otherwise:

| Projectile | Trail | Detonation |
|---|---|---|
| Ball, AP small-arms | none | none |
| Tracer rounds and belts with tracers | `SmokeTrail True`, `TrailParticleType` = the pack's tracer colour, else `flansmod.fmtracer` | none |
| AP / APCR / APDS / APFSDS shot | `flansmod.fmtracer` | none, or small `ExplodeParticles` for impact flash if the pack does it |
| HE / HEAT shell | `flansmod.fmflame` (Warfare 44) or none | `ExplodeParticles 6–10`, `FlareParticleCount 3–8`, `DebrisParticleCount 2–6`, rising with calibre |
| Autocannon HE belts | `flansmod.fmsmoke` | small counts: `ExplodeParticles 4–6` |
| Flak / proximity | `flansmod.fmsmoke` | `FlakParticles 5–15` |
| Rockets, missiles | `flansmod.rocketexhaust` or `smoke`, plus `BoostParticle` where the pack models boost | as HE by warhead |
| Mortar and artillery shells | none | as HE by filler |
| Rifle grenades, launcher grenades | `smoke` | as HE by filler |
| Smoke, flash and gas grenades | unchanged | gameplay: out of scope |

Tracer colours follow the pack's convention. Use a nation-specific colour
(red, green) only with a source; otherwise keep the default tracer. Never raise
existing counts, only fill missing ones.

## 7. Budget

Per event, over all its lines:
- a shoot-particle set of at most 20 particles for main guns, and at most 6 for
  automatic weapons (anything firing more than about 5 rounds per second);
- at most 4 emitter lines per engine and at most 4 per damage location;
- emitter `rate` at least 1, and at least 2 for `largesmoke` at idle;
- a projectile trail of one particle type.

When the pack's existing style exceeds these, keep it and report it; never exceed
it in additions. For shoot particles, multiply the configured line count by the
number of shoot points that fire together. The resulting worst-case particles per
trigger, rather than the lines written once in the definition, is the budgeted
quantity.

## 8. Validation

1. Every particle name used is known to `ParticleHelper`. Every `FlashModel`
   resolves: `DefaultFlash` or a class present in the pack's model package, and
   every existing `MuzzleFlashModel` resolves too.
   Every `FlashTexture` exists under the pack's `assets/flansmod/textures/skins`.
2. Re-run `muzzleReport` for the touched packages when model understanding
   changed, and compare each added `animMuzzleFlashPoint × flashScale × 16` with
   `muzzlePx` (tolerance 0.5 px). For every deployable flash, confirm its
   `DeployedModel` still has a `measured` deployable row and inspect the reported
   point against the model source. When driveables or AA guns were touched, re-run
   `shootPointSync` with the pack filter and confirm `0 to update, 0 to add`, and
   that every driveable that gained `FlashModel DefaultFlash` satisfies §5.1 and
   every bank that gained `ShootParticles*` is placed per §5.2. Confirm every
   audited AA gun satisfies §5.5, its ammo visuals resolve, and no unsupported
   muzzle-effect key was added to its definition.
3. Run the pack's jar task (`manusPacksJar`, `warfare44Pack`, `officialPacksJar`,
   ... from `src/<set>/fmu-module.gradle`), plus `packsManagerJar` if its inputs
   changed.
4. Run `git diff --check` on the touched files, then read the scoped diff and check
   that only added lines appear (plus any user-approved repairs).
5. Static validation cannot prove on-screen placement. Say so, and list the
   suspicious items (SKILL.md).

## 9. Report

Use these sections in the final message (audit mode: proposals; apply mode: done):

1. **Added**: per definition, the lines added and the evidence (report row,
   declared point, family match, or model part).
   **Synced shoot points** (apply mode, when §5.1 or §5.2 ran the sync): the definitions and
   values `shootPointSync -Pwrite` changed, listed apart from the effects.
2. **Skipped, with reason**: `no reliable position`, `not realistic`,
   `mixed flash eligibility`, `engine limitation`, `already has effect`,
   `gameplay key (out of scope)`.
   For each AA gun, include the proposed muzzle-effect silhouette and template
   under `engine limitation`, followed separately by any ammunition effects that
   were actually added.
3. **Suspicious (in-game check candidates)**: the short, concrete list.
4. **Existing issues found**: broken names, mispositioned existing flashes,
   budget-breaking styles, for the user to decide.
5. **Validation**: what ran, what didn't, and the results.
6. **Wiki**: keys used that `ConfigReference.md` doesn't document.
