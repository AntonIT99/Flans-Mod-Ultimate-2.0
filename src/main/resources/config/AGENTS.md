# Built-in Category Definitions

These rules apply to the built-in `*_categories.json` files in this directory and
supplement the repository-level `AGENTS.md`.

Before researching, adding, changing, sorting, or auditing a category, read
`.agents/skills/flans-category-research/SKILL.md` completely and follow its routing
to the relevant reference files. Those files contain the detailed property
semantics, units, source policy, workflows, and validation checklist. If guidance
conflicts, this `AGENTS.md` takes precedence.

## Non-negotiable scope and identity rules

- Edit these shipped defaults, never runtime copies under `run/config/`.
- Put each definition only in the category file for its supported type: guns,
  bullets/ammunition/shells/missiles, grenades, ground vehicles, or aircraft.
- Match `items` to the sanitized content-pack `ShortName`, written lowercase for
  new entries. Search all bundled, official, and `run/flan` content packs for exact
  aliases and duplicate representations before deciding category membership.
- The pack-local `item.flansmod.<shortname>` value in
  `resources/assets/flansmod/lang/en_us.json` is authoritative for identity and
  title. Only when it is absent, fall back in order to `Name`, `Model`,
  `Description`, and file name. Weaker fields may clarify variant/configuration but
  never override localization.
- Add historical-stat categories only for identifiable real-world items, including
  documented prototypes or paper designs. Leave purely fictional, fantasy,
  science-fiction, joke, gameplay-only, generic, and genuinely unidentifiable
  items uncategorized and report them. A real sub-variant with sparse data is not
  automatically skippable; use the nearest defensible configuration and disclose
  the approximation as directed by the skill.
- Category labels document the exact real item and configuration whose values were
  used. Split materially different variants; share a category only when the
  represented configuration and researched values genuinely match.

## Non-negotiable research and data rules

- Open actual sources; search-result snippets are not evidence. Prefer primary
  technical documents, then specialist historical sources, broad references,
  simulation/game databases, and finally weak discovery-only sources. Cross-check
  material ambiguity and keep all values in one category configuration-consistent.
- Do not invent values except for mandatory gameplay-critical driveable fields
  after exhausting the documented fallback ladder. Less authoritative but
  configuration-compatible values are preferable to missing mandatory ammunition
  fields. Disclose every game-sourced, converted, approximate, substituted, or
  disputed value in the final report.
- Preserve distinctions including projectile versus cartridge/magazine mass,
  explosive filler versus TNT equivalent, hp versus PS, loaded versus empty mass,
  barrel-dependent velocity, armour angle convention, penetration distance and
  obliquity, aircraft engine/boost/loading, and exact ammunition variant.
- Ammunition always has projectile `Mass` and normally `FallSpeed: 1.0`.
  Self-propelled projectiles capable of sustained flight are the `FallSpeed`
  exception. `AddRound` belts omit top-level `Mass` because every round supplies
  it. Shells and missiles require `MuzzleVelocity`, and require
  `PenetrationAt100m` and `ExplosiveMass` whenever the intended value is nonzero.
- Ground vehicles require mass, exactly one engine-power/thrust key, drive type,
  forward speed, and reverse speed. Armoured vehicles require the applicable hull
  and turret faces; tracked vehicles with declared track parts require both track
  `PartArmorMm` entries.
- Aircraft require mass, exactly one engine-power/thrust key, maximum speed, climb
  rate, wing span, and wing area; only genuinely wingless craft omit wing fields.
- Ground-vehicle and aircraft categories always set quoted
  `ReadWeaponsFromGunTypes` and `UseRealisticVehicleHealth` to `"true"`.

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
  sides of ammo groups and all `AddRound` fields; rerun
  `scripts/scanShortnames.py` when coverage changed; review intentionally missing
  rows; confirm ordering and non-conflicting assignments; inspect the scoped diff;
  run `git diff --check`; and report any check that could not be performed.
- Update the sibling wiki when changing a supported property, format, enum, unit,
  or workflow. Ordinary data additions normally do not require a wiki change.
