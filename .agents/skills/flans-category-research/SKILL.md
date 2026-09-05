---
name: flans-category-research
description: Research, add, sort, or audit built-in categories for Flan's Mod Ultimate guns, AA guns, ammunition, bombs, grenades, ground vehicles, ships, aircraft, and armor. Use for *_categories.json work, missing_shortnames.csv triage, historical-stat validation, generic and fictional item balancing, armor balance harmonization, aliases, ammunition groups, and category coverage.
---

# Flans Category Research

Maintain the built-in category JSON files as auditable defaults. The nearest
applicable `AGENTS.md` is authoritative and must be read before this workflow.

Three distinct kinds of work live here:

- **Historical categories** for guns, AA guns, ammunition, bombs, grenades, ground
  vehicles, ships, and aircraft. Values are researched from real-world sources. Only
  an item whose identity and class are both unknowable is left uncategorized.
- **Generic and fictional categories** for the same types, where no exact real model
  exists. These are balance assignments constrained by real anchors: identity is
  resolved as far as the evidence allows, then values come from a selected real
  exemplar or an anchored tier, and the label is marked. Most "generic" items turn
  out to be identifiable once their consumers are inspected.
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
  AA guns, magazines, bullets, cannon shells, missiles, mixed belts, bombs, depth
  charges, naval mines, torpedoes, or grenades.
- [vehicles-aircraft.md](references/vehicles-aircraft.md) for ground vehicles or
  aircraft.
- [ships.md](references/ships.md) for anything that floats: warships, submarines,
  merchant hulls, landing craft, and boats. Read it with `vehicles-aircraft.md`,
  whose mandatory driveable rules still apply.
- [armor.md](references/armor.md) for helmets, uniforms, vests, plates, droid and
  creature shells, and every other `armorFiles` definition.

Also read [generic-and-fictional.md](references/generic-and-fictional.md) whenever
an item cannot be tied to one exact real model: generic labels such as
`75mm AP Tank Shell` or `Generic 500lb bomb`, and any fictional, fantasy,
science-fiction, toy, or joke content. It decides identity and values; the domain
reference still decides which keys are mandatory and in what units. It does not
apply to armor.

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
6. When no exact real model can be established, work the resolution ladder in
   `generic-and-fictional.md` before concluding anything is out of scope. Most
   generic labels resolve to a real item through their consumers; what remains gets a
   marked `(Generic)` or `(Fictional)` category from a selected exemplar or an
   anchored tier, with every mandatory domain field still present.
7. Keep one category internally consistent with one model, mark, loading, barrel,
   engine, boost, armour convention, ammunition variant, and test basis. Do not
   construct a best-of-all-variants specification, and never blend several real
   items into a generic one. Armor is the exception: one
   armor category is a pragmatic group of items that share a slot, set shape,
   coverage, protection class, and ballistic rating, and is deliberately not
   restricted to a single exact model.
8. Edit minimally, preserve file style, and restore the documented category order.
9. Run every applicable check in `format-and-validation.md`. Rerun the scanner after
   coverage changes and classify every remaining relevant row by the resolution step
   it reached.
10. Report additions per JSON file, newly covered short names, unresolved historical
    items and reasons, generic and fictional assignments with their exemplars, tiers,
    and caps, principal sources, fallback values, conversions, and disputed or
    approximate values needing review.

## Completion standard

Finish the requested queue or batch rather than stopping after examples. Do not ask
the user about individual ambiguities: make the best defensible decision, leave only
genuinely unresolved identities unchanged, and disclose them. Being generic or
fictional is not an unresolved identity; only an item whose class cannot be inferred
either is. For armor there is no
unresolved case: assign the nearest defensible tier and disclose the judgement. Do not add comments
or citations to category JSON unless the schema is explicitly changed to support
them; retain source provenance in working notes and the final report instead.
