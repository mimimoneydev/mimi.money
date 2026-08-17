import assert from "node:assert/strict";
import test from "node:test";
import { ReportCache } from "../src/cache.js";
import type { TransactionReport } from "../src/types.js";

const report = {
  requestId: "request",
  address: "0x0000000000000000000000000000000000000001",
  generatedAt: new Date().toISOString(),
  period: { from: new Date().toISOString(), to: new Date().toISOString(), days: 7 },
  transactionLimit: 7,
  transactions: [],
  explorers: [],
  summary: { networksSearched: 0, networksUnavailable: 0, transactionsReturned: 0, partial: false },
  disclaimer: "test",
} satisfies TransactionReport;

test("cache keys wallet addresses case-insensitively", () => {
  const cache = new ReportCache(60);
  cache.set("0xABC", report);
  assert.equal(cache.get("0xabc"), report);
});

test("zero TTL disables caching", () => {
  const cache = new ReportCache(0);
  cache.set("0xabc", report);
  assert.equal(cache.get("0xabc"), undefined);
});
