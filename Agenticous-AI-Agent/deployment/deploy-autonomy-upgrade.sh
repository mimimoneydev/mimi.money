#!/usr/bin/env bash
set -Eeuo pipefail

[[ ${EUID} -eq 0 ]] || { echo "Run as root." >&2; exit 1; }

readonly project=/var/www/agenticous.mimi.money
readonly support=/opt/mimi-support-agenticous
readonly support_env=/etc/mimi-support-x402/client.env
readonly agent_env=/etc/agenticous/agenticous.env
readonly backup=/var/backups/agenticous-autonomy/$(date -u +%Y%m%dT%H%M%SZ)

mkdir -p "$backup"
cp -a "$support/dist" "$backup/support-dist"
cp -a "$support_env" "$backup/client.env"
cp -a "$agent_env" "$backup/agenticous.env"
if [[ -d /opt/openclaw/plugins/agenticous-blockchain ]]; then
  cp -a /opt/openclaw/plugins/agenticous-blockchain "$backup/openclaw-plugin"
fi

rollback() {
  trap - ERR
  echo "Deployment failed; restoring application artifacts and configuration." >&2
  cp -a "$backup/support-dist/." "$support/dist/" || true
  install -o root -g mimi-support-x402 -m 0640 "$backup/client.env" "$support_env" || true
  install -o root -g agenticous -m 0640 "$backup/agenticous.env" "$agent_env" || true
  if [[ -d "$backup/openclaw-plugin" ]]; then cp -a "$backup/openclaw-plugin/." /opt/openclaw/plugins/agenticous-blockchain/ || true; fi
  systemctl restart mimi-support-agenticous.service agenticous.service mimi-openclaw.service || true
  echo "Rollback backup: $backup" >&2
}
trap rollback ERR

cp -a "$project/integration/support-client/dist/." "$support/dist/"
append_default() {
  local file=$1 key=$2 value=$3
  grep -q "^${key}=" "$file" || printf '%s=%s\n' "$key" "$value" >> "$file"
}
append_default "$agent_env" MAXIMUM_PUBLIC_AGENT_SPEND_USD 0.05
append_default "$support_env" AUTONOMOUS_RECIPIENT_POLICY allowlist
append_default "$support_env" AUTONOMOUS_X402_HOST_POLICY allowlist
append_default "$support_env" AUTONOMOUS_ACTIONS_ENABLED false
append_default "$support_env" MAXIMUM_AUTONOMOUS_ACTION_USD 0.05
append_default "$support_env" MAXIMUM_AUTONOMOUS_DAILY_USD 0.25
append_default "$support_env" ALLOWED_AUTONOMOUS_CHAINS BASE
append_default "$support_env" AUTONOMOUS_CONTRACT_POLICY allowlist
append_default "$support_env" ALLOWED_AUTONOMOUS_CONTRACTS ''
append_default "$support_env" AUTONOMY_LEDGER_PATH /var/lib/mimi-support-x402/autonomy-ledger.json

"$project/deployment/install-openclaw-plugin.sh"
systemctl restart mimi-support-agenticous.service
systemctl restart mimi-openclaw.service
systemctl restart agenticous.service

for unit in mimi-support-agenticous.service mimi-openclaw.service agenticous.service; do
  systemctl is-active --quiet "$unit"
done
token=$(sed -n 's/^INTERNAL_TOKEN=//p' "$support_env" | tail -n 1)
ready=0
for _attempt in {1..30}; do
  curl -fsS -H "Authorization: Bearer $token" http://127.0.0.1:4411/healthz >/dev/null \
    && curl -fsS http://127.0.0.1:4410/healthz >/dev/null \
    && curl -fsS http://127.0.0.1:18789/health >/dev/null && { ready=1; break; }
  sleep 1
done
[[ $ready -eq 1 ]]
curl -fsS -H "Authorization: Bearer $token" http://127.0.0.1:4411/v1/autonomy/status >/dev/null
curl -fsS https://agenticous.mimi.money/healthz >/dev/null

trap - ERR
echo "Autonomy upgrade deployed. Backup: $backup"
