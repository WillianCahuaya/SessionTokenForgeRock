#!/bin/bash

set -e

PRIVATE_KEY="mobile-simulator/keys/private-key-a.pem"

HEADER='{"alg":"RS256","typ":"JWT"}'

NOW=$(date +%s)
EXP=$((NOW + 3600))

PAYLOAD=$(python3 - <<PY
import json

payload = {
    "iss": "mobile-simulator",
    "sub": "user-001",
    "userId": "U001",
    "iat": $NOW,
    "exp": $EXP
}

print(json.dumps(payload, separators=(",", ":")))
PY
)

base64url() {
    printf '%s' "$1" | base64 | tr -d '\n=' | tr '+/' '-_'
}

HEADER_B64=$(base64url "$HEADER")
PAYLOAD_B64=$(base64url "$PAYLOAD")

UNSIGNED_TOKEN="${HEADER_B64}.${PAYLOAD_B64}"

SIGNATURE=$(printf '%s' "$UNSIGNED_TOKEN" \
    | openssl dgst -sha256 -sign "$PRIVATE_KEY" \
    | base64 \
    | tr -d '\n=' \
    | tr '+/' '-_')

TOKEN="${UNSIGNED_TOKEN}.${SIGNATURE}"

echo
echo "Token A:"
echo
echo "$TOKEN"
echo
echo "Issued at : $(date -r "$NOW")"
echo "Expires at: $(date -r "$EXP")"
echo
