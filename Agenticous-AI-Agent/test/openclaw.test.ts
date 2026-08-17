import assert from "node:assert/strict";
import test from "node:test";
import { askBlockchainAgent } from "../src/openclaw.js";
import type { TransactionReport } from "../src/types.js";

const report: TransactionReport = {
  requestId: "req", address: "0x0000000000000000000000000000000000000001",
  generatedAt: "2026-08-13T00:00:00.000Z",
  period: { from: "2026-08-06T00:00:00.000Z", to: "2026-08-13T00:00:00.000Z", days: 7 },
  transactionLimit: 7, transactions: [], explorers: [],
  summary: { networksSearched: 0, networksUnavailable: 0, transactionsReturned: 0, partial: false },
  disclaimer: "test",
};

test("OpenClaw orchestration fails open to deterministic evidence when unconfigured", async () => {
  const result = await askBlockchainAgent("inspect", report, { mode: "read-only", maximumExternalSpendUsd: "0" }, { model: "openrouter/google/gemini-3.6-flash", timeoutMs: 1000 });
  assert.equal(result.status, "unavailable");
  assert.match(result.note ?? "", /evidence remains available/i);
});

test("OpenClaw request pins safe authority and evidence instructions", async () => {
  const result = await askBlockchainAgent("inspect", report, { mode: "propose", maximumExternalSpendUsd: "0" }, {
    url: "http://127.0.0.1:18789", token: "secret", model: "google/gemini", timeoutMs: 1000,
  }, async (_url, init) => {
    const body = JSON.parse(String(init?.body));
    assert.equal(body.model, "openclaw");
    assert.equal(new Headers(init?.headers).get("x-openclaw-model"), "google/gemini");
    assert.match(body.messages[0].content, /cannot execute money-moving actions/i);
    assert.match(body.messages[1].content, /evidence:explorer-report/);
    return new Response(JSON.stringify({ choices: [{ message: { content: "Evidence-backed answer [evidence:explorer-report]." } }] }), { status: 200 });
  });
  assert.equal(result.status, "generated");
});

test("autonomous OpenClaw requests remove human approval and retain machine policy boundaries", async () => {
  const result = await askBlockchainAgent("pay for evidence", report, { mode: "autonomous", maximumExternalSpendUsd: "0.05" }, {
    url: "http://127.0.0.1:18789", token: "secret", model: "openrouter/google/gemini-3.6-flash", timeoutMs: 1000,
  }, async (_url, init) => {
    const body = JSON.parse(String(init?.body));
    assert.equal(body.model, "openclaw");
    assert.equal(new Headers(init?.headers).get("x-openclaw-model"), "openrouter/google/gemini-3.6-flash");
    assert.match(body.messages[0].content, /without human approval/i);
    assert.match(body.messages[0].content, /independent budgets/i);
    return new Response(JSON.stringify({ choices: [{ message: { content: "Executed with receipt." } }] }), { status: 200 });
  });
  assert.equal(result.status, "generated");
});
