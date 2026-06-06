package com.aiterminal.common.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny, dependency-free JSON reader/writer.
 *
 * <p>Supports the subset of JSON the mod needs: objects, arrays, strings, numbers,
 * booleans and null. As a convenience for the config file it also tolerates
 * {@code //} line comments and {@code /* *}{@code /} block comments, stripping them
 * before parsing so the on-disk config can be self-documenting while staying readable
 * by this parser.</p>
 *
 * <p>Parsed values map to: {@link Map}&lt;String,Object&gt; (objects), {@link List}&lt;Object&gt;
 * (arrays), {@link String}, {@link Double} (numbers), {@link Boolean} and {@code null}.</p>
 */
public final class MiniJson {

    private final String src;
    private int pos;

    private MiniJson(String src) {
        this.src = src;
    }

    /** Parses a JSON document (comments allowed). Throws {@link JsonException} on malformed input. */
    public static Object parse(String text) {
        if (text == null) {
            throw new JsonException("input is null");
        }
        MiniJson p = new MiniJson(stripComments(text));
        p.skipWhitespace();
        Object value = p.readValue();
        p.skipWhitespace();
        if (p.pos < p.src.length()) {
            throw new JsonException("trailing content at index " + p.pos);
        }
        return value;
    }

    /** Convenience: parse and cast to an object map. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object v = parse(text);
        if (!(v instanceof Map)) {
            throw new JsonException("expected a JSON object at the top level");
        }
        return (Map<String, Object>) v;
    }

    // ---- Parsing ---------------------------------------------------------

    private Object readValue() {
        skipWhitespace();
        if (pos >= src.length()) {
            throw new JsonException("unexpected end of input");
        }
        char c = src.charAt(pos);
        switch (c) {
            case '{':
                return readObject();
            case '[':
                return readArray();
            case '"':
                return readString();
            case 't':
            case 'f':
                return readBoolean();
            case 'n':
                return readNull();
            default:
                if (c == '-' || (c >= '0' && c <= '9')) {
                    return readNumber();
                }
                throw new JsonException("unexpected character '" + c + "' at index " + pos);
        }
    }

    private Map<String, Object> readObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        expect('{');
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return map;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw new JsonException("expected string key at index " + pos);
            }
            String key = readString();
            skipWhitespace();
            expect(':');
            Object value = readValue();
            map.put(key, value);
            skipWhitespace();
            char c = next();
            if (c == ',') {
                continue;
            }
            if (c == '}') {
                return map;
            }
            throw new JsonException("expected ',' or '}' at index " + (pos - 1));
        }
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            list.add(readValue());
            skipWhitespace();
            char c = next();
            if (c == ',') {
                continue;
            }
            if (c == ']') {
                return list;
            }
            throw new JsonException("expected ',' or ']' at index " + (pos - 1));
        }
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= src.length()) {
                throw new JsonException("unterminated string");
            }
            char c = src.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                if (pos >= src.length()) {
                    throw new JsonException("unterminated escape");
                }
                char esc = src.charAt(pos++);
                switch (esc) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (pos + 4 > src.length()) {
                            throw new JsonException("invalid unicode escape");
                        }
                        String hex = src.substring(pos, pos + 4);
                        pos += 4;
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException e) {
                            throw new JsonException("invalid unicode escape \\u" + hex);
                        }
                        break;
                    default:
                        throw new JsonException("invalid escape \\" + esc);
                }
            } else {
                sb.append(c);
            }
        }
    }

    private Double readNumber() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                pos++;
            } else {
                break;
            }
        }
        String num = src.substring(start, pos);
        try {
            return Double.parseDouble(num);
        } catch (NumberFormatException e) {
            throw new JsonException("invalid number '" + num + "'");
        }
    }

    private Boolean readBoolean() {
        if (src.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (src.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        throw new JsonException("invalid literal at index " + pos);
    }

    private Object readNull() {
        if (src.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw new JsonException("invalid literal at index " + pos);
    }

    // ---- Helpers ---------------------------------------------------------

    private void skipWhitespace() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }

    private char peek() {
        if (pos >= src.length()) {
            throw new JsonException("unexpected end of input");
        }
        return src.charAt(pos);
    }

    private char next() {
        if (pos >= src.length()) {
            throw new JsonException("unexpected end of input");
        }
        return src.charAt(pos++);
    }

    private void expect(char c) {
        char actual = next();
        if (actual != c) {
            throw new JsonException("expected '" + c + "' but found '" + actual + "' at index " + (pos - 1));
        }
    }

    /** Removes {@code //} line comments and block comments, ignoring occurrences inside strings. */
    static String stripComments(String text) {
        StringBuilder out = new StringBuilder(text.length());
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                out.append(c);
                if (c == '\\' && i + 1 < text.length()) {
                    out.append(text.charAt(++i));
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                out.append(c);
            } else if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
                i += 2;
                while (i < text.length() && text.charAt(i) != '\n') {
                    i++;
                }
                if (i < text.length()) {
                    out.append('\n');
                }
            } else if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < text.length() && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) {
                    i++;
                }
                i += 1; // loop ++ skips the closing '/'
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    // ---- Writing ---------------------------------------------------------

    /** Escapes a string for inclusion in a JSON document (without surrounding quotes). */
    public static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /** Wraps {@link #escape(String)} in quotes. */
    public static String quote(String s) {
        return '"' + escape(s) + '"';
    }

    public static final class JsonException extends RuntimeException {
        public JsonException(String message) {
            super(message);
        }
    }
}
