package com.aiterminal.common.api;

/**
 * Immutable result of a chat completion request.
 *
 * <ul>
 *   <li>{@code content}      — the model's response text (on success)</li>
 *   <li>{@code success}      — whether the call succeeded</li>
 *   <li>{@code errorMessage} — populated on failure with a human-readable message</li>
 * </ul>
 */
public record ChatResponse(String content, boolean success, String errorMessage) {

    public static ChatResponse ok(String content) {
        return new ChatResponse(content, true, "");
    }

    public static ChatResponse error(String message) {
        return new ChatResponse("", false, message);
    }
}
