import { randomUUID, timingSafeEqual } from "node:crypto";
import express, { type NextFunction, type Request, type Response } from "express";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import { isAddress } from "viem";
import { privateKeyToAccount } from "viem/accounts";
import { x402Client, x402HTTPClient } from "@x402/core/client";
import { ExactEvmScheme } from "@x402/evm/exact/client";
import { wrapFetchWithPayment } from "@x402/fetch";
import { loadConfig } from "./config.js";
import { paymentIsAllowed } from "./policy.js";

const config = loadConfig();
const signer = privateKeyToAccount(config.payerPrivateKey);
const paymentClient = new x402Client()
  .register(config.paymentNetwork, new ExactEvmScheme(signer))
  .registerPolicy((_version, requirements) => requirements.filter(requirement => paymentIsAllowed(requirement, {
    network: config.paymentNetwork,
    amountAtomic: config.paymentAmountAtomic,
    recipient: config.sellerAddress,
  })));
const paidFetch = wrapFetchWithPayment(fetch, paymentClient);
const httpClient = new x402HTTPClient(paymentClient);

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

app.use((req: Request, res: Response, next: NextFunction) => {
  if (!authorized(req.header("authorization"))) return res.status(401).json({ error: "Unauthorized" });
  next();
});

app.get("/healthz", (_req: Request, res: Response) => {
  res.json({ status: "ok", service: "mimi-support-agenticous-client", payerAddress: config.payerAddress });
});

app.post("/v1/reports", async (req: Request, res: Response, next: NextFunction) => {
  const address = typeof req.body?.address === "string" ? req.body.address.trim() : "";
  if (!isAddress(address)) return res.status(422).json({ error: "A valid EVM wallet address is required." });

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), config.requestTimeoutMs);
  try {
    const response = await paidFetch(`${config.agenticousUrl}/v1/reports/transactions`, {
      method: "POST",
      headers: { "content-type": "application/json", "x-request-id": randomUUID() },
      body: JSON.stringify({ address }),
      signal: controller.signal,
    });
    const body: { error?: unknown; [key: string]: unknown } = await response.json()
      .catch(() => ({ error: "Agenticous returned an unreadable response." }));
    if (!response.ok) {
      return res.status(response.status >= 500 ? 502 : response.status).json({
        error: typeof body.error === "string" ? body.error : "Agenticous payment or report failed.",
      });
    }

    const settlement = httpClient.getPaymentSettleResponse(name => response.headers.get(name));
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
      },
    });
  } catch (error) {
    return next(error);
  } finally {
    clearTimeout(timeout);
  }
});

app.use((error: unknown, _req: Request, res: Response, _next: NextFunction) => {
  console.error(JSON.stringify({
    level: "error",
    message: error instanceof Error ? error.message : "Unexpected client error",
  }));
  if (!res.headersSent) res.status(502).json({ error: "The paid Agenticous request failed safely." });
});

const server = app.listen(config.port, config.host, () => {
  console.info(JSON.stringify({
    level: "info",
    message: "MiMi Support x402 client started",
    listen: `${config.host}:${config.port}`,
    payerAddress: config.payerAddress,
  }));
});

function shutdown(): void {
  server.close(error => { if (error) process.exitCode = 1; });
  setTimeout(() => process.exit(1), 10_000).unref();
}

process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);
