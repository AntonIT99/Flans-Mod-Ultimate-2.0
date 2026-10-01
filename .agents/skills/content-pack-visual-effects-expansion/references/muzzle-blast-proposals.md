# Vehicle muzzle-blast proposals

Use this catalogue with `workflow.md` §5.2 and §5.5. It is a set of starting
designs for `ShootParticlesPrimary`, `ShootParticlesSecondary` and
`ShootParticlesPassenger`, not a reason to replace a pack's good existing style. The current `AAGunType` does not support
these keys: for an AA-gun definition, use the catalogue to write a report proposal
only and never paste its lines into the definition. Every effect that can be
applied still needs the position evidence, particle-name validation and budget
checks in the main workflow.

## 1. Source basis

The proposals combine three sources:

- **Warfare 44** (`src/warfare44pack`): strong layered cannon effects built from
  an instantaneous `largeexplode`/`explode` core, radial `largesmoke` and `cloud`,
  and long `flansmod.fmflame` cones. Its examples establish the visual language,
  but many contain 40–50 particles per shot and are too dense to copy as a default.
- **Tyrants and Plebeians Hero Shooter September**: useful contrasting patterns
  include a two-particle MG smoke/spark effect, sparse five- or six-particle naval
  salvos, distinct flash/muzzle/spark layers on some 37 mm guns, and enormous
  explosion/cloud volumes for howitzers and mortars. Treat its largest effects as
  silhouette and motion inspiration only. Its `flansmod.NuFlash`,
  `flansmod.NuMuzzle`, `flansmod.NuSpark` and `smokeShell` names do not exist in
  this project's `FlanParticles`; translate their visual roles to supported names.
- **Purpose-built combinations below**: these retain the readable layered look
  while respecting the current budget and adapting the shape to the weapon.

Reference packs are evidence for a style, not proof that a particle name, density
or vector is appropriate in the selected target pack.

## 2. Reading the vectors

Each line is one particle spawned at the exact fired shoot point or passenger
barrel. The last three numbers are velocity in the muzzle's local basis:

- `+x`: forward along the bore;
- `-x`: rearward, useful for a recoilless backblast or rocket exhaust;
- `+/-y`: vertical spread;
- `+/-z`: side spread.

The examples use `ShootParticlesPrimary`. Replace the key with
`ShootParticlesSecondary` for a secondary cannon bank, or with
`ShootParticlesPassenger <seatId>` for an eligible passenger mount (for example,
`ShootParticlesPassenger 3 flansmod.fmflame 0.58 0 0`). The seat ID belongs to
the driveable's `Passenger` line. Passenger effects follow current aim and the
fired barrel without manual positional offsets, and do not require a flash-model
opt-in. Apply the seat-origin checks in workflow §5.2. Mirror x only when model or
pack evidence shows the barrel faces the opposite way.

Count the bank fan-out before choosing a set:

`particles per trigger = lines in the set × shoot points fired together`

A twin or quad mount therefore needs a smaller per-point set than a single barrel.
Alternating barrels use the per-barrel count; simultaneous barrels use the total.
Current passenger guns alternate barrels and emit one set per successful shot;
do not multiply their set by the mount's barrel count. Account for independently
firing seats and sustained cadence when choosing smoke density.

## 3. Supported visual palette

Use only names accepted by the current `FlanParticles` resolver. These are the most
useful for muzzle effects:

| Role | Particle | Use |
|---|---|---|
| Pressure core | `largeexplode` | One bright, broad impulse for conventional guns. |
| Large pressure core | `hugeexplosion` | One only, reserved for exceptionally large naval guns or mortars after an in-game check. |
| Hot gas | `explode` | A smaller bright puff that can lead the flame cone. |
| Flame | `flansmod.fmflame` | Main orange flash body; shape with forward or lateral velocity. |
| Fine smoke | `smoke` | Light, quick-clearing residue for automatic weapons. |
| Heavy smoke | `largesmoke` | Main smoke body for cannon shots. |
| Dust / vapour | `cloud` | Soft outer volume; use sparingly because a full ring quickly obscures the muzzle. |
| Sparks | `crit` | A few hot fragments or a crisp automatic-weapon accent. |
| Dense mod smoke | `flansmod.fmsmoke` | Compact smoke accent when already part of the pack's style. |
| Rocket exhaust | `flansmod.rocketexhaust` | Rocket motor or recoilless backblast, normally rearward. |
| Energy accent | `endrod`, `spell`, `instantspell`, `portal` | Fictional weapons only, matching their established visual language. |

