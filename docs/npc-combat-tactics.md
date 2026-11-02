# NPC combat movement

Ordinary Custom NPCs and Teams bots share the same survival and vehicle-combat
behaviour. These settings change decisions, positions and equipment choices;
they do not change health, armour, damage, accuracy or movement speed.

## Defaults and controls

In the NPC editor, open **Wolff's Mod > Tactics**. **Survival** enables cover
under fire, disengagement from protected vehicles, collision evasion and
opportunistic anti-tank flanking. These are enabled by default, including for
older NPCs whose saves do not contain the new switches. Explicit saved choices
are retained. **Equipment > Choose weapon for target** is enabled by default.

**Movement limits** defaults to Local manoeuvres within 24 blocks. The anchor
is the followed owner, the home of an NPC using Return to start, or where the
current engagement began. Hold forbids the additional displacement; Free
allows longer manoeuvres for independent NPCs. An explicit Hold tactic still
wins. Flee and avoid responses retain their own movement. Stationary weapons
do not acquire infantry movement from these settings.

Balanced is the general-purpose configuration: a 16-block withdrawal step and
3-block ally spacing. Cautious increases these to 24 and 4 blocks and disables
opportunistic flanking. Aggressive uses a 32-block local radius and a 12-block
withdrawal step, retaining cover and collision avoidance. Applying a profile
fills in behaviour settings only; individual controls remain editable.

Ordinary soldiers fall back below 30% health by default, with 0 disabling
health-based retreat. Retreat can interrupt an ongoing fight without clearing
the enemy target. The existing medic/home destinations remain available.
Threat memory defaults to 15 seconds and allied call-outs to 24 blocks.

## Decisions during a fight

Collision evasion takes priority over withdrawal, cover, combat tactics and
objectives. The NPC predicts a nearby moving hull's short path and seeks the
nearest side. It uses its normal walking and sprint permissions. It cannot
guarantee an escape when terrain, movement limits or vehicle speed leave none.

Recent attackers are remembered separately from the firing target. Cover is
checked from known firing positions against both head and chest, with a
preference for short routes and positions away from nearby allies. A soldier
that can fight steps out to fire and returns to cover while reloading. An
outmatched soldier seeks concealment or withdraws. Unreachable cover is retried
for a bounded time before another candidate is used. Hidden attackers are
represented by their last reported positions rather than tracked through walls.

Weapon selection considers actual carried weapons and ammunition, with finite
or infinite ammunition. Exchanges preserve the real items and cannot interrupt
a reload or bypass the current weapon's firing recovery. Protection estimates
use authored nominal plates and the round's penetration and warhead type;
missing protection remains unknown. Directly targeted occupants are not assumed
to be protected merely because their vehicle has armour.

Ground vehicles coordinate their main weapon choice with aiming and spacing.
Independent seat guns can search without a vehicle main target, while still
requiring an occupant or the existing unmanned-firing permission. Shared-turret
weapons retain their mechanical aiming constraints. The vehicle and driver
positioning controls apply to NPC models and real Flan vehicles respectively.

## Teams and diagnosis

The bot settings Rules page offers a Behaviour profile. **Use NPC settings**
is the default. A battle override applies temporarily through the shared
settings resolver; it does not overwrite a clone's saved settings. Existing
round suppression and retreat-health rules remain independent.

The NPC Diagnosis page reports the current safety movement: evade, retreat,
hide, peek, flank, spread or none. Search results are tied to their destination
so an old pursuit cannot become a withdrawal path. Ordinary decisions and cover
searches remain staggered; urgent collision checks wake distant NPCs.

## API 1.15

`IDriveableType.getPartArmor(part, outwardNormal)` returns an optional nominal
plate, including kinetic and HEAT thickness. The normal uses the semantic
part frame: front -Z, right +X and roof +Y. It excludes slope, impact angle,
distance and live NPC armour replacements. `getModelScale()` exposes the
definition scale for geometric inspection.

`FlansProjectiles.getArmorPerformance(ammunition, sequence)` returns nominal
penetration at 100 metres and whether the round uses the HEAT protection
channel. The sequence selects a round from a mixed belt. These read-only
queries are usable on either side after content loading; they neither launch
projectiles nor change the target or ammunition.
