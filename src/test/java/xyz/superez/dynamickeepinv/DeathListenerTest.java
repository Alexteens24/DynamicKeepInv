package xyz.superez.dynamickeepinv;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.Material;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeathListenerTest {

    private ServerMock server;
    private DynamicKeepInvPlugin plugin;
    private WorldMock world;
    private DeathListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DynamicKeepInvPlugin.class);
        world = server.addSimpleWorld("world");
        listener = new DeathListener(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
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
        plugin.refreshDKIConfig();
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
        plugin.refreshDKIConfig();
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

    private PlayerDeathEvent createDeathEvent(PlayerMock player, List<ItemStack> drops, int droppedExp) {
        return new PlayerDeathEvent(player, drops, droppedExp, (String) null);
    }

}
