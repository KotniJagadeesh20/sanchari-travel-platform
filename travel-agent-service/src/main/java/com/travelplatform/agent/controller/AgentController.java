package com.travelplatform.agent.controller;

import com.travelplatform.agent.client.AuthHeaders;
import com.travelplatform.agent.dto.ChatRequest;
import com.travelplatform.agent.dto.ChatResponse;
import com.travelplatform.agent.service.TravelAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase A entry point: a single stateless chat turn. The caller resends the
 * whole conversation each time (see ChatMessage) — this service holds no
 * conversation state of its own yet.
 */
@RestController
@RequestMapping("/agent")
@Validated
@Tag(name = "Travel Agent", description = "AI travel assistant — search and recommendations, requires JWT")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    @Autowired private TravelAgentService travelAgentService;

    @Operation(summary = "Send a chat turn to the AI travel assistant",
            description = "Send the full conversation so far (including the new user message). " +
                    "The assistant may call search tools internally before replying; only its final " +
                    "text reply is returned.")
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-Email") String email,
            @Parameter(hidden = true) @RequestHeader(value = "X-Authenticated-Name", required = false) String name,
            @Parameter(hidden = true) @RequestHeader(value = "X-Authenticated-Authorities", required = false) String authorities,
            @Parameter(hidden = true) @RequestHeader(value = "X-Authenticated-User-Id", required = false) String userId) {

        AuthHeaders auth = new AuthHeaders(email, name, authorities, userId);
        String reply = travelAgentService.chat(request.getMessages(), auth);
        return ResponseEntity.ok(new ChatResponse(reply));
    }
}
