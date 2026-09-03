package com.travelplatform.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;

/**
 * Wraps GET /api/user/searchbusses/{source}/{destination}/{date} on
 * bus-booking-service. Requires authentication (unlike the other search
 * endpoints), so callers must always supply real AuthHeaders here.
 * source/destination are matched case/whitespace-insensitively server-side
 * now, so this client just trims — no lowercasing or other workaround needed.
 */
@Component
public class BusClient {

    private final RestTemplate restTemplate;

    public BusClient(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public JsonNode search(String source, String destination, LocalDate date, AuthHeaders auth) {
        String uri = UriComponentsBuilder
                .fromUriString("lb://bus-booking-service/api/user/searchbusses/{source}/{destination}/{date}")
                .buildAndExpand(source.trim(), destination.trim(), date)
                .toUriString();

        HttpEntity<Void> request = new HttpEntity<>(auth.toHttpHeaders());
        return restTemplate.exchange(uri, HttpMethod.GET, request, JsonNode.class).getBody();
    }
}
