# Resilience4j 功能介绍

Resilience4j 是一个轻量级的容错库，专为 Java 8 和函数式编程设计，是 Hystrix 的替代方案。

## 核心功能模块

### 1. Circuit Breaker（熔断器）

#### 功能说明
熔断器模式用于防止系统在出现故障时继续尝试执行可能失败的操作，从而保护系统免受级联故障的影响。

#### 工作原理
熔断器有三种状态：
- **Closed（关闭）**：正常状态，请求通过
- **Open（打开）**：故障阈值达到，请求直接拒绝，快速失败
- **Half-Open（半打开）**：尝试放行少量请求，检测服务是否恢复

#### 项目中的配置
```yaml
resilience4j:
  circuitbreaker:
    instances:
      thirdPartyPayment:
        sliding-window-type: COUNT_BASED      # 基于计数的滑动窗口
        sliding-window-size: 4                # 窗口大小：4个请求
        minimum-number-of-calls: 2            # 最小请求数：2个
        failure-rate-threshold: 50            # 失败率阈值：50%
        wait-duration-in-open-state: 10s      # 打开状态等待时间：10秒
        permitted-number-of-calls-in-half-open-state: 2  # 半开状态允许的请求数
```

#### 使用场景
- 调用第三方服务（如支付、短信）
- 调用不稳定的远程服务
- 防止级联故障

#### 生产适用性
✅ **非常适合生产环境**  
这是微服务架构中的标准容错模式，广泛使用。

#### 更优方案
- 与 Sentinel 搭配使用（本项目已实现）
- Sentinel 提供更丰富的可视化和动态配置能力


### 2. Bulkhead（舱壁模式）

#### 功能说明
舱壁模式将系统资源隔离成独立的"舱室"，防止一个组件的故障耗尽所有资源。有两种实现方式：
- **Semaphore（信号量）**：限制并发数
- **ThreadPool（线程池）**：使用独立线程池

#### 工作原理（解决什么问题）
- 当某个下游（例如第三方支付）变慢/阻塞时，请求会在调用方侧堆积：线程被占满、连接池被耗尽、GC 压力上升，最终拖垮整个服务。
- 舱壁的目标是：把“对某个依赖的消耗”限制在一个可控范围内，让它最多只影响自己的“舱室”，不会侵蚀到其他业务链路的资源。

#### 两种模式的原理与区别

##### 1) SemaphoreBulkhead（信号量隔离 / 并发隔离）
- **原理**
  - 在进入受保护方法前先获取一个“许可（permit）”，许可数量 = `maxConcurrentCalls`。
  - 获取成功则继续在**当前线程**执行；获取失败则快速失败（或等待 `maxWaitDuration`）。
  - 本质是“限制并发进入临界区”的计数信号量。
- **资源隔离粒度**
  - 只能隔离“并发数”，不能隔离“线程池”。
  - 慢调用依然会占用调用方线程（例如 Tomcat/Netty 工作线程或业务线程池中的线程）。
- **优点**
  - 开销小，实现简单；不会引入线程切换和额外队列。
  - 非常适合保护**短时、可快速失败**的外部依赖。
- **局限 / 风险**
  - 如果被保护的方法是阻塞式 I/O（HTTP/DB）且耗时很长，会持续占用调用方线程；在高并发下仍可能把调用方线程池拖慢。
  - 不能做到“线程级隔离”，只能做到“进入数量限制”。
- **适合场景**
  - 依赖调用是**同步阻塞**但耗时可控，主要担心突发流量把依赖打爆。
  - Reactive/异步链路中，不希望引入额外线程池（更倾向用并发上限做保护）。
  - 作为“第三方依赖的并发阀门”，配合超时（TimeLimiter/HTTP client timeout）和熔断（CircuitBreaker）。

##### 2) ThreadPoolBulkhead（线程池隔离 / 线程隔离）
- **原理**
  - 为某类依赖调用分配一个**独立线程池**（以及可选队列）。
  - 调用被提交到该线程池执行；当线程池/队列满时快速失败（或按策略拒绝/阻塞等待）。
  - 本质是“把执行资源从主线程池搬到隔离线程池”，从而避免依赖阻塞占用主业务线程。
- **资源隔离粒度**
  - 隔离“执行线程”和“排队容量”，可以保护主业务线程池不被慢依赖耗尽。
- **优点**
  - 对阻塞式 I/O 非常有效：第三方慢/卡死时，最多耗尽自己的隔离线程池，不会拖垮主线程池。
  - 更接近 Hystrix 的经典线程隔离思路。
- **局限 / 风险**
  - 引入线程切换、上下文传递成本（如 ThreadLocal、MDC 日志上下文需要额外处理）。
  - 配置不当可能导致线程池过多、资源浪费或排队导致延迟增大。
  - 在纯 Reactor（WebFlux）链路中，如果不慎引入阻塞调用，正确做法通常是先把阻塞调用切到专用 Scheduler，而不是盲目堆线程池。
- **适合场景**
  - 依赖调用是**阻塞且不可控**（第三方、老系统、慢 SQL、文件/IO），且必须保护主线程池。
  - 需要把“慢依赖的排队”限制在自己的队列里，并对外快速拒绝。

