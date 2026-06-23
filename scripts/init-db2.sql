-- 订单库0初始化脚本
CREATE DATABASE IF NOT EXISTS order_db0 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE order_db0;

-- 创建订单表（16个分片）
CREATE TABLE IF NOT EXISTS t_order_0 (
    order_id BIGINT NOT NULL COMMENT '订单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_no VARCHAR(50) NOT NULL COMMENT '订单编号',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    status TINYINT DEFAULT 0 COMMENT '订单状态: 0-待支付, 1-已支付, 2-已取消, 3-已完成, 4-已退款',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收货人电话',
    receiver_address VARCHAR(200) NOT NULL COMMENT '收货地址',
    remark VARCHAR(500) COMMENT '订单备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志: 0-未删除, 1-已删除',
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE IF NOT EXISTS t_order_1 LIKE t_order_0;
CREATE TABLE IF NOT EXISTS t_order_2 LIKE t_order_0;
CREATE TABLE IF NOT EXISTS t_order_3 LIKE t_order_0;
CREATE TABLE IF NOT EXISTS t_order_4 LIKE t_order_0;
CREATE TABLE IF NOT EXISTS t_order_5 LIKE t_order_0;
CREATE TABLE IF NOT EXISTS t_order_6 LIKE t_order_0;
CREATE TABLE IF NOT EXISTS t_order_7 LIKE t_order_0;
CREATE TABLE IF NOT EXISTS t_order_8 LIKE t_order_0;
CREATE TABLE IF NOT EXISTS t_order_9 LIKE t_order_0;
CREATE TABLE IF NOT EXISTS t_order_10 LIKE t_order_0;
CREATE TABLE IF NOT EXISTS t_order_11 LIKE t_order_0;
CREATE TABLE IF NOT EXISTS t_order_12 LIKE t_order_0;
CREATE TABLE IF NOT EXISTS t_order_13 LIKE t_order_0;
CREATE TABLE IF NOT EXISTS t_order_14 LIKE t_order_0;
CREATE TABLE IF NOT EXISTS t_order_15 LIKE t_order_0;

-- 创建订单项表（16个分片）
CREATE TABLE IF NOT EXISTS t_order_item_0 (
    item_id BIGINT NOT NULL COMMENT '订单项ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    product_image VARCHAR(200) COMMENT '商品图片',
    price DECIMAL(10,2) NOT NULL COMMENT '商品单价',
    quantity INT NOT NULL COMMENT '购买数量',
    subtotal DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志: 0-未删除, 1-已删除',
    PRIMARY KEY (item_id),
    KEY idx_order_id (order_id),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项表';

CREATE TABLE IF NOT EXISTS t_order_item_1 LIKE t_order_item_0;
CREATE TABLE IF NOT EXISTS t_order_item_2 LIKE t_order_item_0;
CREATE TABLE IF NOT EXISTS t_order_item_3 LIKE t_order_item_0;
CREATE TABLE IF NOT EXISTS t_order_item_4 LIKE t_order_item_0;
CREATE TABLE IF NOT EXISTS t_order_item_5 LIKE t_order_item_0;
CREATE TABLE IF NOT EXISTS t_order_item_6 LIKE t_order_item_0;
CREATE TABLE IF NOT EXISTS t_order_item_7 LIKE t_order_item_0;
CREATE TABLE IF NOT EXISTS t_order_item_8 LIKE t_order_item_0;
CREATE TABLE IF NOT EXISTS t_order_item_9 LIKE t_order_item_0;
CREATE TABLE IF NOT EXISTS t_order_item_10 LIKE t_order_item_0;
CREATE TABLE IF NOT EXISTS t_order_item_11 LIKE t_order_item_0;
CREATE TABLE IF NOT EXISTS t_order_item_12 LIKE t_order_item_0;
CREATE TABLE IF NOT EXISTS t_order_item_13 LIKE t_order_item_0;
CREATE TABLE IF NOT EXISTS t_order_item_14 LIKE t_order_item_0;
CREATE TABLE IF NOT EXISTS t_order_item_15 LIKE t_order_item_0;

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
