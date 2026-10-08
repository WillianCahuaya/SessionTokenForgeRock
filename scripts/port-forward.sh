#!/bin/bash

set -e

echo "Starting ForgeRock port-forwards..."

kubectl port-forward svc/am 8080:80 -n forgerock &
AM_PID=$!

kubectl port-forward svc/ig 8081:80 -n forgerock &
IG_PID=$!

cleanup() {
  echo
  echo "Stopping port-forwards..."
  kill "$AM_PID" "$IG_PID" 2>/dev/null || true
}

trap cleanup INT TERM EXIT

echo
echo "AM: http://localhost:8080  -> svc/am:80"
echo "IG: http://localhost:8081  -> svc/ig:80"
echo
echo "Press Ctrl+C to stop both."
echo

wait
