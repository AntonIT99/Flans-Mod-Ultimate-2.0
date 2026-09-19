---
name: content-pack-definition-sync
description: Reorganize tracked Flan content-pack .txt definitions under src/*/resources/flans_content and synchronize them with built-in category values while preserving legacy fallbacks. Use for source-definition cleanup or category-to-definition synchronization, not category research, runtime packs, or parser implementation.
---

# Content Pack Definition Sync

Normalize source definitions without changing their intended content identity or
silently discarding compatibility behavior. Category JSON is authoritative for the
properties it supplies; the current parser and consumers are authoritative for how
lines combine and which legacy fallbacks remain effective.

## Scope and prerequisites

- Default `TARGET` to the repository root at `../../..` relative to this skill.
  Work only in user-selected definitions below
  `TARGET/src/*/resources/flans_content/<pack>/definitions/`.
- Read the repository `AGENTS.md`, `src/main/resources/config/AGENTS.md`,
  `../flans-category-research/SKILL.md`, and its
  `references/format-and-validation.md` before editing. Do not load historical
  research or balance tables merely to copy already-authored category values.
- Inspect the scoped definitions, their concrete type parsers, category application
  code, and relevant runtime consumers. Consult the sibling wiki's
  `ConfigReference.md`; read `Realistic-Vehicle-Physics.md` and
  `Vehicle-Armour-and-Damage.md` when the scope touches those systems.
- Do not edit `run/flan`, archives, build output, generated metadata, or runtime
  category copies. Do not alter built-in category JSON in this workflow. An absent,
  conflicting, or apparently wrong category is an unresolved source-data issue, not
  permission to invent a replacement; use `flans-category-research` under a separate
  category-maintenance request.
- Preserve unrelated changes. Record the original encoding and line ending of each
  definition and retain them unless the user explicitly requests conversion.

Run `scripts/audit_definition_sync.py --path <scoped path>` before editing to build
the deterministic scalar-gap and repeatable-property inventory. Treat its output as
a queue; parser tracing decides semantics.

## Resolve category truth

1. Determine the definition type from the folder mapping in `EnumType`, then select
   only its matching `src/main/resources/config/<identifier>_categories.json`.
   Match category `items` against the sanitized `ShortName`, case-insensitively.
2. Apply every matching category in file order, including its property-level
   `exceptions` and `propertyModes`. Modes are case-insensitive: omitted or unknown
   modes preserve legacy `append`, `replace` discards values from the definition and
   earlier categories, and `ifAbsent` applies only while the effective property has
   no values. Same-file multi-category membership is valid only when the ordered
   result is intentional. Stop on an unresolved conflict instead of selecting a
   convenient value.
3. Preserve the category key spelling and value tokens. Trace alias families and
   parser read order: a differently named legacy alias can write the same field
   after the category key and defeat the intended value. Keep one coherent semantic
   value, using the category's key/unit for category-owned data.
4. For an ordinary single-value property, add or replace the definition line so its
   value agrees with the resolved category value. Remove obsolete duplicates and
   contradictory aliases after confirming they do not serve a distinct fallback.
5. Resolve repeatable properties using their effective mode chain before deciding
   whether to mirror them:
   - `append` remains definition-dependent and can double-apply mirrored values. For
     ammo groups, ammo overrides, belts, per-part armour, and other accumulating or
     ordered properties, leave append-owned lines in the category and report the
     textual gap unless duplicate application is demonstrably idempotent.
   - `replace` makes the final sequence from the last replacement onward independent
     of earlier definition values. Mirror that complete resolved sequence into the
     definition as the categories-disabled fallback; with categories enabled it is
     replaced rather than accumulated. Complete category-authored `AddRound` belts
     should follow this path.
   - `ifAbsent` is a fallback. Preserve an existing definition sequence because it
     deliberately prevents the category value from applying. If the definition is
     empty, mirroring the resolved fallback is safe: the category then skips an
     identical effective value.
   Evaluate later categories after earlier ones. A later `replace` may make earlier
   `append` values irrelevant, while a later `append` becomes part of the complete
   replacement-owned sequence.

## Preserve and improve fallbacks

- Keep parameters that no category supplies when they remain parsed and effective.
  Do not reduce a definition to its category-owned subset.
- Keep legacy values used when an optional modern system is inactive, incomplete,
  overridden by server configuration, or unavailable. Modern and legacy keys that
  model different systems are not duplicates merely because one outranks the other.
- When category data supplies enough inputs for a deterministic, semantically
  equivalent fallback, update or add the legacy value to approximate the modern
  result. Read [fallbacks.md](references/fallbacks.md) before doing so. Never derive a
  fallback from a display value, an incompatible unit, a shared round with several
  effective weapon velocities, or an unverified formula.
- Preserve authored fallbacks when no defensible conversion exists. Report why they
  could not be synchronized; do not replace uncertainty with a guessed number.
- Keep values outside both category ownership and optional-system fallback unless
  the user separately asks for obsolete-parameter removal. Use the analysis-only
  parameter-audit skills for evidence when effectiveness is uncertain.

## Reorganize each definition

Reorder complete parameter lines, never tokens within a line. Keep dependent and
repeatable sequences in their semantic order; do not alphabetize recipes, parts,
seats, shoot points, paintjobs, ammo belts, or animation steps.

Use short `// <Group>` headings and one blank line between non-empty groups. Adapt
the groups to the concrete type; typical order is:

1. identity and description;
2. item, model, texture, icon, and rendering;
3. recipe, inventory, and equipment/ammo mapping;
4. controls, operation, movement, and physics;
5. weapons, ballistics, damage, detonation, and protection;
6. parts, geometry, seats, collision, and animation;
7. sounds, particles, and other presentation;
8. remaining type-specific behavior.

Place a legacy fallback beside the modern values for the same behavior rather than
in a disconnected catch-all section. Remove tutorial prose, per-parameter
descriptions, commented-out example directives, generator banners, decorative
separators, and empty group headings. Preserve only legally required attribution or
a genuinely file-level note that affects maintenance; parameter explanations belong
in the wiki, not definitions. Never rewrite names, resources, filenames, casing, or
numeric style merely for aesthetics.

## Validate

1. Re-run `scripts/audit_definition_sync.py` on exactly the changed files. Resolve
   every scalar gap/mismatch, every safe `replace`/empty-`ifAbsent` repeatable sync
   gap, and explain every remaining `append` overlap or category-only repeatable.
   Confirm every scoped file has a valid `ShortName` and type match.
2. Compare the before/after semantic inventory. Apart from category synchronization
   and explicitly derived fallbacks, every active line and every ordered/repeatable
   sequence must be preserved. Confirm aliases, inline comments, blank values, and
   duplicate keys behave as the current parser expects.
3. Check that comments are group titles, groups are non-empty, and definitions retain
   their original encoding and line endings. Review the scoped diff and run
   `git diff --check`.
4. Run focused parser/physics tests when a fallback was derived. For changed bundled
   or official pack inputs, run the applicable `packsManagerJar` and/or
   `officialPacksJar` task required by repository rules. A full build is not required
   for pure definition reordering unless another rule or a behavior change requires
   it.
5. Update the wiki only if supported parameters, formulas, units, or workflow
   semantics changed. Ordinary definition cleanup and synchronization do not require
   a documentation edit.

Report scoped files, category-owned scalar changes, category property modes used,
mirrored `replace` or `ifAbsent` sequences, derived fallbacks with formulas and
assumptions, preserved unresolved fallbacks, remaining `append` hazards, removed
comment count, and validation performed or omitted.
