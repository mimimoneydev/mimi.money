#!/usr/bin/env bash
set -euo pipefail

if [[ ${EUID} -ne 0 ]]; then
  echo "Run this deployment as root." >&2
  exit 1
fi

project_dir=/var/www/agenticous.mimi.money
support_dir=/var/www/support.mimi.money
support_client_dir=/opt/mimi-support-agenticous
agent_secret_dir=/etc/agenticous
support_secret_dir=/etc/mimi-support-x402
backup_root=/var/backups/mimi-support-agenticous

if ! id -u agenticous >/dev/null 2>&1; then
  useradd --system --home-dir /nonexistent --shell /usr/sbin/nologin agenticous
fi
if ! id -u mimi-support-x402 >/dev/null 2>&1; then
  useradd --system --home-dir /nonexistent --shell /usr/sbin/nologin mimi-support-x402
fi

install -d -m 0750 -o root -g agenticous "$agent_secret_dir"
install -d -m 0711 -o root -g root "$support_secret_dir"
install -d -m 0750 -o root -g root "$backup_root"
install -d -m 0700 -o mimi-support-x402 -g mimi-support-x402 /var/lib/mimi-support-x402

if [[ ! -s "$agent_secret_dir/seller-wallet.key" ]]; then
  umask 077
  openssl rand -hex 32 | sed 's/^/0x/' > "$agent_secret_dir/seller-wallet.key"
fi
if [[ ! -s "$support_secret_dir/payer-wallet.key" ]]; then
  umask 077
  openssl rand -hex 32 | sed 's/^/0x/' > "$support_secret_dir/payer-wallet.key"
fi
if [[ ! -s "$support_secret_dir/internal.token" ]]; then
  umask 077
  openssl rand -hex 32 > "$support_secret_dir/internal.token"
fi
chmod 0600 "$agent_secret_dir/seller-wallet.key" "$support_secret_dir/payer-wallet.key" "$support_secret_dir/internal.token"

agent_key=$(tr -d '\r\n' < "$agent_secret_dir/seller-wallet.key")
support_key=$(tr -d '\r\n' < "$support_secret_dir/payer-wallet.key")
internal_token=$(tr -d '\r\n' < "$support_secret_dir/internal.token")
openrouter_key=$(sed -n 's/^OPENROUTER_API_KEY=//p' "$project_dir/.env" | tail -n 1)
gemini_key=$(sed -n 's/^GEMINI_API_KEY=//p' "$project_dir/.env" | tail -n 1)
configured_seller_address=$(sed -n 's/^SELLER_ADDRESS=//p' "$project_dir/.env" | tail -n 1)
configured_support_wallet=$(sed -n 's/^CIRCLE_AGENT_WALLET_ADDRESS=//p' "$project_dir/.env" | tail -n 1)
if [[ -z "$openrouter_key" && -z "$gemini_key" ]]; then
  echo "GEMINI_API_KEY and OPENROUTER_API_KEY are both missing from $project_dir/.env" >&2
  exit 1
fi
agent_address=$(cd "$project_dir" && PRIVATE_KEY="$agent_key" node --input-type=module -e 'import {privateKeyToAccount} from "viem/accounts"; process.stdout.write(privateKeyToAccount(process.env.PRIVATE_KEY).address)')
support_address=$(cd "$project_dir" && PRIVATE_KEY="$support_key" node --input-type=module -e 'import {privateKeyToAccount} from "viem/accounts"; process.stdout.write(privateKeyToAccount(process.env.PRIVATE_KEY).address)')
if [[ -n "$configured_seller_address" ]]; then
  if [[ ! "$configured_seller_address" =~ ^0x[0-9a-fA-F]{40}$ ]] || [[ "$configured_seller_address" =~ ^0x0{40}$ ]]; then
    echo "SELLER_ADDRESS in $project_dir/.env must be a non-zero EVM address." >&2
    exit 1
  fi
  agent_address="$configured_seller_address"
fi
if [[ -n "${CIRCLE_AGENTICOUS_WALLET_ADDRESS:-}" ]]; then
  if [[ ! "$CIRCLE_AGENTICOUS_WALLET_ADDRESS" =~ ^0x[0-9a-fA-F]{40}$ ]]; then
    echo "CIRCLE_AGENTICOUS_WALLET_ADDRESS must be a valid EVM address." >&2
    exit 1
  fi
  agent_address="$CIRCLE_AGENTICOUS_WALLET_ADDRESS"
