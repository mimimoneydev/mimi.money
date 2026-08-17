import { execFile } from "node:child_process";
import { promisify } from "node:util";
import { lookup } from "node:dns/promises";
import { isIP } from "node:net";
import type { ClientConfig } from "./config.js";

const runFile = promisify(execFile);

function evmAddress(value: unknown, name: string): `0x${string}` {
  if (typeof value !== "string" || !/^0x[0-9a-fA-F]{40}$/.test(value)) throw new Error(`${name} must be a valid EVM address`);
  return value as `0x${string}`;
}

function transactionHash(value: unknown): `0x${string}` {
  if (typeof value !== "string" || !/^0x[0-9a-fA-F]{64}$/.test(value)) throw new Error("transactionHash must be a valid EVM transaction hash");
  return value as `0x${string}`;
}

function paddedAddress(value: string): string {
  return value.toLowerCase().slice(2).padStart(64, "0");
}

async function rpc(config: ClientConfig, chainId: number, method: string, params: unknown[]): Promise<unknown> {
  const url = config.rpcUrls[chainId];
  if (!url) throw new Error("Unsupported chainId");
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), Math.min(config.requestTimeoutMs, 15_000));
  try {
    const response = await fetch(url, {
      method: "POST",
      signal: controller.signal,
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ jsonrpc: "2.0", id: 1, method, params }),
    });
    if (!response.ok) throw new Error(`RPC returned HTTP ${response.status}`);
    const body = await response.json() as { result?: unknown; error?: { message?: string } };
    if (body.error) throw new Error(`RPC error: ${body.error.message ?? "unknown"}`);
    return body.result;
  } finally {
    clearTimeout(timeout);
  }
}

export async function chainQuery(config: ClientConfig, input: Record<string, unknown>): Promise<unknown> {
  const chainId = Number(input.chainId);
  if (!Number.isSafeInteger(chainId) || chainId <= 0) throw new Error("chainId is invalid");
  const operation = input.operation;
  if (operation === "native_balance") {
    const owner = evmAddress(input.address, "address");
    const result = await rpc(config, chainId, "eth_getBalance", [owner, "latest"]);
    return { source: "rpc", chainId, operation, address: owner, balanceWei: result, observedAt: new Date().toISOString() };
  }
  if (operation === "receipt") {
    const hash = transactionHash(input.transactionHash);
    const [receipt, latest] = await Promise.all([
      rpc(config, chainId, "eth_getTransactionReceipt", [hash]),
      rpc(config, chainId, "eth_blockNumber", []),
    ]);
    return { source: "rpc", chainId, operation, transactionHash: hash, receipt, latestBlock: latest, observedAt: new Date().toISOString() };
  }
  if (operation === "allowance") {
    const owner = evmAddress(input.address, "address");
    const token = evmAddress(input.token, "token");
    const spender = evmAddress(input.spender, "spender");
    const data = `0xdd62ed3e${paddedAddress(owner)}${paddedAddress(spender)}`;
    const result = await rpc(config, chainId, "eth_call", [{ to: token, data }, "latest"]);
    return { source: "rpc", chainId, operation, owner, token, spender, allowanceAtomic: result, observedAt: new Date().toISOString() };
  }
  if (operation === "erc20_balance") {
    const owner = evmAddress(input.address, "address");
    const token = evmAddress(input.token, "token");
    const data = `0x70a08231${paddedAddress(owner)}`;
    const result = await rpc(config, chainId, "eth_call", [{ to: token, data }, "latest"]);
    return { source: "rpc", chainId, operation, owner, token, balanceAtomic: result, observedAt: new Date().toISOString() };
  }
  if (operation === "contract_query") {
    const contract = evmAddress(input.contract, "contract");
    const data = input.data;
    if (typeof data !== "string" || !/^0x[0-9a-fA-F]{8,8192}$/.test(data) || data.length % 2 !== 0) throw new Error("data must be bounded ABI-encoded calldata");
    const result = await rpc(config, chainId, "eth_call", [{ to: contract, data }, "latest"]);
    return { source: "rpc", chainId, operation, contract, data, result, observedAt: new Date().toISOString() };
  }
  throw new Error("Unsupported chain query operation");
}

