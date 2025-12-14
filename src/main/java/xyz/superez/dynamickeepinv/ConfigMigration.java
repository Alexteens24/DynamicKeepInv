package xyz.superez.dynamickeepinv;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

        // Check for missing keys
        for (String key : defConfig.getKeys(true)) {
            if (!config.contains(key)) {
                config.set(key, defConfig.get(key));
                changed = true;
                plugin.getLogger().info("Added missing key to " + filename + ": " + key);
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
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save migrated " + filename, e);
            }
        }
    }
}