fi
requested_support_wallet=${CIRCLE_AGENT_WALLET_ADDRESS:-$configured_support_wallet}
if [[ -n "$requested_support_wallet" ]]; then
  if [[ ! "$requested_support_wallet" =~ ^0x[0-9a-fA-F]{40}$ ]] || [[ "$requested_support_wallet" =~ ^0x0{40}$ ]] || ! command -v circle >/dev/null 2>&1; then
    echo "Circle Agent Wallet mode requires a valid CIRCLE_AGENT_WALLET_ADDRESS and the Circle CLI." >&2
    exit 1
  fi
  if ! node -e 'const [major, minor] = process.versions.node.split(".").map(Number); process.exit(major > 20 || (major === 20 && minor >= 18) ? 0 : 1)'; then
    echo "Circle Agent Wallet mode requires Node.js 20.18.2 or newer." >&2
    exit 1
  fi
  support_address="$requested_support_wallet"
  support_wallet_setting="CIRCLE_AGENT_WALLET_ADDRESS=$support_address"
else
  support_wallet_setting="PAYER_PRIVATE_KEY=$support_key"
fi

agent_env=$(mktemp /tmp/agenticous-env.XXXXXX)
support_env=$(mktemp /tmp/mimi-support-x402-env.XXXXXX)
support_php=$(mktemp /tmp/mimi-support-x402-php.XXXXXX)
cleanup() {
  rm -f "$agent_env" "$support_env" "$support_php"
}
trap cleanup EXIT
umask 077
printf '%s\n' \
  'HOST=127.0.0.1' \
  'PORT=4410' \
  'TRUST_PROXY=1' \
  'PUBLIC_ORIGIN=https://agenticous.mimi.money' \
  "SELLER_ADDRESS=$agent_address" \
  'FACILITATOR_URL=https://x402.mimi.money' \
  'PAYMENT_NETWORK=eip155:8453' \
  'REPORT_PRICE_USD=0.01' \
  'EXPLORER_TIMEOUT_MS=10000' \
  'REPORT_CACHE_SECONDS=60' \
  'MAXIMUM_PUBLIC_AGENT_SPEND_USD=0.05' \
  'AGENT_RUN_STORE_PATH=/var/lib/agenticous/runs.json' > "$agent_env"
printf '%s\n' \
  "GEMINI_API_KEY=$gemini_key" \
  "OPENROUTER_API_KEY=$openrouter_key" \
  'GEMINI_API_BASE_URL=https://generativelanguage.googleapis.com/v1beta' \
  'GEMINI_LIGHT_MODEL=gemini-2.5-flash-lite' \
  'GEMINI_INTENSE_MODEL=gemini-3.5-flash' \
  'OPENROUTER_LIGHT_MODEL=google/gemini-2.5-flash-lite' \
  'OPENROUTER_INTENSE_MODEL=google/gemini-3.5-flash' \
  'OPENROUTER_PROVIDER=google-vertex/eu' \
  'OPENROUTER_BASE_URL=https://openrouter.ai/api/v1' \
  'GEMINI_TIMEOUT_MS=12000' >> "$agent_env"
if [[ -n "${OPENCLAW_TOKEN:-}" ]]; then
  printf '%s\n' \
    'OPENCLAW_URL=http://127.0.0.1:18789' \
    "OPENCLAW_TOKEN=$OPENCLAW_TOKEN" \
    "OPENCLAW_MODEL=${OPENCLAW_MODEL:-openrouter/google/gemini-3.6-flash}" \
    'OPENCLAW_TIMEOUT_MS=45000' >> "$agent_env"
fi
printf '%s\n' \
  'HOST=127.0.0.1' \
  'PORT=4411' \
  "INTERNAL_TOKEN=$internal_token" \
  "$support_wallet_setting" \
  'CIRCLE_CLI_PATH=/usr/local/bin/circle' \
  'CIRCLE_CHAIN=BASE' \
  'AGENTICOUS_URL=https://agenticous.mimi.money' \
  "AGENTICOUS_SELLER_ADDRESS=$agent_address" \
  'PAYMENT_NETWORK=eip155:8453' \
  'PAYMENT_AMOUNT_ATOMIC=10000' \
  'REQUEST_TIMEOUT_MS=30000' \
  'AUTONOMOUS_TRANSFERS_ENABLED=false' \
  'MAXIMUM_AUTONOMOUS_TRANSFER_USD=0.05' \
  'ALLOWED_TRANSFER_RECIPIENTS=' \
  'AUTONOMOUS_RECIPIENT_POLICY=allowlist' \
  'X402_PURCHASES_ENABLED=false' \
  'MAXIMUM_X402_PURCHASE_USD=0.05' \
  'ALLOWED_X402_HOSTS=' \
  'AUTONOMOUS_X402_HOST_POLICY=allowlist' \
  'AUTONOMOUS_ACTIONS_ENABLED=false' \
  'MAXIMUM_AUTONOMOUS_ACTION_USD=0.05' \
  'MAXIMUM_AUTONOMOUS_DAILY_USD=0.25' \
  'ALLOWED_AUTONOMOUS_CHAINS=BASE' \
  'AUTONOMOUS_CONTRACT_POLICY=allowlist' \
  'ALLOWED_AUTONOMOUS_CONTRACTS=' \
  'AUTONOMY_LEDGER_PATH=/var/lib/mimi-support-x402/autonomy-ledger.json' > "$support_env"
