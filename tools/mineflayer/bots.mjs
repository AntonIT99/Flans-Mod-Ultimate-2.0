import mineflayer from "mineflayer";
import minecraftData from "minecraft-data";
import { setTimeout as delay } from "node:timers/promises";
import { serverStatus, target, port, modProtocol, packetTypes } from "./environment.mjs";
import { attachLoader } from "./loaders.mjs";
import { Reader, varint } from "./wire.mjs";

const types = packetTypes();
const outbound = new Map(types.filter((entry) => entry.direction === "Server").map((entry) => [entry.name, entry]));
const inbound = new Map(types.filter((entry) => entry.direction === "Client").map((entry) => [entry.id, entry.name]));

export async function createDevelopmentBot(username, { timeout = 30000 } = {}) {
  if (!/^[A-Za-z0-9_]{1,16}$/.test(username)) throw new Error("Bot username must contain 1–16 letters, numbers or underscores");
  await serverStatus();
  const events = [];
  let sequence = 0;
  let connected = false;
  const record = (type, data = {}) => {
    events.push({ sequence: ++sequence, time: new Date().toISOString(), type, ...data });
    if (events.length > 256) events.shift();
  };
  const bot = mineflayer.createBot({
    host: "127.0.0.1", port, username, version: target.version, auth: "offline",
    hideErrors: true, logErrors: false, respawn: false, checkTimeoutInterval: 30000,
    viewDistance: "tiny", brand: "FMU-Mineflayer",
    // Modded Brigadier serializers are not vanilla command nodes. A headless
    // test player does not need client completion; keep this packet opaque.
    customPackets: { [minecraftData(target.version).version.majorVersion]: { play: { toClient: { types: {
      packet_declare_commands: "restBuffer", packet_declare_recipes: "restBuffer",
    } } } } },
  });
  bot._client.prependListener("declare_commands", (packet) => {
    record("command_tree", { bytes: packet.length, decoded: false });
    packet.nodes = [{ children: [], flags: { command_node_type: 0 } }];
    packet.rootIndex = 0;
  });
  bot._client.prependListener("declare_recipes", (packet) => {
    record("recipe_book", { bytes: packet.length, decoded: false });
    packet.recipes = [];
  });
  if (process.env.FMU_MINEFLAYER_TRACE) {
    bot._client.on("state", () => {
      const incoming = [];
      bot._client.decompressor?.prependListener("data", (buffer) => { incoming.push(buffer.subarray(0, 200).toString("hex")); if (incoming.length > 12) incoming.shift(); });
      bot._client.deserializer.prependListener("error", () => console.error("Recent incoming packets:", incoming));
    });
  }
  bot.on("error", (error) => {
    connected = false;
    record("error", { message: error.message });
    bot._client.end(`Test client failed: ${error.message}`);
  });
  bot.on("kicked", (reason) => record("kicked", { reason }));
  bot.on("end", (reason) => { connected = false; bot.clearControlStates?.(); record("end", { reason }); });
  bot.on("health", () => record("health", { health: bot.health, food: bot.food }));
  bot.on("death", () => { bot.clearControlStates(); record("death"); });
  bot.on("spawn", () => record("spawn", { position: bot.entity.position }));
  bot.on("messagestr", (message) => record("message", { message }));
  let network;
  try { network = attachLoader(bot, { ...target, port }, modProtocol, record); }
  catch (error) { bot.end(error.message); throw error; }
  bot._client.on("custom_payload", ({ channel, data }) => {
    if (!Buffer.isBuffer(data)) return;
    const mod = channel === "flansmodultimate:main" || channel === "flansmodultimate:clientbound";
    const reader = new Reader(data);
    const id = mod && data.length ? (target.loader === "forge" ? reader.byte() : reader.varint()) : null;
    record("payload", { channel, packet: mod ? inbound.get(id) || `unknown:${id}` : undefined,
      bytes: data.length, hex: data.subarray(0, 1024).toString("hex"), truncated: data.length > 1024 });
  });
  const close = () => {
    connected = false;
    bot.clearControlStates?.();
    if (bot.quit) bot.quit("Development test complete");
    else bot._client.end("Development test complete");
  };
  try {
    await new Promise((resolve, reject) => {
      const timer = setTimeout(() => finish(new Error(`Bot did not spawn within ${timeout} ms`)), timeout);
      const spawn = () => finish();
      const error = (error) => finish(error);
      const end = (reason) => finish(new Error(`Bot disconnected before spawn: ${reason}; ${JSON.stringify(events.slice(-4))}`));
      function finish(error) {
        clearTimeout(timer);
        bot.off("spawn", spawn);
        bot.off("error", errorListener);
        bot.off("end", end);
        if (error) reject(error); else resolve();
      }
      const errorListener = error;
      bot.once("spawn", spawn);
      bot.once("error", errorListener);
      bot.once("end", end);
    });
    if (!network.negotiated) throw new Error("Bot spawned without the expected modded handshake");
    connected = true;
  } catch (error) {
    close();
    throw error;
  }
  function sendModPacket(name, bytes) {
    if (!connected) throw new Error("Bot is disconnected");
    const entry = outbound.get(name);
    if (!entry) throw new Error(`Unknown client-to-server packet ${name}`);
    const header = target.loader === "forge" ? Buffer.from([entry.id]) : varint(entry.id);
    bot._client.write("custom_payload", { channel: target.loader === "forge" ? "flansmodultimate:main" : "flansmodultimate:serverbound", data: Buffer.concat([header, bytes]) });
    record("sent_packet", { packet: name, bytes: bytes.length });
  }
  function snapshot() {
    const itemNames = network.registries.get("minecraft:item");
    return {
      username, connected, version: target.version, loader: target.loader, port,
      position: bot.entity?.position, yaw: bot.entity?.yaw, pitch: bot.entity?.pitch,
      health: bot.health, food: bot.food, gameMode: bot.game?.gameMode, dimension: bot.game?.dimension,
      inventory: bot.inventory.items().map((item) => ({ slot: item.slot, id: item.type, name: itemNames?.get(item.type) || item.name, count: item.count, nbt: item.nbt, components: item.components })),
      entities: Object.values(bot.entities).slice(0, 128).map((entity) => ({ id: entity.id, name: network.registries.get("minecraft:entity_type")?.get(entity.entityType) || entity.name,
        username: entity.username, position: entity.position })),
      packs: Object.keys(network.manifest), negotiated: network.negotiated, eventCursor: sequence,
      registries: Object.fromEntries([...network.registries].map(([name, entries]) => [name, entries.size])),
    };
  }
  return { bot, network, close, snapshot, sendModPacket, events: (after = 0) => ({ cursor: sequence, oldest: events[0]?.sequence, events: events.filter((entry) => entry.sequence > after) }) };
}

