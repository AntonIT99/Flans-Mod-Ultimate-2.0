# Mecha Chassis Harmonization

Applies to `definitions/mechas` -> `mecha_categories.json`, including generic,
fictional, industrial and combat walkers. Every chassis is eligible. `mechaItems`
are tools/upgrades (`mecha_item`), not chassis; the chassis scanner excludes them.
Mounted guns and ammunition retain their own category doctrine.

## Scope and evidence

Chassis profiles are gameplay assignments, not historical performance
specifications or lore power scaling. Mass is different: prefer a documented real
or canonical mass when one exists, and otherwise author a defensible approximation.
Resolve identity using pack-local English localization, then `Name`, `Model`,
`Description`, filename if absent. Inspect the definition, geometry, engine
compatibility, tools and upgrades. Search all available packs for aliases. Use the
source ladder in [research-policy.md](research-policy.md) for real or published
fictional specifications.
Group by role and assigned profile; cosmetic variants share values. Split only for
balance-relevant differences, and keep geometry-specific health entries separate.
Use labels such as `Heavy combat walker chassis (Fictional)`; suffix `(Generic)`
or `(Fictional)` once when applicable. No real-mecha ceiling is required.

The following tables are initial normative balance targets, anchored to the
bundled Titan scale (Proto core/hips/arm HP 200/100/30; Alpha 400/200/60;
Zero 600/300/120). Mobility tradeoffs and reach are deliberate new balance choices,
not measured performance. With normalized health enabled, the HP cells are
reference per-part allocation ratios rather than an absolute total. Existing
positive authored HP may remain as the allocation weights when their distribution
is coherent. Revise tables coherently after playtesting rather than inventing
pack-specific exceptions or presenting these defaults as playtested.

## Chassis profile

Assign by represented role and construction, not existing inflated health, price,
franchise prestige or visual size alone. With no defensible role, use Utility and
report the uncertain assignment. Recon trades durability for mobility; heavy
chassis trade mobility for durability. A large industrial walker is not automatically
combat-armored. Do not mix the strongest cells from different rows.

| Profile | RealMaxSpeedKmh | RotateSpeed | StepHeight | JumpHeight | Reach | core HP | hips HP | each arm HP | head HP if present |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Utility / unarmored industrial | 15 | 15 | 1 | 0 | 3 | 200 | 100 | 30 | 30 |
| Recon / light combat | 22 | 20 | 1 | 2 | 3 | 200 | 100 | 30 | 30 |
| Assault / medium combat | 19 | 15 | 2 | 1 | 4 | 400 | 200 | 60 | 60 |
| Heavy / siege combat | 12 | 10 | 2 | 0 | 5 | 600 | 300 | 120 | 120 |

Write all movement/reach keys. Retain applicable positive authored part-health
entries unless their allocation is malformed or demonstrably incoherent.
`RealMaxSpeedKmh` is the chassis speed in km/h at engine and addon multipliers of
`1.0`; runtime speed is `RealMaxSpeedKmh / 72 * engineSpeed * addonMultiplier`
blocks/tick. A positive `RealMaxSpeedKmh` takes precedence for mecha movement.
Definitions without it retain the legacy dimensionless `MoveSpeed` path, whose
base speed is `4.3 * MoveSpeed` blocks/second before the same modifiers.
`RotateSpeed` is degrees/tick. `StepHeight` is an integer number of blocks.
`JumpHeight` is a nominal block-height parameter, not a guaranteed measured apex:
velocity is `sqrt(abs(9.81 * (JumpHeight + 0.2) / 200))`.
Zero still gives a small impulse; never claim it disables jumping and never use
negative values as a disable switch. `Reach` is dimensionless: it multiplies a
tool's reach in blocks, with effective reach clamped to 1..32 blocks. Test actual
engine/tool combinations, not just cells.

## Mass and durability without changing geometry

Every chassis requires a finite positive `RealMassKg` and quoted
`UseRealisticVehicleHealth: "true"`. Keep the opt-in in every category that authors
`RealMassKg`, rather than relying on membership in a separate profile category. Use
the whole represented chassis's ordinary operational mass, including integral
armour, equipment and normal fuel or power stores. Exclude carried cargo and
optional handheld guns, tools or addons. Keep one loading convention across
variants that share a category.

Choose mass evidence in this order:

1. A documented real mass for an animal, industrial machine or other real chassis,
   or a canonical published mass for a fictional chassis.
2. A documented sibling configuration adjusted for known differences in armour,
   equipment or scale.
3. A calculation from defensible dimensions, materials and occupied fraction, with
   each assumption recorded.
4. A gameplay-coherent estimate from the nearest comparable chassis or role when no
   stronger basis exists.

