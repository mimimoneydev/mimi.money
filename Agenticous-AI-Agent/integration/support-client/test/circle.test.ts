import assert from "node:assert/strict";
import test from "node:test";
import { payWithCircleAgentWallet } from "../src/circle.js";
import type { ClientConfig } from "../src/config.js";

const config: ClientConfig = {
  host: "127.0.0.1",
  port: 4411,
  internalToken: "x".repeat(32),
  payerAddress: "0x0000000000000000000000000000000000000002",
  paymentMode: "circle-agent-wallet",
  circleCliPath: "/usr/local/bin/circle",
  circleChain: "BASE",
  agenticousUrl: "https://agenticous.mimi.money",
  sellerAddress: "0x0000000000000000000000000000000000000001",
  paymentNetwork: "eip155:8453",
  paymentAmountAtomic: "10000",
  requestTimeoutMs: 30_000,
  rpcUrls: { 8453: "https://mainnet.base.org" },
  autonomousTransfersEnabled: false,
  maximumTransferUsd: "1.00",
  allowedTransferRecipients: [],
  recipientPolicy: "allowlist",
  x402PurchasesEnabled: false,
  maximumX402PurchaseUsd: "0.05",
  allowedX402Hosts: [],
  x402HostPolicy: "allowlist",
  autonomousActionsEnabled: false,
  maximumActionUsd: "1.00",
  maximumDailyUsd: "5.00",
  allowedActionChains: ["BASE"],
  contractPolicy: "allowlist",
  allowedContracts: [],
  autonomyLedgerPath: "/var/lib/mimi-support-x402/test-ledger.json",
};

test("Circle Agent Wallet payment is capped and uses argument-safe execution", async () => {
  let captured: readonly string[] = [];
  const report = await payWithCircleAgentWallet(config, "0x0000000000000000000000000000000000000003", async (file, args) => {
    assert.equal(file, "/usr/local/bin/circle");
    captured = args;
    return { stdout: JSON.stringify({ requestId: "paid-report" }) };
  });
  assert.deepEqual(report, { requestId: "paid-report" });
  assert.equal(captured[captured.indexOf("--max-amount") + 1], "0.01");
  assert.equal(captured[captured.indexOf("--chain") + 1], "BASE");
  assert.equal(captured[captured.indexOf("--method") + 1], "POST");
});
