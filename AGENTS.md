# AGENTS.md

Nested `AGENTS.md` files add rules for their directories.

## On-demand game testing

When the user requests game testing, reproduction in Minecraft, multiplayer
verification or runtime profiling, read
`.agents/skills/flans-game-testing/SKILL.md` and follow its selected mode.
Do not automatically invoke the skill or launch clients, servers, bots,
GameTests or profiler captures for ordinary implementation work. This does not
change required formatting, compilation or unit checks. Creating or maintaining
testing documentation does not itself authorize a live run.

After requested testing or testing-tool repairs, reconcile confirmed reusable
lessons into the skill and owning testing guides using the maintenance protocol
in `docs/game-testing.md`. Correct stale advice and consolidate workarounds;
do not add session transcripts or duplicate manuals. Maintenance does not schedule
future testing or expand the user's requested scope.

## In-game testing with Minecraft MCP

For client UI, rendering or gameplay verification, read `docs/minecraft-mcp.md`.
Use the optional `-PwithMinecraftMcp` Gradle flag and the MCP server matching
this checkout's Minecraft version. Check `get_minecraft_status` before acting;
the bridge pins the loader and port and checks the MCP mod version.
Setup and agent registration are
available through `scripts/setup-minecraft-mcp.ps1 -RegisterCodex`. Use the
HTTP fallback documented there if tools are unavailable in the current session.

For performance profiling, read `docs/spark.md`. Spark loads in default dev runs;
use the matching `spark_<minecraft version>` analysis MCP server or `-PwithoutSpark` to opt out.

## Headless multiplayer testing

For headless multiplayer test players, read `docs/mineflayer.md`. Use the
matching `mineflayer_<minecraft version>` MCP server and isolated
`runServer -PwithMineflayer` profile. Check `get_server_status` before joining;
bots do not replace real-client rendering, GUI or modded-physics checks.

## Reading and investigation

- Use `rg` to locate relevant symbols or headings, then read matching sections and enough
  surrounding code to understand callers and invariants. Expand when evidence requires it;
  avoid dumping whole large files or reference repositories by default.
- Read applicable skills once and follow only references relevant to the selected workflow.
  For long guides, locate and read relevant sections rather than every section.
- Reuse instructions, excerpts and findings already in the current context; reread when files
  change, context is missing or correctness needs confirmation.

## Task-specific workflows

- For version-dependent changes or work ported between maintained branches, read
  `.agents/skills/flans-version-porting/SKILL.md` before editing.
- For merging `master` into a target version branch, also read
  `.agents/skills/flans-master-merge-port/SKILL.md` before starting the merge.
- For built-in `*_categories.json` research or maintenance, read
  `src/main/resources/config/AGENTS.md`, then
  `.agents/skills/flans-category-research/SKILL.md` and the references it routes to.
- For reorganizing or synchronizing source content-pack `.txt` definitions from
  built-in category values, read
  `.agents/skills/content-pack-definition-sync/SKILL.md` before editing.
- For auditing, repairing, or historically expanding ammunition in a selected
  source content pack, read
  `.agents/skills/content-pack-ammunition-expansion/SKILL.md` before editing.
- For auditing or expanding the visual effects (muzzle flashes, shoot particles,
  emitters, trails, explosion particles) of a selected source content pack, read
  `.agents/skills/content-pack-visual-effects-expansion/SKILL.md` before editing.
- For adding plausible `muzzleFlashPoint` coordinates to existing Java gun models
  in a selected source content pack, read
  `.agents/skills/content-pack-muzzle-flash-points/SKILL.md` before editing.
- For auditing or reshaping driveable part hitboxes and their HP distribution in
  a selected source content pack, read
  `.agents/skills/content-pack-driveable-hitboxes/SKILL.md` before editing.
- For decompiling reference classes that exist only as bytecode, read
  `.agents/skills/mod-class-decompilation/SKILL.md`; output goes to the gitignored
  `decompiled/` cache.
- For auditing and updating the wiki `ConfigReference.md` so it exhaustively and
  accurately documents the current legacy content-pack parsers, read
  `.agents/skills/config-reference-audit/SKILL.md` before auditing.

## Repository Rules

