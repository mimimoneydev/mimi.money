import { randomUUID } from "node:crypto";
import express, { type NextFunction, type Request, type Response } from "express";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import { isAddress } from "viem";
import { HTTPFacilitatorClient, x402ResourceServer } from "@x402/core/server";
import { paymentMiddleware } from "@x402/express";
import { BatchFacilitatorClient, GatewayEvmScheme } from "@circle-fin/x402-batching/server";
import { loadConfig } from "./config.js";
import { ReportCache } from "./cache.js";
import { createTransactionReport, EXPLORERS } from "./explorers.js";
import { generateTransactionIntelligence } from "./gemini.js";
import { askBlockchainAgent } from "./openclaw.js";
import { gatewayPaymentOption } from "./payments.js";
import { AgentRunStore } from "./runs.js";
import type { AgentRun } from "./types.js";

const config = loadConfig();
const app = express();
const cache = new ReportCache(config.reportCacheSeconds);
const runStore = new AgentRunStore(config.runStorePath);
await runStore.initialize();

app.disable("x-powered-by");
app.set("trust proxy", config.trustProxy);
app.use((req, _res, next) => {
  const origin = new URL(config.publicOrigin);
  req.headers["x-forwarded-proto"] = origin.protocol.slice(0, -1);
  req.headers["x-forwarded-host"] = origin.host;
  next();
});
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      imgSrc: ["'self'", "data:"],
      styleSrc: ["'self'"],
      scriptSrc: ["'self'"],
      connectSrc: ["'self'"],
    },
  },
  crossOriginResourcePolicy: { policy: "same-origin" },
}));
app.use(express.json({ limit: "16kb", type: ["application/json", "application/*+json"] }));
app.use((req, res, next) => {
  const supplied = req.header("x-request-id");
  const requestId = supplied && /^[a-zA-Z0-9._:-]{1,128}$/.test(supplied) ? supplied : randomUUID();
  res.setHeader("x-request-id", requestId);
  next();
});

const apiLimiter = rateLimit({
  windowMs: 60_000,
  limit: 60,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  message: { error: "Too many requests. Please retry shortly." },
});
app.use("/v1", apiLimiter);

app.get("/healthz", (_req, res) => {
  res.json({
    status: "ok",
    service: "agenticous-ai-agent",
    version: "1.1.0",
    intelligence: {
      primaryProvider: "google-ai-studio",
      fallbackProvider: `openrouter/${config.openRouterProvider}`,
      lightModel: config.geminiLightModel,
      intenseModel: config.geminiIntenseModel,
      primaryConfigured: Boolean(config.geminiApiKey),
      fallbackConfigured: Boolean(config.openRouterApiKey),
      configured: Boolean(config.geminiApiKey || config.openRouterApiKey),
    },
    orchestration: { provider: "openclaw", configured: Boolean(config.openClawUrl && config.openClawToken), model: config.openClawModel },
    payments: { provider: "circle-gateway", fallbackFacilitator: config.facilitatorUrl },
  });
});

app.get("/v1/capabilities", (_req, res) => {
  res.json({
    service: "Agenticous AI agent",
    price: `$${config.reportPriceUsd}`,
    payment: {
      protocol: "x402",
      network: config.paymentNetwork,
      provider: "Circle Gateway Nanopayments",
      facilitator: "Circle Gateway",
      fallbackFacilitator: config.facilitatorUrl,
      receivingAddress: config.sellerAddress,
      basescanAddressUrl: `https://basescan.org/address/${config.sellerAddress}`,
    },
    intelligence: {
      primaryProvider: "google-ai-studio",
      fallbackProvider: `openrouter/${config.openRouterProvider}`,
      lightModel: config.geminiLightModel,
      intenseModel: config.geminiIntenseModel,
      routing: "Light reports use Gemini 2.5 Flash-Lite and escalate to Gemini 3.5 Flash on failure; intensive reports start on Gemini 3.5 Flash.",
    },
    report: { periodDays: 7, maximumTransactions: 7 },
    agent: {
      endpoint: "/v1/agent/runs",
      authorityModes: ["read-only", "propose", "autonomous"],
      maximumAutonomousSpendUsd: config.maximumPublicAgentSpendUsd,
      execution: "Autonomous runs may execute without human approval through independently policy-controlled Circle Agent Wallet tools.",
    },
    networks: EXPLORERS.map(item => ({
      network: item.network,
      chainId: item.chainId,
      name: item.name,
      explorer: item.explorerBase,
      testnet: Boolean(item.testnet),
    })),
  });
});

