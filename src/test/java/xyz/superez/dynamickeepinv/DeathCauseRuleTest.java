package xyz.superez.dynamickeepinv;

import org.bukkit.configuration.file.YamlConfiguration;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class DeathCauseRuleTest {

    @Mock
    private DynamicKeepInvPlugin plugin;
    @Mock
    private PlayerDeathEvent event;
    @Mock
    private Player player;
    @Mock
    private Player killer;

    private DeathCauseRule rule;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(event.getEntity()).thenReturn(player);
        rule = new DeathCauseRule();
    }

    private static YamlConfiguration baseYaml() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("schedule.milestones", List.of(
                Map.of("at", 0, "keep-items", true, "keep-xp", true, "announce", true),
                Map.of("at", 13000, "keep-items", false, "keep-xp", false, "announce", true)));
        y.set("death-rules.bypass-permission", true);
        return y;
    }

    @Test
    @DisplayName("Rule disabled in config → null")
    void testRuleDisabled() {
        YamlConfiguration y = baseYaml();
        y.set("death-rules.death-cause.enabled", false);
        when(plugin.getDKIConfig()).thenReturn(new DKIConfig(y));

        RuleResult result = rule.evaluate(event, plugin);

        assertNull(result);
    }

    @Test
    @DisplayName("PvP death (killer present) → PVP reason, reads pvp config")
    void testPvpDeath() {
        YamlConfiguration y = baseYaml();
        y.set("death-rules.death-cause.enabled", true);
        y.set("death-rules.death-cause.pvp.keep-items", true);
        y.set("death-rules.death-cause.pvp.keep-xp", false);
        when(plugin.getDKIConfig()).thenReturn(new DKIConfig(y));
        when(player.getKiller()).thenReturn(killer);

        RuleResult result = rule.evaluate(event, plugin);

        assertNotNull(result);
        assertEquals(RuleReasons.PVP, result.reason());
        assertTrue(result.keepItems());
        assertFalse(result.keepXp());
    }

    @Test
    @DisplayName("PvE death (no killer) → PVE reason, reads pve config")
    void testPveDeath() {
        YamlConfiguration y = baseYaml();
        y.set("death-rules.death-cause.enabled", true);
        y.set("death-rules.death-cause.pve.keep-items", false);
        y.set("death-rules.death-cause.pve.keep-xp", true);
        when(plugin.getDKIConfig()).thenReturn(new DKIConfig(y));
        when(player.getKiller()).thenReturn(null);

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
