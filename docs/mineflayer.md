# Headless multiplayer tests with Mineflayer

Mineflayer is an optional Node.js test client for the dedicated development
server. It complements the graphical clients controlled through
[Minecraft MCP](minecraft-mcp.md). Bots can move, observe health and inventory,
act as multiplayer targets, and send this checkout's Flan packets. They do not
execute Java client code or validate rendering, audio, animation, GUI behavior,
vehicle prediction, or client-side configuration application.

## Setup and launch

Use Node.js 22+ and this checkout's normal Java installation (17 for Forge
1.20.1, 21 for NeoForge 1.21.1). From each checkout:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-mineflayer.ps1 -RegisterCodex
./gradlew.bat runServer -PwithMineflayer --console=plain
```

Setup installs exact locked dependencies with `npm ci --ignore-scripts`,
prepares the isolated server, and optionally registers the version's stdio MCP
bridge. Restart the agent session after registration. Other MCP clients can run
`node` with the absolute path to `tools/mineflayer/bridge.mjs`; no working
directory assumption is required. The checked-in `.mcp.json` also includes it.

| Checkout | Loader | MCP server | Game port |
| --- | --- | --- | --- |
| 1.20.1 | Forge | `mineflayer_1_20_1` | 25571 |
| 1.21.1 | NeoForge | `mineflayer_1_21_1` | 25572 |

The profile in `tools/mineflayer/server.properties` generates a flat survival
world under `build/mineflayer/server`. It binds to `127.0.0.1` and uses offline
identities, so no Microsoft login or additional account is needed. Both
versions can run simultaneously. The existing `run` world and running clients
are independent. The source sets normally loaded by the checkout remain loaded.
On first preparation, `run/flan` is copied into the isolated server if present;
later changes to those runtime packs require deliberately refreshing that copy.
Changes to packaged source packs follow the ordinary Gradle run.

Preparation copies an already accepted `run/eula.txt`. If none exists, accept
the Minecraft EULA in the isolated server's `eula.txt`, or set
`FMU_MINEFLAYER_EULA` to an already accepted file before setup or launch.
Preparation does not accept the EULA for you. `gradlew clean` removes this
disposable world and the NeoForge runtime export; Node dependencies remain.

On NeoForge only, `-PwithMineflayer` also loads the separate `mineflayerdev`
source set. Once the server starts, it writes `runtime.json` containing the
loaded payload registrations, port, version and PID. This includes channels
from CustomNPCs and other loaded dev modules. The helper uses the pinned
NeoForge registration field because there is no public enumeration API. It is
never included in release jars. Ordinary runs do not load it.

## Agent workflow

1. Read this guide before using the bots. Start the isolated server with the
   checkout's Gradle wrapper and wait for `Done`. Use an interactive terminal
   when console commands will be needed. Never repurpose a user's world.
2. Call `get_server_status` on the matching Mineflayer MCP server. The bridge
   checks the exact Minecraft protocol and the profile's identifying MOTD at
   its fixed loopback address. It does not scan for or select another server.
   `FMU_MINEFLAYER_PORT` explicitly selects a different local port, for example
   for diagnostics; a matching marked server is still required.
3. Use `connect_bot` with a distinct username (1–16 letters, digits or
   underscores). Up to eight bots belong to each bridge. Forge performs the
   FML3 login exchange; NeoForge negotiates from its live runtime export and
   checks that its server process still exists. Both check the Flan protocol
   against the current source and read the pack manifest and registry IDs.
4. Prepare fixtures through the dedicated server console: `give`, `tp`,
   `gamemode`, `damage`, and `data get entity` are useful. Bots are not
   operators by default. `get_bot_state` reports server-sent health and inventory,
   the bot's predicted position, and tracked entities; `get_bot_events` reports health/death,
   disconnects and bounded raw custom-payload observations. Save the returned
   cursor to request only newer events. The history holds 256 events and
   each payload includes at most 1024 bytes plus its original length.
5. Use `control_bot` for bounded movement, look, equip, attack, use, respawn,
   firing and reloading. Angles are radians. Movement and firing require
   `milliseconds` between 1 and 10000. Supply a list of movement `controls`,
   an inventory `slot` for equip, or an `entityId` for attack. Fire and reload
   send actual `PacketGunInput` and `PacketGunReload` requests, with a release
   after firing. Check server state and outcomes; sending a packet alone is
   not proof that its action succeeded.
6. For other Flan actions, `list_mod_packets` exposes the checkout's registered
   packets. Read that packet's Java `encodeInto` method, then use
   `send_mod_packet` with its name and only its body as hexadecimal. The bridge
   derives the correct Forge byte discriminator or NeoForge VarInt envelope.
   For example, `PacketGunScopedState` or `PacketDriveableInput` require their
   own codecs; do not guess a legacy or other-version layout. Restart the
   bridge and server after packet registration or protocol edits.
7. Disconnect every bot with `disconnect_bot` and stop only the server you
   started, using `stop` in its console. Closing the bridge also disconnects
   its bots. Check the isolated server's `logs/latest.log` for errors.

The Node API is also available without MCP:

```javascript
import { createDevelopmentBot, control } from "./tools/mineflayer/bots.mjs";
const session = await createDevelopmentBot("TestTarget");
try {
  await control(session, { action: "move", controls: ["forward"], milliseconds: 500 });
  console.log(session.snapshot());
  // session.bot exposes the upstream Mineflayer API for a scripted scenario.
} finally {
  session.close();
}
```

## Coverage and limits

Inventory observations use server item IDs and resource names, including pack
items. Unknown mod item metadata uses a conservative stack size; use the
server console for fixture quantities rather than relying on vanilla crafting
or stack-size logic. Modded recipe books and Brigadier command trees remain
opaque: this adapter does not validate recipes or provide command completion.
Custom client payloads are observed, not applied by Java handlers. Some modded
entity spawns, data serializers, item components or custom configuration tasks
may need an additional adapter before they can be used in a scenario.

Use vanilla blocks in movement fixtures. Mineflayer's vanilla collision model
does not reproduce custom block shapes, driveable hulls or modded physics.
Use GameTests/server assertions for exact tick timing and internal gameplay
state, and real modded clients for GUI, rendering, riding and prediction checks.
An unsupported decoder or configuration task is a failed test setup, not a
successful game test. Do not relax the mod's normal connection requirements
to let bots in.

## Validation and diagnostics

```powershell
npm test --prefix tools/mineflayer
node tools/mineflayer/smoke.mjs
node tools/mineflayer/smoke.mjs --live
```

The offline checks cover target rejection, malformed loader payloads, Forge
login replies, packet ID uniqueness and the actual stdio MCP transport. The
live smoke requires the matching isolated server and checks modded login,
the content-pack manifest, health, movement synchronized to a second bot,
and disconnect cleanup. It does
not claim to test damage or weapon behavior. If necessary,
`FMU_MINEFLAYER_TRACE=1` prints recent incoming packet prefixes on a decoder
error to stderr. An unavailable server, stale NeoForge runtime export, or
Flan protocol mismatch requires starting/restarting the correct Gradle run.

Upstream: [Mineflayer](https://github.com/PrismarineJS/mineflayer). The bridge
uses Mineflayer and `minecraft-protocol` directly; it does not depend on the
older Forge handshake plugin or claim general modpack compatibility.
