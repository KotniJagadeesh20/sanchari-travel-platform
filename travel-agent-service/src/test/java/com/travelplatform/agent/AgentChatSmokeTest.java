package com.travelplatform.agent;

import com.travelplatform.agent.client.AuthHeaders;
import com.travelplatform.agent.dto.ChatMessage;
import com.travelplatform.agent.service.TravelAgentService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the COMPLETE Phase 1 flow, not just any one layer:
 *
 *   TravelAgentService.chat()
 *     -> AnthropicClient (real call to api.anthropic.com)
 *     -> stop_reason == "tool_use"
 *     -> ToolExecutor -> a real domain client -> a real running Sanchari service
 *     -> ToolResultNormalizer (result unwrapped to a bare array)
 *     -> tool_result sent back to Anthropic
 *     -> final text answer
 *
 * Deliberately @Disabled by default: this makes a REAL, BILLED call to the
 * Anthropic API every time it runs, and requires the full docker-compose
 * stack (including travel-agent-service itself, registered in Eureka) to be
 * up with a real ANTHROPIC_API_KEY set. Remove @Disabled locally to run it;
 * do not remove it in anything that runs unattended (CI, mvn verify).
 *
 * This test intentionally does NOT go through the Gateway/HTTP layer — it
 * calls TravelAgentService directly in-process, supplying AuthHeaders the
 * way AgentController would after extracting them from the gateway-forwarded
 * request headers. The Gateway's JWT validation + header-forwarding is
 * existing, unchanged infrastructure (see JwtAuthFilter), not something this
 * step needs to re-prove; what's new and unverified is everything below
 * AgentController, which this test does exercise.
 */
@SpringBootTest
@Disabled("Costs real Anthropic API usage and requires the full docker-compose stack. Remove locally to run.")
class AgentChatSmokeTest {

    @Autowired
    private TravelAgentService travelAgentService;

    private final AuthHeaders auth = new AuthHeaders("smoke-test@example.com", "Smoke Test", "ROLE_USER", "smoke-test-user");

    @Test
    void searchDestinationsPrompt_completesFullToolLoopAndReturnsAnAnswer() {
        List<ChatMessage> conversation = List.of(
                new ChatMessage("user", "Find me beach destinations with a budget under 20000 rupees."));

        String reply = travelAgentService.chat(conversation, auth);

        assertThat(reply)
                .as("Should get a real final answer, not an empty string or an error placeholder")
                .isNotBlank();
        assertThat(reply.toLowerCase())
                .as("A sane reply to a destinations query should mention destinations/beaches in some form, " +
                        "not silently ignore the tool results and answer something unrelated")
                .containsAnyOf("beach", "destination");
    }

    @Test
    void busSearchPrompt_toleratesTheNormalizedBusArrayShape() {
        // Specifically exercises the tool whose raw shape needed unwrapping
        // (BusResponse -> busses) — confirms the normalized array Claude
        // receives is actually usable, not just structurally an array.
        List<ChatMessage> conversation = List.of(
                new ChatMessage("user", "Are there any buses from Hyderabad to Bangalore next Friday?"));

        String reply = travelAgentService.chat(conversation, auth);

        assertThat(reply).isNotBlank();
    }
}
