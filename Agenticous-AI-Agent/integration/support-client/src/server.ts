import { randomUUID, timingSafeEqual, webcrypto } from "node:crypto";
import express, { type NextFunction, type Request, type Response } from "express";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import { isAddress } from "viem";
import { privateKeyToAccount } from "viem/accounts";
import { x402Client, x402HTTPClient } from "@x402/core/client";
import { ExactEvmScheme } from "@x402/evm/exact/client";
import { wrapFetchWithPayment } from "@x402/fetch";
import { registerBatchScheme } from "@circle-fin/x402-batching/client";
import { loadConfig } from "./config.js";
import { paymentIsAllowed } from "./policy.js";
import { payAgentRunWithCircleWallet, payWithCircleAgentWallet } from "./circle.js";
import { chainQuery, circleAutonomousAction, circleServicePurchase, circleServiceSearch, circleTransfer, circleWalletQuery } from "./tools.js";
import { AutonomyLedger } from "./ledger.js";

if (!globalThis.crypto) {
  Object.defineProperty(globalThis, "crypto", { value: webcrypto, configurable: false });
}

const config = loadConfig();
const autonomyLedger = new AutonomyLedger(config.autonomyLedgerPath, config.maximumDailyUsd);
await autonomyLedger.initialize();
const signer = config.payerPrivateKey ? privateKeyToAccount(config.payerPrivateKey) : undefined;
const paymentClient = signer ? new x402Client() : undefined;
if (paymentClient && signer) {
  registerBatchScheme(paymentClient, { signer, fallbackScheme: new ExactEvmScheme(signer) });
  paymentClient.registerPolicy((_version, requirements) => requirements.filter(requirement => paymentIsAllowed(
      requirement,
      { network: config.paymentNetwork, amountAtomic: config.paymentAmountAtomic, recipient: config.sellerAddress },
    )));
}
const paidFetch = paymentClient ? wrapFetchWithPayment(fetch, paymentClient) : undefined;
const httpClient = paymentClient ? new x402HTTPClient(paymentClient) : undefined;
const app = express();

app.disable("x-powered-by");
app.use(helmet());
app.use(express.json({ limit: "8kb" }));
app.use(rateLimit({ windowMs: 60_000, limit: 30, legacyHeaders: false, standardHeaders: "draft-7" }));

function authorized(value: string | undefined): boolean {
  if (!value?.startsWith("Bearer ")) return false;
  const supplied = Buffer.from(value.slice(7));
  const expected = Buffer.from(config.internalToken);
  return supplied.length === expected.length && timingSafeEqual(supplied, expected);
}

app.use((req, res, next) => {
  if (!authorized(req.header("authorization"))) return res.status(401).json({ error: "Unauthorized" });
  next();
});

app.get("/healthz", (_req, res) => {
  res.json({ status: "ok", service: "mimi-support-agenticous-client", payerAddress: config.payerAddress, walletProvider: config.paymentMode });
});

app.post("/v1/reports", async (req, res, next) => {
  const address = typeof req.body?.address === "string" ? req.body.address.trim() : "";
  if (!isAddress(address)) return res.status(422).json({ error: "A valid EVM wallet address is required." });

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), config.requestTimeoutMs);
  try {
    if (config.paymentMode === "circle-agent-wallet") {
      const report = await payWithCircleAgentWallet(config, address);
      return res.json({
        report,
        payment: {
          success: true,
          network: config.paymentNetwork,
          payer: config.payerAddress,
          recipient: config.sellerAddress,
          amountAtomic: config.paymentAmountAtomic,
          amountUsd: "0.01",
          provider: "Circle Agent Wallet + Gateway Nanopayments",
          explorerUrl: `https://basescan.org/address/${config.sellerAddress}`,
        },
      });
    }
    const response = await paidFetch!(`${config.agenticousUrl}/v1/reports/transactions`, {
      method: "POST",
      headers: { "content-type": "application/json", "x-request-id": randomUUID() },
      body: JSON.stringify({ address }),
      signal: controller.signal,
    });
    const body = await response.json().catch(() => ({ error: "The Agenticous AI agent returned an unreadable response." }));
    if (!response.ok) {
      return res.status(response.status >= 500 ? 502 : response.status).json({
        error: typeof body?.error === "string" ? body.error : "The Agenticous AI agent payment or report failed.",
      });
    }
    const settlement = httpClient!.getPaymentSettleResponse(name => response.headers.get(name));
    return res.json({
      report: body,
      payment: {
        success: settlement.success,
        transaction: settlement.transaction,
        network: settlement.network,
        payer: settlement.payer ?? config.payerAddress,
        recipient: config.sellerAddress,
        amountAtomic: settlement.amount ?? config.paymentAmountAtomic,
        amountUsd: "0.01",
        provider: "Circle Gateway Nanopayments",
        explorerUrl: /^0x[0-9a-fA-F]{64}$/.test(settlement.transaction ?? "")
          ? `https://basescan.org/tx/${settlement.transaction}`
          : `https://basescan.org/address/${config.sellerAddress}`,
      },
    });
  } catch (error) {
    return next(error);
  } finally {
    clearTimeout(timeout);
  }
});

