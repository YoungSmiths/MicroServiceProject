package com.microservice.gateway.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 负载均衡配置
 * 
 * 可以针对不同服务配置不同的负载均衡策略：
 * - RoundRobinLoadBalancer: 轮询（默认）
 * - RandomLoadBalancer: 随机
 * - WeightedResponseTimeLoadBalancer: 响应时间加权
 */
@Configuration
@LoadBalancerClients({
    @LoadBalancerClient(name = "user-service", configuration = UserServiceLoadBalancerConfig.class),
    @LoadBalancerClient(name = "order-service", configuration = OrderServiceLoadBalancerConfig.class)
})
public class LoadBalancerConfig {

}

/**
 * 用户服务负载均衡配置 - 使用随机策略
 */
class UserServiceLoadBalancerConfig {
    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(
                loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
                name);
    }
}

/**
 * 订单服务负载均衡配置 - 使用轮询策略（默认，这里显式指定）
 */
class OrderServiceLoadBalancerConfig {
    @Bean
    public ReactorLoadBalancer<ServiceInstance> roundRobinLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer(
                loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
                name);
    }
}
