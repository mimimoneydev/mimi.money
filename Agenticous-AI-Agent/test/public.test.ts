import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import vm from "node:vm";

test("public page identifies the Agenticous AI agent and exposes its complete decision loop", async () => {
  const html = await readFile("public/index.html", "utf8");
  const labels = ["OBSERVE", "ANALYZE", "AI REASON", "TXs VERIFY", "ORGANISE", "EXECUTE"];

  assert.match(html, /data-decision-loop data-cycle-seconds="30"/);
  assert.match(html, /Agenticous AI agent/);
  for (const label of labels) assert.match(html, new RegExp(`>${label}<`));
  assert.doesNotMatch(html, /agenticous \/ transaction-report/);
  assert.match(html, /decision-loop\.js\?v=1/);
  assert.match(html, /decision-loop\.css\?v=1/);
});

test("decision-loop browser script is valid JavaScript and health-backed", async () => {
  const script = await readFile("public/assets/decision-loop.js", "utf8");

  assert.doesNotThrow(() => new vm.Script(script));
  assert.match(script, /fetch\("\/healthz"/);
  assert.match(script, /window\.setInterval\(tick, 1000\)/);
});
