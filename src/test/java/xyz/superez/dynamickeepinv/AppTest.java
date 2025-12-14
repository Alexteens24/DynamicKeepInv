package xyz.superez.dynamickeepinv;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.plugin.PluginManagerMock;
import org.bukkit.GameRule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DynamicKeepInv Plugin
 * Demonstrates testing and debugging techniques
 */
class DynamicKeepInvPluginTest {

    private ServerMock server;
    private DynamicKeepInvPlugin plugin;
    private WorldMock world;
    private Path dataFolder;

    @BeforeEach
    void setUp() throws Exception {
        // Setup MockBukkit server and plugin before each test
        server = MockBukkit.mock();
        plugin = instantiatePlugin();
        PluginManagerMock pluginManager = server.getPluginManager();
        pluginManager.registerLoadedPlugin(plugin);
        pluginManager.enablePlugin(plugin);
        world = server.addSimpleWorld("world");

        System.out.println("=== Test Setup Complete ===");
    }

    @AfterEach
    void tearDown() {
        // Cleanup after each test
        MockBukkit.unmock();
        if (dataFolder != null) {
            try {
                Files.walk(dataFolder)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                                // Best-effort cleanup
                            }
                        });
            } catch (IOException ignored) {
                // ignore
            }
        }
        System.out.println("=== Test Cleanup Complete ===");
    }

    @Test
    @DisplayName("Test: Plugin loads successfully")
    void testPluginLoads() {
        System.out.println("Running: testPluginLoads");

        assertNotNull(plugin, "Plugin should not be null");
        assertTrue(plugin.isEnabled(), "Plugin should be enabled");

        System.out.println("✓ Plugin loaded successfully");
    }

    @Test
    @DisplayName("Test: Config file loads with default values")
    void testConfigDefaults() {
        System.out.println("Running: testConfigDefaults");

        assertTrue(plugin.getConfig().getBoolean("enabled"),
                "Plugin should be enabled by default");
        // Updated to new config paths
        assertTrue(plugin.getConfig().getBoolean("rules.day.keep-items"),
                "Keep inventory should be ON during day");
        assertFalse(plugin.getConfig().getBoolean("rules.night.keep-items"),
                "Keep inventory should be OFF during night");
        assertEquals(100, plugin.getConfig().getInt("check-interval"),
                "Check interval should be 100 ticks");

        System.out.println("✓ Config defaults are correct");
    }

    @Test
    @DisplayName("Test: Day time enables keep inventory")
    void testDayTimeKeepInventory() {
        System.out.println("Running: testDayTimeKeepInventory");

        // Set world time to day (6000 = noon)
        world.setTime(6000);
        System.out.println("World time set to: " + world.getTime() + " (Day)");

        // Wait for scheduler to run
        server.getScheduler().performOneTick();

        // Check if keep inventory is enabled
        Boolean keepInv = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
        System.out.println("Keep Inventory value: " + keepInv);

        assertTrue(keepInv, "Keep inventory should be ON during day");
        System.out.println("✓ Day time keep inventory works correctly");
    }

    @Test
    @DisplayName("Test: Night time disables keep inventory")
    void testNightTimeKeepInventory() {
        System.out.println("Running: testNightTimeKeepInventory");

        // Set world time to night (18000 = midnight)
        world.setTime(18000);
        System.out.println("World time set to: " + world.getTime() + " (Night)");

        // Wait for scheduler to run
        server.getScheduler().performOneTick();

        // Check if keep inventory is disabled
        Boolean keepInv = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
        System.out.println("Keep Inventory value: " + keepInv);

        assertFalse(keepInv, "Keep inventory should be OFF during night");
        System.out.println("✓ Night time keep inventory works correctly");
    }

    @Test
    @DisplayName("Test: Day to Night transition")
    void testDayToNightTransition() {
        System.out.println("Running: testDayToNightTransition");

        // Start at day
        world.setTime(6000);
        server.getScheduler().performOneTick();
        Boolean dayKeepInv = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
        System.out.println("Day (6000): Keep Inventory = " + dayKeepInv);

        // Transition to night
        world.setTime(18000);
        // Advance enough ticks to allow the scheduled task (100 tick interval) to run again
        server.getScheduler().performTicks(plugin.getConfig().getInt("check-interval", 100));
        Boolean nightKeepInv = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
        System.out.println("Night (18000): Keep Inventory = " + nightKeepInv);

        assertTrue(dayKeepInv, "Should be ON during day");
        assertFalse(nightKeepInv, "Should be OFF during night");
        assertNotEquals(dayKeepInv, nightKeepInv, "Values should change");

        System.out.println("✓ Day/Night transition works correctly");
    }

    @Test
    @DisplayName("Test: Command execution - status")
    void testStatusCommand() {
        System.out.println("Running: testStatusCommand");

        boolean result = executeCommand("status");
        assertTrue(result, "Status command should execute successfully");

        System.out.println("✓ Status command works");
    }

    @Test
    @DisplayName("Test: Command execution - toggle")
    void testToggleCommand() {
        System.out.println("Running: testToggleCommand");

        boolean initialState = plugin.getConfig().getBoolean("enabled");
        System.out.println("Initial state: " + initialState);

        executeCommand("toggle");
        boolean newState = plugin.getConfig().getBoolean("enabled");
        System.out.println("New state: " + newState);

        assertNotEquals(initialState, newState, "Toggle should change state");
        System.out.println("✓ Toggle command works");
    }

    @Test
    @DisplayName("Test: Multiple worlds support")
    void testMultipleWorlds() {
        System.out.println("Running: testMultipleWorlds");

        // Create additional worlds
        WorldMock world2 = server.addSimpleWorld("world_nether");
        WorldMock world3 = server.addSimpleWorld("world_the_end");

        // Set all to day time
        world.setTime(6000);
        world2.setTime(6000);
        world3.setTime(6000);

        server.getScheduler().performOneTick();

        // Check all worlds have keep inventory enabled
        assertTrue(world.getGameRuleValue(GameRule.KEEP_INVENTORY));
        assertTrue(world2.getGameRuleValue(GameRule.KEEP_INVENTORY));
        assertTrue(world3.getGameRuleValue(GameRule.KEEP_INVENTORY));

        System.out.println("✓ Multiple worlds support works");
    }

    @Test
    @DisplayName("Test: Disabled worlds are skipped")
    void testDisabledWorld() {
        System.out.println("Running: testDisabledWorld");

        // Create additional worlds
        WorldMock worldNether = server.addSimpleWorld("world_nether");

        // Set config to only enable "world" (uppercase W to test case insensitivity)
        // Updated to new config path
        plugin.getConfig().set("worlds.enabled", java.util.Arrays.asList("World"));

        // Set times
        world.setTime(6000); // Day
        worldNether.setTime(6000); // Day

        // Pre-condition: Set world_nether to FALSE manually to verify plugin doesn't change it to TRUE (Day)
        worldNether.setGameRule(GameRule.KEEP_INVENTORY, false);

        server.getScheduler().performTicks(plugin.getConfig().getInt("check-interval", 100));

        // Enabled world should be TRUE (Day)
        assertTrue(world.getGameRuleValue(GameRule.KEEP_INVENTORY), "Enabled world should be modified");

        // Disabled world should be FALSE (Untouched)
        assertFalse(worldNether.getGameRuleValue(GameRule.KEEP_INVENTORY), "Disabled world should NOT be modified");

        System.out.println("✓ Disabled worlds logic works");
    }

    /**
     * Example: Performance test
     */
    @Test
    @DisplayName("Performance: Check task execution time")
    void testPerformance() {
        System.out.println("Running: testPerformance");

        long startTime = System.nanoTime();

        // Simulate 100 ticks (5 seconds in-game)
        for (int i = 0; i < 100; i++) {
            server.getScheduler().performOneTick();
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000; // Convert to ms

        System.out.println("100 ticks executed in: " + duration + "ms");
        assertTrue(duration < 1000, "Should complete in less than 1 second");

        System.out.println("✓ Performance test passed");
    }

    private boolean executeCommand(String... args) {
        Command dummyCommand = new Command("dynamickeepinv") {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] commandArgs) {
                return false;
            }
        };
        return plugin.onCommand(server.getConsoleSender(), dummyCommand, "dki", args);
    }

    @SuppressWarnings({"deprecation", "removal"})
    private DynamicKeepInvPlugin instantiatePlugin() throws Exception {
        JavaPluginLoader loader = new JavaPluginLoader(server);
        PluginDescriptionFile description;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(input, "plugin.yml not found in classpath");
            description = new PluginDescriptionFile(input);
        }

        dataFolder = Files.createTempDirectory("dki-data");
        Path pluginJar = Files.createTempFile("dki-plugin", ".jar");
        return new DynamicKeepInvPlugin(loader, description, dataFolder.toFile(), pluginJar.toFile());
    }
}
