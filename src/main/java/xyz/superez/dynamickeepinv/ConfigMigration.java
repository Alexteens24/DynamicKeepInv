package xyz.superez.dynamickeepinv;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class ConfigMigration {

    private final JavaPlugin plugin;

    public ConfigMigration(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkAndMigrate() {
        migrateFile("config.yml");
        migrateFile("messages.yml");
    }

    private void migrateFile(String filename) {
        File file = new File(plugin.getDataFolder(), filename);
        if (!file.exists()) {
            plugin.saveResource(filename, false);
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        FileConfiguration defConfig;

        try (InputStream defConfigStream = plugin.getResource(filename)) {
            if (defConfigStream == null) {
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)) {
                defConfig = YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load default config for " + filename, e);
            return;
        }

        boolean changed = false;
        int addedKeys = 0;
        int removedKeys = 0;

        if ("config.yml".equals(filename)) {
            if (ConfigLayoutMigration.migrateLegacyLayoutToV9(config, plugin.getLogger())) {
                changed = true;
            }
            if (ConfigLayoutMigration.migrateScheduleMilestoneKeysV10(config, plugin.getLogger())) {
                changed = true;
            }
            if (ConfigLayoutMigration.migrateScheduleToMilestonesV11(config, plugin.getLogger())) {
                changed = true;
            }
        }

        // Check for missing keys (Add)
        for (String key : defConfig.getKeys(true)) {
            if (!config.contains(key)) {
                config.set(key, defConfig.get(key));
                changed = true;
                addedKeys++;
                plugin.getLogger().info("Added missing key to " + filename + ": " + key);
            }
        }

        // Check for extra keys (Remove) — snapshot keys after merge so layout migration is reflected
        Set<String> userKeys = new HashSet<>(config.getKeys(true));
        for (String key : userKeys) {
            // We need to check if the key is present in the default config.
            // However, we must be careful not to remove keys that might be valid but not in default (e.g., dynamic keys like worlds.overrides.world_name)

            // Special handling for dynamic sections
            if (filename.equals("config.yml")) {
                if (key.startsWith("worlds.overrides")) {
                    continue; // Skip dynamic world overrides
                }
                // Legacy nested key — keep so old configs are not stripped on migrate
                if ("messages.format".equals(key)) {
                    continue;
                }
            }

            if (!defConfig.contains(key)) {
                config.set(key, null);
                changed = true;
                removedKeys++;
                plugin.getLogger().info("Removed deprecated/unknown key from " + filename + ": " + key);
            }
        }

        // Special handling for config version
        if (filename.equals("config.yml")) {
            int currentVersion = config.getInt("config-version", 0);
            int newVersion = defConfig.getInt("config-version", 0);
            if (currentVersion < newVersion) {
                config.set("config-version", newVersion);
                changed = true;
                plugin.getLogger().info("Updated " + filename + " version from " + currentVersion + " to " + newVersion);
            }
        }

        if (changed) {
            try {
                config.save(file);
                plugin.getLogger().info("Successfully migrated " + filename + ".");
                if (addedKeys > 0 || removedKeys > 0) {
                     plugin.getLogger().warning("Config updated: Added " + addedKeys + " keys, Removed " + removedKeys + " keys.");
                     plugin.getLogger().warning("Please review your " + filename + " to ensure settings are correct.");
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save migrated " + filename, e);
            }
        }

        if (filename.equals("config.yml")) {
            validateConfig(config);
        }
    }

    /**
     * Validate config values and warn on out-of-range or nonsensical settings.
     * Does not modify the config — only logs warnings.
     */
    private void validateConfig(FileConfiguration config) {
        boolean ok = true;

        String rulesForSchedule = ConfigReadCompat.rulesRoot(config);
        List<ScheduleSupport.ScheduleMilestone> milestones = ScheduleSupport.loadMilestones(config, rulesForSchedule);
        if (!ScheduleSupport.validateMilestones(milestones, plugin.getLogger())) {
            ok = false;
        }

        int checkInterval = ConfigReadCompat.firstInt(config, "plugin.check-interval", "check-interval", 100);
        if (checkInterval <= 0) {
            plugin.getLogger().warning("[Config] plugin.check-interval must be > 0, got: " + checkInterval);
            ok = false;
        }

        double cost = config.getDouble("economy.cost", 0.0);
        if (cost < 0) {
            plugin.getLogger().warning("[Config] economy.cost must be >= 0, got: " + cost);
            ok = false;
        }

        long guiTimeout = config.getLong("economy.gui.timeout", 30);
        if (guiTimeout <= 0) {
            plugin.getLogger().warning("[Config] economy.gui.timeout must be > 0, got: " + guiTimeout);
            ok = false;
        }

        long guiExpire = config.getLong("economy.gui.expire-time", 300);
        if (guiExpire <= 0) {
            plugin.getLogger().warning("[Config] economy.gui.expire-time must be > 0, got: " + guiExpire);
            ok = false;
        }

        String mode = config.getString("economy.mode", "charge-to-keep");
        if (mode != null && !mode.equalsIgnoreCase("charge-to-keep")
                && !mode.equalsIgnoreCase("charge-to-bypass")
                && !mode.equalsIgnoreCase("gui")) {
            plugin.getLogger().warning("[Config] economy.mode must be 'charge-to-keep', 'charge-to-bypass', or 'gui'. Got: " + mode);
            ok = false;
        }

        String msgFormat = firstNonBlank(config.getString("message-format"), config.getString("messages.format"));
        if (msgFormat != null) {
            String f = msgFormat.trim().toLowerCase();
            if (!f.equals("legacy")
                    && !f.equals("minimessage")
                    && !f.equals("mini")
                    && !f.equals("mm")) {
                plugin.getLogger().warning("[Config] message-format (or messages.format) must be 'legacy' or 'minimessage' (aliases: mini, mm). Got: " + msgFormat);
                ok = false;
            }
        }

        if (ok) {
            plugin.getLogger().info("[Config] Validation passed.");
        } else {
            plugin.getLogger().warning("[Config] One or more config values are invalid. Plugin will attempt to continue with defaults where possible.");
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}

