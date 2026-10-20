# Composite armour category assignments

Research date: 2026-10-21. Target: `master`, Minecraft 1.20.1 / Forge.

This batch adds HEAT protection to nine existing vehicle categories covering
14 short names, with matching fallbacks in 11 tracked source definitions.
No categories or memberships were added. Runtime packs were inspected for
identity and aliases, but were not edited.

The user explicitly requested documented gameplay estimates when reliable
published HEAT equivalents were unavailable. Every value below is a total
effective protection in millimetres RHAe, not extra physical plate thickness.
These are shipped gameplay defaults, not verified military specifications.

## Authored values

| Category | Hull front | Hull sides | Turret front | Turret sides |
| --- | --- | --- | --- | --- |
| CATTB Abrams Test Bed | 1000 | unchanged | 1400 | 450 |
| FV4034 Challenger 2 | 900 | unchanged | 1300 | 500 |
| GTK Boxer | 120 | 100 | unchanged | unchanged |
| K2 Black Panther PIP | 1100 | unchanged | 1500 | 600 |
| Leopard 2A4 | 800 | unchanged | 1000 | 450 |
| Leopard 2A6 | 850 | unchanged | 1300 | 600 |
| Leopard 2A7+ PSO | 1100 | 650 | 1400 | 650 |
| M1 Abrams | 900 | unchanged | 1200 | 400 |
| T-90MS | 1200 | 400 | 1350 | 600 |

The properties are `ArmorFrontVsHeatMm`, `ArmorSideVsHeatMm`,
`TurretArmorFrontVsHeatMm`, and `TurretArmorSideVsHeatMm`, as applicable.
Unchanged faces retain their existing kinetic-equivalent protection against
HEAT. Roofs, bottoms, rears, tracks, and other part overrides receive no new
HEAT bonus. No slopes or kinetic armour values were changed or converted.

## Evidence and confidence

- Leopard 2A4 turret front: the [FAS Leopard 2 profile](https://man.fas.org/dod-101/sys/land/row/leopard2.htm)
  publishes 1000 mm against HEAT. This is a public assessment, not a disclosed
  production armour specification. Its 55.15 t configuration matches this category.
- M1 turret front, Challenger 2 turret front, and the Leopard 2A5 basis for the
  2A6 turret use published estimates collected in the
  [2013 armour comparison, notes 2, 16 and 21](https://www.pdf-archive.com/2013/08/10/tableau-des-blindages/tableau-des-blindages.pdf).
  Selected values are 1200, 1300 and 1300 mm respectively. That compilation
  draws partly on forum discussions; these are low-confidence public estimates.
  Its turret maxima are simplified into whole facing values by the current schema.
- The [War Thunder Challenger 2 development article](https://warthunder.com/en/news/6035-development-challenger-2-and-rank-vii-armored-vehicles-en)
  supports the greater-than-900 mm frontal turret protection class, but does not
  establish the selected 1300 mm estimate or the hull value.
- [War Thunder's Leopard 2A6 profile](https://wiki.warthunder.com/unit/germ_leopard_2a6)
  describes the 2A6 as closely following the 2A5 layout. Carrying the 2A5 turret
  estimate to the 2A6 is a disclosed sibling-configuration approximation.
- T-90MS turret front: 1350 mm is the upper end of the older T-90 assessment
  in note 4 of the same comparison. Applying it to the MS is a gameplay
  assignment, not a researched measurement of Relikt or its welded turret.

All remaining cells in the table are **invented gameplay estimates**, explicitly
authorized by the user, rather than numbers supplied by the sources above.
They were assigned per configuration and face instead of applying one common
multiplier:

- Hull fronts distinguish early Leopard arrays, later reinforced Leopard arrays,
  conventional Abrams/Challenger protection, and the proposed or upgraded tanks.
  Each is one broad face; glacis, lower plate, hatches, and weak spots are not
  independently represented by these category keys.
- Turret-side estimates represent weaker arrays than the frontal cheeks. They
  remain well below the frontal values and do not imply uniform real coverage.
- Leopard 2A7+ PSO receives an assigned reinforced front and side profile, rather
  than claiming a measured improvement over the 2A6.
- CATTB receives a modest increase over the configured Abrams front. The
  [Armored Warfare CATTB article](https://armoredwarfare.com/en/news/general/development-cattb)
  discusses advanced composite armour, but its quoted equivalents do not identify
  a threat channel clearly enough to copy them as HEAT resistance. The selected
  numbers are our assignments, not Armored Warfare statistics.
- K2 PIP uses a proposed-improvement profile above the early Leopard/Abrams hull
  assignments. [Hyundai Rotem](https://www.hyundai-rotem.co.kr/en/business/defense/details.do?productNm=K2%20Main%20Battle%20Tank)
  confirms composite and reactive armour on the K2 but publishes no face-specific
  RHAe. No numeric K2 game statistic was copied from an inaccessible wiki.
- Boxer gets limited hull-front/side improvement, substantially below MBT arrays
  and typical anti-tank warhead penetration. [KNDS](https://knds.com/en/products/boxer)
  confirms modular add-on protection, not these numeric equivalents. Its hull
  defaults apply to all five existing Boxer representations; the cannon and
  Skyranger turret modules get no separate turret-specific assignment. As before,
  faces without an authored turret plate inherit the hull defaults, including
  these new HEAT values; this remains a coarse category-level approximation.

Static HEAT thickness does not implement ERA consumption, tandem-charge
discrimination, slat-armour probability, or active protection. T-90MS values
approximate the configured protected facing with its package present throughout
the part's life. Do not describe this as a separate reactive-armour mechanic.

## Identity and coverage

Pack-local English localization was checked before weaker definition fields.
Official `abrams` is localized as the broad M1 Abrams family; its definition,
texture and 120 mm weapon configuration identify the M1A2-style representation
used here. The existing category label and unrelated statistics remain intact.

- Official Modern Warfare: `abrams`, `challyii`, `leo2a6`, `t90`.
- Manus Modern Warfare: `leopard2a6`, `leopard2a7`, `boxer1` through `boxer5`.
- Installed Rainfire Battle Squadrons: `forestedabrams`, `k2pip`, `leo2a4`.
  These three get category defaults only; the runtime ZIP remains untouched.

Aliases `leo2a6` and `leopard2a6` share one compatible category profile. Existing
Boxer armament categories remain separate and do not repeat the armour properties.
Earlier steel-armoured vehicles, ordinary aircraft, and ships receive no composite
upgrade from this batch. No identifiable modern composite tank in this inventory
is left without an assigned profile. Exact classified protection remains uncertain.

## Synchronization and validation

The new properties are scalar overrides. Matching source lines are safe
categories-disabled fallbacks; enabling categories selects the same final values.
Existing repeatable category-only properties were retained, with no geometry,
recipes, ammo sequences, comments, or legacy movement fallback changes.

Validation covers strict JSON parsing, category ordering, membership conflicts,
positive finite thicknesses with matching base plates, source/category agreement,
and preservation of all prior definition bytes after removing the added lines.
The definition-sync audit and packaging results are reported with task completion.
The shortname scanner found two unrelated pre-existing gaps: `waterwalkingboots`
(wearable armour) and `plumepack` (a credits bullet). This batch adds no membership
and does not alter those unrelated definitions.

The wiki already documents the parameter syntax and runtime semantics. This is a
data-only assignment batch, so no wiki format or behavior change is required.
