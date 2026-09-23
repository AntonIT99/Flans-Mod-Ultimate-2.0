---
name: content-pack-parameter-audit
description: Audit the legacy Flan content-pack .txt parameters present in this repository's run/flan and src/main inputs, reporting only keys that the current implementation does not parse or parses without an effective feature. Use for a focused installed-pack compatibility audit, not reference-project comparison or implementation.
---

# Content Pack Parameter Audit

Find ineffective parameters that are actually used by the content packs available to this repository.

## Fixed scope

- Default `TARGET` to the repository root at `../../..` relative to this skill directory; resolve and canonicalize it before use. An explicit target repository may override it.
- Audit legacy `.txt` definitions found in `TARGET/run/flan` and, if any exist, beneath `TARGET/src/main`. Include content-pack directories and ZIP/JAR archives using the same pack roots and type-folder recognition as the current loader.
- Audit the implementation beneath `TARGET/src/main`, including common and client code, resources, mixins, and compatibility paths when they can consume a parsed value.
- Do not inspect another project, construct a reference key inventory, report target-only parser keys, or compare semantic features across versions.
- Do not report a parser parameter merely because code accepts it. A parameter is in scope only when it occurs in at least one applicable audited `.txt` definition.

If either scoped path is absent, record that fact and continue with the path that exists. If no applicable definitions are found, stop and report that the audit corpus is empty rather than broadening the search.

## Build the observed-key inventory

1. Follow the current content loader, `EnumType` folder mapping, and `TypeFile` line handling to determine which files and first tokens are definition parameters. Match runtime behavior rather than assuming a generic text format.
2. Inspect archives without unpacking them into tracked source directories. Ignore unrelated text files, documentation, comments, assets, generated/build/cache output, and definitions outside the two scoped roots.
3. Associate each observed key with its applicable concrete type, pack, archive or directory, and definition file. Preserve representative source spellings while normalizing only as the runtime parser does.
4. Keep the inventory exhaustive by distinct `(type, normalized key)` pair. The same spelling used by different types may have different status and must be investigated separately.

Record occurrence counts and all affected pack names. Keep representative file paths for evidence; do not dump every duplicate occurrence into the report.

## Trace parsing and effective use

For every observed pair, locate its effective parser. Follow inherited `InfoType` behavior, delegated readers, aliases, alternate spellings, helper methods, `TypeFile` lookups, annotations, reflection, category/config injection, and compatibility adapters as applicable. Parsing is case-insensitive where the implementation makes it so; case alone is not a gap.

Then trace the parsed value through fields and objects to current consumers. Check gameplay logic, entities and items, registries, networking and serialization, recipes, rendering and models, animation, audio, particles, GUIs, teams, and other relevant systems. Client-visible behavior is a real feature, but client-only code must not be mistaken for server gameplay behavior.

Classify findings as:

- `UNPARSED`: the key occurs in an applicable definition but no effective parser accepts it for that type.
- `PARSED_UNUSED`: the parser accepts the key, but its value has no effective current consumer beyond assignment, copying, default propagation, serialization with no downstream read, logging/debug output, or dead/unreachable code.
- `UNCERTAIN`: indirect flow, reflection, generated code, optional integration, or ambiguous type association prevents a reliable decision.
- `ACTIVE` is an internal conclusion only and is omitted from findings.

A write, getter, copy constructor, codec, packet field, or similarly named field is not by itself proof of use. Conversely, inherited use, reflection, model/animation lookup, registration, or a downstream deserialized read may be effective; investigate before classifying it. Prefer `UNCERTAIN` over a false positive.

## Report

Write `reports/content-pack-parameter-audit.md` under `TARGET`. Update that file when it is already the report for this same audit scope. If it contains unrelated material, add a numeric suffix rather than overwriting it.

Start with a concise scope summary: audited roots, packs/archives, applicable definition-file count, distinct `(type, key)` count, and any unreadable inputs. Then use:

```markdown
## Unparsed parameters
## Parsed but unused parameters
## Uncertain parameters
```

Omit empty finding sections. Group entries by concrete definition type. For each distinct finding include:

```markdown
### <Type> / <Parameter> — UNPARSED|PARSED_UNUSED|UNCERTAIN

Observed: `<pack(s); representative definition path(s); occurrence count>`
Parser checked: `<paths and symbols, or no matching parser>`
Consumers checked: `<paths and symbols relevant to the conclusion>`
Finding: <Concise explanation of why the parameter has no effective feature or remains uncertain.>
Confidence: HIGH|MEDIUM|LOW
```

End with one compact summary table containing every reported `(type, parameter)` pair, status, affected-pack count, occurrence count, and confidence. Add `## Areas requiring deeper audit` only for uncertain or low-confidence findings.

## Safety and completion

This skill is analysis-only. Do not edit code, content packs, archives, definitions, generated output, or runtime files. Create or update only the report and disposable analysis artifacts outside tracked source inputs.

Before finishing, verify that every reported key exists in the scoped definitions, every scoped type folder was considered, inherited/common parsing was checked, and no `ACTIVE` key appears as a finding.

Keep the final chat response short: counts for `UNPARSED`, `PARSED_UNUSED`, and `UNCERTAIN`, the report path, and the 3-5 most important findings. Detailed evidence belongs in the report.
