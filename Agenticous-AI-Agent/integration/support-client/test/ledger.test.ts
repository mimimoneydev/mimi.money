import assert from "node:assert/strict";
import test from "node:test";
import { mkdtemp } from "node:fs/promises";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { AutonomyLedger, microusd } from "../src/ledger.js";

test("USD conversion is exact", () => {
  assert.equal(microusd("1.000001"), 1_000_001n);
  assert.equal(microusd("0.01"), 10_000n);
});

test("ledger enforces idempotency and daily budget", async () => {
  const directory = await mkdtemp(join(tmpdir(), "agenticous-ledger-"));
  const ledger = new AutonomyLedger(join(directory, "ledger.json"), "0.05");
  await ledger.initialize();
  let calls = 0;
  const action = () => { calls += 1; return Promise.resolve({ tx: "0x1" }); };
  assert.deepEqual(await ledger.execute("action:12345678", "x402", "0.03", { url: "https://example.com" }, action), { tx: "0x1" });
  assert.deepEqual(await ledger.execute("action:12345678", "x402", "0.03", { url: "https://example.com" }, action), { tx: "0x1" });
  assert.equal(calls, 1);
  await assert.rejects(() => ledger.execute("action:87654321", "x402", "0.03", {}, action), /daily budget/i);
  await assert.rejects(() => ledger.execute("action:12345678", "x402", "0.03", { url: "https://different.example" }, action), /different action/i);
});
