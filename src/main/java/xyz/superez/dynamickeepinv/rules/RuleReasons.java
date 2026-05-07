package xyz.superez.dynamickeepinv.rules;

public final class RuleReasons {
    private RuleReasons() {}

    // Core
    public static final String BYPASS           = "bypass";
    public static final String UNKNOWN          = "unknown";

    // Death cause
    public static final String PVP              = "pvp";
    public static final String PVE              = "pve";

    // Time-based (legacy reasons still accepted from older persisted data)
    public static final String TIME_DAY         = "time-day";
    public static final String TIME_NIGHT       = "time-night";

    /** Active schedule segment index from {@code schedule.milestones} (reason value is this prefix + index). */
    public static final String TIME_SEGMENT_PREFIX = "time-segment-";

    public static String timeSegmentReason(int index) {
        return TIME_SEGMENT_PREFIX + index;
    }

    // Lands integration
    public static final String LANDS_DEFER      = "lands-defer";
    public static final String LANDS_OWN        = "lands-own";
    public static final String LANDS_OTHER      = "lands-other";
    public static final String LANDS_WILDERNESS = "lands-wilderness";

    // GriefPrevention integration
    public static final String GP_OWN           = "gp-own";
    public static final String GP_OTHER         = "gp-other";
    public static final String GP_WILDERNESS    = "gp-wilderness";

    // WorldGuard integration
    public static final String WG_OWN           = "wg-own";
    public static final String WG_OTHER         = "wg-other";
    public static final String WG_WILDERNESS    = "wg-wilderness";

    // Towny integration
    public static final String TOWNY_OWN        = "towny-own";
    public static final String TOWNY_OTHER      = "towny-other";
    public static final String TOWNY_WILDERNESS = "towny-wilderness";

    // Economy
    public static final String ECONOMY_BYPASS   = "economy-bypass";
    public static final String ECONOMY          = "economy";

    // Special rules
    public static final String FIRST_DEATH      = "first-death";
    public static final String DEATH_STREAK     = "death-streak";

    /**
     * Maps a rule-reason string (typically a constant defined in this class) to the short bucket keys
     * used by stats / death breakdown (for example {@code day}, {@code lands}).
     */
    public static String normalizeForStats(String reason) {
        if (reason == null) {
            return UNKNOWN;
        }
        if (reason.startsWith(TIME_SEGMENT_PREFIX)) {
            return "schedule";
        }
        return switch (reason) {
            case TIME_DAY -> "day";
            case TIME_NIGHT -> "night";
            case PVP -> "pvp";
            case PVE -> "pve";
            case LANDS_OWN, LANDS_OTHER, LANDS_WILDERNESS, LANDS_DEFER -> "lands";
            case GP_OWN, GP_OTHER, GP_WILDERNESS -> "griefprevention";
            case WG_OWN, WG_OTHER, WG_WILDERNESS -> "worldguard";
            case TOWNY_OWN, TOWNY_OTHER, TOWNY_WILDERNESS -> "towny";
            case FIRST_DEATH -> "first-death";
            case DEATH_STREAK -> "death-streak";
            case BYPASS -> "bypass";
            case ECONOMY_BYPASS, ECONOMY -> "economy";
            default -> UNKNOWN;
        };
    }
}
