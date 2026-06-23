package com.microservice.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.redisson.spring.starter.RedissonAutoConfiguration;

/**
 * 网关启动类
 */
@EnableDiscoveryClient
@SpringBootApplication(exclude = RedissonAutoConfiguration.class)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
