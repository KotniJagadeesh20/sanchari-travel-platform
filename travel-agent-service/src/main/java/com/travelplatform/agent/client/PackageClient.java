package com.travelplatform.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

/** Wraps GET /packages/search on travel-packages-service. */
@Component
public class PackageClient {

    private final RestTemplate restTemplate;

    public PackageClient(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public JsonNode search(String destinationId, String keyword, Double maxBudget,
                            Integer minDurationDays, Integer maxDurationDays, AuthHeaders auth) {
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString("lb://travel-packages-service/packages/search")
                .queryParamIfPresent("destinationId", Optional.ofNullable(destinationId))
                .queryParamIfPresent("keyword", Optional.ofNullable(keyword))
                .queryParamIfPresent("maxBudget", Optional.ofNullable(maxBudget))
                .queryParamIfPresent("minDurationDays", Optional.ofNullable(minDurationDays))
                .queryParamIfPresent("maxDurationDays", Optional.ofNullable(maxDurationDays));

        HttpEntity<Void> request = new HttpEntity<>(auth.toHttpHeaders());
        return restTemplate.exchange(uri.build().toUri(), HttpMethod.GET, request, JsonNode.class).getBody();
    }
}
