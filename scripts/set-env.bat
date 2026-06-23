@rem Windows 环境变量设置脚本

@echo off

rem 设置 Java 环境
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

rem 设置 Maven 环境
set MAVEN_HOME=C:\tools\apache-maven-3.9.5
set PATH=%MAVEN_HOME%\bin;%PATH%

rem 设置 Nacos 环境
set NACOS_SERVER=127.0.0.1:8848
set NACOS_NAMESPACE=public
set NACOS_GROUP=MICRO_GROUP

rem 设置 MySQL 环境
set MYSQL_HOST=127.0.0.1
set MYSQL_PORT=3306
set MYSQL_USER=root
set MYSQL_PASSWORD=root

rem 设置 Redis 环境
set REDIS_HOST=127.0.0.1
set REDIS_PORT=6379
set REDIS_PASSWORD=

rem 设置 Sentinel 环境
set SENTINEL_DASHBOARD=127.0.0.1:8181

echo Environment variables set successfully!
