# Ground Vehicles and Aircraft

Read this reference when editing `vehicle_categories.json` or
`plane_categories.json`.

Marine craft are driveables too and every mandatory rule below still applies to
them, but displacement, knots, shaft power, belt and deck armour, and the
vehicle-versus-plane routing trap are covered separately in
[ships.md](ships.md). Read that as well for anything that floats.

Mandatory driveable fields follow completeness-over-omission. Work down the source
ladder from exact primary evidence through specialist and broad references to a
configuration-compatible game source. If every source tier fails, use a
gameplay-coherent estimate rather than omit a mandatory driveable key, and disclose
it explicitly. Never use that exception for ordinary optional fields.

For every ground-vehicle and aircraft category, set quoted
`ReadWeaponsFromGunTypes` and `UseRealisticVehicleHealth` to `"true"`. Confirm a
positive `RealMassKg` and inspect authored hitbox-health weights. Missing weights
cause a warning and fallback to authored health, which should be understood rather
than accidental. `ReadWeaponsFromGunTypes` keeps mounted delay, velocity, spread,
and damage derived from the researched gun definition instead of duplicating them
in each driveable, but only for a weapon bank that actually has a gun to read from.
Which cadence key a category needs follows from that, and is set out below.

## Mounted weapon cadence

This section applies to ground vehicles and aircraft alike; the keys live on
`DriveableType`.

A driveable has two weapon banks, primary and secondary, and each resolves its
firing cadence like this:

```text
ReadWeaponsFromGunTypes true AND this bank has a PilotGun/AddGun mount
    -> the mounted GunType's own RoundsPerMin
otherwise
    -> ShootDelayPrimarySeconds
     > RoundsPerMinPrimary
     > ShootDelayPrimary > ShellDelay > BombDelay      (legacy ticks)
     > 60 rounds per minute
```

with `ShootDelaySecondarySeconds`, `RoundsPerMinSecondary`,
`ShootDelaySecondary`, and `ShootDelay` as the secondary bank's equivalents.

The consequence that decides everything: **`ReadWeaponsFromGunTypes` only takes over
the cadence when that bank's shoot points include a real gun mount.** A bank made of
bare `ShootPointPrimary` lines firing shells or bombs straight out of the ammo bank
has no `GunType` at all, so it falls through to the driveable's own keys, and if
none is authored it silently sits at the default 60 rounds per minute — one shot
per second, for a Tiger's 8.8 cm as much as for a Sherman's 75 mm.

So decide per bank, by inspecting the definition's shoot points:

| The bank is | Author | Because |
| --- | --- | --- |
| A main gun firing shells, one round at a time | `ShootDelayPrimarySeconds` | Sources give tank and naval guns a *loading cycle in seconds*. Author the sustained rate a trained crew held, not a burst-of-three record. |
| An autocannon, machine gun, or rocket pod on a bare shoot point | `RoundsPerMinPrimary` | Sources give automatic weapons a cyclic rate. It is the same unit, and the same 1200 ceiling, as `RoundsPerMin` in `gun_categories.json`. |
| Backed by a `PilotGun` / `AddGun` mount | nothing | With `ReadWeaponsFromGunTypes: "true"` the cadence comes from that gun's own researched category. A driveable-level key here is dead: it is shadowed, and it will silently diverge from the gun category the day someone corrects the gun. |
| A bomb or torpedo release | `ShootDelaySecondarySeconds` | Release interval in seconds, matching how bomb-bay intervals are documented. |

Author exactly one cadence key per bank. Two are not an error, but the loser is
invisible, and a category that carries both is a future contradiction.

Both keys floor at one tick. `RoundsPerMinPrimary` therefore caps at 1200, exactly
as gun `RoundsPerMin` does, and `ShootDelayPrimarySeconds` cannot express anything
faster than 0.05 s. `ReloadTimePrimary` and `PlaceTimePrimary` are separate floors
applied on top of the resolved delay, not alternatives to it.

Passenger and turret mounts are outside all of this. A passenger gun always uses its
own `GunType` cadence regardless of `ReadWeaponsFromGunTypes` and of every key
above, so a hull with twenty passenger mounts needs no driveable cadence key for
them — only correct `gun_categories.json` entries.

## Ground vehicles

Use the exact mark and operating configuration. Prefer loaded/combat mass so mass,
engine, speed, armour, and armament describe one operational vehicle.

Every ground vehicle requires:

| Property | Unit | Requirement |
| --- | --- | --- |
| `RealMassKg` | kg, finite and > 0 | Loaded/combat mass; this is not legacy vehicle `Mass`. |
| exactly one of `RealEnginePowerKw`, `RealEnginePowerHp`, `RealEnginePowerPS`, or `RealEngineThrustKn` | source unit, finite and > 0 | Preserve the source's convention. `1 hp = 0.745699872 kW`; `1 PS = 0.73549875 kW`. Do not define aliases together. |
| `DriveType` | enum | `RWD`, `FWD`, `AWD`, `TRACKED`, or `MARINE`. Mandatory. A true ground vehicle takes one of the first four; a hull that floats takes `MARINE`, which is never inferred and must be authored. |
| `RealMaxSpeedKmh` | km/h, finite and > 0 | Governed road maximum for the represented setup, not an exceptional downhill value. |
| `RealMaxReverseSpeedKmh` | km/h, finite and > 0 | Gearbox-limited reverse speed; use a game fallback rather than omit it. |

The realistic propulsion profile activates only when mass, maximum speed, and one
engine-power value are valid. Never leave an accidental half-profile.

Additional properties:

