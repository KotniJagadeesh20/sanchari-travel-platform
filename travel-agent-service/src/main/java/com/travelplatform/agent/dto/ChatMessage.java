package com.travelplatform.agent.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * One turn of plain user/assistant text. Phase A is stateless — the caller
 * (frontend) resends the whole conversation each time, same principle as
 * any LLM API integration with no server-side session (see the "no memory
 * between completions" note in the platform's AI usage guidance). Tool use
 * happens entirely inside one /agent/chat call and is not exposed in this
 * DTO — the client only ever sees plain text turns in and out.
 */
public class ChatMessage {

    @NotBlank
    private String role; // "user" or "assistant"

    @NotBlank
    private String content;

    public ChatMessage() {}

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
