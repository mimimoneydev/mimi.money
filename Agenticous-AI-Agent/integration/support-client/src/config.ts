import "dotenv/config";
import { getAddress, isAddress } from "viem";
import { privateKeyToAccount } from "viem/accounts";

export type ClientConfig = {
  host: string;
  port: number;
  internalToken: string;
  payerPrivateKey?: `0x${string}`;
  payerAddress: `0x${string}`;
  paymentMode: "circle-agent-wallet" | "local-key";
  circleCliPath: string;
  circleChain: string;
  agenticousUrl: string;
  sellerAddress: `0x${string}`;
  paymentNetwork: `eip155:${string}`;
  paymentAmountAtomic: string;
  requestTimeoutMs: number;
  rpcUrls: Readonly<Record<number, string>>;
  autonomousTransfersEnabled: boolean;
  maximumTransferUsd: string;
  allowedTransferRecipients: readonly `0x${string}`[];
  recipientPolicy: "allowlist" | "any";
  x402PurchasesEnabled: boolean;
  maximumX402PurchaseUsd: string;
  allowedX402Hosts: readonly string[];
  x402HostPolicy: "allowlist" | "public-internet";
  autonomousActionsEnabled: boolean;
  maximumActionUsd: string;
  maximumDailyUsd: string;
  allowedActionChains: readonly string[];
  contractPolicy: "allowlist" | "any";
  allowedContracts: readonly `0x${string}`[];
  autonomyLedgerPath: string;
};

function integer(name: string, fallback: number, minimum: number): number {
  const raw = process.env[name];
  const value = raw ? Number(raw) : fallback;
  if (!Number.isInteger(value) || value < minimum) throw new Error(`${name} is invalid`);
  return value;
}

