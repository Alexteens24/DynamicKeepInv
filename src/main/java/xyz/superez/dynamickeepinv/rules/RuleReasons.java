package xyz.superez.dynamickeepinv.rules;

public final class RuleReasons {
    private RuleReasons() {}

    // Core
    public static final String BYPASS           = "bypass";
    public static final String UNKNOWN          = "unknown";

    // Death cause
    public static final String PVP              = "pvp";
    public static final String PVE              = "pve";

    // Time-based
    public static final String TIME_DAY         = "time-day";
    public static final String TIME_NIGHT       = "time-night";

    // Lands integration
    public static final String LANDS_DEFER      = "lands-defer";
    public static final String LANDS_OWN        = "lands-own";
    public static final String LANDS_OTHER      = "lands-other";
    public static final String LANDS_WILDERNESS = "lands-wilderness";

    // GriefPrevention integration
    public static final String GP_OWN           = "gp-own";
    public static final String GP_OTHER         = "gp-other";
    public static final String GP_WILDERNESS    = "gp-wilderness";

    // Economy
    public static final String ECONOMY_BYPASS   = "economy-bypass";
    public static final String ECONOMY          = "economy";
}
