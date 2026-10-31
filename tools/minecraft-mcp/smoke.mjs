import assert from "node:assert/strict";
import { createServer } from "node:http";
import { readFileSync, mkdirSync, existsSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

const target = JSON.parse(readFileSync(new URL("target.json", import.meta.url), "utf8"));
const live = process.argv.includes("--live");
let mode = "wrong";
let commands = 0;
const mock = createServer(async (request, response) => {
  response.setHeader("Content-Type", "application/json");
  if (request.url === "/api/status") {
    response.end(JSON.stringify({ ok: true, type: "minecraft-mod", version: mode === "wrong" ? "other-version" : target.modVersion,
      loader: target.loader, port: mock.address().port, pid: 1, uptime: 1 }));
  } else {
    commands++;
    let body = "";
    for await (const chunk of request) body += chunk;
    response.end(JSON.stringify({ ok: true, request: JSON.parse(body) }));
  }
});
if (!live) await new Promise((resolve) => mock.listen(0, "127.0.0.1", resolve));
const transport = new StdioClientTransport({
  command: process.execPath,
  args: [fileURLToPath(new URL("bridge.mjs", import.meta.url))],
  env: { ...process.env, ...(!live ? { FMU_MCP_PORT: String(mock.address().port) } : {}) },
  stderr: "inherit",
});
const client = new Client({ name: "flans-mcp-smoke", version: "1.0.0" });
try {
  await client.connect(transport);
  const { tools } = await client.listTools();
  assert(tools.some((tool) => tool.name === "screenshot_to_file"));
  assert(!tools.some((tool) => tool.name === "launch_minecraft"));
  if (!live) {
    assert.equal((await client.callTool({ name: "ping", arguments: {} })).isError, true);
    assert.equal(commands, 0, "Wrong version must receive no game commands");
    mode = "correct";
    const result = await client.callTool({ name: "mouse_drag", arguments: {
      x_start: 1, y_start: 2, x_end: 3, y_end: 4, button: "left",
    } });
    assert(!result.isError, JSON.stringify(result));
    assert.deepEqual(JSON.parse(result.content[0].text).request, { cmd: "mouse_drag", x1: 1, y1: 2, x2: 3, y2: 4, button: "left" });
    await new Promise((resolve) => mock.close(resolve));
    assert.equal((await client.callTool({ name: "ping", arguments: {} })).isError, true);
  } else {
    for (const name of ["get_minecraft_status", "ping", "get_screen_buttons"]) {
      const result = await client.callTool({ name, arguments: {} });
      assert(!result.isError, JSON.stringify(result));
      console.log(`${name}: ${JSON.stringify(result)}`);
    }
    const directory = fileURLToPath(new URL("../../build/minecraft-mcp", import.meta.url));
    mkdirSync(directory, { recursive: true });
    const path = `${directory}/smoke.png`;
    const result = await client.callTool({ name: "screenshot_to_file", arguments: { path } });
    assert(!result.isError, JSON.stringify(result));
    assert(existsSync(path), `Screenshot was not saved: ${JSON.stringify(result)}`);
    console.log(`screenshot: ${path}`);
  }
  console.log(`PASS: ${live ? "live" : "mock"} MCP smoke (${target.version}, ${tools.length} tools)`);
} finally {
  await client.close();
  mock.close();
}
