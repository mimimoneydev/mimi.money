export type ExplorerConfig = {
  network: string;
  chainId: number;
  name: string;
  apiBase?: string;
  apiKind?: "blockscout" | "routescan";
  explorerBase: string;
  testnet?: boolean;
};

export type Transaction = {
  network: string;
  chainId: number;
  chain: string;
  hash: string;
  timestamp: string;
  blockNumber?: number;
  direction: "in" | "out" | "self" | "contract";
  from?: string;
  to?: string;
  status: "success" | "failed" | "unknown";
  asset?: string;
  amount?: string;
  method?: string;
  explorerUrl: string;
};

export type ExplorerStatus = {
  network: string;
  chainId: number;
  chain: string;
  explorer: string;
  status: "searched" | "unavailable";
  transactionsFound: number;
  note?: string;
};

export type TransactionReport = {
  requestId: string;
  address: string;
  generatedAt: string;
  period: {
    from: string;
    to: string;
    days: 7;
  };
  transactionLimit: 7;
  transactions: Transaction[];
  explorers: ExplorerStatus[];
  summary: {
    networksSearched: number;
    networksUnavailable: number;
    transactionsReturned: number;
    partial: boolean;
  };
  intelligence?: {
    provider: "google-ai-studio" | "openrouter";
    upstreamProvider: string;
    model: string;
    status: "generated" | "unavailable";
    overview?: string;
    notableActivity?: string[];
    note?: string;
  };
  disclaimer: string;
};

export type AgentAuthority = {
  mode: "read-only" | "propose" | "autonomous";
  maximumExternalSpendUsd: string;
};

export type AgentRun = {
  id: string;
  status: "running" | "completed" | "failed";
  intent: string;
  address: string;
  createdAt: string;
  updatedAt: string;
  authority: AgentAuthority;
  report?: TransactionReport;
  answer?: string;
  evidence: Array<{ id: string; kind: "explorer-report"; requestId: string }>;
  orchestration: {
    provider: "openclaw";
    status: "generated" | "unavailable";
    note?: string;
  };
  error?: string;
};
