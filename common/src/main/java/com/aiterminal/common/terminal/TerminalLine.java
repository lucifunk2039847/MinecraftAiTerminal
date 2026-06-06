package com.aiterminal.common.terminal;

/**
 * Encodes a single terminal history entry as a {@code String} (the block entity stores a
 * {@code List<String>}), tagging each line with its {@link Kind} so the screen can colour it.
 * Format: one tag character, a unit-separator, then the raw text.
 */
public final class TerminalLine {

    public enum Kind {
        USER('U'),
        RESPONSE('R'),
        ERROR('E'),
        INFO('I');

        private final char tag;

        Kind(char tag) {
            this.tag = tag;
        }

        static Kind fromTag(char c) {
            for (Kind k : values()) {
                if (k.tag == c) {
                    return k;
                }
            }
            return INFO;
        }
    }

    private static final char SEP = '\u001F';

    private TerminalLine() {
    }

    public static String encode(Kind kind, String text) {
        return kind.tag + String.valueOf(SEP) + (text == null ? "" : text);
    }

    public static Kind kindOf(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return Kind.INFO;
        }
        return Kind.fromTag(encoded.charAt(0));
    }

    public static String textOf(String encoded) {
        if (encoded == null) {
            return "";
        }
        int idx = encoded.indexOf(SEP);
        return idx >= 0 ? encoded.substring(idx + 1) : encoded;
    }
}
