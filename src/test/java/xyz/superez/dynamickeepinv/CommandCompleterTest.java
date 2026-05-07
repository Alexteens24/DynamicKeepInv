package xyz.superez.dynamickeepinv;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class CommandCompleterTest {

    @Mock
    private DynamicKeepInvPlugin plugin;
    @Mock
    private CommandSender sender;
    @Mock
    private Command command;

    private CommandCompleter completer;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        completer = new CommandCompleter(plugin);
    }

    private static DKIConfig dkiWithEconomy(boolean economyEnabled, String mode) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("schedule.milestones", List.of(
                Map.of("at", 0, "keep-items", true, "keep-xp", true, "announce", true),
                Map.of("at", 13000, "keep-items", false, "keep-xp", false, "announce", true)));
        y.set("death-rules.bypass-permission", true);
        y.set("economy.enabled", economyEnabled);
        y.set("economy.mode", mode);
        return new DKIConfig(y);
    }

    @Test
    public void testAdminCompletions() {
        when(plugin.getDKIConfig()).thenReturn(dkiWithEconomy(false, "gui"));
        when(sender.hasPermission("dynamickeepinv.admin")).thenReturn(true);
        when(sender.hasPermission("dynamickeepinv.stats")).thenReturn(false);

        List<String> results = completer.onTabComplete(sender, command, "dki", new String[]{""});

        assertTrue(results.contains("status"));
        assertTrue(results.contains("reload"));
        assertTrue(results.contains("enable"));
        assertTrue(results.contains("disable"));
        assertTrue(results.contains("toggle"));
        assertTrue(results.contains("test"));
        assertEquals(6, results.size());
    }

    @Test
    public void testStatsCompletion() {
        when(plugin.getDKIConfig()).thenReturn(dkiWithEconomy(false, "gui"));
        when(sender.hasPermission("dynamickeepinv.admin")).thenReturn(false);
        when(sender.hasPermission("dynamickeepinv.stats")).thenReturn(true);

        List<String> results = completer.onTabComplete(sender, command, "dki", new String[]{""});

        assertTrue(results.contains("stats"));
        assertEquals(1, results.size());
    }

    @Test
    public void testEconomyCompletions() {
        when(plugin.getDKIConfig()).thenReturn(dkiWithEconomy(true, "gui"));
        when(sender.hasPermission("dynamickeepinv.admin")).thenReturn(false);
        when(sender.hasPermission("dynamickeepinv.stats")).thenReturn(false);

        List<String> results = completer.onTabComplete(sender, command, "dki", new String[]{""});

        assertTrue(results.contains("confirm"));
        assertTrue(results.contains("autopay"));
        assertEquals(2, results.size());
    }

    @Test
    public void testPartialMatch() {
        when(plugin.getDKIConfig()).thenReturn(dkiWithEconomy(false, "gui"));
        when(sender.hasPermission("dynamickeepinv.admin")).thenReturn(true);

        List<String> results = completer.onTabComplete(sender, command, "dki", new String[]{"sta"});

        assertTrue(results.contains("status"));
        assertTrue(!results.contains("reload"));
    }
}
