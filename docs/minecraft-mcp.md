# Agent-driven Minecraft testing

Use this guide for [on-demand game testing](game-testing.md). After requested
tests or tool repairs, apply its maintenance protocol to retain verified lessons.

For optional headless multiplayer test players, see [Mineflayer](mineflayer.md).
Use real MCP-controlled clients for rendering, GUI and modded-client behavior.

For optional performance captures and agent-readable call trees, see
[spark profiling](spark.md).

[Minecraft Mod MCP](https://github.com/langyo/minecraft-mod-mcp) is an optional
development mod for inspecting and operating a running Minecraft client. It is
not included in published Flan's Mod jars or content packs. Version 0.4.21 is
pinned in `tools/minecraft-mcp`, with a locked Node dependency tree and a SHA-256
check on the downloaded mod.

## Setup and launch

Requirements: Node.js 20+, the checkout's normal Java/Gradle environment, and a
desktop capable of running Minecraft. Run from the checkout root on Windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-minecraft-mcp.ps1 -RegisterCodex
./gradlew.bat runClient -PwithMinecraftMcp
```

The setup installs the bridge with `npm ci`, downloads the mod to the ignored
`build/minecraft-mcp` directory, and registers this version's server in the user's
Codex configuration. Run setup in each checkout. Restart the agent session after
registering servers so it discovers the tools. The game's existing process must
be restarted with the Gradle flag to load the mod; setup cannot inject it into an
already running JVM. Ordinary launches without the flag remain available.

Claude Code and other agents supporting project `.mcp.json` can use the checked-in
configuration after setup. For another MCP client, use `node` as the command and
the absolute path to `tools/minecraft-mcp/bridge.mjs` as its single argument.
No working-directory assumption is required by the bridge. On other operating
systems, use `npm ci --prefix tools/minecraft-mcp` and the Gradle wrapper, then
register that command in your agent's MCP settings.

| Checkout | Loader | MCP server | Primary port | `runClient2` port |
| --- | --- | --- | --- | --- |
| 1.20.1 | Forge | `minecraft_1_20_1` | 9876 | 9874 |
| 1.21.1 | NeoForge | `minecraft_1_21_1` | 9875 | 9873 |

On Forge 1.20.1, setup must prepare the jar before launching with the flag.
ForgeGradle remaps that verified jar through `fg.deobf`; adding the production
jar directly to the development classpath causes SRG-method startup errors.
NeoForge uses the downloaded jar through `localRuntime`.

Both versions may run simultaneously. The adapter verifies the expected Minecraft
MCP mod version, loader and port before every game action and never falls back to another
client. A port conflict may make the upstream mod fall back to a different port;
resolve the conflict and restart the client rather than letting an agent guess.
For a second client, launch `runClient2 -PwithMinecraftMcp` and run a separate
bridge process with `FMU_MCP_PORT` set to that version's second-client port.

## Repeatable UI preflight (Forge 1.20.1)

Use the mapped development companion for this checkout's UI tests. It loads only
with `-PwithMinecraftMcp`; its source set, metadata and resources never enter
published jars. The companion binds loopback port 9976 (9974 for `runClient2`),
and the bridge verifies its protocol, Minecraft version, loader and PID against
the upstream mod before each request. An older running client must be restarted.
Restart the agent's MCP connection to discover newly added tools; the smoke script
starts a fresh bridge and works without restarting the agent session.

```powershell
./gradlew.bat runClient -PwithMinecraftMcp -PmcpTestProfile
# Once startup reaches the title screen, in another terminal:
node tools/minecraft-mcp/smoke.mjs --live --fixture
```

The optional profile uses `run/mcp-test` (`run/mcp-test-2` for the second client),
a 1280×800 initial window and respects the player's saved GUI scale. Override
the initial size with `-PmcpWidth=1920 -PmcpHeight=1080` when testing larger layouts.
`fixture_world` disables pause-on-focus-loss in this isolated profile and creates or reopens the fixed
`mcp-ui-fixture` flat creative world there. `fixture_editor` reuses a tagged NPC,
equips the player with the required CustomNPCs wand, establishes the server's
editing NPC and opens the real Wolff's Mod editor. Fixture operations require
control mode and the isolated profile. They preserve fixture edits between runs;
they do not reset NPC settings or delete saves.

The live preflight requires the title screen. It checks readable widgets, enters
control mode, opens Options and returns using observed widgets, rejects an old
snapshot, validates PNG dimensions against the framebuffer and checks for a
uniform image. With `--fixture`, it also opens the world and editor, waits for
enabled controls, checks Movement → Tactics → Survival and Movement limits,
then saves and quits to title so the test can run again. It accepts the remembered
hub selection on repeat runs. Control mode is released in `finally`, including
after a failed check. Without `--fixture`, no world is opened.

Use these tools for subsequent tests:

- `ui_state`: current screen, unique widget paths, labels and translation keys,
  enabled/visible state, GUI/framebuffer dimensions and a snapshot revision.
- `ui_click`: requires that revision, widget path and `expected_screen`; waits
  for `result_screen` within a bounded timeout. For edits within the same screen,
  poll `ui_state` for the expected control or value as well.
- `ui_capture`: captures the complete framebuffer with dimensions and sampled
  color count. Files must be PNGs directly inside `build/minecraft-mcp/captures`.
  Open the returned image for actual visual verification; these diagnostics alone
  do not prove correct layout or rendering.
- `ui_close`: calls the observed screen's mapped close/save handler and verifies
  the resulting screen. It avoids upstream Escape injection reporting success
  while a text field or custom GUI keeps the editor open.

Prefer structured state polling for progress; take screenshots at visual
checkpoints or on failure. After one failed capture and one recovery attempt,
diagnose the client log, window state and adapter identity before retrying.
Do not repeat ambiguous menu clicks. A startup warning/error screen is a failed
preflight to investigate, rather than a control to dismiss automatically.

If newly registered tools are unavailable in the current agent session, use
the smoke script or POST `{"cmd":"ui_state"}` to
`http://127.0.0.1:9976/api/cmd`. Verify both `/api/status` responses and matching
PIDs first. `FMU_MCP_UI_PORT` overrides the companion port for bridge mock tests;
normal second-client use derives it from `FMU_MCP_PORT + 100`.

## Agent workflow

1. Read this guide when the user requests in-game UI, rendering or gameplay
   validation. Use the checkout's Gradle launcher so the current sources and packs
   are loaded. The adapter deliberately omits upstream account, installation and
   launcher tools, which would start a separate game environment.
2. Call `get_minecraft_status` on the matching server. Check MCP mod version, loader, port
   and PID before proceeding. An offline error is expected until the game starts.
3. On Forge 1.20.1 prefer `ui_state`, `ui_click` and `ui_capture` above. Otherwise,
   inspect with `get_screen_buttons`, `enumerate_widgets`, `get_player_info` and
   `get_world_info`. Use `screenshot_to_file` with an absolute path under this
   checkout's `build` directory and open the returned image with your image tool.
   Some custom Flan GUIs draw controls manually, so widget enumeration may be
   incomplete; use screenshots and coordinates for those controls. In these dev
   environments, upstream button labels may also be blank, IDs may all be zero,
   and widgets may appear twice. Prefer observed indexes or coordinates to IDs.
4. For interactive tests, call `enter_control_mode` directly on the matching
   MCP server and verify that the result contains `"control_mode": true`.
   No manual overlay click or user handoff is required by the pinned mod version;
   direct takeover was verified on both Forge 1.20.1 and NeoForge 1.21.1. The
   upstream tool description and its "ESC > MCP Take Over" error hint are stale;
   the adapter corrects the tool description. If input reports "not in control
   mode", call `enter_control_mode` and check its result before retrying input.
   Report an actual takeover error instead of assuming that a click is needed.
   Inspection is available without taking control. `mcp.background_input=true`
   keeps injected input inside Minecraft rather than moving or confining the
   Windows desktop cursor.
5. Test in a disposable development world. Use `execute_command` to arrange items,
   entities and game mode when useful, then exercise the actual input flow with
   `press_key`, `click`, `use_item`, and the configured mod key bindings. Capture
   the relevant before/after state and check the active profile's `logs/latest.log`
   for errors (`run/mcp-test/logs/latest.log` for the isolated primary client).
6. Finish with `exit_control_mode`. Stop only game processes you started for the
   task; do not kill an existing user's client.

Screenshots and client state are evidence of visible behavior. They do not prove
server authority, multiplayer synchronization, exact weapon timing or vehicle
part HP. Keep relevant unit/GameTests and dedicated-server checks for those.

The upstream status `version` field is the MCP mod version (0.4.21), not the
Minecraft version. The adapter adds `configuredMinecraftVersion` from this
checkout's target file; this is configuration, not a runtime version probe.

## Tested recovery lessons

These lessons were verified with the Forge 1.20.1 mapped companion; upstream
and other-version limitations still require their own checks.

| Failure or misleading observation | Recovery and verification |
| --- | --- |
| Offline status while Gradle is still starting Minecraft | Poll the pinned status endpoint and active log with a deadline. Run live preflight after the title screen is ready; investigate startup errors instead of treating them as gameplay failures. |
| Forge warns that `mcpDev` has no valid resource pack info | Keep `src/mcpDev/resources/pack.mcmeta` with the branch's pack format. Restart the owned client and verify the warning is gone; do not merely dismiss it. |
| Upstream labels are blank or widget IDs repeat | Use mapped `ui_state` labels, translation keys and snapshot/widget references. Do not guess from a stale button index. |
| A menu action returns success but the expected page is absent | Use `ui_click` with a resulting screen, plus widget/value assertions for changes within `GuiWolffsMod`. Refresh state after changes; old snapshots are deliberately rejected. |
| NPC editor opens with every control disabled | The fixture needs the server's editing NPC and the CustomNPCs wand in hand. `fixture_editor` supplies both; wait for enabled hub controls before testing. |
| A repeated run times out waiting for Movement to become enabled | The editor remembers its selected hub; the selected tab is disabled by design. Accept an already selected hub and assert its page controls instead of clicking it again. |
| Upstream `close_screen` reports success while the editor stays open | Use mapped `ui_close`; its `onClose` path runs close/save hooks. Assert the resulting screen before continuing cleanup. |
| A reopened flat fixture prompts for the Apocalypse dimension | `fixture_world` records “without Apocalypse” for its fixed disposable world before loading it. Do not apply this fixture policy to a user's world. |
| The background test pauses immediately after joining | The isolated fixture disables `pauseOnLostFocus`. Verify the world advances without repeatedly dismissing Pause. Keep the window restored for capture. |
| Controls look unlike the player's setup | Respect saved GUI scale and report actual window/framebuffer sizes. This session used scale 4 at 3440×1351; that is evidence for that layout, not a universal default. |
| A screenshot saves but is cropped, black or stale | Prefer mapped `ui_capture`, check dimensions and image diagnostics, then open the image. After one failed capture and one recovery, diagnose before retrying; use the native fallback below only when needed. |
| Restarting cannot open the previous redirected log file | Use a distinct log filename while the previous launcher still owns its file. Stop only identified processes you started. |

The final reused-world NPC editor smoke passed twice consecutively in about
6–7 seconds per run on this machine, after startup. Treat this as a local
baseline, not a performance threshold for other hardware or scenarios.
The lessons above belong here rather than being copied into each testing skill.

## Screenshot limitations and native fallback

Use mapped capture on Forge 1.20.1 first. For upstream/other-version capture,
keep the game window restored, even when it is behind other apps. In testing,
a minimized 1.21.1 window produced a black MCP screenshot. Restoring it enabled
capture, but the upstream image could still be cropped to stale/default dimensions.
Always inspect the actual image; a successful file save does not prove that it
captured the complete scene. For reliable visual verification on this Windows
machine, use the available Computer Use skill/plugin to select the Minecraft
window and capture its state. Select the exact version's window, restore it if
minimized, and refresh before input. This native capture was verified on 1.21.1.
Agents without that capability should report the visual limitation rather than
claiming that a black/cropped screenshot validates rendering.

## Diagnostics and fallback without MCP tools

```powershell
Invoke-RestMethod http://127.0.0.1:9876/api/status # 9875 for 1.21.1
node tools/minecraft-mcp/smoke.mjs
node tools/minecraft-mcp/smoke.mjs --live
```

The first smoke check validates MCP initialization, tool schemas, offline errors,
wrong-environment and companion PID rejection, command routing, stale references
and failed transition reporting using local mock HTTP servers. `--live` runs the
mapped UI preflight described above through the actual MCP transport. Use
`FMU_MCP_PORT` for a second-client test.

If the agent cannot load an MCP server until its next session, it can use the
mod's loopback HTTP API directly. Verify `/api/status` against the table first,
then POST `/api/cmd` with a JSON body such as
`{"cmd":"get_screen_buttons"}` or
`{"cmd":"screenshot_to_file","path":"C:/absolute/checkout/build/mcp.png"}`.
Parameters are top-level fields, not nested under `params`. `/api/events` is a
debug SSE stream, not an MCP transport. The normal MCP entry point is the Node
stdio bridge. Do not expose the HTTP port to the network.

An occupied port, missing Node dependencies, an old game JVM, or the wrong Java
installation are the first things to check after a connection/startup failure.
Re-run setup after `gradlew clean`; downloads are cached only under `build`.
When updating the upstream version, update both target files, their release
SHA-256 hashes, `package.json`/lockfiles, and repeat mock and live smoke checks.
