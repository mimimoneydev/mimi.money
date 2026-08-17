import { Type } from "typebox";
import { buildJsonPluginConfigSchema, definePluginEntry, type OpenClawPluginDefinition } from "openclaw/plugin-sdk/plugin-entry";
import { readFileSync } from "node:fs";

type PluginConfig = {
  supportClientUrl?: string;
  supportClientTokenFile?: string;
  explorerTimeoutMs?: number;
};

const explorers = [
  { chainId: 1, chain: "Ethereum", api: "https://eth.blockscout.com", explorer: "https://eth.blockscout.com" },
  { chainId: 137, chain: "Polygon", api: "https://polygon.blockscout.com", explorer: "https://polygon.blockscout.com" },
  { chainId: 4326, chain: "MegaETH", api: "https://megaeth.blockscout.com", explorer: "https://megaeth.blockscout.com" },
  { chainId: 8453, chain: "Base", api: "https://base.blockscout.com", explorer: "https://base.blockscout.com" },
  { chainId: 84532, chain: "Base Sepolia", api: "https://base-sepolia.blockscout.com", explorer: "https://base-sepolia.blockscout.com" },
  { chainId: 31612, chain: "Mezo", api: "https://api.explorer.mezo.org", explorer: "https://explorer.mezo.org" },
  { chainId: 42161, chain: "Arbitrum One", api: "https://arbitrum.blockscout.com", explorer: "https://arbitrum.blockscout.com" },
  { chainId: 421614, chain: "Arbitrum Sepolia", api: "https://arbitrum-sepolia.blockscout.com", explorer: "https://arbitrum-sepolia.blockscout.com" },
  { chainId: 42220, chain: "Celo", api: "https://celo.blockscout.com", explorer: "https://celo.blockscout.com" },
] as const;

function address(value: string): string {
  if (!/^0x[0-9a-fA-F]{40}$/.test(value)) throw new Error("A valid EVM address is required");
  return value;
}

async function jsonFetch(url: string, init: RequestInit, timeoutMs: number): Promise<unknown> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, { ...init, signal: controller.signal });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return await response.json();
  } finally {
    clearTimeout(timeout);
  }
}

async function walletActivity(wallet: string, days: number, timeoutMs: number): Promise<unknown> {
  const normalized = address(wallet);
  const since = Date.now() - days * 86_400_000;
  const settled = await Promise.allSettled(explorers.map(async (item) => {
    const url = `${item.api}/api/v2/addresses/${encodeURIComponent(normalized)}/transactions`;
    const body = await jsonFetch(url, { headers: { accept: "application/json", "user-agent": "Agenticous-OpenClaw/1.0" } }, timeoutMs) as { items?: unknown[] };
    const transactions = Array.isArray(body.items) ? body.items.flatMap((raw) => {
      if (!raw || typeof raw !== "object") return [];
      const tx = raw as Record<string, unknown>;
      if (typeof tx.hash !== "string" || typeof tx.timestamp !== "string" || Date.parse(tx.timestamp) < since) return [];
      return [{ chainId: item.chainId, chain: item.chain, hash: tx.hash, timestamp: tx.timestamp, status: tx.status, from: tx.from, to: tx.to, value: tx.value, explorerUrl: `${item.explorer}/tx/${tx.hash}` }];
    }).slice(0, 20) : [];
    return { chainId: item.chainId, chain: item.chain, status: "searched", transactions };
  }));
  return {
    address: normalized,
    periodDays: days,
    sources: settled.map((result, index) => result.status === "fulfilled" ? result.value : ({ chainId: explorers[index]?.chainId, chain: explorers[index]?.chain, status: "unavailable", transactions: [] })),
    warning: "Explorer-indexed observations may be delayed or incomplete. Use receipt/RPC evidence for important claims.",
  };
}

async function supportClient(config: PluginConfig, path: string, body: unknown): Promise<unknown> {
  const supportClientUrl = config.supportClientUrl ?? "http://127.0.0.1:4411";
  const supportClientTokenFile = config.supportClientTokenFile ?? "/etc/openclaw/agenticous-client.token";
  if (!/^http:\/\/(?:127\.0\.0\.1|localhost|\[::1\]):\d+$/.test(supportClientUrl)) throw new Error("Support client URL must be loopback");
  if (supportClientTokenFile !== "/etc/openclaw/agenticous-client.token") throw new Error("Support client token file is not allowlisted");
  const supportClientToken = readFileSync(supportClientTokenFile, "utf8").trim();
  if (supportClientToken.length < 32) throw new Error("Support client token is not configured");
  return jsonFetch(`${supportClientUrl}${path}`, {
    method: "POST",
    headers: { authorization: `Bearer ${supportClientToken}`, "content-type": "application/json" },
    body: JSON.stringify(body),
  }, 35_000);
}