Approximate values are expected for sparse, generic and fictional content. Prefer a
rounded value that reflects the evidence's uncertainty; do not imply kilogram-level
precision from a broad estimate. Collision boxes are often simplified and may be
used as a scale cross-check, not automatically as physical outer dimensions. Do not
substitute a namesake object's mass when the pack depicts a differently scaled
avatar or adaptation. Report every approximate, calculated, sibling-derived or
disputed mass and its basis.

Normalized total HP is
`realisticVehicleHealthScale * RealMassKg^(2/3)`, rounded to the nearest integer.
Positive authored part HP determines only how that total is distributed. The
chassis table supplies reference ratios when a definition has missing, nonpositive
or incoherent weights. Inspect the resulting total and per-part values; mass is not
permission to mix profile rows or retain a broken allocation.

Mechas have no coupled real-world propulsion profile. Their independent
`RealMaxSpeedKmh` movement path does not require power, thrust, `DriveType`, reverse
speed, wings or rotors. Mass, normalized health and maximum movement speed operate
independently of the ground and aircraft profiles. Do not add millimetre armor or
resistance tiers without a separately justified penetration-balance design.

Do not override a `SetupPart` family solely to activate or tune normalized health;
`RealMassKg` controls total HP and the source definition's positive HP values already
act as allocation weights. Check the final entry for every part across the fixed
Setup* reader order, not merely the last line in the file. If an override is needed
to repair missing, nonpositive or incoherent weights, use the same `SetupPart` family
key as the source and preserve part name, all six geometry values, optional
penetration resistance and crew multiplier exactly. Array values contain
`part HP x y z width height depth [resistance] [crewMultiplier]`, without the key.
Keep negative geometry dimensions if authored. Do not create absent body parts or
copy another model's boxes. Additional part layouts need an explicit documented
mapping to these roles; if runtime support or mapping cannot be established, report
the chassis as unfinished, not fully harmonized.

Preserve models, textures, collision dimensions, arm origins, animation, seats,
recipes, sounds, cargo and inventory permissions. Categories are not a model repair
mechanism. Shared movement categories and per-model health categories may overlap
only on disjoint property keys; never let two categories assign `SetupPart` to the
same item even for different parts.

## Fall behavior and weapons

Every chassis writes quoted `TakeFallDamage: "true"`, `FallDamageMultiplier: 1.0`,
`FallDamageFactor: 1.0`, quoted `DamageBlocksFromFalling: "false"`,
`BlockDamageFromFalling: 1.0`, and quoted `SquashMobs: "false"`.
These defaults avoid free fall immunity and passive crushing while leaving addon
features meaningful. Addons can override fall immunity and landing block breaking;
verify those paths and server block-breaking restrictions in playtesting.

Set quoted `ReadWeaponsFromGunTypes: "true"` for mounted gun inheritance.
For fixed weapon banks, read only **Mounted weapon cadence** in
[vehicles-aircraft.md](vehicles-aircraft.md): a bank backed by a gun mount gets no
category cadence; a firing bank without one needs exactly one appropriate cadence.
Handheld guns retain their GunType, and mecha tools retain their MechaItemType.
Never rebalance a shared gun or addon silently as part of chassis coverage.
Record engine speed, addon speed/damage/reach modifiers and fall immunity when
assessing effective balance; unchanged equipment can overwhelm a balanced chassis.

## Validation and report

Use [format-and-validation.md](format-and-validation.md) for JSON and coverage.
Additionally verify:

- Every selected chassis has one documented profile, all mandatory scalar keys,
  finite positive `RealMassKg` and `RealMaxSpeedKmh`, explicit fall/weapon/health
  flags and all applicable part HP values.
- Resolved Setup* entries retain geometry, resistance and crew exposure; health
  scaling is enabled, positive authored HP acts as allocation weights, and no
  alternate Setup* key overrides the intended weights.
- Every mass has a documented loading convention and evidence or estimation basis;
  derived totals and per-part allocations are plausible for the assigned profile.
- No model, inventory, recipe, gun or addon identity has changed.
- Compare movement with identical engines/addons, turning, step clearance,
  jumping/landing, tool reach, part destruction and mounted/handheld firing.
  Include one representative per changed profile and exceptional part layout;
  verify upgrades still work and server/client behavior agrees.
- Rerun the scanner: remaining mecha rows are gaps, not fictional-content skips.
  A missing shipped `mecha_categories.json` means all chassis are queued; create
  that file when assigning the first real batch, not an empty coverage placeholder.

Report profile membership, newly covered names, mass values and bases, every mass
approximation or conversion, uncertain roles, geometry-specific splits,
engine/addon assumptions, outstanding mappings and checks not performed.
Distinguish static parser checks from actual gameplay validation.
