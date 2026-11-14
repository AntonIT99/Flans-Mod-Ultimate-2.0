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

## Choosing Gradle tasks

Choose validation from the changed behaviour and its consumers rather than running `build` at the end
of every task. `build` assembles and reobfuscates every module, including content packs. Use the Gradle
wrapper; common tasks are `test`, `build`, `runData`, `packsManagerJar` and `officialPacksJar`. Use
`--stacktrace` only to diagnose a failed build. Keep `gradlew` executable.

- Ordinary Java logic, AI or UI changes: compile the affected source sets and run focused tests. For
  NPC-only changes use `compileNpcsJava` and `test --tests '<relevant class or package>'`. Changing
  existing entity behaviour alone does not require assembling every pack.
- Registration, networking, resource loading or packaging changes: include the relevant artifact tasks
  (`jar`, `npcsJar`, `packsManagerJar` or the affected pack's jar task). Build packs when their inputs,
  packaging or a shared API they consume change; an unchanged pack is not rebuilt because another module changed.
- Full `build`: branch-wide ports, loader setup, shared build/source-set changes, release verification,
  or cross-module effects that scoped checks cannot cover. Include affected pack consumers when shared
  signatures, model classes or content contracts change.
- Keep Gradle's required task dependencies. Test filters do not prune compilation dependencies, and some
  analysis tasks need outputs of other source sets. Use `--dry-run` if the task graph is unexpectedly
  broad; do not add blanket `-x compile...` exclusions.
- Let Gradle reuse up-to-date outputs; do not routinely clean, rebuild, change build directories or
  disable caching. After comment, formatting, suppression or documentation-only follow-ups, rerun only
  the affected checks. A commit-only request does not repeat completed checks.

## Parallel tasks and shared build outputs

- Prefer a separate Git worktree per concurrent implementation task. Each task owns its source edits,
  `.gradle`, `build`, generated resources, inspection results and temporary logs. Do not redirect
  different worktrees to the same build or project-cache directory. The shared Gradle user home and
  dependency cache stay shared; do not create separate dependency caches by default.
- Within one checkout, run builds and inspections through `scripts/run-validation.ps1 Gradle ...` or
  `scripts/run-validation.ps1 Qodana ...`. The coordinator holds a checkout-specific mutex for the whole
  invocation; different worktrees have independent queues. Gradle's parallelism inside one invocation
  stays enabled. `-WaitSeconds`, `-TimeoutSeconds`, `-StatusSeconds` and `-Task` precede the tool name.
  Each invocation runs in a Windows job whose descendants are stopped before the queue is released.
  Check coordinator changes with `scripts/test-validation-coordinator.ps1`; its tests use fake tools.
- Direct `gradlew` and internal `qodana-inspect.ps1` launches, IDE auto-builds and source edits bypass the
  queue. `idea-inspect.ps1` and the coordinator's `Idea` tool are compatibility aliases for queued Qodana
  scans. Do not edit, format, clean or regenerate a checkout while another task validates it; a queued
  command sees the files present when it starts.
- Never delete another task's outputs or caches, remove Gradle lock files, run global `gradlew --stop`,
  or terminate processes by name. When cancelling a coordinator, make sure its own children have stopped
  before another validation starts. Do not cancel another task's work.
- Use unique task-specific temporary files and log names. On machines running several worktrees, reduce
  workers for a heavy invocation (for example `--max-workers=2`) instead of changing project-wide parallelism.

## Analysis findings

Successful checks need no reviewing agent. Read compact summaries before full logs, group duplicate
findings and separate introduced issues from pre-existing ones before editing. Fix introduced findings
and only the existing issues the task needs. Do not turn validation into unrelated warning cleanup.

- Up to three simple, mechanical SonarLint or IntelliJ findings in task-owned files may be fixed directly
  by the primary agent.
- Otherwise delegate interpretation, fixes and affected reruns to one Terra, Luna, Sonnet or Haiku
  subagent: Terra with medium reasoning for Java findings, Luna or Haiku for mechanical ones, raising
  reasoning within these families for harder findings. Give it fresh context with explicit paths, source
  sets and scope, not the parent conversation. Do not edit the delegated files concurrently. It returns
  changed paths, a concise finding summary, validation results and blockers.
- If no eligible subagent is available, report the analysis as incomplete rather than silently falling
  back to Opus, Sol or Astra. Additional high-end model reviews require user approval; continuing the
  already-selected primary session does not. Do not delegate mere command launching or log reading.

Rerun affected checks after a fix, rather than every analyzer or the full suite. Do not automatically
retry broken infrastructure: report the failure, diagnose a concrete cause, and retry once only if it was
corrected. The shared Claude configuration has no automatic IntelliJ Stop hook. Live Minecraft testing
remains subject to its separate skill and is never implied by a validation profile.
