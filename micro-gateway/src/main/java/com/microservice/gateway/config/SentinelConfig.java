package com.microservice.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

/**
 * Sentinel Gateway 限流规则配置
 * 双层限流：
 * 1. 第一层：Sentinel Gateway（动态配置，支持 Dashboard 热更新）
 * 2. 第二层：Gateway RequestRateLimiter + Redis 令牌桶（静态兜底）
 */
@Slf4j
@Configuration
public class SentinelConfig {

    @PostConstruct
    public void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // 用户服务限流规则
        GatewayFlowRule userRule = new GatewayFlowRule();
        // 对应 application.yml 中的路由 ID
        userRule.setResource("user-service");
        // 限流阈值模式：QPS
        userRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // 限流阈值：每秒最多 30 个请求
        userRule.setCount(30);
        // 流控效果：快速失败
        userRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        // 统计窗口时间（秒）
        userRule.setIntervalSec(1);
        rules.add(userRule);

        // 订单服务限流规则
        GatewayFlowRule orderRule = new GatewayFlowRule();
        orderRule.setResource("order-service");
        orderRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // 订单服务更严格，每秒最多 15 个请求
        orderRule.setCount(15);
        orderRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        orderRule.setIntervalSec(1);
        rules.add(orderRule);

        // 加载规则
        GatewayRuleManager.loadRules(rules);
        log.info("Sentinel Gateway 限流规则初始化完成: user-service QPS=30, order-service QPS=15");
    }
}
