# Agenticous AI agent OpenClaw plugin

OpenClaw tools for evidence-backed EVM investigation and autonomous Circle Agent Wallet operations. Read operations are deterministic. Transfers, swaps, bridges, contract calls, and x402 purchases require no human approval; every write is checked independently by the MiMi Support policy broker for enablement, exact spend, rolling budget, chain, target, simulation, and idempotency.

Required plugin configuration:

```json
{
  "supportClientUrl": "http://127.0.0.1:4411",
  "supportClientTokenFile": "/etc/openclaw/agenticous-client.token",
  "explorerTimeoutMs": 10000
}
```

## Build

```bash
npm install
npm run plugin:build
npm run plugin:validate
npm test
```
