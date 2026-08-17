import "dotenv/config";
import { randomUUID } from "node:crypto";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import express, { type NextFunction, type Request, type Response } from "express";
import rateLimit from "express-rate-limit";
import helmet from "helmet";
import type { PaymentPayload, PaymentRequirements, SettleResponse, VerifyResponse } from "@x402/core/types";
import { ComplianceScreener } from "./compliance.js";
import { NETWORK_CATALOG, loadConfig } from "./config.js";
import { createFacilitator, isConfiguredNetwork, transactionId } from "./facilitator.js";
import { UsageMeter } from "./usage.js";

const config = loadConfig();
const { facilitator, address, rpcByNetwork } = createFacilitator(config);
const usage = new UsageMeter(config.usageDataFile, config.freeSettlements, config.settlementFeeUsd);
await usage.initialize();
const compliance = new ComplianceScreener(config);
const app = express();
const publicDir = join(dirname(fileURLToPath(import.meta.url)), "../../public");

app.set("trust proxy", config.trustProxy);
app.disable("x-powered-by");
app.use((req, res, next) => {
  const requestId = req.header("x-request-id")?.slice(0, 128) || randomUUID();
  res.setHeader("x-request-id", requestId);
  res.setHeader("access-control-allow-origin", "*");
  res.setHeader("access-control-allow-methods", "GET,POST,OPTIONS");
  res.setHeader("access-control-allow-headers", "Content-Type,X-Request-ID");
  if (req.method === "OPTIONS") return res.sendStatus(204);
  next();
});
app.use(helmet({ contentSecurityPolicy: false, crossOriginResourcePolicy: { policy: "cross-origin" } }));
app.use(express.json({ limit: "128kb", strict: true }));
app.use(
  "/v1",
  rateLimit({
    windowMs: 60_000,
    limit: 120,
    standardHeaders: "draft-7",
    legacyHeaders: false,
  }),
);

app.get("/healthz", (_req, res) => {
  res.json({
    status: "ok",
    service: "mimi-money-x402-facilitator",
    x402Version: 2,
    facilitatorAddress: address,
    networks: config.networks,
    compliance: compliance.enabled ? "configured" : "not-configured",
    compliancePolicy: config.complianceFailClosed ? "fail-closed" : "fail-open",
    uptimeSeconds: Math.floor(process.uptime()),
  });
});

app.get(["/supported", "/v1/supported"], (_req, res) => res.json(facilitator.getSupported()));
app.get("/v1/pricing", (_req, res) => res.json(usage.snapshot()));
app.get("/v1/networks", (_req, res) => {
  res.json(
    config.networks.map(network => ({
      id: network,
      chainId: NETWORK_CATALOG[network].id,
      name: NETWORK_CATALOG[network].name,
      testnet: Boolean(NETWORK_CATALOG[network].testnet),
    })),
  );
});

app.post(["/verify", "/v1/verify"], async (req, res, next) => {
  try {
    const input = paymentInput(req);
    ensureSupported(input.paymentPayload, input.paymentRequirements);
    const screening = await compliance.screen(input.paymentPayload, input.paymentRequirements);
    if (!screening.allowed) {
      return res.status(403).json({ isValid: false, invalidReason: screening.reason ?? "Screening denied" });
    }
    const response: VerifyResponse = await facilitator.verify(
      input.paymentPayload,
      input.paymentRequirements,
    );
    return res.json(response);
  } catch (error) {
    return next(error);
  }
});

app.post(["/settle", "/v1/settle"], async (req, res, next) => {
  try {
    const input = paymentInput(req);
    ensureSupported(input.paymentPayload, input.paymentRequirements);
    const screening = await compliance.screen(input.paymentPayload, input.paymentRequirements);
    if (!screening.allowed) {
      return res.status(403).json({
        success: false,
        errorReason: screening.reason ?? "Screening denied",
        network: input.paymentPayload.accepted.network,
      } satisfies Partial<SettleResponse>);
    }
    const response: SettleResponse = await facilitator.settle(
      input.paymentPayload,
      input.paymentRequirements,
    );
    if (response.success) await usage.record(transactionId(response, input.paymentPayload));
    return res.json(response);
  } catch (error) {
    return next(error);
  }
});

app.use(express.static(publicDir, { extensions: ["html"], maxAge: "1h", index: "index.html" }));
app.use((req, res) => res.status(404).json({ error: "Not found", path: req.path }));
app.use((error: unknown, req: Request, res: Response, _next: NextFunction) => {
  const message = error instanceof Error ? error.message : "Unexpected error";
  const status = message.startsWith("Missing ") || message.startsWith("Unsupported ") ? 400 : 500;
  console.error(JSON.stringify({ level: "error", message, path: req.path, requestId: res.getHeader("x-request-id") }));
  res.status(status).json({ error: status === 500 ? "Facilitator request failed" : message });
});

const server = app.listen(config.port, config.host, () => {
  console.info(
    JSON.stringify({
      level: "info",
      message: "MiMi Money x402 facilitator started",
      listen: `${config.host}:${config.port}`,
      address,
      networks: config.networks,
      rpcNetworks: Object.keys(rpcByNetwork),
      compliance: compliance.enabled,
    }),
  );
});

for (const signal of ["SIGINT", "SIGTERM"] as const) {
  process.once(signal, () => {
    console.info(JSON.stringify({ level: "info", message: `Received ${signal}; shutting down` }));
    server.close(error => process.exit(error ? 1 : 0));
    setTimeout(() => process.exit(1), 10_000).unref();
  });
}

function paymentInput(req: Request): {
  paymentPayload: PaymentPayload;
  paymentRequirements: PaymentRequirements;
} {
  const { paymentPayload, paymentRequirements } = req.body ?? {};
  if (!paymentPayload) throw new Error("Missing paymentPayload");
  if (!paymentRequirements) throw new Error("Missing paymentRequirements");
  return { paymentPayload, paymentRequirements };
}

function ensureSupported(payload: PaymentPayload, requirements: PaymentRequirements): void {
  if (!isConfiguredNetwork(payload.accepted.network, config)) {
    throw new Error(`Unsupported payment network: ${String(payload.accepted.network)}`);
  }
  if (requirements.network !== payload.accepted.network) {
    throw new Error("Payment payload and requirements networks do not match");
  }
}
