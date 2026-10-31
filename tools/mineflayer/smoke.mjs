import assert from "node:assert/strict";
import { setTimeout as delay } from "node:timers/promises";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import { fileURLToPath } from "node:url";
import { target } from "./environment.mjs";

const client = new Client({ name: "mineflayer-smoke", version: "1.0.0" });
const transport = new StdioClientTransport({ command: process.execPath, args: [fileURLToPath(new URL("bridge.mjs", import.meta.url))], stderr: "inherit", env: { ...process.env } });
async function call(name, args = {}) {
  const result = await client.callTool({ name, arguments: args });
  assert(!result.isError, `${name}: ${JSON.stringify(result)}`);
  return JSON.parse(result.content[0].text);
}
try {
  await client.connect(transport);
  const { tools } = await client.listTools();
  assert(tools.some((tool) => tool.name === "control_bot"));
  assert(tools.some((tool) => tool.name === "send_mod_packet"));
  if (process.argv.includes("--live")) {
    console.log(await call("get_server_status"));
    const suffix = Date.now().toString(36).slice(-5);
    const username = `FMU_S_${suffix}`;
    const observer = `FMU_V_${suffix}`;
    const state = await call("connect_bot", { username });
    assert.equal(state.negotiated, true);
    assert.equal(state.version, target.version);
    assert(state.packs.length > 0, "No content pack manifest received");
    await call("connect_bot", { username: observer });
    await delay(2000);
    const before = await call("get_bot_state", { username });
    const observedBefore = (await call("get_bot_state", { username: observer })).entities.find((entity) => entity.username === username);
    assert(observedBefore, "Second client is not tracking the moving bot");
    assert(before.health > 0);
    await call("control_bot", { username, action: "look", yaw: 0, pitch: 0 });
    const after = await call("control_bot", { username, action: "move", controls: ["forward"], milliseconds: 1000 });
    assert(after.connected);
    assert(Math.hypot(after.position.x - before.position.x, after.position.z - before.position.z) > 0.25, "Bot did not move");
    await delay(300);
    const observedAfter = (await call("get_bot_state", { username: observer })).entities.find((entity) => entity.username === username);
    assert(observedAfter && Math.hypot(observedAfter.position.x - observedBefore.position.x, observedAfter.position.z - observedBefore.position.z) > 0.25,
      "Server did not synchronize movement to the observing client");
    console.log(JSON.stringify({ username: after.username, health: after.health, packs: after.packs.length, registries: after.registries, before: before.position, after: after.position }));
    await call("disconnect_bot", { username });
    await call("disconnect_bot", { username: observer });
    assert.equal((await call("list_bots")).length, 0);
  } else {
    assert.equal((await call("list_bots")).length, 0);
    assert((await call("list_mod_packets")).some((entry) => entry.name === "PacketGunInput"));
    assert((await client.callTool({ name: "get_bot_state", arguments: { username: "Missing" } })).isError);
  }
  console.log(`PASS: ${target.version} Mineflayer ${process.argv.includes("--live") ? "live" : "offline"} MCP smoke`);
} finally { await client.close(); }
