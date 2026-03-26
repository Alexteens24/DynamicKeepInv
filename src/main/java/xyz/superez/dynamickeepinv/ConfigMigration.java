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

        // Check for missing keys (Add)
        for (String key : defConfig.getKeys(true)) {
            if (!config.contains(key)) {
                config.set(key, defConfig.get(key));
                changed = true;
                addedKeys++;
                plugin.getLogger().info("Added missing key to " + filename + ": " + key);
            }
        }

        // Check for extra keys (Remove)
        // We iterate over the user's config keys and check if they exist in the default config.
        // Note: getKeys(true) returns all keys (nested).
        Set<String> userKeys = new HashSet<>(config.getKeys(true));
        for (String key : userKeys) {
            // We need to check if the key is present in the default config.
            // However, we must be careful not to remove keys that might be valid but not in default (e.g., dynamic keys like worlds.overrides.world_name)

            // Special handling for dynamic sections
            if (filename.equals("config.yml")) {
                if (key.startsWith("worlds.overrides")) {
                    continue; // Skip dynamic world overrides
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

        long dayStart   = config.getLong("time.day-start",   0);
        long nightStart = config.getLong("time.night-start", 13000);
        if (dayStart < 0 || dayStart > 24000) {
            plugin.getLogger().warning("[Config] time.day-start must be between 0 and 24000, got: " + dayStart);
            ok = false;
        }
        if (nightStart < 0 || nightStart > 24000) {
            plugin.getLogger().warning("[Config] time.night-start must be between 0 and 24000, got: " + nightStart);
            ok = false;
        }
        if (dayStart >= nightStart) {
            plugin.getLogger().warning("[Config] time.day-start (" + dayStart + ") should be less than time.night-start (" + nightStart + ").");
            ok = false;
        }

        long dayTrigger   = config.getLong("time.triggers.day",   -1);
        long nightTrigger = config.getLong("time.triggers.night", -1);
        if (dayTrigger != -1 && (dayTrigger < 0 || dayTrigger > 24000)) {
            plugin.getLogger().warning("[Config] time.triggers.day must be -1 or between 0 and 24000, got: " + dayTrigger);
            ok = false;
        }
        if (nightTrigger != -1 && (nightTrigger < 0 || nightTrigger > 24000)) {
            plugin.getLogger().warning("[Config] time.triggers.night must be -1 or between 0 and 24000, got: " + nightTrigger);
            ok = false;
        }

        int checkInterval = config.getInt("check-interval", 100);
        if (checkInterval <= 0) {
            plugin.getLogger().warning("[Config] check-interval must be > 0, got: " + checkInterval);
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

        if (ok) {
            plugin.getLogger().info("[Config] Validation passed.");
        } else {
            plugin.getLogger().warning("[Config] One or more config values are invalid. Plugin will attempt to continue with defaults where possible.");
        }
    }
}

