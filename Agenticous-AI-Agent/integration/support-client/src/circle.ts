import { execFile } from "node:child_process";
import { promisify } from "node:util";
import type { ClientConfig } from "./config.js";

const runFile = promisify(execFile);
type CommandRunner = (file: string, args: readonly string[], options: {
  timeout: number;
  maxBuffer: number;
  windowsHide: boolean;
}) => Promise<{ stdout: string | Buffer }>;

export async function payWithCircleAgentWallet(
  config: ClientConfig,
  address: string,
  runner: CommandRunner = runFile,
): Promise<unknown> {
  const endpoint = `${config.agenticousUrl}/v1/reports/transactions`;
  const { stdout } = await runner(config.circleCliPath, [
    "services", "pay", endpoint,
    "--address", config.payerAddress,
    "--chain", config.circleChain,
    "--max-amount", "0.01",
    "--method", "POST",
    "--data", JSON.stringify({ address }),
    "--quiet",
    "--timeout", String(Math.ceil(config.requestTimeoutMs / 1000)),
  ], {
    timeout: config.requestTimeoutMs + 5_000,
    maxBuffer: 1024 * 1024,
    windowsHide: true,
  });
  return JSON.parse(stdout.toString().trim());
}

export async function payAgentRunWithCircleWallet(
  config: ClientConfig,
  payload: { address: string; intent: string; authority?: { mode?: string; maximumExternalSpendUsd?: string } },
  runner: CommandRunner = runFile,
): Promise<unknown> {
  const endpoint = `${config.agenticousUrl}/v1/agent/runs`;
  const { stdout } = await runner(config.circleCliPath, [
    "services", "pay", endpoint,
    "--address", config.payerAddress,
    "--chain", config.circleChain,
    "--max-amount", "0.01",
    "--method", "POST",
    "--data", JSON.stringify(payload),
    "--quiet",
    "--timeout", String(Math.ceil(config.requestTimeoutMs / 1000)),
  ], { timeout: config.requestTimeoutMs + 5_000, maxBuffer: 1024 * 1024, windowsHide: true });
  return JSON.parse(stdout.toString().trim());
}