export function loadConfig(): ClientConfig {
  const privateKey = process.env.PAYER_PRIVATE_KEY ?? "";
  const circleAddress = process.env.CIRCLE_AGENT_WALLET_ADDRESS?.trim() ?? "";
  const useCircleAgentWallet = circleAddress !== "";
  if (!useCircleAgentWallet && !/^0x[0-9a-fA-F]{64}$/.test(privateKey)) {
    throw new Error("Configure CIRCLE_AGENT_WALLET_ADDRESS or a 32-byte PAYER_PRIVATE_KEY");
  }
  if (useCircleAgentWallet && (!isAddress(circleAddress) || /^0x0{40}$/i.test(circleAddress))) {
    throw new Error("CIRCLE_AGENT_WALLET_ADDRESS is invalid");
  }
  const internalToken = process.env.INTERNAL_TOKEN ?? "";
  if (internalToken.length < 32) throw new Error("INTERNAL_TOKEN must contain at least 32 characters");
  const seller = process.env.AGENTICOUS_SELLER_ADDRESS ?? "";
  if (!isAddress(seller) || /^0x0{40}$/i.test(seller)) throw new Error("AGENTICOUS_SELLER_ADDRESS is invalid");
  const paymentNetwork = process.env.PAYMENT_NETWORK ?? "eip155:8453";
  if (!/^eip155:\d+$/.test(paymentNetwork)) throw new Error("PAYMENT_NETWORK is invalid");
  const amount = process.env.PAYMENT_AMOUNT_ATOMIC ?? "10000";
  if (!/^\d+$/.test(amount) || BigInt(amount) <= 0n) throw new Error("PAYMENT_AMOUNT_ATOMIC is invalid");
  const agenticous = new URL(process.env.AGENTICOUS_URL ?? "https://agenticous.mimi.money");
  if (!["https:", "http:"].includes(agenticous.protocol)) throw new Error("AGENTICOUS_URL must be HTTP(S)");
  const account = useCircleAgentWallet ? undefined : privateKeyToAccount(privateKey as `0x${string}`);
  const rpcDefaults: Record<number, string> = {
    1: "https://ethereum-rpc.publicnode.com",
    137: "https://polygon-bor-rpc.publicnode.com",
    8453: "https://mainnet.base.org",
    84532: "https://sepolia.base.org",
    42161: "https://arb1.arbitrum.io/rpc",
    421614: "https://sepolia-rollup.arbitrum.io/rpc",
    43114: "https://api.avax.network/ext/bc/C/rpc",
  };
  for (const chainId of Object.keys(rpcDefaults).map(Number)) {
    const override = process.env[`RPC_URL_${chainId}`]?.trim();
    if (override) {
      const parsed = new URL(override);
      if (parsed.protocol !== "https:") throw new Error(`RPC_URL_${chainId} must use HTTPS`);
      rpcDefaults[chainId] = parsed.toString();
    }
  }
  const maximumTransferUsd = process.env.MAXIMUM_AUTONOMOUS_TRANSFER_USD?.trim() || "1.00";
  if (!/^\d+(?:\.\d{1,6})?$/.test(maximumTransferUsd) || Number(maximumTransferUsd) <= 0) throw new Error("MAXIMUM_AUTONOMOUS_TRANSFER_USD is invalid");
  const allowedTransferRecipients = (process.env.ALLOWED_TRANSFER_RECIPIENTS ?? "").split(",").map(item => item.trim()).filter(Boolean);
  if (allowedTransferRecipients.some(item => !isAddress(item))) throw new Error("ALLOWED_TRANSFER_RECIPIENTS contains an invalid address");
  const maximumX402PurchaseUsd = process.env.MAXIMUM_X402_PURCHASE_USD?.trim() || "0.05";
  if (!/^\d+(?:\.\d{1,6})?$/.test(maximumX402PurchaseUsd) || Number(maximumX402PurchaseUsd) <= 0 || Number(maximumX402PurchaseUsd) > 0.05) throw new Error("MAXIMUM_X402_PURCHASE_USD is invalid");
  const allowedX402Hosts = (process.env.ALLOWED_X402_HOSTS ?? "").split(",").map(item => item.trim().toLowerCase()).filter(Boolean);
  if (allowedX402Hosts.some(item => !/^(?:[a-z0-9-]+\.)+[a-z]{2,}$/.test(item))) throw new Error("ALLOWED_X402_HOSTS contains an invalid hostname");
  const choice = <T extends string>(name: string, fallback: T, allowed: readonly T[]): T => {
    const value = (process.env[name]?.trim() || fallback) as T;
    if (!allowed.includes(value)) throw new Error(`${name} is invalid`);
    return value;
  };
  const maximumActionUsd = process.env.MAXIMUM_AUTONOMOUS_ACTION_USD?.trim() || "1.00";
  const maximumDailyUsd = process.env.MAXIMUM_AUTONOMOUS_DAILY_USD?.trim() || "5.00";
  for (const [name, value] of [["MAXIMUM_AUTONOMOUS_ACTION_USD", maximumActionUsd], ["MAXIMUM_AUTONOMOUS_DAILY_USD", maximumDailyUsd]] as const) {
    if (!/^\d+(?:\.\d{1,6})?$/.test(value) || Number(value) <= 0) throw new Error(`${name} is invalid`);
  }
  const allowedActionChains = (process.env.ALLOWED_AUTONOMOUS_CHAINS ?? "BASE").split(",").map(item => item.trim().toUpperCase()).filter(Boolean);
  if (allowedActionChains.some(item => !/^[A-Z][A-Z0-9-]{1,31}$/.test(item))) throw new Error("ALLOWED_AUTONOMOUS_CHAINS is invalid");
  const allowedContracts = (process.env.ALLOWED_AUTONOMOUS_CONTRACTS ?? "").split(",").map(item => item.trim()).filter(Boolean);
  if (allowedContracts.some(item => !isAddress(item))) throw new Error("ALLOWED_AUTONOMOUS_CONTRACTS contains an invalid address");
  const autonomyLedgerPath = process.env.AUTONOMY_LEDGER_PATH?.trim() || "/var/lib/mimi-support-x402/autonomy-ledger.json";
  if (!autonomyLedgerPath.startsWith("/var/lib/mimi-support-x402/")) throw new Error("AUTONOMY_LEDGER_PATH must be below /var/lib/mimi-support-x402");

  return {
    host: process.env.HOST ?? "127.0.0.1",
    port: integer("PORT", 4411, 1),
    internalToken,
    payerPrivateKey: account ? privateKey as `0x${string}` : undefined,
    payerAddress: useCircleAgentWallet ? getAddress(circleAddress) : account!.address,
    paymentMode: useCircleAgentWallet ? "circle-agent-wallet" : "local-key",
    circleCliPath: process.env.CIRCLE_CLI_PATH?.trim() || "/usr/local/bin/circle",
    circleChain: process.env.CIRCLE_CHAIN?.trim() || "BASE",
    agenticousUrl: agenticous.toString().replace(/\/$/, ""),
    sellerAddress: getAddress(seller),
    paymentNetwork: paymentNetwork as `eip155:${string}`,
    paymentAmountAtomic: amount,
    requestTimeoutMs: integer("REQUEST_TIMEOUT_MS", 30_000, 1_000),
    rpcUrls: rpcDefaults,
    autonomousTransfersEnabled: process.env.AUTONOMOUS_TRANSFERS_ENABLED === "true",
    maximumTransferUsd,
    allowedTransferRecipients: allowedTransferRecipients.map(item => getAddress(item)),
    recipientPolicy: choice("AUTONOMOUS_RECIPIENT_POLICY", "allowlist", ["allowlist", "any"]),
    x402PurchasesEnabled: process.env.X402_PURCHASES_ENABLED === "true",
    maximumX402PurchaseUsd,
    allowedX402Hosts,
    x402HostPolicy: choice("AUTONOMOUS_X402_HOST_POLICY", "allowlist", ["allowlist", "public-internet"]),
    autonomousActionsEnabled: process.env.AUTONOMOUS_ACTIONS_ENABLED === "true",
    maximumActionUsd,
    maximumDailyUsd,
    allowedActionChains,
    contractPolicy: choice("AUTONOMOUS_CONTRACT_POLICY", "allowlist", ["allowlist", "any"]),
    allowedContracts: allowedContracts.map(item => getAddress(item)),
    autonomyLedgerPath,
  };
}
