# 代码能力分析文档

本文档按“代码已落地能力”重新梳理当前项目，重点覆盖以下能力：负载均衡、熔断降级、限流、服务隔离、超时重试、第三方调用、分布式锁、分布式事务、分库分表。

## 总览

| 能力 | 当前状态 | 说明 |
| --- | --- | --- |
| 负载均衡 | 已实现 | Gateway + LoadBalancer、OpenFeign 按服务名调用 |
| 熔断降级 | 已实现 | Resilience4j `CircuitBreaker` + Feign fallback |
| 限流 | 已实现 | Gateway `RequestRateLimiter` + Redis 令牌桶 + Sentinel 网关限流 |
| 服务隔离 | 已实现 | Resilience4j `Bulkhead` 信号量隔离 |
| 超时重试 | 已实现 | 第三方支付客户端短超时 + `@Retryable` 自动重试 |
| 第三方调用 | 已实现 | 订单服务通过 HTTP 调用模拟第三方支付平台 |
| 分布式锁 | 已实现 | Redisson 分布式锁已落在订单/用户关键操作 |
| 分布式事务 | 已实现 | Seata AT + `@GlobalTransactional` + 跨服务下单同步场景 |
| 分库分表 | 已实现 | ShardingSphere 双库 16 分表 |

## 1. 本轮补齐的业务场景

为避免能力只停留在依赖和配置层，本轮补了两条可直接验证的场景：

1. 第三方支付治理场景
   - 网关提供本地模拟第三方支付接口 `/mock/payment/charge`
   - 支持 `SUCCESS`、`FAIL`、`TIMEOUT`、`BIZ_FAIL` 四类结果
   - 订单服务支付接口 `/api/order/pay/{orderId}` 发起远程支付
   - 在这条链路中真正落地：第三方调用、超时、重试、熔断、降级、隔离

2. 下单分布式事务场景
   - 订单服务创建订单后，同步调用用户服务更新“用户下单画像”
   - 用户服务维护 `orderCount`、`lastOrderNo`
   - 订单创建接口增加 `simulateDistributedTxFailure`
   - 当该值为 `true` 时，订单服务会在用户侧同步成功后主动抛异常
   - 在 `SEATA_ENABLED=true` 时，可验证跨服务回滚闭环

## 2. 负载均衡

### 体现位置

1. `micro-gateway/pom.xml`
   - 已引入 `spring-cloud-starter-gateway`
   - 已引入 `spring-cloud-starter-loadbalancer`

2. `micro-gateway/src/main/resources/application.yml`
   - 网关路由配置：使用 `lb://` 协议启用服务发现和负载均衡
   - 禁用 Ribbon（使用 Spring Cloud LoadBalancer）
   - 网关自身也注册到 Nacos（`register-enabled: true`）
   - 路由示例：
     ```yaml
     routes:
       - id: user-service
         uri: lb://user-service
         predicates:
           - Path=/api/user/**
     ```

3. `micro-gateway/src/main/java/com/microservice/gateway/config/LoadBalancerConfig.java`
   - 负载均衡策略配置类
   - 用户服务：随机策略（`RandomLoadBalancer`）
   - 订单服务：轮询策略（`RoundRobinLoadBalancer`）
   - 可扩展：可以针对不同服务配置不同策略

4. `micro-order-service/src/main/java/com/microservice/order/OrderServiceApplication.java`
   - 已启用 `@EnableFeignClients`

5. `micro-order-service/src/main/java/com/microservice/order/feign/UserFeignClient.java`
   - 通过 `@FeignClient(name = "user-service", ...)` 按服务名调用用户服务

### 结论

- 负载均衡能力已完整实现：
  - 网关使用 `lb://` 协议 + Nacos 服务发现
  - 负载均衡策略可配置（随机、轮询等）
  - Feign 客户端也具备负载均衡能力
  - 不再写死端口，完全通过服务名调用 

## 3. 熔断降级

### 体现位置

1. `micro-order-service/src/main/java/com/microservice/order/service/ThirdPartyPaymentService.java`
   - 对第三方支付调用增加 `@CircuitBreaker(name = "thirdPartyPayment", fallbackMethod = "paymentFallback")`
   - 当支付平台异常、超时或熔断器已打开时，统一返回降级结果 `DEGRADED`

2. `micro-order-service/src/main/resources/application.yml`
   - 已配置 `resilience4j.circuitbreaker.instances.thirdPartyPayment`
   - 定义了滑动窗口、失败阈值、半开恢复、Open 状态等待时长

