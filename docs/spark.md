# Agent-driven performance profiling

[spark](https://spark.lucko.me/) is enabled in the default development runs. It is not
embedded in published mod jars or content packs. Each checkout pins its matching
mod and download hash in `tools/spark/target.json`; the analysis MCP dependency
and its transitive dependencies are locked by `tools/spark/package-lock.json`.

## Setup

From each checkout, using its normal Java/Gradle environment and Node.js 20+:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-spark.ps1 -RegisterCodex
./gradlew.bat runClient -PwithMinecraftMcp
```

Prepare Minecraft MCP separately as described in [minecraft-mcp.md](minecraft-mcp.md).
The spark setup installs dependencies with `npm ci --ignore-scripts`, downloads
and verifies the mod, tests the analysis bridge, and optionally registers it in
Codex. Restart the agent session to discover newly registered tools. The checked-in
`.mcp.json` also registers the bridge for project-aware MCP clients.

| Checkout | spark mod | Analysis MCP server |
| --- | --- | --- |
| Forge 1.20.1 | `1.10.53-forge` | `spark_1_20_1` |
| NeoForge 1.21.1 | `1.10.124-neoforge-1.21.1` | `spark_1_21_1` |

Normal `runClient`, `runClient2` and `runServer` configurations load spark,
independently of Minecraft MCP; no profiler flag is needed. Use `-PwithoutSpark`
to omit it for a particular launch. ForgeGradle remaps the pinned Modrinth Maven
artifact; each run depends on `prepareSpark`, which verifies its hash. NeoForge
loads the verified preparation output through `localRuntime`. Gradle resolves and
verifies the mod automatically after `clean`, so plain launches need no separate
setup. The setup script is needed for the Node analysis tools and registration.
Reload the Gradle project in IntelliJ to refresh existing IDE run classpaths.

The analysis bridge uses an absolute installation path internally and needs no
particular working directory. It can inspect saved profiles with Minecraft
offline. It does not start the game or take control of it.

## Capture through Minecraft MCP

1. Read [minecraft-mcp.md](minecraft-mcp.md), call `get_minecraft_status` on the
   matching server, and enter control mode. Use a disposable development world.
   Check `run/logs/latest.log` for spark startup and command responses.
2. Warm up the world and reproduce the workload before capturing. Record the
   checkout/commit, entity counts, scene, JVM settings, sample interval and
   duration so a later capture can repeat them.
3. Check `/spark profiler info` for server work or `/sparkc profiler info` for
   client work. Do not stop or replace someone else's active recording.
4. For each command, call `open_chat`, `type_text` with the complete slash command
   and `press_enter: false`, then `press_key` with `key: "Enter"`. Submit through
   chat even for server work: the upstream `execute_command` helper can bypass
   the client command dispatcher, so it is unsuitable for `/sparkc`.
5. Start the appropriate capture:

   ```text
   /spark profiler start --force-java-sampler
   /sparkc profiler start --thread * --force-java-sampler
   ```

   Use one of these commands, depending on the side being investigated. Verify
   spark's response in the log; an MCP input acknowledgement only proves that
   input was submitted. Keep the world running and the relevant scene visible.
6. After a representative interval (usually 30–60 seconds), stop on the same side:

   ```text
   /spark profiler stop --save-to-file
   /sparkc profiler stop --save-to-file
   ```

   Always stop a recording you started, including when a test fails. Use
   `profiler cancel` to discard it. Avoid automatic `--timeout` captures in this
   local workflow: their completion can upload a report instead of saving it.
7. Call `spark_files` on this checkout's analysis server, then `load_profile`
   with the resulting absolute path. Captures normally live under
   `run/config/spark`; the second client uses `run/second-client/config/spark`.
   The file locator also checks `run/spark` and `build/spark`. A dedicated
   server console can run the same `/spark` commands without Minecraft MCP.
8. Finish with `exit_control_mode`. Keep captures local unless sharing is
   explicitly part of the task. Local saves need no viewer upload.

## Analyze and compare

Call `spark_environment` for this checkout's configuration and capabilities.
The bridge reuses the pinned third-party
[spark-profiler-mcp](https://github.com/Imanity-Software/spark-profiler-mcp)
analysis tools:

- `load_profile` returns the `profileId` used by subsequent tools.
- `list_threads` identifies the recorded threads. Select `Server thread` for
  server ticking and `Render thread` for client rendering explicitly.
- `get_top_self_time`, `get_call_tree` and `search_call_tree` expose hot methods
  and their callers. Limit tree depth and result counts; search for
  `com.flansmodultimate` or `com.wolffsmod` when investigating our code.
- `get_sources_breakdown` groups attributed work by mod, when the profile
  contains source attribution.
- `get_health` and `get_system_stats` expose the profile's recorded tick, CPU,
  memory and GC metrics. Some metrics are absent on clients or in short captures.
- `get_heap_summary` reads saved `.sparkheap` summaries. Check the pinned mod's
  command help before using memory-report flags; command support varies by version.

The upstream `diagnose` tool targets server tuning. Treat its recommendations as
hypotheses, especially for development clients, and inspect the measured methods
before changing code or JVM settings. Java sampling includes waiting and sleeping
frames; the analysis tool's native-frame filter does not remove every Java wait.
Sampled milliseconds are statistical stack weights, not exact per-call timings.

On Windows spark uses its Java sampler. Native async sampling and allocation
profiling require a supported Linux/macOS environment. Client stacks can reveal
CPU-side rendering work but do not measure GPU frame time. Compare warmed-up
captures of the same scenario and duration, with the same sampler and interval;
keep background load comparable and inspect absolute tick/frame behavior as well
as percentages. A large percentage alone does not prove a bottleneck.

## Validation and maintenance

```powershell
node tools/spark/smoke.mjs
node tools/spark/smoke.mjs C:/absolute/path/to/capture.sparkprofile
```

The smoke check tests stdio MCP initialization, tool discovery, checkout-relative
file discovery, gzip decoding and known server/render self-time values. With a
capture path it also reads real sampled threads, hot methods and call trees.

When updating spark, update the target version, URL and hash together and repeat
a live capture on that loader. When updating the analysis package, regenerate
the lockfile and validate real captures from both versions. Do not modify files
inside `node_modules` or downloaded jars to fix compatibility.