- When adding or renaming config options exposed in the options screen, add or update
  their translation keys in the same change. `ConfigOptionFactory` uses
  `options.flansmodultimate.<config leaf key>` for captions, the optional `.tooltip`
  suffix for localized descriptions, and `options.flansmodultimate.value.<enum type
  without Enum, lowercase>.<constant, lowercase>` for enum labels. Keep required
  tooltip variants in sync with their callers and update every available locale
  in the owning namespace, following `src/main/resources/assets/AGENTS.md`.
- Keep gameplay state server-authoritative. Do not initialize client-only classes
  from common or server code.
- Keep Forge and NeoForge references (`net.minecraftforge`, `net.neoforged`) in `platform/` or `mixin/`.
  `LoaderBoundaryTest` enforces this for the main mod and NPC module; its allowlist may only shrink.
- Preserve deterministic legacy-content loading and compatibility. Do not bulk-change
  definition whitespace, casing, filenames, encodings, or layouts.
- Do not edit generated output, runtime files, or generated metadata; change the
  generator or its input instead.
- Keep the main mod, bundled packs, and official packs as separate artifacts. Run
  `packsManagerJar` and/or `officialPacksJar` when their inputs or packaging change.
- Create or update documentation only when it adds practical value for users or
  developers by explaining a new mechanic or a fundamental change. Routine content
  additions, such as textures, paintjobs, or new items, do not warrant documentation
  on their own. Do not create wiki pages, audit reports, or other documentation merely
  to record that work was done, unless explicitly requested.
- In a mixed worktree, preserve unrelated changes and stage explicit paths only.
- Some source sets under `src/` (for example `src/wolff*pack/`) are independent git
  repositories with their own `.git` directory and remote. They are excluded from this
  repository through `.git/info/exclude`, not because they are local-only or scratch
  content: they are real, versioned, published work. Treat edits there as seriously as
  edits to the main repository. Check `git status`/`git diff` *inside* that directory
  (for example `git -C src/wolffromepack status`), and commit there, not in the root
  repository. A clean root `git status` does not mean such a pack is unchanged.
  Before assuming a `src/` directory is untracked or disposable, check for a nested
  `.git`.

## Delegated commits

When the user requests a commit in a worktree containing unrelated changes:

- The main agent identifies the intended repository, files and exact hunks, describes what
  belongs in the commit, and supplies the commit message. Do not delegate ownership decisions
  based only on a fresh subagent's interpretation of the working diff.
- Delegate staging and committing to an available Terra, Luna, Sonnet or Haiku subagent with
  low reasoning for mechanical staging or medium reasoning for mixed hunks, where supported.
  Give it fresh context with the explicit scope, exact hunks or patch, commit message and
  completed validation results. Do not edit the affected files or index concurrently.
- The subagent stages only the specified changes, inspects the complete staged diff, runs
  `git diff --cached --check`, and commits only the intended changes. Preserve unrelated working
  and staged changes. Stop and return to the main agent on ambiguous hunks or pre-existing
  staged changes whose ownership is unclear; never reset or unstage them to simplify the commit.
- Return the commit hash, included paths and any blockers in a concise summary. Reuse successful
  validation; a commit-only request does not trigger new builds or inspections. If no eligible
  subagent is available, report the limitation before substituting another model or workflow.

## Changelogs

`CHANGELOG.md` holds the release notes of the next main mod release, and `src/npcs/CHANGELOG.md`
those of the next NPC module release. The CurseForge tasks upload each file whole (see
`docs/curseforge-releases.md`); content-pack modules without a changelog of their own upload the
root one. These are the one exception to the documentation rule above: keep them current.

- Keep release notes selective: include major features, meaningful gameplay or usability changes,
  notable fixes of released behaviour, and compatibility or migration requirements. A change being
  visible to users does not by itself warrant an entry. Skip minor content corrections, individual
  asset or translation changes, small UI tweaks, refactoring, tests, tooling and internal cleanups.
- In the same change, update the relevant feature summary when the change meets that threshold.
  Group related improvements and fixes into one concise, general bullet describing their practical
  benefit; do not enumerate every affected item, parameter, setting, implementation detail or edge case.
- Write for users, in English, with one short sentence per bullet where possible and a few clear
  topic sections. Keep essential command names, opt-in controls and breaking changes in backticks;
  leave configuration syntax and exhaustive details to the appropriate reference documentation.
- Fold changes to something not yet released into its existing bullet instead of adding a fix line.
  Avoid repeating highlights in topic sections or appending unrelated details to an ever-growing
  bullet. Review the surrounding entries for overlap before adding one; prioritize a short, readable
  overview of the release over a complete record of the work.