const fallbackFacilitator = new HTTPFacilitatorClient({ url: config.facilitatorUrl });
const circleFacilitator = new BatchFacilitatorClient();
circleFacilitator
  .onAfterVerify(async ({ requirements, result }) => {
    console.info(JSON.stringify({
      level: result.isValid ? "info" : "warn",
      event: "circle_gateway_verify",
      valid: result.isValid,
      reason: result.invalidReason,
      network: requirements.network,
      amount: requirements.amount,
      timeoutSeconds: requirements.maxTimeoutSeconds,
    }));
  })
  .onVerifyFailure(async ({ requirements, error }) => {
    console.error(JSON.stringify({
      level: "error",
      event: "circle_gateway_verify_error",
      message: error.message,
      network: requirements.network,
      amount: requirements.amount,
      timeoutSeconds: requirements.maxTimeoutSeconds,
    }));
  })
  .onAfterSettle(async ({ requirements, result }) => {
    console[result.success ? "info" : "error"](JSON.stringify({
      level: result.success ? "info" : "error",
      event: "circle_gateway_settle",
      success: result.success,
      reason: result.errorReason,
      network: result.network || requirements.network,
      transaction: result.transaction || undefined,
      amount: requirements.amount,
      timeoutSeconds: requirements.maxTimeoutSeconds,
    }));
  })
  .onSettleFailure(async ({ requirements, error }) => {
    console.error(JSON.stringify({
      level: "error",
      event: "circle_gateway_settle_error",
      message: error.message,
      network: requirements.network,
      amount: requirements.amount,
      timeoutSeconds: requirements.maxTimeoutSeconds,
    }));
  });
// Circle 3.3.0's FacilitatorClient declaration narrows an optional core field,
// although the wire/runtime contract is the same x402 v2 interface.
const resourceServer = new x402ResourceServer([circleFacilitator as never, fallbackFacilitator]);
resourceServer.register(config.paymentNetwork, new GatewayEvmScheme());

app.use(paymentMiddleware({
  "POST /v1/reports/transactions": {
    accepts: [gatewayPaymentOption(config)],
    description: "Search MiMi-supported blockchain explorers for up to seven recent transactions in the previous seven days.",
    mimeType: "application/json",
  },
  "POST /v1/agent/runs": {
    accepts: [gatewayPaymentOption(config)],
    description: "Run an evidence-backed OpenClaw blockchain investigation for an EVM wallet. Financial execution is not exposed by this endpoint.",
    mimeType: "application/json",
  },
}, resourceServer));

app.get("/v1/agent/runs/:id", (req, res) => {
  if (!/^[0-9a-f-]{36}$/i.test(req.params.id)) return res.status(422).json({ error: "Invalid run id." });
  const run = runStore.get(req.params.id);
  return run ? res.json(run) : res.status(404).json({ error: "Agent run not found." });
});

app.post("/v1/reports/transactions", async (req, res, next) => {
  try {
    const address = typeof req.body?.address === "string" ? req.body.address.trim() : "";
    if (!isAddress(address)) {
      return res.status(422).json({ error: "A valid EVM wallet address is required." });
    }
    const cached = cache.get(address);
    if (cached) return res.json({ ...cached, cached: true });
    const report = await createTransactionReport(address, config.explorerTimeoutMs);
    report.intelligence = await generateTransactionIntelligence(report, intelligenceConfig());
    cache.set(address, report);
    return res.json({ ...report, cached: false });
  } catch (error) {
    return next(error);
  }
});

app.post("/v1/agent/runs", async (req, res, next) => {
  const createdAt = new Date().toISOString();
  const id = randomUUID();
  const address = typeof req.body?.address === "string" ? req.body.address.trim() : "";
  const intent = typeof req.body?.intent === "string" ? req.body.intent.trim() : "";
  const requestedMode = req.body?.authority?.mode;
  const mode = requestedMode === "autonomous" ? "autonomous" : requestedMode === "propose" ? "propose" : "read-only";
  const maximumExternalSpendUsd = typeof req.body?.authority?.maximumExternalSpendUsd === "string"
    ? req.body.authority.maximumExternalSpendUsd.trim()
    : "0";
  if (!isAddress(address)) return res.status(422).json({ error: "A valid EVM wallet address is required." });
  if (!intent || intent.length > 2_000) return res.status(422).json({ error: "Intent must contain 1 to 2000 characters." });
  if (!/^\d+(?:\.\d{1,6})?$/.test(maximumExternalSpendUsd) || Number(maximumExternalSpendUsd) > Number(config.maximumPublicAgentSpendUsd)) {
    return res.status(422).json({ error: `maximumExternalSpendUsd must be between 0 and ${config.maximumPublicAgentSpendUsd}.` });
  }
  if (mode !== "autonomous" && Number(maximumExternalSpendUsd) !== 0) return res.status(422).json({ error: "Only autonomous runs may request external spend." });

  const run: AgentRun = {
    id,
    status: "running",
    intent,
    address,
    createdAt,
    updatedAt: createdAt,
    authority: { mode, maximumExternalSpendUsd },
    evidence: [],
    orchestration: { provider: "openclaw", status: "unavailable", note: "Run is starting." },
  };
  try {
    await runStore.set(run);
    return res.json(await executeAgentRun(run));
  } catch (error) {
    const failed: AgentRun = {
      ...run,
      status: "failed",
      updatedAt: new Date().toISOString(),
      error: "Agent run failed safely.",
      orchestration: { provider: "openclaw", status: "unavailable", note: error instanceof Error ? error.message : "Unknown error" },
    };
    await runStore.set(failed).catch(() => undefined);
    return next(error);
  }
});