Do not use a particle merely because it is available. Historical guns should stay
within explosion, flame, smoke, cloud and occasional spark layers.

## 4. Shape grammar

Build a custom set by choosing one element from each needed layer:

1. **Core**: zero or one `largeexplode`, plus zero or one `explode`. Automatic
   cannons normally omit `largeexplode` so every round does not look like a shell
   detonation.
2. **Axial flame**: one to three `flansmod.fmflame` particles at different +x
   speeds make a readable cone without exact duplicates.
3. **Brake or crown flame**: paired +/-z or +/-y vectors reveal side-port or radial
   muzzle brakes. Use pairs so the flash stays centred.
4. **Smoke body**: two to eight smoke particles form the lingering volume. A long
   gun gets a narrower forward cone; a howitzer gets a wider, slower cloud.
5. **Accents**: zero to two sparks, dust puffs or an extra pressure puff. Accents
   should clarify the weapon rather than fill unused budget.

Prefer near-symmetry with small variations. Perfect duplicated rings read as flat;
large unpaired vectors make the shot appear to leave the barrel sideways.

## 5. Ready-to-adapt proposals

These blocks assume one shoot point fires per trigger. Keep the appropriate key,
remove layers already present, and adjust vectors to the actual muzzle geometry.

### A. Rifle-calibre vehicle MG — 3 particles

For a light coaxial or pintle MG when the bank supports shoot particles. The flash
model remains the main visual; this adds brief gas and smoke.

```text
ShootParticlesPrimary flansmod.fmflame 0.28 0 0
ShootParticlesPrimary flansmod.fmsmoke 0.08 0.015 0
ShootParticlesPrimary crit 0.22 -0.015 0
```

### B. Heavy MG, 12.7–15 mm — 5 particles

```text
ShootParticlesPrimary flansmod.fmflame 0.45 0 0
ShootParticlesPrimary flansmod.fmflame 0.25 0.025 0.02
ShootParticlesPrimary smoke 0.16 0.025 -0.025
ShootParticlesPrimary smoke 0.13 -0.02 0.03
ShootParticlesPrimary crit 0.3 -0.025 -0.02
```

### C. Rapid 20–30 mm autocannon — 6 particles

Keeps each round readable without stacking explosion sprites during a burst.

```text
ShootParticlesPrimary flansmod.fmflame 0.58 0 0
ShootParticlesPrimary flansmod.fmflame 0.34 0.04 0.025
ShootParticlesPrimary flansmod.fmflame 0.3 -0.035 -0.03
ShootParticlesPrimary smoke 0.2 0.035 -0.04
ShootParticlesPrimary smoke 0.18 -0.03 0.045
ShootParticlesPrimary crit 0.38 0.01 0
```

For a twin simultaneous mount, use a three-particle subset per barrel: one axial
flame and two opposed smoke/flame vectors.

### D. Rapid 37–57 mm cannon — 6 particles

Use for automatic AA guns and high-cadence naval mounts. A single `explode` gives
weight without repeating the broad `largeexplode` sprite every round.

```text
ShootParticlesPrimary explode 0.55 0 0
ShootParticlesPrimary flansmod.fmflame 0.72 0 0
ShootParticlesPrimary flansmod.fmflame 0.38 0.06 0.04
ShootParticlesPrimary flansmod.fmflame 0.34 -0.05 -0.05
ShootParticlesPrimary smoke 0.24 0.05 -0.06
ShootParticlesPrimary smoke 0.21 -0.045 0.06
```

### E. Single-shot 37–57 mm gun — 10 particles

For light tank guns and towed anti-tank guns.

