package xyz.superez.dynamickeepinv;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * One-time in-place upgrade of {@code config.yml} from the legacy layout to v9
 * ({@code plugin.*}, {@code schedule.*}, {@code death-rules.*}, {@code broadcast.*}, …).
 */
public final class ConfigLayoutMigration {

    private ConfigLayoutMigration() {}

    /**
     * @return true if the configuration was modified
     */
    public static boolean migrateLegacyLayoutToV9(FileConfiguration c, Logger log) {
        if (!hasLegacyLayout(c)) {
            return false;
        }

        boolean changed = false;

        changed |= moveBool(c, "enabled", "plugin.enabled");
        changed |= moveBool(c, "debug", "plugin.debug");
        changed |= moveInt(c, "check-interval", "plugin.check-interval");

        changed |= moveLong(c, "time.day-start", "schedule.switch-to-day-rules-at");
        changed |= moveLong(c, "time.night-start", "schedule.switch-to-night-rules-at");
        changed |= moveLong(c, "time.triggers.day", "schedule.broadcast-day-crossing-at");
        changed |= moveLong(c, "time.triggers.night", "schedule.broadcast-night-crossing-at");
        clearSection(c, "time");

        if (c.getConfigurationSection("rules") != null) {
            if (c.getConfigurationSection("death-rules") == null) {
                c.set("death-rules", c.get("rules"));
                changed = true;
            }
            c.set("rules", null);
            changed = true;
        }

        changed |= moveBool(c, "messages.broadcast.enabled", "broadcast.enabled");
        changed |= moveStr(c, "messages.broadcast.permission", "broadcast.permission");
        changed |= moveBool(c, "messages.broadcast.events.day-change", "broadcast.notify-day");
        changed |= moveBool(c, "messages.broadcast.events.night-change", "broadcast.notify-night");
        changed |= moveBool(c, "messages.broadcast.display.chat", "broadcast.use-chat");
        changed |= moveBool(c, "messages.broadcast.display.action-bar", "broadcast.use-action-bar");
        changed |= moveBool(c, "messages.broadcast.display.title", "broadcast.use-title");
        changed |= moveBool(c, "messages.broadcast.sound.enabled", "broadcast.sound.enabled");
        changed |= moveStr(c, "messages.broadcast.sound.day", "broadcast.sound.day");
        changed |= moveStr(c, "messages.broadcast.sound.night", "broadcast.sound.night");
        clearSection(c, "messages.broadcast");

        changed |= moveBool(c, "messages.death.enabled", "death-feedback.enabled");
        changed |= moveBool(c, "messages.death.chat", "death-feedback.chat");
        changed |= moveBool(c, "messages.death.action-bar", "death-feedback.action-bar");
        clearSection(c, "messages.death");

        pruneEmptySection(c, "messages");

        String topFmt = c.getString("message-format");
        if ((topFmt == null || topFmt.isBlank()) && c.isSet("messages.format")) {
            c.set("message-format", c.get("messages.format"));
            changed = true;
        }
        if (c.isSet("messages.format")) {
            c.set("messages.format", null);
            changed = true;
        }
        pruneEmptySection(c, "messages");

        if (c.getConfigurationSection("hooks.mmoitems") != null) {
            if (c.getConfigurationSection("mmoitems") == null) {
                c.set("mmoitems", c.get("hooks.mmoitems"));
                changed = true;
            }
            c.set("hooks.mmoitems", null);
            changed = true;
        }
        pruneEmptySection(c, "hooks");

        if (changed && log != null) {
            log.info("[Config] Migrated config layout to v9 (plugin / schedule / death-rules / broadcast / death-feedback / mmoitems).");
        }
        return changed;
    }

    /**
     * Renames v9 {@code schedule.day-start} / {@code night-start} / {@code triggers} to v10 milestone keys.
     */
    public static boolean migrateScheduleMilestoneKeysV10(FileConfiguration c, Logger log) {
        if (!c.isConfigurationSection("schedule")) {
            return false;
        }
        boolean changed = false;
        if (c.isSet("schedule.day-start") && !c.isSet("schedule.switch-to-day-rules-at")) {
            c.set("schedule.switch-to-day-rules-at", c.getLong("schedule.day-start"));
            c.set("schedule.day-start", null);
            changed = true;
        }
        if (c.isSet("schedule.night-start") && !c.isSet("schedule.switch-to-night-rules-at")) {
            c.set("schedule.switch-to-night-rules-at", c.getLong("schedule.night-start"));
            c.set("schedule.night-start", null);
            changed = true;
        }
        ConfigurationSection trig = c.getConfigurationSection("schedule.triggers");
        if (trig != null) {
            if (!c.isSet("schedule.broadcast-day-crossing-at") && trig.isSet("day")) {
                c.set("schedule.broadcast-day-crossing-at", trig.getLong("day"));
                changed = true;
            }
            if (!c.isSet("schedule.broadcast-night-crossing-at") && trig.isSet("night")) {
                c.set("schedule.broadcast-night-crossing-at", trig.getLong("night"));
                changed = true;
            }
            c.set("schedule.triggers", null);
            changed = true;
        }
        if (changed && log != null) {
            log.info("[Config] schedule: migrated to milestone keys (switch-to-*-rules-at, broadcast-*-crossing-at).");
        }
        return changed;
    }

