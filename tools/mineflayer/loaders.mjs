import { Reader, varint, utf, array, wrapForge } from "./wire.mjs";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

// Wire formats checked against the pinned loader sources in Gradle's cache.
// This negotiates a headless observer; it does not execute Java client handlers.
export function attachLoader(bot, target, modProtocol, event) {
  const client = bot._client;
  const state = { negotiated: false, registries: new Map(), channels: [], manifest: {} };
  const payload = (channel, data = Buffer.alloc(0)) => client.write("custom_payload", { channel, data });
  const guarded = (handler) => (packet) => {
    try { handler(packet); }
    catch (error) { client.emit("error", error); client.end(`Loader negotiation failed: ${error.message}`); }
  };
  function registry(name, entries) {
    state.registries.set(name, new Map(entries));
    event("registry", { name, entries: entries.length });
    if (name === "minecraft:item") {
      // Item's factory closes over bot.registry. Keep server IDs and expose full resource names.
      const vanilla = bot.registry.itemsByName;
      const items = { ...bot.registry.items };
      for (const [id, resource] of entries) {
        const base = resource.startsWith("minecraft:") ? vanilla[resource.slice(10)] : null;
        items[id] = base ? { ...base, id } : { id, name: resource, displayName: resource, stackSize: 1 };
      }
      bot.registry.items = items;
    }
  }
  function manifest(reader) {
    state.manifest = Object.fromEntries(reader.array((r) => [r.utf(), r.utf()]));
    reader.end();
    event("pack_manifest", { packs: Object.keys(state.manifest) });
  }
  if (target.loader === "forge") {
    client.tagHost = "\0FML3\0";
    // Replace only the protocol library's automatic negative login-plugin reply.
    const listener = client.listeners("login_plugin_request").find((fn) => fn.name === "onLoginPluginRequest");
    if (!listener) throw new Error("minecraft-protocol login listener changed");
    client.removeListener("login_plugin_request", listener);
    client.on("login_plugin_request", guarded((packet) => {
      if (packet.channel !== "fml:loginwrapper") {
        client.write("login_plugin_response", { messageId: packet.messageId });
        return;
      }
      const wrapper = new Reader(packet.data);
      const channel = wrapper.utf();
      const reader = new Reader(wrapper.bytes(wrapper.varint()));
      wrapper.end();
      const discriminator = reader.byte();
      let reply;
      if (channel === "flansmodultimate:pack_manifest") {
        if (discriminator !== 0) throw new Error("Unknown content pack manifest packet");
        manifest(reader);
        // noResponse messages are not acknowledged; an empty wrapper is invalid.
        return;
      }
      if (channel !== "fml:handshake") throw new Error(`Unsupported Forge login channel ${channel}`);
      if (discriminator === 1) {
        const mods = reader.array((r) => r.utf());
        const channels = reader.array((r) => [r.utf(), r.utf()]);
        reader.array((r) => r.utf()); // Registry names; empty hashes request complete snapshots.
        reader.array((r) => r.utf()); // Custom datapack registries.
        reader.end();
        const main = channels.find(([name]) => name === "flansmodultimate:main");
        if (main?.[1] !== modProtocol) throw new Error(`Flan protocol mismatch: expected ${modProtocol}, received ${main?.[1]}`);
        state.channels = channels;
        reply = Buffer.concat([Buffer.from([2]), array(mods, utf), array(channels, ([name, version]) => Buffer.concat([utf(name), utf(version)])), varint(0)]);
        state.negotiated = true;
        event("negotiated", { loader: "forge", protocol: modProtocol, mods });
      } else if (discriminator === 3) {
        const name = reader.utf();
        if (reader.byte()) {
          const entries = reader.array((r) => { const resource = r.utf(); return [r.varint(), resource]; });
          reader.array((r) => [r.utf(), r.utf()]); // Aliases.
          reader.array((r) => [r.utf(), r.utf()]); // Overrides.
          reader.array((r) => r.varint()); // Blocked IDs.
          registry(name, entries);
        }
        reader.end();
        reply = Buffer.from([99]);
      } else if (discriminator === 4) {
        event("server_config", { file: reader.utf() });
        reply = Buffer.from([99]);
      } else if (discriminator === 5) {
        // Informational mod names/versions, with no application reply.
        return;
      } else throw new Error(`Unsupported Forge handshake message ${discriminator}`);
      client.write("login_plugin_response", { messageId: packet.messageId, data: wrapForge("fml:handshake", reply) });
    }));
  } else {
    client.on("custom_payload", guarded(({ channel, data }) => {
      if (!Buffer.isBuffer(data)) return;
      const reader = new Reader(data);
      if (channel === "neoforge:register") {
        // NeoForge's query is empty; use the actual running server's development
        // export. This also covers mandatory channels from optional dev mods.
        const runtime = JSON.parse(readFileSync(fileURLToPath(new URL("../../build/mineflayer/server/runtime.json", import.meta.url)), "utf8"));
        if (runtime.minecraftVersion !== target.version || runtime.loader !== target.loader || runtime.port !== target.port) throw new Error("Wrong Mineflayer runtime export; restart this checkout's test server");
        process.kill(runtime.pid, 0); // Reject an export left by an exited server.
        const query = Buffer.from(runtime.payloadQuery, "base64");
        const exported = new Reader(query);
        const phases = exported.array((r) => ({ phase: r.varint(), channels: r.array((r) => ({
          name: r.utf(), version: r.utf(), flow: r.byte() ? r.varint() : null, optional: r.byte(),
        })) }));
        exported.end();
        state.channels = phases.flatMap((phase) => phase.channels);
        const main = state.channels.find((entry) => entry.name === "flansmodultimate:serverbound");
        if (main?.version !== modProtocol) throw new Error("The running test server has a different Flan protocol; restart it from this checkout");
        payload(channel, query);
      } else if (channel === "neoforge:network") {
        const phases = reader.array((r) => ({ phase: r.varint(), channels: r.array((r) => ({ key: r.utf(), name: r.utf(), version: r.utf() })) }));
        reader.end();
        const main = phases.flatMap((phase) => phase.channels).find((entry) => entry.name === "flansmodultimate:serverbound");
        if (main?.version !== modProtocol) throw new Error(`NeoForge negotiated the wrong Flan protocol: ${main?.version}`);
        state.negotiated = true;
        event("negotiated", { loader: "neoforge", protocol: modProtocol });
      } else if (channel === "minecraft:register") {
        // Register only channels the server has advertised, at its current protocol phase.
        payload(channel, data);
      } else if (channel === "c:version" || channel === "c:register") {
        payload(channel, data);
      } else if (channel === "neoforge:frozen_registry") {
        const name = reader.utf();
        const entries = reader.array((r) => [r.varint(), r.utf()]);
        reader.array((r) => [r.utf(), r.utf()]);
        reader.end();
        registry(name, entries);
      } else if (channel === "neoforge:frozen_registry_sync_completed") {
        payload(channel);
      } else if (channel === "neoforge:known_registry_data_maps") {
        // Do not claim to implement mandatory Java data maps.
        reader.array((r) => {
          const name = r.utf();
          return r.array((r) => {
            const map = r.utf();
            if (r.byte()) throw new Error(`Unsupported mandatory data map ${name}/${map}`);
            return map;
          });
        });
        reader.end();
        payload("neoforge:known_registry_data_maps_reply", varint(0));
      } else if (channel === "neoforge:extensible_enum_data") {
        reader.array((r) => {
          const name = r.utf();
          r.utf();
          if (r.byte()) throw new Error(`Unsupported extended enum ${name}`);
          return name;
        });
        reader.end();
        payload("neoforge:extensible_enum_ack");
      } else if (channel === "neoforge:feature_flags") {
        if (reader.varint() !== 0) throw new Error("Custom feature flags require an additional bot adapter");
        payload("neoforge:feature_flags_ack");
      } else if (channel === "flansmodultimate:pack_manifest") {
        manifest(reader);
      } else if (channel === "neoforge:modded_network_setup_failed") {
        throw new Error(`NeoForge rejected the bot's channels: ${data.toString("hex")}`);
      }
    }));
  }
  return state;
}
