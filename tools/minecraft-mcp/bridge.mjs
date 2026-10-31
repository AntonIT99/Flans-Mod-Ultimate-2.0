// Reuse upstream tool schemas and HTTP client, with explicit environment selection.
// The upstream launcher/discovery selects the first client it finds; development
// runs must instead stay attached to this checkout's version, loader and port.
import { readFileSync } from "node:fs";
import { ModClient, TOOLS } from "minecraft-mod-mcp";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";

const target = JSON.parse(readFileSync(new URL("target.json", import.meta.url), "utf8"));
const port = Number(process.env.FMU_MCP_PORT || target.port);
if (!Number.isInteger(port) || port < 1024 || port > 65535) throw new Error("Invalid FMU_MCP_PORT");
const baseUrl = `http://127.0.0.1:${port}`;
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
for (const tool of TOOLS) {
  if (management.has(tool.name)) continue;
  server.registerTool(tool.name, {
    description: tool.description,
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
