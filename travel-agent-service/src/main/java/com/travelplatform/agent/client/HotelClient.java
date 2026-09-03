package com.travelplatform.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

/** Wraps GET /hotels on hotel-service. */
@Component
public class HotelClient {

    private final RestTemplate restTemplate;

    public HotelClient(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public JsonNode search(String destinationId, Integer starRating, String roomType,
                            Double minPrice, Double maxPrice, AuthHeaders auth) {
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString("lb://hotel-service/hotels")
                .queryParamIfPresent("destinationId", Optional.ofNullable(destinationId))
                .queryParamIfPresent("starRating", Optional.ofNullable(starRating))
                .queryParamIfPresent("roomType", Optional.ofNullable(roomType))
                .queryParamIfPresent("minPrice", Optional.ofNullable(minPrice))
                .queryParamIfPresent("maxPrice", Optional.ofNullable(maxPrice));

        HttpEntity<Void> request = new HttpEntity<>(auth.toHttpHeaders());
        return restTemplate.exchange(uri.build().toUri(), HttpMethod.GET, request, JsonNode.class).getBody();
    }
}
