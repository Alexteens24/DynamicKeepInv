package xyz.superez.dynamickeepinv;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import xyz.superez.dynamickeepinv.rules.FirstDeathRule;
import xyz.superez.dynamickeepinv.rules.RuleReasons;
import xyz.superez.dynamickeepinv.rules.RuleResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

class FirstDeathRuleTest {

    @Mock
    private DynamicKeepInvPlugin plugin;
    @Mock
    private StatsManager statsManager;
    @Mock
    private PlayerDeathEvent event;
    @Mock
    private Player player;

    private final FirstDeathRule rule = new FirstDeathRule();
    private UUID playerId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        playerId = UUID.randomUUID();

        when(event.getEntity()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Alex");
    }

    @Test
    void returnsNullWhenRuleDisabled() {
        when(plugin.getDKIConfig()).thenReturn(config(false, true, true));

        RuleResult result = rule.evaluate(event, plugin);

        assertNull(result);
    }

    @Test
    void returnsNullWhenStatsManagerUnavailable() {
        when(plugin.getDKIConfig()).thenReturn(config(true, true, true));
        when(plugin.getStatsManager()).thenReturn(null);

        RuleResult result = rule.evaluate(event, plugin);

        assertNull(result);
    }

    @Test
    void returnsConfiguredDecisionOnFirstDeath() {
        when(plugin.getDKIConfig()).thenReturn(config(true, false, true));
        when(plugin.getStatsManager()).thenReturn(statsManager);
        when(statsManager.getTotalDeaths(playerId)).thenReturn(0);

        RuleResult result = rule.evaluate(event, plugin);

        assertNotNull(result);
        assertEquals(false, result.keepItems());
        assertEquals(true, result.keepXp());
        assertEquals(RuleReasons.FIRST_DEATH, result.reason());
    }

    @Test
    void returnsNullWhenPlayerAlreadyHasDeaths() {
        when(plugin.getDKIConfig()).thenReturn(config(true, true, true));
        when(plugin.getStatsManager()).thenReturn(statsManager);
        when(statsManager.getTotalDeaths(playerId)).thenReturn(3);

        RuleResult result = rule.evaluate(event, plugin);

        assertNull(result);
    }

    private DKIConfig config(boolean enabled, boolean keepItems, boolean keepXp) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("rules.first-death.enabled", enabled);
        yaml.set("rules.first-death.keep-items", keepItems);
        yaml.set("rules.first-death.keep-xp", keepXp);
        return new DKIConfig(yaml);
    }
}