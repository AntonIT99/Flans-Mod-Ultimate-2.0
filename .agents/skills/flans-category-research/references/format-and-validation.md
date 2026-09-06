# Category Format and Validation

Read this reference before changing category JSON, scanner behavior, membership,
ordering, repeatable properties, or ammunition groups.

## Files and scanner

The built-in `*_categories.json` files under `src/main/resources/config/` are
shipped defaults copied at runtime to `config/flansmodultimate/default/`. Never edit
runtime copies under `run/`.

A category applies only to the definition type named by its file. Gun short names
belong in `gun_categories.json`, AA-gun short names in `aagun_categories.json`, and
magazine, cartridge, bullet, shell, missile, bomb, depth-charge, mine, and torpedo
short names in `bullet_categories.json`; use the analogous grenade, vehicle, plane,
and armor files for their types. Bombs live in the `bullets` folder and belong in
`bullet_categories.json` despite resembling grenades. Ships have no file of their
own: they follow the folder the pack put them in, so a hull under `definitions/planes`
belongs in `plane_categories.json` and one under `definitions/vehicles` in
`vehicle_categories.json`. See [ships.md](ships.md) for what that choice changes.

Use `scripts/scanShortnames.py` to find supported short names absent from all
category JSON. It scans bundled source packs and `run/flan` ZIP packs and writes
`missing_shortnames.csv` at repository root. Its `full_name` should come from that
pack's `item.flansmod.<shortname>` English localization when present, falling back
to definition `Name`. Treat the CSV as an initial research queue, not proof that a
row qualifies or shares another category's statistics. Inspect every definition
and neighboring file.

Match category `items` to sanitized content-pack `ShortName`, never display name,
file name, or registry ID. Add short names in lowercase. Search all bundled,
official, and available runtime packs for aliases, skins, variants, and duplicate
representations before considering membership complete.

Consult the sibling `Flans-Mod-Ultimate-2.0.wiki` repository first for format and
parser behavior, especially `Category-System.md`, `ConfigReference.md`,
`Realistic-Vehicle-Physics.md`, and `Vehicle-Armour-and-Damage.md`. Confirm behavior
in the relevant type class before introducing a key not already used in category
files. Update the wiki when changing supported properties, formats, enums, units,
or workflows; ordinary data additions normally need no wiki update.

## JSON and application semantics

Use this shape:

```json
"Human-readable category name": {
    "properties": {
        "ConfigKey": 123
    },
    "items": [
        "definition_shortname"
    ]
}
```

- Preserve four-space indentation, blank lines between categories, key spelling,
  and nearby numeric style. Do not reformat the whole file.
- JSON numbers and strings become legacy config-line values. Use quoted strings for
  booleans (`"true"` / `"false"`) and whitespace-separated argument values; never
  raw JSON booleans in `properties`.
- Use arrays when repeatable properties need several lines, such as `AddRound`,
  `AddToAmmoGroup`, `UseAmmoGroup`, or `PartArmorMm`. Do not repeat the property
  name inside a value.
- Single-value properties normally use the last applied line and override the
  content definition. Repeatable properties accumulate. Confirm parser behavior
  before assuming how a new property applies.
- A short name normally appears in exactly one type file. The one sanctioned
  exception is a hull being migrated between `definitions/planes` and
  `definitions/vehicles`: register it in both, because only the file matching the
  installed definition's type ever applies. See [ships.md](ships.md).
- A short name may belong to **several categories of the same file** as long as no two
  of them set the same property. This is how a weapon with two distinct behaviours is
  authored: a bayonet rifle takes a melee category carrying `MeleeDamage` and a gun
  category carrying its ballistics.
- Never put one short name in categories that assign conflicting values. When
  variants share most values, use an exact variant category or `exceptions` to
  exclude only the differing property. An `exceptions` map affects only the named
  property; another category may still supply it.
- Do not combine legacy and modern aliases for one concept in a category. Some
  readers deliberately prioritize one alias, and map iteration must not select
  between contradictory values.

## Ordering

Order category labels using case-insensitive **natural** order: split each label into
runs of digits and runs of text, compare digit runs numerically and text runs
alphabetically, and rank a digit run before a text run at the same position.

Consequences, all of which the shipped files follow:

- `SU-85` precedes `SU-100`, and `Light Tank M5A1 Stuart` precedes
  `Light Tank M24 Chaffee`. Plain string order would reverse both.
- `Messerschmitt Bf 109 G-6` precedes `Messerschmitt Bf 109 G-10`.
- A label starting with a number leads the file and is ordered numerically among its
  peers, so `2 cm FlaK 38` and `3.7 cm FlaK M42` come before `5-inch/25 Mark 17`,
  and all of them before `AA-12`.

For `bullet_categories.json`, put known metric calibre first in the label and sort
those labels by:

1. numeric calibre;
2. numeric case length when present;
3. the natural order above as the fallback.

Labels without a leading metric calibre use the natural fallback. So 128 mm and
150 mm sort after 88 mm, not between 12.7 mm and 20 mm.

## Validation checklist

After every category edit:

