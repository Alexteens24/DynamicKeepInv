package xyz.superez.dynamickeepinv;

/**
 * Economy operating mode as configured in {@code economy.mode}.
 */
public enum EconomyMode {
    CHARGE_TO_KEEP,
    CHARGE_TO_BYPASS,
    GUI;

    public static EconomyMode from(String raw) {
        if (raw == null) return CHARGE_TO_KEEP;
        return switch (raw.toLowerCase()) {
            case "charge-to-bypass" -> CHARGE_TO_BYPASS;
            case "gui"              -> GUI;
            default                 -> CHARGE_TO_KEEP;
        };
    }
}