export const controls = ["forward", "back", "left", "right", "jump", "sprint", "sneak"];

export async function control(session, action) {
  if (!session.snapshot().connected) throw new Error("Bot is disconnected");
  const bot = session.bot;
  switch (action.action) {
    case "move": {
      if (!Array.isArray(action.controls) || action.controls.some((key) => !controls.includes(key))) throw new Error("Invalid movement controls");
      if (!Number.isInteger(action.milliseconds) || action.milliseconds < 1 || action.milliseconds > 10000) throw new Error("Movement must last 1–10000 ms");
      try {
        for (const key of action.controls) bot.setControlState(key, true);
        await delay(action.milliseconds);
      } finally { bot.clearControlStates(); }
      break;
    }
    case "look":
      if (!Number.isFinite(action.yaw) || !Number.isFinite(action.pitch) || Math.abs(action.pitch) > Math.PI / 2) throw new Error("Supply yaw and pitch in radians; pitch must be between -π/2 and π/2");
      await bot.look(action.yaw, action.pitch, true);
      break;
    case "equip": {
      const item = bot.inventory.slots[action.slot];
      if (!item) throw new Error(`Inventory slot ${action.slot} is empty`);
      await bot.equip(item, action.offhand ? "off-hand" : "hand");
      break;
    }
    case "attack": {
      const entity = bot.entities[action.entityId];
      if (!entity) throw new Error(`Entity ${action.entityId} is not tracked`);
      bot.attack(entity);
      break;
    }
    case "use":
      try { bot.activateItem(Boolean(action.offhand)); await delay(250); }
      finally { bot.deactivateItem(); }
      break;
    case "respawn": bot.respawn(); break;
    case "reload": session.sendModPacket("PacketGunReload", varint(action.offhand ? 1 : 0)); break;
    case "fire": {
      const hand = action.offhand ? 1 : 0;
      if (!Number.isInteger(action.milliseconds) || action.milliseconds < 1 || action.milliseconds > 10000) throw new Error("Firing must last 1–10000 ms");
      try {
        session.sendModPacket("PacketGunInput", Buffer.from([1, 0, hand]));
        await delay(action.milliseconds);
      } finally { if (session.snapshot().connected) session.sendModPacket("PacketGunInput", Buffer.from([0, 0, hand])); }
      break;
    }
    default: throw new Error(`Unknown bot action ${action.action}`);
  }
  return session.snapshot();
}
