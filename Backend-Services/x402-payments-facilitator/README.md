# MiMi Money X402 Facilitator

MiMi Money X402 is a self-hosted TypeScript facilitator for the [x402](https://docs.x402.org/) HTTP payment protocol. It verifies signed payment authorizations and settles compatible stablecoin or token payments on supported EVM networks.

The production deployment is available at **<https://x402.mimi.money>**. Apache terminates HTTPS and proxies requests to a loopback-only Node.js service managed by systemd.

> **Production readiness:** the application and HTTPS service are deployed, but settlement is not operational until the facilitator address has native gas on each enabled network. With the default fail-closed policy, payment verification and settlement are also denied until a compliance screening webhook is configured.

## Contents

- [What the facilitator does](#what-the-facilitator-does)
- [Features](#features)
- [Architecture](#architecture)
- [How x402 payments work](#how-x402-payments-work)
- [Supported networks](#supported-networks)
- [Schemes and forms of value](#schemes-and-forms-of-value)
- [Use cases](#use-cases)
- [Public API](#public-api)
- [Integrate a resource server](#integrate-a-resource-server)
- [Build an x402 client](#build-an-x402-client)
- [Local setup](#local-setup)
- [Configuration](#configuration)
- [Production deployment](#production-deployment)
- [Compliance screening](#compliance-screening)
- [Pricing and usage metering](#pricing-and-usage-metering)
- [Operations](#operations)
- [Security model](#security-model)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Project layout](#project-layout)

## What the facilitator does

The facilitator is the payment-processing component between an x402 resource server and an EVM network. It:

1. Publishes the payment schemes and networks it supports.
2. Receives a signed payment payload and the seller's payment requirements.
3. Optionally sends the complete request to a compliance-screening provider.
4. Verifies the authorization, balance, timing, recipient, asset, and signature rules through the official x402 SDK.
5. Submits valid payments to the correct configured network.
6. Waits for an on-chain receipt and returns the standard x402 settlement response.
7. Records each unique successful settlement in the monthly pricing meter.

The facilitator does **not** host the seller's paid content. A resource server protects that content and calls this service to verify and settle payments.

## Features

- x402 protocol version 2 using `@x402/core` and `@x402/evm` 2.21.0.
- Standard `/supported`, `/verify`, and `/settle` facilitator endpoints.
- Versioned `/v1/*` aliases for operational integrations.
- Eleven explicitly configured EVM mainnet and testnet networks.
- Separate Viem public and wallet clients for every chain, preventing wrong-chain RPC reuse.
- `exact` payments for fixed-price resources.
- `upto` payments for metered or usage-capped resources.
- EIP-3009 and Permit2 routing provided by the official x402 EVM implementation.
- Optional external compliance screening with configurable fail-open or fail-closed behavior.
- Persistent UTC monthly settlement metering with transaction deduplication.
- First 1,000 successful settlements per month free, then $0.001 per settlement.
- Request IDs, JSON errors, body-size limits, security headers, and rate limiting on versioned API routes.
- Graceful `SIGTERM`/`SIGINT` shutdown for systemd restarts.
- Branded responsive MiMi Money website and service-status display.
- Apache HTTPS reverse proxy with Let's Encrypt renewal.
- Hardened, unprivileged systemd service with automatic restart.

## Architecture

```text
Buyer / AI agent
      │
      │  HTTP request + PAYMENT-SIGNATURE
      ▼
Seller's x402 resource server
      │
      ├── POST /verify ──────────────────────────────┐
      │                                              │
      └── POST /settle ──────────────────────────────┤
                                                     ▼
Internet ── HTTPS ── Apache ── 127.0.0.1:4402 ── MiMi facilitator
                                                        │
                         compliance webhook ◄───────────┤
                                                        │
                         usage meter ◄───────────────────┤
                                                        │
                         chain-specific RPC client ◄─────┘
                                                        │
                                                        ▼
                                                Supported EVM chain
```

Runtime boundaries:

- **Apache:** public ports 80/443, TLS termination, HTTP-to-HTTPS redirection, reverse proxy.
- **Node.js:** bound to `127.0.0.1:4402`; never exposed directly to the internet.
- **systemd:** runs Node as the dedicated `mimi-x402` user and restarts it after failures.
- **Environment:** `/etc/mimi-x402/facilitator.env`, readable only by root.
- **Mutable data:** `/var/lib/mimi-x402/usage.json`, writable only by the service account.
- **Application:** `/var/www/x402.mimi.money`.

## How x402 payments work

1. A client requests a protected HTTP resource.
2. The resource server responds with HTTP `402 Payment Required` and accepted payment terms.
3. The client selects a scheme/network and signs a payment authorization with its wallet.
4. The client repeats the request with a `PAYMENT-SIGNATURE` header.
5. The resource server calls MiMi Money `/verify`.
6. After successful verification, the resource server performs the requested work.
7. The resource server calls MiMi Money `/settle`.
8. MiMi Money submits the authorized transaction and waits for confirmation.
9. The resource server returns the content and a `PAYMENT-RESPONSE` header.

The facilitator cannot move arbitrary wallet funds. It can only execute payment authorizations signed by the payer according to the selected scheme.

## Supported networks

Networks use CAIP-2 identifiers. Enabled networks are controlled with `EVM_NETWORKS`.

| Network | CAIP-2 ID | Chain ID | Native gas asset | Environment |
|---|---:|---:|---|---|
| Ethereum | `eip155:1` | 1 | ETH | Mainnet |
| Polygon | `eip155:137` | 137 | POL | Mainnet |
| Monad | `eip155:143` | 143 | MON | Mainnet |
| MegaETH | `eip155:4326` | 4326 | ETH | Mainnet |
| Base | `eip155:8453` | 8453 | ETH | Mainnet |
| Base Sepolia | `eip155:84532` | 84532 | ETH | Testnet |
| Mezo | `eip155:31612` | 31612 | BTC | Mainnet |
| Arbitrum One | `eip155:42161` | 42161 | ETH | Mainnet |
| Arbitrum Sepolia | `eip155:421614` | 421614 | ETH | Testnet |
| Celo | `eip155:42220` | 42220 | CELO | Mainnet |
| Avalanche C-Chain | `eip155:43114` | 43114 | AVAX | Mainnet |

Every enabled network currently advertises both `exact` and `upto`, producing 22 scheme/network capability entries. Check the live configuration instead of hard-coding assumptions:

```sh
curl -fsS https://x402.mimi.money/v1/networks
curl -fsS https://x402.mimi.money/supported
```

### RPC behavior

Public RPC defaults are included for evaluation. Production operators should configure dedicated authenticated endpoints through the corresponding `RPC_<CHAIN_ID>` variables. The service creates a distinct signer/client pair for each network and verifies RPC chain IDs during deployment checks.

## Schemes and forms of value

### Exact

Use `exact` when the final price is known before the request, such as `$0.01` for an API response, file, article, or model inference.

### Upto

Use `upto` when the payer authorizes a maximum and the seller settles the actual usage, such as tokens consumed, compute time, bytes transferred, or tool calls.

### Stablecoins and tokens

The official EVM mechanism routes compatible payments through EIP-3009 or Permit2. The asset contract, amount, network, and token-specific metadata come from the resource server's payment requirements. Resource servers should use an x402 default stablecoin where available or provide an explicit compatible token definition.

Supporting a chain does not mean every token on that chain is automatically safe or compatible. Before offering an asset, validate its contract address, decimals, EIP-712 domain data, transfer mechanism, liquidity, and issuer risk.

### Native assets

The facilitator wallet needs each chain's native asset for settlement gas. The buyer's payment asset is normally a compatible ERC-20 token, not the gas asset.

### Fiat

x402 is network and currency agnostic, but this deployment contains only the official EVM mechanism. Fiat settlement requires a separate payment-processor adapter, merchant account, webhook verification, reconciliation, refund handling, and compliance credentials. No fiat rail is currently active.

## Use cases

- Paid APIs without API keys or monthly subscriptions.
- AI agents purchasing data, tools, inference, or compute autonomously.
- Per-article, per-file, or per-download content access.
- Usage-metered inference, storage, bandwidth, or RPC calls.
- Machine-to-machine services and MCP tool monetization.
- Pay-per-query datasets and market-data endpoints.
- SaaS feature unlocks and credit top-ups.
- Cross-chain checkout where the seller offers several accepted networks.
- Testnet prototyping on Base Sepolia or Arbitrum Sepolia before mainnet rollout.

## Public API

Base URL: `https://x402.mimi.money`

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/` | Branded service website |
| `GET` | `/healthz` | Process, network, facilitator-address, and compliance readiness |
| `GET` | `/supported` | Standard x402 capability discovery |
| `POST` | `/verify` | Standard x402 payment verification |
| `POST` | `/settle` | Standard x402 on-chain settlement |
| `GET` | `/v1/supported` | Versioned capability-discovery alias |
| `POST` | `/v1/verify` | Versioned verification alias |
| `POST` | `/v1/settle` | Versioned settlement alias |
| `GET` | `/v1/networks` | Human-readable enabled-network list |
| `GET` | `/v1/pricing` | Current UTC monthly usage and pricing snapshot |

### Health

```sh
curl -fsS https://x402.mimi.money/healthz
```

Example:

```json
{
  "status": "ok",
  "service": "mimi-money-x402-facilitator",
  "x402Version": 2,
  "facilitatorAddress": "0x...",
  "networks": ["eip155:8453", "eip155:84532"],
  "compliance": "configured",
  "compliancePolicy": "fail-closed",
  "uptimeSeconds": 3600
}
```

`status: "ok"` means the process is running. Inspect `compliance` separately; `not-configured` with a fail-closed policy means payment operations will be denied.

### Verify and settle request

Both endpoints accept the standard x402 body:

```json
{
  "paymentPayload": {
    "x402Version": 2,
    "accepted": {
      "scheme": "exact",
      "network": "eip155:84532",
      "asset": "0x...",
      "amount": "1000",
      "payTo": "0x...",
      "maxTimeoutSeconds": 60,
      "extra": {}
    },
    "payload": {}
  },
  "paymentRequirements": {
    "scheme": "exact",
    "network": "eip155:84532",
    "asset": "0x...",
    "amount": "1000",
    "payTo": "0x...",
    "maxTimeoutSeconds": 60,
    "extra": {}
  }
}
```

Applications should generate this structure through an x402 SDK. Do not manually construct production signatures.

### Errors and request IDs

- Missing or unsupported input returns HTTP `400`.
- Compliance denial returns HTTP `403` with a protocol-compatible failure body.
- Unexpected facilitator/RPC failures return HTTP `500` without exposing internal details.
- Responses include `x-request-id`; callers may supply their own `X-Request-ID` up to 128 characters.
- JSON request bodies are limited to 128 KiB.
- `/v1/*` routes are limited to 120 requests per minute per proxied client IP.

## Integrate a resource server

The resource server owns the paid endpoint, price, receiving wallet, and accepted networks. The facilitator performs verification and settlement.

Install the server packages in the resource-server project:

```sh
npm install @x402/core @x402/evm @x402/express express
```

Example Express resource server:

```ts
import express from "express";
import { HTTPFacilitatorClient, x402ResourceServer } from "@x402/core/server";
import { paymentMiddleware } from "@x402/express";
import { ExactEvmScheme } from "@x402/evm/exact/server";

const seller = process.env.SELLER_ADDRESS as `0x${string}`;
if (!seller) throw new Error("SELLER_ADDRESS is required");

const facilitator = new HTTPFacilitatorClient({
  url: "https://x402.mimi.money",
});

const resourceServer = new x402ResourceServer(facilitator).register(
  "eip155:84532",
  new ExactEvmScheme(),
);

const app = express();

app.use(
  paymentMiddleware(
    {
      "GET /premium-data": {
        accepts: [
          {
            scheme: "exact",
            price: "$0.01",
            network: "eip155:84532",
            payTo: seller,
          },
        ],
        description: "MiMi premium data",
        mimeType: "application/json",
      },
    },
    resourceServer,
  ),
);

app.get("/premium-data", (_req, res) => {
  res.json({ value: "paid response" });
});

app.listen(4021);
```

Before offering a mainnet route:

1. Confirm the network appears in `/supported`.
2. Use a seller wallet you control as `payTo`; do not use the facilitator address.
3. Confirm the payment asset and transfer mechanism are compatible.
4. Test the complete `402 → sign → verify → settle → 200` flow on testnet.
5. Add idempotency, request logging, and application-level fulfillment controls.

## Build an x402 client

Clients need a funded payer wallet and an x402-aware HTTP wrapper.

```sh
npm install @x402/core @x402/evm @x402/fetch viem
```

```ts
import { x402Client, wrapFetchWithPayment, x402HTTPClient } from "@x402/fetch";
import { ExactEvmScheme } from "@x402/evm/exact/client";
import { UptoEvmScheme } from "@x402/evm/upto/client";
import { privateKeyToAccount } from "viem/accounts";

const privateKey = process.env.PAYER_PRIVATE_KEY as `0x${string}`;
if (!privateKey) throw new Error("PAYER_PRIVATE_KEY is required");

const signer = privateKeyToAccount(privateKey);
const paymentClient = new x402Client()
  .register("eip155:*", new ExactEvmScheme(signer))
  .register("eip155:*", new UptoEvmScheme(signer));

const fetchWithPayment = wrapFetchWithPayment(fetch, paymentClient);
const response = await fetchWithPayment("https://seller.example/premium-data");
const result = await new x402HTTPClient(paymentClient).processResponse(response);

console.dir(result, { depth: null });
```

Never put an unattended private key in browser JavaScript. Browser clients should use a wallet connector or a purpose-built delegated wallet with explicit spending controls.

## Local setup

### Requirements

- Linux, macOS, or another Node-compatible environment.
- Node.js 18.18 or newer.
- npm 9 or newer.
- An EVM private key dedicated to facilitator settlement.
- RPC access for each enabled network.
- Native gas on each network used for settlement.

### Install

```sh
git clone https://github.com/<your-github-username>/mimi-money-x402-facilitator.git
cd mimi-money-x402-facilitator
npm ci
cp .env.example .env
```

Replace `<your-github-username>` with the GitHub account or organization that
hosts your fork. `npm ci` installs the exact dependency versions recorded in
`package-lock.json` and is recommended for a reproducible local setup.

Generate a new private key with a trusted wallet or secret-management system, then set `EVM_PRIVATE_KEY` in `.env`. Do not reuse a personal wallet key or commit `.env`.

For local evaluation without a screening provider, explicitly choose a fail-open policy:

```dotenv
COMPLIANCE_FAIL_CLOSED=false
```

That setting is not recommended for regulated production traffic.

Build and start:

```sh
npm run typecheck
npm test
npm run build
npm start
```

The default listener is `http://127.0.0.1:4402`.

Development watch mode:

```sh
npm run dev
```

## Configuration

Configuration is read through environment variables at process startup.

| Variable | Required | Default | Description |
|---|---|---|---|
| `HOST` | No | `127.0.0.1` | Listener address. Keep loopback when using Apache. |
| `PORT` | No | `4402` | Internal HTTP port. |
| `TRUST_PROXY` | No | `1` | Number of trusted reverse-proxy hops used by Express. |
| `LOG_LEVEL` | No | `info` | Reserved operational log-level setting. |
| `EVM_PRIVATE_KEY` | Yes | — | 32-byte, `0x`-prefixed facilitator settlement key. |
| `EVM_NETWORKS` | No | All catalog networks | Comma-separated CAIP-2 IDs to register. |
| `RPC_<CHAIN_ID>` | No | Public chain default | Per-network JSON-RPC override. |
| `COMPLIANCE_WEBHOOK_URL` | Production | — | Screening endpoint called before verify and settle. |
| `COMPLIANCE_WEBHOOK_TOKEN` | No | — | Bearer token sent to the screening endpoint. |
| `COMPLIANCE_FAIL_CLOSED` | No | `true` | Deny payment operations when screening is absent/unavailable. |
| `COMPLIANCE_TIMEOUT_MS` | No | `4000` | Screening timeout; minimum 250 ms. |
| `FREE_SETTLEMENTS_PER_MONTH` | No | `1000` | Number of free unique successful settlements each UTC month. |
| `SETTLEMENT_FEE_USD` | No | `0.001` | Metered fee after the free tier. |
| `USAGE_DATA_FILE` | No | `./data/usage.json` | Persistent usage-state path. |

Example network restriction:

```dotenv
EVM_NETWORKS=eip155:84532,eip155:421614
RPC_84532=https://your-base-sepolia-provider.example
RPC_421614=https://your-arbitrum-sepolia-provider.example
```

The application validates booleans, numbers, private-key shape, enabled-network names, and minimum values during startup. Invalid configuration causes an immediate non-zero exit so systemd can surface the failure.

## Production deployment

The checked-in deployment templates target Ubuntu with Apache and systemd.

### 1. Build the release

```sh
cd /var/www/x402.mimi.money
npm ci
npm run typecheck
npm test
npm run build
npm audit --omit=dev
```

### 2. Create the service account and state directory

```sh
sudo useradd --system --home-dir /nonexistent --shell /usr/sbin/nologin mimi-x402
sudo install -d -m 0750 -o mimi-x402 -g mimi-x402 /var/lib/mimi-x402
sudo install -d -m 0750 -o root -g mimi-x402 /etc/mimi-x402
```

If the user already exists, skip `useradd`.

### 3. Install production configuration

```sh
sudo install -m 0600 -o root -g root \
  deployment/facilitator.env /etc/mimi-x402/facilitator.env
sudoedit /etc/mimi-x402/facilitator.env
```

Replace `GENERATE_ON_INSTALL` with a dedicated secret key and configure dedicated RPC URLs plus the compliance provider. Never expose the private key in logs, shell history, tickets, or monitoring output.

### 4. Install and start systemd

```sh
sudo install -m 0644 deployment/mimi-x402.service \
  /etc/systemd/system/mimi-x402.service
sudo systemctl daemon-reload
sudo systemctl enable --now mimi-x402.service
sudo systemctl status mimi-x402.service --no-pager
```

### 5. Configure Apache

Required modules:

```sh
sudo a2enmod proxy proxy_http rewrite ssl headers
sudo install -m 0644 deployment/x402.mimi.money.conf \
  /etc/apache2/sites-available/x402.mimi.money.conf
sudo a2ensite x402.mimi.money.conf
sudo apache2ctl configtest
sudo systemctl reload apache2
```

Point the DNS `A`/`AAAA` record for `x402.mimi.money` to the server before requesting TLS.

### 6. Install TLS

```sh
sudo certbot --apache --non-interactive --agree-tos --redirect \
  -d x402.mimi.money
sudo systemctl is-enabled certbot.timer
sudo certbot renew --dry-run
```

### 7. Fund the facilitator

Get the address without printing the private key:

```sh
curl -fsS https://x402.mimi.money/healthz
```

Send a carefully limited amount of the native gas asset to that address on every enabled chain. Monitor balances and replenish through a controlled treasury process. The same EVM address is used across chains, but balances are network-specific.

### 8. Validate production

```sh
curl -fsS https://x402.mimi.money/healthz
curl -fsS https://x402.mimi.money/supported
curl -fsS https://x402.mimi.money/v1/networks
curl -fsS https://x402.mimi.money/v1/pricing
```

Complete at least one low-value testnet payment before enabling any mainnet seller routes.

## Compliance screening

Screening is performed before both verification and settlement. When configured, the facilitator sends:

```http
POST <COMPLIANCE_WEBHOOK_URL>
Content-Type: application/json
Authorization: Bearer <COMPLIANCE_WEBHOOK_TOKEN>
```

```json
{
  "paymentPayload": { "...": "complete standard x402 payload" },
  "paymentRequirements": { "...": "complete seller requirements" }
}
```

The provider must return JSON containing a boolean `allowed` field:

```json
{
  "allowed": true,
  "provider": "screening-provider-name"
}
```

Denial example:

```json
{
  "allowed": false,
  "reason": "Policy match"
}
```

Behavior:

- `allowed: false` rejects the operation with HTTP `403`.
- A non-2xx response, timeout, invalid JSON, or missing `allowed` is a provider failure.
- With `COMPLIANCE_FAIL_CLOSED=true`, provider failure denies the payment.
- With `COMPLIANCE_FAIL_CLOSED=false`, provider failure allows the payment to continue.
- The current integration sends payment data to the configured provider. Review data-processing, retention, jurisdiction, and privacy requirements before production use.

The webhook is an integration interface, not a compliance certification. The operator remains responsible for selecting appropriate screening policies, providers, record retention, licensing, sanctions controls, and legal review.

## Pricing and usage metering

The included meter implements:

- First **1,000** unique successful settlements in each UTC calendar month: **free**.
- Every successful settlement after the free tier: **$0.001**.
- Verification attempts, denied payments, and failed settlements are not counted.
- Repeated settlement responses with the same transaction identifier are deduplicated.

Inspect current usage:

```sh
curl -fsS https://x402.mimi.money/v1/pricing
```

Example:

```json
{
  "month": "2026-08",
  "settled": 1200,
  "freeTierRemaining": 0,
  "billableSettlements": 200,
  "accruedFeeUsd": 0.2,
  "freeSettlements": 1000,
  "feePerSettlementUsd": 0.001
}
```

State is atomically written to `/var/lib/mimi-x402/usage.json` with mode `0600`. The meter calculates usage and accrued fees; it does not create invoices or collect payment. Connect it to an authenticated billing/reconciliation system before using it as a commercial billing source.

## Operations

### Service commands

```sh
sudo systemctl status mimi-x402.service --no-pager
sudo systemctl restart mimi-x402.service
sudo systemctl stop mimi-x402.service
sudo systemctl start mimi-x402.service
sudo systemctl is-enabled mimi-x402.service
```

### Logs

```sh
sudo journalctl -u mimi-x402.service -n 100 --no-pager
sudo journalctl -u mimi-x402.service -f
sudo tail -f /var/log/apache2/x402.mimi.money_access.log
sudo tail -f /var/log/apache2/x402.mimi.money_error.log
```

Logs contain request paths and request IDs but must never contain `EVM_PRIVATE_KEY` or compliance bearer tokens.

### Safe configuration change

```sh
sudoedit /etc/mimi-x402/facilitator.env
sudo systemctl restart mimi-x402.service
sudo systemctl is-active mimi-x402.service
curl -fsS https://x402.mimi.money/healthz
```

Environment changes are read only during process startup.

### Safe application upgrade

```sh
cd /var/www/x402.mimi.money
npm ci
npm run typecheck
npm test
npm run build
sudo systemctl restart mimi-x402.service
curl -fsS https://x402.mimi.money/healthz
```

Pin and review x402 SDK upgrades. Protocol or package-version changes should be tested against a testnet buyer and resource server before deployment.

### Backup and restore

Back up:

- `/etc/mimi-x402/facilitator.env` through an encrypted secrets backup.
- `/var/lib/mimi-x402/usage.json` for usage reconciliation.
- `/var/www/x402.mimi.money` or the source-control repository.
- Apache and systemd templates if they diverge from the checked-in versions.

Never place unencrypted private keys in ordinary filesystem snapshots or public repositories. Test restore procedures without exposing secrets.

### Monitoring recommendations

- Poll `/healthz` over HTTPS.
- Alert when systemd is not active or restarts repeatedly.
- Monitor Apache 4xx/5xx rates and facilitator error logs.
- Monitor RPC latency, throttling, and chain-ID mismatches.
- Monitor native gas balance independently on every enabled chain.
- Monitor compliance-provider latency and denial/error rates.
- Reconcile successful settlements against chain receipts and the usage meter.
- Alert before TLS certificate expiration even though Certbot renewal is automatic.

## Security model

- The Node service binds to loopback only; Apache is the public boundary.
- TLS is mandatory for public traffic.
- The facilitator uses a dedicated unprivileged Unix account.
- systemd denies privilege escalation, home access, capabilities, and unrelated filesystem writes.
- The private key lives outside the application/web root in a root-only environment file.
- Only `/var/lib/mimi-x402` is writable by the service.
- Each chain has an isolated RPC and wallet client.
- Smart-wallet factory deployment through EIP-6492 is disabled by an empty factory allowlist.
- JSON size limits reduce memory-abuse risk.
- Helmet security headers and request IDs are enabled.
- CORS is intentionally open because facilitator endpoints are protocol infrastructure; do not place administrative endpoints in this application without authentication.

Production hardening still recommended:

- Store or sign through an HSM, KMS, MPC wallet, or isolated signer instead of a plaintext environment key.
- Use dedicated authenticated RPC providers with allowlists and quotas.
- Add edge/WAF rate limiting for both standard and versioned paths.
- Add authenticated metrics that do not expose payment or wallet secrets.
- Establish gas limits, treasury limits, incident response, key rotation, and emergency shutdown procedures.
- Audit token contracts and Permit2 usage offered by resource servers.
- Perform an independent security review before processing material value.

## Testing

Available checks:

```sh
npm run typecheck  # strict TypeScript validation
npm test           # network catalog and pricing-meter tests
npm run build      # production JavaScript build
npm audit --omit=dev
```

Current automated coverage verifies:

- The complete required network catalog and unique chain IDs.
- Monthly free-tier calculations.
- Per-settlement fee calculations.
- Duplicate transaction handling.
- Atomic, valid usage-state persistence.

Recommended pre-release integration checks:

1. Start with only Base Sepolia or Arbitrum Sepolia enabled.
2. Confirm `/supported` through `HTTPFacilitatorClient.getSupported()`.
3. Run a real fixed-price payment from a test payer to a test seller.
4. Confirm verification succeeds and settlement returns a chain transaction.
5. Confirm the seller received the correct token amount.
6. Repeat the same settlement request and confirm it is not double-counted.
7. Simulate compliance denial, timeout, and malformed provider responses.
8. Restart systemd and confirm the usage state persists.

## Troubleshooting

### Service repeatedly restarts

```sh
sudo systemctl status mimi-x402.service --no-pager -l
sudo journalctl -u mimi-x402.service -n 100 --no-pager
```

Common causes are an invalid private-key shape, unsupported `EVM_NETWORKS` value, invalid boolean/number, missing build output, or unreadable environment file.

### `403` from verify or settle

Check `/healthz`. If compliance is `not-configured` and the policy is `fail-closed`, configure `COMPLIANCE_WEBHOOK_URL` or explicitly change the policy after a documented risk decision. If screening is configured, inspect the provider response and facilitator logs.

### Settlement fails after verification

Check:

- The facilitator address has native gas on the selected chain.
- The RPC URL serves the expected chain ID and supports transaction submission.
- The payer authorization is still within its validity window.
- The payer has enough token balance and allowance/Permit2 setup.
- The asset contract and EIP-712 metadata match the selected network.
- The seller's `payTo`, amount, scheme, and network match the signed requirements.

### Apache returns 502

```sh
sudo systemctl is-active mimi-x402.service
curl -fsS http://127.0.0.1:4402/healthz
sudo apache2ctl configtest
sudo journalctl -u mimi-x402.service -n 50 --no-pager
```

### HTTPS or renewal problem

```sh
sudo certbot certificates
sudo certbot renew --dry-run
sudo systemctl status certbot.timer --no-pager
```

Confirm DNS still resolves to this server and ports 80/443 are reachable.

### Wrong or unavailable RPC

Set the appropriate `RPC_<CHAIN_ID>` in `/etc/mimi-x402/facilitator.env`, restart the service, and run a chain-ID check before restoring traffic. Never silently point one CAIP-2 network at another chain's endpoint.

### Pricing count appears wrong

Inspect `/var/lib/mimi-x402/usage.json` and compare stored transaction IDs with successful on-chain receipts. Do not edit the file while the service is running. Stop the service, create a backup, reconcile carefully, then restart.

## Project layout

```text
.
├── src/
│   ├── server.ts          HTTP service, routes, validation, and shutdown
│   ├── facilitator.ts     x402 facilitator and per-chain signer construction
│   ├── config.ts          environment validation and EVM network catalog
│   ├── compliance.ts      external screening integration
│   └── usage.ts           persistent monthly pricing meter
├── public/
│   ├── index.html         MiMi Money X402 website
│   └── assets/            logo, favicon, CSS, and browser JavaScript
├── test/
│   ├── config.test.ts     network catalog tests
│   └── usage.test.ts      pricing and persistence tests
├── data/
│   └── .gitkeep           empty runtime-data directory; generated data is ignored
├── deployment/
│   ├── facilitator.env    production environment template
│   ├── mimi-x402.service  hardened systemd unit
│   └── x402.mimi.money.conf Apache virtual-host template
├── .env.example           local configuration reference
├── package.json           pinned dependencies and scripts
└── tsconfig.json          strict TypeScript build configuration
```

## References

- [x402 documentation](https://docs.x402.org/)
- [x402 protocol and TypeScript SDK](https://github.com/x402-foundation/x402)
- [x402 wallet concepts](https://docs.x402.org/core-concepts/wallet)
- [Cloudflare x402 agent payments](https://developers.cloudflare.com/agents/tools/payments/x402/)
- [Polygon RPC documentation](https://docs.polygon.technology/pos/reference/rpc-endpoints)
- [Mezo developer network configuration](https://mezo.org/docs/developers/getting-started/configure-environment)

## License and responsibility

The upstream x402 SDK is licensed by its respective authors. Apply the appropriate license to this MiMi Money deployment before redistribution.

Operating a payment facilitator can introduce financial, security, privacy, sanctions, consumer-protection, tax, and licensing obligations. This README describes the software behavior; it is not legal, compliance, accounting, or financial advice.
