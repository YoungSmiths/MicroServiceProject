package com.microservice.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class GatewayController {

    @GetMapping("/")
    public Map<String, Object> index() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", "micro-gateway");
        result.put("status", "UP");
        result.put("userServicePath", "/api/user/**");
        result.put("orderServicePath", "/api/order/**");
        result.put("mockThirdPartyPath", "/mock/payment/charge");
        return result;
    }
}
