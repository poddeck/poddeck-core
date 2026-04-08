#!/bin/sh
set -e

if [ -z "$AUTH_KEY" ]; then
  echo "ERROR: AUTH_KEY environment variable is required (32-character secret for JWT signing)"
  exit 1
fi

if [ -z "$REFRESH_KEY" ]; then
  echo "ERROR: REFRESH_KEY environment variable is required (32-character secret for JWT refresh signing)"
  exit 1
fi

cat > config.ini <<EOF
[database]
hostname = ${DB_HOST:-localhost}
port = ${DB_PORT:-5432}
username = ${DB_USERNAME:-poddeck}
password = ${DB_PASSWORD:-changeme}
database = ${DB_DATABASE:-poddeck}

[communication]
port = ${GRPC_PORT:-10101}
host = ${GRPC_HOST:-localhost}

[api]
port = ${API_PORT:-8080}
authentication_key = ${AUTH_KEY}
refresh_key = ${REFRESH_KEY}
allowed_origins = ${ALLOWED_ORIGINS:-http://localhost}
EOF

exec java -jar core.jar
