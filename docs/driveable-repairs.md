# Driveable repairs

At the driver's controls, press **I** (rebindable as **Repair driveable**) with
throttle at or below 5% in either direction to start a repair session. Aircraft
must be on the ground or have a non-air block five blocks below their origin.
The default session lasts 200 ticks (10 seconds). Throttle and all driveable
weapons are locked during repairs; the action bar shows seconds remaining.
Repair sessions are transient and stop when the vehicle unloads.

Ordinary parts recover `1 + floor(maxHP * 0.005)` HP per cycle. Wheels, tracks
and engines recover `1 + floor(maxHP * 0.025)`. The first repair happens after
15 active ticks, then every 21 ticks. Destroyed parts must first be rebuilt
through the Repair menu or restored with a tool. Dependencies still apply.

The Repair menu consumes the part's required materials and restores ordinary
destroyed parts to `min(maxHP, max(1, floor(1 + maxHP / 115)))` HP. ERA, ERA2,
ERA3, APS and ADS parts return to full HP. Repair tools and the operator command
`/flansmodultimate repair [target]` remain available.

## Ships and carriers

An `EpicShip` uses its flare/countermeasure deployment as damage control instead
of the I-key session. At near-zero throttle it repairs surviving parts other
than buoyancy at the faster 2.5% rate. It cannot use throttle or weapons while
damage control is active. The cycle interval ranges from 21 ticks with full
crew to 567 ticks with no crew. Local and overall crew fractions both matter.
Buoyancy separately recovers 1 HP every 20–400 ticks, depending on overall crew.

Crew means actual occupants of configured, intact seats, including NPCs. A part
with no seats has a zero local crew fraction. This adapts Labjac's numeric crew
pools to this project's existing seat system; it does not add virtual crew or
reuse the hitbox's crew damage multiplier as a crew count.

Aircraft mounted on a driveable with `CanMountEntity true`, or resting on its
shaped collision hull at low throttle, receive carrier servicing: 1 HP per tick
plus the normal timed repairs at near-zero throttle. Nearby aircraft that are
not supported by the carrier do not qualify. Destroyed parts are not rebuilt.

## Content-pack settings and compatibility

- `canRepair true` enables timed repairs (default true). `NewRepairSystem` is
  accepted as Labjac's alias. These settings apply to ordinary vehicles and
  aircraft; ships use damage control and mechas retain their nanorepair upgrade.
- `RepairTime 200` sets the session duration in ticks, with a minimum of 1.
- `EpicShip`, `HasFlare`, `TimeFlareUsing`, `FlareDelay` and `CanMountEntity`
  retain their existing meanings.

Sessions and carrier servicing consume no materials or fuel. Mecha nanorepair
retains its fuel cost and creative-driver exemption. Labjac's external money
deduction and placeholder balance check are not ported: this project has no
corresponding economy integration. Clients and servers must both be updated
because the input protocol now includes the repair action.
