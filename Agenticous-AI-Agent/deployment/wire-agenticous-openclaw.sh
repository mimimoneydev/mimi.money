#!/usr/bin/env bash
set -Eeuo pipefail

[[ ${EUID} -eq 0 ]] || { echo "Run as root." >&2; exit 1; }
readonly config=/var/lib/openclaw/.openclaw/openclaw.json
readonly agent_env=/etc/agenticous/agenticous.env
readonly backup=/var/backups/agenticous-autonomy/agenticous.env.before-openclaw.$(date -u +%Y%m%dT%H%M%SZ)

[[ -r "$config" ]] || { echo "OpenClaw configuration is unavailable." >&2; exit 1; }
token=$(/usr/local/bin/node -e 'const fs=require("fs");const c=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));const t=c?.gateway?.auth?.token;if(typeof t!=="string"||t.length<32)process.exit(1);process.stdout.write(t)' "$config")
[[ ${#token} -ge 32 ]] || { echo "OpenClaw gateway token is unavailable." >&2; exit 1; }

mkdir -p "$(dirname "$backup")"
cp -a "$agent_env" "$backup"
temporary=$(mktemp /tmp/agenticous-openclaw-env.XXXXXX)
trap 'rm -f "$temporary"' EXIT
grep -v '^OPENCLAW_' "$agent_env" > "$temporary"
printf '%s\n' 'OPENCLAW_URL=http://127.0.0.1:18789' "OPENCLAW_TOKEN=$token" 'OPENCLAW_MODEL=openrouter/google/gemini-3.6-flash' 'OPENCLAW_TIMEOUT_MS=45000' >> "$temporary"
install -o root -g agenticous -m 0640 "$temporary" "$agent_env"
systemctl restart agenticous.service
for _attempt in {1..30}; do
  if curl -fsS http://127.0.0.1:4410/healthz | grep -q '"configured":true'; then
    echo "The Agenticous AI agent is connected to OpenClaw. Backup: $backup"
    exit 0
  fi
  sleep 1
done
install -o root -g agenticous -m 0640 "$backup" "$agent_env"
systemctl restart agenticous.service
echo "OpenClaw wiring failed and was rolled back." >&2
exit 1
