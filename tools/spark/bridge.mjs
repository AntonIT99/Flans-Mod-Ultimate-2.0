// The pinned upstream package owns profile decoding and analysis. Keep our
// additions limited to locating this checkout's captures and describing it.
import { readFile, readdir, stat } from "node:fs/promises";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { registerTools } from "spark-profiler-mcp/dist/tools/register.js";
import { z } from "zod";

const target = JSON.parse(await readFile(new URL("target.json", import.meta.url), "utf8"));
const roots = ["run/config/spark/", "run/spark/", "run/second-client/config/spark/", "run/second-client/spark/", "build/spark/"]
  .map((path) => fileURLToPath(new URL(`../../${path}`, import.meta.url)));
const server = new McpServer({ name: `flans-spark-${target.minecraftVersion}`, version: "1.0.0" });
registerTools(server);

function result(value) {
  return { content: [{ type: "text", text: JSON.stringify(value, null, 2) }] };
}

server.registerTool("spark_environment", {
  description: "Describe this checkout's pinned spark installation and local capture directories. Does not connect to Minecraft.",
  inputSchema: {},
}, async () => result({
  ...target,
  analysisVersion: "0.1.1",
  directories: roots,
  minecraftServer: `minecraft_${target.minecraftVersion.replaceAll(".", "_")}`,
  capture: "Use the matching Minecraft MCP server after verifying its status. Enter control mode, open_chat, type_text the /sparkc or /spark command, then press_key Enter. Check latest.log for the actual spark response. Stop with profiler stop --save-to-file.",
  sampler: process.platform === "win32" ? "Java sampler; native async/allocation profiling is unavailable on Windows." : "Native async sampler if supported by the installed JVM and OS; otherwise Java sampler.",
  interpretation: "Sampling is statistical. Select the Render thread explicitly for client work. Compare identical warmed-up scenarios; server tuning diagnoses may not apply to a mod development client. GPU frame time is not measured.",
}));

async function scan(directory, files) {
  let entries;
  try {
    entries = await readdir(directory, { withFileTypes: true });
  } catch (error) {
    if (error.code === "ENOENT") return;
    throw error;
  }
  for (const entry of entries) {
    const path = join(directory, entry.name);
    // Do not follow symlinks out of a capture directory.
    if (entry.isDirectory()) await scan(path, files);
    else if (entry.isFile() && /\.(sparkprofile|sparkheap|sparkhealth)$/i.test(entry.name)) {
      const info = await stat(path);
      files.push({ path, bytes: info.size, modified: info.mtime.toISOString() });
    }
  }
}

server.registerTool("spark_files", {
  description: "Find saved spark profiles, heap summaries and health reports in this checkout, newest first. Pass an absolute path to load_profile.",
  inputSchema: { limit: z.number().int().min(1).max(100).default(20) },
}, async ({ limit }) => {
  try {
    const files = [];
    for (const directory of roots) await scan(directory, files);
    files.sort((a, b) => b.modified.localeCompare(a.modified));
    return result({ total: files.length, files: files.slice(0, limit) });
  } catch (error) {
    return { isError: true, content: [{ type: "text", text: error.message }] };
  }
});

await server.connect(new StdioServerTransport());
