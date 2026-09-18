package io.github.renatoxico.extract.logging;

public final class LogFields {
    private static final int LIMIT = 4096;
    private static final String TRUNCATED = "...[truncated]";

    private LogFields() {}

    /** Keep untrusted diagnostic fields on one line and bound their encoded size. */
    public static String safe(Object value) {
        if (value == null) return "<unknown>";
        String text = value.toString();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String encoded = switch (c) {
                case '\n' -> "\\n";
                case '\r' -> "\\r";
                case '\t' -> "\\t";
                case '\\' -> "\\\\";
                default -> Character.isISOControl(c) || c == '\u2028' || c == '\u2029'
                    ? String.format("\\u%04x", (int) c) : String.valueOf(c);
            };
            if (result.length() + encoded.length() > LIMIT) {
                result.setLength(Math.min(result.length(), LIMIT - TRUNCATED.length()));
                return result.append(TRUNCATED).toString();
            }
            result.append(encoded);
        }
        return result.toString();
    }
}
