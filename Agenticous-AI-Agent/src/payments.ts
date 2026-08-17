import type { AppConfig } from "./config.js";

// Circle CLI 1.0.0 raises Gateway authorization windows to 30 days before
// signing. The seller must advertise the same value so the payment payload's
// accepted requirements and the requirements sent to /settle remain identical.
export const CIRCLE_AGENT_WALLET_TIMEOUT_SECONDS = 30 * 24 * 60 * 60;

type PaymentConfig = Pick<AppConfig, "paymentNetwork" | "reportPriceUsd" | "sellerAddress">;

export function gatewayPaymentOption(config: PaymentConfig) {
  return {
    scheme: "exact" as const,
    price: `$${config.reportPriceUsd}`,
    network: config.paymentNetwork,
    payTo: config.sellerAddress,
    maxTimeoutSeconds: CIRCLE_AGENT_WALLET_TIMEOUT_SECONDS,
  };
}
