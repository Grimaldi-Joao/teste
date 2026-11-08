#!/usr/bin/env sh
set -eu

BASE_URL="${1:-http://localhost:8080}"
curl --fail --silent --show-error --request POST \
  "$BASE_URL/avaliacao/executar" \
  --output evaluation-report.json

echo "Relatório gerado em evaluation-report.json"
cat evaluation-report.json
