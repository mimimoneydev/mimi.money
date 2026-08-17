import assert from "node:assert/strict";
import { mkdtemp, readFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import { UsageMeter } from "../src/usage.js";

test("meter applies free tier and deduplicates settlements", async () => {
  const directory = await mkdtemp(join(tmpdir(), "mimi-x402-"));
  const filename = join(directory, "usage.json");
  const meter = new UsageMeter(filename, 1, 0.001);
  await meter.initialize();
  assert.equal((await meter.record("tx-1")).accruedFeeUsd, 0);
  assert.equal((await meter.record("tx-1")).settled, 1);
  const snapshot = await meter.record("tx-2");
  assert.equal(snapshot.billableSettlements, 1);
  assert.equal(snapshot.accruedFeeUsd, 0.001);
  const persisted = await readFile(filename, "utf8");
  assert.doesNotThrow(() => JSON.parse(persisted));
});
