#!/bin/bash
# Stop all microservices

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

LOG_DIR="$SCRIPT_DIR/logs"
LOG_FILE="$LOG_DIR/stop-all.log"
mkdir -p "$LOG_DIR"

echo "========================================"
echo "Stopping MicroService Project"
echo "========================================"
echo "$(date '+%Y-%m-%d %H:%M:%S') stop-all.sh begin" > "$LOG_FILE"

echo "[INFO] Stopping micro-gateway..."
pkill -f "micro-gateway-1.0.0.jar" >> "$LOG_FILE" 2>&1
sleep 2

echo "[INFO] Stopping micro-user-service..."
pkill -f "micro-user-service-1.0.0.jar" >> "$LOG_FILE" 2>&1
sleep 2

echo "[INFO] Stopping micro-order-service..."
pkill -f "micro-order-service-1.0.0.jar" >> "$LOG_FILE" 2>&1
sleep 2

echo "[INFO] Stopping Docker containers..."
docker-compose down >> "$LOG_FILE" 2>&1

echo "========================================"
echo "All services stopped"
echo "========================================"
echo "$(date '+%Y-%m-%d %H:%M:%S') stop-all.sh end" >> "$LOG_FILE"
