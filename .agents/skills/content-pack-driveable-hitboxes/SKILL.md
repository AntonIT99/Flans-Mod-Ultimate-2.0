---
name: content-pack-driveable-hitboxes
description: Audit and refine damageable driveable hitboxes and part HP weights for one tracked Flan content pack, with geometry and hull/turret armor alignment. Use for vehicles, planes, mechas, or all three; not parser changes, collision-mesh authoring, or general armor research.
---

# Content Pack Driveable Hitboxes

Take two inputs: a source content-pack name and a class filter (`vehicles`,
`planes`, `mechas`, or `all`). Ask for any missing or ambiguous input before
editing. Resolve the pack to exactly one tracked
`src/*/resources/flans_content/<pack>/` directory. `vehicles` includes ground
vehicles and boats; `planes` includes fixed-wing aircraft and helicopters.
Process only the selected definition folders and their referenced model sources.

## Purpose and authority

Make the damageable part boxes follow the rendered vehicle's meaningful physical
volumes. Correct clear errors, add missing parts, or divide an overbroad box when
the result improves geometry, damage, or armor behavior. Prefer a few accurate,
coherent boxes over many decorative ones. Allocate part HP approximately by the
represented mass and structural importance, using the pack's conventions; do not
infer HP from box volume alone.

Read the repository `AGENTS.md`, nested instructions, and
[references/geometry-and-damage.md](references/geometry-and-damage.md) before an
audit or edit. The reference records the current parser and runtime constraints
that materially affect box design. Check the locally available wiki for intended
behavior. If a model exists only as bytecode, follow
`../mod-class-decompilation/SKILL.md` for read-only reference evidence.

An audit or recommendation request is read-only. An apply, repair, or harmonize
request authorizes definition edits in the selected pack and class filter,
including armor keys directly needed for the new part layout. Establish missing
historical armor values from reliable vehicle-specific evidence; preserve existing
values unless evidence shows they are wrong. If a value cannot be justified,
report the gap rather than inventing one. Do not change model geometry,
parser/runtime code, built-in category defaults, or other packs merely to
simplify a box adjustment.

## Work on each definition

1. Inventory the definition, `Model`, `ModelScale`, all `Setup*Part` boxes,
   real-mass and health mode, armor keys, and every reference to affected parts
   (seats, guns, wheels/tracks, emitters, repair/death behavior). Resolve the
   actual model class and variants; compare box origins and extents with its
   final transformed geometry, including pivots and moving groups. If geometry
   is unavailable or ambiguous, keep the uncertain box and report it.
2. Record each proposed part's physical region, bounds, movement frame,
   destruction consequence, armor source, and HP share before editing. Tighten
   boxes that include large empty areas or miss substantial bodywork. Cover
   exposed major structures without turning thin details into misleading damage
   shields. Avoid broad overlaps that change which part an incoming shot meets.
3. Use one `Setup*Part` line per distinct supported part name: repeated lines
   overwrite rather than add boxes. Split a shape only into meaningful regions
   with distinct supported names and acceptable destruction behavior. Keep the
   `core` representative of the main hull/fuselage; its dimensions also feed
   vehicle geometry, physics, and entity sizing. Preserve optional resistance
   and crew multipliers unless a documented reason requires changing them.
4. For armored ground vehicles, prefer a coherent hull `core` and a separate
   `turret` only for a physically rotating turret. Face-aware `Armor*Mm` already
   distinguishes front, side, rear, top, and bottom, so a standalone
   `frontalArmor` region needs its own real geometry or gameplay purpose.
   Check that turret geometry actually receives `TurretArmor*Mm`; values alone
   do nothing without a turret-mounted part. Add supported turret armor values
   where they are missing and well evidenced. For a fixed casemate or upper hull,
   use hull armor alone when one body box is adequate; assess a distinct hull-fixed
   part when the upper structure needs its own damage region. Do not label it
   `turret` just to obtain turret armor: that also rotates its hitbox and
   armor-facing frame. When the available part/armor semantics cannot express
   the desired directional upper armor, report the limitation rather than
   inventing a false turret.
5. Rebalance HP as approximate weights for normalized-health definitions
   (`UseRealisticVehicleHealth true` with valid `RealMassKg`): positive authored
   part HP controls shares of the mass-derived total. Keep total mass and the
   health mode unchanged. For legacy-health definitions, authored HP is absolute;
   keep overall durability near the prior scale unless the user requested a
   balance change. Treat the `core` as the main hull and normally the largest
   individual HP share; a separate `frontalArmor` box represents only its own
   forward structure, not the mass or armor strength of the entire vehicle.
   Give a rotating turret a meaningful share based on vehicle-specific mass
   evidence where available, with its gun and modeled fittings considered.
   Allocate tracks, wheels, skirts, and other exposed modules by their represented
   structure and compare sister vehicles within the same pack. Do not equate
   armor thickness, box volume, or an existing HP value with mass. Check the
   gameplay effect of destroying each added or resized part.

## Finish

Review every scoped diff and run `git diff --check`. Validate parsed part names,
positive dimensions, unique part identities, armor resolution, HP shares, model
alignment, and references to removed parts. Run the relevant focused validation
and pack jar task required by the repository rules. Where practical, inspect the
result in game at neutral and moved poses, checking front/side/top hits, turret
yaw, part destruction, seats, and ground contact. Static checks alone cannot
prove visual fit; identify any in-game checks that remain.

Report changed and deferred definitions, the main geometry and HP decisions,
armor treatment of turreted and fixed-hull designs, validation performed, and
whether the wiki needed an update. Preserve unrelated work in a mixed worktree;
stage explicit paths only if staging was requested.
