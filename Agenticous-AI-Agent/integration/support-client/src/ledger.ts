import { createHash } from "node:crypto";
import { mkdir, readFile, rename, writeFile } from "node:fs/promises";
import { dirname } from "node:path";

export type LedgerEntry = {
  idempotencyKey: string;
  fingerprint: string;
  kind: string;
  amountMicrousd: string;
  status: "reserved" | "completed" | "failed";
  createdAt: string;
  updatedAt: string;
  result?: unknown;
  error?: string;
};

export function microusd(value: string): bigint {
  if (!/^\d+(?:\.\d{1,6})?$/.test(value)) throw new Error("USD amount must be a non-negative decimal with at most 6 places");
  const [whole = "0", fraction = ""] = value.split(".");
  return BigInt(whole) * 1_000_000n + BigInt(fraction.padEnd(6, "0"));
}

export class AutonomyLedger {
  private entries: LedgerEntry[] = [];
  private queue: Promise<unknown> = Promise.resolve();

  public constructor(private readonly path: string, private readonly dailyLimitUsd: string) {}

  public async initialize(): Promise<void> {
    try {
      const parsed = JSON.parse(await readFile(this.path, "utf8"));
      if (Array.isArray(parsed)) this.entries = parsed.filter(item => item && typeof item === "object") as LedgerEntry[];
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw error;
    }
  }

  public execute<T>(idempotencyKey: string, kind: string, amountUsd: string, input: unknown, operation: () => Promise<T>): Promise<T> {
    const task = this.queue.then(() => this.executeLocked(idempotencyKey, kind, amountUsd, input, operation));
    this.queue = task.catch(() => undefined);
    return task;
  }

  public snapshot(): readonly LedgerEntry[] { return this.entries.slice(-500); }

  private async executeLocked<T>(idempotencyKey: string, kind: string, amountUsd: string, input: unknown, operation: () => Promise<T>): Promise<T> {
    if (!/^[a-zA-Z0-9._:-]{8,128}$/.test(idempotencyKey)) throw new Error("A valid 8-128 character idempotencyKey is required");
    const fingerprint = createHash("sha256").update(JSON.stringify({ kind, amountUsd, input })).digest("hex");
    const existing = this.entries.find(item => item.idempotencyKey === idempotencyKey);
    if (existing) {
      if (existing.fingerprint !== fingerprint) throw new Error("Idempotency key was already used for a different action");
      if (existing.status === "completed") return existing.result as T;
      throw new Error(`Action is already ${existing.status}`);
    }
    const amount = microusd(amountUsd);
    if (amount <= 0n) throw new Error("Action amount must be positive");
    const cutoff = Date.now() - 86_400_000;
    const spent = this.entries.filter(item => item.status !== "failed" && Date.parse(item.createdAt) >= cutoff)
      .reduce((sum, item) => sum + BigInt(item.amountMicrousd), 0n);
    if (spent + amount > microusd(this.dailyLimitUsd)) throw new Error("Autonomous daily budget exceeded");
    const now = new Date().toISOString();
    const entry: LedgerEntry = { idempotencyKey, fingerprint, kind, amountMicrousd: amount.toString(), status: "reserved", createdAt: now, updatedAt: now };
    this.entries.push(entry);
    await this.persist();
    try {
      const result = await operation();
      entry.status = "completed";
      entry.result = result;
      entry.updatedAt = new Date().toISOString();
      await this.persist();
      return result;
    } catch (error) {
      entry.status = "failed";
      entry.error = error instanceof Error ? error.message.slice(0, 500) : "Action failed";
      entry.updatedAt = new Date().toISOString();
      await this.persist();
      throw error;
    }
  }

  private async persist(): Promise<void> {
    await mkdir(dirname(this.path), { recursive: true });
    const temporary = `${this.path}.${process.pid}.tmp`;
    await writeFile(temporary, JSON.stringify(this.entries.slice(-5_000)), { mode: 0o600 });
    await rename(temporary, this.path);
  }
}
