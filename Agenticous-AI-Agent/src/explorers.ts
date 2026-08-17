import { randomUUID } from "node:crypto";
import { getAddress } from "viem";
import type {
  ExplorerConfig,
  ExplorerStatus,
  Transaction,
  TransactionReport,
} from "./types.js";

export const EXPLORERS: readonly ExplorerConfig[] = [
  { network: "eip155:1", chainId: 1, name: "Ethereum", apiBase: "https://eth.blockscout.com", explorerBase: "https://eth.blockscout.com" },
  { network: "eip155:137", chainId: 137, name: "Polygon", apiBase: "https://polygon.blockscout.com", explorerBase: "https://polygon.blockscout.com" },
  { network: "eip155:4326", chainId: 4326, name: "MegaETH", apiBase: "https://megaeth.blockscout.com", explorerBase: "https://megaeth.blockscout.com" },
  { network: "eip155:8453", chainId: 8453, name: "Base", apiBase: "https://base.blockscout.com", explorerBase: "https://base.blockscout.com" },
  { network: "eip155:84532", chainId: 84532, name: "Base Sepolia", apiBase: "https://base-sepolia.blockscout.com", explorerBase: "https://base-sepolia.blockscout.com", testnet: true },
  { network: "eip155:31612", chainId: 31612, name: "Mezo", apiBase: "https://api.explorer.mezo.org", explorerBase: "https://explorer.mezo.org" },
  { network: "eip155:42161", chainId: 42161, name: "Arbitrum One", apiBase: "https://arbitrum.blockscout.com", explorerBase: "https://arbitrum.blockscout.com" },
  { network: "eip155:421614", chainId: 421614, name: "Arbitrum Sepolia", apiBase: "https://arbitrum-sepolia.blockscout.com", explorerBase: "https://arbitrum-sepolia.blockscout.com", testnet: true },
  { network: "eip155:42220", chainId: 42220, name: "Celo", apiBase: "https://celo.blockscout.com", explorerBase: "https://celo.blockscout.com" },
  { network: "eip155:43114", chainId: 43114, name: "Avalanche C-Chain", apiBase: "https://api.routescan.io/v2/network/mainnet/evm/43114/etherscan/api", apiKind: "routescan", explorerBase: "https://subnets.avax.network/c-chain" },
] as const;

type JsonRecord = Record<string, unknown>;

type ExplorerResult = {
  transactions: Transaction[];
  status: ExplorerStatus;
};

function record(value: unknown): JsonRecord | undefined {
  return value !== null && typeof value === "object" && !Array.isArray(value)
    ? value as JsonRecord
    : undefined;
}

function text(value: unknown): string | undefined {
  return typeof value === "string" && value !== "" ? value : undefined;
}

