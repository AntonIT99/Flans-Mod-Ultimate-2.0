# Category Format and Validation

Read this reference before changing category JSON, scanner behavior, membership,
ordering, repeatable properties, or ammunition groups.

## Files and scanner

The built-in `*_categories.json` files under `src/main/resources/config/` are
shipped defaults copied at runtime to `config/flansmodultimate/default/`. Never edit
runtime copies under `run/`.

A category applies only to the definition type named by its file. Gun short names
belong in `gun_categories.json`; magazine, cartridge, bullet, shell, and missile
short names belong in `bullet_categories.json`; use the analogous grenade, vehicle,
and plane files for their types.

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
- Never put one short name in categories that assign conflicting values. When
  variants share most values, use an exact variant category or `exceptions` to
  exclude only the differing property. An `exceptions` map affects only the named
  property; another category may still supply it.
- Do not combine legacy and modern aliases for one concept in a category. Some
  readers deliberately prioritize one alias, and map iteration must not select
  between contradictory values.

## Ordering

Order category labels using case-insensitive alphanumeric order. For
`bullet_categories.json`, put known metric calibre first in the label. Sort labels
that begin with numeric millimetre calibre by:

1. numeric calibre;
2. numeric case length when present;
3. case-insensitive alphanumeric fallback.

Labels without a leading metric calibre use the alphanumeric fallback. For example,
128 mm and 150 mm sort after 88 mm, not between 12.7 mm and 20 mm.

## Validation checklist

After every category edit:

1. Strictly parse every changed JSON file.
2. Locate every added `items` short name in source definitions; confirm sanitized
   spelling, lowercase, category type, identity, aliases, and variant boundaries.
3. Run `scripts/scanShortnames.py` after coverage changes. Inspect all remaining
   relevant rows and classify them as generic, fictional, unsupported, ambiguous,
   or genuinely unresolved rather than assuming every row needs a category.
4. Validate ammo groups in both directions: every selectable shell has each intended
   `AddToAmmoGroup`, and every gun, AA gun, vehicle, or aircraft consumer has exact
   matching `UseAmmoGroup`. Check duplicate ammunition after combining groups.
5. For every `AddRound`, verify `RoundsPerItem > 1`, exactly six positional tokens,
   positive count, projectile mass, deliberate zero/nonzero filler and penetration,
   exact velocity, total repeating belt count, and absence of unintended accumulated
   belts. Confirm category-level `FallSpeed` unless sustained flight applies.
6. Check unit conversions explicitly: projectile kg/g, explosive filler/TNT
   equivalent, hp/PS/kW, mph/knots/km/h, feet/metres, seconds/ticks, and MOA/degrees.
7. Validate mandatory properties against the applicable domain reference:
   ammunition mass/gravity and shell/missile statistics; complete vehicle propulsion,
   armour/turret/track sets; complete aircraft mass/power/speed/span/area/climb; and
   quoted realistic-weapon/health flags for driveables.
8. Recheck configuration consistency and every value based only on a game, broad
   reference, conversion, approximation, neighboring face, or sibling variant.
   Preserve provenance for the final report.
9. Confirm category ordering and scan all category files for duplicate or conflicting
   assignments of affected short names.
10. Review the scoped diff and preserve unrelated worktree changes. Run focused
    parser tests if behavior changed, `git diff --check`, and a full Gradle build only
    when repository-level rules require it. Pure data changes require at minimum
    strict JSON parsing and the targeted checks above. Report checks not performed.

## Completion report

For a classification batch, report:

- number of new categories per JSON file;
- number of newly covered short names;
- identifiable historical items left unresolved and why;
- generic, fictional, unsupported, or otherwise intentionally skipped items;
- principal primary and specialist sources;
- values relying on broad/game/weak fallbacks or a neighboring configuration;
- conversions, RHAe values, inferred belts, copied armour faces, final-resort
  estimates, and disputed or approximate values deserving manual review.
