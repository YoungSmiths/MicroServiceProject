package com.microservice.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;

/**
 * 第三方支付客户端配置。
 */
@Configuration
public class PaymentClientConfig {

    @Bean("paymentRequestFactory")
    @Primary
    public ClientHttpRequestFactory paymentRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(1200);
        factory.setBufferRequestBody(true);
        return factory;
    }

    @Bean("paymentRestTemplate")
    public RestTemplate paymentRestTemplate(@Qualifier("paymentRequestFactory") ClientHttpRequestFactory paymentRequestFactory) {
        return new RestTemplate(paymentRequestFactory);
    }
}
