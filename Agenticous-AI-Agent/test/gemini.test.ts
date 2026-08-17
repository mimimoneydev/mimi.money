import assert from "node:assert/strict";
import test from "node:test";
import { generateTransactionIntelligence } from "../src/gemini.js";
import type { TransactionReport } from "../src/types.js";

const report: TransactionReport = {
  requestId: "req-1",
  address: "0x0000000000000000000000000000000000000001",
  generatedAt: "2026-08-09T12:00:00.000Z",
  period: { from: "2026-08-02T12:00:00.000Z", to: "2026-08-09T12:00:00.000Z", days: 7 },
  transactionLimit: 7,
  transactions: [],
  explorers: [],
  summary: { networksSearched: 10, networksUnavailable: 0, transactionsReturned: 0, partial: false },
  disclaimer: "test",
};

const config = {
  geminiApiKey: "google-secret",
  openRouterApiKey: "openrouter-secret",
  lightModel: "gemini-2.5-flash-lite",
  intenseModel: "gemini-3.5-flash",
  openRouterLightModel: "google/gemini-2.5-flash-lite",
  openRouterIntenseModel: "google/gemini-3.5-flash",
  googleBaseUrl: "https://generativelanguage.googleapis.com/v1beta",
  openRouterProvider: "google-vertex/eu",
  openRouterBaseUrl: "https://openrouter.ai/api/v1",
  timeoutMs: 1000,
} as const;

const googleResponse = (overview = "No activity returned.") => new Response(JSON.stringify({
  candidates: [{ content: { parts: [{ text: JSON.stringify({ overview, notableActivity: [] }) }] } }],
}), { status: 200, headers: { "content-type": "application/json" } });

test("light intelligence uses Google AI Studio Gemini 2.5 Flash-Lite first", async () => {
  const fakeFetch: typeof fetch = async (input, init) => {
    assert.equal(String(input), "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent");
    assert.equal((init?.headers as Record<string, string>)["x-goog-api-key"], "google-secret");
    const request = JSON.parse(String(init?.body));
    assert.equal(request.generationConfig.responseMimeType, "application/json");
    assert.equal(request.generationConfig.responseSchema.additionalProperties, false);
    return googleResponse();
  };
  const result = await generateTransactionIntelligence(report, config, fakeFetch);
  assert.equal(result.status, "generated");
  assert.equal(result.provider, "google-ai-studio");
  assert.equal(result.model, "gemini-2.5-flash-lite");
});

test("light model failure escalates through pinned OpenRouter and then Gemini 3.5 Flash", async () => {
  const calls: string[] = [];
  const fakeFetch: typeof fetch = async (input, init) => {
    calls.push(String(input));
    if (calls.length === 1) return new Response("unavailable", { status: 503 });
    if (calls.length === 2) {
      const request = JSON.parse(String(init?.body));
      assert.equal(request.model, "google/gemini-2.5-flash-lite");
      assert.deepEqual(request.provider, { only: ["google-vertex/eu"], allow_fallbacks: false, require_parameters: true });
      return new Response("unavailable", { status: 503 });
    }
    assert.match(String(input), /gemini-3\.5-flash:generateContent$/);
    return googleResponse("Escalated summary.");
  };
  const result = await generateTransactionIntelligence(report, config, fakeFetch);
  assert.equal(result.status, "generated");
  assert.equal(result.model, "gemini-3.5-flash");
  assert.equal(result.overview, "Escalated summary.");
  assert.equal(calls.length, 3);
});

test("intense work starts on Gemini 3.5 Flash and uses only Vertex EU fallback", async () => {
  const calls: string[] = [];
  const fakeFetch: typeof fetch = async (input, init) => {
    calls.push(String(input));
    if (calls.length === 1) {
      assert.match(String(input), /gemini-3\.5-flash:generateContent$/);
      return new Response("unavailable", { status: 503 });
    }
    const request = JSON.parse(String(init?.body));
    assert.equal(request.model, "google/gemini-3.5-flash");
    assert.deepEqual(request.provider, { only: ["google-vertex/eu"], allow_fallbacks: false, require_parameters: true });
    return new Response(JSON.stringify({ choices: [{ message: { content: JSON.stringify({ overview: "Intense summary.", notableActivity: [] }) } }] }), { status: 200 });
  };
  const result = await generateTransactionIntelligence(report, { ...config, workload: "intense" }, fakeFetch);
  assert.equal(result.status, "generated");
  assert.equal(result.provider, "openrouter");
  assert.equal(result.upstreamProvider, "google-vertex/eu");
  assert.equal(calls.length, 2);
});

test("all model failures preserve explorer results", async () => {
  const fakeFetch: typeof fetch = async () => new Response("unavailable", { status: 503 });
  const result = await generateTransactionIntelligence(report, config, fakeFetch);
  assert.equal(result.status, "unavailable");
  assert.match(result.note ?? "", /explorer results remain available/i);
});
