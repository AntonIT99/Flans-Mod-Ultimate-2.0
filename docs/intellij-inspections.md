# Qodana inspections

The `scripts/idea-inspect.ps1` compatibility entry point runs JetBrains Qodana through the
`qodanaScan` Gradle task. Qodana uses the native Windows JVM Community linter, the shared project
profile `.idea/inspectionProfiles/Project_Default.xml`, and a managed download of the Minecraft
Development plugin. It does not use a local IntelliJ installation or upload reports to Qodana Cloud.

```powershell
powershell -NoProfile -File scripts/run-validation.ps1 -Task 'Feature inspection' Qodana -Files 'src/main/java/com/flansmodultimate/platform/PlatformEnvironment.java'
```

The first explicit scan downloads the native engine and plugin and indexes the imported dependencies.
Subsequent scans reuse the checkout-local cache, while writing a fresh SARIF report for every invocation.
Ordinary Gradle tasks neither load the Qodana plugin nor download its engine. `qodanaScan` is not attached
to `check` or `build`. Direct Gradle use remains available through the coordinator's Gradle tool, but the
Qodana tool adds scope checks, timeouts, unique report directories and SARIF validation.

- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/idea-inspect.ps1` inspects changed
  Java or Kotlin files; `-Path <dir>` scopes to a directory; `-Files <file1,file2>` accepts explicit
  source files; `-MinSeverity WARNING` hides weak warnings; and `-Json` returns a JSON array.
- Finish source edits before inspecting. Run the command through the validation coordinator described
  in the root `AGENTS.md`, and read its result before reporting. It queues behind validation for this
  checkout. Do not edit inspected sources while the scan runs.
- Whole-project inspections for duplicate code, unused declarations, spelling and grammar, vulnerable
  libraries, and Javadoc are disabled by default because they are expensive across the full project.
  `-Full` enables them. `AddedMixinMembersNamePattern` remains disabled because it conflicts with the
  repository's mixin naming convention.
- Each invocation writes reports under its unique `build/qodana/inspect/<run-id>/results` directory.
  Qodana's engine cache is shared under `build/qodana/cache`; `-NoCache` and `-Rebuild` use a private
  cache for that invocation. The Gradle launcher uses a separate project-cache directory so Qodana's
  Gradle import does not contend with the scan task's project-cache lock. Dependencies still use the
  normal Gradle user home.
- The worker rejects a nonzero scan, a missing or malformed SARIF report, and any report that does not
  mark its invocation successful. These failures are incomplete validation. Findings are returned for
  review without making a successful scan task fail. The worker prints at most 2,000 findings while the
  JSON array includes the complete filtered result set.
- The worker timeout defaults to 1,200 seconds; the coordinator limit defaults to 1,800 seconds. If
  raising the worker timeout, pass `-InspectionTimeoutSeconds` and raise the coordinator's `-TimeoutSeconds`
  before the tool name as well. Native project import may invoke
  Gradle tooling outside the worker's process tree, so cancellation can stop the owned scan processes
  without guaranteeing that an already-created Gradle daemon ends immediately.
- Run inspections for large Java changes or when requested, passing `-Files` with task-owned source
  paths. Small features should use an affected-source compile and focused tests; they do not need
  inspections automatically. Fix warnings introduced in touched code. Pre-existing warnings are fixed
  only when in scope; report remaining ones you chose to leave. Treat `LombokGetterMayBeUsed`-style
  suggestions per the Lombok rule in `AGENTS.md`.
- When an inspection conflicts with SonarLint (for example `PointlessBooleanExpression` on
  `Boolean.TRUE.equals(...)`, which Sonar java:S5411 requires), keep the Sonar-compliant code.
- Do not register `scripts/idea-inspect-hook.ps1` as an automatic Claude Code `Stop` hook. Inspections
  are task-scoped and run only for large Java changes or when requested.