3. `micro-order-service/src/main/java/com/microservice/order/feign/UserFeignClientFallback.java`
   - 用户服务调用失败时仍有 Feign fallback 兜底

### 结论

- 熔断和降级都已真正落地，不再只是 Feign fallback 的“半实现”。
- 首次失败会走重试和 fallback，连续失败后熔断器打开，后续请求会直接短路降级。

## 4. 限流

### 体现位置

1. `micro-gateway/src/main/resources/application.yml`
   - `user-service` 路由配置了 `RequestRateLimiter`（第二层）：
     - `redis-rate-limiter.replenishRate: 20`（每秒补充令牌数）
     - `redis-rate-limiter.burstCapacity: 40`（桶容量，允许的最大突发请求数）
   - `order-service` 路由配置了 `RequestRateLimiter`（第二层）：
     - `redis-rate-limiter.replenishRate: 10`
     - `redis-rate-limiter.burstCapacity: 20`
   - 已配置 `spring.cloud.sentinel.transport.dashboard` 和 `spring.cloud.sentinel.scg.fallback`

2. `micro-gateway/src/main/java/com/microservice/gateway/config/RateLimitConfig.java`
   - 提供 `ipKeyResolver`
   - 以来源 IP 作为限流维度

3. `micro-gateway/src/main/java/com/microservice/gateway/config/SentinelConfig.java`（新增）
   - 初始化 Sentinel Gateway 限流规则（第一层）
   - user-service：QPS=30
   - order-service：QPS=15

4. `micro-gateway/pom.xml`
   - 已引入 `spring-boot-starter-data-redis-reactive`
   - 网关限流直接依赖 Redis 令牌桶
   - 已引入 `spring-cloud-starter-alibaba-sentinel`
   - 已引入 `spring-cloud-alibaba-sentinel-gateway`

### 结论

- ✅ 网关限流已实现，并采用“双层限流”：
  - **第一层（动态）**：Sentinel Gateway
    - 默认规则：user-service QPS=30, order-service QPS=15
    - 支持在 Sentinel Dashboard 中动态调整规则，无需重启
    - 命中规则时返回 `{"code":429,"message":"Sentinel 限流，请稍后再试"}`
  - **第二层（静态兜底）**：Gateway `RequestRateLimiter` + Redis 令牌桶
    - user-service：replenishRate=20, burstCapacity=40
    - order-service：replenishRate=10, burstCapacity=20
    - 以来源 IP 作为限流维度
    - 超限时返回 HTTP `429 Too Many Requests`
- 双层限流共同保护网关，提供双重保障

## 5. 服务隔离

### 体现位置

1. `micro-order-service/src/main/java/com/microservice/order/service/ThirdPartyPaymentService.java`
   - 已增加 `@Bulkhead(name = "thirdPartyPaymentBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "paymentFallback")`
   - 作用是把第三方支付访问隔离在独立信号量舱壁内，避免慢请求拖垮订单主链路

2. `micro-order-service/src/main/resources/application.yml`
   - 已配置 `resilience4j.bulkhead.instances.thirdPartyPaymentBulkhead.max-concurrent-calls: 2`
   - 已配置 `max-wait-duration: 0`

### 结论

- 服务隔离已落地。
- 当并发支付请求过多时，超出舱壁容量的请求会立即降级，而不是无限堆积。

## 6. 超时重试

### 体现位置

1. `micro-order-service/src/main/java/com/microservice/order/config/PaymentClientConfig.java`
   - 第三方支付专用 `RestTemplate` 配置了 `connectTimeout=1000ms`
   - 配置了 `readTimeout=1200ms`

2. `micro-order-service/src/main/java/com/microservice/order/integration/ThirdPartyPaymentClient.java`
   - 使用 `@Retryable(include = Exception.class, ...)`
   - 连续失败会自动重试
   - 每次请求会记录当前是第几次尝试

3. `micro-gateway/src/main/java/com/microservice/gateway/controller/MockPaymentController.java`
   - `TIMEOUT` 场景会主动 `sleep 2500ms`
   - 可直接触发订单服务的超时和重试逻辑

### 结论

- 超时和重试都已真正落地在业务调用层，而不是只存在基础组件配置。

## 7. 第三方调用

### 体现位置

1. `micro-gateway/src/main/java/com/microservice/gateway/controller/MockPaymentController.java`
   - 提供可控的模拟第三方支付平台

