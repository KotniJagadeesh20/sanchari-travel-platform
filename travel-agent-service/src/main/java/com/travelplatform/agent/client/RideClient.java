package com.travelplatform.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;

/** Wraps GET /rides/search on ride-share-service (public — no auth required). */
@Component
public class RideClient {

    private final RestTemplate restTemplate;

    public RideClient(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public JsonNode search(String source, String destination, LocalDate date, AuthHeaders auth) {
        String uri = UriComponentsBuilder
                .fromUriString("lb://ride-share-service/rides/search")
                .queryParam("source", source.trim())
                .queryParam("destination", destination.trim())
                .queryParam("date", date)
                .build().toUriString();

        HttpEntity<Void> request = new HttpEntity<>(auth.toHttpHeaders());
        return restTemplate.exchange(uri, HttpMethod.GET, request, JsonNode.class).getBody();
    }
}
