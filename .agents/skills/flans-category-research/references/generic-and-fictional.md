# Generic and Fictional Categories

Read this reference whenever an item cannot be tied to one exact real-world model:
generic labels such as `75mm AP Tank Shell`, `Generic Battleship`, or
`Generic 500lb bomb`, and outright fictional content such as blasters, bolters,
lightsabers, walkers, droids, Nerf blasters, and zombie-pack improvised weapons.

It applies to `gun_categories.json`, `aagun_categories.json`,
`bullet_categories.json`, `grenade_categories.json`, `vehicle_categories.json`, and
`plane_categories.json`. It does not apply to `armor_categories.json`, which already
covers generic and fictional items through its own tier tables; see
[armor.md](armor.md).

Read it together with the applicable domain reference. This file decides *what
identity and what values* a non-historical item gets. The domain reference still
decides *which keys are mandatory* and *what units they use*, and every mandatory
field stays mandatory here.

## Contents

- [Why these items are in scope](#why-these-items-are-in-scope)
- [Vocabulary](#vocabulary)
- [The resolution ladder](#the-resolution-ladder)
- [Exemplar selection: never blend](#exemplar-selection-never-blend)
- [Labels and marking](#labels-and-marking)
- [The balance scale](#the-balance-scale)
- [Fictional power tiers](#fictional-power-tiers)
- [Domain rules](#domain-rules)
- [Anti-patterns](#anti-patterns)
- [Validation additions](#validation-additions)
- [Completion report additions](#completion-report-additions)

## Why These Items Are In Scope

Historical doctrine leaves a generic or fictional item uncategorized, which leaves
it on whatever the content pack authored. Authored values are not harmonized with
anything: a pack's `Generic Battleship` and its researched `Bismarck` sit on
unrelated scales, and a Star Wars blaster and a researched Kar98k do not compete on
one axis at all. Roughly a sixth of the outstanding scanner rows are generic or
fictional, and several bundled packs are almost entirely so.

The goal for these items is not historical truth, which does not exist for them.
It is that a player firing a generic 75 mm tank gun, a lasgun, and a researched
KwK 40 experiences one coherent world where the numbers mean the same thing. That
makes these categories **balance assignments constrained by real anchors**, closer
in spirit to `armor.md` than to `research-policy.md`.

Historical research still comes first. Most "generic" content is generic only in
its label.

## Vocabulary

| Term | Meaning |
| --- | --- |
| **Identifiable** | A real model can be established. Ordinary historical doctrine applies; nothing in this file is used. |
| **Consumer-identifiable** | The item's own label is generic, but the definitions that mount, fire, or carry it identify exactly one real model. Treat as identifiable. |
| **Representative** | The item stands for a real class whose members are all real, but no single member is the item. Values come from one selected real exemplar. |
| **Abstract generic** | The item stands for a class with no usable real membership, or a class so broad that no exemplar is defensible. Values come from an anchored tier. |
| **Fictional** | The item has no real counterpart at all. Values are balance assignments anchored to the real scale. |
| **Unresolved** | Identity cannot be established *and* no defensible class can be inferred. Left uncategorized and reported. |

Only the last row is a correct skip. "Generic" and "fictional" are no longer
reasons to skip on their own.

## The Resolution Ladder

Work down. Stop at the first step that succeeds, and record which step was used.

### R1 — Real identity from the item itself

Apply [research-policy.md](research-policy.md) identity rules unchanged: pack-local
`item.flansmod.<shortname>` localization first, then `Name`, `Model`,
`Description`, file name. A generic-sounding English name frequently hides an exact
model — `Large Bomb` in a WWII pack with a `Model` of `SC1000` is an SC 1000.

If this succeeds, the item joins or creates an ordinary historical category with no
marker. Nothing else in this file applies.

### R2 — Real identity from consumers

Enumerate every definition that references the short name: guns, AA guns, vehicles,
planes, gun boxes, loadouts, and other ammunition. Inspect the consumer's own
identity and its `Description`, which in several packs names the real weapon the
generic ammunition stands for.

If every consumer is one real model, or every consumer mounts the same real weapon,
the item *is* that weapon's ammunition and gets an ordinary historical category —
creating the historical category for the first time when needed. This is the single
most productive step, and it must be attempted before anything below it.

### R3 — Per-consumer overrides, with a representative baseline

Reached when the consumer set is real but plural and materially different. Example:
Warfare 44's `44_75APShell` is fired by the Sherman's M3, the Panzer IV's KwK 40,
the Cromwell's OQF 75 mm, and the Chi-Nu's Type 3 — four real guns, one item.

One shared item used to mean one value set, so the only option was to pick a
representative and accept that every other consumer fired the wrong round. That is
no longer true. The **firing weapon** can restate what the shared round does out of
its own barrel:

```text
AmmoMass              <ammoShortName> <grams>
AmmoMuzzleVelocity    <ammoShortName> <metresPerSecond>
AmmoExplosiveMass     <ammoShortName> <kgTntEquivalent>
AmmoPenetrationAt100m <ammoShortName> <millimetres>
AddRoundForAmmo       <ammoShortName> <name> <count> <massG> [explKg] [mps] [mm]
```

These are gun, AA-gun, and driveable category properties, so they live in
`gun_categories.json`, `aagun_categories.json`, `vehicle_categories.json`, and
`plane_categories.json` — never in `bullet_categories.json`. Each ammunition is
overridden individually, and resolution is against the weapon that actually fires:
a driveable's own bank uses the driveable's overrides, while the same driveable
firing through an `AddGun` / `PilotGun` mount uses that gun's.

So R3 has two halves, and both are done:

1. **Give every identifiable consumer its real round.** For each vehicle, aircraft,
   gun, or AA gun in the consumer set whose weapon can be identified, add the four
   override keys to *that consumer's* category with the exact researched values for
   the round it actually fired. This is ordinary historical research and the values
   carry no marker, because they describe a real gun firing a real shell.
2. **Give the shared item an honest baseline.** The ammunition item still needs a
   `(Generic)` category for consumers you did not cover and for the item viewed on
   its own. Select one real exemplar as described below, copy its complete coherent
   value set, and label it `(Generic)`.

Do not average, and do not mix values from different members into either half.

Prefer overriding on the consumer over splitting the ammunition item: splitting is
not available to a category anyway, since a category cannot create a new definition.

Before doing any of this, check whether the pack already separates the class into
several short names — Warfare 44 provides both `44_75APShell` and
`44_75APShellLong`. If it does, each short name gets its own baseline exemplar and
the split is respected rather than collapsed.

Report the two halves separately: overrides are researched historical values, the
baseline is a balance assignment.

### R4 — Anchored tier

Reached when there is no usable real membership: fictional content, or a class too
broad to exemplify (`Generic Plane`, `Generic Machine-Gun Ammo`, a Nerf dart).

Assign values from [the balance scale](#the-balance-scale) and, for fictional
content, [fictional power tiers](#fictional-power-tiers). Create a `(Generic)` or
`(Fictional)` category.

### R5 — Leave uncategorized

Only when the definition is so incoherent that even a class cannot be inferred: no
usable localization, no model, no description, no consumer, no calibre, no
recognizable role. Report it as unresolved with the fields that were inspected.

Joke and placeholder items are not automatically R5. A joke item that behaves like
a shotgun is an abstract-generic shotgun.

## Exemplar Selection: Never Blend

An exemplar is one real, already-researched or researchable configuration whose
values are copied as a set. Blending is forbidden for the same reason it is
forbidden for historical variants: mass, velocity, penetration, and filler are
physically coupled, and a mean of four rounds is a round that never existed and
whose numbers contradict each other.

Rank the consumer set on the domain's dominant balance axis and take the **median**
member; with an even count, take the lower of the two central members, which keeps
the generic option from being the strongest option.

| Domain | Rank by | Then copy |
| --- | --- | --- |
| Ammunition, shells | `PenetrationAt100m`, or `ExplosiveMass` for HE-only rounds | that round's whole `Mass` / `MuzzleVelocity` / `PenetrationAt100m` / `ExplosiveMass` set |
| Guns, AA guns | `RoundsPerMin` | that weapon's `RoundsPerMin`, `Dispersion`, `MuzzleVelocity` |
| Ground vehicles | `RealMassKg` | that vehicle's whole propulsion and armour set |
| Aircraft | `RealMaxSpeedKmh` | that aircraft's whole mass/power/speed/span/area/climb set |
| Grenades, bombs | `ExplosiveMass` | that item's `ExplosiveMass` and `FragType` |

Two adjustments are allowed after selection, and both must be reported:

- Round the exemplar's values to the precision the generic label deserves. A
  representative 75 mm AP round may carry `Mass: 6800` unchanged, but reporting it
  to four significant figures implies research that was not done for *this* item.
- Where the exemplar is a clear outlier against the rest of the set on a secondary
  axis, substitute the set's median on that one axis only, and say so.

If the consumer set has fewer than three real members, prefer the member that most
of the pack's own content uses, not the strongest one.

## Labels And Marking

Every non-historical category label ends with exactly one marker:

- `(Generic)` — R3 and R4 categories. Values are honest and coherent, but the
  category names a class, not one exact item.
- `(Fictional)` — R4 categories for content with no real counterpart.

The marker is a suffix so that existing ordering rules are untouched: a bullet label
still begins with its known metric calibre and still sorts among the real rounds it
is balanced against, which is exactly where a reviewer needs to see it. It also
makes the whole non-historical population greppable:

```bash
grep -c '(Generic)\|(Fictional)' src/main/resources/config/*_categories.json
```

Label the class, not the marker. Good labels:

```text
75mm Medium Tank AP (Generic)
20mm Aircraft Autocannon HE (Generic)
WWII Single-Engine Fighter (Generic)
WWII Medium Tank (Generic)
500 lb General-Purpose Bomb (Generic)
Standard Blaster Rifle (Fictional)
Heavy Walker (Fictional)
Foam Dart Blaster (Fictional)
```

Bad labels: `Generic Stuff (Generic)`, `75mm Pzgr.39 (Generic)` — the second claims
an exact round it is not, and would collide with the real category of that name.

Name the era, role, and calibre or class that the values actually describe. A
`(Generic)` label that is broader than its values is a lie about scope; one that is
narrower is a lie about identity.

## The Balance Scale

Every damage-relevant number in this mod resolves through two formulas, and both
are driven by projectile `Mass` in grams and velocity in m/s. Balancing a
non-historical item means placing it on that scale, not inventing a damage number.

```text
kinetic damage      = 0.005 * sqrt(massGrams) * velocityMps          (reference 5.0)
penetrating power   = 0.087 * cbrt(0.5 * massKg * velocityMps^2)
explosive damage    = 80 * cbrt(explosiveMassKg)
explosion radius    = 10 * cbrt(explosiveMassKg)
vehicle/AA gun HP   = 5 * realMassKg^(2/3)
```

The coefficients are the shipped defaults of `newDamageSystemDamageReference`,
`kineticPenetrationReference`, `newDamageSystemExplosiveDamageReference`,
`newDamageSystemExplosiveRadiusReference`, and `realisticVehicleHealthScale`.
Confirm them in `ModCommonConfig` before relying on the table below; a server may
retune them, but the *relative* ladder is what matters and it is scale-invariant.

### Calibration ladder

Derived from categories already in `bullet_categories.json` and their guns. Use it
to decide what tier a non-historical weapon should feel like, then work backwards.

| Reference round | Mass g | v m/s | Kinetic damage | Penetrating power |
| --- | --- | --- | --- | --- |
| 9x19mm Parabellum, pistol | 8.0 | 370 | 5.2 | 0.71 |
| 7.62x25mm Tokarev, SMG | 5.5 | 400 | 4.7 | 0.66 |
| 7.92x33mm Kurz | 8.1 | 685 | 9.7 | 1.08 |
| 5.56x45mm NATO | 4.0 | 945 | 9.5 | 1.06 |
| 7.62x39mm | 7.9 | 715 | 10.0 | 1.10 |
| 7.62x51mm NATO | 9.5 | 850 | 13.1 | 1.31 |
| 7.92x57mm Mauser | 12.8 | 760 | 13.6 | 1.35 |
| 12.7x99mm .50 BMG | 42.0 | 866 | 28.1 | 2.18 |
| 20x82mm MG 151/20 | 120.0 | 700 | 38.3 | 2.69 |
| 75mm Pzgr.39, KwK 40 | 6800 | 770 | 317 | 10.99 |
| 88mm Pzgr.39 | 10200 | 773 | 390 | 12.61 |
| 128mm PzGr. | 26400 | 930 | 756 | 19.59 |
| 356mm/45 AP Mark 16 | 680400 | 792 | 3267 | 51.99 |

### Energy and exotic ammunition

A blaster bolt has no mass and carries no bursting charge, so do not invent either.
Author its effect **directly** on the ammunition category:

| Property | Instead of | Note |
| --- | --- | --- |
| `Damage` | `Mass` | The kinetic system is only active when `Mass > 0`, so leaving mass out keeps the bolt off it entirely and makes this value authoritative. |
| `DamageVsLiving`, `DamageVsVehicles` | — | Optional. Both inherit from `Damage`; set one when the fiction makes the bolt markedly better or worse against armour. |
| `Explosion` | `ExplosiveMass` | A radius in blocks, for a bolt that bursts. The derived-blast system is only active when `ExplosiveMass > 0`, so omitting it leaves this radius in force. |
| `MuzzleVelocity` | — | Still authored. The projectile has to travel at some speed, and it drives the visible flight time. |

This applies to blaster bolts, laser and turbolaser pulses, plasma, disruptor beams,
and any other projectile that is not a physical object. It does **not** apply to
fictional solid shot — bolter rounds, gauss slugs, railgun darts, foam darts — which
are masses moving at a velocity and stay on the kinetic system like every real round.

Pick the damage from the calibration ladder above: read off the kinetic damage of the
real class the weapon plays the part of, and author that number. A standard-issue
blaster rifle meant to sit at intermediate-rifle tier is `Damage: 10`; a heavy
support weapon takes the 20 mm rung; a turbolaser takes a naval rung. Report every
such value as invented, because none of it is researched.

Set the gun category's own `Damage` to `1.0` so the ammunition stays authoritative:
the legacy path multiplies the two together.

Give energy ammunition a `FallSpeed` well below 1.0, or `0` when the fiction
genuinely shows a flat-trajectory beam, and report the choice.

## Fictional Power Tiers

Fictional content is anchored to the strongest *already-categorized real* item of
its own kind, per file. Compute the current ceiling rather than trusting a number
written here:

```bash
py -c "import json;d=json.load(open('src/main/resources/config/vehicle_categories.json'));print(max((v['properties'].get('RealMassKg',0),k) for k,v in d.items()))"
```

At the time of writing, the real ceilings are: vehicles 188 000 kg (Maus), 305 mm
front (T95), 105 km/h (Willys Jeep); aircraft 185 973 kg (B-36J), 2414 km/h (F-22A),
330 m/s climb (Su-57); ammunition 706 mm penetration and 88.11 kg TNT equivalent.
Vehicle medians are about 20 t, 40 km/h, 50 mm front; aircraft medians about 4720 kg,
540 km/h, 10.2 m/s.

| Tier | Setting | Anchor | Cap on the dominant stat |
| --- | --- | --- | --- |
| `F0` Mundane | Toys, civilian, sport, improvised, pre-industrial fantasy | The real item it imitates | At or below the real analogue |
| `F1` Grounded | Alternate history, post-apocalyptic scrap, near-contemporary fiction | The matching real era | At or below the era's real ceiling |
| `F2` Advanced | Plausible near-future extrapolation | The strongest real modern analogue | 1.25x that analogue |
| `F3` Exotic | Heavy science fiction and fantasy: energy weapons, walkers, super-heavies, capital craft | The file's strongest real category of that kind | 2x the file ceiling, never more |

`F3` is not a licence to make the fictional pack dominate. A Baneblade may be the
heaviest thing in `vehicle_categories.json`; it may not be ten times the Maus. When
the fiction insists on a scale the cap forbids, keep the cap and express the fiction
through role and behaviour — barrel count, reload, armour distribution — rather than
through a number that makes every real vehicle irrelevant.

Below the cap, place the item by its role within its own franchise, not by its
narrative reputation. Within one franchise the ordering must be internally
consistent: if a pack's elite trooper rifle and standard trooper rifle share one
tier, the pack loses its own texture, and if the standard rifle outranks the elite
one the pack is wrong.

Franchise consistency outranks cross-franchise realism. Balance a pack's contents
against each other first, then check that the pack's median sits near the median of
the file rather than at its top.

## Domain Rules

### Guns and AA guns

`RoundsPerMin` and `Dispersion` stay mandatory. For R3, copy the exemplar. For R4,
use these bands, which are the observed spread of the researched categories already
in `gun_categories.json`:

| Class | Dispersion deg | Typical RPM |
| --- | --- | --- |
| Precision / sniper rifle | 0.09 – 0.15 | 20 – 60 |
| Semiautomatic rifle, DMR | 0.14 – 0.22 | 220 – 320 |
| Assault rifle, automatic carbine | 0.18 – 0.26 | 550 – 850 |
| Light and medium machine gun | 0.25 – 0.35 | 450 – 1150 |
| Submachine gun, PDW | 0.34 – 0.45 | 600 – 1200 |
| Rotary / minigun | 0.40 – 0.50 | 2000 – 4000 |
| Pistol, revolver | 0.60 – 0.78 | 180 – 400 |
| Shotgun | 1.6 – 2.4 | 90 – 300 |
| Rocket / recoilless launcher | 2.0 – 2.2 | 5 – 15 |
| Autocannon, light AA | 0.20 – 0.40 | 120 – 900 |
| Tank and naval gun | 0.10 – 0.25 | 2 – 20 |

Fictional weapons take the band of the real class they play the part of: a blaster
rifle is an assault rifle, a bolter is a heavy automatic weapon, a turbolaser is a
naval gun. A fictional weapon may sit at the favourable end of its band; it does not
get its own band.

For a bow, crossbow, sling, thrown spear, or other pre-firearm launcher, use the
weapon's real historical draw velocity and projectile mass when the weapon is real,
and the `Rocket / recoilless launcher` dispersion band with a manual `RoundsPerMin`
otherwise.

### Melee weapons

A melee weapon defined as a gun takes `MeleeDamage` and nothing else. A gun that
fires no ammunition needs neither `Dispersion` nor `RoundsPerMin`: both describe
shooting, both are inert on a weapon that fires nothing, and a placeholder for them
only invents a number a later reader has to check. The mandatory-for-every-gun rule
applies to guns that shoot.

The `RoundsPerMin` column below therefore applies **only** to a weapon that both
swings and shoots. An item like `ermeysgarand` is a rifle with a bayonet: put it in
two categories at once, one carrying the melee statistics and one carrying the gun
statistics. A short name may belong to any number of categories as long as they do
not assign conflicting values for the same property.

`MeleeDamage` is authored content with no historical scale, so it is harmonized
rather than researched. The ladder is anchored on vanilla Minecraft swords — wooden
4, iron 6, diamond 7, netherite 8 — so a real blade lands where a player expects:

| Class | `MeleeDamage` | `RoundsPerMin` |
| --- | --- | --- |
| Standard, flag, book, joke or debug item | 2 | 60 |
| Shield bash, ancient or riot | 3 | 60 |
| Entrenching tool, screwdriver, improvised light | 4 | 90 |
| Knife, dagger, bayonet | 6 | 100 |
| Short sword, hand axe | 7 | 80 / 60 |
| Baseball bat | 7 | 70 |
| Sabre, katana, machete | 8 | 70 |
| Polearm: spear, pilum, javelin, lance | 9 | 50 |
| Sledgehammer | 9 | 40 |
| Energy blade | 20 | 80 |

Group by what the weapon physically is, not by the pack it came from: a gladius, a
spatha and a shamshir are one short-sword category. A firearm that merely has a
bayonet or butt-stroke is **not** a melee weapon — categorizing it as one would
overwrite its real cadence. Cap any authored value that is infinite or absurd at its
class tier and report the override.

### Ammunition

An R3 or R4 ammunition category obeys the ordinary ammunition rules in
[guns-ammunition-grenades.md](guns-ammunition-grenades.md) without exception:
`Mass` in grams, `FallSpeed: 1.0` for ballistic projectiles, `MuzzleVelocity` for
shells and missiles, and `PenetrationAt100m` / `ExplosiveMass` whenever the intended
value is nonzero.

Additional rules:

- A generic shell's `AddToAmmoGroup` normally names a **generic** family
  (`75mm Generic Medium Tank`), not a historical one. Attach it to a historical
  group only when the generic round is genuinely meant to be that family's stand-in
  and the real vehicles in that group should be able to load it.
- A generic mixed belt still needs a defensible `AddRound` composition. With no
  documented belt and a mixed consumer set, the `1 AP : 1 HE` default in the domain
  reference applies; report it as inferred.
- Magazine and clip items that only change capacity share the ammunition category of
  the round they hold. Nerf clips, drums, and belts are one category per projectile
  type, not one per capacity.
- Energy and exotic ammunition takes direct `Damage` and `Explosion` rather than
  `Mass` and `ExplosiveMass`, per the section above. Fictional solid shot keeps mass
  and stays on the kinetic system.

### Grenades and bombs

`ExplosiveMass` in kg TNT equivalent stays mandatory for anything with a charge.

For a generic grenade, select the exemplar by charge mass among the real grenades of
the same role, or use these tiers when no membership exists: offensive/concussion
0.15–0.20 kg with `LOW_FRAG`; standard fragmentation 0.06–0.09 kg with `STD_FRAG`;
modern pre-fragmented 0.18–0.21 kg with `HIGH_FRAG`; anti-tank shaped charge
0.7–1.0 kg with `THICK_CASE`; demolition charge 1–3 kg with `DEFAULT`.

For a fictional grenade, choose the real tier its role matches and stay inside it.
A thermal detonator is a demolition charge, not a tactical nuclear weapon, unless
the pack's own vehicles are scaled to survive one.

Bombs follow the dedicated bomb section of
[guns-ammunition-grenades.md](guns-ammunition-grenades.md), including its
charge-to-weight ratio method, which is itself the correct fallback for a generic
bomb whose only stated property is its nominal weight.

### Ground vehicles

All mandatory driveable keys stay mandatory: `RealMassKg`, exactly one engine key,
`DriveType`, `RealMaxSpeedKmh`, `RealMaxReverseSpeedKmh`, the applicable armour
faces, both track `PartArmorMm` entries where tracks are declared, and quoted
`ReadWeaponsFromGunTypes` and `UseRealisticVehicleHealth`.

For R4, keep the profile internally consistent. Mass drives health, and power-to-
weight drives how the vehicle actually behaves, so pick mass first and derive the
rest:

| Class | Mass kg | Power | Road km/h | Front mm |
| --- | --- | --- | --- | --- |
| Light unarmoured wheeled | 1 000 – 3 000 | 15 – 25 hp/t | 70 – 105 | 0 |
| Armoured car | 5 000 – 12 000 | 12 – 18 hp/t | 60 – 85 | 8 – 30 |
| Light tank | 8 000 – 16 000 | 12 – 20 hp/t | 40 – 60 | 15 – 45 |
| Medium tank | 18 000 – 35 000 | 10 – 16 hp/t | 35 – 48 | 45 – 90 |
| Heavy tank | 45 000 – 70 000 | 8 – 13 hp/t | 25 – 40 | 100 – 200 |
| Self-propelled gun, open | 15 000 – 30 000 | 10 – 16 hp/t | 35 – 48 | 15 – 60, no roof |
| Truck, utility | 2 500 – 9 000 | 10 – 20 hp/t | 60 – 85 | 0 |

Reverse speed is 10–25% of road speed for a tracked vehicle and 30–60% for a wheeled
one unless the exemplar says otherwise. Side is normally 40–60% of front, rear
30–50%, top and bottom 15–30%, tracks 15–25 mm. State explicitly when a face is a
known zero rather than unspecified.

Marine craft follow [ships.md](ships.md), which carries the naval exemplar rule and
the class bands. Generic naval classes are common in the runtime packs; select the
exemplar by full-load displacement among the real ships the pack already contains,
and never mix navies or decades inside one generic class.

### Aircraft

All mandatory aircraft keys stay mandatory. For R4:

| Class | Mass kg | Power | Max km/h | Climb m/s | Span m | Area m² |
| --- | --- | --- | --- | --- | --- | --- |
| Trainer, light civil | 550 – 1 200 | 65 – 200 hp | 140 – 250 | 3 – 5 | 10 – 11 | 15 – 17 |
| WWII single-engine fighter | 2 800 – 4 200 | 1 000 – 1 800 PS | 500 – 660 | 12 – 20 | 10 – 12 | 17 – 23 |
| WWII dive/torpedo bomber | 3 500 – 6 000 | 1 000 – 1 900 PS | 380 – 570 | 5 – 10 | 13 – 15 | 30 – 40 |
| WWII medium bomber | 8 000 – 15 000 | 2 x 1 200 – 1 700 PS | 400 – 550 | 5 – 8 | 18 – 22 | 45 – 60 |
| WWII heavy bomber | 25 000 – 35 000 | 4 x 1 200 PS | 350 – 460 | 4 – 6 | 30 – 43 | 120 – 180 |
| Early jet fighter | 4 000 – 7 000 | 8 – 25 kN | 800 – 1 000 | 20 – 40 | 10 – 12 | 20 – 25 |
| Modern jet fighter | 12 000 – 20 000 | 70 – 160 kN | 1 800 – 2 400 | 150 – 330 | 10 – 14 | 38 – 60 |

Use PS or hp for piston and turboprop craft and kN for jets; never both. A wingless
craft — rotorcraft, airship, hovering fictional craft — usually should not receive an
ordinary aircraft category at all; when the pack forces one, author a span and area
from the visible model's proportions and report both as modelled rather than
researched.

## Anti-Patterns

- Averaging four real rounds into a fifth that never existed.
- Copying the strongest consumer's values because the item "should feel powerful".
- Labelling a representative category with an exact designation it does not have.
- Filling an unknown armour face from the front face, which stays forbidden here.
- Giving a blaster bolt or laser pulse a `Mass` or an `ExplosiveMass`, which claims
  a physical projectile the fiction does not have.
- Leaving `Mass` off fictional *solid* shot and tuning `Damage` instead; a bolter
  round or a gauss slug belongs on the kinetic system.
- Authoring an energy round's `Damage` without reading it off the calibration ladder.
- Giving a fictional item a bespoke stat band because its franchise says it is
  special.
- Creating one category per capacity variant of the same generic magazine.
- Attaching a generic shell to a historical `AddToAmmoGroup` that real vehicles use,
  without deciding that they should be able to load it.
- Marking a category `(Generic)` when R1 or R2 would have identified it. Attempt
  both before every `(Generic)` category.

## Validation Additions

In addition to the checklist in
[format-and-validation.md](format-and-validation.md):

1. Every `(Generic)` or `(Fictional)` label carries exactly one marker, as a suffix,
   and still satisfies the file's ordering rule.
2. No `(Generic)` or `(Fictional)` label duplicates or shadows a historical label.
3. Every R3 category names its exemplar in the working notes, and its values match
   that exemplar's set rather than a blend.
4. Every energy round carries `Damage` and no `Mass` or `ExplosiveMass`, and its
   damage is traceable to a rung of the calibration ladder; every fictional solid
   round instead carries a mass whose implied kinetic damage lands on its tier.
5. Every fictional category is compared against the current file ceiling for its
   kind, recomputed at edit time, and sits within its tier's cap.
6. Every mandatory field required by the domain reference is present. A generic or
   fictional category is not exempt from any of them.
7. The scanner rows resolved at each ladder step are recorded, so that R3 and R4
   assignments can be revisited when better identification arrives.

## Completion Report Additions

Report, in addition to the standard items:

- counts per resolution step: R1, R2, R3, R4, and R5;
- for every R3 category, the exemplar chosen, the consumer set it was selected from,
  the ranking axis, and any single-axis substitution;
- for every R4 category, the tier assigned and the anchor it was measured against;
- every invented energy-round damage, with the ladder rung it was read from;
- every fictional category that landed at its tier cap, and what the fiction claimed
  instead;
- generic items that were resolved to real identities at R1 or R2, listed separately,
  since those are historical categories and not balance assignments;
- items left at R5, with the fields inspected.
