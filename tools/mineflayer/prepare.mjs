import { readFileSync, writeFileSync, mkdirSync, existsSync, copyFileSync, cpSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { resolve } from "node:path";

const root = fileURLToPath(new URL("../../", import.meta.url));
const target = JSON.parse(readFileSync(new URL("target.json", import.meta.url), "utf8"));
const directory = resolve(root, "build/mineflayer/server");
mkdirSync(directory, { recursive: true });
let properties = readFileSync(new URL("server.properties", import.meta.url), "utf8");
for (const key of ["port", "version", "loader"]) properties = properties.replaceAll(`@${key.toUpperCase()}@`, String(target[key]));
writeFileSync(resolve(directory, "server.properties"), properties);
const eula = process.env.FMU_MINEFLAYER_EULA || resolve(root, "run/eula.txt");
if (existsSync(eula) && /^eula\s*=\s*true\s*$/m.test(readFileSync(eula, "utf8"))) {
  copyFileSync(eula, resolve(directory, "eula.txt"));
}
// Copy only on first preparation; the original runtime packs remain untouched.
const packs = resolve(root, "run/flan");
if (existsSync(packs) && !existsSync(resolve(directory, "flan"))) cpSync(packs, resolve(directory, "flan"), { recursive: true });
console.log(`Prepared ${target.version}/${target.loader} at 127.0.0.1:${target.port}: ${directory}`);
if (!existsSync(resolve(directory, "eula.txt"))) console.log("Accept the Minecraft EULA in this test server's eula.txt before launching, or provide an already accepted file with FMU_MINEFLAYER_EULA.");
