---
name: infotype-parameter-gap-analysis
description: Audit Minecraft mod InfoType subclasses and text-definition parsers for accepted, ignored, and ghost parameters, optionally comparing reference keys with a target case-insensitively. Use for focused legacy type-definition compatibility analysis, not implementation.
---

# Infotype Parameter Gap Analysis

Audit type-definition parameters in `TARGET`, and in optional `REFERENCE`, then identify ghost parameters and reference-only keys.

## Inputs

- Default `TARGET` to the repository root at `../../..` relative to this skill directory; resolve and canonicalize it before use. An explicit `TARGET` overrides it for this invocation only.
- `REFERENCE` is optional. With no path, audit only the target. Treat one unlabeled supplied directory as `REFERENCE`; use the default target.
- Resolve every supplied directory before analysis. Never silently reverse reference and target.

## Inventory parsing

For each repository being audited:

1. Find `InfoType` and every direct or indirect subclass. Include abstract intermediates and inherited parsing behavior when determining the effective parameters of concrete types.
2. Locate all mechanisms that accept legacy text-definition parameters: line/key dispatch, `read`/`parse` methods, helpers, annotations, reflection, aliases, shared superclass parsers, loaders, and compatibility adapters.
3. Inventory actual `.txt` type definitions and associate keys with the applicable type class. Distinguish comments/directives and values from parameter names.
4. For each class, reconcile keys accepted by code with keys observed in definitions. Record source spelling, aliases, owning parser, destination field/action, and representative definitions.

Do not rely only on obvious `if`/`switch` literals. Follow delegated and inherited parsers, normalized keys, case-insensitive dispatch, annotations, and data-driven registration. Avoid generated/build/cache/runtime directories unless directly relevant.

## Determine ghost parameters

Trace every accepted or observed parameter far enough to decide whether its value affects observable behavior. Inspect field reads, call sites, constructors, registries, serialization/networking, entities, items, recipes, rendering, models, GUIs, and other consumers as applicable.

Classify internally:

- `ACTIVE`: parsed value has a real consumer.
- `GHOST_PARSED`: parser accepts/stores the value, but no effective use exists beyond assignment, copying, logging/debug text, or dead code.
- `GHOST_UNPARSED`: key occurs in applicable `.txt` definitions but no effective parser accepts it.
- `UNCERTAIN`: reflection, external integration, generated code, or indirect flow prevents a reliable decision.

Writes alone are not usage. Inheritance, reflection, serialization, registry consumption, or access from other modules may be usage; investigate before labeling a ghost. Prefer `UNCERTAIN` over a false ghost. Report ghost and uncertain parameters, not active parameters, except for a brief note needed to explain an alias or partial path.

## Optional reference-to-target key gap

When `REFERENCE` is provided, add a dedicated list of all reference parameter names that do not exist in the target:

- Build the reference key set from accepted parser names/aliases plus real keys found in applicable reference `.txt` definitions.
- Build the target key set the same way, including inherited/common parsers and aliases.
- Compare normalized parameter names case-insensitively; case differences never count as gaps.
- This list is lexical parser/definition compatibility, not semantic feature equivalence. A differently named target behavior does not make the reference key exist unless the target accepts that reference key as an alias.
- For each absent key, identify the applicable reference type class(es), whether it is parsed and/or observed in definitions, and concise evidence. Do not include target-only keys.

## Report

Write `../../../reports/infotype-parameter-gap-analysis.md` in the current working directory. If it already contains an unrelated analysis, derive a short filesystem-safe reference identifier (or `target-only`) and add it to the filename; add a numeric suffix rather than overwriting.

Use these sections as applicable:

```markdown
## Target ghost parameters
## Reference ghost parameters
## Reference parameters absent from target
## Uncertain parameters
```

Group entries by concrete `InfoType` subclass. Each ghost entry must state the parameter and status, parser/definition evidence, consumer investigation, and why it is ineffective. Each reference-only entry must show reference evidence and the target parsers/aliases checked. Keep evidence to concise paths and symbols; do not dump source.

End with compact tables summarizing ghost findings and, when a reference exists, reference-only keys. Include `HIGH`, `MEDIUM`, or `LOW` confidence. Add `## Areas requiring deeper audit` only for uncertainty, low confidence, substantially indirect parsing, or hidden external consumers.

## Safety and completion

This is analysis-only. Do not edit type definitions or code and do not implement fixes. Create only the report or necessary analysis artifacts.

Keep the final chat response very short: counts by ghost status, reference-only count when applicable, report path, and the 3-5 most important findings. Detailed evidence belongs in the report.
