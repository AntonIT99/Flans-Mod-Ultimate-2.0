---
name: flans-game-testing
description: Run explicitly requested Minecraft game tests (real client, GameTests, Mineflayer, spark) and maintain testing guides; never for ordinary implementation.
---

# Flans Game Testing

## Activation and scope

Use only when the user requests game testing, reproduction in Minecraft,
multiplayer verification, a runtime performance investigation, or invokes this
skill. Creating or maintaining the skill does not authorize a live test run.
Do not launch clients, servers, bots, GameTests, or captures merely because code
changed. Normal formatting, compilation and unit-test requirements still apply.

Use the user's existing authorization for routine fixture setup and local test
actions within the requested scope. Do not ask again for each MCP request. If a
tool blocks an action, report the exact action, stated reason and concrete user
step needed to unblock it; do not claim standing permission overrides tool gates.

Read [the shared testing guide](../../../docs/game-testing.md) and the local
`AGENTS.md` rules. Inspect the current branch, dirty worktree and applicable run
configuration before editing or starting processes. Preserve earlier work and
existing user sessions. Repository-wide means all mod modules and content packs;
do not assume every feature uses the NPC editor fixture.

## Choose evidence and load only its guide

State the scenario, expected observable outcome, fixture and bounded completion
condition. Choose the least expensive mode that can establish that outcome:

| Question | Mode and required guide |
| --- | --- |
| Pure decision, parser or calculation behavior | Focused unit tests; no game launch |
| Real entity lifecycle, navigation or tick behavior | Headless GameTests; shared guide's server section |
| Client GUI, rendering, sound, animation or modded vehicle physics | Real client; [Minecraft MCP](../../../docs/minecraft-mcp.md) |
| Server authority or behavior observed by multiple players | Dedicated server and [Mineflayer](../../../docs/mineflayer.md), adding real clients when Java client behavior matters |
| Measured runtime cost | [spark](../../../docs/spark.md); repeat a warmed, comparable workload |

Read only the selected mode's guide and the relevant source definitions or test
fixtures. Version-dependent implementation or tooling fixes also use
`../flans-version-porting/SKILL.md`. Verify this checkout's targets and capabilities;
Forge 1.20.1 mapped UI tools are not evidence of support on another branch.

## Execute and verify

1. Reuse a matching ready process when it has the right code and test isolation.
   Check the matching status endpoint before action; match version, loader, port
   and PID. New classes or launch flags require restarting a process you own.
   Wait for startup readiness with a bounded status/log check before running the
   preflight. A startup warning, wrong identity or unsupported packet is a setup
   failure to diagnose, not a gameplay result.
2. For UI testing, use the mapped preflight and isolated profile in the MCP guide;
   only the Forge 1.20.1 companion has been verified live so far. Prefer direct fixtures over world-selection menus. The NPC
   fixture is appropriate for NPC editor tests; arrange other features in a
   disposable scene suited to their actual requirements.
3. Enter control mode and verify it for client input. Observe structured state,
   act on current widget references, then assert the outcome. Screen transitions
   and changes within a screen need different assertions. Wait for enabled
   controls and loaded data; a screen opening alone is insufficient. Exercise
   save/reopen or the second observer when persistence or synchronization is the
   behavior under test. A command acknowledgement does not prove success.
4. Capture and open images only for visual checkpoints or failures. Use actual
   framebuffer and GUI dimensions; retain the player's chosen GUI scale. Consult
   the MCP guide's bounded recovery procedure instead of guessing coordinates
   or repeating the same screenshot. Fix reproducible infrastructure faults in
   the adapter or fixture, never in downloaded jars or `node_modules`.
5. After a tooling or fixture fix, repeat the smallest failed scenario. For
   repeatability changes, run the complete scenario twice consecutively against
   reused state, accounting for remembered tabs and saved options. Once it passes,
   extend testing only for remaining acceptance criteria or unresolved risks.
6. Release control and disconnect test bots in cleanup paths even after failure.
   Save/quit or stop only the disposable sessions and processes you own, as the
   scenario requires. Stop your own profiler capture. Report passed, failed and
   untested criteria separately, with evidence and any remaining setup blocker.

## Keep the process efficient and current

Use short state/log polls with deadlines rather than long sleeps. When
orchestrating tools, retain full state locally and return only the screen,
snapshot and controls needed for the next assertion. Request log tails or errors
since a saved cursor.
Batch independent reads, keep dependent actions sequential, and summarize
successful checks instead of dumping full responses. Put raw captures and logs
under ignored `build/` or the active run profile. Do not load all testing guides,
reinspect the same image, or recreate working scripts in each session.

Before completing every requested testing or testing-tool repair session, apply
the [maintenance protocol](../../../docs/game-testing.md#maintaining-the-workflow).
It requires reconciling confirmed lessons into the owning guide and correcting
stale instructions. Update this skill only when routing or decision rules change;
do not accumulate session transcripts or duplicate command manuals. No update is
needed when the maintained guidance already covers everything learned. Report
useful updates and unresolved hypotheses in the task result. Maintenance does not
schedule work or authorize additional game runs.
