package com.travelplatform.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** Wraps GET /destinations/search on travel-packages-service. */
@Component
public class DestinationClient {

    private final RestTemplate restTemplate;

    public DestinationClient(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public JsonNode search(String keyword, String category, Double maxBudget, Integer visitMonth, AuthHeaders auth) {
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString("lb://travel-packages-service/destinations/search")
                .queryParamIfPresent("keyword", java.util.Optional.ofNullable(keyword))
                .queryParamIfPresent("category", java.util.Optional.ofNullable(category))
                .queryParamIfPresent("maxBudget", java.util.Optional.ofNullable(maxBudget))
                .queryParamIfPresent("visitMonth", java.util.Optional.ofNullable(visitMonth));

        HttpEntity<Void> request = new HttpEntity<>(auth.toHttpHeaders());
        return restTemplate.exchange(uri.build().toUri(), HttpMethod.GET, request, JsonNode.class).getBody();
    }
}
