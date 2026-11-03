# IntelliJ inspections

IntelliJ editor warnings are available headlessly through
`scripts/idea-inspect.ps1`, which starts the local IDE headlessly once per call with the shared
profile `.idea/inspectionProfiles/Project_Default.xml` (works while the IDE is open and
alongside other runs). The Minecraft Development plugin is taken from the local IntelliJ
installation when present, so mixins and Minecraft APIs are understood; its
`AddedMixinMembersNamePattern` is switched off, because it contradicts the shared mixin naming rule.

- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/idea-inspect.ps1` inspects
  files with uncommitted changes; `-Path <dir>` inspects a directory instead;
  `-MinSeverity WARNING` hides weak warnings; `-Json` gives machine-readable output.
- Finish source edits and `spotlessApply` before inspecting. Start the inspection through the
  validation coordinator described in the root `AGENTS.md` and read its result before reporting. It may run in the
  background, but queues behind validation of the same checkout. Run it once per task, not after
  every edit, and do not edit inspected sources while it runs.
- By default the whole-project inspections (duplicated code, unused declarations, spelling and
  grammar, vulnerable libraries, Javadoc) are disabled: they analyse the whole dependency graph
  however narrow the scope is. `-Full` adds them back; use it when the question
  really is about dead code or duplication.
- Every run indexes the project and takes one to a few minutes, so give it a 10-minute timeout. Keeping
  the index between runs is not an option: with the indexes of an earlier run in place the headless IDE
  analyses the file snapshot they were built from, and reports warnings for content that has since been
  edited. What does carry over is the result cache under `build/idea-inspect/cache`, so re-running on an
  unchanged working tree answers instantly; `-Rebuild` ignores it.
- Capture the launched inspection's process ID and command when starting it, and enforce the timeout
  on that process only. Verify its parent command and run directory before stopping it; another
  inspection or the user's open IDE may be running alongside it. Report index failures or timeouts
  as incomplete validation instead of repeatedly starting fresh inspections during the same task.
- Output lines are `file:line: [SEVERITY] InspectionId: message`, followed by the problem count and
  how the answer was produced (`cached`, `warm index`, `cold index`).
- For large Java changes, run it on the changed files, and fix warnings you introduced in touched
  code. Pre-existing warnings are fixed only when in scope; report remaining ones you chose
  to leave. Treat `LombokGetterMayBeUsed`-style suggestions per the Lombok rule above.
- When an inspection conflicts with SonarLint (for example `PointlessBooleanExpression` on
  `Boolean.TRUE.equals(...)`, which Sonar java:S5411 requires), keep the Sonar-compliant code.
- If IntelliJ is not installed (`IDEA_HOME` unset and not found), say that this check could
  not be performed.
- `scripts/idea-inspect-hook.ps1` can wrap the check as a Claude Code `Stop` hook, but it runs
  on all changed Java. Use it only when that behavior is wanted; do not register it by default.