##### 选择建议（经验法则）
- **阻塞 I/O 且不可控**：优先 ThreadPoolBulkhead（或至少保证调用在专用线程池执行）+ 超时 + 熔断。
- **不希望引入额外线程池/调用本身较快**：优先 SemaphoreBulkhead + 超时 + 熔断。
- **一定要配超时**：无论哪种 bulkhead，如果没有超时，慢调用都会导致资源长时间被占用，只是“占用在哪里”的区别。

#### 项目中的配置
```yaml
resilience4j:
  bulkhead:
    instances:
      thirdPartyPaymentBulkhead:
        max-concurrent-calls: 2              # 最大并发数：2
        max-wait-duration: 0                 # 等待超时：0（立即拒绝）
```

#### 使用场景
- 第三方服务调用（如本项目的支付接口）
- 慢 SQL 查询
- 外部 API 调用
- 防止慢请求拖垮整个系统

#### 生产适用性
✅ **非常适合生产环境**  
特别是在依赖多个外部服务的场景下，舱壁模式是保护系统稳定性的关键。

#### 更优方案
- 结合线程池隔离和信号量隔离
- 使用 Sentinel 的流量控制和隔离功能


### 3. Rate Limiter（限流器）

#### 功能说明
限流器控制请求的速率，防止系统被过载。支持：
- 固定窗口限流
- 滑动窗口限流
- 令牌桶算法

#### 工作原理
通过限制单位时间内的请求数量，保护系统不被突发流量击垮。

#### 配置示例
```yaml
resilience4j:
  ratelimiter:
    instances:
      myRateLimiter:
        limit-for-period: 100              # 周期内允许的请求数
        limit-refresh-period: 1s           # 刷新周期
        timeout-duration: 0                # 等待超时
```

#### 使用场景
- API 网关限流
- 防止恶意攻击
- 保护下游服务
- 限制内部调用频率

#### 生产适用性
✅ **适合生产环境**  
但在网关层面通常会使用更专业的方案。

#### 更优方案
- 网关层使用 Sentinel 或 Spring Cloud Gateway RequestRateLimiter
- Redis + Lua 实现分布式限流
- 本项目已在网关层实现双层限流（Sentinel + RequestRateLimiter）


### 4. Retry（重试）

#### 功能说明
重试模式在操作失败时自动重试，提高系统的可用性。支持：
- 可配置的重试次数
- 重试间隔
- 指数退避
- 只对特定异常重试

#### 工作原理
当方法抛出异常时，自动重新执行，直到成功或达到最大重试次数。

#### 使用场景
- 网络抖动导致的临时失败
- 第三方服务偶发故障
- 数据库死锁重试
- 幂等操作

#### 生产适用性
✅ **适合生产环境**  
但需要注意：
- 只对幂等操作使用重试
- 避免无限重试
- 配置合理的重试间隔

#### 更优方案
- 结合 Circuit Breaker 使用
- 使用 Spring Retry（更成熟）
- 本项目使用了 `@Retryable` 注解


### 5. Time Limiter（时间限制器）

#### 功能说明
时间限制器设置操作的超时时间，防止长时间阻塞。

#### 工作原理
在指定时间内未完成的操作会被中断。

#### 配置示例
```yaml
resilience4j:
  timelimiter:
    instances:
      myTimeLimiter:
        timeout-duration: 5s               # 超时时间
        cancel-running-future: true        # 是否取消正在运行的任务
```

#### 使用场景
- 外部 API 调用
- 数据库查询
- 防止慢请求占用线程

#### 生产适用性
✅ **适合生产环境**  
超时控制是任何外部调用的标配。

#### 更优方案
- 结合 Hystrix/Resilience4j 的超时控制
- 使用线程池 + Future.get(timeout)
- 配置 HTTP 客户端超时


## 各功能组合使用建议

| 场景 | 推荐组合 |
|------|----------|
| 第三方支付接口 | CircuitBreaker + Bulkhead + TimeLimiter + Retry |
| 内部服务调用 | CircuitBreaker + TimeLimiter |
| 网关限流 | Sentinel + Spring Cloud Gateway RequestRateLimiter |
| 慢 SQL | Bulkhead + TimeLimiter |


## 生产实践总结

| 功能 | 生产常用度 | 推荐替代方案 |
|------|------------|--------------|
| Circuit Breaker | ⭐⭐⭐⭐⭐ | 与 Sentinel 双轨制 |
| Bulkhead | ⭐⭐⭐⭐ | Resilience4j 足够 |
| Rate Limiter | ⭐⭐⭐ | 网关层用 Sentinel |
| Retry | ⭐⭐⭐⭐ | Spring Retry 更成熟 |
| Time Limiter | ⭐⭐⭐⭐ | 配合其他功能使用 |


## 本项目最佳实践

本项目采用了**双层容错架构**：
1. **网关层**：Sentinel + RequestRateLimiter 双层限流
2. **服务层**：Resilience4j CircuitBreaker + Bulkhead 保护第三方调用

这种组合提供了全面的系统保护！
