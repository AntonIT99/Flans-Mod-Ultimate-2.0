---
name: flans-category-research
description: Research, maintain, and audit Flan's Mod Ultimate category JSON and shortname coverage, including historical statistics, weapon cadence and reload cycles, and armor, fictional-item, and mecha balancing.
---

# Flans Category Research

Maintain auditable shipped defaults in `src/main/resources/config/`. Read its
`AGENTS.md` first. Check the current branch and `gradle.properties`; never edit
runtime copies. This skill owns category policy; domain references own values.

## Read only what the task needs

- JSON, ordering, membership, scanner: [format-and-validation.md](references/format-and-validation.md).
- Identity or real-world statistics: [research-policy.md](references/research-policy.md).
  Armor uses only **Identity and scope** and **Source ladder**, for construction.
- Guns, AA guns, ammunition, grenades or bombs: [guns-ammunition-grenades.md](references/guns-ammunition-grenades.md).
- Ground vehicles or aircraft: [vehicles-aircraft.md](references/vehicles-aircraft.md).
- Ships: [ships.md](references/ships.md), plus the shared driveable and weapon-bank
  rules in `vehicles-aircraft.md`; do not load its aircraft sections for ships.
- Wearable armor: [armor.md](references/armor.md). Load its separate worked
  examples only if an assignment needs clarification.
- Mecha chassis (`definitions/mechas`): [mechas.md](references/mechas.md).
- Generic/fictional weapons, ammunition, vehicles or aircraft:
  [generic-and-fictional.md](references/generic-and-fictional.md), in addition to
  their domain reference. Its real-exemplar caps do **not** govern armor or mechas.

Use headings to read applicable sections of long references. Scanner-only and
sorting-only work needs no historical research or domain balance tables.

## Workflow

1. For coverage, run `scripts/scanShortnames.py`. Treat its CSV as a queue, not
   evidence of identity. Prioritize official/bundled packs when requested.
2. Establish identity from pack-local English localization, then `Name`, `Model`,
   `Description`, filename when localization is absent. Inspect consumers and
   configurations; search bundled, official and available `run/flan` packs for aliases.
3. Select the doctrine: researched historical configuration; resolved generic or
   fictional exemplar/tier; fixed armor tables; or mecha chassis tables. Keep each
   configuration coherent. Armor/mecha groups may share a balance profile without
   sharing an exact historical model.
4. Edit minimally, restore documented ordering, and run the applicable format and
   domain checks. Rerun the scanner after coverage changes.
5. Finish the requested batch. Only historical/generic items whose identity **and
   class** remain unknowable may be skipped. Armor and mecha rows remain coverage
   gaps; assign the nearest defensible tier and disclose uncertainty.
6. Report changed files, coverage, unresolved items, evidence, assigned tiers or
   exemplars, estimates/conversions and validation limitations. Keep provenance in
   notes/report, never unsupported citation fields or comments in category JSON.
