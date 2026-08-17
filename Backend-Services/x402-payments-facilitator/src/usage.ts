import { mkdir, readFile, rename, writeFile } from "node:fs/promises";
import { dirname } from "node:path";

type MonthUsage = { settled: number; transactionIds: string[]; updatedAt: string };
type UsageState = { months: Record<string, MonthUsage> };

export type PricingSnapshot = {
  month: string;
  settled: number;
  freeTierRemaining: number;
  billableSettlements: number;
  accruedFeeUsd: number;
  freeSettlements: number;
  feePerSettlementUsd: number;
};

export class UsageMeter {
  private state: UsageState = { months: {} };
  private writeQueue: Promise<void> = Promise.resolve();

  constructor(
    private readonly filename: string,
    private readonly freeSettlements: number,
    private readonly feePerSettlementUsd: number,
  ) {}

  async initialize(): Promise<void> {
    try {
      const parsed = JSON.parse(await readFile(this.filename, "utf8")) as UsageState;
      if (!parsed || typeof parsed !== "object" || typeof parsed.months !== "object") {
        throw new Error("invalid usage data shape");
      }
      this.state = parsed;
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw error;
      await mkdir(dirname(this.filename), { recursive: true });
      await this.persist();
    }
  }

  async record(transactionId: string): Promise<PricingSnapshot> {
    const month = currentMonth();
    const usage = (this.state.months[month] ??= {
      settled: 0,
      transactionIds: [],
      updatedAt: new Date().toISOString(),
    });
    if (usage.transactionIds.includes(transactionId)) return this.snapshot(month);
    usage.settled += 1;
    usage.transactionIds.push(transactionId);
    usage.updatedAt = new Date().toISOString();
    this.writeQueue = this.writeQueue.then(() => this.persist());
    await this.writeQueue;
    return this.snapshot(month);
  }

  snapshot(month = currentMonth()): PricingSnapshot {
    const settled = this.state.months[month]?.settled ?? 0;
    const billable = Math.max(0, settled - this.freeSettlements);
    return {
      month,
      settled,
      freeTierRemaining: Math.max(0, this.freeSettlements - settled),
      billableSettlements: billable,
      accruedFeeUsd: Number((billable * this.feePerSettlementUsd).toFixed(6)),
      freeSettlements: this.freeSettlements,
      feePerSettlementUsd: this.feePerSettlementUsd,
    };
  }

  private async persist(): Promise<void> {
    await mkdir(dirname(this.filename), { recursive: true });
    const temporary = `${this.filename}.${process.pid}.tmp`;
    await writeFile(temporary, `${JSON.stringify(this.state, null, 2)}\n`, { mode: 0o600 });
    await rename(temporary, this.filename);
  }
}

function currentMonth(date = new Date()): string {
  return date.toISOString().slice(0, 7);
}