- Cadence keys for a bank with no gun mount, as set out in
  [Mounted weapon cadence](#mounted-weapon-cadence): `ShootDelayPrimarySeconds` for a
  shell-firing main gun, `RoundsPerMinPrimary` for an autocannon or machine gun.
- `UseAmmoGroup`: exact group already created by shell categories. It is repeatable,
  so use an array for several cannon families. Validate both producers and
  consumers as described in the weapons reference.
- `RealDraftM`: metres, finite and > 0, for boats/floating vehicles whose
  definitions actually float. See [ships.md](ships.md), which also covers the naval
  unit aliases `RealDisplacementT`, `RealDisplacementLongTons`, `RealMaxSpeedKn`,
  and `RealMaxReverseSpeedKn`, and the naval armour keys.

### Armour

Use nominal rolled-homogeneous-equivalent thickness in millimetres, optionally
followed by slope in degrees:

```json
"ArmorFrontMm": "100 9"
```

The format is `<thicknessMm> [slopeDeg]`. Thickness is non-negative and slope is
0–89 degrees. Verify whether the historical source measures angle from vertical or
horizontal before converting. Preserve nominal plate thickness and authored slope
separately; do not replace thickness with calculated line-of-sight thickness.

Available hull keys are `ArmorFrontMm`, `ArmorRearMm`, `ArmorSideMm`, `ArmorTopMm`,
and `ArmorBottomMm`. Turret keys use the `Turret` prefix and also include an optional
`TurretArmorBottomMm` when the represented layout and definition need it.

Every clearly armoured vehicle requires hull `ArmorFrontMm`, `ArmorSideMm`,
`ArmorRearMm`, and `ArmorBottomMm`; an enclosed one also requires `ArmorTopMm`.
For an open-topped vehicle, omit the roof when that accurately represents the
definition, or use an explicit known zero when the schema needs the face. When
exact data remains unavailable after the source ladder, rear may reuse side and
top/bottom may reuse side or rear, but report copied faces.

Every armoured definition with a turret hitbox additionally requires
`TurretArmorFrontMm`, `TurretArmorSideMm`, `TurretArmorRearMm`, and
`TurretArmorTopMm`. Rear may reuse turret side and top may reuse turret side or rear
only as disclosed final fallbacks. A missing slope is acceptable when thickness is
known; a mandatory face is not.

Research faces separately; never fill an unknown face from frontal armour. Explicit
zero means known unarmoured, while omission means unspecified. `ArmorSideMm` applies
to both sides; split variants when asymmetric layouts cannot share one honest value.
For composite, spaced, or reactive protection, use published face-specific RHAe and
report that it is an equivalent rather than plate thickness.

`PartArmorMm` is repeatable and formatted `"<part> <thicknessMm>"`; it overrides
semantic armour for the named part and has no slope. Every tracked vehicle whose
definition declares `leftTrack` and `rightTrack` receives both entries using
running-gear/track thickness or a reasonable disclosed approximation. Verify exact
part names before adding them.

## Aircraft

Use the exact mark, engine, boost setting, propeller, installed equipment, and
loading condition. Prefer normal loaded/operational mass unless the category label
and evidence explicitly describe another condition.

Every aircraft category requires:

| Property | Unit | Requirement |
| --- | --- | --- |
| `RealMassKg` | kg, finite and > 0 | Loaded/operational mass consistent with performance figures. |
| `RealMaxSpeedKmh` | km/h, finite and > 0 | Representative rated maximum for the exact variant; not dive or structural limit speed. |
| `RealWingSpanM` | m, finite and > 0 | Physical span of the represented wing configuration. |
| `RealWingAreaM2` | m², finite and > 0 | Planform/reference area, not span squared or legacy `WingArea`. |
| exactly one of `RealEnginePowerKw`, `RealEnginePowerHp`, `RealEnginePowerPS`, or `RealEngineThrustKn` | source unit, finite and > 0 | Use shaft power for piston/turboprop craft and kN thrust for jets. Do not confuse kN, N, and kgf. |
| `RealClimbRateMs` | m/s, finite and > 0 | Sustained climb corresponding as closely as practical to selected mass and engine setting; not zoom climb. |

Aircraft categories do not use the ground-vehicle armour keys.

Aircraft cadence follows [Mounted weapon cadence](#mounted-weapon-cadence) unchanged.
Most fighters mount their guns as `PilotGun` entries and therefore need no cadence
key at all: `ReadWeaponsFromGunTypes: "true"` pulls the rate from the researched gun
category. Author `RoundsPerMinPrimary` only for a bare-shoot-point gun bank, and
`ShootDelaySecondarySeconds` for a bomb or torpedo release interval.

A genuinely wingless craft such as a rotorcraft, airship, or balloon is the only
case where span and wing area may be absent, and such a definition often should not
receive an ordinary aircraft category at all. Defining both shaft power and thrust
is technically allowed but normally signals mixed or unclear research; aircraft use
thrust when both are present.

The realistic profile activates only with valid mass, maximum speed, span, area,
and engine power or thrust. `RealClimbRateMs` is also mandatory for category
completeness even though profile activation does not require it.

Maximum speed is state-dependent: record a rated service maximum for the same
engine/boost/loading configuration and account for altitude and TAS versus IAS.
Do not use dive speed, structural limit speed, or an unqualified maximum from
another engine or boost setting. Use a sustained climb result, not an instantaneous
or zoom figure.

If a named sub-variant lacks separately documented performance, build it from the
nearest documented mark, adjust known differences, label the authored basis
honestly, and report substitutions. When War Thunder is the necessary fallback,
use its realistic rather than arcade column and disclose whether the figure is
stock/upgraded or otherwise game-mode dependent.
