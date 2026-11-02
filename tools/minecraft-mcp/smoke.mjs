import assert from "node:assert/strict";
import { createServer } from "node:http";
import { readFileSync, existsSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

const target = JSON.parse(readFileSync(new URL("target.json", import.meta.url), "utf8"));
const live = process.argv.includes("--live");
const startedAt = Date.now();
let mode = "wrong";
let commands = 0;
let uiCommands = 0;
let uiMode = "correct";
let mockScreen = "TitleScreen";
const uiMock = createServer(async (request, response) => {
  response.setHeader("Content-Type", "application/json");
  if (request.url === "/api/status") {
    response.end(JSON.stringify({ ok: true, type: "flans-mcp-dev", protocol: 1, minecraft: target.version,
      loader: target.loader, pid: uiMode === "wrong-pid" ? 2 : 1, port: uiMock.address().port }));
    return;
  }
  uiCommands++;
  let body = "";
  for await (const chunk of request) body += chunk;
  const command = JSON.parse(body);
  if (["ui_click", "ui_close"].includes(command.cmd) && command.snapshot !== 1) {
    response.end(JSON.stringify({ error: "Stale UI snapshot" }));
    return;
  }
  if (["ui_click", "ui_close"].includes(command.cmd) && uiMode !== "no-transition") mockScreen = command.result_screen;
  response.end(JSON.stringify({ screen: mockScreen, snapshot: 1, input_consumed: true }));
});
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
if (!live) {
  await new Promise((resolve) => mock.listen(0, "127.0.0.1", resolve));
  await new Promise((resolve) => uiMock.listen(0, "127.0.0.1", resolve));
}
const transport = new StdioClientTransport({
  command: process.execPath,
  args: [fileURLToPath(new URL("bridge.mjs", import.meta.url))],
  env: { ...process.env, ...(!live ? { FMU_MCP_PORT: String(mock.address().port), FMU_MCP_UI_PORT: String(uiMock.address().port) } : {}) },
  stderr: "inherit",
});
const client = new Client({ name: "flans-mcp-smoke", version: "1.0.0" });
async function call(name, args = {}) {
  const result = await client.callTool({ name, arguments: args });
  assert(!result.isError, `${name}: ${JSON.stringify(result)}`);
  return JSON.parse(result.content[0].text);
}
async function untilState(predicate, timeout = 10000) {
  const deadline = Date.now() + timeout;
  let state;
  do {
    state = await call("ui_state");
    if (predicate(state)) return state;
    await new Promise((resolve) => setTimeout(resolve, 200));
  } while (Date.now() < deadline);
  assert.fail(`UI state timed out: ${JSON.stringify(state)}`);
}
async function capture(label) {
  const result = await call("ui_capture");
  assert(existsSync(result.file));
  const png = readFileSync(result.file);
  assert.equal(png.subarray(0, 8).toString("hex"), "89504e470d0a1a0a");
  assert.equal(png.readUInt32BE(16), result.framebuffer_width);
  assert.equal(png.readUInt32BE(20), result.framebuffer_height);
  assert.equal(result.image_width, result.framebuffer_width);
  assert.equal(result.image_height, result.framebuffer_height);
  assert(result.sampled_colors > 8, "Screenshot appears blank or uniform");
  console.log(`${label}: ${result.image_width}x${result.image_height}, ${result.sampled_colors} sampled colors; ${result.file}`);
}
async function click(state, widget, resultScreen) {
  assert(widget, `Required widget missing on ${state.screen}`);
  return call("ui_click", { snapshot: state.snapshot, expected_screen: state.screen,
    widget: widget.id, result_screen: resultScreen, timeout_ms: 5000 });
}
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
    uiMode = "wrong-pid";
    assert.equal((await client.callTool({ name: "ui_state", arguments: {} })).isError, true);
    assert.equal(uiCommands, 0, "Wrong companion process must receive no UI commands");
    uiMode = "correct";
    const clickArgs = { snapshot: 1, widget: "0", expected_screen: "TitleScreen", result_screen: "OptionsScreen", timeout_ms: 100 };
    assert.equal((await call("ui_click", clickArgs)).transition_verified, true);
    assert.equal((await call("ui_close", { ...clickArgs, expected_screen: "OptionsScreen", result_screen: "TitleScreen" })).transition_verified, true);
    assert.equal((await client.callTool({ name: "ui_click", arguments: { ...clickArgs, snapshot: 0 } })).isError, true);
    uiMode = "no-transition";
    mockScreen = "TitleScreen";
    assert.equal((await client.callTool({ name: "ui_click", arguments: clickArgs })).isError, true);
    await new Promise((resolve) => mock.close(resolve));
    assert.equal((await client.callTool({ name: "ping", arguments: {} })).isError, true);
  } else {
    console.log(`status: ${JSON.stringify(await call("get_minecraft_status"))}`);
    await call("ping");
    const initial = await call("ui_state");
    console.log(`screen: ${initial.screen}, ${initial.widgets.length} widgets`);
    assert.equal(initial.screen, "TitleScreen", "Start live preflight from title; diagnose startup screens before testing");
    assert(initial.widgets.some((widget) => widget.label.trim().length > 0));
    try {
      assert.equal((await call("enter_control_mode")).control_mode, true);
      const title = await call("ui_state");
      assert.equal(title.control_mode, true);
      const options = await click(title, title.widgets.find((widget) => widget.translation_key === "menu.options"), "OptionsScreen");
      assert.equal(options.transition_verified, true);
      const stale = await client.callTool({ name: "ui_click", arguments: {
        snapshot: title.snapshot, widget: "0", expected_screen: title.screen, result_screen: "OptionsScreen",
      } });
      assert.equal(stale.isError, true, "Old screen references must be rejected");
      const current = await call("ui_state");
      await click(current, current.widgets.find((widget) => widget.translation_key === "gui.done"), "TitleScreen");
      await capture("title capture");
      assert.equal((await call("ui_state")).control_mode, true);
      if (process.argv.includes("--fixture")) {
        assert.equal(initial.test_profile, true);
        await call("fixture_world");
        await untilState((state) => state.in_world && state.screen === "InGame", 120000);
        await call("fixture_editor");
        const editor = await untilState((state) => state.screen === "GuiWolffsMod" &&
          state.widgets.some((widget) => widget.translation_key.startsWith("wolffsmodnpcs.hub.") && widget.active));
        const movementHub = editor.widgets.find((widget) => widget.translation_key === "wolffsmodnpcs.hub.movement");
        assert(movementHub, "Movement hub is missing");
        if (movementHub.active) await click(editor, movementHub, "GuiWolffsMod");
        const movement = await untilState((state) => state.widgets.some((widget) => widget.translation_key === "wolffsmodnpcs.category.tactics" && widget.active));
        await click(movement, movement.widgets.find((widget) => widget.translation_key === "wolffsmodnpcs.category.tactics"), "GuiWolffsMod");
        const tactics = await untilState((state) => state.widgets.some((widget) => widget.translation_key === "wolffsmodnpcs.tab.survival" && widget.active));
        await click(tactics, tactics.widgets.find((widget) => widget.translation_key === "wolffsmodnpcs.tab.survival"), "GuiWolffsMod");
        const survival = await untilState((state) => state.widgets.some((widget) => widget.translation_key === "wolffsmodnpcs.tab.survival" && !widget.active));
        await capture("NPC survival capture");
        await click(survival, survival.widgets.find((widget) => widget.translation_key === "wolffsmodnpcs.tab.movement_limits"), "GuiWolffsMod");
        await untilState((state) => state.widgets.some((widget) => widget.translation_key === "wolffsmodnpcs.tab.movement_limits" && !widget.active));
        await capture("NPC movement limits capture");
        const finalPage = await call("ui_state");
        await call("ui_close", { snapshot: finalPage.snapshot, expected_screen: finalPage.screen, result_screen: "InGame" });
        await call("pause_game");
        const pause = await untilState((state) => state.screen === "PauseScreen");
        await click(pause, pause.widgets.find((widget) => widget.translation_key === "menu.returnToMenu"), "TitleScreen");
      }
    } finally {
      await call("exit_control_mode");
      assert.equal((await call("ui_state")).control_mode, false);
    }
  }
  console.log(`PASS: ${live ? "live" : "mock"} MCP smoke (${target.version}, ${tools.length} tools, ${Date.now() - startedAt} ms)`);
} finally {
  await client.close();
  mock.close();
  uiMock.close();
}
