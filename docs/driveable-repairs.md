# Driveable repairs

Press **I** at the driver's controls at no more than 5% throttle in either
direction. Vehicles and landed aircraft run a 10-second session; ships run a
separate 40-second damage-control session. Throttle and all driveable weapons
are locked during the session. The action bar shows seconds remaining.
Countermeasures are independent of repairs. Sessions stop on unloading.

Ordinary parts recover 1.5% of maximum HP per second (15% per 10 seconds).
Wheels, tracks and engines recover 2.5% per second. Each pulse restores at
least 0.25 HP. All repairable parts heal simultaneously. A damaged part must
go five seconds without further damage before automatic repairs resume.
Damage restarts its repair clock rather than banking an immediate pulse.

Destroyed parts must first be rebuilt through the Repair menu or restored with
a tool. Rebuilding consumes required materials and restores 10% of maximum HP,
minimum 1 HP capped at maximum HP. ERA, ERA2, ERA3, APS and ADS return to full
HP. Dependencies still apply.

## Ships and carriers

Ship damage control restores 2.5% per pulse at intervals of 1-3 seconds.
Local crew contributes 25% of the crew factor and overall crew 75%.
Compartments without seats use overall crew availability for both factors.
Crew means actual occupants of configured, intact seats, including NPCs.

Buoyancy automatically recovers 1/60 of maximum HP per pulse, minimum 0.25 HP.
Intervals range from one second with full crew to three seconds without crew:
approximately 60-180 seconds for an entire buoyancy HP pool. The five-second
delay after damage also applies.

Aircraft mounted on a driveable with CanMountEntity true, or resting on its
shaped collision hull at low throttle, receive carrier servicing: 1.5% of each
surviving part's maximum HP per second, minimum 0.25 HP. It does not stack with
field repairs. Nearby unsupported aircraft do not qualify.

## Tools and settings

Repair tools work once per second on one part at a time. Each use restores
at most 5% of the part's maximum HP (minimum cap 1 HP), without exceeding the
tool's configured healing amount. Manual tools can work during the automatic
repair delay. The official Blowtorch heals up to 100 HP per use and has 200
uses before recharging; its recharge recipe remains three coal.

- canRepair true enables timed sessions (default true); NewRepairSystem is
  its legacy alias.
- RepairTime 200 sets vehicle/aircraft session duration in ticks.
- ShipRepairTime 800 sets ship damage-control duration in ticks.
- Both durations have a minimum of one tick.

Sessions and carrier servicing consume no materials or fuel. Mecha nanorepair
retains its rates, fuel cost and creative-driver exemption. The operator command
/flansmodultimate repair [target] bypasses repair delays and restores all parts.
External economy charges are not implemented.

## Flan vehicle NPCs

The NPC module uses the same part-HP rates and five-second damage delay for
NPCs wearing vehicle, aircraft, ship or mecha models. Entity maximum health does
not multiply repair strength. Repaired part HP is converted back into the NPC's
health, rather than applying the tool amount as ordinary healing.

With Part health and Automatic repairs enabled, a stationary NPC without a
combat target starts a repair session when its total part health is below 75%.
Aircraft must be landed or mounted; flying aircraft are not teleported to a
carrier. A session stops navigation, movement, melee, carried gun and grenade
attacks, driver guns and crew guns. Further damage delays healing without
removing the movement/fire lock. These sessions are transient; death, respawn,
model changes and unloading clear them. Disable Automatic repairs to stop NPC
maintenance without disabling manual tools. Healing rebuilds parts retains its
original meaning: when disabled, healing and tools repair surviving parts but
do not rebuild destroyed parts.

Engineers in bot battles must carry a working repair tool. Each successful use
consumes its real durability, uses the common 5% per-part cap and observes a
one-second minimum cooldown. Their existing duty cadence can make them slower.
They can restore destroyed parts only while their dependency parts are intact.
The class's nominal repair amount no longer grants free whole-NPC healing.

Player Blowtorches target NPC part surfaces, including destroyed parts. Carrier
servicing works for supported aircraft NPCs on real or NPC carrier hulls with
CanMountEntity enabled. NPC ships count actual occupants of their configured
seats and use the shared compartment flotation/sinking rules, so buoyancy
recovery affects their speed and ability to stay afloat. Part effects controls
these naval movement penalties.

A mecha NPC carrying a Nanorepair upgrade and Flan fuel cans in its equipment or
nine inventory/drop slots repairs surviving parts at the upgrade's rate once
per second. Each repaired part consumes 10 units from a can that has at least
10 units remaining. The upgrade is not consumed. No fuel means no nanorepair;
NPCs do not receive a creative-player fuel exemption.

Commands:

- /flansmodultimate repair [target] also fully restores a surviving vehicle NPC;
  operator level 2 is required, and dead NPCs are not revived.
- /flansmodultimate repair start <target> orders a stationary vehicle NPC to run
  a timed session; operator level 2 is required.
- /flansmodultimate rebuild <target> <part> rebuilds a destroyed NPC part at the
  normal material cost and 10% HP (defensive ERA/APS/ADS parts return at full HP).
  Any player can use this within eight blocks of a repairable hull surface with
  the required materials. It does not bypass part dependencies.