async function executeAgentRun(run: AgentRun): Promise<AgentRun> {
  try {
    const report = await createTransactionReport(run.address, config.explorerTimeoutMs);
    report.intelligence = await generateTransactionIntelligence(report, intelligenceConfig());
    const orchestration = await askBlockchainAgent(run.intent, report, run.authority, {
      url: config.openClawUrl, token: config.openClawToken, model: config.openClawModel, timeoutMs: config.openClawTimeoutMs,
    });
    const completed: AgentRun = { ...run, status: "completed", updatedAt: new Date().toISOString(), report,
      answer: orchestration.answer, evidence: [{ id: "evidence:explorer-report", kind: "explorer-report", requestId: report.requestId }],
      orchestration: { provider: "openclaw", ...orchestration } };
    await runStore.set(completed);
    return completed;
  } catch (error) {
    const failed: AgentRun = { ...run, status: "failed", updatedAt: new Date().toISOString(), error: "Agent run failed safely.",
      orchestration: { provider: "openclaw", status: "unavailable", note: error instanceof Error ? error.message : "Unknown error" } };
    await runStore.set(failed).catch(() => undefined);
    throw error;
  }
}

function intelligenceConfig() {
  return {
    geminiApiKey: config.geminiApiKey,
    openRouterApiKey: config.openRouterApiKey,
    lightModel: config.geminiLightModel,
    intenseModel: config.geminiIntenseModel,
    openRouterLightModel: config.openRouterLightModel,
    openRouterIntenseModel: config.openRouterIntenseModel,
    googleBaseUrl: config.geminiApiBaseUrl,
    openRouterProvider: config.openRouterProvider,
    openRouterBaseUrl: config.openRouterBaseUrl,
    timeoutMs: config.geminiTimeoutMs,
  };
}

app.use(express.static("public", {
  extensions: ["html"],
  maxAge: "1h",
  setHeaders: response => response.setHeader("cache-control", "public, max-age=3600"),
}));

app.use((req, res) => {
  res.status(404).json({ error: "Not found", path: req.path });
});

app.use((error: unknown, req: Request, res: Response, _next: NextFunction) => {
  const requestId = String(res.getHeader("x-request-id") ?? "");
  console.error(JSON.stringify({
    level: "error",
    message: error instanceof Error ? error.message : "Unexpected error",
    method: req.method,
    path: req.path,
    requestId,
  }));
  if (!res.headersSent) res.status(500).json({ error: "Agenticous AI agent request failed safely.", requestId });
});

const server = app.listen(config.port, config.host, () => {
  console.info(JSON.stringify({
    level: "info",
    message: "Agenticous AI agent started",
    listen: `${config.host}:${config.port}`,
    sellerAddress: config.sellerAddress,
    network: config.paymentNetwork,
    price: `$${config.reportPriceUsd}`,
  }));
});

for (const interrupted of runStore.list("running")) {
  void executeAgentRun({ ...interrupted, updatedAt: new Date().toISOString() }).catch(error => {
    console.error(JSON.stringify({ level: "error", message: "Interrupted agent run could not resume", runId: interrupted.id, reason: error instanceof Error ? error.message : "unknown" }));
  });
}

function shutdown(signal: string): void {
  console.info(JSON.stringify({ level: "info", message: `Received ${signal}; shutting down` }));
  server.close(error => {
    if (error) {
      console.error(error);
      process.exitCode = 1;
    }
  });
  setTimeout(() => process.exit(1), 10_000).unref();
}

process.on("SIGTERM", () => shutdown("SIGTERM"));
process.on("SIGINT", () => shutdown("SIGINT"));