function number(value: unknown): number | undefined {
  const parsed = typeof value === "number" ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function nestedAddress(value: unknown): string | undefined {
  const item = record(value);
  return text(item?.hash);
}

function direction(address: string, from?: string, to?: string): Transaction["direction"] {
  const target = address.toLowerCase();
  const fromMatches = from?.toLowerCase() === target;
  const toMatches = to?.toLowerCase() === target;
  if (fromMatches && toMatches) return "self";
  if (fromMatches) return "out";
  if (toMatches) return "in";
  return "contract";
}

function decimalAmount(value: string | undefined, decimals: number | undefined): string | undefined {
  if (!value || !/^\d+$/.test(value) || decimals === undefined || decimals < 0 || decimals > 36) return undefined;
  const padded = value.padStart(decimals + 1, "0");
  const whole = padded.slice(0, -decimals || undefined);
  const fraction = decimals ? padded.slice(-decimals).replace(/0+$/, "") : "";
  return fraction ? `${whole}.${fraction}` : whole;
}

function normalTransaction(config: ExplorerConfig, address: string, input: unknown): Transaction | undefined {
  const item = record(input);
  if (!item) return undefined;
  const hash = text(item.hash);
  const timestamp = text(item.timestamp);
  if (!hash || !timestamp) return undefined;
  const from = nestedAddress(item.from);
  const to = nestedAddress(item.to);
  const statusText = text(item.status)?.toLowerCase();
  return {
    network: config.network,
    chainId: config.chainId,
    chain: config.name,
    hash,
    timestamp,
    blockNumber: number(item.block_number),
    direction: direction(address, from, to),
    from,
    to,
    status: statusText === "ok" ? "success" : statusText === "error" ? "failed" : "unknown",
    asset: "native",
    amount: decimalAmount(text(item.value), 18),
    method: text(item.method),
    explorerUrl: `${config.explorerBase}/tx/${hash}`,
  };
}

function tokenTransaction(config: ExplorerConfig, address: string, input: unknown): Transaction | undefined {
  const item = record(input);
  if (!item) return undefined;
  const hash = text(item.transaction_hash) ?? text(item.tx_hash);
  const timestamp = text(item.timestamp);
  if (!hash || !timestamp) return undefined;
  const from = nestedAddress(item.from);
  const to = nestedAddress(item.to);
  const token = record(item.token);
  const total = record(item.total);
  const decimals = number(total?.decimals) ?? number(token?.decimals);
  return {
    network: config.network,
    chainId: config.chainId,
    chain: config.name,
    hash,
    timestamp,
    blockNumber: number(item.block_number),
    direction: direction(address, from, to),
    from,
    to,
    status: "success",
    asset: text(token?.symbol) ?? text(token?.name) ?? "token",
    amount: decimalAmount(text(total?.value), decimals),
    method: text(item.method) ?? "token transfer",
    explorerUrl: `${config.explorerBase}/tx/${hash}`,
  };
}

async function fetchItems(url: string, timeoutMs: number): Promise<unknown[]> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, {
      signal: controller.signal,
      headers: { accept: "application/json", "user-agent": "Agenticous/1.0" },
    });
    if (response.status === 404) return [];
    if (!response.ok) throw new Error(`Explorer returned HTTP ${response.status}`);
    const body = record(await response.json());
    return Array.isArray(body?.items) ? body.items : [];
  } finally {
    clearTimeout(timeout);
  }
}

function routescanNormalTransaction(config: ExplorerConfig, address: string, input: unknown): Transaction | undefined {
  const item = record(input);
  if (!item) return undefined;
  const hash = text(item.hash);
  const seconds = number(item.timeStamp);
  if (!hash || seconds === undefined) return undefined;
  const from = text(item.from);
  const to = text(item.to);
  return {
    network: config.network,
    chainId: config.chainId,
    chain: config.name,
    hash,
    timestamp: new Date(seconds * 1000).toISOString(),
    blockNumber: number(item.blockNumber),
    direction: direction(address, from, to),
    from,
    to,
    status: text(item.isError) === "1" ? "failed" : "success",
    asset: "native",
    amount: decimalAmount(text(item.value), 18),
    method: text(item.functionName)?.split("(")[0] || undefined,
    explorerUrl: `${config.explorerBase}/tx/${hash}`,
  };
}

function routescanTokenTransaction(config: ExplorerConfig, address: string, input: unknown): Transaction | undefined {
  const item = record(input);
  if (!item) return undefined;
  const transaction = routescanNormalTransaction(config, address, item);
  if (!transaction) return undefined;
  return {
    ...transaction,
    status: "success",
    asset: text(item.tokenSymbol) ?? text(item.tokenName) ?? "token",
    amount: decimalAmount(text(item.value), number(item.tokenDecimal)),
    method: text(item.functionName)?.split("(")[0] || "token transfer",
  };
}

async function fetchRoutescanItems(url: string, timeoutMs: number): Promise<unknown[]> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, {
      signal: controller.signal,
      headers: { accept: "application/json", "user-agent": "Agenticous/1.0" },
    });
    if (!response.ok) throw new Error(`Explorer returned HTTP ${response.status}`);
    const body = record(await response.json());
    return Array.isArray(body?.result) ? body.result : [];
  } finally {
    clearTimeout(timeout);
  }
}