1. Strictly parse every changed JSON file.
2. Locate every added `items` short name in source definitions; confirm sanitized
   spelling, lowercase, category type, identity, aliases, and variant boundaries.
3. Run `scripts/scanShortnames.py` after coverage changes. Inspect all remaining
   relevant rows and classify each one by the resolution step it stopped at, per
   [generic-and-fictional.md](generic-and-fictional.md). A generic or fictional row
   is not by itself a correct skip: only an item whose identity and class are both
   unknowable is. Armor rows are never a correct skip, since every one of them is
   expected to end up in a category.
4. Validate ammo groups in both directions: every selectable shell has each intended
   `AddToAmmoGroup`, and every gun, AA gun, vehicle, or aircraft consumer has exact
   matching `UseAmmoGroup`. Check duplicate ammunition after combining groups.
   For every `RemoveAmmo`, confirm the named short name really reaches that weapon,
   that removing it is a repair or a deliberate narrowing rather than a way of
   avoiding a per-ammo override, and that the weapon keeps at least one round.
   For every per-ammo override, confirm the named short name is the ammunition's own
   `ShortName` and that the ammunition is actually reachable from that weapon through
   `Ammo`, `AddAmmo`, or `UseAmmoGroup`; that the override lives on the definition
   that declares the ammunition, not on a driveable whose mounted gun declares it;
   and that an overridden round stays internally coherent rather than mixing one
   shell's velocity with another's mass.
5. For every `AddRound`, verify `RoundsPerItem > 1`, exactly six positional tokens,
   positive count, projectile mass, deliberate zero/nonzero filler and penetration,
   exact velocity, total repeating belt count, and absence of unintended accumulated
   belts. Confirm category-level `FallSpeed` unless sustained flight applies.
6. Check unit conversions explicitly: projectile kg/g, explosive filler/TNT
   equivalent, hp/PS/kW, mph/knots/km/h, metric tonnes versus imperial long tons,
   feet/metres, seconds/ticks, and MOA/degrees. Prefer the naval unit aliases over
   converting by hand, and never author both spellings of one quantity.
   Confirm muzzle-velocity precedence for every affected gun/AA-gun and ammunition
   pairing: ammunition `MuzzleVelocity` / `BulletSpeed` wins when present; gun
   velocity remains a compatible fallback. Autocannon belts, shells, and missiles
   require ammunition velocity.
7. Validate mandatory properties against the applicable domain reference: every
   categorized gun and AA gun that fires ammunition has nonzero `RoundsPerMin` and
   `Dispersion` (a melee-only gun needs neither), and every
   researched, game-sourced, or invented fallback is identified; AA-gun mass/health
   opt-in and multi-barrel cadence; ammunition mass/gravity and shell/missile
   statistics; every explosive
   grenade's and bomb's nonzero `ExplosiveMass` in kg TNT equivalent, with a bomb's
   `FragType` taken from casing construction, its `Mass` authored as the complete
   filled store, and any authored `Fuse` justified as a real timed function rather
   than an arming delay; complete vehicle
   propulsion, armour/turret/track sets; complete aircraft
   mass/power/speed/span/area/climb; quoted realistic-weapon/health flags for
   driveables; exactly one weapon-bank cadence key per bank that has no gun mount,
   and none on a bank that has one; marine craft additionally carrying `DriveType MARINE`, a full-load
   displacement, an astern speed, `RealDraftM`, and naval rather than hull armour
   keys; and the five mandatory armor properties with their slot, coverage,
   and tier-table checks from `armor.md`.
8. Recheck configuration consistency and every value based only on a game, broad
   reference, conversion, approximation, neighboring face, or sibling variant.
   Preserve provenance for the final report.
9. Confirm category ordering and scan all category files for conflicting assignments of
   affected short names: two categories may share an item, but not a property. For generic and fictional categories, also
   run the additional checks in
   [generic-and-fictional.md](generic-and-fictional.md): marker suffix, no collision
   with a historical label, exemplar traceability, energy-round damage read from the calibration ladder, and
   tier caps measured against the file's current real ceiling.
10. List every placeholder string authored under the blocked-source rule in
    `research-policy.md`, with its property, category, and source. A file that
    contains one is a work in progress and must be reported as such.
11. Review the scoped diff and preserve unrelated worktree changes. Run focused
    parser tests if behavior changed, `git diff --check`, and a full Gradle build only
    when repository-level rules require it. Pure data changes require at minimum
    strict JSON parsing and the targeted checks above. Report checks not performed.

## Completion report

For a classification batch, report:

- number of new categories per JSON file;
- number of newly covered short names;
- identifiable historical items left unresolved and why;
- generic and fictional items, broken down by the resolution step they reached, and
  the additional report items required by `generic-and-fictional.md`;
- unsupported or otherwise intentionally skipped items, excluding armor, where
  nothing is intentionally skipped;
- for armor batches, the additional report items required by `armor.md`;
- principal primary and specialist sources;
- values relying on broad/game/weak fallbacks or a neighboring configuration;
- conversions, RHAe values, inferred belts, copied armour faces, final-resort
  estimates, and disputed or approximate values deserving manual review;
- for naval batches, the additional report items required by `ships.md`.