- Keep each heading's version equal to `mod_version` (main mod) or the module's `archiveVersion`.
- Released notes are archived under `changelogs/<mod id>/<version>.md`, for example
  `changelogs/flansmodultimate/2.1.md` and `changelogs/wolffsmodnpcs/1.0.md`. When a version is
  bumped after a release, move the released file there unchanged and start the changelog over
  with the next version's heading. Never edit archived notes except to fix an error in them.

## Mixins

Before editing any mixin or mixin config, read
`src/main/java/com/flansmodultimate/mixin/AGENTS.md`; its shared rules apply to all
source sets, including NPCs. Keep at most one mixin class per target across the repository.

## External Reference Paths

These sibling directories live outside this repository, next to it in the parent
directory. Read them directly when behaviour, formats, or prior implementations need
checking. They are read-only references: never edit them unless explicitly asked.

- `../Flans-Mod-Ultimate-2.0.wiki` - this project's wiki repository. Consult it for
  documented behaviour, and update it when a new mechanic or fundamental change
  needs explanation that benefits users or developers, following the documentation
  rule above.
- `../Flans-Mod-Ultimate-1.7.10` - the original 1.7.10 project. **Primary** reference for
  legacy behaviour, content-pack formats, and expected gameplay semantics.
- `../FlansMod` - the original 1.12.2 project. **Secondary** reference; use when 1.7.10 is
  ambiguous or the question concerns newer-loader concerns. If the two disagree, 1.7.10
  wins unless the user says otherwise.
- `../Mr-Monorisu-Brazila-master` - the newest source repository of the
  Labjac Flan's Mod fork, used occasionally as an extra reference for alternative
  implementations. Treat it as inspiration, not authority.

## Code Style and Static Analysis

- Follow the project code style: Allman braces (`else`, `catch`, `finally`,
  `while` on new lines), 4-space indents without tabs, 4-space continuation indent,
  200-column right margin. Wildcard imports are allowed. Import groups, in order:
  project/libraries (`com`, `io`, `lombok`, `net.minecraftforge`, `noppes`, `org`),
  then `net.minecraft`, then `javax`/`java`, then static imports last.
- Declare all class and instance fields before methods; do not place methods between
  field declarations.
- Prefer Lombok annotations such as `@Getter`, `@Setter`, and `@NoArgsConstructor`
  (and other suitable Lombok annotations) to replace boilerplate code. Use them in
  line with the class's intended access, mutability, and constructor behavior.
- The sources of truth are `config/spotless/eclipse-java-formatter.xml` (Spotless),
  and `.idea/codeStyles/Project.xml` (IntelliJ). Do not edit
  them unless asked.
- Spotless is ratcheted from `origin/master`, so it only touches changed files. After
  editing Java or build configuration, run scoped `spotlessApply` and `spotlessCheck` together through
  the selected validation profile. The Data profile handles Markdown, `.gitignore`, JSON and PowerShell
  syntax without starting Gradle; it preserves legacy text-definition and binary layouts. Use `// spotless:off` /
  `// spotless:on` only for deliberately hand-aligned code. Never reformat files you
  did not otherwise change.
- SonarLint runs through Gradle with one task per source set (`sonarlintMain`,
  `sonarlintTest`, `sonarlintPacksmanager`, ...). They are on demand only, not part of
  `check`/`build`, and analyse only files changed against `origin/master` (including
  uncommitted and nested pack repository changes); add `-PsonarlintAll` for a full scan.
  `staticAnalysis` runs every source set. Run scoped analysis only when the chosen validation profile requires it. Introduce no new issues in touched code and fix existing ones only when
  they are in scope. Notable rules: no `volatile` on non-primitive fields (S3077; use
  `AtomicReference` or similar instead).
- When code is required but a SonarLint or IntelliJ check flags it, keep the code and add a
  narrowly scoped `@SuppressWarnings` with a short reason, so the code is visibly intentional.
