import { readFileSync, readdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { resolve } from "node:path";
import protocol from "minecraft-protocol";
import minecraftData from "minecraft-data";

export const root = fileURLToPath(new URL("../../", import.meta.url));
export const target = JSON.parse(readFileSync(new URL("target.json", import.meta.url), "utf8"));
export const port = Number(process.env.FMU_MINEFLAYER_PORT || target.port);
if (!Number.isInteger(port) || port < 1024 || port > 65535) throw new Error("Invalid FMU_MINEFLAYER_PORT");
const properties = readFileSync(resolve(root, "gradle.properties"), "utf8");
if (!properties.includes(`minecraft_version=${target.version}`)) throw new Error("Mineflayer target does not match this checkout");
const platform = readFileSync(resolve(root, "src/main/java/com/flansmodultimate/platform/network/NetworkPlatform.java"), "utf8");
export const modProtocol = platform.match(/String PROTOCOL = "([^"]+)"/)[1];
export const identity = `FMU Mineflayer ${target.version} ${target.loader}`;

export function verifyStatus(status) {
  const motd = JSON.stringify(status.description);
  if (status.version?.protocol !== minecraftData(target.version).version.version || !motd.includes(identity)) {
    throw new Error(`Wrong test server at 127.0.0.1:${port}: expected ${identity}, received ${status.version?.name} / ${motd}`);
  }
  return { version: target.version, loader: target.loader, host: "127.0.0.1", port, identity, players: status.players };
}

export async function serverStatus() {
  return verifyStatus(await protocol.ping({ host: "127.0.0.1", port, closeTimeout: 5000 }));
}

// Resolve packet identities from this checkout instead of maintaining a stale ID table.
// Forge orders the combined directions by fully qualified name; NeoForge orders each separately.
export function packetTypes() {
  const directory = resolve(root, "src/main/java/com/flansmodultimate/network");
  const qualified = new Map();
  function visit(path) {
    for (const entry of readdirSync(path, { withFileTypes: true })) {
      const child = resolve(path, entry.name);
      if (entry.isDirectory()) visit(child);
      else if (entry.name.endsWith(".java")) {
        const source = readFileSync(child, "utf8");
        const packageName = source.match(/^package ([\w.]+);/m)?.[1];
        if (packageName) qualified.set(entry.name.slice(0, -5), `${packageName}.${entry.name.slice(0, -5)}`);
      }
    }
  }
  visit(directory);
  const source = readFileSync(resolve(directory, "PacketHandler.java"), "utf8");
  const names = (direction) => {
    const call = source.match(new RegExp(`add${direction}Packets\\(([^;]+)\\);`))?.[1];
    if (!call) throw new Error(`Cannot read ${direction} packet registration`);
    return [...call.matchAll(/(\w+)\.class/g)].map((match) => {
      const fullName = qualified.get(match[1]);
      if (!fullName) throw new Error(`Unknown registered packet ${match[1]}`);
      return { name: match[1], fullName, direction };
    });
  };
  const compare = (a, b) => a.fullName.toLowerCase() < b.fullName.toLowerCase() ? -1 : a.fullName.toLowerCase() > b.fullName.toLowerCase() ? 1 : 0;
  const client = names("Client").sort(compare);
  const server = names("Server").sort(compare);
  if (target.loader === "forge") return [...client, ...server].sort(compare).map((entry, id) => ({ ...entry, id }));
  return [...client.map((entry, id) => ({ ...entry, id })), ...server.map((entry, id) => ({ ...entry, id }))];
}