printf '%s\n' \
  'AGENTICOUS_CLIENT_URL="http://127.0.0.1:4411"' \
  "AGENTICOUS_CLIENT_TOKEN=\"$internal_token\"" > "$support_php"

install -m 0640 -o root -g agenticous "$agent_env" "$agent_secret_dir/agenticous.env"
install -m 0640 -o root -g mimi-support-x402 "$support_env" "$support_secret_dir/client.env"
install -m 0640 -o root -g www-data "$support_php" "$support_secret_dir/php.ini"

install -d -m 0755 -o root -g root "$support_client_dir"
cp -a "$project_dir/integration/support-client/dist" "$support_client_dir/"
install -m 0644 "$project_dir/integration/support-client/package.json" "$support_client_dir/package.json"
install -m 0644 "$project_dir/integration/support-client/package-lock.json" "$support_client_dir/package-lock.json"
npm ci --omit=dev --ignore-scripts --prefix "$support_client_dir"
chown -R root:root "$support_client_dir"
find "$support_client_dir" -type d -exec chmod 0755 {} +
find "$support_client_dir" -type f -exec chmod 0644 {} +

# A deployment copy remains under the Support tree for operator visibility.
# Deny it at the web-server boundary; the runtime executes from /opt.
if [[ -d "$support_dir/x402-client" ]]; then
  install -m 0644 "$project_dir/integration/support-client/deployment/apache-deny.htaccess" "$support_dir/x402-client/.htaccess"
fi

backup_dir="$backup_root/$(date -u +%Y%m%dT%H%M%SZ)"
install -d -m 0750 -o root -g root "$backup_dir"
cp -a "$support_dir/application/controllers/api/Ai_support.php" "$backup_dir/Ai_support.php"
if [[ -f "$support_dir/application/config/agenticous.php" ]]; then
  cp -a "$support_dir/application/config/agenticous.php" "$backup_dir/agenticous.php"
fi
install -m 0644 "$project_dir/integration/support/Ai_support.php" "$support_dir/application/controllers/api/Ai_support.php"
install -m 0644 "$project_dir/integration/support/config-agenticous.php" "$support_dir/application/config/agenticous.php"

php "$project_dir/deployment/migrate-support.php"
php -l "$support_dir/application/controllers/api/Ai_support.php"
php -l "$support_dir/application/config/agenticous.php"

install -m 0644 "$project_dir/deployment/agenticous.service" /etc/systemd/system/agenticous.service
install -m 0644 "$project_dir/integration/support-client/deployment/mimi-support-agenticous.service" /etc/systemd/system/mimi-support-agenticous.service
if [[ -f /etc/letsencrypt/live/agenticous.mimi.money/fullchain.pem ]]; then
  install -m 0644 "$project_dir/deployment/agenticous.mimi.money-production.conf" /etc/apache2/sites-available/agenticous.mimi.money.conf
  install -m 0644 "$project_dir/deployment/agenticous.mimi.money-le-ssl.conf" /etc/apache2/sites-available/agenticous.mimi.money-le-ssl.conf
else
  install -m 0644 "$project_dir/deployment/agenticous.mimi.money.conf" /etc/apache2/sites-available/agenticous.mimi.money.conf
fi

systemctl daemon-reload
systemctl enable agenticous.service mimi-support-agenticous.service
systemctl restart agenticous.service mimi-support-agenticous.service
a2ensite agenticous.mimi.money.conf >/dev/null
apache2ctl configtest
systemctl reload apache2

echo "AGENTICOUS_WALLET_ADDRESS=$agent_address"
echo "MIMI_SUPPORT_WALLET_ADDRESS=$support_address"
echo "Support backup: $backup_dir"