    /**
     * Builds {@code schedule.milestones} from legacy two-boundary schedule + {@code death-rules.day}/{@code .night},
     * then removes those legacy keys.
     */
    public static boolean migrateScheduleToMilestonesV11(FileConfiguration c, Logger log) {
        List<?> existing = c.getList("schedule.milestones");
        if (existing != null && !existing.isEmpty()) {
            return false;
        }
        String rules = c.getConfigurationSection("death-rules") != null ? "death-rules" : "rules";
        long dayAt = ConfigReadCompat.firstLong(c,
                "schedule.switch-to-day-rules-at", "schedule.day-start", "time.day-start", 0);
        long nightAt = ConfigReadCompat.firstLong(c,
                "schedule.switch-to-night-rules-at", "schedule.night-start", "time.night-start", 13000);
        dayAt = ScheduleSupport.normalizeWorldTime(dayAt);
        nightAt = ScheduleSupport.normalizeWorldTime(nightAt);
        boolean dKi = c.getBoolean(rules + ".day.keep-items", true);
        boolean dKx = c.getBoolean(rules + ".day.keep-xp", true);
        boolean nKi = c.getBoolean(rules + ".night.keep-items", false);
        boolean nKx = c.getBoolean(rules + ".night.keep-xp", false);

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(milestoneRow(dayAt, dKi, dKx, true));
        rows.add(milestoneRow(nightAt, nKi, nKx, true));
        c.set("schedule.milestones", rows);

        c.set("schedule.switch-to-day-rules-at", null);
        c.set("schedule.switch-to-night-rules-at", null);
        c.set("schedule.day-start", null);
        c.set("schedule.night-start", null);
        c.set("schedule.triggers", null);
        c.set("schedule.broadcast-day-crossing-at", null);
        c.set("schedule.broadcast-night-crossing-at", null);
        c.set(rules + ".day", null);
        c.set(rules + ".night", null);

        if (!c.isSet("broadcast.period-change")) {
            boolean nd = ConfigReadCompat.firstBool(c, "broadcast.notify-day", "messages.broadcast.events.day-change", true);
            boolean nn = ConfigReadCompat.firstBool(c, "broadcast.notify-night", "messages.broadcast.events.night-change", true);
            c.set("broadcast.period-change", nd || nn);
        }
        c.set("broadcast.notify-day", null);
        c.set("broadcast.notify-night", null);

        if (log != null) {
            log.info("[Config] Converted schedule to schedule.milestones (world-time segments).");
        }
        return true;
    }

    private static Map<String, Object> milestoneRow(long at, boolean keepItems, boolean keepXp, boolean announce) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("at", at);
        m.put("keep-items", keepItems);
        m.put("keep-xp", keepXp);
        m.put("announce", announce);
        return m;
    }

    private static boolean hasLegacyLayout(FileConfiguration c) {
        if (c.getConfigurationSection("time") != null) {
            return true;
        }
        if (c.getConfigurationSection("rules") != null) {
            return true;
        }
        if (c.getConfigurationSection("hooks.mmoitems") != null) {
            return true;
        }
        if (c.getConfigurationSection("messages.broadcast") != null
                || c.getConfigurationSection("messages.death") != null) {
            return true;
        }
        if (c.isSet("messages.format")) {
            return true;
        }
        if (c.isSet("enabled")) {
            return true;
        }
        if (c.isSet("check-interval") && !c.isSet("plugin.check-interval")) {
            return true;
        }
        return c.isSet("debug") && !c.isSet("plugin.debug");
    }

    private static boolean moveBool(FileConfiguration c, String from, String to) {
        if (!c.isSet(from)) {
            return false;
        }
        if (!c.isSet(to)) {
            c.set(to, c.getBoolean(from));
        }
        c.set(from, null);
        return true;
    }

    private static boolean moveInt(FileConfiguration c, String from, String to) {
        if (!c.isSet(from)) {
            return false;
        }
        if (!c.isSet(to)) {
            c.set(to, c.getInt(from));
        }
        c.set(from, null);
        return true;
    }

    private static boolean moveLong(FileConfiguration c, String from, String to) {
        if (!c.isSet(from)) {
            return false;
        }
        if (!c.isSet(to)) {
            c.set(to, c.getLong(from));
        }
        c.set(from, null);
        return true;
    }

    private static boolean moveStr(FileConfiguration c, String from, String to) {
        if (!c.isSet(from)) {
            return false;
        }
        if (!c.isSet(to)) {
            c.set(to, c.getString(from));
        }
        c.set(from, null);
        return true;
    }

    private static void clearSection(FileConfiguration c, String path) {
        if (c.isSet(path)) {
            c.set(path, null);
        }
    }

    private static void pruneEmptySection(FileConfiguration c, String path) {
        if (!c.isConfigurationSection(path)) {
            return;
        }
        ConfigurationSection sec = c.getConfigurationSection(path);
        if (sec != null && sec.getKeys(false).isEmpty()) {
            c.set(path, null);
        }
    }
}
