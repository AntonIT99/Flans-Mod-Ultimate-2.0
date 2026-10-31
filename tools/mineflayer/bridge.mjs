import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { createDevelopmentBot, control, controls } from "./bots.mjs";
import { target, serverStatus, packetTypes } from "./environment.mjs";

// Stdout belongs to JSON-RPC. Third-party diagnostics must not corrupt the transport.
console.log = (...values) => console.error(...values);
const server = new McpServer({ name: `flans-mineflayer-${target.version}`, version: "1.0.0" });
const bots = new Map();
const busy = new Set();
function session(name) {
  const result = bots.get(name);
  if (!result) throw new Error(`No bot named ${name}; call connect_bot first`);
  return result;
}
const username = z.string().regex(/^[A-Za-z0-9_]{1,16}$/);
function tool(name, description, schema, handler) {
  server.registerTool(name, { description, inputSchema: schema }, async (args) => {
    try {
      const result = await handler(args);
      return { content: [{ type: "text", text: JSON.stringify(result, (_, value) => typeof value === "bigint" ? value.toString() : value, 2) }] };
    } catch (error) { return { isError: true, content: [{ type: "text", text: error.message }] }; }
  });
}
tool("get_server_status", "Check the exact loopback test server, Minecraft protocol and checkout marker. Does not scan ports or start Minecraft.", {}, serverStatus);
tool("connect_bot", "Join the isolated modded test server with an offline headless player. Handles Forge/NeoForge negotiation and records mod payloads; does not run Java client code.", { username }, async ({ username }) => {
  if (bots.has(username) || busy.has(username)) throw new Error(`Bot ${username} already exists`);
  if (bots.size + busy.size >= 8) throw new Error("At most eight development bots may be connected by this bridge");
  busy.add(username);
  try {
    const bot = await createDevelopmentBot(username);
    bots.set(username, bot);
    return bot.snapshot();
  } finally { busy.delete(username); }
});
tool("disconnect_bot", "Disconnect and remove a bot owned by this bridge.", { username }, ({ username }) => {
  session(username).close();
  bots.delete(username);
  return { disconnected: username };
});
tool("list_bots", "List this bridge's bots, their health, inventory, positions and negotiated registries.", {}, () => [...bots.values()].map((bot) => bot.snapshot()));
tool("get_bot_state", "Inspect server-reported health, inventory, tracked entities, position and pack manifest. Custom Java client state is not simulated.", { username }, ({ username }) => session(username).snapshot());
tool("get_bot_events", "Read the bounded 256-event history after a cursor: damage/health, death, kicks and raw mod payloads (up to 1024 bytes each).", { username, after: z.number().int().nonnegative().default(0) }, ({ username, after }) => session(username).events(after));
tool("control_bot", "Perform a bounded action. Move/look use vanilla client physics. Fire/reload send the checkout's real Flan packets. Actions are requests; inspect server state to verify results. Angles are radians.", {
  username, action: z.enum(["move", "look", "equip", "attack", "use", "respawn", "fire", "reload"]),
  controls: z.array(z.enum(controls)).optional(), milliseconds: z.number().int().min(1).max(10000).optional(),
  yaw: z.number().optional(), pitch: z.number().min(-Math.PI / 2).max(Math.PI / 2).optional(),
  slot: z.number().int().min(0).max(45).optional(), offhand: z.boolean().optional(), entityId: z.number().int().optional(),
}, async (args) => {
  const name = args.username;
  if (busy.has(name)) throw new Error(`Bot ${name} already has an action in progress`);
  busy.add(name);
  try { return await control(session(name), args); }
  finally { busy.delete(name); }
});
tool("list_mod_packets", "List Flan packet names, directions and IDs derived from this checkout. IDs differ between Forge and NeoForge.", {}, packetTypes);
tool("send_mod_packet", "Send a named Flan client-to-server packet. Supply only its encodeInto body as hexadecimal; the bridge derives the envelope/ID. Read that packet's Java codec before use.", {
  username, packet: z.string(), hex: z.string().regex(/^(?:[0-9a-fA-F]{2})*$/).max(131072),
}, ({ username, packet, hex }) => {
  if (busy.has(username)) throw new Error(`Bot ${username} already has an action in progress`);
  session(username).sendModPacket(packet, Buffer.from(hex, "hex"));
  return { sent: packet, bytes: hex.length / 2 };
});
function shutdown() { for (const bot of bots.values()) bot.close(); bots.clear(); }
process.on("SIGINT", () => { shutdown(); process.exit(0); });
process.on("SIGTERM", () => { shutdown(); process.exit(0); });
const transport = new StdioServerTransport();
await server.connect(transport);
transport.onclose = () => { shutdown(); process.exit(0); };
