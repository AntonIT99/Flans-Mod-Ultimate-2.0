---
name: mod-config-gap-analysis
description: Compare the runtime mod configuration exposed by a reference Minecraft mod with a target port, reporting only reference config parameters that have no semantic target equivalent. Use for focused .minecraft/config compatibility audits, not general feature comparison or implementation.
---

# Mod Config Gap Analysis

Identify every user-facing mod configuration parameter from `REFERENCE` that lacks equivalent functionality in `TARGET`.

## Inputs and direction

- Treat an unlabeled supplied directory as `REFERENCE`.
- Default `TARGET` to the repository root at `../../..` relative to this skill directory; resolve and canonicalize it before use. An explicit `TARGET` overrides it for this invocation only.
- Resolve both directories before analysis. Require a readable reference and never reverse the comparison.
- Analyze the mod-owned configuration intended for `.minecraft/config/`, including client, common, and server config files when applicable. Do not treat content-pack definitions, ordinary assets, launch arguments, gamerules, or unrelated build settings as mod config.
- Report only reference parameters with no target equivalent. Exclude target-only parameters, target improvements, refactors, and parameters already represented in the target.

## Audit workflow

1. Discover the reference config entry points and generated/runtime config files. Trace declarations, registration, categories/sections, defaults, validation/ranges, comments, load/reload handlers, aliases/migrations, and actual consumers.
2. Build a reference inventory containing the external key and file/category path, scope, value type/default, and observable behavior controlled. Include parameters registered indirectly through helpers or specs.
3. Investigate the target for semantic equivalents. Search config declarations and files, renamed or regrouped keys, aliases, command/UI replacements that persist as mod config, and consumers implementing the same configurable behavior.
4. Judge equivalence by user-observable control, not spelling, class, library, file format, or architecture. A target setting is equivalent only when it exposes substantially the same configurable behavior and scope. Record `UNCERTAIN` rather than guessing when dynamic registration or indirect consumption prevents a reliable decision.

Do not infer a parameter from a field name alone: verify that it is exposed through the runtime config system. Conversely, do not call a reference declaration functional without checking whether it is registered and consumed; list dead reference settings separately as ghost/dead settings, not as target feature gaps.

Search strategically and reuse config-system findings. Normally ignore `build/`, `.gradle/`, `run/`, logs, generated output, IDE metadata, and binaries unless a generated example config is the only reliable key inventory. Do not narrate routine searches or dump source excerpts.

## Report

Write `mod-config-gap-analysis.md` in the current working directory. If it already contains an unrelated analysis, derive a short filesystem-safe reference identifier and use `mod-config-gap-analysis-<identifier>.md`, adding a numeric suffix if necessary rather than overwriting.

For each missing parameter, provide:

```markdown
### <config file/category/key> — MISSING|UNCERTAIN

Reference: `<paths/symbols>`
<Type/default/scope and concise behavior.>

Target checked: `<paths/symbols>`
<Relevant settings and consumers inspected.>

Missing: <Exact user-configurable behavior absent from target.>
```

Group by config file/category or subsystem, whichever is clearer. Add a short `## Dead reference settings` section only when declarations are exposed but have no effective reference consumer; do not count them as missing target config.

End with:

```markdown
| Reference parameter | Scope | Status | Confidence |
| ------------------- | ----- | ------ | ---------- |
```

Use `HIGH`, `MEDIUM`, or `LOW` confidence. Add `## Areas requiring deeper audit` only for uncertain or low-confidence items.

## Safety and completion

This is analysis-only. Do not modify either mod, its config, or runtime files. Create only the report or necessary analysis artifacts.

Keep the final chat response very short: missing and uncertain counts, report path, and the 3-5 most important absent settings. Detailed evidence belongs in the report.
