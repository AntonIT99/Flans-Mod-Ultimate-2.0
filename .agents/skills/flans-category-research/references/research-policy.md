# Historical Research Policy

Read this reference for every task that identifies real items or authors historical
values.

## Identity and scope

Use the pack-local `item.flansmod.<shortname>` entry in
`resources/assets/flansmod/lang/en_us.json` as the authority on what an item
represents. Only if that key is absent, fall back in order to the definition's
`Name`, `Model`, `Description`, and file name. Weaker fields may confirm a variant,
armament, configuration, or era but never override localization. Conflicting weak
fields are not a reason to leave an otherwise localized item uncategorized.

Identify the exact model, mark, ammunition loading, engine, weapon mounting, and
relevant test condition before choosing values. A category label documents that
identity and configuration. Search every bundled and official source pack and the
available `run/flan` ZIP packs for exact aliases and variant representations.

Create historical categories for identifiable real items and documented prototypes,
paper designs, or credible reconstructions. Do not create them for generic labels
such as `rifle`, `machinegun`, `pistol`, `tank`, or `fighter` when no real model is
identifiable; purely fictional, fantasy, science-fiction, joke, or gameplay-only
items; or identities that remain genuinely unknowable after inspecting localization,
definitions, models, descriptions, neighboring files, and consumers.

This scope restriction does not apply to `armor_categories.json`. Armor categories
are balance assignments covering every armor item, generic and fictional included,
and they take no values from this policy at all. Use only the identity rules above
and the source ladder below, and only to establish an item's material and
construction. See [armor.md](armor.md).

Sparse data is not itself grounds for skipping a real item. For an undocumented
sub-variant, use the nearest documented sibling configuration, adjust properties
known to differ, make the category label honest about the basis where needed, and
report every carried-over figure. Modern items remain eligible. For composite,
spaced, or reactive armour, use published rolled-homogeneous-armour-equivalent
estimates and label them as RHAe in the report.

## Source ladder

Prefer evidence in this order. Source tier matters more than apparent decimal
precision.

1. Primary technical sources: original military manuals, firing tables,
   ammunition handbooks, acceptance trials, flight-test reports, manufacturer
   data sheets, and official ordnance documents. Scans and faithful reproductions
   are acceptable. Useful repositories include `archive.org`, `lonesentry.com`,
   `wwiiaircraftperformance.com`, and specialist sites reproducing primary records.
2. Specialist historical or technical references that distinguish variants,
   ammunition, and test conditions and preferably identify their own sources.
3. Broad references: museums, established databases, books, and Wikipedia. Use
   these for identification, dimensions, variants, and cross-checking; do not let
   them override better exact technical evidence.
4. Simulation/game databases such as War Thunder, IL-2, DCS, and GHPC. These may
   contain calculated, normalized, or balance-adjusted figures, so use them as
   fallback or cross-check sources. They are nevertheless preferred over omission
   for mandatory gameplay-critical values after better tiers are exhausted.
5. Forums, unsourced wikis, Reddit, search snippets, AI summaries, and copied
   specification lists. Use these to discover terminology or better sources, not
   normally as final evidence.

Open and inspect the actual source. Never use a search-result snippet as evidence.
When a primary source is difficult to interpret, do not substitute an unrelated
easy-to-read value. Search using the exact designation and technical terms such as
`technical manual`, `firing table`, `flight test`, `ammunition handbook`,
`penetration`, `projectile weight`, or `bursting charge`, including original-language
terms when useful.

## Subject-specific starting points

These are preferred starting points, not automatic authorities:

- Guns and AA guns: original weapon and mounting manuals; `modernfirearms.net` for
  actions, dimensions, barrel lengths, and cyclic rates; `forgottenweapons.com`
  for variant and mechanical identification; `navweaps.com` for autocannon, AA,
  naval, and larger-calibre weapons. For AA-gun mass, identify the complete
  represented mounting and its firing/operational configuration rather than using
  barrel or projectile weight.
- Ammunition: original firing tables and ordnance handbooks;
  `panzerworld.com` for documented WWII tank/anti-tank ammunition;
  `quarryhs.co.uk` for heavy machine-gun and aircraft/autocannon ammunition;
  `navweaps.com` for autocannon and larger projectiles; `bulletpicker.com` for US
  ammunition, construction, filler, and reproduced manuals.
- Grenades: original ammunition/EOD manuals, `bulletpicker.com`, `inert-ord.net`,
  and `cat-uxo.com` for identification and cross-checking.
