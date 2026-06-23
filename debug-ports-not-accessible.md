[OPEN] Debug Session: ports-not-accessible

Symptom
- Terminal printed:
  - Nacos: http://127.0.0.1:8848/nacos
  - Sentinel: http://127.0.0.1:8181
  - Gateway: http://127.0.0.1:8080
  - Mock Payment: http://127.0.0.1:8080/mock/payment/charge
- Only 8181 is accessible.

Goal
- Make Nacos (8848) and Gateway (8080) reachable from the local machine.

Hypotheses (falsifiable)
1) Nacos/Gateway processes are not running (not listening on the ports).
2) Nacos/Gateway are running but listening on a different port than printed.
3) Ports 8848/8080 are occupied by other processes, preventing the services from binding.
4) Services exited early due to startup errors (e.g., Nacos dependency / config), so the ports never opened.
5) Services are running in Docker/WSL but ports are not mapped to localhost.

Evidence to collect
- Port listeners for 8181/8080/8848 and owning PIDs.
- HTTP reachability to each URL.
- Process list and recent logs for failed services (if not listening).

Notes
- No business logic changes until evidence is collected.

Evidence (collected)
- 8080 NOT LISTENING, 8848 NOT LISTENING, 8181 OK
- docker ps -a showed:
  - nacos Exited (1), sentinel Up
- nacos logs showed embedded derby init failure:
  - load derby-schema.sql error / Login timeout exceeded

Fix (applied)
- Nacos: removed corrupted embedded derby data directory and recreated container
  - Deleted: data/nacos/derby-data
  - docker compose up -d nacos
- Gateway:
  - Ensure reactive mode: set spring.main.web-application-type=reactive
  - Exclude spring-boot-starter-web from micro-common when used by gateway
  - Add lettuce-core explicitly to avoid being excluded by redisson starter, enabling ReactiveStringRedisTemplate and RequestRateLimiter

Verification (post-fix)
- Nacos: http://127.0.0.1:8848/nacos -> 200
- Gateway: http://127.0.0.1:8080 -> 200
- Mock Payment: POST http://127.0.0.1:8080/mock/payment/charge -> 200
