---
name: config-reference-audit
description: Audit and update the Flan's Mod Ultimate wiki ConfigReference against the current legacy content-pack parser code, correcting missing, stale, and inaccurate parameter documentation while reporting uncertainty. Use for documentation maintenance, not parser implementation, installed-pack compatibility, or runtime TOML/properties config.
---

# Config Reference Audit

Bring the wiki's `ConfigReference.md` up to date as an exhaustive and accurate reference for the content-pack `.txt` parameters accepted by the current code.

## Inputs and scope

- Default `TARGET` to the repository root at `../../..` relative to this skill directory. Resolve and canonicalize it before analysis.
- Default `CONFIG_REFERENCE` to `ConfigReference.md` in the sibling wiki repository named `<TARGET directory name>.wiki`. An explicit target or page path overrides the corresponding default for that invocation only.
- Treat the current target parser code as the source of truth. The wiki is the documentation being checked; never infer parser support from the wiki itself.
- Audit legacy content-pack type definitions loaded through `TypeFile` and the concrete types registered by `EnumType`, including inherited parsers, shared helpers, and delegated parameter families such as gun animation settings, ammo groups, ammo overrides, and ammo removal.
- Include an abstract or intermediate type when its parser contributes parameters to a registered concrete type. Check the wiki's folder mapping and inheritance claims as part of the audit.
- Exclude Forge client/common/server TOML settings, content-loading properties, category JSON schema, commands, gamerules, launch/build configuration, and unrelated wiki pages. Use `Mod-Config.md` only to recognize and exclude runtime mod config.
- Do not compare legacy reference repositories unless the user explicitly requests historical provenance. This audit asks what the current target accepts and does now.

## Build the code inventory

1. Trace the loader from `EnumType`, `TypeFile`, and `InfoType.load` into every effective `read(TypeFile)`, `readLine`, client read, and delegated parser used by registered types.
2. Inventory every accepted external key or key family. Inspect all read helpers, `hasConfigLine`/`hasAnyConfigLine` checks, raw line dispatch, aliases, arrays or maps of key names, dynamic prefixes or suffixes, and helpers outside `common.types` reached by a type parser. Do not rely on a regex over string literals alone.
3. Record for each entry: accepted spelling, case normalization, owning parser, applicable concrete types after inheritance, aliases, format, default, units, validation/range, repeatability/accumulation, fallback or precedence rules, sanitization, and observable behavior.
4. Trace consumers far enough to validate the documented purpose. A stored field or getter alone does not prove the claimed behavior. If a parsed value has no effective consumer, keep it in the accepted-key inventory and flag any claimed effect as inaccurate or uncertain rather than silently dropping the key.
5. Treat structured multi-line declarations and pattern-based keys as parameter families, but enumerate finite accepted variants where the code does. Distinguish recipe payload rows and other continuation data from actual parameter keys.

Use tests and representative source definitions only as supporting evidence. Generated output, `build/`, `.gradle/`, runtime `run/` files, and old reports are not authoritative parser inventories.

## Build the documentation inventory

Read the entire `ConfigReference.md`, including the parser rules, folder/class and inheritance tables, all parameter tables, alias rows, special-system sections, prose constraints, and examples.

Normalize key spelling case-insensitively for comparison because `TypeFile` does so. Preserve source spelling in evidence. A key mentioned only incidentally in prose or an example is not fully documented unless readers can determine its applicable type, accepted form, and purpose from the page.

Reconcile base-class documentation through inheritance: a parameter accurately documented once under a parent covers its concrete descendants. Conversely, a key documented under the wrong type, or an alias shown for a type that does not accept it, is not covered correctly.

## Classify findings

- `MISSING`: current code accepts a parameter/key family, but the page does not document it adequately.
- `STALE`: the page presents a parameter, alias, type, folder, or inheritance relationship that current code no longer accepts or exposes as stated.
- `INACCURATE`: the key exists, but the page materially misstates its applicable types, format, default, units, range, repeat behavior, fallback/precedence, sanitization, or effective purpose.
- `UNCERTAIN`: indirect, reflective, generated, environment-specific, or ambiguous behavior prevents a reliable conclusion.

Do not report capitalization-only differences. Do not count one alias row as multiple findings unless the aliases have different problems. Avoid style, wording, or desired-feature suggestions that do not affect correctness or completeness.

## Update the wiki

Correct every `MISSING`, `STALE`, and `INACCURATE` finding supported by high- or medium-confidence code evidence:

- Preserve the page's existing organization and concise table style. Add a parameter to its owning base or concrete type rather than duplicating inherited keys under descendants.
- Group true aliases in one row. Keep separate rows when similarly named keys have different parsing, defaults, units, precedence, or behavior.
- Update parser rules, inheritance/folder tables, special-system prose, and examples when the mismatch is broader than one parameter row.
- Describe current observable behavior without promising behavior that the parser does not implement. Mention accepted legacy misspellings as aliases when they remain supported.
- Do not guess at `UNCERTAIN` findings. Leave ambiguous documentation unchanged unless the existing statement is demonstrably false, and record the unresolved issue in the report.

After editing, repeat the code-to-wiki and wiki-to-code reconciliation. A corrected key is not complete until its applicable type, format, default, units, aliases, and purpose agree with the parser and consumers.

## Record the audit

Write `reports/config-reference-audit.md` under `TARGET`. Update it when it is already the report for this same target and page; if it contains unrelated material, add a numeric suffix instead of overwriting it.

Start with the resolved target and wiki page, the audited parser/type count, the number of accepted distinct normalized keys or finite variants, and any dynamic families or unreadable inputs. State that confirmed findings were applied to the wiki. Then use these sections, omitting empty ones:

```markdown
## Missing documentation
## Stale documentation
## Inaccurate documentation
## Uncertain documentation
```

For every finding include:

```markdown
### <Type or shared parser> / <Key or family> — MISSING|STALE|INACCURATE|UNCERTAIN

Code evidence: `<paths and symbols>`
Wiki evidence: `<heading, table row, or absent after checking relevant sections>`
Finding: <Exact mismatch and the documentation correction applied, or why it remains unresolved.>
Confidence: HIGH|MEDIUM|LOW
```

End with one compact table containing every finding, its applicable type(s), status, and confidence. Add `## Areas requiring deeper audit` only for uncertain or low-confidence items.

## Safety and completion

Edit only the target wiki `ConfigReference.md` and the audit report. Do not edit parser code, tests, content packs, generated output, runtime files, or unrelated documentation. Preserve unrelated changes in both repositories.

Before finishing, verify that every registered `EnumType` was covered; inherited, delegated, raw-line, alias, and pattern-based parsing were checked; every wiki parameter row was reconciled back to code; confirmed findings were corrected; uncertain findings were not guessed; and both repositories pass `git diff --check` for scoped changes. Report zero findings explicitly when the page was already exhaustive and accurate.

Keep the final chat response short: corrected counts for `MISSING`, `STALE`, and `INACCURATE`, the unresolved `UNCERTAIN` count, the wiki and report paths, and the 3-5 most important changes. Detailed evidence belongs in the report.
