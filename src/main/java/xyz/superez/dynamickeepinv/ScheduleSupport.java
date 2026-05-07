package xyz.superez.dynamickeepinv;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * World time is 0–23999 (24000-length cycle). {@link ScheduleMilestone}s are sorted by {@code at};
 * the active segment is {@code [milestone[i].at, milestone[i+1].at)} with wrap (last segment runs to first {@code at}).
 */
public final class ScheduleSupport {

    public record ScheduleMilestone(long at, boolean keepItems, boolean keepXp, boolean announce) {}

    private ScheduleSupport() {}

    public static long normalizeWorldTime(long time) {
        long t = time % 24000L;
        if (t < 0) {
            t += 24000L;
        }
        return t;
    }

    /**
     * Index of the milestone that starts the segment containing {@code worldTime}.
     */
    public static int segmentIndex(long worldTime, List<ScheduleMilestone> sorted) {
        int n = sorted.size();
        if (n == 0) {
            return 0;
        }
        long t = normalizeWorldTime(worldTime);
        int best = -1;
        for (int i = 0; i < n; i++) {
            if (sorted.get(i).at() <= t) {
                best = i;
            }
        }
        if (best < 0) {
            return n - 1;
        }
        return best;
    }

    /**
     * Loads {@code schedule.milestones}, or builds a two-segment schedule from legacy keys.
     */
    public static List<ScheduleMilestone> loadMilestones(FileConfiguration cfg, String rulesRoot) {
        List<?> raw = cfg.getList("schedule.milestones");
        if (raw != null && !raw.isEmpty()) {
            List<ScheduleMilestone> out = new ArrayList<>();
            for (Object o : raw) {
                if (!(o instanceof Map<?, ?> map)) {
                    continue;
                }
                long at = toLong(map.get("at"), 0L);
                at = normalizeWorldTime(at);
                boolean ki = toBool(map.get("keep-items"), true);
                boolean kx = toBool(map.get("keep-xp"), true);
                boolean ann = toBool(map.get("announce"), true);
                out.add(new ScheduleMilestone(at, ki, kx, ann));
            }
            if (!out.isEmpty()) {
                out.sort(Comparator.comparingLong(ScheduleMilestone::at));
                return List.copyOf(out);
            }
        }
        return defaultTwoSegmentFromLegacy(cfg, rulesRoot);
    }

    private static List<ScheduleMilestone> defaultTwoSegmentFromLegacy(FileConfiguration cfg, String rulesRoot) {
        long dayAt = ConfigReadCompat.firstLong(cfg,
                "schedule.switch-to-day-rules-at", "schedule.day-start", "time.day-start", 0);
        long nightAt = ConfigReadCompat.firstLong(cfg,
                "schedule.switch-to-night-rules-at", "schedule.night-start", "time.night-start", 13000);
        dayAt = normalizeWorldTime(dayAt);
        nightAt = normalizeWorldTime(nightAt);
        boolean dKi = cfg.getBoolean(rulesRoot + ".day.keep-items", true);
        boolean dKx = cfg.getBoolean(rulesRoot + ".day.keep-xp", true);
        boolean nKi = cfg.getBoolean(rulesRoot + ".night.keep-items", false);
        boolean nKx = cfg.getBoolean(rulesRoot + ".night.keep-xp", false);
        List<ScheduleMilestone> pair = new ArrayList<>(2);
        pair.add(new ScheduleMilestone(dayAt, dKi, dKx, true));
        pair.add(new ScheduleMilestone(nightAt, nKi, nKx, true));
        pair.sort(Comparator.comparingLong(ScheduleMilestone::at));
        return List.copyOf(pair);
    }

    private static long toLong(Object v, long def) {
        if (v == null) {
            return def;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(v.toString().trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean toBool(Object v, boolean def) {
        if (v == null) {
            return def;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof Number n) {
            return n.intValue() != 0;
        }
        String s = v.toString().trim().toLowerCase(Locale.ROOT);
        if (s.equals("true") || s.equals("yes") || s.equals("1")) {
            return true;
        }
        if (s.equals("false") || s.equals("no") || s.equals("0")) {
            return false;
        }
        return def;
    }

    /**
     * Validates milestone list; logs warnings, does not throw.
     */
    public static boolean validateMilestones(List<ScheduleMilestone> milestones, java.util.logging.Logger log) {
        if (milestones == null || milestones.isEmpty()) {
            if (log != null) {
                log.warning("[Config] schedule.milestones must contain at least one entry.");
            }
            return false;
        }
        if (milestones.size() > 32) {
            if (log != null) {
                log.warning("[Config] schedule.milestones has more than 32 entries; ignoring extras is not supported — trim the list.");
            }
            return false;
        }
        Set<Long> seen = new HashSet<>();
        for (ScheduleMilestone m : milestones) {
            if (m.at() < 0 || m.at() > 23999) {
                if (log != null) {
                    log.warning("[Config] schedule milestone at must be 0–23999, got: " + m.at());
                }
                return false;
            }
            if (!seen.add(m.at())) {
                if (log != null) {
                    log.warning("[Config] schedule.milestones: duplicate at=" + m.at());
                }
                return false;
            }
        }
        return true;
    }
}
