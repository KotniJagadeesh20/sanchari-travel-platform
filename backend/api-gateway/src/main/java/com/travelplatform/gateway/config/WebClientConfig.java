package com.travelplatform.gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * A load-balanced WebClient.Builder, resolving lb://service-name URIs via
 * Eureka + Spring Cloud LoadBalancer — the same mechanism the Gateway's own
 * routes already use for lb://bus-booking-service etc. This lets the booking
 * aggregator call downstream services directly by name without hardcoding
 * hosts/ports, and without going through a Gateway Route (which would just
 * proxy the call, not let us fan it out in parallel and merge results).
 */
@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
