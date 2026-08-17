import "dotenv/config";
import { getAddress, isAddress } from "viem";

export type AppConfig = {
  host: string;
  port: number;
  trustProxy: number;
  publicOrigin: string;
  sellerAddress: `0x${string}`;
  facilitatorUrl: string;
  paymentNetwork: `eip155:${string}`;
  reportPriceUsd: string;
  explorerTimeoutMs: number;
  reportCacheSeconds: number;
  geminiApiKey?: string;
  openRouterApiKey?: string;
  geminiLightModel: string;
  geminiIntenseModel: string;
  openRouterLightModel: string;
  openRouterIntenseModel: string;
  geminiApiBaseUrl: string;
  openRouterProvider: string;
  openRouterBaseUrl: string;
  geminiTimeoutMs: number;
  openClawUrl?: string;
  openClawToken?: string;
  openClawModel: string;
  openClawTimeoutMs: number;
  runStorePath?: string;
  maximumPublicAgentSpendUsd: string;
};

function integer(name: string, fallback: number, minimum: number): number {
  const raw = process.env[name];
  const value = raw ? Number(raw) : fallback;
  if (!Number.isInteger(value) || value < minimum) {
    throw new Error(`${name} must be an integer greater than or equal to ${minimum}`);
  }
  return value;
}

function usd(name: string, fallback: string): string {
  const value = process.env[name] ?? fallback;
  if (!/^\d+(?:\.\d{1,6})?$/.test(value) || Number(value) <= 0) {
    throw new Error(`${name} must be a positive decimal amount`);
  }
  return value;
}

