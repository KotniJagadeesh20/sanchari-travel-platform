package com.travelplatform.agent.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * Resolves lb://service-name URIs via Eureka + Spring Cloud LoadBalancer —
     * same mechanism the Gateway's Booking Aggregator uses (there via
     * WebClient since the Gateway is reactive; here via RestTemplate since
     * this service, like the rest of the platform outside the Gateway, is
     * plain blocking Spring MVC).
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }

    /**
     * Plain, non-load-balanced RestTemplate for calling the external
     * Anthropic API (api.anthropic.com) — not a service in our own Eureka
     * registry, so it must not go through the @LoadBalanced one above.
     */
    @Bean
    public RestTemplate anthropicRestTemplate() {
        return new RestTemplate();
    }
}
