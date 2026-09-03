package com.travelplatform.agent.client;

import org.springframework.http.HttpHeaders;

/**
 * The four identity headers the Gateway's JwtAuthFilter sets on every
 * authenticated request in to this service. When this service in turn calls
 * another downstream service directly (bypassing the Gateway), it must
 * forward these itself — otherwise that service's own JwtValidator sees no
 * identity and rejects anything requiring authentication (e.g. Bus search).
 */
public record AuthHeaders(String email, String name, String authorities, String userId) {

    public HttpHeaders toHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (email != null) headers.set("X-Authenticated-Email", email);
        if (name != null) headers.set("X-Authenticated-Name", name);
        if (authorities != null) headers.set("X-Authenticated-Authorities", authorities);
        if (userId != null) headers.set("X-Authenticated-User-Id", userId);
        return headers;
    }
}
