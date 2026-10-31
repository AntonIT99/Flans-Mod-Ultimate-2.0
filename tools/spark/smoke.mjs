import assert from "node:assert/strict";
import { mkdir, unlink, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { gzipSync } from "node:zlib";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import { sparkRoot } from "spark-profiler-mcp/dist/parser/decode.js";

const directory = new URL("../../build/spark/smoke/", import.meta.url);
await mkdir(directory, { recursive: true });
const fixture = new URL("fixture.sparkprofile", directory);
const type = sparkRoot().lookupType("spark.SamplerData");
const bytes = type.encode(type.fromObject({
  metadata: { startTime: 1000, endTime: 2000, interval: 4, platformMetadata: { name: "smoke-fixture", minecraftVersion: "fixture" } },
  threads: ["Server thread", "Render thread"].map((name) => ({
    name, times: [1000], childrenRefs: [0], children: [
      { className: "com.flansmodultimate.SmokeFixture", methodName: "tick", times: [1000], childrenRefs: [1] },
      { className: "com.flansmodultimate.SmokeFixture", methodName: "work", times: [750] },
    ],
  })),
})).finish();
await writeFile(fixture, gzipSync(bytes));

const client = new Client({ name: "flans-spark-smoke", version: "1.0.0" });
const transport = new StdioClientTransport({
  command: process.execPath,
  args: [fileURLToPath(new URL("bridge.mjs", import.meta.url))],
  // Resolve everything relative to the bridge, never the invoking shell.
  cwd: fileURLToPath(directory),
  stderr: "inherit",
});
async function call(name, args = {}) {
  const response = await client.callTool({ name, arguments: args });
  assert(!response.isError, JSON.stringify(response));
  return JSON.parse(response.content[0].text);
}
try {
  await client.connect(transport);
  const { tools } = await client.listTools();
  for (const name of ["load_profile", "get_call_tree", "get_top_self_time", "get_heap_summary", "spark_environment", "spark_files"])
    assert(tools.some((tool) => tool.name === name), `Missing tool: ${name}`);
  const environment = await call("spark_environment");
  const files = await call("spark_files");
  assert(files.files.some((file) => file.path === fileURLToPath(fixture)));
  const profile = await call("load_profile", { source: fileURLToPath(fixture) });
  const threads = await call("list_threads", { profileId: profile.id });
  assert.equal(threads.length, 2);
  for (const thread of ["Server thread", "Render thread"]) {
    const hot = await call("get_top_self_time", { profileId: profile.id, thread });
    assert.equal(hot.methods[0].method, "com.flansmodultimate.SmokeFixture.work");
    assert.equal(hot.methods[0].selfMs, 750);
  }
  for (const source of process.argv.slice(2)) {
    const actual = await call("load_profile", { source });
    const sampled = await call("list_threads", { profileId: actual.id });
    assert(sampled.length > 0, "Real capture must contain sampled threads");
    const selected = sampled.filter((item) => ["Render thread", "Server thread"].includes(item.name));
    for (const thread of selected.length ? selected : [sampled[0]]) {
      const hot = await call("get_top_self_time", { profileId: actual.id, thread: thread.name });
      assert(hot.totalMs > 0, `No sampled time on ${thread.name}`);
      await call("get_call_tree", { profileId: actual.id, thread: thread.name });
    }
    console.log(`PASS: loaded real capture ${source} (${sampled.length} threads)`);
  }
  console.log(`PASS: spark MCP (${environment.minecraftVersion}/${environment.loader}, ${tools.length} tools, gzip decode, server/render self-time, cwd independence)`);
} finally {
  try {
    await client.close();
  } finally {
    await unlink(fixture);
  }
}
