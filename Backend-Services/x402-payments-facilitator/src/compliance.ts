import type { AppConfig } from "./config.js";

export type ScreeningResult = { allowed: boolean; reason?: string; provider?: string };

export class ComplianceScreener {
  constructor(private readonly config: AppConfig) {}

  get enabled(): boolean {
    return Boolean(this.config.complianceUrl);
  }

  async screen(paymentPayload: unknown, paymentRequirements: unknown): Promise<ScreeningResult> {
    if (!this.config.complianceUrl) {
      if (this.config.complianceFailClosed) {
        return { allowed: false, reason: "Compliance screening is not configured" };
      }
      return { allowed: true, provider: "disabled" };
    }

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.config.complianceTimeoutMs);
    try {
      const response = await fetch(this.config.complianceUrl, {
        method: "POST",
        headers: {
          "content-type": "application/json",
          ...(this.config.complianceToken
            ? { authorization: `Bearer ${this.config.complianceToken}` }
            : {}),
        },
        body: JSON.stringify({ paymentPayload, paymentRequirements }),
        signal: controller.signal,
      });
      if (!response.ok) throw new Error(`screening provider returned HTTP ${response.status}`);
      const result = (await response.json()) as ScreeningResult;
      if (typeof result.allowed !== "boolean") throw new Error("invalid screening provider response");
      return result;
    } catch (error) {
      if (this.config.complianceFailClosed) {
        return {
          allowed: false,
          reason: error instanceof Error ? error.message : "Compliance screening unavailable",
        };
      }
      return { allowed: true, reason: "Screening unavailable; fail-open policy applied" };
    } finally {
      clearTimeout(timer);
    }
  }
}
