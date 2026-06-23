-- 用户库1初始化脚本
CREATE DATABASE IF NOT EXISTS user_db1 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE user_db1;

-- 创建用户表（16个分片）
CREATE TABLE IF NOT EXISTS t_user_0 (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    gender TINYINT DEFAULT 0 COMMENT '性别: 0-女, 1-男',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    order_count INT DEFAULT 0 COMMENT '累计下单次数',
    last_order_no VARCHAR(50) COMMENT '最近一次下单编号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志: 0-未删除, 1-已删除',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS t_user_1 LIKE t_user_0;
CREATE TABLE IF NOT EXISTS t_user_2 LIKE t_user_0;
CREATE TABLE IF NOT EXISTS t_user_3 LIKE t_user_0;
CREATE TABLE IF NOT EXISTS t_user_4 LIKE t_user_0;
CREATE TABLE IF NOT EXISTS t_user_5 LIKE t_user_0;
CREATE TABLE IF NOT EXISTS t_user_6 LIKE t_user_0;
CREATE TABLE IF NOT EXISTS t_user_7 LIKE t_user_0;
CREATE TABLE IF NOT EXISTS t_user_8 LIKE t_user_0;
CREATE TABLE IF NOT EXISTS t_user_9 LIKE t_user_0;
CREATE TABLE IF NOT EXISTS t_user_10 LIKE t_user_0;
CREATE TABLE IF NOT EXISTS t_user_11 LIKE t_user_0;
CREATE TABLE IF NOT EXISTS t_user_12 LIKE t_user_0;
CREATE TABLE IF NOT EXISTS t_user_13 LIKE t_user_0;
CREATE TABLE IF NOT EXISTS t_user_14 LIKE t_user_0;
CREATE TABLE IF NOT EXISTS t_user_15 LIKE t_user_0;

CREATE TABLE IF NOT EXISTS undo_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    branch_id BIGINT NOT NULL,
    xid VARCHAR(100) NOT NULL,
    context VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB NOT NULL,
    log_status INT NOT NULL,
    log_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    log_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ext VARCHAR(100),
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT undo_log';
