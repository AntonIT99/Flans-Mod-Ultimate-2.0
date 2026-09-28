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
   tool could not place the point. §5.1 turns this into placement decisions.
4. Note which flash asset the pack already uses (`FlashModel`/`FlashTexture` pairs,
   `MuzzleFlashModel`), and the pack's particle conventions: shoot-particle sets,
   emitter sets, trail types.
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
| Driveable | `ShootParticlesPrimary` / `ShootParticlesSecondary name x y z` | Spawned once per fired shoot point at that point's shoot origin (root plus offset), the same point the projectile leaves from. The particles are therefore exactly as well placed as the shoot point (§5.1). `x y z` is a direction and speed in the model basis: +x forward, y up. |
| Driveable | `AddEmitter` / `AddParticle name rate [ox,oy,oz] [ex,ey,ez] [vx,vy,vz] minThrottle maxThrottle minHealth maxHealth part` | Continuous emitter. The origin, extents and velocity are in type-file pixels. It emits every `rate` ticks while throttle and part-health fraction are inside the bounds. It runs only while the engine is on (vehicles and planes) and within the local emitter range. Turret and barrel parts follow the turret. |
| Driveable | `EmittersRequireOccupant` | Emitters only while occupied. |
| Attachment | `DisableMuzzleFlash` / `DisableFlash` | Suppresses both flash models and the world particle while fitted. |

Engine limitations. Report these; don't work around them:
- deployed guns (`Deployable True`, drawn by `ModelMG`) can draw `FlashModel` or
  `MuzzleFlashModel` at their measured muzzle, but still have no world
  muzzle-flash particle path;
- vehicle passenger or seat guns get no shoot particles or flash;
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

## 5. Driveables

### 5.1 Shoot particles

Shoot particles are drawn at the shoot point, so place them only on a point
whose position is known. Never measure or estimate a driveable muzzle by hand.
Decide per bank from the `shootPointSync` report (§1.3):

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
shoot particles or flash (§2 limitations). Their rows matter here only as a check
on the model: a gun that measures far from its gunner seat, or across the hull,
points to a model or seat problem worth reporting.

Add sets only to banks without any `ShootParticles*` line. Use the pack's own set
for the same weapon class when it exists. Otherwise use these. Rows marked W44 are
Warfare 44's own sets; the others are proposals derived from them, to scale with
care:

| Weapon class | Set per shot point (direction = forward +x) | Source |
|---|---|---|
| Rifle-calibre MG, aircraft MG | ring of 5 `crit 0.4 ±0.1` + 1 `flansmod.fmflame 0 0 0` | W44 planes |
| HMG / 20–40 mm autocannon | `flansmod.fmflame 0.5 0 0` + `flansmod.fmflame 0.25 0 0`, optionally 2 `smoke 0.3 ±0.05` | W44 (flames), proposed (smoke) |
| Tank or AT gun, 76 mm and up; howitzer | `largeexplode 0 0 0` + `explode 1.5 0 0` + ring of 8 `largesmoke 0.5 ±0.1` + ring of 8 `cloud 0.05 ±0.2` | W44 tanks |
| Tank or AT gun, 37–75 mm | the heavy set without the `cloud` ring, and 4–6 `largesmoke` | proposed |
| Muzzle-braked guns | bias the ring sideways (larger ±z) rather than forward | proposed |
| Recoilless or rocket primary | launch plume `flansmod.rocketexhaust` or `explode -1.5 0 0` backwards; never a forward flash | proposed |

Mirror the direction (`-x`) only when the pack's model faces backwards (see
existing reversed sets in the pack before assuming).

### 5.2 Engine exhaust

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

### 5.3 Damage smoke and fire

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
it in additions.

## 8. Validation

1. Every particle name used is known to `ParticleHelper`. Every `FlashModel`
   resolves: `DefaultFlash` or a class present in the pack's model package, and
   every existing `MuzzleFlashModel` resolves too.
   Every `FlashTexture` exists under the pack's `assets/flansmod/textures/skins`.
2. Re-run `muzzleReport` for the touched packages when model understanding
   changed, and compare each added `animMuzzleFlashPoint × flashScale × 16` with
   `muzzlePx` (tolerance 0.5 px). For every deployable flash, confirm its
   `DeployedModel` still has a `measured` deployable row and inspect the reported
   point against the model source. When driveables were touched, re-run
   `shootPointSync` with the pack filter and confirm `0 to update, 0 to add`, and
   that every bank that gained `ShootParticles*` is placed per §5.1.
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
   **Synced shoot points** (apply mode, when §5.1 ran the sync): the definitions and
   values `shootPointSync -Pwrite` changed, listed apart from the effects.
2. **Skipped, with reason**: `no reliable position`, `not realistic`,
   `engine limitation`, `already has effect`, `gameplay key (out of scope)`.
3. **Suspicious (in-game check candidates)**: the short, concrete list.
4. **Existing issues found**: broken names, mispositioned existing flashes,
   budget-breaking styles, for the user to decide.
5. **Validation**: what ran, what didn't, and the results.
6. **Wiki**: keys used that `ConfigReference.md` doesn't document.
