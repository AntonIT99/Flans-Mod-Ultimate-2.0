import assert from "node:assert/strict";
import { test } from "node:test";
import { EventEmitter } from "node:events";
import minecraftData from "minecraft-data";
import { Reader, varint, utf, array, wrapForge } from "../wire.mjs";
import { attachLoader } from "../loaders.mjs";
import { verifyStatus, identity, target, packetTypes } from "../environment.mjs";

test("server selection rejects another version or an unmarked server", () => {
  const correct = { version: { protocol: minecraftData(target.version).version.version }, description: { text: identity } };
  assert.equal(verifyStatus(correct).version, target.version);
  assert.throws(() => verifyStatus({ ...correct, version: { protocol: -1 } }), /Wrong test server/);
  assert.throws(() => verifyStatus({ ...correct, description: { text: "A different local server" } }), /Wrong test server/);
});

test("loader codec round-trips Unicode and rejects truncated input", () => {
  const reader = new Reader(Buffer.concat([varint(16384), utf("Wolff's – Pack"), array([1, 130], varint)]));
  assert.equal(reader.varint(), 16384);
  assert.equal(reader.utf(), "Wolff's – Pack");
  assert.deepEqual(reader.array((r) => r.varint()), [1, 130]);
  reader.end();
  assert.throws(() => new Reader(Buffer.from([128])).varint(), /Truncated/);
  assert.throws(() => new Reader(Buffer.from([4, 1])).utf(), /Truncated/);
  assert.throws(() => new Reader(Buffer.from([128, 128, 128, 128, 128, 0])).varint(), /Invalid/);
});

function forgeFixture() {
  const client = new EventEmitter();
  const writes = [];
  const errors = [];
  client.write = (name, packet) => writes.push({ name, packet });
  client.end = (reason) => { client.ended = reason; };
  client.on("error", (error) => errors.push(error));
  client.on("login_plugin_request", function onLoginPluginRequest() { throw new Error("Default negative response must be replaced"); });
  const bot = { _client: client, registry: { items: {}, itemsByName: {} } };
  const state = attachLoader(bot, { loader: "forge" }, "32", () => {});
  return { client, writes, errors, state };
}

test("Forge informational login payloads receive no invalid empty wrapper", () => {
  const { client, writes, state } = forgeFixture();
  const manifest = Buffer.concat([Buffer.from([0]), array([["Pack", "hash"]], ([name, hash]) => Buffer.concat([utf(name), utf(hash)]))]);
  client.emit("login_plugin_request", { channel: "fml:loginwrapper", messageId: 1, data: wrapForge("flansmodultimate:pack_manifest", manifest) });
  client.emit("login_plugin_request", { channel: "fml:loginwrapper", messageId: 2, data: wrapForge("fml:handshake", Buffer.from([5, 0])) });
  assert.deepEqual(state.manifest, { Pack: "hash" });
  assert.equal(writes.length, 0);
});

function modList(version) {
  return Buffer.concat([Buffer.from([1]), array(["flansmodultimate"], utf),
    array([["flansmodultimate:main", version]], ([name, marker]) => Buffer.concat([utf(name), utf(marker)])), varint(0), varint(0)]);
}

test("Forge responds with mod channels and rejects a stale Flan protocol", () => {
  const correct = forgeFixture();
  correct.client.emit("login_plugin_request", { channel: "fml:loginwrapper", messageId: 3, data: wrapForge("fml:handshake", modList("32")) });
  assert.equal(correct.state.negotiated, true);
  assert.equal(correct.writes[0].packet.messageId, 3);
  const wrapper = new Reader(correct.writes[0].packet.data);
  assert.equal(wrapper.utf(), "fml:handshake");
  const reply = new Reader(wrapper.bytes(wrapper.varint()));
  assert.equal(reply.byte(), 2);
  assert.deepEqual(reply.array((r) => r.utf()), ["flansmodultimate"]);
  assert.deepEqual(reply.array((r) => [r.utf(), r.utf()]), [["flansmodultimate:main", "32"]]);
  assert.equal(reply.varint(), 0);
  reply.end();
  const stale = forgeFixture();
  stale.client.emit("login_plugin_request", { channel: "fml:loginwrapper", messageId: 3, data: wrapForge("fml:handshake", modList("old")) });
  assert.equal(stale.writes.length, 0);
  assert.match(stale.client.ended, /mismatch/);
});

test("packet IDs are unique in their transport and only registered packet names are exposed", () => {
  const entries = packetTypes();
  const keys = entries.map((entry) => `${target.loader === "forge" ? "combined" : entry.direction}:${entry.id}`);
  assert.equal(new Set(keys).size, entries.length);
  assert(entries.some((entry) => entry.name === "PacketGunInput" && entry.direction === "Server"));
  if (target.loader === "forge") assert(entries.every((entry) => entry.id < 256));
});
