# Built-in Category Definitions

These rules apply to the built-in `*_categories.json` files in this directory and
supplement the repository-level `AGENTS.md`.

Before researching, adding, changing, sorting, or auditing a category, read
`.agents/skills/flans-category-research/SKILL.md` completely and follow its routing
to the relevant reference files. Those files contain the detailed property
semantics, units, source policy, workflows, and validation checklist. If guidance
conflicts, this `AGENTS.md` takes precedence.

`armor_categories.json` follows a different doctrine from every other file here.
Its categories are balance assignments derived from fixed tier tables, not
researched historical values, and they intentionally cover generic and fictional
items. Where a rule below is marked as not applying to armor, the armor rules in
`.agents/skills/flans-category-research/references/armor.md` govern instead.

Generic and fictional items in every other file are also in scope, under a third
doctrine: identity is resolved as far as the evidence allows, and whatever remains
gets a marked `(Generic)` or `(Fictional)` category whose values come from a
selected real exemplar or an anchored tier.
`.agents/skills/flans-category-research/references/generic-and-fictional.md` governs
that work, and every mandatory field below still applies to those categories.

## Non-negotiable scope and identity rules

- Edit these shipped defaults, never runtime copies under `run/config/`.
- Put each definition only in the category file for its supported type: guns,
  AA guns, bullets/ammunition/shells/missiles/bombs, grenades, ground vehicles,
  marine craft, aircraft, or armor.
- Match `items` to the sanitized content-pack `ShortName`, written lowercase for
  new entries. Search all bundled, official, and `run/flan` content packs for exact
  aliases and duplicate representations before deciding category membership.
- The pack-local `item.flansmod.<shortname>` value in
  `resources/assets/flansmod/lang/en_us.json` is authoritative for identity and
  title. Only when it is absent, fall back in order to `Name`, `Model`,
  `Description`, and file name. Weaker fields may clarify variant/configuration but
  never override localization.
- Add historical-stat categories only for identifiable real-world items, including
  documented prototypes or paper designs. A real sub-variant with sparse data is not
  automatically skippable; use the nearest defensible configuration and disclose
  the approximation as directed by the skill. This rule does not apply to armor,
  where every item is categorized.
- Purely fictional, fantasy, science-fiction, joke, gameplay-only, and generic items
  do not get historical categories, but they are not skipped either. Resolve identity
  from the item and then from its consumers first; a generic label very often names a
  real weapon. What genuinely remains gets a `(Generic)` or `(Fictional)` category
  built from one selected real exemplar or an anchored tier, never from a blend of
  several real items. Only an item whose identity and class are both unknowable is
  left uncategorized, and it must be reported.
- A `(Generic)` or `(Fictional)` marker is a suffix, appears exactly once, and never
  duplicates or shadows a historical label. Fictional values are capped against the
  strongest already-categorized real item of the same kind in the same file,
  recomputed at edit time. Fictional projectiles that are not solid shot declare an
  effective `Mass` so they stay inside the kinetic system; they are not balanced by
  omitting mass and tuning `Damage`.
- Category labels document the exact real item and configuration whose values were
  used. Split materially different variants; share a category only when the
  represented configuration and researched values genuinely match. This rule does
  not apply to armor, where a label names a pragmatic group of items that share a
  slot, set shape, coverage, protection class, and ballistic rating, and where
  splitting is justified only by a balance-relevant difference. A `(Generic)` or
  `(Fictional)` label instead names the class the values actually describe, and must
  never claim an exact designation the item does not have.

## Non-negotiable research and data rules

- Open actual sources; search-result snippets are not evidence. Prefer primary
  technical documents, then specialist historical sources, broad references,
  simulation/game databases, and finally weak discovery-only sources. Cross-check
  material ambiguity and keep all values in one category configuration-consistent.
- Do not invent values except for mandatory gameplay-critical driveable fields and
  the mandatory `RoundsPerMin` and `Dispersion` values of guns and AA guns after
  exhausting the documented fallback ladder. Less authoritative but
  configuration-compatible values are preferable to missing mandatory ammunition
  fields. A final gameplay-coherent gun or AA-gun value must fit its type, era,
  calibre, and configuration and be disclosed as invented in the final report.
  Generic and fictional categories are a separate case, not an exception to this
  rule: their values are not invented but assigned, either copied whole from a named
  real exemplar or read off the anchored tier tables in the skill's generic and
  fictional reference, and both the exemplar and the tier are reported.
- Preserve distinctions including projectile versus cartridge/magazine mass,
  explosive filler versus TNT equivalent, hp versus PS, loaded versus empty mass,
  barrel-dependent velocity, armour angle convention, penetration distance and
  obliquity, aircraft engine/boost/loading, and exact ammunition variant.
- Ammunition always has projectile `Mass` and normally `FallSpeed: 1.0`.
  Self-propelled projectiles capable of sustained flight are the `FallSpeed`
  exception. `AddRound` belts omit top-level `Mass` because every round supplies
  it. Shells and missiles require `MuzzleVelocity`, and require
  `PenetrationAt100m` and `ExplosiveMass` whenever the intended value is nonzero.
- Treat ammunition `MuzzleVelocity` / `BulletSpeed` as authoritative when present:
  it takes precedence over gun velocity. For simple/small-arms guns, define the
  barrel-specific velocity on the gun category as the normal source and fallback.
  For autocannon belts, shells, missiles, and other ammunition-authoritative
  patterns, define the exact round/gun velocity on the bullet category; a matching
  gun-category velocity may remain as a compatible fallback. Never assign
  contradictory configurations or silently rely on which side wins.
