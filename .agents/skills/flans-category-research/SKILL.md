---
name: flans-category-research
description: Research, add, sort, or audit built-in categories for Flan's Mod Ultimate guns, AA guns, ammunition, grenades, ground vehicles, aircraft, and armor. Use for *_categories.json work, missing_shortnames.csv triage, historical-stat validation, armor balance harmonization, aliases, ammunition groups, and category coverage.
---

# Flans Category Research

Maintain the built-in category JSON files as auditable defaults. The nearest
applicable `AGENTS.md` is authoritative and must be read before this workflow.

Two distinct kinds of work live here:

- **Historical categories** for guns, AA guns, ammunition, grenades, ground
  vehicles, and aircraft. Values are researched from real-world sources, and
  fictional or unidentifiable content is left uncategorized.
- **Armor categories**, which are balance assignments rather than research
  products. Every armor item is categorized, including generic and fictional ones,
  and every value is a lookup from the fixed tables in the armor reference. Do not
  apply the historical-only scope rules to armor.

## Load only the references needed

Always read:

- [research-policy.md](references/research-policy.md) before identifying items or
  researching real-world values.
- [format-and-validation.md](references/format-and-validation.md) before changing
  JSON, scanner behavior, category membership, ordering, or ammo groups.

Then read only the applicable domain reference:

- [guns-ammunition-grenades.md](references/guns-ammunition-grenades.md) for guns,
  AA guns, magazines, bullets, cannon shells, missiles, mixed belts, or grenades.
- [vehicles-aircraft.md](references/vehicles-aircraft.md) for ground vehicles,
  boats represented in the vehicle system, or aircraft.
- [armor.md](references/armor.md) for helmets, uniforms, vests, plates, droid and
  creature shells, and every other `armorFiles` definition.

Read several domain references only when the task spans several domains. Do not
rely on a summary here in place of the routed references.

For armor work, `research-policy.md` is still read, but only its identity rules and
its source ladder apply, and only for establishing an item's material and
construction. Armor values are never researched.

## Standard workflow

1. Check the current branch and `gradle.properties`. Edit the shipped files under
   `src/main/resources/config/`, never copied runtime defaults.
2. For coverage work, run `scripts/scanShortnames.py` and treat
   `missing_shortnames.csv` as a research queue, not as a list to categorize
   mechanically. When prioritization is requested, process official/bundled source
   packs before `run/flan` ZIP packs.
3. Inspect each actual definition, its pack-local English localization, neighboring
   definitions, model/description fields, and every consumer or weapon mounting
   needed to establish identity and configuration.
4. Search all bundled, official, and available `run/flan` packs for aliases and
   duplicate representations of the exact item. Decide whether they share the same
   configuration or require separate categories.
5. Research the relevant properties under the source ladder. Open the evidence,
   verify units and test conditions, and cross-check conflicts or surprising values.
   For armor, research stops at material and construction; the numbers then come
   from the armor reference tables.
6. Keep one category internally consistent with one model, mark, loading, barrel,
   engine, boost, armour convention, ammunition variant, and test basis. Do not
   construct a best-of-all-variants specification. Armor is the exception: one
   armor category is a pragmatic group of items that share a slot, set shape,
   coverage, protection class, and ballistic rating, and is deliberately not
   restricted to a single exact model.
7. Edit minimally, preserve file style, and restore the documented category order.
8. Run every applicable check in `format-and-validation.md`. Rerun the scanner after
   coverage changes and classify every remaining relevant row as intentionally
   skipped or unresolved.
9. Report additions per JSON file, newly covered short names, unresolved historical
   items and reasons, skipped fictional/generic items, principal sources, fallback
   values, conversions, and disputed or approximate values needing review.

## Completion standard

Finish the requested queue or batch rather than stopping after examples. Do not ask
the user about individual ambiguities: make the best defensible decision, leave only
genuinely unresolved identities unchanged, and disclose them. For armor there is no
unresolved case: assign the nearest defensible tier and disclose the judgement. Do not add comments
or citations to category JSON unless the schema is explicitly changed to support
them; retain source provenance in working notes and the final report instead.
