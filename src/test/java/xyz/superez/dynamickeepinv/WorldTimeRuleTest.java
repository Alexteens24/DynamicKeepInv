package xyz.superez.dynamickeepinv;

import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorldTimeRuleTest {

    @Mock
    private DynamicKeepInvPlugin plugin;
    @Mock
    private PlayerDeathEvent event;
    @Mock
    private Player player;
    @Mock
    private World world;

    private WorldTimeRule rule;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(event.getEntity()).thenReturn(player);
        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        rule = new WorldTimeRule();
    }

    private DKIConfig configWithTwoSegments(boolean firstKeepItems, boolean secondKeepItems) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("schedule.milestones", List.of(
                Map.of("at", 0, "keep-items", firstKeepItems, "keep-xp", true, "announce", true),
                Map.of("at", 13000, "keep-items", secondKeepItems, "keep-xp", false, "announce", true)
        ));
        y.set("death-rules.bypass-permission", true);
        return new DKIConfig(y);
    }

    @Test
    @DisplayName("World time 6000 → first segment (index 0)")
    void testFirstSegment() {
        when(world.getTime()).thenReturn(6000L);
        when(plugin.getDKIConfig()).thenReturn(configWithTwoSegments(true, false));

        RuleResult result = rule.evaluate(event, plugin);

        assertNotNull(result);
        assertEquals(RuleReasons.timeSegmentReason(0), result.reason());
        assertTrue(result.keepItems());
        assertTrue(result.keepXp());
    }

    @Test
    @DisplayName("World time 18000 → second segment (index 1)")
    void testSecondSegment() {
        when(world.getTime()).thenReturn(18000L);
        when(plugin.getDKIConfig()).thenReturn(configWithTwoSegments(true, false));

        RuleResult result = rule.evaluate(event, plugin);

        assertNotNull(result);
        assertEquals(RuleReasons.timeSegmentReason(1), result.reason());
        assertFalse(result.keepItems());
        assertFalse(result.keepXp());
    }

    @Test
    @DisplayName("Legacy world override day=false applies to first segment")
    void testWorldOverrideFirstSegment() {
        when(world.getTime()).thenReturn(6000L);
        YamlConfiguration y = new YamlConfiguration();
        y.set("schedule.milestones", List.of(
                Map.of("at", 0, "keep-items", true, "keep-xp", true, "announce", true),
                Map.of("at", 13000, "keep-items", false, "keep-xp", false, "announce", true)
        ));
        y.set("death-rules.bypass-permission", true);
        y.set("worlds.overrides.world.day", false);
        when(plugin.getDKIConfig()).thenReturn(new DKIConfig(y));

        RuleResult result = rule.evaluate(event, plugin);

        assertNotNull(result);
        assertEquals(RuleReasons.timeSegmentReason(0), result.reason());
        assertFalse(result.keepItems());
    }

    @Test
    @DisplayName("getName returns correct identifier")
    void testGetName() {
        assertEquals("WorldTimeRule", rule.getName());
    }
}
