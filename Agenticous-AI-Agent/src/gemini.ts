import type { TransactionReport } from "./types.js";

type GeminiConfig = {
  geminiApiKey?: string;
  openRouterApiKey?: string;
  lightModel: string;
  intenseModel: string;
  openRouterLightModel: string;
  openRouterIntenseModel: string;
  googleBaseUrl: string;
  openRouterProvider: string;
  openRouterBaseUrl: string;
  timeoutMs: number;
  workload?: "auto" | "light" | "intense";
};

type Intelligence = NonNullable<TransactionReport["intelligence"]>;
type Route = {
  provider: "google-ai-studio" | "openrouter";
  upstreamProvider: "google-ai-studio" | "google-vertex/eu";
  model: string;
};

const responseSchema = {
  type: "object",
  additionalProperties: false,
  properties: {
    overview: { type: "string" },
    notableActivity: { type: "array", items: { type: "string" }, maxItems: 3 },
  },
  required: ["overview", "notableActivity"],
} as const;

function cleanStrings(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return value.filter((item): item is string => typeof item === "string")
    .map(item => item.trim()).filter(Boolean).slice(0, 3);
}

function parseIntelligence(raw: string): { overview: string; notableActivity: string[] } {
  const parsed = JSON.parse(raw) as { overview?: unknown; notableActivity?: unknown };
  if (typeof parsed.overview !== "string" || !parsed.overview.trim()) throw new Error("Model returned an invalid summary");
  return { overview: parsed.overview.trim(), notableActivity: cleanStrings(parsed.notableActivity) };
}

function prompt(report: TransactionReport): { system: string; user: string } {
  const evidence = { address: report.address, period: report.period, summary: report.summary, transactions: report.transactions };
  return {
    system: "You summarize blockchain explorer evidence for customer support. Use only the supplied JSON. Never infer wallet ownership, identity, intent, risk, fraud, or missing activity. Keep amounts, chains, directions, timestamps, and status exact. State when the result is partial.",
    user: `Summarize this seven-day wallet report for a support reply:\n${JSON.stringify(evidence)}`,
  };
}

async function googleAiStudio(
  route: Route,
  apiKey: string,
  baseUrl: string,
  messages: ReturnType<typeof prompt>,
  signal: AbortSignal,
  fetcher: typeof fetch,
): Promise<string> {
  const response = await fetcher(`${baseUrl}/models/${encodeURIComponent(route.model)}:generateContent`, {
    method: "POST",
    signal,
    headers: { "content-type": "application/json", "x-goog-api-key": apiKey },
    body: JSON.stringify({
      systemInstruction: { parts: [{ text: messages.system }] },
      contents: [{ role: "user", parts: [{ text: messages.user }] }],
      generationConfig: { responseMimeType: "application/json", responseSchema, maxOutputTokens: 350 },
    }),
  });
  if (!response.ok) throw new Error(`Google AI Studio returned HTTP ${response.status}`);
  const body = await response.json() as { candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }> };
  const raw = body.candidates?.[0]?.content?.parts?.map(part => part.text ?? "").join("").trim();
  if (!raw) throw new Error("Google AI Studio returned no text");
  return raw;
}

async function openRouter(
  route: Route,
  apiKey: string,
  config: GeminiConfig,
  messages: ReturnType<typeof prompt>,
  signal: AbortSignal,
  fetcher: typeof fetch,
): Promise<string> {
  const response = await fetcher(`${config.openRouterBaseUrl}/chat/completions`, {
    method: "POST",
    signal,
    headers: {
      "content-type": "application/json",
      authorization: `Bearer ${apiKey}`,
      "http-referer": "https://agenticous.mimi.money",
      "x-title": "Agenticous AI agent for MiMi Money Support",
    },
    body: JSON.stringify({
      model: route.model,
      provider: { only: [config.openRouterProvider], allow_fallbacks: false, require_parameters: true },
      messages: [{ role: "system", content: messages.system }, { role: "user", content: messages.user }],
      response_format: { type: "json_schema", json_schema: { name: "wallet_activity_summary", strict: true, schema: responseSchema } },
      max_tokens: 350,
    }),
  });
  if (!response.ok) throw new Error(`OpenRouter returned HTTP ${response.status}`);
  const body = await response.json() as { choices?: Array<{ message?: { content?: string } }> };
  const raw = body.choices?.[0]?.message?.content?.trim();
  if (!raw) throw new Error("OpenRouter returned no text");
  return raw;
}

function routes(report: TransactionReport, config: GeminiConfig): Route[] {
  const intense = config.workload === "intense" || (config.workload !== "light" && report.transactions.length >= 4);
  const route = (provider: Route["provider"], model: string): Route => ({
    provider,
    upstreamProvider: provider === "google-ai-studio" ? "google-ai-studio" : "google-vertex/eu",
    model,
  });
  if (intense) return [route("google-ai-studio", config.intenseModel), route("openrouter", config.openRouterIntenseModel)];
  return [
    route("google-ai-studio", config.lightModel),
    route("openrouter", config.openRouterLightModel),
    route("google-ai-studio", config.intenseModel),
    route("openrouter", config.openRouterIntenseModel),
  ];
}

export async function generateTransactionIntelligence(
  report: TransactionReport,
  config: GeminiConfig,
  fetcher: typeof fetch = fetch,
): Promise<Intelligence> {
  if (!config.geminiApiKey && !config.openRouterApiKey) {
    return { provider: "google-ai-studio", upstreamProvider: "google-ai-studio", model: config.lightModel, status: "unavailable", note: "Gemini API and OpenRouter API keys are not configured." };
  }

  const messages = prompt(report);
  const deadline = Date.now() + config.timeoutMs;
  const failures: string[] = [];
  for (const route of routes(report, config)) {
    const key = route.provider === "google-ai-studio" ? config.geminiApiKey : config.openRouterApiKey;
    if (!key) continue;
    const remaining = deadline - Date.now();
    if (remaining <= 0) break;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), Math.min(6_000, remaining));
    try {
      const raw = route.provider === "google-ai-studio"
        ? await googleAiStudio(route, key, config.googleBaseUrl, messages, controller.signal, fetcher)
        : await openRouter(route, key, config, messages, controller.signal, fetcher);
      const parsed = parseIntelligence(raw);
      return { ...route, status: "generated", ...parsed };
    } catch (error) {
      failures.push(`${route.provider}/${route.model}: ${error instanceof Error ? error.message : "unknown error"}`);
    } finally {
      clearTimeout(timeout);
    }
  }

  console.warn(JSON.stringify({ level: "warn", message: "Gemini intelligence unavailable; returning verified explorer data", attempts: failures, requestId: report.requestId }));
  return { provider: "google-ai-studio", upstreamProvider: "google-ai-studio", model: config.lightModel, status: "unavailable", note: "AI summary was unavailable; explorer results remain available." };
}
