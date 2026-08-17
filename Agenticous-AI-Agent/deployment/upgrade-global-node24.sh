#!/usr/bin/env bash
set -Eeuo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "This script must run as root." >&2
  exit 1
fi

readonly SOURCE_RUNTIME="/opt/openclaw/tools/node-v24.15.0"
readonly TARGET_ROOT="/opt/nodejs"
readonly TARGET_RUNTIME="${TARGET_ROOT}/node-v24.15.0"
readonly TARGET_LINK="${TARGET_ROOT}/current"
readonly BACKUP_ROOT="/var/backups/node-runtime-upgrade"
readonly RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)"
readonly BACKUP_DIR="${BACKUP_ROOT}/${RUN_ID}"
readonly EXPECTED_VERSION="v24.15.0"

readonly -a BINARIES=(node npm npx corepack)
readonly -a UNITS=(
  agenticous.service
  mimi-x402.service
  mimi-support-agenticous.service
  mimichat-socket.service
  peers-p2p.service
  pm2-root.service
)
readonly -a NODE_UNITS=(
  agenticous.service
  mimi-x402.service
  mimi-support-agenticous.service
  mimichat-socket.service
  peers-p2p.service
)

declare -a ACTIVE_UNITS=()
declare -a CREATED_LINKS=()
runtime_created=0
units_changed=0
cutover_started=0

rollback() {
  local exit_code=$?
  trap - ERR
  if [[ "${cutover_started}" -eq 1 ]]; then
    echo "Upgrade failed; restoring the previous Node service configuration." >&2
    if [[ "${units_changed}" -eq 1 ]]; then
      for unit in "${UNITS[@]}"; do
        if [[ -f "${BACKUP_DIR}/${unit}" ]]; then
          install -o root -g root -m 0644 "${BACKUP_DIR}/${unit}" "/etc/systemd/system/${unit}"
        fi
      done
      systemctl daemon-reload || true
    fi
    for binary in "${CREATED_LINKS[@]}"; do
      unlink "/usr/local/bin/${binary}" || true
    done
    for unit in "${ACTIVE_UNITS[@]}"; do
      systemctl restart "${unit}" || true
    done
  fi
  echo "Rollback data: ${BACKUP_DIR}" >&2
  exit "${exit_code}"
}
trap rollback ERR

if [[ ! -x "${SOURCE_RUNTIME}/bin/node" ]]; then
  echo "Verified Node runtime is missing: ${SOURCE_RUNTIME}" >&2
  exit 1
fi
if [[ "$("${SOURCE_RUNTIME}/bin/node" --version)" != "${EXPECTED_VERSION}" ]]; then
  echo "Unexpected source Node version." >&2
  exit 1
fi

mkdir -p "${BACKUP_DIR}/metadata" "${TARGET_ROOT}"
{
  echo "date=${RUN_ID}"
  echo "old_node=$(/usr/bin/node --version)"
  echo "new_node=${EXPECTED_VERSION}"
} > "${BACKUP_DIR}/metadata/versions"

for unit in "${UNITS[@]}"; do
  if [[ -f "/etc/systemd/system/${unit}" ]]; then
    cp -a "/etc/systemd/system/${unit}" "${BACKUP_DIR}/${unit}"
  fi
  if systemctl is-active --quiet "${unit}"; then
    ACTIVE_UNITS+=("${unit}")
  fi
done

for binary in "${BINARIES[@]}"; do
  if [[ -e "/usr/local/bin/${binary}" || -L "/usr/local/bin/${binary}" ]]; then
    echo "Refusing to replace existing /usr/local/bin/${binary}." >&2
    exit 1
  fi
done

if [[ ! -d "${TARGET_RUNTIME}" ]]; then
  staging_runtime="${TARGET_ROOT}/.node-v24.15.0-${RUN_ID}"
  cp -a "${SOURCE_RUNTIME}" "${staging_runtime}"
  "${staging_runtime}/bin/node" --version
  mv "${staging_runtime}" "${TARGET_RUNTIME}"
  runtime_created=1
fi

ln -sfn "${TARGET_RUNTIME}" "${TARGET_LINK}"
for binary in "${BINARIES[@]}"; do
  ln -s "${TARGET_LINK}/bin/${binary}" "/usr/local/bin/${binary}"
  CREATED_LINKS+=("${binary}")
done

cutover_started=1
for unit in "${NODE_UNITS[@]}"; do
  unit_path="/etc/systemd/system/${unit}"
  if [[ ! -f "${unit_path}" ]]; then
    echo "Missing expected unit: ${unit_path}" >&2
    false
  fi
  sed -i 's#ExecStart=/usr/bin/node #ExecStart=/usr/local/bin/node #' "${unit_path}"
  if ! grep -q '^ExecStart=/usr/local/bin/node ' "${unit_path}"; then
    echo "Failed to update ${unit}." >&2
    false
  fi
done
units_changed=1

systemctl daemon-reload
for unit in "${ACTIVE_UNITS[@]}"; do
  systemctl restart "${unit}"
  systemctl is-active --quiet "${unit}"
done

[[ "$(/usr/local/bin/node --version)" == "${EXPECTED_VERSION}" ]]
[[ "$(PATH=/usr/local/bin:/usr/bin command node --version)" == "${EXPECTED_VERSION}" ]]

declare -A HEALTH_URLS=(
  [mimi-x402.service]="http://127.0.0.1:4402/healthz"
  [agenticous.service]="http://127.0.0.1:4410/healthz"
  [mimichat-socket.service]="https://127.0.0.1:9001/health"
)
for unit in "${ACTIVE_UNITS[@]}"; do
  if [[ -n "${HEALTH_URLS[${unit}]:-}" ]]; then
    ready=0
    for _attempt in {1..30}; do
      if curl --insecure --fail --silent --max-time 3 "${HEALTH_URLS[${unit}]}" >/dev/null; then
        ready=1
        break
      fi
      sleep 1
    done
    if [[ "${ready}" -ne 1 ]]; then
      echo "Readiness check failed for ${unit}." >&2
      false
    fi
  fi
done

for unit in "${NODE_UNITS[@]}"; do
  if systemctl is-active --quiet "${unit}"; then
    main_pid="$(systemctl show --property MainPID --value "${unit}")"
    executable="$(readlink -f "/proc/${main_pid}/exe")"
    if [[ "${executable}" != "${TARGET_RUNTIME}/bin/node" ]]; then
      echo "${unit} is running an unexpected executable: ${executable}" >&2
      false
    fi
  fi
done

trap - ERR
echo "Global Node upgrade completed: $(/usr/local/bin/node --version)"
echo "Backup and rollback data: ${BACKUP_DIR}"
echo "Active services verified: ${ACTIVE_UNITS[*]:-none}"
