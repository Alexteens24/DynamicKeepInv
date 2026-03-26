package xyz.superez.dynamickeepinv;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import xyz.superez.dynamickeepinv.rules.DeathRule;
import xyz.superez.dynamickeepinv.rules.RuleManager;
import xyz.superez.dynamickeepinv.rules.RuleReasons;
import xyz.superez.dynamickeepinv.rules.RuleResult;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuleManagerTest {

    @Mock private DynamicKeepInvPlugin plugin;
    @Mock private FileConfiguration config;
    @Mock private PlayerDeathEvent event;
    @Mock private Logger logger;

    private RuleManager manager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(logger);
        doNothing().when(plugin).debug(anyString());
        manager = new RuleManager(plugin);
    }

    @Test
    @DisplayName("First rule returning non-null wins")
    void testFirstRuleWins() {
        RuleResult expected = new RuleResult(true, true, RuleReasons.BYPASS);
        DeathRule first = mockRule("first", expected);
        DeathRule second = mockRule("second", new RuleResult(false, false, RuleReasons.TIME_NIGHT));

        manager.registerRule(first);
        manager.registerRule(second);

        RuleResult result = manager.evaluate(event);

        assertEquals(expected, result);
        verify(second, never()).evaluate(any(), any());
    }

    @Test
    @DisplayName("Null-returning rule passes to next rule")
    void testNullRulePassesToNext() {
        RuleResult expected = new RuleResult(false, false, RuleReasons.TIME_NIGHT);
        DeathRule first = mockRule("first", null);
        DeathRule second = mockRule("second", expected);

        manager.registerRule(first);
        manager.registerRule(second);

        RuleResult result = manager.evaluate(event);

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("All rules returning null → null result")
    void testAllNullReturnsNull() {
        manager.registerRule(mockRule("first", null));
        manager.registerRule(mockRule("second", null));

        RuleResult result = manager.evaluate(event);

        assertNull(result);
    }

    @Test
    @DisplayName("No rules registered → null")
    void testNoRules() {
        assertNull(manager.evaluate(event));
    }

    @Test
    @DisplayName("Rule that throws exception is caught: next rule evaluated")
    void testExceptionInRuleCaughtAndContinues() {
        DeathRule badRule = mock(DeathRule.class);
        when(badRule.getName()).thenReturn("bad");
        when(badRule.evaluate(any(), any())).thenThrow(new RuntimeException("boom"));

        RuleResult expected = new RuleResult(true, false, RuleReasons.PVP);
        DeathRule goodRule = mockRule("good", expected);

        manager.registerRule(badRule);
        manager.registerRule(goodRule);

        RuleResult result = manager.evaluate(event);

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("clearRules removes all registered rules")
    void testClearRules() {
        manager.registerRule(mockRule("a", new RuleResult(true, true, RuleReasons.BYPASS)));
        manager.clearRules();

        assertNull(manager.evaluate(event));
    }

    private DeathRule mockRule(String name, RuleResult result) {
        DeathRule rule = mock(DeathRule.class);
        when(rule.getName()).thenReturn(name);
        when(rule.evaluate(any(), any())).thenReturn(result);
        return rule;
    }
}
