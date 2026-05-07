package xyz.superez.dynamickeepinv;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Reads config keys from the v9 layout first, then legacy paths, so old configs still work
 * before {@link ConfigLayoutMigration} runs.
 */
final class ConfigReadCompat {

    private ConfigReadCompat() {}

    static String rulesRoot(FileConfiguration c) {
        return c.getConfigurationSection("death-rules") != null ? "death-rules" : "rules";
    }

    static boolean firstBool(FileConfiguration c, String primary, String fallback, boolean def) {
        if (c.isSet(primary)) {
            return c.getBoolean(primary);
        }
        if (fallback != null && c.isSet(fallback)) {
            return c.getBoolean(fallback);
        }
        return def;
    }

    static int firstInt(FileConfiguration c, String primary, String fallback, int def) {
        if (c.isSet(primary)) {
            return c.getInt(primary);
        }
        if (fallback != null && c.isSet(fallback)) {
            return c.getInt(fallback);
        }
        return def;
    }

    static long firstLong(FileConfiguration c, String primary, String fallback, long def) {
        if (c.isSet(primary)) {
            return c.getLong(primary);
        }
        if (fallback != null && c.isSet(fallback)) {
            return c.getLong(fallback);
        }
        return def;
    }

    /** Try primary, then fb1, then fb2 (any may be null to skip). */
    static long firstLong(FileConfiguration c, String primary, String fb1, String fb2, long def) {
        if (c.isSet(primary)) {
            return c.getLong(primary);
        }
        if (fb1 != null && c.isSet(fb1)) {
            return c.getLong(fb1);
        }
        if (fb2 != null && c.isSet(fb2)) {
            return c.getLong(fb2);
        }
        return def;
    }

    static String firstString(FileConfiguration c, String primary, String fallback, String def) {
        if (c.isSet(primary)) {
            String v = c.getString(primary);
            return v != null ? v : def;
        }
        if (fallback != null && c.isSet(fallback)) {
            String v = c.getString(fallback);
            return v != null ? v : def;
        }
        return def;
    }

    static List<String> firstStringList(FileConfiguration c, String primary, String fallback) {
        if (c.isSet(primary)) {
            return c.getStringList(primary);
        }
        if (fallback != null && c.isSet(fallback)) {
            return c.getStringList(fallback);
        }
        return List.of();
    }
}
