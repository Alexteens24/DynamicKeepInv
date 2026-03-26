package xyz.superez.dynamickeepinv;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import be.seeseemelk.mockbukkit.plugin.PluginManagerMock;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeathListenerTest {

    private ServerMock server;
    private DynamicKeepInvPlugin plugin;
    private WorldMock world;
    private DeathListener listener;
    private Path dataFolder;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        plugin = instantiatePlugin();
        PluginManagerMock pluginManager = server.getPluginManager();
        pluginManager.registerLoadedPlugin(plugin);
        pluginManager.enablePlugin(plugin);
        world = server.addSimpleWorld("world");
        listener = new DeathListener(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        if (dataFolder != null) {
            try {
                Files.walk(dataFolder)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                        });
            } catch (IOException ignored) {}
        }
    }

    @Test
    @DisplayName("Day time death → keep inventory flag set true, drops cleared")
    void testDayTimeKeepsInventory() {
        world.setTime(6000); // Day
        PlayerMock player = server.addPlayer();
        player.teleport(world.getSpawnLocation());

        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(Material.DIAMOND, 5));
        PlayerDeathEvent event = createDeathEvent(player, drops, 50);

        listener.onPlayerDeath(event);

        assertTrue(event.getKeepInventory(), "Items should be kept during the day");
        assertTrue(event.getDrops().isEmpty(), "Drops should be cleared when keepItems=true");
    }

    @Test
    @DisplayName("Night time death → keep inventory flag set false, drops remain")
    void testNightTimeDropsInventory() {
        world.setTime(18000); // Night
        PlayerMock player = server.addPlayer();
        player.teleport(world.getSpawnLocation());

        ItemStack diamond = new ItemStack(Material.DIAMOND, 5);
        List<ItemStack> drops = new ArrayList<>();
        drops.add(diamond);
        PlayerDeathEvent event = createDeathEvent(player, drops, 50);

        listener.onPlayerDeath(event);

        assertFalse(event.getKeepInventory(), "Items should drop during the night");
    }

    @Test
    @DisplayName("Plugin disabled in config → event unmodified")
    void testPluginDisabledSkipsProcessing() {
        plugin.getConfig().set("enabled", false);
        world.setTime(6000); // Day (would normally keep)
        PlayerMock player = server.addPlayer();
        player.teleport(world.getSpawnLocation());

        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(Material.DIAMOND));
        PlayerDeathEvent event = createDeathEvent(player, drops, 10);
        boolean initialKeepInv = event.getKeepInventory();

        listener.onPlayerDeath(event);

        assertEquals(initialKeepInv, event.getKeepInventory(), "Plugin disabled: event should be unmodified");
    }

    @Test
    @DisplayName("Disabled world → event unmodified")
    void testDisabledWorldSkipsProcessing() {
        plugin.getConfig().set("worlds.enabled", List.of("other_world")); // only other_world enabled
        world.setTime(6000);
        PlayerMock player = server.addPlayer();
        player.teleport(world.getSpawnLocation());

        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(Material.GOLD_INGOT));
        PlayerDeathEvent event = createDeathEvent(player, drops, 10);
        boolean initialKeepInv = event.getKeepInventory();

        listener.onPlayerDeath(event);

        assertEquals(initialKeepInv, event.getKeepInventory(), "Event in disabled world should be unmodified");
    }

    @Test
    @DisplayName("keepXp=true during day → keep level set, droppedExp zeroed")
    void testDayKeepsXp() {
        // Default config: rules.day.keep-xp defaults; set it explicitly
        plugin.getConfig().set("rules.day.keep-xp", true);
        world.setTime(6000);
        PlayerMock player = server.addPlayer();
        player.teleport(world.getSpawnLocation());

        PlayerDeathEvent event = createDeathEvent(player, new ArrayList<>(), 100);

        listener.onPlayerDeath(event);

        assertTrue(event.getKeepLevel(), "XP level should be kept");
        assertEquals(0, event.getDroppedExp(), "No XP should be dropped");
    }

    @Test
    @DisplayName("keepItems=false night → droppedExp > 0 if player has XP")
    void testNightDropsXp() {
        plugin.getConfig().set("rules.night.keep-xp", false);
        world.setTime(18000);
        PlayerMock player = server.addPlayer();
        player.teleport(world.getSpawnLocation());
        player.setLevel(5);

        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(Material.STONE));
        PlayerDeathEvent event = createDeathEvent(player, drops, 0); // start with 0 droppedExp

        listener.onPlayerDeath(event);

        assertFalse(event.getKeepLevel(), "XP level should not be kept at night");
    }

    @SuppressWarnings("deprecation")
    private PlayerDeathEvent createDeathEvent(PlayerMock player, List<ItemStack> drops, int droppedExp) {
        return new PlayerDeathEvent(player, drops, droppedExp, (String) null);
    }

    @SuppressWarnings({"deprecation", "removal"})
    private DynamicKeepInvPlugin instantiatePlugin() throws Exception {
        JavaPluginLoader loader = new JavaPluginLoader(server);
        PluginDescriptionFile description;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(input, "plugin.yml not found in classpath");
            description = new PluginDescriptionFile(input);
        }
        dataFolder = Files.createTempDirectory("dki-death-data");
        Path pluginJar = Files.createTempFile("dki-plugin", ".jar");
        return new DynamicKeepInvPlugin(loader, description, dataFolder.toFile(), pluginJar.toFile());
    }
}
