package com.travelplatform.busbooking.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    /**
     * @LoadBalanced lets this RestTemplate resolve logical service names
     * (e.g. http://notification-service/...) through Eureka instead of a
     * hardcoded host:port — see NotificationClient.
     *
     * Built from Spring Boot's auto-configured RestTemplateBuilder (rather than
     * `new RestTemplate()`) so the ObservationRestTemplateCustomizer that Boot
     * wires in when Micrometer Tracing is on the classpath actually applies.
     * Without this, calls to notification-service would be invisible to Zipkin
     * even though every other hop in the trace is instrumented.
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