async function circle(config: ClientConfig, args: readonly string[]): Promise<unknown> {
  if (config.paymentMode !== "circle-agent-wallet") throw new Error("Circle Agent Wallet mode is not configured");
  const { stdout } = await runFile(config.circleCliPath, [...args, "--output", "json"], {
    timeout: config.requestTimeoutMs,
    maxBuffer: 1024 * 1024,
    windowsHide: true,
  });
  return JSON.parse(stdout.trim());
}

export async function circleWalletQuery(config: ClientConfig, operation: unknown): Promise<unknown> {
  if (operation === "status") return circle(config, ["wallet", "status", "--type", "agent"]);
  if (operation === "balance") return circle(config, ["wallet", "balance", "--address", config.payerAddress, "--chain", config.circleChain]);
  if (operation === "transactions") return circle(config, ["transaction", "list", "--address", config.payerAddress, "--chain", config.circleChain, "--limit", "20"]);
  if (operation === "budget") return circle(config, ["wallet", "limit", "budget", "--address", config.payerAddress]);
  throw new Error("Unsupported Circle wallet operation");
}

export function transferIsAllowed(config: ClientConfig, recipient: string, amountUsd: string): boolean {
  if (!config.autonomousTransfersEnabled || config.paymentMode !== "circle-agent-wallet") return false;
  if (!/^0x[0-9a-fA-F]{40}$/.test(recipient) || !/^\d+(?:\.\d{1,6})?$/.test(amountUsd)) return false;
  const atomic = (value: string): bigint => {
    const [whole = "0", fraction = ""] = value.split(".");
    return BigInt(whole) * 1_000_000n + BigInt(fraction.padEnd(6, "0"));
  };
  return atomic(amountUsd) <= atomic(config.maximumTransferUsd)
    && (config.recipientPolicy === "any" || config.allowedTransferRecipients.some(item => item.toLowerCase() === recipient.toLowerCase()));
}

export async function circleTransfer(config: ClientConfig, recipientValue: unknown, amountValue: unknown): Promise<unknown> {
  const recipient = evmAddress(recipientValue, "recipient");
  const amountUsd = typeof amountValue === "string" ? amountValue : "";
  if (!transferIsAllowed(config, recipient, amountUsd)) throw new Error("Transfer denied by the independent wallet policy");
  const base = ["wallet", "transfer", recipient, "--amount", amountUsd, "--address", config.payerAddress, "--chain", config.circleChain] as const;
  const simulation = await circle(config, [...base, "--estimate"]);
  const execution = await circle(config, base);
  return { simulation, execution };
}

export async function circleServiceSearch(config: ClientConfig, queryValue: unknown): Promise<unknown> {
  const query = typeof queryValue === "string" ? queryValue.trim() : "";
  if (query.length > 100) throw new Error("Service query is too long");
  return circle(config, ["services", "search", ...(query ? [query] : []), "--limit", "20"]);
}

export async function circleServicePurchase(config: ClientConfig, input: Record<string, unknown>): Promise<unknown> {
  if (!config.x402PurchasesEnabled) throw new Error("x402 purchases are disabled");
  const target = new URL(typeof input.url === "string" ? input.url : "");
  const allowedHost = config.x402HostPolicy === "public-internet"
    ? await publicHostname(target.hostname)
    : config.allowedX402Hosts.includes(target.hostname.toLowerCase());
  if (target.protocol !== "https:" || target.username || target.password || target.port || !allowedHost) {
    throw new Error("x402 service host is not allowlisted");
  }
  const maximumUsd = typeof input.maximumUsd === "string" ? input.maximumUsd : "";
  const decimal = (value: string): bigint => {
    if (!/^\d+(?:\.\d{1,6})?$/.test(value)) throw new Error("maximumUsd is invalid");
    const [whole = "0", fraction = ""] = value.split(".");
    return BigInt(whole) * 1_000_000n + BigInt(fraction.padEnd(6, "0"));
  };
  if (decimal(maximumUsd) > decimal(config.maximumX402PurchaseUsd)) throw new Error("x402 purchase exceeds policy");
  const method = typeof input.method === "string" ? input.method.toUpperCase() : "GET";
  if (!(method === "GET" || method === "POST")) throw new Error("Only GET and POST x402 purchases are allowed");
  const data = input.data;
  if (data !== undefined && (typeof data !== "string" || data.length > 8_192 || (() => { try { JSON.parse(data); return false; } catch { return true; } })())) throw new Error("x402 POST data must be bounded JSON");
  const args = ["services", "pay", target.toString(), "--address", config.payerAddress, "--chain", config.circleChain, "--max-amount", maximumUsd, "--method", method, "--timeout", String(Math.ceil(config.requestTimeoutMs / 1000))];
  if (data !== undefined) args.push("--data", data);
  const simulation = await circle(config, [...args, "--estimate"]);
  const result = await circle(config, args);
  return { simulation, result };
}