- Ground vehicles: original manuals, trials, and official/manufacturer
  specifications; `panzerworld.com`, `tanks-encyclopedia.com`, and `tank-afv.com`.
  War Thunder is an expected fallback for armour layout, plate slope, reverse
  speed, ammunition, and engine data when historical tables are unavailable.
- Aircraft: original pilot notes, manufacturer reports, and military flight tests;
  `wwiiaircraftperformance.com`; `kurfurst.org` for applicable Bf 109/German
  documentation; `airvectors.net` and comparable specialist references. IL-2,
  DCS, and War Thunder remain fallbacks or sanity checks. If using a War Thunder
  performance table, use its realistic rather than arcade column and report that.

## Critical distinctions

Always verify the distinction relevant to the value:

- projectile mass versus complete cartridge, case, propellant, or magazine mass;
- explosive filler mass versus total projectile/grenade mass and actual filler
  mass versus TNT equivalent;
- muzzle velocity for the exact projectile/load and represented barrel length;
- cyclic rate versus practical or sustained rate;
- measured penetration versus calculated penetration, test criterion and plate
  quality, normal impact versus obliquity, and 100 metres versus 100 yards;
- combat/loaded versus empty vehicle or aircraft mass;
- gross versus net engine output and mechanical hp versus metric PS;
- nominal plate thickness versus line-of-sight thickness, source angle measured
  from vertical versus horizontal, base plate versus applique, and RHA thickness
  versus composite-protection RHAe;
- governed road speed versus theoretical/downhill speed and gearbox-limited reverse;
- exact aircraft mark, engine, boost/manifold pressure, fuel grade, propeller,
  equipment, loaded/test mass, altitude, TAS versus IAS, and sustained versus
  instantaneous/zoom climb.

Do not infer gun dispersion from effective range. For cannon penetration, prefer
the exact shell/gun combination and a normal-impact value. Never silently relabel a
100-yard value as 100 metres or an oblique result as normal-impact penetration.
When several penetration criteria exist, choose the one most consistent with
neighboring categories and explain the criterion in the final report.

## Conflicts, conversions, and completeness

When credible sources disagree, first verify variant, date, configuration,
ammunition, units, conventions, and test conditions such as altitude, temperature,
plate angle and quality, engine rating, ammunition lot, and loaded mass. Prefer the
primary source for the exact represented configuration; otherwise choose the more
specialized and better-documented source and cross-check material disagreements
independently. Do not average conflicting figures. A newer or more precise-looking
source is not automatically better.

Derive values only through deterministic, supported transformations. Valid examples
include kg to g, mph or knots to km/h, ft to m, seconds to Minecraft ticks, MOA to
degrees, hp/PS/kW when the original convention is known, and filler mass to TNT
equivalent when the charge mass and a defensible factor for that documented explosive
composition are available. In particular, an explosive grenade's `ExplosiveMass`
must be authored from an exact TNT-equivalent figure or this documented derivation;
continue researching the charge and composition rather than leaving it unset. Omit
it only when the grenade has no explosive charge. Preserve the source's unit when a
matching property exists—for example, prefer `RealEnginePowerPS` for a PS source.
Do not calculate missing historical penetration from calibre, kinetic energy, or a
game formula merely to fill a field.

Generally omit an unresolved optional value instead of inventing it. Fuse timing
remains optional when no defensible value exists. `RoundsPerMin` and `Dispersion`
are mandatory for every categorized gun and AA gun: continue down the source ladder,
using a configuration-compatible game value where necessary. If that fails, author a
final gameplay-coherent value based on the weapon's type, era, calibre, action or
mounting, barrel configuration, and neighboring categories. Never present that value
as historically verified; identify it as invented in the final report. Mandatory
ammunition, explosive-grenade, and driveable values otherwise follow their domain
reference. Only mandatory driveable values may also use a final gameplay-coherent
estimate after every source tier fails, with the same explicit disclosure.

For War Thunder ammunition fallback, open the carrying vehicle's page, follow
**Armaments** to the gun and **Available ammunition**, then open the exact munition.
Use projectile mass in kg, muzzle velocity in m/s, and TNT equivalent in g; convert
projectile kg to grams and TNT-equivalent grams to kilograms. Confirm the exact
munition and vehicle/gun configuration.

## Traceability

Keep working notes sufficient to audit each category. The final report must identify
principal sources and separately call out game-sourced values, unit conversions,
TNT-equivalent conversions, copied sibling/configuration values, disputed or
approximate figures, RHAe, inferred belt compositions, and invented final-resort
driveable estimates. Do not add comments or citation metadata to category JSON
unless the schema is intentionally extended.
