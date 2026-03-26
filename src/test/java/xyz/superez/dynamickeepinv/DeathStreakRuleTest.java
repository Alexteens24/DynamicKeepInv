package xyz.superez.dynamickeepinv;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import xyz.superez.dynamickeepinv.rules.DeathStreakRule;
import xyz.superez.dynamickeepinv.rules.RuleReasons;
import xyz.superez.dynamickeepinv.rules.RuleResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

class DeathStreakRuleTest {

    @Mock
    private DynamicKeepInvPlugin plugin;
    @Mock
    private PlayerDeathEvent event;
    @Mock
    private Player player;

    private DeathStreakRule rule;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rule = new DeathStreakRule();

        when(event.getEntity()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Alex");
    }

    @Test
    void returnsNullWhenRuleDisabled() {
        when(plugin.getDKIConfig()).thenReturn(config(false, 2, 60, true, true));

        RuleResult result = rule.evaluate(event, plugin);

        assertNull(result);
    }

    @Test
    void reachesThresholdOnNthDeath() {
        when(plugin.getDKIConfig()).thenReturn(config(true, 3, 60, true, false));

        assertNull(rule.evaluate(event, plugin));
        assertNull(rule.evaluate(event, plugin));

        RuleResult result = rule.evaluate(event, plugin);

        assertNotNull(result);
        assertEquals(true, result.keepItems());
        assertEquals(false, result.keepXp());
        assertEquals(RuleReasons.DEATH_STREAK, result.reason());
    }

    @Test
    void expiredDeathsDoNotCountTowardThreshold() throws InterruptedException {
        when(plugin.getDKIConfig()).thenReturn(config(true, 2, 1, true, true));

        assertNull(rule.evaluate(event, plugin));
        Thread.sleep(1100);

        RuleResult result = rule.evaluate(event, plugin);

        assertNull(result);
    }

    @Test
    void clearResetsTrackedDeaths() {
        when(plugin.getDKIConfig()).thenReturn(config(true, 2, 60, true, true));

        assertNull(rule.evaluate(event, plugin));
        rule.clear();

        RuleResult result = rule.evaluate(event, plugin);

        assertNull(result);
    }

    private DKIConfig config(boolean enabled, int threshold, int windowSeconds, boolean keepItems, boolean keepXp) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("rules.streak.enabled", enabled);
        yaml.set("rules.streak.threshold", threshold);
        yaml.set("rules.streak.window-seconds", windowSeconds);
        yaml.set("rules.streak.keep-items", keepItems);
        yaml.set("rules.streak.keep-xp", keepXp);
        return new DKIConfig(yaml);
    }
}