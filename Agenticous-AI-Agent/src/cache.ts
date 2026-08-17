import type { TransactionReport } from "./types.js";

type Entry = { expiresAt: number; report: TransactionReport };

export class ReportCache {
  private readonly entries = new Map<string, Entry>();

  public constructor(private readonly ttlSeconds: number, private readonly maximum = 500) {}

  public get(address: string): TransactionReport | undefined {
    const key = address.toLowerCase();
    const entry = this.entries.get(key);
    if (!entry) return undefined;
    if (entry.expiresAt <= Date.now()) {
      this.entries.delete(key);
      return undefined;
    }
    return entry.report;
  }

  public set(address: string, report: TransactionReport): void {
    if (this.ttlSeconds <= 0) return;
    if (this.entries.size >= this.maximum) {
      const oldest = this.entries.keys().next().value as string | undefined;
      if (oldest) this.entries.delete(oldest);
    }
    this.entries.set(address.toLowerCase(), {
      expiresAt: Date.now() + this.ttlSeconds * 1000,
      report,
    });
  }
}
