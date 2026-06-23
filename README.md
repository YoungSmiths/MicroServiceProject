# 微服务项目技术选型文档

## 项目概述
基于 Java + Spring Cloud 的高可用、高并发、负载均衡微服务架构模板，支持分布式锁、分布式事务、分库分表。

## 技术选型

### 1. 核心框架
| 组件 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| Spring Boot | Spring Boot | 2.7.18 | 基础框架 |
| Spring Cloud | Spring Cloud Alibaba | 2021.0.5.0 | 微服务生态 |
| Java | JDK | 17 | LTS 版本 |

### 2. 服务注册与配置中心
| 组件 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| Nacos | Alibaba Nacos | 2.2.3 | 服务注册、配置管理 |

### 3. 网关
| 组件 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| Spring Cloud Gateway | Gateway | 3.1.5 | 异步非阻塞网关 |

### 4. 负载均衡
| 组件 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| Spring Cloud LoadBalancer | LoadBalancer | 3.1.5 | 内置负载均衡器 |

### 5. 分布式锁
| 组件 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| Redisson | Redisson | 3.23.5 | Redis 客户端，支持分布式锁 |
| Redis | Redis | 7.0 | 缓存、分布式锁存储 |

### 6. 分布式事务
| 组件 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| Seata | Seata | 1.7.0 | AT 模式分布式事务 |

### 7. 分库分表
| 组件 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| ShardingSphere | ShardingSphere-JDBC | 5.3.2 | 分库分表中间件 |

### 8. 数据库
| 组件 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| MySQL | MySQL | 8.0 | 关系型数据库 |
| Druid | Druid | 1.2.18 | 数据库连接池 |

### 9. 链路追踪
| 组件 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| Sentinel | Sentinel | 1.8.6 | 流量控制、熔断降级 |

### 10. 日志
| 组件 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| Logback | Logback | 1.2.12 | 日志框架 |

---

## 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         用户请求                                  │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Cloud Gateway                          │
│                    (负载均衡、路由、限流)                           │
└─────────────────────────────────────────────────────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
┌─────────────────────────┐         ┌─────────────────────────┐
│      用户服务模块         │         │      订单服务模块         │
│    (user-service)       │         │   (order-service)       │
│  ┌─────────────────┐   │         │  ┌─────────────────┐   │
│  │  分布式锁        │   │         │  │  分布式事务       │   │
│  │  (Redisson)     │   │         │  │  (Seata AT)     │   │
│  └─────────────────┘   │         │  └─────────────────┘   │
└─────────────────────────┘         └─────────────────────────┘
                    │                           │
                    └─────────────┬─────────────┘
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                    ShardingSphere-JDBC                           │
│                    (分库分表中间件)                                │
└─────────────────────────────────────────────────────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
          ┌─────────────────┐         ┌─────────────────┐
          │   MySQL DB0     │         │   MySQL DB1     │
          │  (用户表分片)     │         │  (订单表分片)     │
          └─────────────────┘         └─────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                         Nacos                                    │
│              (服务注册中心 + 配置中心)                             │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                         Redis                                   │
│                    (缓存 + 分布式锁)                              │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                         Seata Server                            │
│                      (分布式事务协调)                             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 模块结构

```
MicroServiceProject/
├── pom.xml                          # 父工程
├── README.md                        # 技术选型文档
├── config/                          # 配置文件目录
│   ├── nacos-config.yaml           # Nacos 配置
│   └── seata-config.yaml           # Seata 配置
├── docker-compose.yml               # Docker 部署文件
├── scripts/                         # SQL 脚本
│   ├── init-db0.sql                 # 数据库0初始化
│   └── init-db1.sql                 # 数据库1初始化
├── micro-common/                    # 公共模块
│   └── pom.xml
├── micro-gateway/                   # 网关模块
│   └── pom.xml
├── micro-user-service/              # 用户服务
│   └── pom.xml
└── micro-order-service/             # 订单服务
    └── pom.xml
```

---

## 核心技术详解

### 1. 高可用架构
- 服务集群部署，无单点故障
- Nacos 注册中心集群
- Redis 哨兵模式
- MySQL 主从复制

### 2. 高并发架构
- 网关层限流 (Gateway RequestRateLimiter + Redis)
- 异步非阻塞响应
- Redis 缓存
- 数据库连接池优化

### 3. 负载均衡
- Spring Cloud LoadBalancer 客户端负载均衡
- Gateway 路由层负载均衡
- 支持权重路由、服务熔断

### 4. 熔断降级
- Resilience4j CircuitBreaker + Bulkhead
- Feign 降级策略
- 第三方调用超时/重试/熔断/隔离

### 5. 分布式锁
- Redisson 实现
- 支持可重入锁、读锁、写锁
- 锁自动续期机制
- 公平锁/非公平锁

### 6. 分布式事务
- Seata AT 模式
- 一站式分布式事务解决方案
- 自动回滚异常事务
- TCC/Saga 模式可扩展

### 7. 分库分表
- ShardingSphere-JDBC
- 分片键选择策略
- 分片算法 (范围/哈希/时间)
- 读写分离支持

---

## 环境要求

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose
- Redis 7.0+
- MySQL 8.0+

---

## 快速启动

### 0. 编译项目
```bash
# Windows
build.bat

# Linux/Mac
chmod +x build.sh
./build.sh
```

### 1. 一键启动所有服务
```bash
# Windows
start-all.bat

# Linux/Mac
chmod +x start-all.sh
./start-all.sh

# 启用 Seata（可选）
# Windows: set SEATA_ENABLED=true && start-all.bat
# Linux/Mac: SEATA_ENABLED=true ./start-all.sh
```

### 2. 一键停止所有服务
```bash
# Windows
stop-all.bat

# Linux/Mac
chmod +x stop-all.sh
./stop-all.sh
```

### 3. 访问地址
启动后可以访问以下地址：
- Nacos: http://127.0.0.1:8848/nacos
- Sentinel: http://127.0.0.1:8181
- Gateway: http://127.0.0.1:8080
- Mock Payment: http://127.0.0.1:8080/mock/payment/charge

### 4. 手工启动（可选）
如果需要手工启动单个组件，可以参考以下命令：
```bash
# 启动基础设施
docker-compose up -d

# 启动各微服务
java -jar micro-gateway/target/micro-gateway-1.0.0.jar
java -jar micro-user-service/target/micro-user-service-1.0.0.jar
java -jar micro-order-service/target/micro-order-service-1.0.0.jar
```

---

## 脚本说明

| 脚本 | 说明 |
|------|------|
| `build.bat` / `build.sh` | 编译整个项目，打包所有模块 |
| `start-all.bat` / `start-all.sh` | 一键启动 Docker 基础设施 + 三个微服务 |
| `stop-all.bat` / `stop-all.sh` | 一键停止三个微服务 + Docker 基础设施 |

## 日志文件
所有服务日志会保存在项目根目录的 `logs/` 目录下：
- `start-all.log` / `stop-all.log`：启动/停止脚本自身的日志
- `gateway.log`：网关服务日志
- `user-service.log`：用户服务日志
- `order-service.log`：订单服务日志
