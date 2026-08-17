#!/usr/bin/env bash
set -Eeuo pipefail

[[ ${EUID} -eq 0 ]] || { echo "Run as root." >&2; exit 1; }
readonly source_env=/etc/agenticous/agenticous.env
readonly target_env=/etc/openclaw/mimi-support.env
readonly cli=/opt/openclaw/bin/openclaw
readonly home=/var/lib/openclaw
readonly model=openrouter/google/gemini-3.6-flash
readonly backup_dir=/var/backups/agenticous-autonomy

key=$(sed -n 's/^OPENROUTER_API_KEY=//p' "$source_env" | tail -n 1)
[[ -n $key ]] || { echo "Agenticous AI agent OpenRouter credential is unavailable." >&2; exit 1; }
mkdir -p "$backup_dir" /etc/openclaw
if [[ -f $target_env ]]; then cp -a "$target_env" "$backup_dir/mimi-support.env.before-openrouter.$(date -u +%Y%m%dT%H%M%SZ)"; fi
temporary=$(mktemp /tmp/openclaw-env.XXXXXX)
trap 'rm -f "$temporary"' EXIT
if [[ -f $target_env ]]; then grep -v '^OPENROUTER_API_KEY=' "$target_env" > "$temporary"; fi
printf 'OPENROUTER_API_KEY=%s\n' "$key" >> "$temporary"
install -o root -g openclaw -m 0640 "$temporary" "$target_env"
sudo -u openclaw env HOME="$home" OPENROUTER_API_KEY="$key" "$cli" models set "$model"
sudo -u openclaw env HOME="$home" OPENROUTER_API_KEY="$key" "$cli" config validate
systemctl restart mimi-openclaw.service
systemctl is-active --quiet mimi-openclaw.service
echo "OpenClaw model configured: $model"
