#!/usr/bin/env bash
set -Eeuo pipefail

[[ ${EUID} -eq 0 ]] || { echo "Run as root after completing Circle login for mimi-support-x402." >&2; exit 1; }
[[ $# -eq 1 && $1 =~ ^0x[0-9a-fA-F]{40}$ ]] || { echo "Usage: $0 0xCircleAgentWalletAddress" >&2; exit 1; }
readonly wallet=$1
readonly env_file=/etc/mimi-support-x402/client.env
readonly backup=/var/backups/agenticous-autonomy/client.env.before-circle.$(date -u +%Y%m%dT%H%M%SZ)

sudo -u mimi-support-x402 env HOME=/var/lib/mimi-support-x402 CIRCLE_ACCEPT_TERMS=1 circle wallet status --type agent --output json >/dev/null
sudo -u mimi-support-x402 env HOME=/var/lib/mimi-support-x402 CIRCLE_ACCEPT_TERMS=1 circle wallet limit budget --address "$wallet" --output json >/dev/null
mkdir -p "$(dirname "$backup")"
cp -a "$env_file" "$backup"
temporary=$(mktemp /tmp/mimi-circle-autonomy.XXXXXX)
trap 'rm -f "$temporary"' EXIT
grep -Ev '^(PAYER_PRIVATE_KEY|CIRCLE_AGENT_WALLET_ADDRESS|CIRCLE_ACCEPT_TERMS|AUTONOMOUS_TRANSFERS_ENABLED|X402_PURCHASES_ENABLED|AUTONOMOUS_ACTIONS_ENABLED|AUTONOMOUS_RECIPIENT_POLICY|AUTONOMOUS_X402_HOST_POLICY)=' "$env_file" > "$temporary"
printf '%s\n' \
  "CIRCLE_AGENT_WALLET_ADDRESS=$wallet" \
  'CIRCLE_ACCEPT_TERMS=1' \
  'AUTONOMOUS_TRANSFERS_ENABLED=true' \
  'AUTONOMOUS_RECIPIENT_POLICY=any' \
  'X402_PURCHASES_ENABLED=true' \
  'AUTONOMOUS_X402_HOST_POLICY=public-internet' \
  'AUTONOMOUS_ACTIONS_ENABLED=true' >> "$temporary"
install -o root -g mimi-support-x402 -m 0640 "$temporary" "$env_file"
systemctl restart mimi-support-agenticous.service
if ! systemctl is-active --quiet mimi-support-agenticous.service; then
  install -o root -g mimi-support-x402 -m 0640 "$backup" "$env_file"
  systemctl restart mimi-support-agenticous.service
  echo "Activation failed and was rolled back." >&2
  exit 1
fi
echo "Circle autonomy activated. Backup: $backup"
