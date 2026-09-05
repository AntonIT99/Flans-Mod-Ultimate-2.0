# Ships and Marine Craft

Read this reference for anything that floats: warships, submarines, merchant hulls,
landing craft, patrol and torpedo boats, river gunboats, rowing boats, and sailing
vessels. Read it together with [vehicles-aircraft.md](vehicles-aircraft.md), whose
mandatory driveable rules still apply in full.

Ships are not a separate definition type. They are ordinary driveables carrying the
legacy `Boat` keyword, so they land in `vehicle_categories.json` or
`plane_categories.json` depending on which folder the pack put them in, and that
choice changes what they can be given. Roughly 139 outstanding scanner rows are
marine craft, most of them in the naval runtime packs.

## Contents

- [Routing: vehicle or plane](#routing-vehicle-or-plane)
- [Identifying a marine craft](#identifying-a-marine-craft)
- [Mandatory properties](#mandatory-properties)
- [Displacement](#displacement)
- [Speed and astern speed](#speed-and-astern-speed)
- [Power](#power)
- [Draft and drive type](#draft-and-drive-type)
- [Health](#health)
- [Armour](#armour)
- [Traps](#traps)
- [Craft that are not warships](#craft-that-are-not-warships)
- [Submarines](#submarines)
- [Weapons](#weapons)
- [Generic and fictional ships](#generic-and-fictional-ships)
- [Sanity checks](#sanity-checks)
- [Completion report additions](#completion-report-additions)

## Routing: Vehicle Or Plane

Check the folder before writing anything. A ship in `definitions/vehicles/` is a
`VehicleType` and belongs in `vehicle_categories.json`. A ship in
`definitions/planes/` is a `PlaneType` and belongs in `plane_categories.json`,
however little sense that makes — the bundled Manus ships pack defines the Prinz
Eugen as a plane with `Mode Plane` and `Boat True`.

That routing is not cosmetic. The physics resolver gates the coupled real-world
profile on the definition's category:

| Authored as | Realistic propulsion | Realistic health | Armour keys | `RealDraftM` |
| --- | --- | --- | --- | --- |
| `vehicles/` | yes, via the ground profile | yes | yes | yes |
| `planes/` | no — the aircraft profile would demand a wing span and area | yes | yes | yes |

So a ship authored as a plane still gets mass-derived health, armour, and draft, but
never gets `RealMaxSpeedKmh` propulsion; its speed stays on the legacy
`MaxThrottle` curve. Author the mass, armour, and draft anyway, omit the propulsion
keys rather than leaving a half-profile, and report the definition as a
misfiled hull so it can be moved later.

## Identifying A Marine Craft

The definition is a marine craft when it declares `Boat`, `FloatOnWater true`, or
`PlaceableOnWater True` with `PlaceableOnLand False`. Amphibious ground vehicles
also float, so the discriminator is whether the thing has wheels or tracks that
carry it on land; a DUKW is a ground vehicle that swims, not a ship.

Identity otherwise follows [research-policy.md](research-policy.md) unchanged. Naval
definitions are unusually generous: they routinely carry a `Description` line
holding the real displacement, length, beam, draft, speed, and machinery, because
the pack renders it as an in-game stat block. Treat that as a lead worth checking,
never as evidence — it is pack-authored text, it is frequently the *model's*
dimensions rather than the ship's, and the naval packs scale their hulls, so a
"Length 69 m" line on a 207 m battleship is describing the model.

Pin the exact ship **and the exact year**. Warships were rebuilt far more than tanks
were: displacement, armament, radar, and sometimes armour differ between a 1941 and
a 1945 fit of the same hull, and the packs name their variants accordingly
(`ussAlabama1945`, `NAV_Arizona1941`). One category is one ship in one refit.

## Mandatory Properties

A marine craft in `vehicle_categories.json` requires everything a ground vehicle
requires, minus the wheeled-vehicle assumptions:

| Property | Unit | Requirement |
| --- | --- | --- |
| `RealDisplacementT` or `RealDisplacementLongTons` or `RealMassKg` | tonnes / long tons / kg | Full-load displacement. See below. |
| exactly one engine key | kW / hp / PS | Total installed shaft power, all shafts. |
| `RealMaxSpeedKn` or `RealMaxSpeedKmh` | knots / km/h | Designed or trials maximum, clean bottom. |
| `RealMaxReverseSpeedKn` or `RealMaxReverseSpeedKmh` | knots / km/h | Full astern. |
| `DriveType` | enum | `MARINE`. |
| `RealDraftM` | m | Full-load mean draft. Only takes effect on a hull that actually floats. |
| `UseRealisticVehicleHealth` | quoted boolean | `"true"`. |
| `ReadWeaponsFromGunTypes` | quoted boolean | `"true"`. |

`RealDisplacementT`, `RealDisplacementLongTons`, `RealMaxSpeedKn`, and
`RealMaxReverseSpeedKn` are unit aliases, not separate quantities: they convert into
`RealMassKg` and `RealMaxSpeedKmh` and override them when both are present. Use the
unit the source states so no conversion is done by hand. Never author both spellings
of the same quantity in one category.

## Displacement

Naval sources publish several displacements for the same ship, and they are not
interchangeable:

- **Standard** (Washington Treaty): fully manned and stored, no fuel or reserve
  feed water. Always the smallest, and the one treaty-era references quote first.
- **Normal**: standard plus part of the fuel.
- **Full load** / **deep load**: everything aboard. Typically 10–25% above standard.
- **Light** / **empty**: rarely useful here.
- **Submerged** (submarines): larger than surfaced.

Use **full load**, because mass drives health and speed drives propulsion, and both
should describe one operational condition. A treaty battleship listed at 35,000 tons
standard is around 45,000 tons full load, and using the treaty figure understates
its health by about 18%.

Verify the unit. British and American records before metrication use **long tons**
(2240 lb, 1016.05 kg), not metric tonnes; German, Italian, and Japanese records use
metric tonnes. A 1.6% error is small on its own but the health curve raises it to
the two-thirds power alongside everything else, so declare it honestly with the
matching key rather than converting silently.

Submerged displacement belongs to a submarine's submerged category only if the pack
models the two states separately, which none currently do. Use surfaced full load
and report the choice.

## Speed And Astern Speed

Author knots directly with `RealMaxSpeedKn`. Prefer the trials or designed maximum
for the represented refit; a wartime ship with a fouled bottom and added topweight
made several knots less, and using a wartime service figure alongside a designed
power output describes two different ships.

Astern speed is rarely published. Steam and diesel warships develop roughly 40% of
ahead power astern, which gives about 55–65% of ahead speed for a hull whose
resistance rises with the cube of speed; in practice most warships are quoted around
one third to one half of full speed astern. Use a documented figure when one exists,
otherwise take **40% of ahead speed** and report it as derived. A twin-screw motor
launch or a paddle steamer can do materially better; a single-screw merchant does
worse.

## Power

Ships are rated in **shaft horsepower** (shp), which is mechanical horsepower, so it
belongs in `RealEnginePowerHp`. German and Japanese sources give metric horsepower
(PS or 馬力) and belong in `RealEnginePowerPS`. Modern gas-turbine ships are rated in
kW or MW.

Use **total installed power across all shafts**, not per-shaft, and use the same
rating condition as the speed: designed power with designed speed, or trials
overload power with the trials speed. Do not pair an overload trial power with a
service speed.

Sail has no engine key and therefore cannot complete the propulsion profile. Leave
a sailing vessel on legacy propulsion, author its mass, draft, and armour, and
report it.

## Draft And Drive Type

`RealDraftM` is the full-load **mean draft**: the depth of the hull below the
waterline, which the flotation model uses to settle the hull so its bottom sits that
far under the surface. Use mean draft, not the maximum draft over a sonar dome or a
propeller, and not the "draft over all". It replaces the legacy constant `Buoyancy`
lift; a hull with no draft keeps that constant exactly.

`RealDraftM` is dropped unless the definition actually floats, meaning
`FloatOnWater true` or the legacy `Boat` keyword. Confirm one of the two is present
before authoring it, because there is no warning when it is silently discarded.

`DriveType MARINE` is the marine propulsion layout: the launch ceiling is left
unscaled because a propeller is not traction limited the way a wheel is, and the
grade response is neutral. It is deliberately **never inferred** — `Boat` and
`FloatOnWater` are also carried by amphibious wheeled and tracked vehicles, so a
hull that wants it has to declare it. Give an amphibious ground vehicle its real
land layout instead: a Sd.Kfz. 2 Schwimmwagen is `AWD`, not `MARINE`.

## Health

Total hit points come from the shared curve, with the shipped scale of 5.0:

```text
totalHp = realisticVehicleHealthScale * massKg^(2/3) = 5 * massKg^(2/3)
```

The exponent matters more than the constant. Health scales with surface area rather
than volume, so a hull nineteen times heavier is only about seven times tougher.
That is the correct shape for a ship, because what actually separates a destroyer
from a battleship is armour, and armour is gated separately by penetration.

| Class | Full load t | Total HP |
| --- | --- | --- |
| Motor torpedo boat | 50 | 6 800 |
| River gunboat | 300 | 22 400 |
| Corvette, minesweeper | 1 000 | 50 000 |
| Fleet destroyer | 2 500 | 92 100 |
| Large destroyer | 3 800 | 121 800 |
| Light cruiser | 10 000 | 232 100 |
| Heavy cruiser | 17 000 | 330 600 |
| Battlecruiser | 35 000 | 535 000 |
| Fast battleship | 45 000 | 632 600 |
| Fleet carrier | 36 000 | 545 100 |
| Yamato | 72 800 | 871 700 |

For scale, the heaviest categorized land vehicle, the Maus at 188 t, has 16 400 HP,
and an M4A3 Sherman has 5 200.

### What actually sinks a ship

Total HP is not the sinking condition. Every naval part is a child of `core`, and
destroying `core` destroys the driveable outright, so a hull sinks when its **core
box** runs out of health, not when the sum of every compartment does. Destroying any
other part only removes that part, plus the effects the entity already models:
engine- and boiler-room losses cut available throttle by up to 80%, a destroyed
`steering` part removes yaw authority entirely, and a destroyed `bow` or `stern`
each cost a further 10% of throttle.

The realistic health system distributes the derived total across parts **in
proportion to the authored `SetupPart` health**, treating those numbers purely as
relative weights. So the authored weight given to `core` is what decides how long
the ship survives, and it varies wildly between packs — the USS Alabama gives `core`
about 13% of its total weight, which means roughly 85 000 of its 633 000 HP.

A category cannot fix that. `SetupPart` carries the part's collision geometry as
well as its health, so re-declaring it from a shared category would need each
definition's own box dimensions. Treat the core weight as a per-definition property:
check it, and when it is grossly out of line — under about 5% or over about 40% of
the total — say so in the report rather than compensating by distorting the
displacement.

## Armour

Warship armour is published per structure, not per hull facing, so ships use the
naval semantic keys rather than `ArmorFrontMm` and friends. Each covers every part
of one structure, and each accepts the same `<thicknessMm> [slopeDeg]` format:

| Key | Covers | Source figure |
| --- | --- | --- |
| `ArmorBeltMm` | `belt`, `port`, `starboard`, `leftsideArmor`, `rightsideArmor` | Main belt over the machinery and magazines, at its maximum thickness |
| `ArmorDeckMm` | `deck`, `deck2`, `deck3` | Main armour deck; add splinter and weather decks only if the definition has separate boxes for them |
| `ArmorCitadelMm` | `citadel` | Barbette or citadel side, usually the thickest plate on the ship |
| `ArmorBulkheadMm` | `bulkhead`, `bulkhead2` | Transverse armoured bulkheads closing the citadel |
| `ArmorConningTowerMm` | `conningTower`, `aftTower`, `bridge` | Conning tower side |
| `ArmorTorpedoBulgeMm` | `torpedoBulge` 1–4 | Torpedo bulkhead / holding bulkhead |
| `ArmorMachineryMm` | `engineRoom1`–`8`, `boilerRoom1`–`8`, `steering` | Machinery-space protection, usually the steering-gear box |
| `ArmorSuperstructureMm` | `superstructure` | Splinter plating, often nil |
| `ArmorBowMm`, `ArmorSternMm` | `bow`, `stern` | Unarmoured ends, usually an explicit `0` |
| `ArmorFlightDeckMm` | `flightDeck`, `flightDeck2`, `hangar`, `hangarDeck` 1–3 | Armoured flight deck on carriers that had one, otherwise `0` |

Turret armour uses the existing turret semantic keys. `turret1` through `turret16`
are all recognised as turret-mounted, so `TurretArmorFrontMm`, `TurretArmorSideMm`,
`TurretArmorRearMm`, and `TurretArmorTopMm` apply to every main-battery mount at
once — face, side, rear, and roof, exactly as naval sources publish them.

Resolution order is override, then turret semantic, then hull semantic. A naval key
resolves as a part override, so it beats the turret and hull keys; an explicit
`PartArmorMm <part>` line is more specific still and beats the naval key for that
one part. Use `PartArmorMm` only for a genuine exception, such as one turret that
was up-armoured.

### Getting the numbers right

- **Inclination**: give an inclined belt its real angle in the second field —
  `"ArmorBeltMm": "310 19"` for a 19-degree internal belt. Verify whether the source
  measures from the vertical, as naval references normally do, or from the
  horizontal.
- **Plate quality**: cemented (KC, Krupp, Class A) and homogeneous (STS, Class B, NVNC)
  plate are not equivalent at the same thickness, and the model has no quality axis.
  Use nominal thickness, and note in the report when a ship's protection is unusually
  strong or weak for its thickness.
- **Torpedo defence** is published as a resistance to a warhead in kilograms of TNT,
  not as millimetres. There is no supported way to express that. Author the holding
  bulkhead's actual plate thickness for `ArmorTorpedoBulgeMm`, and report that the
  system's rated capacity is not represented.
- **Deck versus belt** is decided by which collision box the projectile actually
  hits, so plunging fire that strikes the deck box gets deck armour without any
  extra modelling. Do not try to compensate by inflating deck values.
- **Unarmoured ends** are usually a known zero, not unknown. Author `0` explicitly so
  it is distinguishable from "not researched".

## Traps

- **`SetupPart`'s optional `[resistance]` field is not millimetres.** The line is
  `SetupPart <part> <health> <x> <y> <z> <width> <height> <depth> [resistance]
  [crewMultiplier]`, and `[resistance]` defaults to `5`. It is the collision box's
  abstract penetration resistance, the same currency as a projectile's penetrating
  power, where roughly one point is one unarmoured player. Naval packs frequently
  author millimetre-looking numbers there — the USS Alabama's belt carries `440` —
  which is a very large abstract budget, not 440 mm of steel. Physical armour comes
  only from the millimetre keys. Never copy a `SetupPart` resistance into an
  `Armor*Mm` key, or the reverse.
- **`leftTrack` and `rightTrack` on a ship.** Naval packs reuse the track parts as
  extra machinery compartments because the parts existed. They are not covered by
  `ArmorMachineryMm`, and a `PartArmorMm leftTrack` line on a ship means something
  entirely different from the same line on a tank. Inspect the box position before
  assuming.
- **`Boat` without `FloatOnWater`.** `RealDraftM` is dropped for a hull the resolver
  does not consider floating. The legacy `Boat` keyword sets `FloatOnWater`, but a
  definition that sets neither gets no draft, silently.
- **Displacement in the wrong ton.** See [Displacement](#displacement).
- **Per-shaft power.** Multiply by the number of shafts.
- **The model is not the ship.** Naval packs scale hulls down by a factor of two to
  three. Never take a dimension from the model, the collision boxes, or the pack's
  own description block.

## Craft That Are Not Warships

- **Merchant hulls, tugs, ferries, barges**: displacement is often published as gross
  register tonnage, which is a *volume* measure and must never be used as mass. Use
  deadweight plus light ship, or full-load displacement. Armour is an explicit `0`
  on every face.
- **Wooden sailing ships and rowing boats**: no engine key, so no propulsion profile.
  Mass is the vessel's displacement, still. Armour is `0`; timber scantlings are not
  armour and must not be authored as millimetres of steel.
- **Landing craft**: use loaded displacement including the embarked load the
  definition represents, and author the bow ramp and well as unarmoured unless the
  craft is a gun-support conversion.
- **Motor torpedo boats and patrol boats**: `0` armour on everything except the small
  plated wheelhouse some carried, high speed, high power-to-weight, tiny health. They
  are the naval equivalent of a soft-skin vehicle and should feel like one.

## Submarines

Only the surfaced state is modelled. Author surfaced full-load displacement, surfaced
speed, surfaced diesel power, and surfaced draft, so that one operational condition
is described throughout, and report that the submerged state is unrepresented.

Pressure hull thickness is engineering plate, not armour. Author `0` armour except
for a conning-tower value where one was genuinely armoured, and never convert a
diving depth into millimetres.

## Weapons

Naval guns are `guns` definitions and belong in `gun_categories.json` with
`RoundsPerMin` and `Dispersion` like any other gun; naval shells belong in
`bullet_categories.json` as individually selectable shells with `AddToAmmoGroup`.
Neither is a ship-category concern beyond the ammunition group.

`ReadWeaponsFromGunTypes: "true"` keeps a mounting's delay, velocity, spread, and
damage derived from the researched gun definition, which matters more for ships than
for anything else: one hull can mount twenty-odd guns across three or four calibres,
and duplicating their stats in the driveable would guarantee divergence.

Almost all of a warship's armament is passenger mounts, and a passenger gun always
takes its cadence from its own `GunType` regardless of any driveable key. So a ship
category normally authors no cadence key at all — the secondary battery, the AA
suite, and usually the main battery are all correct as soon as their
`gun_categories.json` entries are.

The exception is a main battery wired as a bare `ShootPointPrimary` bank firing
shells out of the shell slots, which several naval packs do. That bank has no
`GunType` to read, so it defaults to 60 rounds per minute — a battleship main gun
firing once a second. Give it `ShootDelayPrimarySeconds` with the turret's real
loading cycle: roughly 30 s for a WWII battleship main battery, 15 s for a heavy
cruiser, 4–6 s for a destroyer's semi-automatic mount. See
[Mounted weapon cadence](vehicles-aircraft.md#mounted-weapon-cadence).

Give the hull a `UseAmmoGroup` array naming each real gun family it actually carried,
and validate both directions. Do not attach a group merely because the calibre
matches — a 127 mm dual-purpose mount and a 128 mm tank gun are not one family.

## Generic And Fictional Ships

Generic naval classes are common in the runtime packs: `Generic Battleship`,
`Generic Allied Destroyer`, `Generic Heavy Cruiser`, `Generic Cargo Ship`. Handle
them through [generic-and-fictional.md](generic-and-fictional.md) with two naval
specifics:

- Rank the consumer or class set by **full-load displacement** and take the median
  member, then copy that ship's whole coherent set: displacement, power, speed,
  draft, and every armour value together. A generic battleship built from one real
  ship's numbers is coherent; one assembled from the best belt, the best speed, and
  the best deck of three different ships is not.
- Prefer an exemplar from the same navy and decade the label implies. `Generic Axis
  Destroyer` and `Generic Allied Destroyer` are different ships and must not share a
  category.

Fictional and fantasy hulls take the `F0`–`F3` tiers, anchored against the heaviest,
fastest, and best-protected *real* ship already categorized in the same file rather
than against land vehicles.

## Sanity Checks

Before finishing a naval batch, confirm the shape of the result:

1. Displacement, speed, and power describe one condition and one refit year.
2. Health lands within the class band in the table above. A destroyer at cruiser
   health means the displacement is wrong.
3. Belt thickness exceeds deck thickness for any ship built before about 1930, and
   the gap narrows or reverses for later designs built against plunging fire.
4. The ship's own main-battery `PenetrationAt100m` exceeds its own belt. A ship that
   cannot penetrate itself at point-blank range is usually a units error on one side
   or the other.
5. A destroyer's guns cannot penetrate a battleship's belt, and a battleship's guns
   overmatch a destroyer's `0` armour completely. That asymmetry is the point of the
   armour system; do not soften it with health.
6. Unarmoured ends, decks, and superstructure are explicit zeros rather than absent.

## Completion Report Additions

- displacement condition used (standard, normal, full load, submerged) and the ton
  unit, per ship;
- power rating condition and whether it is total or per-shaft in the source;
- derived astern speeds and the fraction used;
- ships misfiled as `planes/` definitions, with the propulsion keys that were
  therefore omitted;
- definitions whose authored `core` health weight is grossly out of band, with the
  fraction observed;
- torpedo-defence systems whose rated capacity could not be represented;
- armour faces authored as explicit zeros versus faces left unresearched;
- naval keys authored against parts the definition does not actually declare.