2. `micro-order-service/src/main/java/com/microservice/order/integration/ThirdPartyPaymentClient.java`
   - 订单服务通过 HTTP 调用 `/mock/payment/charge`
   - 请求对象为 `ThirdPartyPaymentRequest`
   - 响应对象为 `ThirdPartyPaymentResponse`

3. `micro-order-service/src/main/java/com/microservice/order/service/impl/OrderServiceImpl.java`
   - 支付订单时通过 `thirdPartyPaymentService.charge(...)` 发起真实远程调用

### 结论

- 项目中已经存在真实的第三方平台调用链路，不再只是提供 `RestTemplate` Bean。

## 8. 分布式锁

### 体现位置

1. `micro-common/src/main/java/com/microservice/common/config/DistributedLock.java`
   - 封装了 Redisson 锁能力

2. `micro-order-service/src/main/java/com/microservice/order/service/impl/OrderServiceImpl.java`
   - 创建订单按用户维度加锁
   - 取消订单、支付订单、退款订单按订单维度加锁

3. `micro-user-service/src/main/java/com/microservice/user/service/impl/UserServiceImpl.java`
   - 注册、更新、删除用户时加锁
   - 同步用户下单画像时按用户维度加锁

### 结论

- 分布式锁不仅存在于工具类，还已落在多个关键写操作链路。

## 9. 分布式事务

### 体现位置

1. `micro-order-service/src/main/java/com/microservice/order/service/impl/OrderServiceImpl.java`
   - `createOrder(...)` 已增加 `@GlobalTransactional(name = "create-order-tx", rollbackFor = Exception.class)`
   - 这是明确的全局事务发起点

2. `micro-order-service/src/main/java/com/microservice/order/service/impl/OrderServiceImpl.java`
   - 本地保存订单、订单项后，调用用户服务同步用户下单画像
   - `simulateDistributedTxFailure=true` 时，会在远程成功后主动抛出异常

3. `micro-user-service/src/main/java/com/microservice/user/service/impl/UserServiceImpl.java`
   - `syncOrderProfile(...)` 会更新用户画像字段 `orderCount`、`lastOrderNo`
   - 这是全局事务中的用户服务分支操作

4. `scripts/init-db0.sql`
5. `scripts/init-db1.sql`
6. `scripts/init-db2.sql`
7. `scripts/init-db3.sql`
   - 四个业务库都已补齐 `undo_log`

8. `scripts/init-seata.sql`
   - 已修正并补齐 Seata Server 元数据表脚本

### 结论

- 分布式事务已从“只接了 Seata 依赖和配置”升级为“有真实业务编排和发起点”的完整实现。
- 验证该能力时，需要启动 `seata-server`，并设置环境变量 `SEATA_ENABLED=true`。

## 10. 分库分表

### 体现位置

1. `micro-order-service/src/main/resources/application.yml`
   - 订单服务配置 `ds0`、`ds1`
   - `t_order` 和 `t_order_item` 都按 16 分表路由

2. `micro-user-service/src/main/resources/application.yml`
   - 用户服务配置 `ds0`、`ds1`
   - `t_user` 按 16 分表路由

3. `docker-compose.yml`
   - 已编排 4 个 MySQL 实例，和分库分表配置一一对应

### 结论

- 分库分表能力已完整实现，并且和数据库部署方式保持一致。

## 11. 双保险架构：Sentinel + Resilience4j

### 架构设计

本项目采用 **"网关层 Sentinel + 服务层 Resilience4j"** 的双保险架构，充分发挥两者优势：

| 层级 | 组件 | 职责 | 优势 |
| --- | --- | --- | --- |
| **网关层** | Sentinel | 入口限流、热点参数限流、系统自适应保护 | 动态规则实时生效，无需重启服务；可视化监控面板 |
| **服务层** | Resilience4j | 熔断降级、服务隔离、内部接口保护 | 静态配置更稳定，代码级控制更精细 |

### 体现位置

1. **网关层 Sentinel**：
   - `micro-gateway/pom.xml`：引入 `spring-cloud-starter-alibaba-sentinel` 和 `spring-cloud-alibaba-sentinel-gateway`
   - `micro-gateway/src/main/resources/application.yml`：在 `spring.cloud.sentinel` 下配置 Dashboard 连接、客户端端口和 `scg` 降级返回
   - 网关启动后会向 Sentinel Dashboard 注册，应用名显示为 `micro-gateway`
   - 使用方式：在 Sentinel Dashboard（默认 `http://127.0.0.1:8181`）中直接配置网关流控规则，实时生效

