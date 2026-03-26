package xyz.superez.dynamickeepinv;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import xyz.superez.dynamickeepinv.rules.BypassPermissionRule;
import xyz.superez.dynamickeepinv.rules.RuleReasons;
import xyz.superez.dynamickeepinv.rules.RuleResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BypassPermissionRuleTest {

    @Mock private DynamicKeepInvPlugin plugin;
    @Mock private FileConfiguration config;
    @Mock private PlayerDeathEvent event;
    @Mock private Player player;

    private BypassPermissionRule rule;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getConfig()).thenReturn(config);
        when(event.getEntity()).thenReturn(player);
        rule = new BypassPermissionRule();
    }

    @Test
    @DisplayName("Rule disabled in config → always null")
    void testRuleDisabled() {
        when(config.getBoolean("rules.bypass-permission", true)).thenReturn(false);
        when(player.hasPermission("dynamickeepinv.bypass")).thenReturn(true);

        RuleResult result = rule.evaluate(event, plugin);

        assertNull(result, "Rule should return null when disabled");
    }

    @Test
    @DisplayName("Player has bypass permission → keep all")
    void testPlayerHasBypassPermission() {
        when(config.getBoolean("rules.bypass-permission", true)).thenReturn(true);
        when(player.hasPermission("dynamickeepinv.bypass")).thenReturn(true);

        RuleResult result = rule.evaluate(event, plugin);

        assertNotNull(result);
        assertTrue(result.keepItems());
        assertTrue(result.keepXp());
        assertEquals(RuleReasons.BYPASS, result.reason());
    }

    @Test
    @DisplayName("Player lacks bypass permission → defer (null)")
    void testPlayerLacksPermission() {
        when(config.getBoolean("rules.bypass-permission", true)).thenReturn(true);
        when(player.hasPermission("dynamickeepinv.bypass")).thenReturn(false);

        RuleResult result = rule.evaluate(event, plugin);

        assertNull(result, "Rule should return null when player lacks permission");
    }

    @Test
    @DisplayName("getName returns correct identifier")
    void testGetName() {
        assertEquals("BypassPermissionRule", rule.getName());
    }
}