function privateIp(value: string): boolean {
  const ip = value.toLowerCase();
  if (ip.includes(":")) return ip === "::1" || ip.startsWith("fc") || ip.startsWith("fd") || ip.startsWith("fe8") || ip.startsWith("fe9") || ip.startsWith("fea") || ip.startsWith("feb") || ip.startsWith("::ffff:127.") || ip.startsWith("::ffff:10.") || ip.startsWith("::ffff:192.168.");
  const octets = ip.split(".").map(Number);
  return octets[0] === 10 || octets[0] === 127 || octets[0] === 0 || (octets[0] === 169 && octets[1] === 254)
    || (octets[0] === 172 && octets[1]! >= 16 && octets[1]! <= 31) || (octets[0] === 192 && octets[1] === 168)
    || (octets[0] === 100 && octets[1]! >= 64 && octets[1]! <= 127) || octets[0]! >= 224;
}

async function publicHostname(hostname: string): Promise<boolean> {
  const normalized = hostname.toLowerCase();
  if (normalized === "localhost" || normalized.endsWith(".localhost") || normalized.endsWith(".local") || normalized.endsWith(".internal")) return false;
  if (isIP(normalized)) return !privateIp(normalized);
  if (!/^(?:[a-z0-9-]+\.)+[a-z]{2,}$/.test(normalized)) return false;
  const addresses = await lookup(normalized, { all: true, verbatim: true });
  return addresses.length > 0 && addresses.every(item => !privateIp(item.address));
}

function boundedText(value: unknown, name: string, maximum = 256): string {
  if (typeof value !== "string" || value.length < 1 || value.length > maximum || /[\u0000-\u001f]/.test(value)) throw new Error(`${name} is invalid`);
  return value;
}

function actionAllowed(config: ClientConfig, input: Record<string, unknown>): { operation: string; chain: string; amountUsd: string } {
  if (!config.autonomousActionsEnabled || config.paymentMode !== "circle-agent-wallet") throw new Error("Autonomous Circle actions are not configured");
  const operation = boundedText(input.operation, "operation", 32);
  const chain = boundedText(input.chain ?? config.circleChain, "chain", 32).toUpperCase();
  if (!config.allowedActionChains.includes(chain)) throw new Error("Chain denied by autonomous policy");
  const amountUsd = boundedText(input.maximumUsd ?? input.amountUsd ?? input.amount ?? "0", "maximumUsd", 32);
  const atomic = (value: string) => {
    if (!/^\d+(?:\.\d{1,6})?$/.test(value)) throw new Error("Amount is invalid");
    const [whole = "0", fraction = ""] = value.split(".");
    return BigInt(whole) * 1_000_000n + BigInt(fraction.padEnd(6, "0"));
  };
  if (atomic(amountUsd) <= 0n || atomic(amountUsd) > atomic(config.maximumActionUsd)) throw new Error("Action exceeds the per-action policy");
  return { operation, chain, amountUsd };
}

function ensureWithinDeclaredMaximum(actual: string, maximum: string): void {
  const atomic = (value: string) => {
    if (!/^\d+(?:\.\d{1,6})?$/.test(value)) throw new Error("Amount is invalid");
    const [whole = "0", fraction = ""] = value.split(".");
    return BigInt(whole) * 1_000_000n + BigInt(fraction.padEnd(6, "0"));
  };
  if (atomic(actual) <= 0n || atomic(actual) > atomic(maximum)) throw new Error("Action amount exceeds its declared maximum");
}

