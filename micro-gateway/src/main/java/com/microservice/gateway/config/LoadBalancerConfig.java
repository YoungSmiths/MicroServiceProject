package com.microservice.gateway.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 负载均衡配置
 * 
 * 可以针对不同服务配置不同的负载均衡策略：
 * - RoundRobinLoadBalancer: 轮询（默认）
 * - RandomLoadBalancer: 随机
 * - WeightedResponseTimeLoadBalancer: 响应时间加权
 * - ConsistentHashLoadBalancer: 基于用户ID的一致性hash
 */
@Configuration
@LoadBalancerClients({
    @LoadBalancerClient(name = "user-service", configuration = UserServiceLoadBalancerConfig.class),
    @LoadBalancerClient(name = "order-service", configuration = OrderServiceLoadBalancerConfig.class)
})
public class LoadBalancerConfig {

}

/**
 * 一致性Hash算法实现
 */
class ConsistentHash {
    // 使用TreeMap维护hash环，key为hash值，value为服务实例
    private final SortedMap<Long, ServiceInstance> ring = new TreeMap<>();
    // 每个真实节点对应的虚拟节点数量
    private final int virtualNodes;
    // 虚拟节点的后缀标识
    private static final String VIRTUAL_NODE_SUFFIX = "#VN";

    /**
     * 构造函数，初始化一致性hash环
     * @param instances 初始服务实例列表
     * @param virtualNodes 每个节点的虚拟节点数量
     */
    public ConsistentHash(List<ServiceInstance> instances, int virtualNodes) {
        // 保存虚拟节点数量
        this.virtualNodes = virtualNodes;
        // 遍历所有服务实例，逐个添加到hash环中
        for (ServiceInstance instance : instances) {
            add(instance);
        }
    }

    /**
     * 向hash环中添加服务实例
     * @param instance 要添加的服务实例
     */
    public void add(ServiceInstance instance) {
        // 使用主机名+端口作为节点的唯一标识
        String key = instance.getHost() + ":" + instance.getPort();
        // 为每个真实节点创建多个虚拟节点，提高hash分布的均匀性
        for (int i = 0; i < virtualNodes; i++) {
            // 生成虚拟节点的key：真实节点key + 虚拟节点后缀 + 序号
            long hash = hash(key + VIRTUAL_NODE_SUFFIX + i);
            // 将虚拟节点放入hash环
            ring.put(hash, instance);
        }
    }

    /**
     * 从hash环中移除服务实例
     * @param instance 要移除的服务实例
     */
    public void remove(ServiceInstance instance) {
        // 获取节点的唯一标识
        String key = instance.getHost() + ":" + instance.getPort();
        // 移除该节点对应的所有虚拟节点
        for (int i = 0; i < virtualNodes; i++) {
            // 生成虚拟节点的hash值
            long hash = hash(key + VIRTUAL_NODE_SUFFIX + i);
            // 从hash环中删除该虚拟节点
            ring.remove(hash);
        }
    }

    /**
     * 根据key获取对应的服务实例
     * @param key 用于hash计算的key（如用户ID）
     * @return 对应的服务实例，如果环为空则返回null
     */
    public ServiceInstance get(String key) {
        // 如果hash环为空，返回null
        if (ring.isEmpty()) {
            return null;
        }
        // 计算key的hash值
        long hash = hash(key);
        // 获取hash环中大于等于该hash值的所有节点
        SortedMap<Long, ServiceInstance> tailMap = ring.tailMap(hash);
        // 如果没有找到（key的hash值大于所有节点），则取环上第一个节点（顺时针查找）
        Long targetHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        // 返回对应的服务实例
        return ring.get(targetHash);
    }

    /**
     * 计算字符串的hash值
     * @param key 要计算hash的字符串
     * @return hash值（非负）
     */
    private long hash(String key) {
        // 初始化hash值
        long h = 0;
        // 遍历字符串的每个字符
        for (int i = 0; i < key.length(); i++) {
            // 使用31作为乘数，这是String.hashCode()的经典算法
            // 31是一个奇素数，能提供较好的分布性
            h = 31 * h + key.charAt(i);
        }
        // 确保hash值为非负数（Long.MAX_VALUE的二进制是01111111...）
        return h & Long.MAX_VALUE;
    }
}

/**
 * 基于用户ID的一致性hash负载均衡器
 */
class ConsistentHashLoadBalancer implements ReactorServiceInstanceLoadBalancer {
    private static final int VIRTUAL_NODES = 160;
    private final String serviceId;
    private final org.springframework.beans.factory.ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    private final Map<String, ConsistentHash> cache = new ConcurrentHashMap<>();

    public ConsistentHashLoadBalancer(org.springframework.beans.factory.ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.serviceId = serviceId;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
                .getIfAvailable(NoopServiceInstanceListSupplier::new);
        return supplier.get(request).next()
                .map(serviceInstances -> processInstanceList(serviceInstances, request));
    }

    private Response<ServiceInstance> processInstanceList(List<ServiceInstance> instances, Request<?> request) {
        if (instances.isEmpty()) {
            return new EmptyResponse();
        }

        String instancesKey = instances.stream()
                .map(i -> i.getHost() + ":" + i.getPort())
                .sorted()
                .collect(Collectors.joining(","));

        ConsistentHash consistentHash = cache.computeIfAbsent(instancesKey, k -> new ConsistentHash(instances, VIRTUAL_NODES));

        String userId = extractUserId(request);

        ServiceInstance selected = consistentHash.get(userId != null ? userId : UUID.randomUUID().toString());
        return new DefaultResponse(selected);
    }

    private String extractUserId(Request<?> request) {
        Object context = request.getContext();
        if (context instanceof ServerWebExchange) {
            ServerWebExchange exchange = (ServerWebExchange) context;
            HttpHeaders headers = exchange.getRequest().getHeaders();
            
            String userId = headers.getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return userId;
            }
            
            userId = headers.getFirst("userId");
            if (userId != null && !userId.isEmpty()) {
                return userId;
            }
            
            String path = exchange.getRequest().getURI().getPath();
            if (path.contains("/user/")) {
                String[] parts = path.split("/");
                for (int i = 0; i < parts.length - 1; i++) {
                    if ("user".equals(parts[i]) && i + 1 < parts.length) {
                        return parts[i + 1];
                    }
                }
            }
        }
        return null;
    }
}

/**
 * 用户服务负载均衡配置 - 使用基于用户ID的一致性hash策略
 */
class UserServiceLoadBalancerConfig {
    @Bean
    public ReactorServiceInstanceLoadBalancer consistentHashLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new ConsistentHashLoadBalancer(
                loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
                name);
    }
}

/**
 * 订单服务负载均衡配置 - 使用轮询策略（默认，这里显式指定）
 */
class OrderServiceLoadBalancerConfig {
    @Bean
    public ReactorServiceInstanceLoadBalancer roundRobinLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer(
                loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
                name);
    }
}