- Run deterministic validation commands directly; successful checks do not need an additional reviewing agent.
  When SonarLint or IntelliJ findings need interpretation or fixes, delegate that work and affected reruns to one
  available Terra, Luna, Sonnet or Haiku subagent. Prefer Terra with medium reasoning for Java findings;
  use Luna or Haiku for simple, mechanical findings, or Sonnet when available. Select a comparable thinking
  level where supported, raising it within these model families for harder findings. Opus, Sol
  and Astra must only coordinate this work and consume a concise result, not analyse or fix the
  SonarLint or IntelliJ findings themselves. Start the subagent with fresh context, explicit affected paths,
  source sets and task scope rather than the full parent conversation. Use the validation
  coordinator and do not edit the delegated files concurrently. Preserve behaviour and unrelated
  changes; return changed paths, a concise finding summary, validation results and unresolved blockers. If no eligible model
  or model-selectable subagent tool is available, report the affected analysis as incomplete rather than
  silently falling back to Opus, Sol or Astra. This delegation does not make either analysis mandatory
  for changes exempted below. Do not launch additional high-end models or increase delegated work to a high-end
  model without the user's explicit approval. This does not require approval to continue the primary session
  using the model the user already selected. Do not delegate mere command launching, waiting, or successful-log reading.

## Qodana Inspections

When running or maintaining Qodana's IntelliJ-based inspections, read `docs/intellij-inspections.md`
for scope, cache, timeout and process-safety instructions. Use the delegated analysis
workflow above. The Build and Validation section below determines when inspections apply.

## Build and Validation

Choose one profile at the start and briefly state its scope and reason. Use
`powershell -NoProfile -File scripts/validate.ps1 -Profile <profile> -Task '<user task>' -Files '<path1;path2>'`.
Pass only task-owned files, including deletions; never select the whole dirty worktree automatically.
`-Plan` prints the deterministic plan without starting validation. `-Apply` formats the selected files.

| Profile | When | Checks |
| --- | --- | --- |
| Data | Documentation, translations, simple data, tooling syntax | Scoped lightweight formatting, JSON/PowerShell syntax, diff checks; use owning pack/data validators when needed |
| Focused | Small Java feature or fix | Scoped Spotless, affected compilation, explicitly selected tests; no SonarLint or Qodana |
| Analysis | Substantial logic changes or refactoring | Focused checks plus SonarLint for selected files/source sets |
| Deep | Large Java refactor, or requested inspections | Analysis plus one Gradle-managed Qodana scan for selected Java/Kotlin files |
| Release | Branch ports, build setup, shared contracts with broad effects, release verification | Full build and explicitly selected additional artifact tasks |

`-Tests '<class/package pattern>'` selects tests. Focused, Analysis and Deep require tests or an explicit
`-NoTestsReason`; it is not permission to omit useful tests. `-SourceSets` overrides source-set inference,
and `-Artifacts` adds the affected jar tasks. Small shared-contract changes may still require consumer builds.
Do not run a full build or static analysis merely because Java changed. Automatic inspection Stop hooks are disabled by
default; both Codex and Claude Code use the same explicit profiles. See `docs/validation.md` for examples.

Use the Gradle wrapper. Common tasks are `test`, `build`, `runData`, `packsManagerJar`, and
`officialPacksJar`; use `--stacktrace` only to diagnose a failed build. Keep `gradlew` executable.

Choose validation from the changed behaviour and its consumers, rather than running `build`
at the end of every task. `build` assembles and reobfuscates every module, including content packs.

- For ordinary Java logic, AI, or UI changes, compile the affected source sets and run focused
  tests first. For NPC-only changes, use `compileNpcsJava` and `test --tests '<relevant class or package>'`,
  plus the required formatting and scoped static analysis. Changing existing entity behaviour alone
  does not require assembling every pack.
- For changes to registration, networking, resource loading or an artifact's packaging, include
  the relevant checks and artifact tasks (`jar`, `npcsJar`, `packsManagerJar`, or the affected pack's
  jar task). Build packs when their inputs, packaging, or a shared API they consume change; an unchanged
  pack does not need its jar rebuilt merely because another module changed.
- Use full `build` for branch-wide ports, loader setup, shared build/source-set changes, release
  verification, or changes whose effects span modules and cannot be covered confidently by scoped
  checks. Include affected pack consumers when shared signatures, model classes, or content contracts
  change, even when the pack files themselves are unchanged.
- Keep Gradle's required task dependencies. Test filters select tests to execute; they do not prune
  compilation dependencies. Some analysis tasks also need outputs of other source sets. Use
  `--dry-run` if the task graph is unexpectedly broad; do not add blanket `-x compile...` exclusions.
- Batch related Gradle checks into one invocation where practical. Queue Gradle and Qodana through
  the validation coordinator below instead of starting competing invocations for the same checkout.
  In a mixed worktree, scope formatting and analysis to this task's files or validate an isolated
  checkout when unrelated changes prevent checks; preserve unrelated edits and staged content.