```text
ShootParticlesPrimary explode 0.7 0 0
ShootParticlesPrimary flansmod.fmflame 0.95 0 0
ShootParticlesPrimary flansmod.fmflame 0.58 0.07 0.05
ShootParticlesPrimary flansmod.fmflame 0.54 -0.06 -0.06
ShootParticlesPrimary flansmod.fmflame 0.42 0.04 -0.08
ShootParticlesPrimary largesmoke 0.3 0.07 0.06
ShootParticlesPrimary largesmoke 0.28 -0.06 -0.07
ShootParticlesPrimary largesmoke 0.24 0.04 -0.09
ShootParticlesPrimary largesmoke 0.22 -0.04 0.09
ShootParticlesPrimary crit 0.48 0 0
```

### F. Medium 60–90 mm unbraked gun — 14 particles

The narrow forward cone suits a plain barrel or compact muzzle collar.

```text
ShootParticlesPrimary largeexplode 0 0 0
ShootParticlesPrimary explode 1.05 0 0
ShootParticlesPrimary flansmod.fmflame 1.35 0 0
ShootParticlesPrimary flansmod.fmflame 0.95 0.08 0.04
ShootParticlesPrimary flansmod.fmflame 0.9 -0.07 -0.05
ShootParticlesPrimary flansmod.fmflame 0.68 0.04 -0.09
ShootParticlesPrimary flansmod.fmflame 0.62 -0.04 0.1
ShootParticlesPrimary largesmoke 0.42 0.09 0.07
ShootParticlesPrimary largesmoke 0.4 -0.08 -0.08
ShootParticlesPrimary largesmoke 0.34 0.05 -0.11
ShootParticlesPrimary largesmoke 0.32 -0.05 0.12
ShootParticlesPrimary largesmoke 0.28 0.1 -0.02
ShootParticlesPrimary cloud 0.08 0.08 0.12
ShootParticlesPrimary cloud 0.07 -0.07 -0.13
```

### G. Medium 60–100 mm side-port muzzle brake — 16 particles

The small +x component keeps the blast attached to the muzzle while the large +/-z
component creates the paired side jets.

```text
ShootParticlesPrimary largeexplode 0 0 0
ShootParticlesPrimary explode 0.75 0 0
ShootParticlesPrimary flansmod.fmflame 0.28 0 0.82
ShootParticlesPrimary flansmod.fmflame 0.28 0 -0.82
ShootParticlesPrimary flansmod.fmflame 0.24 0.12 0.68
ShootParticlesPrimary flansmod.fmflame 0.24 0.12 -0.68
ShootParticlesPrimary flansmod.fmflame 0.22 -0.1 0.62
ShootParticlesPrimary flansmod.fmflame 0.22 -0.1 -0.62
ShootParticlesPrimary largesmoke 0.18 0.03 0.46
ShootParticlesPrimary largesmoke 0.18 0.03 -0.46
ShootParticlesPrimary largesmoke 0.16 0.1 0.38
ShootParticlesPrimary largesmoke 0.16 0.1 -0.38
ShootParticlesPrimary largesmoke 0.15 -0.08 0.35
ShootParticlesPrimary largesmoke 0.15 -0.08 -0.35
ShootParticlesPrimary cloud 0.06 0 0.22
ShootParticlesPrimary cloud 0.06 0 -0.22
```

For a multi-baffle brake, retain the paired shape and vary magnitude slightly; do
not add a new pair for every visible baffle.

### H. Heavy 100–130 mm tank or anti-tank gun — 18 particles

```text
ShootParticlesPrimary largeexplode 0 0 0
ShootParticlesPrimary explode 1.4 0 0
ShootParticlesPrimary flansmod.fmflame 1.7 0 0
ShootParticlesPrimary flansmod.fmflame 1.25 0.1 0.08
ShootParticlesPrimary flansmod.fmflame 1.18 -0.09 -0.09
ShootParticlesPrimary flansmod.fmflame 0.88 0.08 -0.13
ShootParticlesPrimary flansmod.fmflame 0.82 -0.07 0.14
ShootParticlesPrimary flansmod.fmflame 0.62 0.14 0
ShootParticlesPrimary largesmoke 0.52 0.12 0.1
ShootParticlesPrimary largesmoke 0.5 -0.11 -0.11
ShootParticlesPrimary largesmoke 0.44 0.08 -0.16
ShootParticlesPrimary largesmoke 0.42 -0.08 0.17
ShootParticlesPrimary largesmoke 0.36 0.16 0.02
ShootParticlesPrimary largesmoke 0.34 -0.15 -0.02
ShootParticlesPrimary cloud 0.09 0.13 0.18
ShootParticlesPrimary cloud 0.09 0.13 -0.18
ShootParticlesPrimary cloud 0.08 -0.12 0.19
ShootParticlesPrimary cloud 0.08 -0.12 -0.19
```

