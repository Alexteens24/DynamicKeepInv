package xyz.superez.dynamickeepinv;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandCompleter implements TabCompleter {
    private final DynamicKeepInvPlugin plugin;

    public CommandCompleter(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            if (sender.hasPermission("dynamickeepinv.admin")) {
                commands.add("status");
                commands.add("reload");
                commands.add("enable");
                commands.add("disable");
                commands.add("toggle");
            }

            if (sender.hasPermission("dynamickeepinv.stats")) {
                commands.add("stats");
            }

            if (plugin.getConfig().getBoolean("economy.enabled", false)
                    && "gui".equalsIgnoreCase(plugin.getConfig().getString("economy.mode", "charge-to-keep"))) {
                commands.add("confirm");
                commands.add("autopay");
            }

            StringUtil.copyPartialMatches(args[0], commands, completions);
            Collections.sort(completions);
            return completions;
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("stats") && sender.hasPermission("dynamickeepinv.stats.others")) {
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    players.add(p.getName());
                }
                StringUtil.copyPartialMatches(args[1], players, completions);
                Collections.sort(completions);
                return completions;
            }
        }

        return Collections.emptyList();
    }
}
