package com.aiterminal.common.config;

import com.aiterminal.common.json.MiniJson;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * All user-configurable values for the mod.
 *
 * <p>Backed by {@code config/aiterminal.json}. The file is auto-created with defaults and
 * inline comments on first launch. Loaded once at mod init; to apply changes the user edits
 * the file and restarts the game. Missing/malformed fields fall back to defaults (with a log
 * warning) rather than crashing.</p>
 */
public final class AITerminalConfig {

    private static final Logger LOG = System.getLogger("aiterminal-config");
    public static final String FILE_NAME = "aiterminal.json";

    // ---- Defaults --------------------------------------------------------
    public static final String DEFAULT_BASE_URL = "http://localhost:3000";
    public static final String DEFAULT_API_KEY = "";
    public static final String DEFAULT_MODEL = "";
    public static final boolean DEFAULT_WEB_SEARCH = true;
    public static final int DEFAULT_MAX_RESPONSE_LENGTH = 2000;
    public static final String DEFAULT_SYSTEM_PROMPT =
            "You are a helpful Minecraft assistant with access to web search. "
            + "Answer questions about Minecraft gameplay, crafting, mods, and mechanics accurately and concisely. "
            + "When referencing mod-specific content, note which mod it belongs to. "
            + "Keep answers focused and practical for an in-game player.";

    // ---- Values ----------------------------------------------------------
    private String openwebuiBaseUrl = DEFAULT_BASE_URL;
    private String openwebuiApiKey = DEFAULT_API_KEY;
    private String openwebuiModel = DEFAULT_MODEL;
    private boolean webSearchEnabled = DEFAULT_WEB_SEARCH;
    private int maxResponseLength = DEFAULT_MAX_RESPONSE_LENGTH;
    private String systemPrompt = DEFAULT_SYSTEM_PROMPT;

    public String getOpenwebuiBaseUrl() {
        return openwebuiBaseUrl;
    }

    public String getOpenwebuiApiKey() {
        return openwebuiApiKey;
    }

    public String getOpenwebuiModel() {
        return openwebuiModel;
    }

    public boolean isWebSearchEnabled() {
        return webSearchEnabled;
    }

    public int getMaxResponseLength() {
        return maxResponseLength;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * Loads the config from {@code <configDir>/aiterminal.json}, creating it with defaults if absent.
     * Never throws for bad content — falls back to defaults field-by-field and logs warnings.
     */
    public static AITerminalConfig loadOrCreate(Path configDir) {
        AITerminalConfig config = new AITerminalConfig();
        Path file = configDir.resolve(FILE_NAME);
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(configDir);
                Files.writeString(file, defaultFileContents(), StandardCharsets.UTF_8);
                LOG.log(Level.INFO, "Created default config at " + file);
                return config;
            }
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            config.readFrom(raw);
        } catch (IOException e) {
            LOG.log(Level.ERROR, "Could not read or write config file " + file + "; using defaults.", e);
        } catch (RuntimeException e) {
            LOG.log(Level.ERROR, "Config file " + file + " is malformed; using defaults. (" + e.getMessage() + ")");
        }
        return config;
    }

    private void readFrom(String raw) {
        Map<String, Object> json = MiniJson.parseObject(raw);
        openwebuiBaseUrl = readString(json, "openwebui_base_url", DEFAULT_BASE_URL);
        openwebuiApiKey = readString(json, "openwebui_api_key", DEFAULT_API_KEY);
        openwebuiModel = readString(json, "openwebui_model", DEFAULT_MODEL);
        webSearchEnabled = readBoolean(json, "web_search_enabled", DEFAULT_WEB_SEARCH);
        maxResponseLength = readInt(json, "max_response_length", DEFAULT_MAX_RESPONSE_LENGTH);
        systemPrompt = readString(json, "system_prompt", DEFAULT_SYSTEM_PROMPT);
    }

    private static String readString(Map<String, Object> json, String key, String def) {
        Object v = json.get(key);
        if (v instanceof String s) {
            return s;
        }
        if (v != null) {
            warnWrongType(key, "string");
        } else if (!json.containsKey(key)) {
            warnMissing(key);
        }
        return def;
    }

    private static boolean readBoolean(Map<String, Object> json, String key, boolean def) {
        Object v = json.get(key);
        if (v instanceof Boolean b) {
            return b;
        }
        if (v != null) {
            warnWrongType(key, "boolean");
        } else if (!json.containsKey(key)) {
            warnMissing(key);
        }
        return def;
    }

    private static int readInt(Map<String, Object> json, String key, int def) {
        Object v = json.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v != null) {
            warnWrongType(key, "number");
        } else if (!json.containsKey(key)) {
            warnMissing(key);
        }
        return def;
    }

    private static void warnMissing(String key) {
        LOG.log(Level.WARNING, "Config field '" + key + "' is missing; using its default value.");
    }

    private static void warnWrongType(String key, String expected) {
        LOG.log(Level.WARNING, "Config field '" + key + "' has the wrong type (expected " + expected
                + "); using its default value.");
    }

    /** The exact text written to disk on first launch — valid JSON with explanatory comments. */
    public static String defaultFileContents() {
        return "{\n"
                + "  // AI Terminal configuration. Edit this file and restart Minecraft to apply changes.\n"
                + "  // Comments (// and /* */) are allowed; the rest must stay valid JSON.\n"
                + "  \"_comment\": \"AI Terminal configuration. Edit this file and restart Minecraft to apply changes.\",\n"
                + "\n"
                + "  // Base URL of your OpenWebUI instance (no trailing slash).\n"
                + "  \"openwebui_base_url\": " + MiniJson.quote(DEFAULT_BASE_URL) + ",\n"
                + "\n"
                + "  // Bearer token for the OpenWebUI API (Settings -> Account -> API Keys).\n"
                + "  \"openwebui_api_key\": " + MiniJson.quote("your-api-key-here") + ",\n"
                + "\n"
                + "  // Model ID exactly as it appears in OpenWebUI (e.g. \\\"llama3.1:8b\\\").\n"
                + "  \"openwebui_model\": " + MiniJson.quote("your-model-name-here") + ",\n"
                + "\n"
                + "  // Send features.web_search=true with every request.\n"
                + "  \"web_search_enabled\": " + DEFAULT_WEB_SEARCH + ",\n"
                + "\n"
                + "  // Responses longer than this many characters are truncated in the terminal display.\n"
                + "  \"max_response_length\": " + DEFAULT_MAX_RESPONSE_LENGTH + ",\n"
                + "\n"
                + "  // System prompt injected as the first message of every request.\n"
                + "  \"system_prompt\": " + MiniJson.quote(DEFAULT_SYSTEM_PROMPT) + "\n"
                + "}\n";
    }
}
