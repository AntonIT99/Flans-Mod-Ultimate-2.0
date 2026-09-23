---
name: mod-feature-gap-analysis
description: Perform a one-way semantic feature-gap analysis from a user-supplied reference Minecraft mod to a target port, reporting only missing, partial, or uncertain reference functionality. Use for comparing mod codebases without implementing changes.
---

# Mod Feature Gap Analysis

Find functionality that exists in `REFERENCE` but is missing or incomplete in `TARGET`.

## Inputs and direction

- Treat an unlabeled supplied directory as `REFERENCE`.
- Default `TARGET` permanently to the repository root at `../../..` relative to this skill directory. Resolve and canonicalize that path before use.
- An explicitly supplied `TARGET` overrides that default for this invocation only.
- Resolve and verify both directories before analysis. If `REFERENCE` is absent or unreadable, request it; never silently reverse the comparison.
- The comparison is strictly `REFERENCE` -> `TARGET`. Exclude target-only functionality, target improvements, modernization/refactors, and reference behavior already fully represented in the target.

## Discover and compare

Discover features systematically from the reference, subsystem by subsystem, instead of using a fixed checklist. Cover major and secondary observable behavior across gameplay, entities, weapons/ammunition, vehicles/aircraft/mechas, physics, rendering/animation, GUIs, configuration and pack capabilities, networking, controls, commands, teams/modes, inventories/crafting, sounds/effects, AI, interactions, persistence, synchronization, quality-of-life behavior, and special cases when present.

For each discovered reference behavior:

1. Trace enough reference code, resources, definitions, call sites, and data flow to understand its observable functionality.
2. Search the target for direct, renamed, moved, split, merged, generalized, componentized, data-driven, or newer-mechanism equivalents.
3. Follow relevant fields, packets, handlers, entity logic, renderers, registries, components, and call sites as needed.
4. Classify internally as `PRESENT`, `PARTIAL`, `MISSING`, or `UNCERTAIN`.

Names and architecture alone are never proof. Do not stop because a broad system exists; audit its individual mechanics and secondary behaviors. Before marking `MISSING`, understand the reference behavior and inspect plausible direct and indirect target equivalents. Prefer `UNCERTAIN` over guessing. Do not report `PRESENT`, except for a brief note needed to delimit a `PARTIAL` finding.

Work strategically: search before opening many files, reuse target-system findings within the run, and avoid generated/build/cache/runtime material unless directly relevant. Normally ignore `build/`, `.gradle/`, `run/`, `logs/`, `generated/`, IDE metadata, and binaries. Do not narrate routine searches or spend report space on confirmed presence.

## Report

Write the complete report in the current working directory. Use `../../../reports/feature-gap-analysis.md` when it does not already contain an unrelated analysis. Otherwise derive a short filesystem-safe identifier from the reference directory and use `feature-gap-analysis-<identifier>.md`; if needed, add a numeric suffix rather than overwriting any unrelated report.

Group findings by subsystem. Every finding must contain:

```markdown
### <Feature> — MISSING|PARTIAL|UNCERTAIN

Reference: `<paths/symbols>`
<Concise explanation of the behavior.>

Target checked: `<paths/symbols>`
<What was inspected and why it is or is not equivalent.>

Missing: <Exactly what reference functionality remains absent.>
```

Use concise path/symbol evidence, not large source excerpts. For `PARTIAL`, distinguish the represented behavior from the missing remainder.

End with a compact table containing only reported findings:

```markdown
| Subsystem | Feature | Status | Confidence |
| --------- | ------- | ------ | ---------- |
```

Confidence is `HIGH`, `MEDIUM`, or `LOW`. Then add `## Areas requiring deeper audit` only for uncertain findings, low confidence, substantially different implementations, or areas where subtle functionality may remain hidden.

## Safety and completion

This workflow is analysis-only. Do not modify or implement anything in either codebase. Create only the report or necessary analysis artifacts.

Keep the final chat response extremely short: counts of `MISSING`, `PARTIAL`, and `UNCERTAIN`; the report path; and the 3-5 most important gaps. Detailed evidence belongs only in the report.