2. **服务层 Resilience4j**：
   - `micro-order-service/pom.xml`：引入 `resilience4j-spring-boot2`
   - `micro-order-service/src/main/java/com/microservice/order/service/ThirdPartyPaymentService.java`：`@CircuitBreaker` 和 `@Bulkhead` 注解使用
   - `micro-order-service/src/main/resources/application.yml`：配置熔断和舱壁参数

### 同时配置两个怎么办？

执行顺序按调用链叠加生效：
```
请求 → Sentinel 限流 → Gateway RequestRateLimiter → Resilience4j 熔断/隔离 → 业务逻辑
```

- **Sentinel**：网关入口第一关，按 QPS、热点参数等维度拦截
- **Gateway RequestRateLimiter**：Redis 令牌桶限流，作为 Sentinel 的补充
- **Resilience4j**：服务内部最后一道防线，按接口/资源维度熔断和隔离

### 验证 Sentinel

1. 确保 Sentinel Dashboard 运行（docker-compose 中已有 sentinel-dashboard，端口 8181）
2. 启动网关服务，确认 `micro-gateway` 已在控制台应用列表中出现
3. 若页面未立即显示，先访问一次网关接口，例如 `GET /api/user/hello` 产生流量
4. 在 Dashboard 中选择 `micro-gateway` 应用，配置流控规则（例如 QPS=5）
5. 快速连续访问接口，验证 Sentinel 规则是否生效

### 接入注意事项

1. `micro-gateway/src/main/resources/application.yml` 中，Sentinel 配置必须位于同一个 `spring.cloud` 根节点下
2. 若错误地把 `spring:` 拆成两段，`spring.cloud.sentinel.transport.dashboard` 虽然写在文件里，但运行时会读取不到，表现为：
   - 网关进程能启动
   - Dashboard 中看不到 `micro-gateway`
   - Sentinel 客户端端口与 dashboard 地址为 `null`
3. 正确配置后，网关会监听业务端口 `8080` 和 Sentinel 客户端端口 `8719`

### 生产最佳实践

1. **Sentinel 规则持久化**：推荐将 Sentinel 规则存储在 Nacos 中，避免重启后规则丢失（配置已在 application.yml 中注释，按需启用）
2. **分层控制**：
   - 网关层：控制整体入口流量
   - 服务层：保护核心业务接口
3. **监控告警**：利用 Sentinel Dashboard 观察流量趋势，及时调整规则
4. **命名一致性**：控制台中展示的应用名以 `spring.application.name` 为准，本项目网关应用名为 `micro-gateway`

## 12. 验证方式

### 1. 验证第三方调用、超时重试、熔断降级、服务隔离

1. 先创建订单
   - `POST /api/order/create`

2. 支付成功场景
   - `PUT /api/order/pay/{orderId}`
   - body: `{"scenario":"SUCCESS"}`

3. 第三方 500 场景
   - body: `{"scenario":"FAIL"}`
   - 观察自动重试和降级返回

4. 第三方超时场景
   - body: `{"scenario":"TIMEOUT"}`
   - 观察超时、自动重试和 fallback

5. 第三方业务失败场景
   - body: `{"scenario":"BIZ_FAIL"}`
   - 不触发重试，直接返回业务失败

6. 熔断器打开验证
   - 连续多次调用 `FAIL` 或 `TIMEOUT`
   - 再调用一次 `SUCCESS`
   - 若熔断器处于 Open 状态，会直接得到 `DEGRADED`

### 2. 验证分布式事务

1. 启动 Seata
   - `docker compose up -d seata-server`
   - 设置 `SEATA_ENABLED=true`

2. 调用创建订单接口
   - `POST /api/order/create`
   - body 中加 `simulateDistributedTxFailure: true`

3. 预期结果
   - 订单服务会在用户侧画像同步成功后主动抛出异常
   - Seata 开启时，订单库和用户库都应回滚
   - 若关闭 Seata，这个场景会出现“远程已成功、本地回滚”的数据不一致，可直观看到全局事务的价值

## 13. 当前结论

当前项目中以下能力均已在代码层真正落地：

- 负载均衡
- 熔断降级
- 限流
- 服务隔离
- 超时重试
- 第三方调用
- 分布式锁
- 分布式事务
- 分库分表

其中最关键的变化是：

- 不再只有依赖和配置，已经补出真实业务入口和可回放场景
- 文档中的每项能力都能对应到明确代码文件和验证方式