### I. Short howitzer, 75–155 mm — 18 particles

This uses a slower, wider cloud than the tank-gun cone.

```text
ShootParticlesPrimary largeexplode 0 0 0
ShootParticlesPrimary explode 0.9 0 0
ShootParticlesPrimary flansmod.fmflame 1.05 0 0
ShootParticlesPrimary flansmod.fmflame 0.58 0.14 0.12
ShootParticlesPrimary flansmod.fmflame 0.55 -0.12 -0.13
ShootParticlesPrimary flansmod.fmflame 0.48 0.08 -0.17
ShootParticlesPrimary largesmoke 0.3 0.18 0.12
ShootParticlesPrimary largesmoke 0.3 0.18 -0.12
ShootParticlesPrimary largesmoke 0.28 -0.16 0.14
ShootParticlesPrimary largesmoke 0.28 -0.16 -0.14
ShootParticlesPrimary largesmoke 0.24 0.08 0.2
ShootParticlesPrimary largesmoke 0.24 0.08 -0.2
ShootParticlesPrimary largesmoke 0.22 -0.08 0.21
ShootParticlesPrimary largesmoke 0.22 -0.08 -0.21
ShootParticlesPrimary cloud 0.05 0.16 0.2
ShootParticlesPrimary cloud 0.05 0.16 -0.2
ShootParticlesPrimary cloud 0.05 -0.14 0.22
ShootParticlesPrimary cloud 0.05 -0.14 -0.22
```

### J. Black-powder field gun — 18 particles

Use for early cannon where the smoke cloud is the signature. Keep the flame core
smaller than the broad pale smoke body.

```text
ShootParticlesPrimary largeexplode 0 0 0
ShootParticlesPrimary explode 0.65 0 0
ShootParticlesPrimary flansmod.fmflame 0.75 0 0
ShootParticlesPrimary flansmod.fmflame 0.42 0.1 0.08
ShootParticlesPrimary flansmod.fmflame 0.4 -0.09 -0.09
ShootParticlesPrimary largesmoke 0.28 0.16 0.12
ShootParticlesPrimary largesmoke 0.28 0.16 -0.12
ShootParticlesPrimary largesmoke 0.26 -0.14 0.14
ShootParticlesPrimary largesmoke 0.26 -0.14 -0.14
ShootParticlesPrimary cloud 0.08 0.2 0
ShootParticlesPrimary cloud 0.08 -0.18 0
ShootParticlesPrimary cloud 0.07 0 0.22
ShootParticlesPrimary cloud 0.07 0 -0.22
ShootParticlesPrimary cloud 0.06 0.16 0.16
ShootParticlesPrimary cloud 0.06 0.16 -0.16
ShootParticlesPrimary cloud 0.06 -0.15 0.17
ShootParticlesPrimary cloud 0.06 -0.15 -0.17
ShootParticlesPrimary smoke 0.18 0.03 0
```

### K. Naval gun, 150–203 mm — 20 particles

This expands the sparse Tyrants and Plebeians naval pattern into a readable hot
core, smoke body and damp-looking outer vapour without copying its duplicate lines.

```text
ShootParticlesPrimary largeexplode 0 0 0
ShootParticlesPrimary largeexplode 0.25 0 0
ShootParticlesPrimary explode 1.55 0 0
ShootParticlesPrimary flansmod.fmflame 1.9 0 0
ShootParticlesPrimary flansmod.fmflame 1.35 0.12 0.1
ShootParticlesPrimary flansmod.fmflame 1.3 -0.11 -0.11
ShootParticlesPrimary flansmod.fmflame 0.95 0.09 -0.16
ShootParticlesPrimary flansmod.fmflame 0.9 -0.08 0.17
ShootParticlesPrimary largesmoke 0.58 0.14 0.12
ShootParticlesPrimary largesmoke 0.56 -0.13 -0.13
ShootParticlesPrimary largesmoke 0.5 0.1 -0.18
ShootParticlesPrimary largesmoke 0.48 -0.09 0.19
ShootParticlesPrimary largesmoke 0.42 0.18 0
ShootParticlesPrimary largesmoke 0.4 -0.17 0
ShootParticlesPrimary cloud 0.1 0.18 0.22
ShootParticlesPrimary cloud 0.1 0.18 -0.22
ShootParticlesPrimary cloud 0.09 -0.17 0.23
ShootParticlesPrimary cloud 0.09 -0.17 -0.23
ShootParticlesPrimary cloud 0.07 0 0.28
ShootParticlesPrimary cloud 0.07 0 -0.28
```

