# Agent-driven Minecraft testing

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

## Agent workflow

1. Read this guide when a task benefits from in-game UI, rendering or gameplay
   validation. Use the checkout's Gradle launcher so the current sources and packs
   are loaded. The adapter deliberately omits upstream account, installation and
   launcher tools, which would start a separate game environment.
2. Call `get_minecraft_status` on the matching server. Check MCP mod version, loader, port
   and PID before proceeding. An offline error is expected until the game starts.
3. Inspect with `get_screen_buttons`, `enumerate_widgets`, `get_player_info` and
   `get_world_info`. Use `screenshot_to_file` with an absolute path under this
   checkout's `build` directory and open the returned image with your image tool.
   Some custom Flan GUIs draw controls manually, so widget enumeration may be
   incomplete; use screenshots and coordinates for those controls. In these dev
   environments, upstream button labels may also be blank, IDs may all be zero,
   and widgets may appear twice. Prefer observed indexes or coordinates to IDs.
4. For interactive tests, activate the mod's MCP overlay/pause-screen control
   button in Minecraft, then call `enter_control_mode`. If the mod requests a
   user handoff, report that concrete requirement. Inspection is available
   without taking control. `mcp.background_input=true` keeps injected input inside
   Minecraft rather than moving or confining the Windows desktop cursor.
5. Test in a disposable development world. Use `execute_command` to arrange items,
   entities and game mode when useful, then exercise the actual input flow with
   `press_key`, `click`, `use_item`, and the configured mod key bindings. Capture
   the relevant before/after state and check `run/logs/latest.log` for errors.
6. Finish with `exit_control_mode`. Stop only game processes you started for the
   task; do not kill an existing user's client.

Screenshots and client state are evidence of visible behavior. They do not prove
server authority, multiplayer synchronization, exact weapon timing or vehicle
part HP. Keep relevant unit/GameTests and dedicated-server checks for those.

The upstream status `version` field is the MCP mod version (0.4.21), not the
Minecraft version. The adapter adds `configuredMinecraftVersion` from this
checkout's target file; this is configuration, not a runtime version probe.

## Screenshot limitations and native fallback

Keep the game window restored, even when it is behind other apps. In testing,
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
wrong-environment rejection and command routing using local mock HTTP servers.
`--live` checks the configured game's status, ping, GUI query and screenshot save
through the actual MCP transport. Use `FMU_MCP_PORT` for a second-client test.

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
