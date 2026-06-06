package com.aiterminal.common.api;

import com.aiterminal.common.config.AITerminalConfig;
import com.aiterminal.common.json.MiniJson;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles all HTTP communication with an OpenWebUI instance.
 *
 * <p>Uses the built-in {@link HttpClient} (no third-party HTTP libraries). The request is
 * <b>streamed</b> (Server-Sent Events): tokens are delivered to a {@link StreamListener} as they
 * arrive. There is a short connect timeout but <b>no read timeout</b>, so slow models / web search
 * can take as long as they need. All failure modes are reported via {@link StreamListener#onError}
 * instead of throwing.</p>
 */
public final class OpenWebUIClient {

    /** Fail fast if we can't even establish a connection; generation itself is unbounded. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    /** Receives streamed output. All callbacks happen on an HTTP worker thread. */
    public interface StreamListener {
        /** A fragment of the assistant's answer. */
        void onToken(String text);

        /** The stream finished successfully. */
        void onComplete();

        /** The request failed; {@code message} is human-readable. */
        void onError(String message);
    }

    private final AITerminalConfig config;
    private final HttpClient httpClient;

    public OpenWebUIClient(AITerminalConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    /**
     * Streams the answer to the player's question. The returned future completes when the stream
     * ends (or fails); cancelling it best-effort aborts the request. Never completes exceptionally
     * in a way the caller must handle — errors are delivered through {@code listener.onError}.
     */
    public CompletableFuture<Void> askStreaming(String question, StreamListener listener) {
        final String endpoint = stripTrailingSlash(config.getOpenwebuiBaseUrl()) + "/api/chat/completions";

        final HttpRequest request;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(question)));
            String apiKey = config.getOpenwebuiApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }
            request = builder.build();
        } catch (IllegalArgumentException e) {
            listener.onError("Invalid OpenWebUI URL: " + endpoint);
            return CompletableFuture.completedFuture(null);
        }

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> consumeStream(response, listener))
                .exceptionally(throwable -> {
                    Throwable cause = unwrap(throwable);
                    if (!(cause instanceof CancellationException)) {
                        listener.onError(exceptionMessage(cause));
                    }
                    return null;
                });
    }

    private void consumeStream(HttpResponse<Stream<String>> response, StreamListener listener) {
        int status = response.statusCode();
        if (status != 200) {
            String body = response.body().collect(Collectors.joining("\n"));
            listener.onError(httpErrorMessage(status, body));
            return;
        }

        boolean[] done = {false};
        response.body().forEach(line -> {
            if (done[0]) {
                return;
            }
            String payload = parseSseData(line);
            if (payload == null) {
                return;
            }
            if (payload.equals("[DONE]")) {
                done[0] = true;
                return;
            }
            String content = extractDeltaContent(payload);
            if (content != null && !content.isEmpty()) {
                listener.onToken(content);
            }
        });
        listener.onComplete();
    }

    /** Extracts {@code choices[0].delta.content} (streaming) or {@code .message.content} (fallback). */
    private static String extractDeltaContent(String json) {
        try {
            Map<String, Object> root = MiniJson.parseObject(json);
            Object error = root.get("error");
            if (error != null) {
                return null;
            }
            Object choicesObj = root.get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
                return null;
            }
            if (choices.get(0) instanceof Map<?, ?> choice) {
                Object delta = choice.get("delta");
                if (delta instanceof Map<?, ?> d && d.get("content") instanceof String s) {
                    return s;
                }
                Object message = choice.get("message");
                if (message instanceof Map<?, ?> m && m.get("content") instanceof String s) {
                    return s;
                }
            }
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Returns the payload of a {@code data:} SSE line, or {@code null} for comments/blank lines. */
    private static String parseSseData(String line) {
        if (line == null || line.isEmpty() || line.startsWith(":")) {
            return null;
        }
        if (line.startsWith("data:")) {
            String rest = line.substring(5);
            if (rest.startsWith(" ")) {
                rest = rest.substring(1);
            }
            return rest;
        }
        return null;
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
        sb.append("\"stream\":true");
        sb.append('}');
        return sb.toString();
    }

    private static String httpErrorMessage(int status, String body) {
        return switch (status) {
            case 401 -> "Unauthorized (401): check your OpenWebUI API key in aiterminal.json.";
            case 403 -> "Forbidden (403): this API key is not allowed to use that model.";
            case 404 -> "Not found (404): check openwebui_base_url and that the model exists.";
            case 422 -> "Unprocessable (422): the request was rejected. Check openwebui_model is exact"
                    + " and try web_search_enabled=false. " + snippet(body);
            case 429 -> "Rate limited (429): too many requests, please wait and try again.";
            default -> "OpenWebUI returned HTTP " + status + ": " + snippet(body);
        };
    }

    private static String exceptionMessage(Throwable cause) {
        if (cause instanceof HttpConnectTimeoutException) {
            return "Could not connect to OpenWebUI (connection timed out)."
                    + " Is it running and is openwebui_base_url correct?";
        }
        if (cause instanceof ConnectException) {
            String detail = cause.getMessage();
            return "Could not connect to OpenWebUI"
                    + (detail != null ? " (" + detail + ")" : "")
                    + ". Is it running and is openwebui_base_url correct?";
        }
        String msg = cause.getMessage();
        return "Request failed: " + (msg != null ? msg : cause.getClass().getSimpleName());
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

    private static String snippet(String body) {
        if (body == null) {
            return "";
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
