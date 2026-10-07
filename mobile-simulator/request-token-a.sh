#!/bin/bash

set -e

AM_URL="${AM_URL:-http://am:8080}"

CLIENT_ID="mobile-simulator"
CLIENT_SECRET="mobile-secret"

USERNAME="${USERNAME:-testuser}"
PASSWORD="${PASSWORD:-Password123!}"

echo "Requesting Token A from ForgeRock AM..."

curl -k \
  --resolve am:8080:127.0.0.1 \
  -X POST \
  "${AM_URL}/am/oauth2/access_token" \
  -u "${CLIENT_ID}:${CLIENT_SECRET}" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "username=${USERNAME}" \
  --data-urlencode "password=${PASSWORD}" \
  --data-urlencode "scope=profile" \
  --data-urlencode "auth_chain=ldapService"
