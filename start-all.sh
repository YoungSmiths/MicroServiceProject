#!/bin/bash
# Start all microservices

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

LOG_DIR="$SCRIPT_DIR/logs"
LOG_FILE="$LOG_DIR/start-all.log"
mkdir -p "$LOG_DIR"

JAVA_EXE="$(which java 2>/dev/null || echo '/usr/bin/java')"

GATEWAY_JAR="$SCRIPT_DIR/micro-gateway/target/micro-gateway-1.0.0.jar"
USER_JAR="$SCRIPT_DIR/micro-user-service/target/micro-user-service-1.0.0.jar"
ORDER_JAR="$SCRIPT_DIR/micro-order-service/target/micro-order-service-1.0.0.jar"
REDIS_PASSWORD="123456"
SENTINEL_DASHBOARD="127.0.0.1:8181"
SEATA_ENABLED="${SEATA_ENABLED:-false}"
JM_LOG_PATH="$LOG_DIR/nacos-client"
mkdir -p "$JM_LOG_PATH"

echo "========================================"
echo "Starting MicroService Project"
echo "========================================"
echo "[INFO] SEATA_ENABLED = $SEATA_ENABLED"
echo "[INFO] To enable Seata, run: SEATA_ENABLED=true ./start-all.sh"
echo "========================================"
echo "$(date '+%Y-%m-%d %H:%M:%S') start-all.sh begin" > "$LOG_FILE"
echo "[INFO] java executable: $JAVA_EXE" >> "$LOG_FILE"

if [ ! -f "$GATEWAY_JAR" ]; then
    echo "[ERROR] Gateway jar not found: $GATEWAY_JAR"
    echo "[ERROR] Please run build.sh first"
    echo "$(date '+%Y-%m-%d %H:%M:%S') gateway jar missing" >> "$LOG_FILE"
    exit 1
fi

if [ ! -f "$USER_JAR" ]; then
    echo "[ERROR] User Service jar not found: $USER_JAR"
    echo "[ERROR] Please run build.sh first"
    echo "$(date '+%Y-%m-%d %H:%M:%S') user-service jar missing" >> "$LOG_FILE"
    exit 1
fi

if [ ! -f "$ORDER_JAR" ]; then
    echo "[ERROR] Order Service jar not found: $ORDER_JAR"
    echo "[ERROR] Please run build.sh first"
    echo "$(date '+%Y-%m-%d %H:%M:%S') order-service jar missing" >> "$LOG_FILE"
    exit 1
fi

echo "[INFO] Checking Docker..."
if ! docker --version > /dev/null 2>&1; then
    echo "[ERROR] Docker not found or not running"
    echo "$(date '+%Y-%m-%d %H:%M:%S') docker check failed" >> "$LOG_FILE"
    exit 1
fi

echo "[INFO] Starting infrastructure services (Docker)..."
docker-compose up -d >> "$LOG_FILE" 2>&1

echo "[INFO] Waiting for infrastructure to start (15 seconds)..."
sleep 15

echo "[INFO] Starting Gateway..."
echo "$(date '+%Y-%m-%d %H:%M:%S') starting gateway" >> "$LOG_FILE"
nohup "$JAVA_EXE" -jar "$GATEWAY_JAR" > "$LOG_DIR/gateway.log" 2>&1 &

echo "[INFO] Waiting for Gateway to start (5 seconds)..."
sleep 5

echo "[INFO] Starting User Service..."
echo "$(date '+%Y-%m-%d %H:%M:%S') starting user-service" >> "$LOG_FILE"
SEATA_ENABLED="$SEATA_ENABLED" nohup "$JAVA_EXE" -jar "$USER_JAR" > "$LOG_DIR/user-service.log" 2>&1 &

echo "[INFO] Waiting for User Service to start (5 seconds)..."
sleep 5

echo "[INFO] Starting Order Service..."
echo "$(date '+%Y-%m-%d %H:%M:%S') starting order-service" >> "$LOG_FILE"
SEATA_ENABLED="$SEATA_ENABLED" nohup "$JAVA_EXE" -jar "$ORDER_JAR" > "$LOG_DIR/order-service.log" 2>&1 &

echo "========================================"
echo "All services started!"
echo "========================================"
echo "Access URLs:"
echo "  Nacos:     http://127.0.0.1:8848/nacos"
echo "  Sentinel:  http://127.0.0.1:8181"
echo "  Gateway:   http://127.0.0.1:8080"
echo "  Mock Payment: http://127.0.0.1:8080/mock/payment/charge"
echo "========================================"
echo "To stop all services, run: ./stop-all.sh"
echo "========================================"
echo "$(date '+%Y-%m-%d %H:%M:%S') start-all.sh end" >> "$LOG_FILE"
