#!/bin/sh
set -eu

control_socket=/run/openresty/control.sock
control_bridge_port=${CONTROL_API_BRIDGE_PORT:-81}
rm -f "$control_socket"

center_id=${RUNTIME_CENTER_ID:-}
control_plane=${RUNTIME_CONTROL_PLANE_URL:-}
generated_config=""
if [ -n "$center_id" ] && [ -n "$control_plane" ]; then
  materialize_url="${control_plane%/}/api/centers/${center_id}/native-configurations/materialize"
  for _ in $(seq 1 30); do
    if curl -fsS -X POST "$materialize_url" >/dev/null 2>&1; then
      generated_config="/etc/openresty/generated/${center_id}/nginx.conf"
      break
    fi
    sleep 1
  done
fi

if [ -n "$generated_config" ] && [ -f "$generated_config" ]; then
  set -- -c "$generated_config" "$@"
else
  echo "Native configuration is unavailable; starting with the bundled bootstrap configuration" >&2
fi

/usr/local/openresty/nginx/sbin/openresty "$@" &
nginx_pid=$!

for _ in $(seq 1 50); do
  [ -S "$control_socket" ] && break
  sleep 0.1
done

if [ ! -S "$control_socket" ]; then
  echo "Control API Unix socket was not created" >&2
  kill "$nginx_pid" 2>/dev/null || true
  exit 1
fi

# The TCP endpoint is only attached to the private container network. Socat is
# deliberately kept outside the Nginx worker model: workers never need access
# to the root-owned Control API socket.
socat TCP-LISTEN:"$control_bridge_port",reuseaddr,fork UNIX-CONNECT:"$control_socket" &
socat_pid=$!

terminate() {
  kill -TERM "$socat_pid" "$nginx_pid" 2>/dev/null || true
}
trap terminate INT TERM

wait "$nginx_pid"
status=$?
kill "$socat_pid" 2>/dev/null || true
wait "$socat_pid" 2>/dev/null || true
exit "$status"
