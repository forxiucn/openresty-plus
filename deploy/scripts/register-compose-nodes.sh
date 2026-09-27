#!/usr/bin/env bash
set -euo pipefail

base_url="${CONTROL_PLANE_URL:-http://127.0.0.1:8080}"
center_id="${CENTER_ID:?CENTER_ID is required}"
register_node() {
  local name="$1" port="$2" api_port="$3"
  for _ in $(seq 1 30); do
    if curl -fsS "${base_url}/api/centers/${center_id}/nodes" | grep -q '"name":"'"${name}"'"'; then
      return 0
    fi
    if curl -fsS -X POST "${base_url}/api/centers/${center_id}/nodes" \
      -H 'Content-Type: application/json' \
      -d '{"name":"'"${name}"'","host":"127.0.0.1","servicePort":'"${port}"',"controlApiUrl":"http://127.0.0.1:'"${api_port}"'"}' >/dev/null; then
      return 0
    fi
    sleep 2
  done
  echo "Unable to register ${name}" >&2
  return 1
}

register_node openresty-east-1 18080 18081