const plugin: OpenClawPluginDefinition = definePluginEntry({
  id: "agenticous-blockchain",
  name: "Agenticous AI agent",
  description: "Evidence-backed multichain investigation and machine-policy-gated autonomous Circle Agent Wallet operations for the Agenticous AI agent, MiMi Support, and x402 clients.",
  configSchema: buildJsonPluginConfigSchema({
    type: "object",
    additionalProperties: false,
    properties: {
      supportClientUrl: { type: "string" },
      supportClientTokenFile: { type: "string" },
      explorerTimeoutMs: { type: "integer", minimum: 500, maximum: 30000 },
    },
  }),
  register(api) {
    const config = api.pluginConfig as PluginConfig;
    api.registerTool({
      name: "agenticous_wallet_activity",
      label: "Wallet activity",
      description: "Inspect recent EVM wallet activity across supported Blockscout explorers. Results are observations and identify unavailable sources.",
      parameters: Type.Object({
        address: Type.String({ description: "Public EVM wallet address" }),
        days: Type.Optional(Type.Integer({ minimum: 1, maximum: 30, default: 7 })),
      }),
      async execute(_id, params) {
        const input = params as { address: string; days?: number };
        const details = await walletActivity(String(input.address), Number(input.days ?? 7), config.explorerTimeoutMs ?? 10_000);
        return { content: [{ type: "text", text: JSON.stringify(details) }], details };
      },
    });
    api.registerTool({
      name: "agenticous_chain_query",
      label: "Blockchain RPC query",
      description: "Read native balances, ERC-20 allowances, transaction receipts, contract calls, and finality through the policy-controlled support client.",
      parameters: Type.Object({
        operation: Type.Union([Type.Literal("native_balance"), Type.Literal("erc20_balance"), Type.Literal("allowance"), Type.Literal("receipt"), Type.Literal("contract_query")]),
        chainId: Type.Integer({ minimum: 1 }),
        address: Type.Optional(Type.String()),
        token: Type.Optional(Type.String()),
        spender: Type.Optional(Type.String()),
        transactionHash: Type.Optional(Type.String()),
        contract: Type.Optional(Type.String()),
        data: Type.Optional(Type.String({ description: "ABI-encoded eth_call data" })),
      }),
      async execute(_id, params) {
        const details = await supportClient(config, "/v1/tools/chain-query", params);
        return { content: [{ type: "text", text: JSON.stringify(details) }], details };
      },
    });
    api.registerTool({
      name: "agenticous_circle_wallet",
      label: "Circle wallet status",
      description: "Read the configured Circle Agent Wallet address, balance, transaction history, and spending-policy budget.",
      parameters: Type.Object({ operation: Type.Union([Type.Literal("status"), Type.Literal("balance"), Type.Literal("transactions"), Type.Literal("budget")]) }),
      async execute(_id, params) {
        const details = await supportClient(config, "/v1/tools/circle-wallet", params);
        return { content: [{ type: "text", text: JSON.stringify(details) }], details };
      },
    });
    api.registerTool({
      name: "agenticous_circle_transfer",
      label: "Circle transfer",
      description: "Autonomously transfer USDC from the Circle Agent Wallet without human approval. The support client independently enforces machine policy, idempotency, and budgets.",
      parameters: Type.Object({ recipient: Type.String(), amountUsd: Type.String({ pattern: "^[0-9]+(?:\\.[0-9]{1,6})?$" }), reason: Type.String({ minLength: 1, maxLength: 240 }) }),
      async execute(_id, params) {
        const details = await supportClient(config, "/v1/tools/circle-transfer", { ...params as object, idempotencyKey: `transfer:${_id}` });
        return { content: [{ type: "text", text: JSON.stringify(details) }], details };
      },
    }, { optional: true });
    api.registerTool({
      name: "agenticous_x402_search",
      label: "Search x402 services",
      description: "Search Circle's x402 service marketplace by keyword.",
      parameters: Type.Object({ query: Type.Optional(Type.String({ maxLength: 100 })) }),
      async execute(_id, params) {
        const details = await supportClient(config, "/v1/tools/circle-services", { ...params as object, operation: "search" });
        return { content: [{ type: "text", text: JSON.stringify(details) }], details };
      },
    });
    api.registerTool({
      name: "agenticous_x402_purchase",
      label: "Purchase x402 service",
      description: "Autonomously purchase from an eligible HTTPS x402 endpoint within per-action and daily USDC budgets, without human approval.",
      parameters: Type.Object({
        url: Type.String(), method: Type.Optional(Type.Union([Type.Literal("GET"), Type.Literal("POST")])),
        data: Type.Optional(Type.String({ maxLength: 8192, description: "JSON request body" })),
        maximumUsd: Type.String({ pattern: "^[0-9]+(?:\\.[0-9]{1,6})?$" }),
      }),
      async execute(_id, params) {
        const details = await supportClient(config, "/v1/tools/circle-services", { ...params as object, operation: "purchase", idempotencyKey: `x402:${_id}` });
        return { content: [{ type: "text", text: JSON.stringify(details) }], details };
      },
    }, { optional: true });
    api.registerTool({
      name: "agenticous_autonomous_action",
      label: "Autonomous onchain action",
      description: "Autonomously transfer, swap, bridge, or execute a contract after independent simulation, budget, chain, recipient, and contract policy checks. No human approval is requested.",
      parameters: Type.Object({
        operation: Type.Union([Type.Literal("transfer"), Type.Literal("swap"), Type.Literal("bridge"), Type.Literal("contract_execute")]),
        chain: Type.String(),
        maximumUsd: Type.String({ pattern: "^[0-9]+(?:\\.[0-9]{1,6})?$" }),
        recipient: Type.Optional(Type.String()), amountUsd: Type.Optional(Type.String()),
        sellToken: Type.Optional(Type.String()), sellAmount: Type.Optional(Type.String()), buyToken: Type.Optional(Type.String()), minimumBuyAmount: Type.Optional(Type.String()), slippageBps: Type.Optional(Type.Integer({ minimum: 1, maximum: 500 })),
        destinationChain: Type.Optional(Type.String()), contract: Type.Optional(Type.String()), functionSignature: Type.Optional(Type.String()), parameters: Type.Optional(Type.Array(Type.String(), { maxItems: 32 })), nativeAmount: Type.Optional(Type.String()),
        reason: Type.String({ minLength: 1, maxLength: 240 }),
      }),
      async execute(id, params) {
        const details = await supportClient(config, "/v1/tools/circle-action", { ...params as object, idempotencyKey: `action:${id}` });
        return { content: [{ type: "text", text: JSON.stringify(details) }], details };
      },
    }, { optional: true });
  },
});

export default plugin;
