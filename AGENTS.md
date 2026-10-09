# AGENTS.md

Nested `AGENTS.md` files add rules for their directories.

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

- Keep gameplay state server-authoritative. Do not initialize client-only classes
  from common or server code.
- Preserve deterministic legacy-content loading and compatibility. Do not bulk-change
  definition whitespace, casing, filenames, encodings, or layouts.
- Do not edit generated output, runtime files, or generated metadata; change the
  generator or its input instead.
- Keep the main mod, bundled packs, and official packs as separate artifacts. Run
  `packsManagerJar` and/or `officialPacksJar` when their inputs or packaging change.
- For every feature or significant change, check whether the locally available wiki
  repository should be updated too.
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

## Mixins

Mixins live in `src/main/java/com/flansmodultimate/mixin` (config
`src/main/resources/flansmodultimate.mixins.json`) and `src/npcs/java/com/wolffsmod/npcs/mixin`
(config `src/npcs/resources/wolffsmodnpcs.mixins.json`).

- At most one mixin class per target class, across every source set and mixin config. Add new
  injections, shadows, and accessors to the existing class for that target instead of creating
  another. A class with a multi-target `@Mixin({A.class, B.class})` owns all of those targets.
- Name a new mixin after its target (`EntityMixin`, `EntityRendererMixin`); `*Accessor` is for a
  class that only declares `@Accessor`/`@Invoker` methods. Older feature-named mixins
  (for example `NpcArmorControlsMixin`) keep their names.
- Keep each class's Javadoc listing what it does for its target, so merged hooks stay findable,
  and group the hooks by feature inside the class.
- Register every mixin in its config: `mixins` for targets present on both sides, `client` for
  client-only targets. A mixin whose target is common but that also hooks a client-only member
  (for example `@OnlyIn(Dist.CLIENT)` methods) stays in `mixins` and marks that injector
  `require = 0`, with a comment saying why.
- Prefix handler methods, `@Unique` members, accessor/invoker methods, and methods of
  interfaces a mixin adds to its target with the mod id, without `$`, so they cannot collide
  with the target or other mods: `flansmodultimateCollideWithDriveableHulls`,
  `wolffsmodnpcsShieldCooldown`, and `WOLFFSMODNPCS_DISTANCE_DATA` for `@Unique` constants.
- IntelliJ and SonarLint misread mixin code. Suppress such false positives with
  `@SuppressWarnings` on the class or member plus a short reason, for example `DataFlowIssue`
  on `(Target) (Object) this` casts, `java:S1905` on a required `(Object) this instanceof`
  cast, or `FieldCanBeLocal` on `@Shadow` fields.

## External Reference Paths

These sibling directories live outside this repository, next to it in the parent
directory. Read them directly when behaviour, formats, or prior implementations need
checking. They are read-only references: never edit them unless explicitly asked.

- `../Flans-Mod-Ultimate-2.0.wiki` - this project's wiki repository. Consult it for
  documented behaviour, and update it for every feature or significant change.
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
  160-column right margin. Wildcard imports are allowed. Import groups, in order:
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
  editing Java, `.gradle`, `.properties`, `.gitignore`, or `.md` files, run
  `gradlew spotlessApply`, then `gradlew spotlessCheck`. Use `// spotless:off` /
  `// spotless:on` only for deliberately hand-aligned code. Never reformat files you
  did not otherwise change.
- SonarLint runs through Gradle with one task per source set (`sonarlintMain`,
  `sonarlintTest`, `sonarlintPacksmanager`, ...; all are part of `check`). Run the
  tasks for the source sets you touched. Introduce no new issues in touched code and fix existing ones only when
  they are in scope. Notable rules: no `volatile` on non-primitive fields (S3077; use
  `AtomicReference` or similar instead).
- When code is required but a SonarLint or IntelliJ check flags it, keep the code and add a
  narrowly scoped `@SuppressWarnings` with a short reason, so the code is visibly intentional.

## IntelliJ Inspections

IntelliJ editor warnings are available headlessly through
`scripts/idea-inspect.ps1`, which runs the local IDE's `inspect.bat` with the shared
profile `.idea/inspectionProfiles/Project_Default.xml` (works while the IDE is open).

- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/idea-inspect.ps1` inspects
  files with uncommitted changes; `-Path <dir>` inspects a directory instead;
  `-MinSeverity WARNING` hides weak warnings; `-Json` gives machine-readable output.
- Output lines are `file:line: [SEVERITY] InspectionId: message`. A run takes one to a few
  minutes; give it a 10-minute timeout and run it once per task, not after every edit.
- After editing Java, run it on the changed files, and fix warnings you introduced in touched
  code. Pre-existing warnings are fixed only when in scope; report remaining ones you chose
  to leave. Treat `LombokGetterMayBeUsed`-style suggestions per the Lombok rule above.
- When an inspection conflicts with SonarLint (for example `PointlessBooleanExpression` on
  `Boolean.TRUE.equals(...)`, which Sonar java:S5411 requires), keep the Sonar-compliant code.
- If IntelliJ is not installed (`IDEA_HOME` unset and not found), say that this check could
  not be performed.

## Build and Validation

Use the Gradle wrapper. Common tasks are `test`, `build`, `runData`, `packsManagerJar`, and
`officialPacksJar`; use `--stacktrace` only to diagnose a failed build. Run focused
tests first. Run a full build after loader setup, registries, networking, entities,
resources, source sets, or packaging changes. Keep `gradlew` executable.

Before completion, review the scoped diff, run relevant checks, `spotlessCheck`,
SonarLint on touched source sets, and `git diff --check`,
and report validation that could not be performed.
