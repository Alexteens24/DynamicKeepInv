package xyz.superez.dynamickeepinv;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class CommandCompleterTest {

    @Mock
    private DynamicKeepInvPlugin plugin;
    @Mock
    private FileConfiguration config;
    @Mock
    private CommandSender sender;
    @Mock
    private Command command;

    private CommandCompleter completer;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getConfig()).thenReturn(config);
        completer = new CommandCompleter(plugin);
    }

    @Test
    public void testAdminCompletions() {
        when(sender.hasPermission("dynamickeepinv.admin")).thenReturn(true);
        when(sender.hasPermission("dynamickeepinv.stats")).thenReturn(false);
        when(config.getBoolean(eq("economy.enabled"), eq(false))).thenReturn(false);

        List<String> results = completer.onTabComplete(sender, command, "dki", new String[]{""});

        assertTrue(results.contains("status"));
        assertTrue(results.contains("reload"));
        assertTrue(results.contains("enable"));
        assertTrue(results.contains("disable"));
        assertTrue(results.contains("toggle"));
        assertEquals(5, results.size());
    }

    @Test
    public void testStatsCompletion() {
        when(sender.hasPermission("dynamickeepinv.admin")).thenReturn(false);
        when(sender.hasPermission("dynamickeepinv.stats")).thenReturn(true);
        when(config.getBoolean(eq("economy.enabled"), eq(false))).thenReturn(false);

        List<String> results = completer.onTabComplete(sender, command, "dki", new String[]{""});

        assertTrue(results.contains("stats"));
        assertEquals(1, results.size());
    }

    @Test
    public void testEconomyCompletions() {
        when(sender.hasPermission("dynamickeepinv.admin")).thenReturn(false);
        when(sender.hasPermission("dynamickeepinv.stats")).thenReturn(false);
        when(config.getBoolean(eq("economy.enabled"), eq(false))).thenReturn(true);
        when(config.getString(eq("economy.mode"), anyString())).thenReturn("gui");

        List<String> results = completer.onTabComplete(sender, command, "dki", new String[]{""});

        assertTrue(results.contains("confirm"));
        assertTrue(results.contains("autopay"));
        assertEquals(2, results.size());
    }

    @Test
    public void testPartialMatch() {
        when(sender.hasPermission("dynamickeepinv.admin")).thenReturn(true);

        List<String> results = completer.onTabComplete(sender, command, "dki", new String[]{"sta"});

        assertTrue(results.contains("status"));
        assertTrue(!results.contains("reload"));
    }
}
