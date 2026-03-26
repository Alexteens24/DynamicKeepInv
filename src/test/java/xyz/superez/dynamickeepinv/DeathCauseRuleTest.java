package xyz.superez.dynamickeepinv;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import xyz.superez.dynamickeepinv.rules.DeathCauseRule;
import xyz.superez.dynamickeepinv.rules.RuleReasons;
import xyz.superez.dynamickeepinv.rules.RuleResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeathCauseRuleTest {

    @Mock private DynamicKeepInvPlugin plugin;
    @Mock private FileConfiguration config;
    @Mock private PlayerDeathEvent event;
    @Mock private Player player;
    @Mock private Player killer;

    private DeathCauseRule rule;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getConfig()).thenReturn(config);
        when(event.getEntity()).thenReturn(player);
        rule = new DeathCauseRule();
    }

    @Test
    @DisplayName("Rule disabled in config → null")
    void testRuleDisabled() {
        when(config.getBoolean("rules.death-cause.enabled", false)).thenReturn(false);

        RuleResult result = rule.evaluate(event, plugin);

        assertNull(result);
    }

    @Test
    @DisplayName("PvP death (killer present) → PVP reason, reads pvp config")
    void testPvpDeath() {
        when(config.getBoolean("rules.death-cause.enabled", false)).thenReturn(true);
        when(player.getKiller()).thenReturn(killer);
        when(config.getBoolean("rules.death-cause.pvp.keep-items", false)).thenReturn(true);
        when(config.getBoolean("rules.death-cause.pvp.keep-xp", false)).thenReturn(false);

        RuleResult result = rule.evaluate(event, plugin);

        assertNotNull(result);
        assertEquals(RuleReasons.PVP, result.reason());
        assertTrue(result.keepItems());
        assertFalse(result.keepXp());
    }

    @Test
    @DisplayName("PvE death (no killer) → PVE reason, reads pve config")
    void testPveDeath() {
        when(config.getBoolean("rules.death-cause.enabled", false)).thenReturn(true);
        when(player.getKiller()).thenReturn(null);
        when(config.getBoolean("rules.death-cause.pve.keep-items", false)).thenReturn(false);
        when(config.getBoolean("rules.death-cause.pve.keep-xp", false)).thenReturn(true);

        RuleResult result = rule.evaluate(event, plugin);

        assertNotNull(result);
        assertEquals(RuleReasons.PVE, result.reason());
        assertFalse(result.keepItems());
        assertTrue(result.keepXp());
    }

    @Test
    @DisplayName("getName returns correct identifier")
    void testGetName() {
        assertEquals("DeathCauseRule", rule.getName());
    }
}
