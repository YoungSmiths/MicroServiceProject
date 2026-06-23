# Docker 服务部署文档

## 概述

本文档描述了微服务项目所需的基础设施服务的Docker部署方式，包括MySQL、Redis和Nacos。

## 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- 系统内存建议 4GB+

## 快速启动

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f
```

## 服务详情

### 1. MySQL 服务

| 配置项 | 值 |
|--------|-----|
| 镜像 | mysql:8.0 |
| 容器名 | micro-mysql |
| 端口 | 3306 |
| 密码 | root123456 |
| 数据库 | microservice_db |

**连接信息：**
- Host: localhost (或 mysql)
- Port: 3306
- Username: root
- Password: root123456
- Database: microservice_db

**挂载卷：**
- `mysql-data:/var/lib/mysql` - 数据持久化
- `./docker/mysql/init:/docker-entrypoint-initdb.d` - 初始化脚本
- `./docker/mysql/conf/my.cnf:/etc/mysql/conf.d/my.cnf` - 配置文件

---

### 2. Redis 服务

| 配置项 | 值 |
|--------|-----|
| 镜像 | redis:7.2 |
| 容器名 | micro-redis |
| 端口 | 6379 |

**连接信息：**
- Host: localhost (或 redis)
- Port: 6379
- 无密码（仅本地开发使用）

**挂载卷：**
- `redis-data:/data` - 数据持久化
- `./docker/redis/conf/redis.conf:/etc/redis/redis.conf` - 配置文件

---

### 3. Nacos 服务

| 配置项 | 值 |
|--------|-----|
| 镜像 | nacos/nacos-server:v2.2.3 |
| 容器名 | micro-nacos |
| 端口 | 8848, 9848 |
| 运行模式 | standalone |

**访问地址：**
- 控制台: http://localhost:8848/nacos
- 默认用户名: nacos
- 默认密码: nacos

**端口说明：**
- 8848: HTTP端口
- 9848: gRPC端口（Nacos 2.0+）

**挂载卷：**
- `nacos-data:/home/nacos/data` - 数据持久化

---

## 目录结构

```
├── docker-compose.yml          # Docker Compose 配置文件
├── docker/
│   ├── mysql/
│   │   ├── init/
│   │   │   └── 1-init.sql      # MySQL 初始化脚本
│   │   └── conf/
│   │       └── my.cnf          # MySQL 配置文件
│   └── redis/
│       └── conf/
│           └── redis.conf      # Redis 配置文件
```

---

## 数据库表结构

### user 用户表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 用户ID |
| username | VARCHAR(50) | 用户名 |
| password | VARCHAR(100) | 密码 |
| nickname | VARCHAR(50) | 昵称 |
| email | VARCHAR(100) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| status | TINYINT | 状态 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### order 订单表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 订单ID |
| order_no | VARCHAR(50) | 订单编号 |
| user_id | BIGINT | 用户ID |
| total_amount | DECIMAL(10,2) | 订单总额 |
| status | TINYINT | 订单状态 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### order_item 订单明细表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 订单明细ID |
| order_id | BIGINT | 订单ID |
| product_id | BIGINT | 商品ID |
| product_name | VARCHAR(100) | 商品名称 |
| price | DECIMAL(10,2) | 商品单价 |
| quantity | INT | 购买数量 |
| sub_total | DECIMAL(10,2) | 小计金额 |
| create_time | DATETIME | 创建时间 |

---

## 常用命令

```bash
# 启动服务
docker-compose up -d

# 停止服务
docker-compose down

# 停止服务并删除数据卷
docker-compose down -v

# 重启单个服务
docker-compose restart mysql

# 查看服务日志
docker-compose logs -f mysql

# 进入MySQL容器
docker exec -it micro-mysql mysql -uroot -p

# 进入Redis容器
docker exec -it micro-redis redis-cli

# 重新构建并启动
docker-compose up -d --build
```

---

## 微服务配置示例

### application.yml 中的数据库配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/microservice_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root123456
    driver-class-name: com.mysql.cj.jdbc.Driver

  redis:
    host: localhost
    port: 6379

  nacos:
    discovery:
      server-addr: localhost:8848
    config:
      server-addr: localhost:8848
```

---

## 注意事项

1. **数据安全**：生产环境请务必修改默认密码
2. **端口冲突**：确保本地 3306、6379、8848、9848 端口未被占用
3. **性能调优**：可根据服务器配置调整 MySQL 和 Redis 的参数
4. **Nacos**：首次启动可能需要等待约30秒才能完全就绪
