package xyz.superez.dynamickeepinv;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import xyz.superez.dynamickeepinv.rules.RuleReasons;
import xyz.superez.dynamickeepinv.rules.RuleResult;
import xyz.superez.dynamickeepinv.rules.WorldTimeRule;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorldTimeRuleTest {

    @Mock private DynamicKeepInvPlugin plugin;
    @Mock private FileConfiguration config;
    @Mock private PlayerDeathEvent event;
    @Mock private Player player;
    @Mock private World world;

    private WorldTimeRule rule;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getConfig()).thenReturn(config);
        when(event.getEntity()).thenReturn(player);
        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        // Default: no world overrides
        when(config.contains("worlds.overrides.world")).thenReturn(false);
        // Default time range 0 → 13000 = day
        when(config.getLong("time.day-start", 0)).thenReturn(0L);
        when(config.getLong("time.night-start", 13000)).thenReturn(13000L);
        rule = new WorldTimeRule();
    }

    @Test
    @DisplayName("Day time (6000) → TIME_DAY reason, reads rules.day config")
    void testDayTime() {
        when(world.getTime()).thenReturn(6000L);
        when(plugin.isTimeInRange(6000L, 0L, 13000L)).thenReturn(true);
        when(config.getBoolean("rules.day.keep-items", true)).thenReturn(true);
        when(config.getBoolean("rules.day.keep-xp", true)).thenReturn(false);

        RuleResult result = rule.evaluate(event, plugin);

        assertNotNull(result);
        assertEquals(RuleReasons.TIME_DAY, result.reason());
        assertTrue(result.keepItems());
        assertFalse(result.keepXp());
    }

    @Test
    @DisplayName("Night time (18000) → TIME_NIGHT reason, reads rules.night config")
    void testNightTime() {
        when(world.getTime()).thenReturn(18000L);
        when(plugin.isTimeInRange(18000L, 0L, 13000L)).thenReturn(false);
        when(config.getBoolean("rules.night.keep-items", false)).thenReturn(false);
        when(config.getBoolean("rules.night.keep-xp", false)).thenReturn(false);

        RuleResult result = rule.evaluate(event, plugin);

        assertNotNull(result);
        assertEquals(RuleReasons.TIME_NIGHT, result.reason());
        assertFalse(result.keepItems());
        assertFalse(result.keepXp());
    }

    @Test
    @DisplayName("World override for day respected")
    void testWorldOverrideDay() {
        when(world.getTime()).thenReturn(6000L);
        when(plugin.isTimeInRange(6000L, 0L, 13000L)).thenReturn(true);
        when(config.contains("worlds.overrides.world")).thenReturn(true);
        when(config.contains("worlds.overrides.world.day")).thenReturn(true);
        when(config.getBoolean("worlds.overrides.world.day")).thenReturn(false); // override: no keep during day
        when(config.getBoolean("rules.day.keep-items", false)).thenReturn(false);
        when(config.getBoolean("rules.day.keep-xp", false)).thenReturn(false);

        RuleResult result = rule.evaluate(event, plugin);

        assertNotNull(result);
        assertEquals(RuleReasons.TIME_DAY, result.reason());
        assertFalse(result.keepItems());
    }

    @Test
    @DisplayName("getName returns correct identifier")
    void testGetName() {
        assertEquals("WorldTimeRule", rule.getName());
    }
}