export async function circleAutonomousAction(config: ClientConfig, input: Record<string, unknown>): Promise<unknown> {
  const { operation, chain, amountUsd: maximumUsd } = actionAllowed(config, input);
  const key = boundedText(input.idempotencyKey, "idempotencyKey", 128);
  if (!/^[a-zA-Z0-9._:-]{8,128}$/.test(key)) throw new Error("idempotencyKey is invalid");
  if (operation === "transfer") {
    ensureWithinDeclaredMaximum(String(input.amountUsd ?? ""), maximumUsd);
    return circleTransfer(config, input.recipient, input.amountUsd);
  }
  if (operation === "swap") {
    const sellToken = boundedText(input.sellToken, "sellToken", 64);
    const buyToken = boundedText(input.buyToken, "buyToken", 64);
    const sellAmount = boundedText(input.sellAmount, "sellAmount", 32);
    ensureWithinDeclaredMaximum(sellAmount, maximumUsd);
    const minimumBuyAmount = boundedText(input.minimumBuyAmount, "minimumBuyAmount", 32);
    const slippageBps = Number(input.slippageBps ?? 100);
    if (!Number.isInteger(slippageBps) || slippageBps < 1 || slippageBps > 500) throw new Error("slippageBps is invalid");
    const base = ["wallet", "swap", sellToken, sellAmount, buyToken, minimumBuyAmount, "--address", config.payerAddress, "--chain", chain, "--slippage-bps", String(slippageBps), "--idempotency-key", key] as const;
    const quote = await circle(config, ["wallet", "swap", sellToken, sellAmount, buyToken, "--chain", chain, "--quote", "--slippage-bps", String(slippageBps)]);
    const execution = await circle(config, base);
    return { quote, execution };
  }
  if (operation === "bridge") {
    const destinationChain = boundedText(input.destinationChain, "destinationChain", 32).toUpperCase();
    if (!config.allowedActionChains.includes(destinationChain)) throw new Error("Destination chain denied by autonomous policy");
    const amount = boundedText(input.amountUsd, "amountUsd", 32);
    ensureWithinDeclaredMaximum(amount, maximumUsd);
    const recipient = input.recipient === undefined ? undefined : evmAddress(input.recipient, "recipient");
    const args = ["bridge", "transfer", destinationChain, ...(recipient ? [recipient] : []), "--amount", amount, "--address", config.payerAddress, "--chain", chain, "--idempotency-key", key];
    const fee = await circle(config, ["bridge", "get-fee", destinationChain, "--chain", chain]);
    const execution = await circle(config, args);
    return { fee, execution };
  }
  if (operation === "contract_execute") {
    const contract = evmAddress(input.contract, "contract");
    if (config.contractPolicy !== "any" && !config.allowedContracts.some(item => item.toLowerCase() === contract.toLowerCase())) throw new Error("Contract denied by autonomous policy");
    const signature = boundedText(input.functionSignature, "functionSignature", 256);
    if (!/^[A-Za-z_$][A-Za-z0-9_$]*\([^)]{0,200}\)$/.test(signature)) throw new Error("functionSignature is invalid");
    const parameters = Array.isArray(input.parameters) ? input.parameters.map((item, index) => boundedText(item, `parameters[${index}]`, 1024)) : [];
    if (parameters.length > 32) throw new Error("Too many contract parameters");
    const nativeAmount = typeof input.nativeAmount === "string" ? input.nativeAmount : "0";
    if (nativeAmount !== "0") throw new Error("Autonomous contract calls cannot attach native value");
    const base = ["wallet", "execute", signature, ...parameters, "--contract", contract, "--address", config.payerAddress, "--chain", chain, "--amount", nativeAmount] as const;
    const simulation = await circle(config, [...base, "--estimate"]);
    const execution = await circle(config, base);
    return { simulation, execution };
  }
  throw new Error("Unsupported autonomous action");
}
