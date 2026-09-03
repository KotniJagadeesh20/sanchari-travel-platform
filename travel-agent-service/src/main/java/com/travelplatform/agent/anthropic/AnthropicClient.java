package com.travelplatform.agent.anthropic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Direct HTTP wrapper around POST https://api.anthropic.com/v1/messages —
 * no SDK. Deliberate choice: this platform is blocking Spring MVC + JsonNode
 * throughout (see BookingAggregatorService in the Gateway), and the tool-use
 * loop needs enough control over the request/response shape that a thin
 * direct wrapper is simpler to reason about than adopting an SDK dependency
 * for a single endpoint.
 */
@Component
public class AnthropicClient {

    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public AnthropicClient(@Qualifier("anthropicRestTemplate") RestTemplate restTemplate,
                            ObjectMapper objectMapper,
                            @Value("${anthropic.api-key}") String apiKey,
                            // Confirm this against Anthropic's current model list before deploying —
                            // model strings/aliases change over time.
                            @Value("${anthropic.model:claude-sonnet-4-6}") String model,
                            @Value("${anthropic.max-tokens:2048}") int maxTokens) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    /**
     * Sends one turn of the conversation. messages and tools are pre-built
     * JsonNode arrays (see ToolRegistry for tool schemas, TravelAgentService
     * for message construction) so the caller keeps full control over the
     * exact request shape without this class needing matching POJOs for
     * every content-block variant Anthropic supports.
     */
    public JsonNode sendMessage(String systemPrompt, ArrayNode messages, ArrayNode tools) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("system", systemPrompt);
        body.set("messages", messages);
        if (tools != null && tools.size() > 0) {
            body.set("tools", tools);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", ANTHROPIC_VERSION);

        HttpEntity<ObjectNode> request = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(ENDPOINT, request, JsonNode.class);
    }
}
