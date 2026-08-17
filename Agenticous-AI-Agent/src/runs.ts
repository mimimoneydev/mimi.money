import { mkdir, readFile, rename, writeFile } from "node:fs/promises";
import { dirname } from "node:path";
import type { AgentRun } from "./types.js";

export class AgentRunStore {
  private readonly runs = new Map<string, AgentRun>();
  private loaded = false;
  private persistQueue: Promise<void> = Promise.resolve();

  public constructor(private readonly filePath?: string, private readonly maximum = 1_000) {}

  public async initialize(): Promise<void> {
    if (this.loaded) return;
    this.loaded = true;
    if (!this.filePath) return;
    try {
      const parsed = JSON.parse(await readFile(this.filePath, "utf8")) as unknown;
      if (!Array.isArray(parsed)) return;
      for (const item of parsed) {
        if (item && typeof item === "object" && typeof (item as AgentRun).id === "string") {
          this.runs.set((item as AgentRun).id, item as AgentRun);
        }
      }
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code !== "ENOENT") {
        console.warn(JSON.stringify({ level: "warn", message: "Agent run store could not be restored; starting empty" }));
      }
    }
  }

  public get(id: string): AgentRun | undefined {
    return this.runs.get(id);
  }

  public list(status?: AgentRun["status"]): AgentRun[] {
    return [...this.runs.values()].filter(run => !status || run.status === status);
  }

  public async set(run: AgentRun): Promise<void> {
    if (this.runs.size >= this.maximum && !this.runs.has(run.id)) {
      const oldest = this.runs.keys().next().value as string | undefined;
      if (oldest) this.runs.delete(oldest);
    }
    this.runs.set(run.id, run);
    this.persistQueue = this.persistQueue.catch(() => undefined).then(() => this.persist());
    await this.persistQueue;
  }

  private async persist(): Promise<void> {
    if (!this.filePath) return;
    await mkdir(dirname(this.filePath), { recursive: true });
    const temporary = `${this.filePath}.${process.pid}.tmp`;
    await writeFile(temporary, JSON.stringify([...this.runs.values()]), { mode: 0o600 });
    await rename(temporary, this.filePath);
  }
}