- Every grenade and every bomb that possesses an explosive charge requires
  `ExplosiveMass` in kg TNT equivalent. Do not omit it merely because no source
  states the TNT equivalent: derive it from documented charge mass and explosive
  composition when a defensible equivalence factor exists, or, for a bomb with no
  documented filler, from the charge-to-weight ratio bands in the skill's weapons
  reference. Omit it only when the item has no explosive charge.
- Bombs, depth charges, naval mines, and torpedoes are `bullet_categories.json`
  entries, not grenades. A bomb's `Mass` is the complete filled store in grams, its
  `FragType` comes from casing construction, and it takes no authored
  `MuzzleVelocity`. `Fuse` on a bullet-type entity is a maximum airborne lifetime,
  not a timed detonation delay: author it only for ordnance with a real timed
  function, and never as a bomb's arming delay.
- Guns and AA guns require `RoundsPerMin` and `Dispersion`. AA guns additionally
  require `RealMassKg` and quoted `UseRealisticVehicleHealth: "true"`. Exhaust
  research and source-compatible fallbacks first; if still unavailable, establish a
  gameplay-coherent value and report it as invented. Interpret AA multi-barrel
  cadence from the definition's `NumBarrels`, `FireAlternately`, and `NumBullets`;
  ammunition supplies projectile mass, muzzle velocity, filler, and penetration.
- Ground vehicles require mass, exactly one engine-power/thrust key, drive type,
  forward speed, and reverse speed. Armoured vehicles require the applicable hull
  and turret faces; tracked vehicles with declared track parts require both track
  `PartArmorMm` entries.
- Marine craft are ordinary driveables and take every driveable rule above, plus
  `DriveType MARINE`, full-load displacement, an astern speed, and `RealDraftM`.
  They use the naval armour keys (`ArmorBeltMm`, `ArmorDeckMm`, `ArmorCitadelMm`,
  `ArmorBulkheadMm`, `ArmorConningTowerMm`, `ArmorTorpedoBulgeMm`,
  `ArmorMachineryMm`, `ArmorSuperstructureMm`, `ArmorBowMm`, `ArmorSternMm`,
  `ArmorFlightDeckMm`) instead of the hull faces, and the existing turret keys for
  their main-battery mounts. A hull filed under `definitions/planes` belongs in
  `plane_categories.json` and cannot complete a propulsion profile; author its mass,
  armour and draft, omit the propulsion keys, and report it. `RealDisplacementT`,
  `RealDisplacementLongTons`, `RealMaxSpeedKn`, and `RealMaxReverseSpeedKn` are unit
  aliases for mass and speed: author the unit the source states and never both
  spellings of one quantity. Never copy a `SetupPart` penetration-resistance value
  into a millimetre armour key or the reverse.
- Aircraft require mass, exactly one engine-power/thrust key, maximum speed, climb
  rate, wing span, and wing area; only genuinely wingless craft omit wing fields.
- Ground-vehicle and aircraft categories always set quoted
  `ReadWeaponsFromGunTypes` and `UseRealisticVehicleHealth` to `"true"`.
- Weapon-bank cadence is authored per bank and only when that bank has no
  `PilotGun`/`AddGun` mount to read from, because `ReadWeaponsFromGunTypes` takes
  over the cadence only when such a mount exists and an unauthored bank silently
  defaults to 60 rounds per minute. A shell-firing main gun takes
  `ShootDelayPrimarySeconds` as a sustained loading cycle; an autocannon or machine
  gun takes `RoundsPerMinPrimary`; a bomb or torpedo release takes
  `ShootDelaySecondarySeconds`. Author exactly one cadence key per bank, and none at
  all for a bank backed by a gun mount or for passenger mounts, which always use
  their own `GunType`.
- Armor values are never researched, calculated, averaged, or invented. Research
  establishes only material and construction; every authored number is a lookup
  from the protection-class and ballistic-rating tables in `armor.md`, optionally
  multiplied by the item's coverage and rounded as specified there.
- Every armor category writes `Defence`, `BulletDefence`, `PenetrationResistance`,
  `ArmorPoints`, and `Toughness`, including zero values, so that harmonization is
  deterministic. `PenetrationResistance` is per-slot, never scaled by coverage, and
  never below the unarmoured value of its slot.
- One armor category covers exactly one armor slot. Never write `Type`, a model,
  texture, icon, colour, name, description, or recipe key into an armor category,
  and never write `Durability`, `KnockbackModifier`, or `KnockbackReduction`.

## Non-negotiable format and validation rules

- Preserve four-space indentation, blank lines, key spelling, and nearby numeric
  style. Do not reformat an entire category file incidentally.
- Order category labels case-insensitively and alphanumerically. In
  `bullet_categories.json`, labels with known metric calibre must begin with it;
  sort those by numeric calibre, numeric case length when present, then
  case-insensitive alphanumeric fallback. Thus 128 mm and 150 mm follow 88 mm.
- Use quoted strings for booleans and whitespace-separated legacy arguments. Use
  arrays for repeatable properties. Never assign one short name conflicting
  category values; use exact variant categories or property-specific `exceptions`.
- After edits: strictly parse every changed JSON file; verify each short name and
  definition type; validate mandatory fields and unit conversions; validate both
  sides of ammo groups and all `AddRound` fields; recompute every armor value
  against its tier table, coverage, and slot; recheck every generic and fictional
  category for its marker, its exemplar or tier, its recomputed effective masses,
  and its cap against the file's current real ceiling; rerun
  `scripts/scanShortnames.py` when coverage changed; review remaining
  rows by the resolution step they reached, treating any remaining armor row as a
  gap rather than a skip; confirm
  ordering and non-conflicting assignments; inspect the scoped diff;
  run `git diff --check`; and report any check that could not be performed.
- Update the sibling wiki when changing a supported property, format, enum, unit,
  or workflow. Ordinary data additions normally do not require a wiki change.
