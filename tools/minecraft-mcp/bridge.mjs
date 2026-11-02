// Reuse upstream tool schemas and HTTP client, with explicit environment selection.
// The upstream launcher/discovery selects the first client it finds; development
// runs must instead stay attached to this checkout's version, loader and port.
import { readFileSync } from "node:fs";
import { ModClient, TOOLS } from "minecraft-mod-mcp";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const target = JSON.parse(readFileSync(new URL("target.json", import.meta.url), "utf8"));
const port = Number(process.env.FMU_MCP_PORT || target.port);
if (!Number.isInteger(port) || port < 1024 || port > 65535) throw new Error("Invalid FMU_MCP_PORT");
const baseUrl = `http://127.0.0.1:${port}`;
const uiPort = Number(process.env.FMU_MCP_UI_PORT || port + 100);
if (!Number.isInteger(uiPort) || uiPort < 1024 || uiPort > 65535) throw new Error("Invalid FMU_MCP_UI_PORT");
const uiUrl = `http://127.0.0.1:${uiPort}`;
const mod = new ModClient();
const server = new McpServer({ name: `flans-minecraft-${target.version}`, version: "1.0.0" });

async function status() {
  const response = await fetch(`${baseUrl}/api/status`, { signal: AbortSignal.timeout(3000) });
  if (!response.ok) throw new Error(`Minecraft status returned HTTP ${response.status}`);
  const result = await response.json();
  if (result.ok !== true || result.type !== "minecraft-mod" || result.version !== target.modVersion || result.loader !== target.loader || result.port !== port) {
    throw new Error(`Wrong Minecraft environment at ${baseUrl}: ${JSON.stringify(result)}`);
  }
  return { ...result, configuredMinecraftVersion: target.version };
}

// Never scan another environment, including after a disconnect or game restart.
const discover = mod.discover.bind(mod);
mod.discover = async () => {
  await status();
  const result = await discover(port, port);
  if (!result) throw new Error(`Minecraft is unavailable at ${baseUrl}`);
  return { ...result, configuredMinecraftVersion: target.version };
};

server.registerTool("get_minecraft_status", {
  description: "Check the exact development client version, loader, port and PID. Does not scan other clients.",
  inputSchema: {},
}, async () => {
  try {
    return { content: [{ type: "text", text: JSON.stringify(await status(), null, 2) }] };
  } catch (error) {
    return { isError: true, content: [{ type: "text", text: `${error.message}. Start gradlew runClient -PwithMinecraftMcp in the ${target.version} checkout.` }] };
  }
});

const management = new Set([
  "launch_minecraft", "kill_minecraft", "get_minecraft_status", "install_version",
  "list_supported_versions", "list_installed_versions", "detect_java",
  "create_offline_account", "list_accounts", "install_server", "launch_server", "serve",
]);

async function mapped(command, params = {}) {
  const upstream = await status();
  const identityResponse = await fetch(`${uiUrl}/api/status`, { signal: AbortSignal.timeout(3000) });
  const identity = await identityResponse.json();
  if (!identityResponse.ok || identity.ok !== true || identity.type !== "flans-mcp-dev" || identity.protocol !== 1 ||
      identity.minecraft !== target.version || identity.loader !== target.loader || identity.port !== uiPort || identity.pid !== upstream.pid) {
    throw new Error(`Wrong mapped UI companion at ${uiUrl}: ${JSON.stringify(identity)}`);
  }
  const response = await fetch(`${uiUrl}/api/cmd`, {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ ...params, cmd: command }), signal: AbortSignal.timeout(command === "fixture_editor" ? 35000 : 12000),
  });
  const result = await response.json();
  if (!response.ok || result.error || result.ok === false) throw new Error(JSON.stringify(result));
  return result;
}

const mappedTools = [
  ["fixture_world", "Create or reopen the fixed disposable flat creative world. Requires control mode and -PmcpTestProfile, from title. Poll ui_state until in_world.", {}],
  ["fixture_editor", "Prepare a reusable NPC and open its real equipment/tactics editor in the isolated fixture world. Requires control mode and -PmcpTestProfile.", {}],
  ["ui_state", "Read mapped screen, readable widget labels, snapshot IDs and framebuffer/GUI dimensions.", {}],
  ["ui_capture", "Capture the complete framebuffer. PNG path must be directly inside build/minecraft-mcp/captures. Returns dimensions and image diagnostics.", { path: z.string().optional() }],
  ["ui_close", "Close the observed screen using its mapped onClose handler (including save hooks), and verify the resulting screen. Requires control mode and a current snapshot.", {
    snapshot: z.number().int(), expected_screen: z.string(), result_screen: z.string(),
    timeout_ms: z.number().int().min(100).max(10000).default(5000),
  }],
  ["ui_click", "Click a widget from a current UI snapshot and wait for the required resulting screen. Rejects stale references. Requires control mode.", {
    snapshot: z.number().int(), widget: z.string(), expected_screen: z.string(), result_screen: z.string(),
    timeout_ms: z.number().int().min(100).max(10000).default(5000),
  }],
];
for (const [name, description, inputSchema] of mappedTools) {
  server.registerTool(name, { description, inputSchema }, async (params) => {
    try {
      let result = await mapped(name, params);
      if (name === "ui_click" || name === "ui_close") {
        if (name === "ui_click" && !result.input_consumed) throw new Error(`Widget did not consume input: ${JSON.stringify(result)}`);
        const deadline = Date.now() + params.timeout_ms;
        while (result.screen !== params.result_screen && Date.now() < deadline) {
          await new Promise((resolve) => setTimeout(resolve, 100));
          result = await mapped("ui_state");
        }
        if (result.screen !== params.result_screen) throw new Error(`Transition timed out; expected ${params.result_screen}: ${JSON.stringify(result)}`);
        result.transition_verified = true;
      }
      return { content: [{ type: "text", text: JSON.stringify(result, null, 2) }] };
    } catch (error) {
      return { isError: true, content: [{ type: "text", text: `${error.message}. Mapped UI requires a client restarted with -PwithMinecraftMcp.` }] };
    }
  });
}
for (const tool of TOOLS) {
  if (management.has(tool.name)) continue;
  server.registerTool(tool.name, {
    // Upstream's schema incorrectly says the user must click the overlay first.
    description: tool.name === "enter_control_mode"
      ? "Enter MCP control mode directly through the API. No manual overlay click is required in the pinned mod version. Verify control_mode is true before input; call exit_control_mode when finished."
      : tool.description,
    inputSchema: tool.inputSchema,
  }, async (params) => {
    try {
      if (tool.name === "wait") {
        await new Promise((resolve) => setTimeout(resolve, Math.min(params.seconds, 60) * 1000));
        return { content: [{ type: "text", text: "Wait complete" }] };
      }
      // Check identity on every action, even when a previously connected process exits.
      await mod.discover();
      if (tool.name === "mouse_drag") {
        params = { x1: params.x_start, y1: params.y_start, x2: params.x_end, y2: params.y_end, button: params.button };
      }
      const result = tool.name === "screenshot" ? await mod.screenshot() : await mod.sendCommand(tool.name, params);
      if (result?.error || result?.ok === false) throw new Error(JSON.stringify(result));
      return { content: [{ type: "text", text: typeof result === "string" ? result : JSON.stringify(result, null, 2) }] };
    } catch (error) {
      return { isError: true, content: [{ type: "text", text: `${error.message} (${target.version}/${target.loader} at ${baseUrl})` }] };
    }
  });
}

await server.connect(new StdioServerTransport());