async function searchExplorer(
  config: ExplorerConfig,
  address: string,
  sinceMs: number,
  timeoutMs: number,
): Promise<ExplorerResult> {
  if (!config.apiBase) {
    return {
      transactions: [],
      status: {
        network: config.network,
        chainId: config.chainId,
        chain: config.name,
        explorer: config.explorerBase,
        status: "unavailable",
        transactionsFound: 0,
        note: "No documented unauthenticated address-history API is configured.",
      },
    };
  }

  const encoded = encodeURIComponent(address);
  const routescan = config.apiKind === "routescan";
  const base = routescan
    ? `${config.apiBase}?module=account&address=${encoded}&page=1&offset=100&sort=desc&action=`
    : `${config.apiBase}/api/v2/addresses/${encoded}/`;
  const results = await Promise.allSettled([
    routescan ? fetchRoutescanItems(`${base}txlist`, timeoutMs) : fetchItems(`${base}transactions`, timeoutMs),
    routescan ? fetchRoutescanItems(`${base}tokentx`, timeoutMs) : fetchItems(`${base}token-transfers`, timeoutMs),
  ]);
  const usable = results.filter((result): result is PromiseFulfilledResult<unknown[]> => result.status === "fulfilled");
  if (usable.length === 0) {
    return {
      transactions: [],
      status: {
        network: config.network,
        chainId: config.chainId,
        chain: config.name,
        explorer: config.explorerBase,
        status: "unavailable",
        transactionsFound: 0,
        note: "Explorer API did not respond successfully.",
      },
    };
  }

  const normalItems = results[0]?.status === "fulfilled" ? results[0].value : [];
  const tokenItems = results[1]?.status === "fulfilled" ? results[1].value : [];
  const normalized = [
    ...normalItems.map(item => routescan ? routescanNormalTransaction(config, address, item) : normalTransaction(config, address, item)),
    ...tokenItems.map(item => routescan ? routescanTokenTransaction(config, address, item) : tokenTransaction(config, address, item)),
  ].filter((item): item is Transaction => Boolean(item));

  const byHash = new Map<string, Transaction>();
  for (const transaction of normalized) {
    if (Date.parse(transaction.timestamp) < sinceMs) continue;
    const key = `${transaction.network}:${transaction.hash.toLowerCase()}`;
    const existing = byHash.get(key);
    if (!existing || existing.asset === "native") byHash.set(key, transaction);
  }

  return {
    transactions: [...byHash.values()],
    status: {
      network: config.network,
      chainId: config.chainId,
      chain: config.name,
      explorer: config.explorerBase,
      status: "searched",
      transactionsFound: byHash.size,
      note: usable.length < 2 ? "One explorer data feed was unavailable; results may be incomplete." : undefined,
    },
  };
}

export async function createTransactionReport(
  rawAddress: string,
  timeoutMs: number,
  now = new Date(),
): Promise<TransactionReport> {
  const address = getAddress(rawAddress);
  const toMs = now.getTime();
  const fromMs = toMs - 7 * 24 * 60 * 60 * 1000;
  const settled = await Promise.allSettled(
    EXPLORERS.map(config => searchExplorer(config, address, fromMs, timeoutMs)),
  );

  const results: ExplorerResult[] = settled.map((result, index) => {
    if (result.status === "fulfilled") return result.value;
    const config = EXPLORERS[index]!;
    return {
      transactions: [],
      status: {
        network: config.network,
        chainId: config.chainId,
        chain: config.name,
        explorer: config.explorerBase,
        status: "unavailable",
        transactionsFound: 0,
        note: "Explorer search failed safely.",
      },
    };
  });

  const transactions = results
    .flatMap(result => result.transactions)
    .sort((left, right) => Date.parse(right.timestamp) - Date.parse(left.timestamp))
    .slice(0, 7);
  const explorers = results.map(result => result.status);
  const unavailable = explorers.filter(item => item.status === "unavailable").length;

  return {
    requestId: randomUUID(),
    address,
    generatedAt: now.toISOString(),
    period: { from: new Date(fromMs).toISOString(), to: now.toISOString(), days: 7 },
    transactionLimit: 7,
    transactions,
    explorers,
    summary: {
      networksSearched: explorers.length - unavailable,
      networksUnavailable: unavailable,
      transactionsReturned: transactions.length,
      partial: unavailable > 0 || explorers.some(item => item.note?.startsWith("One explorer")),
    },
    disclaimer: "Explorer data is observational and may be delayed or incomplete. Verify important transactions on the linked explorer and never treat an address alone as proof of payment.",
  };
}
