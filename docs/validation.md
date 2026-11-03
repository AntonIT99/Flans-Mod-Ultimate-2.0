# Validation for coding agents

Codex, Claude Code and terminal users share the same deterministic scripts. Select task-owned paths
explicitly; the runner never selects every uncommitted change or calls a model. Inspect the plan first
when scope is uncertain.

```powershell
# Documentation, translation JSON and script syntax: no Gradle or Qodana startup.
powershell -NoProfile -File scripts/validate.ps1 -Profile Data -Task 'Update translations' -Files 'src/main/resources/assets/flansmod/lang/en_us.json' -Apply

# Small Java change: affected compilation and explicitly selected tests.
powershell -NoProfile -File scripts/validate.ps1 -Profile Focused -Task 'Banner editing' -Files 'src/main/java/com/flansmodultimate/common/block/entity/TeamBannerBlockEntity.java;src/test/java/com/flansmodultimate/common/block/entity/TeamBannerBlockEntityTest.java' -Tests '*TeamBannerBlockEntityTest' -Apply -Plan
```

Remove `-Plan` to execute. The five profiles are Data, Focused, Analysis (adds scoped SonarLint), Deep
(also adds Gradle-managed Qodana), and Release (full build). Use Analysis for substantial logic changes and Deep for
large Java refactors or requested inspections. Small shared-contract changes can still need affected
consumer artifacts: add `-Artifacts 'jar;npcsJar'`. Release builds all modules and runs the full test suite.

`-Files`, `-Tests`, `-SourceSets` and `-Artifacts` accept semicolon/comma separated values. Quote paths
and patterns. Source sets are inferred from Java paths and module descriptors; use `-SourceSets` for
resource-only changes that need compilation/artifact checks. Focused, Analysis and Deep require test
filters or a specific `-NoTestsReason`. A reason records a validation gap; it does not replace useful tests.

Data checks JSON syntax and PowerShell parsing, applies/checks Markdown and `.gitignore` formatting,
and follows the existing resource-JSON numeric-array formatting rule. It never rewrites legacy pack text
or binary assets. Schema, pack-parser, image and gameplay checks remain the responsibility of their
owning workflow. Java and Gradle/properties changes require a compilation/build profile.

Scoped Spotless and SonarLint targets are supplied by a temporary init script. The shared build
configuration is unchanged, and compile/test dependencies remain intact. Nested pack repositories
receive their own diff checks. Deleted tracked files are accepted; misspelled nonexistent paths are rejected.

## Output, status and cancellation

Compact output is the default: task, run ID, outcome, duration, bounded failure diagnostics and log paths.
Full stdout/stderr and `result.json` stay under `.gradle/validation-runs/<run ID>/`. Explicitly requested
SonarLint reports are summarized and deduplicated, with at most 20 findings in the output. Findings remain
unclassified until reviewed against the task's changes; a successful tool exit alone does not mean no warnings.
Each run records its timing without launching automatic benchmarks or model reviews.

`-OutputMode Json` returns structured run envelopes; a multi-stage profile emits one JSON object per stage.
Full streams raw output when needed for diagnostics. `Qodana -Json` and the compatibility `Idea -Json` interface return raw
inspection JSON unless an explicit output mode is selected. Inspection Compact excerpts show up to 30 lines plus the final summary;
complete results remain in the logs.

```powershell
powershell -NoProfile -File scripts/run-validation.ps1 Status
powershell -NoProfile -File scripts/run-validation.ps1 -OutputMode Json Status
powershell -NoProfile -File scripts/run-validation.ps1 -RunId '<ID shown by Status>' Cancel
```

Status identifies the task, command, supervisor, last observed Gradle task and age of the last output.
An alive process with quiet output is not automatically healthy or stuck. Some phases, particularly
Qodana indexing, emit no phase events. Cancellation signals the exact run, rather than killing by process
name or a potentially recycled PID. The supervisor stops its descendants before releasing the queue.

Low-level Gradle, Qodana and Data commands remain available through `run-validation.ps1`; Idea is a compatibility alias.
Put coordinator options before the tool name. Default queue wait: 600 seconds; Data execution: 120 seconds;
Gradle and Qodana supervisor: 1800 seconds. Longer work needs explicit timeout adjustments.
Qodana installs its own native Community engine through Gradle; no user IntelliJ installation is needed.
The first inspection downloads the CLI, engine and managed plugins. Subsequent scans retain a checkout-local cache.
See [Qodana inspections](intellij-inspections.md) for setup, scoping and report validation.

Gradle uses `--no-daemon` for process containment, sacrificing warm daemon reuse. Batch related checks
and retain incremental outputs. Do not re-enable a shared daemon as a shortcut around cancellation;
a reusable supervised daemon needs a separate measured design.

## Model work and follow-ups

Successful checks need no reviewing agent. Read compact summaries before full logs. An economical
analysis agent may investigate findings with explicit files and concise context. Extra high-end model
reviews or escalations require user approval; continuing the already-selected primary session does not.

Fix introduced findings and only the existing issues necessary for the task. Rerun affected checks after
a fix, rather than every analyzer or the full suite. Do not automatically retry broken infrastructure.
The shared Claude configuration has no automatic IntelliJ Stop hook. Live Minecraft testing remains
subject to its separate skill and is never implied by a validation profile.