app.post("/v1/agent-runs", async (req, res, next) => {
  const address = typeof req.body?.address === "string" ? req.body.address.trim() : "";
  const intent = typeof req.body?.intent === "string" ? req.body.intent.trim() : "";
  if (!isAddress(address)) return res.status(422).json({ error: "A valid EVM wallet address is required." });
  if (!intent || intent.length > 2_000) return res.status(422).json({ error: "Intent must contain 1 to 2000 characters." });
  const payload = { address, intent, authority: req.body?.authority };
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), config.requestTimeoutMs);
  try {
    if (config.paymentMode === "circle-agent-wallet") {
      const run = await payAgentRunWithCircleWallet(config, payload);
      return res.json({ run, payment: { success: true, network: config.paymentNetwork, payer: config.payerAddress, recipient: config.sellerAddress, amountAtomic: config.paymentAmountAtomic, amountUsd: "0.01", provider: "Circle Agent Wallet + Gateway Nanopayments" } });
    }
    const response = await paidFetch!(`${config.agenticousUrl}/v1/agent/runs`, {
      method: "POST", headers: { "content-type": "application/json", "x-request-id": randomUUID() },
      body: JSON.stringify(payload), signal: controller.signal,
    });
    const body = await response.json().catch(() => ({ error: "The Agenticous AI agent returned an unreadable response." }));
    if (!response.ok) return res.status(response.status >= 500 ? 502 : response.status).json({ error: body?.error ?? "Agent run failed." });
    const settlement = httpClient!.getPaymentSettleResponse(name => response.headers.get(name));
    return res.json({ run: body, payment: { success: settlement.success, transaction: settlement.transaction, network: settlement.network, payer: settlement.payer ?? config.payerAddress, recipient: config.sellerAddress, amountAtomic: settlement.amount ?? config.paymentAmountAtomic, amountUsd: "0.01", provider: "Circle Gateway Nanopayments" } });
  } catch (error) {
    return next(error);
  } finally {
    clearTimeout(timeout);
  }
});

app.post("/v1/tools/chain-query", async (req, res, next) => {
  try {
    return res.json(await chainQuery(config, req.body && typeof req.body === "object" ? req.body : {}));
  } catch (error) {
    return next(error);
  }
});

app.post("/v1/tools/circle-wallet", async (req, res, next) => {
  try {
    return res.json(await circleWalletQuery(config, req.body?.operation));
  } catch (error) {
    return next(error);
  }
});

app.post("/v1/tools/circle-transfer", async (req, res, next) => {
  try {
    if (typeof req.body?.reason !== "string" || req.body.reason.trim().length < 1 || req.body.reason.length > 240) {
      return res.status(422).json({ error: "A bounded transfer reason is required." });
    }
    const key = typeof req.body?.idempotencyKey === "string" ? req.body.idempotencyKey : "";
    return res.json(await autonomyLedger.execute(key, "transfer", String(req.body?.amountUsd ?? ""), req.body, () => circleTransfer(config, req.body?.recipient, req.body?.amountUsd)));
  } catch (error) {
    return next(error);
  }
});

app.post("/v1/tools/circle-services", async (req, res, next) => {
  try {
    if (req.body?.operation === "search") return res.json(await circleServiceSearch(config, req.body?.query));
    if (req.body?.operation === "purchase") {
      const key = typeof req.body?.idempotencyKey === "string" ? req.body.idempotencyKey : "";
      return res.json(await autonomyLedger.execute(key, "x402_purchase", String(req.body?.maximumUsd ?? ""), req.body, () => circleServicePurchase(config, req.body)));
    }
    return res.status(422).json({ error: "Unsupported Circle services operation." });
  } catch (error) {
    return next(error);
  }
});

app.post("/v1/tools/circle-action", async (req, res, next) => {
  try {
    const body = req.body && typeof req.body === "object" ? req.body as Record<string, unknown> : {};
    const key = typeof body.idempotencyKey === "string" ? body.idempotencyKey : "";
    const amount = String(body.maximumUsd ?? body.amountUsd ?? body.amount ?? "");
    const kind = typeof body.operation === "string" ? body.operation : "unknown";
    return res.json(await autonomyLedger.execute(key, kind, amount, body, () => circleAutonomousAction(config, body)));
  } catch (error) {
    return next(error);
  }
});

app.get("/v1/autonomy/status", (_req, res) => {
  const entries = autonomyLedger.snapshot();
  res.json({
    enabled: config.autonomousActionsEnabled,
    walletProvider: config.paymentMode,
    payerAddress: config.payerAddress,
    policies: {
      maximumActionUsd: config.maximumActionUsd,
      maximumDailyUsd: config.maximumDailyUsd,
      recipientPolicy: config.recipientPolicy,
      x402HostPolicy: config.x402HostPolicy,
      contractPolicy: config.contractPolicy,
      chains: config.allowedActionChains,
    },
    actions: entries.map(({ result: _result, ...entry }) => entry),
  });
});

app.use((error: unknown, _req: Request, res: Response, _next: NextFunction) => {
  const message = error instanceof Error ? error.message : "Unexpected client error";
  const policyDenied = /(denied by|disabled|not configured|exceeds (?:the )?.*policy|budget exceeded|not allowlisted|chain denied|contract denied)/i.test(message);
  const invalidRequest = /\b(invalid|required|unsupported|must be|too (?:long|many))\b/i.test(message);
  const status = policyDenied ? 403 : invalidRequest ? 422 : 502;
  console.error(JSON.stringify({ level: status >= 500 ? "error" : "warn", message, status }));
  if (!res.headersSent) res.status(status).json({
    error: policyDenied ? "Autonomous action denied by machine policy." : invalidRequest ? "Invalid autonomous action request." : "The paid Agenticous AI agent request failed safely.",
    code: policyDenied ? "POLICY_DENIED" : invalidRequest ? "INVALID_ACTION" : "UPSTREAM_FAILURE",
  });
});

const server = app.listen(config.port, config.host, () => {
  console.info(JSON.stringify({ level: "info", message: "MiMi Support x402 client started", listen: `${config.host}:${config.port}`, payerAddress: config.payerAddress }));
});

function shutdown(): void {
  server.close(error => { if (error) process.exitCode = 1; });
  setTimeout(() => process.exit(1), 10_000).unref();
}
process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);