For several naval barrels firing together, divide the 20-particle event budget by
the simultaneous barrel count. A twin mount can use six particles per barrel: one
`largeexplode`, one `explode`, two opposed `largesmoke`, and two opposed `cloud`
particles. A triple or quad mount needs five or fewer per barrel; remove one cloud
or merge the two pressure roles rather than exceeding the event budget.

### L. Very heavy mortar or siege gun, 200 mm and up — 20 particles

The Tyrants and Plebeians 13-inch mortar uses hundreds of particles. This captures
its broad silhouette within the project budget. `hugeexplosion` needs an in-game
check; replace it with `largeexplode` if it overwhelms the model.

```text
ShootParticlesPrimary hugeexplosion 0 0 0
ShootParticlesPrimary largeexplode 0.4 0 0
ShootParticlesPrimary explode 0.8 0.16 0
ShootParticlesPrimary explode 0.8 -0.14 0
ShootParticlesPrimary flansmod.fmflame 0.9 0 0
ShootParticlesPrimary flansmod.fmflame 0.45 0.16 0.14
ShootParticlesPrimary flansmod.fmflame 0.42 -0.15 -0.15
ShootParticlesPrimary largesmoke 0.25 0.22 0
ShootParticlesPrimary largesmoke 0.25 -0.2 0
ShootParticlesPrimary largesmoke 0.23 0 0.24
ShootParticlesPrimary largesmoke 0.23 0 -0.24
ShootParticlesPrimary largesmoke 0.2 0.18 0.18
ShootParticlesPrimary largesmoke 0.2 0.18 -0.18
ShootParticlesPrimary cloud 0.04 -0.18 0.2
ShootParticlesPrimary cloud 0.04 -0.18 -0.2
ShootParticlesPrimary cloud 0.04 0.24 0.12
ShootParticlesPrimary cloud 0.04 0.24 -0.12
ShootParticlesPrimary cloud 0.04 -0.22 0.13
ShootParticlesPrimary cloud 0.04 -0.22 -0.13
ShootParticlesPrimary cloud 0.03 0 0.3
```

### M. Recoilless rifle or rocket launcher — 12 particles

This is a launch plume, not a front cannon flash. Confirm the shoot point is at the
tube mouth and that rearward velocity matches the model orientation.

```text
ShootParticlesPrimary flansmod.fmflame 0.35 0 0
ShootParticlesPrimary smoke 0.18 0.04 0.04
ShootParticlesPrimary smoke 0.18 -0.04 -0.04
ShootParticlesPrimary flansmod.rocketexhaust -0.8 0 0
ShootParticlesPrimary flansmod.rocketexhaust -0.65 0.08 0.08
ShootParticlesPrimary flansmod.rocketexhaust -0.65 0.08 -0.08
ShootParticlesPrimary flansmod.rocketexhaust -0.6 -0.07 0.09
ShootParticlesPrimary flansmod.rocketexhaust -0.6 -0.07 -0.09
ShootParticlesPrimary smoke -0.38 0.12 0
ShootParticlesPrimary smoke -0.38 -0.11 0
ShootParticlesPrimary cloud -0.18 0 0.14
ShootParticlesPrimary cloud -0.18 0 -0.14
```

### N. Fictional kinetic or energy cannon — 6 particles

Use only when the pack already establishes an energy visual language. Pick one
accent family; do not combine every magical particle.

