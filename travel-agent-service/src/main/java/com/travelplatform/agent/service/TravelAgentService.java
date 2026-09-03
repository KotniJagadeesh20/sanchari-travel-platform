package com.travelplatform.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.travelplatform.agent.anthropic.AnthropicClient;
import com.travelplatform.agent.client.AuthHeaders;
import com.travelplatform.agent.dto.ChatMessage;
import com.travelplatform.agent.tool.ToolExecutor;
import com.travelplatform.agent.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Phase A: Search & Recommendations only. Runs the standard Anthropic
 * tool-use loop — send messages+tools, execute any tool_use blocks the
 * model asks for, feed results back as tool_result, repeat until the model
 * stops asking for tools (stop_reason != "tool_use") — then returns its
 * final text reply.
 *
 * No booking action is ever taken here; the tool set only contains search
 * tools (see ToolRegistry). Booking Assistance (Phase C) is a deliberately
 * separate, later addition, and per the roadmap must require explicit user
 * confirmation before calling any booking API — nothing in this loop should
 * be extended to call a booking endpoint without that confirmation step
 * being designed in first.
 */
@Service
public class TravelAgentService {

    private static final Logger log = LoggerFactory.getLogger(TravelAgentService.class);
    private static final int MAX_TOOL_ITERATIONS = 5;

    private static final String SYSTEM_PROMPT = """
            You are Sanchari's AI travel assistant. Help the user find destinations, \
            travel packages, hotels, buses, and rides using the search tools available \
            to you. Ask a clarifying question only when you genuinely cannot make progress \
            without more information; otherwise make a reasonable assumption and proceed. \
            Always base concrete details (prices, names, availability) on tool results — \
            never invent them. If a tool reports it's unavailable, say so plainly and \
            suggest the user try again shortly, rather than pretending you have the data.""";

    private final AnthropicClient anthropicClient;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final ObjectMapper objectMapper;

    public TravelAgentService(AnthropicClient anthropicClient, ToolRegistry toolRegistry,
                               ToolExecutor toolExecutor, ObjectMapper objectMapper) {
        this.anthropicClient = anthropicClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.objectMapper = objectMapper;
    }

    public String chat(List<ChatMessage> conversation, AuthHeaders auth) {
        ArrayNode messages = objectMapper.createArrayNode();
        for (ChatMessage m : conversation) {
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("role", m.getRole());
            msg.put("content", m.getContent());
            messages.add(msg);
        }

        ArrayNode tools = toolRegistry.buildToolDefinitions();

        for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
            JsonNode response = anthropicClient.sendMessage(SYSTEM_PROMPT, messages, tools);

            if (response.has("error")) {
                log.warn("Anthropic API error: {}", response.get("error"));
                return "Sorry, I'm having trouble reaching the assistant right now. Please try again shortly.";
            }

            JsonNode content = response.get("content");
            String stopReason = response.path("stop_reason").asText();

            if (!"tool_use".equals(stopReason)) {
                return extractText(content);
            }

            // Assistant's turn (including its tool_use blocks) goes back into
            // the conversation verbatim before we can send tool_result back.
            ObjectNode assistantMsg = objectMapper.createObjectNode();
            assistantMsg.put("role", "assistant");
            assistantMsg.set("content", content);
            messages.add(assistantMsg);

            ArrayNode toolResults = objectMapper.createArrayNode();
            for (JsonNode block : content) {
                if (!"tool_use".equals(block.path("type").asText())) continue;

                String toolUseId = block.get("id").asText();
                String toolName = block.get("name").asText();
                JsonNode input = block.get("input");

                Object result = toolExecutor.execute(toolName, input, auth);

                ObjectNode toolResult = objectMapper.createObjectNode();
                toolResult.put("type", "tool_result");
                toolResult.put("tool_use_id", toolUseId);
                toolResult.put("content", toJsonString(result));
                toolResults.add(toolResult);
            }

            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.set("content", toolResults);
            messages.add(userMsg);
        }

        log.warn("Hit MAX_TOOL_ITERATIONS ({}) without a final answer", MAX_TOOL_ITERATIONS);
        return "I gathered a lot of information but couldn't quite finish putting it together — could you narrow down your request a bit?";
    }

    private String extractText(JsonNode content) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText())) {
                sb.append(block.path("text").asText());
            }
        }
        return sb.toString();
    }

    private String toJsonString(Object result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return String.valueOf(result);
        }
    }
}
