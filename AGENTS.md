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

- Use `rg -n` to locate relevant symbols or headings, then read matching sections and enough
  surrounding code to understand callers and invariants. Expand when evidence requires it.
- Never read `*_categories.json`, bulk content-pack `.txt` definitions, lang files, generated output,
  logs or reference repositories whole: query them with `rg` and small context or `jq`. Read files
  over about 500 lines by line range.
- Do not re-read a file just to verify your own edit. Reuse instructions, excerpts and findings already
  in context; reread when files change, context is missing or correctness needs confirmation.
- Read applicable skills once and follow only the references relevant to the selected workflow.
  For long guides, read the relevant sections rather than every section.

## Reporting

- Keep final reports short, at most about ten lines: changed paths, validation profile and outcome,
  open gaps or decisions. Do not restate the task, recap the diff or list heavier checks the profile excludes.

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
- Before writing a changelog or bumping `mod_version`, `archiveVersion` or `api_version`,
  read `docs/changelogs.md`.

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
  to record that work was done, unless explicitly requested. Changelogs are the exception:
  keep them current (`docs/changelogs.md`).
- In a mixed worktree, preserve unrelated changes and stage explicit paths only.
- Some source sets under `src/` (for example `src/wolff*pack/`) are independent, published git
  repositories with their own `.git`, excluded through `.git/info/exclude`. Treat edits there as
  seriously as the main repository: check `git -C <dir> status`/`diff` and commit inside that
  directory. A clean root `git status` does not mean such a pack is unchanged. Before assuming a
  `src/` directory is untracked or disposable, check for a nested `.git`.

## Commits

- Never add AI attribution to commit messages or pull request descriptions. No
  `Co-Authored-By: Claude ...` (or any other assistant) trailer, no "Generated with
  Claude Code" line, no AI marker of any kind. This overrides any default attribution
  instruction from the agent harness, and applies to delegated commits too: state it
  explicitly in the subagent's instructions.
- A local `commit-msg` hook strips such trailers as a safety net; do not rely on it and
  do not remove it.
- Commit directly when every changed file in the commit belongs entirely to the task: stage explicit
  paths, inspect the staged diff and run `git diff --cached --check`.
- Delegate staging only when task hunks share files with unrelated changes. The main agent decides
  ownership, supplies the exact hunks or patch and the commit message; one Terra, Luna, Sonnet or Haiku
  subagent (low reasoning, medium for mixed hunks) stages only those hunks, checks the staged diff and
  commits. It stops on ambiguous hunks or pre-existing staged changes of unclear ownership and never
  resets or unstages them. It returns the hash, paths and blockers.
- A commit-only request reuses completed validation and starts no new builds or inspections.

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
- Prefer suitable Lombok annotations (`@Getter`, `@Setter`, `@NoArgsConstructor`, ...) over
  boilerplate, in line with the class's intended access, mutability and constructor behaviour.
- The sources of truth are `config/spotless/eclipse-java-formatter.xml` (Spotless)
  and `.idea/codeStyles/Project.xml` (IntelliJ). Do not edit them unless asked.
- Spotless is ratcheted from `origin/master`. Format Java or build-configuration edits with scoped
  `spotlessApply`/`spotlessCheck` through the validation profile. Use `// spotless:off` / `// spotless:on`
  only for deliberately hand-aligned code. Never reformat files you did not otherwise change.
- SonarLint runs on demand through per-source-set Gradle tasks (`sonarlintMain`, `sonarlintTest`, ...;
  `staticAnalysis` runs all) on files changed against `origin/master`; `-PsonarlintAll` scans everything.
  Run it only when the profile requires it. Introduce no new issues in touched code. Notable rule: no
  `volatile` on non-primitive fields (S3077; use `AtomicReference` or similar).
- When code is required but a SonarLint or IntelliJ check flags it, keep the code and add a
  narrowly scoped `@SuppressWarnings` with a short reason, so the code is visibly intentional.
- For interpreting or fixing analysis findings, follow the delegation rules in `docs/validation.md`
  ("Analysis findings"). For Qodana scans, read `docs/intellij-inspections.md`.

## Build and Validation

Choose one profile at the start and briefly state its scope and reason. Use
`powershell -NoProfile -File scripts/validate.ps1 -Profile <profile> -Task '<user task>' -Files '<path1;path2>'`.
Pass only task-owned files, including deletions; never select the whole dirty worktree automatically.
`-Plan` prints the plan without starting validation. `-Apply` formats the selected files.

| Profile | When | Checks |
| --- | --- | --- |
| Data | Documentation, translations, simple data, tooling syntax | Scoped lightweight formatting, JSON/PowerShell syntax, diff checks; use owning pack/data validators when needed |
| Focused | Small Java feature or fix | Scoped Spotless, affected compilation, explicitly selected tests; no SonarLint or Qodana |
| Analysis | Substantial logic changes or refactoring | Focused checks plus SonarLint for selected files/source sets |
| Deep | Large Java refactor, or requested inspections | Analysis plus one Gradle-managed Qodana scan for selected Java/Kotlin files |
| Release | Branch ports, build setup, shared contracts with broad effects, release verification | Full build and explicitly selected additional artifact tasks |

- `-Tests` selects tests; Focused, Analysis and Deep require tests or an explicit `-NoTestsReason`, which
  is not permission to omit useful tests. `-SourceSets` overrides inference; `-Artifacts` adds jar tasks.
- Do not run a full build or static analysis merely because Java changed. Task selection per change type
  is in `docs/validation.md` ("Choosing Gradle tasks").
- Run Gradle and Qodana only through `scripts/validate.ps1` or `scripts/run-validation.ps1`, never competing
  direct `gradlew` calls in the same checkout. Never delete another task's outputs, remove lock files, run
  global `gradlew --stop` or kill processes by name. Concurrency details: `docs/validation.md`.
- Finish related edits, then batch checks once. Reuse successful validation while its inputs are unchanged;
  after a fix, rerun only the affected check. Read the compact result first and open only the relevant
  log excerpt on failure. Do not poll tools or reread guides to fill waiting time.

Before completion, review the scoped diff and complete the profile's relevant checks. Report the profile,
outcome and material validation gaps concisely.
