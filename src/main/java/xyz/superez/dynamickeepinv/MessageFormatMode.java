package xyz.superez.dynamickeepinv;

/**
 * How user-facing strings (for example from {@code messages.yml}) are parsed into Adventure components.
 * Primary config key: {@code message-format} (root of {@code config.yml}). Fallback: {@code messages.format}.
 */
public enum MessageFormatMode {
    /** {@code &} color codes (legacy ampersand serializer). */
    LEGACY,
    /** MiniMessage tags, e.g. {@code <red>Hello</red>}. */
    MINIMESSAGE;

    public static MessageFormatMode fromConfig(String raw) {
        if (raw == null) {
            return LEGACY;
        }
        return switch (raw.trim().toLowerCase()) {
            case "minimessage", "mini", "mm" -> MINIMESSAGE;
            default -> LEGACY;
        };
    }
}
