import assert from "node:assert/strict";
import test from "node:test";
import { AgentRunStore } from "../src/runs.js";
import type { AgentRun } from "../src/types.js";

test("agent run store preserves state in memory", async () => {
  const store = new AgentRunStore();
  await store.initialize();
  const run: AgentRun = {
    id: "run", status: "running", intent: "inspect", address: "0x0000000000000000000000000000000000000001",
    createdAt: "2026-08-13T00:00:00.000Z", updatedAt: "2026-08-13T00:00:00.000Z",
    authority: { mode: "read-only", maximumExternalSpendUsd: "0" }, evidence: [],
    orchestration: { provider: "openclaw", status: "unavailable" },
  };
  await store.set(run);
  assert.deepEqual(store.get("run"), run);
  assert.deepEqual(store.list("completed"), []);
  assert.deepEqual(store.list("running"), [run]);
});
