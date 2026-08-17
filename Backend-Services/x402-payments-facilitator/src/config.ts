import { defineChain, type Chain } from "viem";

export const NETWORK_CATALOG = {
  "eip155:1": chain(1, "Ethereum", "ETH", "https://ethereum-rpc.publicnode.com"),
  "eip155:137": chain(137, "Polygon", "POL", "https://polygon.drpc.org"),
  "eip155:143": chain(143, "Monad", "MON", "https://rpc.monad.xyz"),
  "eip155:4326": chain(4326, "MegaETH", "ETH", "https://mainnet.megaeth.com/rpc"),
  "eip155:8453": chain(8453, "Base", "ETH", "https://mainnet.base.org"),
  "eip155:84532": chain(84532, "Base Sepolia", "ETH", "https://sepolia.base.org", true),
  "eip155:31612": chain(31612, "Mezo", "BTC", "https://mezo-mainnet.boar.network"),
  "eip155:42161": chain(42161, "Arbitrum One", "ETH", "https://arb1.arbitrum.io/rpc"),
  "eip155:421614": chain(
    421614,
    "Arbitrum Sepolia",
    "ETH",
    "https://sepolia-rollup.arbitrum.io/rpc",
    true,
  ),
  "eip155:42220": chain(42220, "Celo", "CELO", "https://forno.celo.org"),
  "eip155:43114": chain(
    43114,
    "Avalanche C-Chain",
    "AVAX",
    "https://api.avax.network/ext/bc/C/rpc",
  ),
} as const;

export type SupportedNetwork = keyof typeof NETWORK_CATALOG;

export type AppConfig = {
  host: string;
  port: number;
  trustProxy: number;
  privateKey: `0x${string}`;
  networks: SupportedNetwork[];
  complianceUrl?: string;
  complianceToken?: string;
  complianceFailClosed: boolean;
  complianceTimeoutMs: number;
  usageDataFile: string;
  freeSettlements: number;
  settlementFeeUsd: number;
};

function chain(id: number, name: string, symbol: string, rpcUrl: string, testnet = false): Chain {
  return defineChain({
    id,
    name,
    nativeCurrency: { name: symbol, symbol, decimals: 18 },
    rpcUrls: { default: { http: [rpcUrl] } },
    testnet,
  });
}

function integer(name: string, fallback: number, minimum = 0): number {
  const raw = process.env[name];
  const value = raw === undefined || raw === "" ? fallback : Number(raw);
  if (!Number.isInteger(value) || value < minimum) {
    throw new Error(`${name} must be an integer greater than or equal to ${minimum}`);
  }
  return value;
}

function numberValue(name: string, fallback: number, minimum = 0): number {
  const raw = process.env[name];
  const value = raw === undefined || raw === "" ? fallback : Number(raw);
  if (!Number.isFinite(value) || value < minimum) {
    throw new Error(`${name} must be a number greater than or equal to ${minimum}`);
  }
  return value;
}

function booleanValue(name: string, fallback: boolean): boolean {
  const raw = process.env[name];
  if (raw === undefined || raw === "") return fallback;
  if (raw === "true") return true;
  if (raw === "false") return false;
  throw new Error(`${name} must be true or false`);
}

export function loadConfig(): AppConfig {
  const privateKey = process.env.EVM_PRIVATE_KEY;
  if (!privateKey || !/^0x[0-9a-fA-F]{64}$/.test(privateKey)) {
    throw new Error("EVM_PRIVATE_KEY must be a 32-byte 0x-prefixed hexadecimal private key");
  }

  const requested = (process.env.EVM_NETWORKS ?? Object.keys(NETWORK_CATALOG).join(","))
    .split(",")
    .map(value => value.trim())
    .filter(Boolean);
  const unknown = requested.filter(value => !(value in NETWORK_CATALOG));
  if (unknown.length) throw new Error(`Unsupported EVM_NETWORKS: ${unknown.join(", ")}`);
  if (!requested.length) throw new Error("EVM_NETWORKS must contain at least one network");

  return {
    host: process.env.HOST ?? "127.0.0.1",
    port: integer("PORT", 4402, 1),
    trustProxy: integer("TRUST_PROXY", 1),
    privateKey: privateKey as `0x${string}`,
    networks: [...new Set(requested)] as SupportedNetwork[],
    complianceUrl: process.env.COMPLIANCE_WEBHOOK_URL || undefined,
    complianceToken: process.env.COMPLIANCE_WEBHOOK_TOKEN || undefined,
    complianceFailClosed: booleanValue("COMPLIANCE_FAIL_CLOSED", true),
    complianceTimeoutMs: integer("COMPLIANCE_TIMEOUT_MS", 4000, 250),
    usageDataFile: process.env.USAGE_DATA_FILE ?? "./data/usage.json",
    freeSettlements: integer("FREE_SETTLEMENTS_PER_MONTH", 1000),
    settlementFeeUsd: numberValue("SETTLEMENT_FEE_USD", 0.001),
  };
}

export function rpcUrl(network: SupportedNetwork): string {
  const chainId = NETWORK_CATALOG[network].id;
  return process.env[`RPC_${chainId}`] || NETWORK_CATALOG[network].rpcUrls.default.http[0]!;
}
