package xyz.superez.dynamickeepinv;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommandDispatcherTest {

    private ServerMock server;
    private DynamicKeepInvPlugin plugin;
    private CommandDispatcher dispatcher;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DynamicKeepInvPlugin.class);
        dispatcher = new CommandDispatcher(plugin);
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testCommandRequiresTargetForConsole() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("dynamickeepinv.admin")).thenReturn(true);

        dispatcher.dispatch(sender, dummyCommand(), "dki", new String[]{"test"});

        List<String> messages = capturedMessages(sender);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Usage: /dki test [player]")));
    }

    @Test
    void testCommandDetectsBypassPermission() {
        PlayerMock target = server.addPlayer("BypassUser");
        target.teleport(world.getSpawnLocation());
        target.addAttachment(plugin, "dynamickeepinv.bypass", true);

        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("dynamickeepinv.admin")).thenReturn(true);

        dispatcher.dispatch(sender, dummyCommand(), "dki", new String[]{"test", target.getName()});

        List<String> messages = capturedMessages(sender);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("[BYPASS]")));
    }

    @Test
    void testCommandDetectsFirstDeathRule() {
        plugin.getConfig().set("rules.first-death.enabled", true);
        plugin.getConfig().set("rules.first-death.keep-items", true);
        plugin.getConfig().set("rules.first-death.keep-xp", false);
        plugin.refreshDKIConfig();

        PlayerMock target = server.addPlayer("FirstDeathUser");
        target.teleport(world.getSpawnLocation());

        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("dynamickeepinv.admin")).thenReturn(true);

        dispatcher.dispatch(sender, dummyCommand(), "dki", new String[]{"test", target.getName()});

        List<String> messages = capturedMessages(sender);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("[FIRST-DEATH]") && msg.contains("keepItems=true") && msg.contains("keepXp=false")));
    }

    @Test
    void testCommandFallsBackToNightTimeRule() {
        PlayerMock target = server.addPlayer("NightUser");
        world.setTime(18000);
        target.teleport(world.getSpawnLocation());

        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("dynamickeepinv.admin")).thenReturn(true);

        dispatcher.dispatch(sender, dummyCommand(), "dki", new String[]{"test", target.getName()});

        List<String> messages = capturedMessages(sender);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("[TIME]") && msg.contains("Night")));
    }

    @Test
    void statusCommandShowsRuleChainNames() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("dynamickeepinv.admin")).thenReturn(true);

        dispatcher.dispatch(sender, dummyCommand(), "dki", new String[]{"status"});

        List<String> messages = capturedMessages(sender);
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("Active Rule Chain")));
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("BypassPermissionRule")));
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("WorldTimeRule")));
    }

    private List<String> capturedMessages(CommandSender sender) {
        ArgumentCaptor<Component> messages = ArgumentCaptor.forClass(Component.class);
        verify(sender, atLeastOnce()).sendMessage(messages.capture());
        PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
        return messages.getAllValues().stream().map(serializer::serialize).toList();
    }

    private Command dummyCommand() {
        return new Command("dynamickeepinv") {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return true;
            }
        };
    }
}