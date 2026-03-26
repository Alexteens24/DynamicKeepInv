package xyz.superez.dynamickeepinv;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandDispatcher {

    private final DynamicKeepInvPlugin plugin;

    public CommandDispatcher(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean dispatch(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("stats")) {
            return handleStatsCommand(sender, args);
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            return handleConfirmCommand(sender);
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("autopay")) {
            return handleAutoPayCommand(sender);
        }

        if (!sender.hasPermission("dynamickeepinv.admin")) {
            sender.sendMessage(plugin.parseMessage(plugin.getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.parseMessage(plugin.getMessage("help.header")));
            sender.sendMessage(plugin.parseMessage(plugin.getMessage("help.status")));
            sender.sendMessage(plugin.parseMessage(plugin.getMessage("help.reload")));
            sender.sendMessage(plugin.parseMessage(plugin.getMessage("help.enable")));
            sender.sendMessage(plugin.parseMessage(plugin.getMessage("help.disable")));
            sender.sendMessage(plugin.parseMessage(plugin.getMessage("help.toggle")));
            sender.sendMessage(plugin.parseMessage(plugin.getMessage("help.stats")));
            sender.sendMessage(plugin.parseMessage(plugin.getMessage("help.autopay")));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status":
                showStatus(sender);
                break;

            case "reload":
                plugin.reloadConfig();
                plugin.loadMessages();
                plugin.reloadIntegrations();
                plugin.reloadPendingDeathManager();
                plugin.reloadStatsSystem();
                if (plugin.getConfig().getBoolean("enabled", true)) {
                    plugin.startChecking();
                } else {
                    plugin.stopChecking(true);
                }
                sender.sendMessage(plugin.parseMessage(plugin.getMessage("commands.reload")));
                break;

            case "enable":
                plugin.getConfig().set("enabled", true);
                plugin.saveConfig();
                plugin.startChecking();
                sender.sendMessage(plugin.parseMessage(plugin.getMessage("commands.enabled")));
                break;

            case "disable":
                plugin.getConfig().set("enabled", false);
                plugin.saveConfig();
                plugin.stopChecking(true);
                sender.sendMessage(plugin.parseMessage(plugin.getMessage("commands.disabled")));
                break;

            case "toggle":
                boolean newState = !plugin.getConfig().getBoolean("enabled", true);
                plugin.getConfig().set("enabled", newState);
                plugin.saveConfig();
                if (newState) {
                    plugin.startChecking();
                    sender.sendMessage(plugin.parseMessage(plugin.getMessage("commands.enabled")));
                } else {
                    plugin.stopChecking(true);
                    sender.sendMessage(plugin.parseMessage(plugin.getMessage("commands.disabled")));
                }
                break;

            default:
                sender.sendMessage(plugin.parseMessage(plugin.getMessage("commands.unknown")));
        }

        return true;
    }

    private boolean handleStatsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.parseMessage("&cThis command can only be used by players!"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("dynamickeepinv.stats")) {
            sender.sendMessage(plugin.parseMessage(plugin.getMessage("no-permission")));
            return true;
        }

        if (plugin.getStatsGUI() == null || plugin.getStatsManager() == null) {
            sender.sendMessage(plugin.parseMessage(plugin.getMessage("stats.disabled")));
            return true;
        }

        if (args.length >= 2) {
            if (player.hasPermission("dynamickeepinv.stats.others")) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    plugin.getStatsGUI().openStats(player, target.getUniqueId(), target.getName());
                } else {
                    org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(args[1]);
                    if (offlineTarget.hasPlayedBefore() || offlineTarget.isOnline()) {
                        String displayName = offlineTarget.getName() != null ? offlineTarget.getName() : args[1];
                        plugin.getStatsGUI().openStats(player, offlineTarget.getUniqueId(), displayName);
                    } else {
                        sender.sendMessage(plugin.parseMessage(plugin.getMessage("stats.player-not-found").replace("{player}", args[1])));
                    }
                }
            } else {
                sender.sendMessage(plugin.parseMessage(plugin.getMessage("no-permission")));
            }
        } else {
            plugin.getStatsGUI().openStats(player);
        }

        return true;
    }

    private boolean handleConfirmCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.parseMessage("&cThis command can only be used by players!"));
            return true;
        }

        if (!plugin.getConfig().getBoolean("economy.enabled", false)
                || !"gui".equalsIgnoreCase(plugin.getConfig().getString("economy.mode", "charge-to-keep"))) {
            sender.sendMessage(plugin.parseMessage("&cDeath confirmation GUI is not enabled! Set economy mode to 'gui' in config."));
            return true;
        }

        if (plugin.getPendingDeathManager() == null || plugin.getDeathConfirmGUI() == null) {
            sender.sendMessage(plugin.parseMessage("&cDeath confirmation GUI is not enabled!"));
            return true;
        }

        PendingDeath pending = plugin.getPendingDeathManager().getPendingDeath(player.getUniqueId());
        if (pending == null || pending.isProcessed()) {
            sender.sendMessage(plugin.parseMessage("&eYou don't have a pending death to confirm."));
            return true;
        }

        plugin.getDeathConfirmGUI().openGUI(player, pending);
        return true;
    }

    private boolean handleAutoPayCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.parseMessage("&cThis command can only be used by players!"));
            return true;
        }

        if (!plugin.getConfig().getBoolean("economy.enabled", false)
                || !"gui".equalsIgnoreCase(plugin.getConfig().getString("economy.mode", "charge-to-keep"))) {
            sender.sendMessage(plugin.parseMessage("&cAuto-pay requires GUI economy mode! Set economy mode to 'gui' in config."));
            return true;
        }

        if (plugin.getPendingDeathManager() == null) {
            sender.sendMessage(plugin.parseMessage("&cDeath confirmation GUI is not enabled!"));
            return true;
        }

        boolean newState = plugin.getPendingDeathManager().toggleAutoPay(player.getUniqueId());
        String msgKey = newState ? "economy.gui.auto-pay-enabled" : "economy.gui.auto-pay-disabled";
        String msg = plugin.getMessage(msgKey);
        player.sendMessage(plugin.parseMessage(msg));
        return true;
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage(plugin.parseMessage(plugin.getMessage("status.header")));
        sender.sendMessage(plugin.parseMessage(plugin.getMessage("status.enabled")
                .replace("{value}", String.valueOf(plugin.getConfig().getBoolean("enabled", true)))));
        sender.sendMessage(plugin.parseMessage(plugin.getMessage("status.keep-inv-day")
                .replace("{value}", String.valueOf(plugin.getConfig().getBoolean("rules.day.keep-items", true)))));
        sender.sendMessage(plugin.parseMessage(plugin.getMessage("status.keep-inv-night")
                .replace("{value}", String.valueOf(plugin.getConfig().getBoolean("rules.night.keep-items", false)))));
        sender.sendMessage(plugin.parseMessage(plugin.getMessage("status.check-interval")
                .replace("{value}", String.valueOf(plugin.getConfig().getInt("check-interval", 100)))));

        sender.sendMessage(plugin.parseMessage(plugin.getMessage("status.world-header")));
        long dayStart = plugin.getConfig().getLong("time.day-start", 0);
        long nightStart = plugin.getConfig().getLong("time.night-start", 13000);
        for (World world : Bukkit.getWorlds()) {
            long time = world.getTime();
            boolean isDay = plugin.isTimeInRange(time, dayStart, nightStart);
            Boolean keepInv = world.getGameRuleValue(GameRule.KEEP_INVENTORY);

            String period = isDay ? plugin.getMessage("status.day") : plugin.getMessage("status.night");
            String status = (keepInv != null && keepInv) ? plugin.getMessage("status.on") : plugin.getMessage("status.off");

            String worldInfo = plugin.getMessage("status.world-info")
                    .replace("{world}", world.getName())
                    .replace("{time}", String.valueOf(time))
                    .replace("{period}", period)
                    .replace("{status}", status);

            sender.sendMessage(plugin.parseMessage(worldInfo));
        }
    }
}
