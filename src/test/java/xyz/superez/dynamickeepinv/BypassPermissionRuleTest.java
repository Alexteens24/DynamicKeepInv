package xyz.superez.dynamickeepinv;

import org.bukkit.configuration.file.YamlConfiguration;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class BypassPermissionRuleTest {

    @Mock
    private DynamicKeepInvPlugin plugin;
    @Mock
    private PlayerDeathEvent event;
    @Mock
    private Player player;

    private BypassPermissionRule rule;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(event.getEntity()).thenReturn(player);
        rule = new BypassPermissionRule();
    }

    private static YamlConfiguration baseYaml(boolean bypassEnabled) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("schedule.milestones", List.of(
                Map.of("at", 0, "keep-items", true, "keep-xp", true, "announce", true),
                Map.of("at", 13000, "keep-items", false, "keep-xp", false, "announce", true)));
        y.set("death-rules.bypass-permission", bypassEnabled);
        return y;
    }

    @Test
    @DisplayName("Rule disabled in config → always null")
    void testRuleDisabled() {
        when(plugin.getDKIConfig()).thenReturn(new DKIConfig(baseYaml(false)));
        when(player.hasPermission("dynamickeepinv.bypass")).thenReturn(true);

        RuleResult result = rule.evaluate(event, plugin);

        assertNull(result, "Rule should return null when disabled");
    }

    @Test
    @DisplayName("Player has bypass permission → keep all")
    void testPlayerHasBypassPermission() {
        when(plugin.getDKIConfig()).thenReturn(new DKIConfig(baseYaml(true)));
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
        when(plugin.getDKIConfig()).thenReturn(new DKIConfig(baseYaml(true)));
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
