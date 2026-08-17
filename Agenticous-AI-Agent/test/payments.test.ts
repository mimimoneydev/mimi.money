import assert from "node:assert/strict";
import test from "node:test";
import { GatewayEvmScheme } from "@circle-fin/x402-batching/server";
import {
  CIRCLE_AGENT_WALLET_TIMEOUT_SECONDS,
  gatewayPaymentOption,
} from "../src/payments.js";

const config = {
  paymentNetwork: "eip155:8453" as const,
  reportPriceUsd: "0.01",
  sellerAddress: "0xAb959fbF16FB3C1ddfE140c0Eac604B2EfEAE312" as const,
};

test("Gateway requirements match Circle CLI 1.0.0's 30-day signing window", () => {
  const option = gatewayPaymentOption(config);
  assert.equal(CIRCLE_AGENT_WALLET_TIMEOUT_SECONDS, 2_592_000);
  assert.equal(option.maxTimeoutSeconds, 2_592_000);
  assert.deepEqual(option, {
    scheme: "exact",
    price: "$0.01",
    network: "eip155:8453",
    payTo: config.sellerAddress,
    maxTimeoutSeconds: 2_592_000,
  });
});

test("Gateway scheme preserves the Circle CLI-compatible timeout", async () => {
  const option = gatewayPaymentOption(config);
  const requirement = await new GatewayEvmScheme().enhancePaymentRequirements({
    scheme: option.scheme,
    network: option.network,
    amount: "10000",
    asset: "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913",
    payTo: option.payTo,
    maxTimeoutSeconds: option.maxTimeoutSeconds,
    extra: {},
  }, {
    x402Version: 2,
    scheme: "exact",
    network: "eip155:8453",
    extra: {
      name: "GatewayWalletBatched",
      version: "1",
      verifyingContract: "0x77777777dcc4d5a8b6e418fd04d8997ef11000ee",
    },
  }, []);

  assert.equal(requirement.maxTimeoutSeconds, 2_592_000);
  assert.equal(requirement.extra?.name, "GatewayWalletBatched");
});
