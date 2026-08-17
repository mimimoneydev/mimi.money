import assert from "node:assert/strict";
import test from "node:test";
import { transferIsAllowed } from "../src/tools.js";
import type { ClientConfig } from "../src/config.js";

const recipient = "0x0000000000000000000000000000000000000003" as const;
const config: ClientConfig = {
  host: "127.0.0.1", port: 4411, internalToken: "x".repeat(32),
  payerAddress: "0x0000000000000000000000000000000000000002",
  paymentMode: "circle-agent-wallet", circleCliPath: "/usr/local/bin/circle", circleChain: "BASE",
  agenticousUrl: "https://agenticous.mimi.money",
  sellerAddress: "0x0000000000000000000000000000000000000001",
  paymentNetwork: "eip155:8453", paymentAmountAtomic: "10000", requestTimeoutMs: 30_000,
  rpcUrls: { 8453: "https://mainnet.base.org" }, autonomousTransfersEnabled: true,
  maximumTransferUsd: "1.00", allowedTransferRecipients: [recipient],
  recipientPolicy: "allowlist",
  x402PurchasesEnabled: false, maximumX402PurchaseUsd: "0.05", allowedX402Hosts: [],
  x402HostPolicy: "allowlist", autonomousActionsEnabled: false,
  maximumActionUsd: "1.00", maximumDailyUsd: "5.00", allowedActionChains: ["BASE"],
  contractPolicy: "allowlist", allowedContracts: [], autonomyLedgerPath: "/var/lib/mimi-support-x402/test-ledger.json",
};

test("Circle transfers require enablement, recipient allowlist, and bounded exact decimal amount", () => {
  assert.equal(transferIsAllowed(config, recipient, "1.00"), true);
  assert.equal(transferIsAllowed(config, recipient, "1.000001"), false);
  assert.equal(transferIsAllowed(config, "0x0000000000000000000000000000000000000004", "0.01"), false);
  assert.equal(transferIsAllowed({ ...config, autonomousTransfersEnabled: false }, recipient, "0.01"), false);
  assert.equal(transferIsAllowed({ ...config, paymentMode: "local-key" }, recipient, "0.01"), false);
  assert.equal(transferIsAllowed({ ...config, recipientPolicy: "any" }, "0x0000000000000000000000000000000000000004", "0.01"), true);
});
