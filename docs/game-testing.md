# On-demand game testing

Use the repository's [Flans Game Testing skill](../.agents/skills/flans-game-testing/SKILL.md)
when the user requests game testing, a reproduction in Minecraft, multiplayer
verification or runtime profiling. Ordinary implementation tasks do not trigger
this workflow or start clients, servers, bots, GameTests or profiler captures.
Required formatting, compilation and unit checks remain governed by `AGENTS.md`.
Documentation maintenance does not authorize a new live run or scheduled testing.

## Choose the evidence

Define what the player or server should observe, how the fixture triggers it,
and what constitutes completion or failure before operating the game. Separate
test-infrastructure readiness from the feature assertion.

| Evidence needed | Preferred route | What it does not prove |
| --- | --- | --- |
| Deterministic logic or parsing | Focused Gradle unit tests | Real entity lifecycle or client presentation |
| Entity lifecycle, server ticks and navigation | Headless GameTests | Client rendering, menus or multiplayer synchronization |
| GUI, rendering, animation, audio, riding and prediction | [Real Minecraft client](minecraft-mcp.md) | Server authority or a second client's observations |
| Dedicated-server multiplayer and packet outcomes | [Mineflayer](mineflayer.md), with real clients for Java client features | Modded-client rendering or custom physics from bot prediction |
| Performance under a repeatable workload | [spark](spark.md) | GPU time from Java stacks, or causal improvement from percentages alone |

Use the smallest combination covering the requested behavior. Successful UI
preflight proves the test controls work; it is not a general gameplay regression
suite. Opening an editor does not prove settings loaded, saved or reached the
server. Sending a packet does not prove its handler accepted or applied it.

## Prepare and reuse a scene

Check the actual checkout, pending changes, Java/loader configuration and target
files before reusing a process. Confirm matching status and PID. A process built
before a Java edit cannot validate that edit; installing an MCP tool does not
inject its mod into a running JVM. Do not launch a second copy just because a
new agent session cannot discover the existing tool connection: the documented
HTTP or stdio fallback may already provide access.

Use an isolated world or server. Arrange the feature's real prerequisites,
including equipment, permissions, ammunition, content packs and editing context.
Keep game mutations server-authoritative. Do not repurpose a user's save or
weaken normal gameplay/network validation to make a test pass. Check nested pack
repositories when identifying the source of a scene's items or models.

For GUI tests, follow the [mapped UI preflight](minecraft-mcp.md#repeatable-ui-preflight-mapped-companion).
The reusable NPC editor fixture is one example, not a required scene for unrelated
features. The profile respects saved GUI scale; choose a representative window
size and report actual dimensions. Its fixed world preserves edits between runs,
so reused state is intentional, not a clean reset.

Poll readiness rather than sending clicks during startup or sleeping for an
assumed duration. Preserve logs with the active run directory: the isolated
client uses `run/mcp-test`, GameTests use `run/gametest`, and Mineflayer uses
`build/mineflayer/server`. A different profile's `run/logs/latest.log` can be stale.
Use distinct log filenames for restarts while a previous process holds its file.

## Headless server scenarios

Inspect this checkout's `gameTestServer` run and source-set wiring before use.
On both maintained branches, the NPC test module is loaded only by:

```powershell
./gradlew.bat runGameTestServer --console=plain
```

NPC tests live in `src/npcGameTest/java`; their templates live in
`src/npcGameTest/resources/data/flansmodultimate/structure` on 1.21.1. See the
[version differences](minecraft-version-code-differences.md#custom-npcs-headless-gametests)
for the CustomNPCs save-directory seam and the singular `structure` directory
on 1.21.1. Do not assume another module already has this test wiring.

Assert the real behavior over sufficient ticks with a timeout. For example,
starting a retreat goal is different from reaching a safer position while
retaining the target. Control random inputs and starting geometry where the
scenario depends on them. Use actual fixtures and source-set membership rather
than inventing Gradle filters or mixing JUnit `--tests` with GameTest discovery.
Check both the process result and test summary; inspect failures in the active
profile's log. Keep development-only fixtures out of published artifacts.

## Evidence and recovery

Prefer structured state for navigation and waiting. Save and open an image when
evaluating visible layout or rendering. Validate full framebuffer dimensions;
a saved PNG or nonuniform color sample alone is not visual proof. For exact UI
recovery limits and known failures, use the [MCP guide](minecraft-mcp.md#tested-recovery-lessons).

Bound polls and retries. After a failed action, inspect fresh state and the
relevant new log output, classify the failure, and make a justified recovery.
Do not repeat an unchanged failing action or restart unrelated clients. Distinguish
fixture errors, tool errors and product defects in the result. Preserve useful
evidence before restarting a process you own. Honor existing user authorization;
if a mandatory tool gate blocks progress, state what was blocked and exactly
what the user can do to unblock it.

For an infrastructure repeatability fix, run the affected complete scenario twice
against reused state after the fix, when that game testing is within the user's
request. A fresh JVM passing once can hide remembered UI state, saved settings,
missing equipment or cleanup failures. Do not rerun broad suites merely to fill
time after the relevant checks pass. Keep evidence local unless sharing is asked.

## Maintaining the workflow

At the end of every requested game-testing or testing-tool repair session:

1. Identify confirmed new failure causes, successful recovery steps, missing
   prerequisites and measured improvements. Keep unverified explanations labeled
   as hypotheses; do not promote them into mandatory rules. Record the relevant
   version/loader/profile when a lesson is environment-specific.
2. Update the owning location in the same task: workflow selection and maintenance
   rules here; client controls and recovery in `minecraft-mcp.md`; bot protocols
   and multiplayer setup in `mineflayer.md`; capture/analysis in `spark.md`;
   cross-version seams in `minecraft-version-code-differences.md`. Update the
   skill only for changes to activation, routing or decision rules. Prefer fixing
   reusable tooling or fixtures when prose alone would leave agents repeating
   fragile manual work.
3. Replace stale advice in place, consolidate duplicates and remove workarounds
   superseded by a verified fix. Keep durable cause → remedy → verification
   guidance, not a chronological failure diary, raw output or transient PIDs.
   Read only the relevant guide sections on the next task.
4. Validate changed instructions against current source and available tools. For
   tooling changes, run focused offline checks first, then the authorized live
   scenario when needed. For documentation-only changes, validate links, skill
   metadata, command references and formatting without launching Minecraft.
   Follow repository checks appropriate to any code or packaging edits.
5. Report useful retained lessons, tests performed and unresolved limitations
   briefly. If there is no new reusable lesson, leave the guides alone; do not
   manufacture documentation changes merely to record a successful session.

This maintenance is part of the requested session, not a background job. No
automatic game runs, future schedules or additional per-action approval rituals
are implied. A user-requested narrower test scope still controls the task.