```text
ShootParticlesPrimary endrod 0.65 0 0
ShootParticlesPrimary endrod 0.42 0.05 0.04
ShootParticlesPrimary endrod 0.4 -0.05 -0.04
ShootParticlesPrimary crit 0.5 0.04 -0.05
ShootParticlesPrimary crit 0.48 -0.04 0.05
ShootParticlesPrimary portal 0.12 0 0
```

## 6. AA-gun mapping

AA guns belong in the visual audit even though their muzzle-effect path is not yet
implemented. Use this mapping to make their report proposals consistent and to
select visuals for equivalent AA weapons implemented as driveables:

| AA-gun class | Proposal starting point | Current actionable visuals |
|---|---|---|
| Rifle-calibre sentry or tripod | A, or B for an HMG | Barrel sync; tracer belt only when sourced or pack-consistent. |
| 20–30 mm automatic cannon | C | Barrel sync; autocannon HE trail and restrained impact particles. |
| 35–57 mm automatic AA cannon | D | Barrel sync; tracer/HE belt visuals and flak only for timed or proximity ammunition. |
| 37–57 mm slow single-shot gun | E | Barrel sync; shell trail and impact matched to the round. |
| 60–100 mm dual-purpose gun | F or G according to muzzle brake | Barrel sync; shell trail, HE impact and sourced flak behaviour. |
| Heavy or naval AA gun | H or K, reduced for simultaneous barrels | Barrel sync; heavy-shell projectile and detonation visuals. |
| AA missile launcher | M as launch-plume inspiration | Missile trail and `BoostParticle`; no cannon flash proposal. |
| Fictional laser or plasma turret | N, recoloured only through supported pack assets | Pack-specific projectile trail and impact language. |

For `FireAlternately True`, budget one barrel's proposal per volley. For
`FireAlternately False`, multiply by every loaded barrel that can fire together and
reduce the per-barrel set until the complete volley fits the automatic or main-gun
event limit. Record both numbers in the report.

If engine support is later added, re-check the implemented syntax and renderer
semantics before applying any proposal. This catalogue does not make inert keys
valid by documenting them.

## 7. Adaptation rules

### Calibre

- Under 20 mm: flash model plus at most a few gas, smoke or spark particles.
- 20–40 mm automatic: stay at six particles per fired barrel.
- 37–75 mm single-shot: use 8–12 particles.
- 76–155 mm: use 12–18 particles.
- Above 155 mm: use up to 20 particles only when one barrel fires per event.

Calibre alone does not determine the set. Barrel length, propellant era, muzzle
device and rate of fire can move a weapon one tier lighter or heavier.

### Rate of fire

At more than about five rounds per second, remove the broad pressure sprite first,
then reduce smoke. Preserve one or two flame particles so individual rounds remain
visible. Never let a rapid cannon continuously cover the vehicle in explosion
sprites.

### Muzzle devices

- Plain muzzle: narrow +x cone.
- Pepper-pot or radial brake: compact symmetric +/-y and +/-z crown.
- Two-baffle side brake: paired +/-z jets with small +x carry.
- Slotted flash hider: several short, narrow flame vectors and little smoke.
- Suppressor: no front flash model; if the weapon still needs a gas effect, use a
  very small smoke-only set and confirm the pack's intended behaviour.

### Environment and era

- Black powder: broader, slower smoke and cloud; less long axial flame.
- WWII smokeless cannon: bright core, strong flame and visible smoke.
- Modern tank gun: sharp bright core, fast side jets where braked, less lingering
  smoke unless the pack intentionally uses a cinematic style.
- Naval gun: strong pressure and vapour volume, reduced per-barrel density for
  simultaneous salvos.
- Enclosed coaxial gun: small flash and quick smoke; avoid a cloud that fills the
  turret face.

## 8. Choosing and reporting a proposal

For each cannon bank or passenger mount, record:

1. weapon class, approximate calibre and rate of fire;
2. fired shoot-point count and whether barrels alternate or fire together;
3. muzzle-device shape from the model or reliable reference;
4. existing particle layers;
5. selected proposal or custom combination;
6. final line count per barrel and worst-case particles per trigger;
7. placement evidence and any in-game check candidate.

When none of the templates fits, compose a new set from §4. Explain the intended
silhouette in one sentence, such as "brief axial flame followed by paired brake
jets and a light smoke crown." That intent is more useful for review than saying
only that the effect is creative.