- Reuse successful validation while its relevant inputs remain unchanged. After comment, formatting,
  warning-suppression, or documentation-only follow-ups, rerun only the checks affected by that edit;
  do not repeat a full test suite or rebuild all artifacts automatically. A commit-only request does
  not require repeating previously completed checks.

- Let Gradle reuse up-to-date outputs; do not routinely clean, rebuild, change build directories, or disable caching.
  Finish related edits, then batch checks once. After a finding fix, rerun the affected check and its necessary
  dependencies rather than restarting the whole profile. Do not automatically retry failed infrastructure;
  report the failure, diagnose a concrete cause, and retry once only if that cause was corrected.
- Read the compact result first. Full logs and reports stay on disk; open only the relevant diagnostic excerpt
  on failure. Group duplicate findings and distinguish introduced issues from pre-existing ones before editing.
  Do not turn validation into unrelated warning cleanup. Do not repeatedly poll tools or reread guides to fill
  waiting time. Use bounded waits and meaningful progress updates.

Before completion, review the scoped diff and complete the chosen profile's relevant checks. Report the profile,
outcome and material validation gaps concisely. Do not list every heavier check intentionally excluded by that profile.

### Parallel tasks and shared build outputs

- Prefer a separate Git worktree per concurrent implementation task. Each task owns its source edits,
  `.gradle`, `build`, generated resources, inspection results and temporary logs. Do not redirect
  different worktrees to the same build or project-cache directory. The normal shared Gradle user home
  and dependency cache can remain shared; do not create separate dependency caches by default.
- Within one checkout, run agent build and inspection commands through
  `powershell -NoProfile -File scripts/run-validation.ps1 Gradle <tasks and options>` or
  `powershell -NoProfile -File scripts/run-validation.ps1 Qodana <inspection options>`.
  The coordinator holds a checkout-specific mutex for the entire invocation. Different worktrees have
  independent queues. Gradle's parallelism inside one invocation remains enabled.
  `-WaitSeconds` limits queue waiting (default 600 seconds); `-TimeoutSeconds` limits execution
  (default 1800 seconds for Gradle and Qodana). Both options precede the tool name.
  Status on stderr identifies the owner and reports progress every 30 seconds (`-StatusSeconds` overrides it).
  Use `-Task '<user task>'` to label low-level commands. Output defaults to Compact: full logs and a structured
  `result.json` stay on disk; `-OutputMode Json` returns a run envelope and `-OutputMode Full` streams raw logs.
  `scripts/run-validation.ps1 Status` shows the current task, last observed Gradle task and output age.
  `scripts/run-validation.ps1 -RunId <id from Status> Cancel` requests cancellation of that exact invocation.
  Quiet output is a liveness observation, not proof of progress or a hang. Keep periodic waiting output out of
  model reasoning; the scripts supervise processes without an agent loop.
  The coordinator contains each invocation in a Windows job and stops its descendants before releasing the
  queue. Gradle runs with `--no-daemon` so timeout cancellation cannot leave work in a shared daemon;
  batch related checks to amortize startup. Logs remain under `.gradle/validation-runs/`.
  Check coordinator changes with `powershell -NoProfile -File scripts/test-validation-coordinator.ps1`;
  its process tests use fake tools and launch neither Minecraft nor IntelliJ.
- Direct `gradlew` and internal `qodana-inspect.ps1` launches, IDE auto-builds, and source edits bypass the queue.
  `idea-inspect.ps1` and the coordinator's `Idea` tool are compatibility aliases for queued Qodana scans.
  Coordinate those explicitly or use another worktree. Do not edit, format, clean, or regenerate a
  checkout while another task validates it. A queued command sees the files present when it starts;
  it does not capture or freeze a source snapshot.
- Never delete another task's outputs/caches, remove Gradle lock files, run global `gradlew --stop`,
  or terminate processes by name to resolve contention. If cancelling a coordinator, identify its
  own child processes as well and ensure they have stopped before another validation starts;
  an orphaned child can outlive the shell that held the mutex. Do not cancel another task's work.
- Use unique task-specific temporary files and log names. On machines running several worktrees,
  reduce workers for a heavy invocation if needed (for example `--max-workers=2`) rather than
  changing project-wide parallelism or stopping other tasks' daemons.
