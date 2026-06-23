#!/bin/bash

# Linux/Mac 环境变量设置脚本

# 设置 Java 环境
export JAVA_HOME=/usr/lib/jvm/java-17
export PATH=$JAVA_HOME/bin:$PATH

# 设置 Maven 环境
export MAVEN_HOME=/opt/maven
export PATH=$MAVEN_HOME/bin:$PATH

# 设置 Nacos 环境
export NACOS_SERVER=127.0.0.1:8848
export NACOS_NAMESPACE=public
export NACOS_GROUP=MICRO_GROUP

# 设置 MySQL 环境
export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=3306
export MYSQL_USER=root
export MYSQL_PASSWORD=root

# 设置 Redis 环境
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_PASSWORD=

# 设置 Sentinel 环境
export SENTINEL_DASHBOARD=127.0.0.1:8181

echo "Environment variables set successfully!"
