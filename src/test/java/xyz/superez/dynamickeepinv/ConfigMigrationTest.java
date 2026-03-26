package xyz.superez.dynamickeepinv;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ConfigMigrationTest {

    @TempDir
    Path tempDir;

    @Mock
    private JavaPlugin plugin;

    @Mock
    private Logger logger;

    private File dataFolder;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        dataFolder = tempDir.toFile();
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getLogger()).thenReturn(logger);
    }

    @Test
    public void testConfigMigration() throws Exception {
        // Create an old config file with an extra key
        File configFile = new File(dataFolder, "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("config-version: 1\n");
            writer.write("old-setting: true\n");
            writer.write("deprecated-setting: true\n");
        }

        // Mock default config resource
        String defaultConfigContent = "config-version: 2\n" +
                                      "new-setting: true\n" +
                                      "old-setting: false\n"; // Should not overwrite existing value
        InputStream defConfigStream = new ByteArrayInputStream(defaultConfigContent.getBytes(StandardCharsets.UTF_8));
        when(plugin.getResource("config.yml")).thenReturn(defConfigStream);

        // Mock messages.yml resource to avoid NPE/logging error (it returns null so it skips)
        when(plugin.getResource("messages.yml")).thenReturn(null);

        // Run migration
        ConfigMigration migration = new ConfigMigration(plugin);
        migration.checkAndMigrate();

        // Verify migration
        YamlConfiguration loadedConfig = YamlConfiguration.loadConfiguration(configFile);

        // Version should be updated
        assertEquals(2, loadedConfig.getInt("config-version"));

        // New setting should be added
        assertTrue(loadedConfig.contains("new-setting"));
        assertTrue(loadedConfig.getBoolean("new-setting"));

        // Old setting should be preserved (value true from file, not false from default)
        assertTrue(loadedConfig.getBoolean("old-setting"));

        // Deprecated setting should be removed because it's not in default config
        assertFalse(loadedConfig.contains("deprecated-setting"));
    }

    @Test
    public void testConfigMigrationPreservesWorldOverrides() throws Exception {
        // Create an config with dynamic world overrides
        File configFile = new File(dataFolder, "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("config-version: 2\n");
            writer.write("worlds:\n  overrides:\n    my_custom_world:\n      day: true\n");
        }

        // Mock default config resource
        String defaultConfigContent = "config-version: 2\n" +
                                      "worlds:\n  overrides: {}\n";
        InputStream defConfigStream = new ByteArrayInputStream(defaultConfigContent.getBytes(StandardCharsets.UTF_8));
        when(plugin.getResource("config.yml")).thenReturn(defConfigStream);

        when(plugin.getResource("messages.yml")).thenReturn(null);

        // Run migration
        ConfigMigration migration = new ConfigMigration(plugin);
        migration.checkAndMigrate();

        // Verify migration
        YamlConfiguration loadedConfig = YamlConfiguration.loadConfiguration(configFile);

        // Dynamic key should be preserved
        assertTrue(loadedConfig.contains("worlds.overrides.my_custom_world.day"));
    }

    @Test
    public void testMessagesMigration() throws Exception {
        // Create an old messages file with extra key
        File messagesFile = new File(dataFolder, "messages.yml");
        try (FileWriter writer = new FileWriter(messagesFile)) {
            writer.write("messages:\n  en:\n    existing: \"Old\"\n    deprecated: \"Bye\"\n");
        }

        // Mock default messages resource
        String defaultMessagesContent = "messages:\n  en:\n    existing: \"New\"\n    new: \"New Message\"\n";
        InputStream defMessagesStream = new ByteArrayInputStream(defaultMessagesContent.getBytes(StandardCharsets.UTF_8));
        when(plugin.getResource("messages.yml")).thenReturn(defMessagesStream);

        // Mock config.yml resource
        when(plugin.getResource("config.yml")).thenReturn(null);

        // Run migration
        ConfigMigration migration = new ConfigMigration(plugin);

        // Create dummy config.yml so migrateFile("config.yml") doesn't try to saveResource
        File configFile = new File(dataFolder, "config.yml");
        configFile.createNewFile();

        migration.checkAndMigrate();

        // Verify migration
        YamlConfiguration loadedMessages = YamlConfiguration.loadConfiguration(messagesFile);

        // New message should be added
        assertEquals("New Message", loadedMessages.getString("messages.en.new"));

        // Existing message should be preserved
        assertEquals("Old", loadedMessages.getString("messages.en.existing"));

        // Deprecated message should be removed
        assertFalse(loadedMessages.contains("messages.en.deprecated"));
    }

    @Test
    public void testConfigValidationPassesForSaneValues() throws Exception {
        File configFile = new File(dataFolder, "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("config-version: 6\n");
            writer.write("time:\n  day-start: 0\n  night-start: 13000\n  triggers:\n    day: -1\n    night: -1\n");
            writer.write("check-interval: 100\n");
            writer.write("economy:\n  cost: 10\n  mode: gui\n  gui:\n    timeout: 30\n    expire-time: 300\n");
        }

        String defaultConfigContent = "config-version: 6\n" +
                "time:\n  day-start: 0\n  night-start: 13000\n  triggers:\n    day: -1\n    night: -1\n" +
                "check-interval: 100\n" +
                "economy:\n  cost: 10\n  mode: gui\n  gui:\n    timeout: 30\n    expire-time: 300\n";
        when(plugin.getResource("config.yml")).thenReturn(new ByteArrayInputStream(defaultConfigContent.getBytes(StandardCharsets.UTF_8)));
        when(plugin.getResource("messages.yml")).thenReturn(null);

        new ConfigMigration(plugin).checkAndMigrate();

        verify(logger).info("[Config] Validation passed.");
        verify(logger, never()).warning(contains("[Config] One or more config values are invalid"));
    }

    @Test
    public void testConfigValidationWarnsForInvalidValues() throws Exception {
        File configFile = new File(dataFolder, "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("config-version: 6\n");
            writer.write("time:\n  day-start: 14000\n  night-start: 13000\n  triggers:\n    day: 25000\n    night: -2\n");
            writer.write("check-interval: 0\n");
            writer.write("economy:\n  cost: -1\n  mode: bad-mode\n  gui:\n    timeout: 0\n    expire-time: 0\n");
        }

        String defaultConfigContent = "config-version: 6\n" +
                "time:\n  day-start: 0\n  night-start: 13000\n  triggers:\n    day: -1\n    night: -1\n" +
                "check-interval: 100\n" +
                "economy:\n  cost: 10\n  mode: charge-to-keep\n  gui:\n    timeout: 30\n    expire-time: 300\n";
        when(plugin.getResource("config.yml")).thenReturn(new ByteArrayInputStream(defaultConfigContent.getBytes(StandardCharsets.UTF_8)));
        when(plugin.getResource("messages.yml")).thenReturn(null);

        new ConfigMigration(plugin).checkAndMigrate();

        verify(logger).warning(contains("time.day-start (14000) should be less than time.night-start (13000)"));
        verify(logger).warning(contains("time.triggers.day must be -1 or between 0 and 24000"));
        verify(logger).warning(contains("time.triggers.night must be -1 or between 0 and 24000"));
        verify(logger).warning(contains("check-interval must be > 0"));
        verify(logger).warning(contains("economy.cost must be >= 0"));
        verify(logger).warning(contains("economy.gui.timeout must be > 0"));
        verify(logger).warning(contains("economy.gui.expire-time must be > 0"));
        verify(logger).warning(contains("economy.mode must be 'charge-to-keep', 'charge-to-bypass', or 'gui'"));
        verify(logger).warning(contains("[Config] One or more config values are invalid"));
    }
}
