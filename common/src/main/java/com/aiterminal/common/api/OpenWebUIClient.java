package com.aiterminal.common.api;

import com.aiterminal.common.config.AITerminalConfig;
import com.aiterminal.common.json.MiniJson;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Handles all HTTP communication with an OpenWebUI instance.
 *
 * <p>Uses the built-in {@link HttpClient} (no third-party HTTP libraries). Every call is
 * asynchronous ({@link CompletableFuture}) so the game thread is never blocked. All failure
 * modes are translated into a {@link ChatResponse} carrying a human-readable message instead
 * of throwing.</p>
 */
public final class OpenWebUIClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final AITerminalConfig config;
    private final HttpClient httpClient;

    public OpenWebUIClient(AITerminalConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    /**
     * Sends the player's question to OpenWebUI and completes with the model's answer
     * (or a readable error). Never completes exceptionally.
     */
    public CompletableFuture<ChatResponse> ask(String question) {
        final String baseUrl = stripTrailingSlash(config.getOpenwebuiBaseUrl());
        final String endpoint = baseUrl + "/api/chat/completions";

        final HttpRequest request;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(question)));
            String apiKey = config.getOpenwebuiApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }
            request = builder.build();
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(
                    ChatResponse.error("Invalid OpenWebUI URL: " + endpoint));
        }

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::handleResponse)
                .exceptionally(OpenWebUIClient::handleException);
    }

    private ChatResponse handleResponse(HttpResponse<String> response) {
        int status = response.statusCode();
        String body = response.body();
        if (status == 200) {
            return parseContent(body);
        }
        return switch (status) {
            case 401 -> ChatResponse.error("Unauthorized (401): check your OpenWebUI API key in aiterminal.json.");
            case 403 -> ChatResponse.error("Forbidden (403): this API key is not allowed to use that model.");
            case 404 -> ChatResponse.error("Not found (404): check openwebui_base_url and that the model exists.");
            case 429 -> ChatResponse.error("Rate limited (429): too many requests, please wait and try again.");
            default -> ChatResponse.error("OpenWebUI returned HTTP " + status + ": " + snippet(body));
        };
    }

    private ChatResponse parseContent(String body) {
        try {
            Map<String, Object> root = MiniJson.parseObject(body);
            Object choicesObj = root.get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
                // Surface an OpenWebUI-style error payload if present.
                Object error = root.get("error");
                if (error != null) {
                    return ChatResponse.error("OpenWebUI error: " + stringifyError(error));
                }
                return ChatResponse.error("OpenWebUI response contained no choices.");
            }
            Object first = choices.get(0);
            if (first instanceof Map<?, ?> choice) {
                Object message = choice.get("message");
                if (message instanceof Map<?, ?> msg) {
                    Object content = msg.get("content");
                    if (content instanceof String s) {
                        return ChatResponse.ok(s);
                    }
                }
            }
            return ChatResponse.error("Could not find choices[0].message.content in the response.");
        } catch (RuntimeException e) {
            return ChatResponse.error("Malformed JSON from OpenWebUI: " + e.getMessage());
        }
    }

    private String buildRequestBody(String question) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        sb.append("\"model\":").append(MiniJson.quote(nullToEmpty(config.getOpenwebuiModel()))).append(',');
        sb.append("\"messages\":[");
        sb.append("{\"role\":\"system\",\"content\":").append(MiniJson.quote(nullToEmpty(config.getSystemPrompt()))).append("},");
        sb.append("{\"role\":\"user\",\"content\":").append(MiniJson.quote(nullToEmpty(question))).append('}');
        sb.append("],");
        sb.append("\"features\":{\"web_search\":").append(config.isWebSearchEnabled()).append("},");
        sb.append("\"stream\":false");
        sb.append('}');
        return sb.toString();
    }

    private static ChatResponse handleException(Throwable t) {
        Throwable cause = unwrap(t);
        if (cause instanceof HttpTimeoutException) {
            return ChatResponse.error("Request timed out after 30 seconds. Is the model loaded and responsive?");
        }
        if (cause instanceof ConnectException) {
            String detail = cause.getMessage();
            return ChatResponse.error("Could not connect to OpenWebUI"
                    + (detail != null ? " (" + detail + ")" : "")
                    + ". Is it running and is openwebui_base_url correct?");
        }
        String msg = cause.getMessage();
        return ChatResponse.error("Request failed: "
                + (msg != null ? msg : cause.getClass().getSimpleName()));
    }

    private static Throwable unwrap(Throwable t) {
        Throwable c = t;
        while ((c instanceof java.util.concurrent.CompletionException
                || c instanceof java.util.concurrent.ExecutionException)
                && c.getCause() != null) {
            c = c.getCause();
        }
        return c;
    }

    private static String stringifyError(Object error) {
        if (error instanceof Map<?, ?> m && m.get("message") instanceof String s) {
            return s;
        }
        return String.valueOf(error);
    }

    private static String snippet(String body) {
        if (body == null) {
            return "(empty body)";
        }
        String trimmed = body.strip();
        return trimmed.length() > 300 ? trimmed.substring(0, 300) + "..." : trimmed;
    }

    private static String stripTrailingSlash(String url) {
        String u = (url == null) ? AITerminalConfig.DEFAULT_BASE_URL : url.strip();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