export function loadConfig(): AppConfig {
  const sellerAddress = process.env.SELLER_ADDRESS ?? "";
  if (!isAddress(sellerAddress) || /^0x0{40}$/i.test(sellerAddress)) {
    throw new Error("SELLER_ADDRESS must be a non-zero EVM address");
  }

  const paymentNetwork = process.env.PAYMENT_NETWORK ?? "eip155:8453";
  if (!/^eip155:\d+$/.test(paymentNetwork)) {
    throw new Error("PAYMENT_NETWORK must be an EVM CAIP-2 network identifier");
  }

  const facilitatorUrl = process.env.FACILITATOR_URL ?? "https://x402.mimi.money";
  const parsedFacilitator = new URL(facilitatorUrl);
  if (!(["https:", "http:"].includes(parsedFacilitator.protocol))) {
    throw new Error("FACILITATOR_URL must be an HTTP(S) URL");
  }
  const publicOrigin = new URL(process.env.PUBLIC_ORIGIN ?? "https://agenticous.mimi.money");
  if (!["https:", "http:"].includes(publicOrigin.protocol) || publicOrigin.pathname !== "/") {
    throw new Error("PUBLIC_ORIGIN must be an HTTP(S) origin without a path");
  }

  const geminiLightModel = process.env.GEMINI_LIGHT_MODEL?.trim() || "gemini-2.5-flash-lite";
  const geminiIntenseModel = process.env.GEMINI_INTENSE_MODEL?.trim() || "gemini-3.5-flash";
  const openRouterLightModel = process.env.OPENROUTER_LIGHT_MODEL?.trim() || "google/gemini-2.5-flash-lite";
  const openRouterIntenseModel = process.env.OPENROUTER_INTENSE_MODEL?.trim() || "google/gemini-3.5-flash";
  if (geminiLightModel !== "gemini-2.5-flash-lite" || geminiIntenseModel !== "gemini-3.5-flash") {
    throw new Error("Google AI Studio models must be gemini-2.5-flash-lite and gemini-3.5-flash");
  }
  if (openRouterLightModel !== "google/gemini-2.5-flash-lite" || openRouterIntenseModel !== "google/gemini-3.5-flash") {
    throw new Error("OpenRouter models must be google/gemini-2.5-flash-lite and google/gemini-3.5-flash");
  }
  const geminiApiBaseUrl = new URL(process.env.GEMINI_API_BASE_URL?.trim() || "https://generativelanguage.googleapis.com/v1beta");
  if (geminiApiBaseUrl.protocol !== "https:" || geminiApiBaseUrl.hostname !== "generativelanguage.googleapis.com" || geminiApiBaseUrl.pathname.replace(/\/$/, "") !== "/v1beta") {
    throw new Error("GEMINI_API_BASE_URL must be https://generativelanguage.googleapis.com/v1beta");
  }
  const openRouterProvider = process.env.OPENROUTER_PROVIDER?.trim() || "google-vertex/eu";
  if (openRouterProvider !== "google-vertex/eu") {
    throw new Error("OPENROUTER_PROVIDER must be google-vertex/eu");
  }
  const openRouterBaseUrl = new URL(process.env.OPENROUTER_BASE_URL?.trim() || "https://openrouter.ai/api/v1");
  if (openRouterBaseUrl.protocol !== "https:" || openRouterBaseUrl.hostname !== "openrouter.ai" || openRouterBaseUrl.pathname.replace(/\/$/, "") !== "/api/v1") {
    throw new Error("OPENROUTER_BASE_URL must be https://openrouter.ai/api/v1");
  }

  const openClawRaw = process.env.OPENCLAW_URL?.trim();
  const openClawUrl = openClawRaw ? new URL(openClawRaw) : undefined;
  if (openClawUrl && !(openClawUrl.protocol === "http:" && ["127.0.0.1", "::1", "localhost"].includes(openClawUrl.hostname))) {
    throw new Error("OPENCLAW_URL must use HTTP on loopback");
  }
  const runStorePath = process.env.AGENT_RUN_STORE_PATH?.trim() || undefined;
  if (runStorePath && !runStorePath.startsWith("/var/lib/agenticous/")) {
    throw new Error("AGENT_RUN_STORE_PATH must be below /var/lib/agenticous");
  }

  return {
    host: process.env.HOST ?? "127.0.0.1",
    port: integer("PORT", 4410, 1),
    trustProxy: integer("TRUST_PROXY", 1, 0),
    publicOrigin: publicOrigin.origin,
    sellerAddress: getAddress(sellerAddress),
    facilitatorUrl: parsedFacilitator.toString().replace(/\/$/, ""),
    paymentNetwork: paymentNetwork as `eip155:${string}`,
    reportPriceUsd: usd("REPORT_PRICE_USD", "0.01"),
    explorerTimeoutMs: integer("EXPLORER_TIMEOUT_MS", 10_000, 500),
    reportCacheSeconds: integer("REPORT_CACHE_SECONDS", 60, 0),
    geminiApiKey: process.env.GEMINI_API_KEY?.trim() || undefined,
    openRouterApiKey: process.env.OPENROUTER_API_KEY?.trim() || undefined,
    geminiLightModel,
    geminiIntenseModel,
    openRouterLightModel,
    openRouterIntenseModel,
    geminiApiBaseUrl: geminiApiBaseUrl.toString().replace(/\/$/, ""),
    openRouterProvider,
    openRouterBaseUrl: openRouterBaseUrl.toString().replace(/\/$/, ""),
    geminiTimeoutMs: integer("GEMINI_TIMEOUT_MS", 12_000, 1_000),
    openClawUrl: openClawUrl?.toString().replace(/\/$/, ""),
    openClawToken: process.env.OPENCLAW_TOKEN?.trim() || undefined,
    openClawModel: process.env.OPENCLAW_MODEL?.trim() || "openrouter/google/gemini-3.6-flash",
    openClawTimeoutMs: integer("OPENCLAW_TIMEOUT_MS", 45_000, 1_000),
    runStorePath,
    maximumPublicAgentSpendUsd: usd("MAXIMUM_PUBLIC_AGENT_SPEND_USD", "0.05"),
  };
}
