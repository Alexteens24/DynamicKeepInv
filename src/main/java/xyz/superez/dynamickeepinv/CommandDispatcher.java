package xyz.superez.dynamickeepinv;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.superez.dynamickeepinv.hooks.GriefPreventionHook;
import xyz.superez.dynamickeepinv.hooks.LandsHook;
import xyz.superez.dynamickeepinv.hooks.TownyHook;
import xyz.superez.dynamickeepinv.hooks.WorldGuardHook;
import xyz.superez.dynamickeepinv.rules.DeathStreakRule;

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
            sender.sendMessage(plugin.parseMessage("&7/dki test [player] &8- &fDiagnose rules for a player"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status":
                showStatus(sender);
                break;

            case "reload":
                new xyz.superez.dynamickeepinv.ConfigMigration(plugin).checkAndMigrate();
                plugin.reloadConfig();
                plugin.loadMessages();
                plugin.reloadIntegrations();
                plugin.reloadRuleManager();
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
                plugin.refreshDKIConfig();
                plugin.startChecking();
                sender.sendMessage(plugin.parseMessage(plugin.getMessage("commands.enabled")));
                break;

            case "disable":
                plugin.getConfig().set("enabled", false);
                plugin.saveConfig();
                plugin.refreshDKIConfig();
                plugin.stopChecking(true);
                sender.sendMessage(plugin.parseMessage(plugin.getMessage("commands.disabled")));
                break;

            case "toggle":
                boolean newState = !plugin.getConfig().getBoolean("enabled", true);
                plugin.getConfig().set("enabled", newState);
                plugin.saveConfig();
                plugin.refreshDKIConfig();
                if (newState) {
                    plugin.startChecking();
                    sender.sendMessage(plugin.parseMessage(plugin.getMessage("commands.enabled")));
                } else {
                    plugin.stopChecking(true);
                    sender.sendMessage(plugin.parseMessage(plugin.getMessage("commands.disabled")));
                }
                break;

            case "test":
                handleTestCommand(sender, args);
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

        DKIConfig cfg = plugin.getDKIConfig();
        if (!cfg.economyEnabled || cfg.economyMode != EconomyMode.GUI) {
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

        DKIConfig cfg = plugin.getDKIConfig();
        if (!cfg.economyEnabled || cfg.economyMode != EconomyMode.GUI) {
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

    /**
     * Diagnostic command: walks the same rule chain as RuleManager and reports
     * which rule would fire for the target player at their current location.
     *
     * Rule order mirrors DynamicKeepInvPlugin.setupRuleManager():
     *   BypassPermissionRule → FirstDeathRule → DeathStreakRule
     *   → ProtectionRule → DeathCauseRule (*) → WorldTimeRule
     *
     * (*) DeathCauseRule requires a real killer from a PlayerDeathEvent and cannot
     * be simulated here. It will be shown as a note if enabled.
     */
    private void handleTestCommand(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.parseMessage("&cPlayer &f" + args[1] + " &cnot found or offline."));
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(plugin.parseMessage("&cUsage: /dki test [player]"));
            return;
        }

        DKIConfig cfg = plugin.getDKIConfig();
        sender.sendMessage(plugin.parseMessage("&7--- &eDKI Diagnostic: &f" + target.getName() + " &7---"));
        sender.sendMessage(plugin.parseMessage("&7World: &f" + target.getWorld().getName()
                + " &7| Loc: &f" + target.getLocation().getBlockX()
                + "," + target.getLocation().getBlockY()
                + "," + target.getLocation().getBlockZ()));

        if (!cfg.enabled) {
            sender.sendMessage(plugin.parseMessage("&c[DISABLED] Plugin is disabled globally."));
            return;
        }

        // 1. BypassPermissionRule
        if (cfg.bypassPermissionEnabled && target.hasPermission("dynamickeepinv.bypass")) {
            sender.sendMessage(plugin.parseMessage("&a[BYPASS] Has bypass permission → keep items & xp"));
            return;
        }

        // 2. FirstDeathRule
        if (cfg.firstDeathEnabled && plugin.getStatsManager() != null) {
            int deaths = plugin.getStatsManager().getTotalDeaths(target.getUniqueId());
            if (deaths == 0) {
                sender.sendMessage(plugin.parseMessage("&a[FIRST-DEATH] 0 deaths on record"
                        + " → keepItems=" + cfg.firstDeathKeepItems
                        + " keepXp=" + cfg.firstDeathKeepXp));
                return;
            }
        }

        // 3. DeathStreakRule
        if (cfg.deathStreakEnabled) {
            xyz.superez.dynamickeepinv.rules.RuleManager rm = plugin.getRuleManager();
            DeathStreakRule streakRule = rm != null ? rm.getRule(DeathStreakRule.class) : null;
            int streakCount = streakRule != null
                    ? streakRule.getRecentDeathCount(target.getUniqueId(), (long) cfg.deathStreakWindowSec * 1000L)
                    : 0;
            sender.sendMessage(plugin.parseMessage("&7[STREAK] " + streakCount + "/" + cfg.deathStreakThreshold
                    + " deaths in window (" + cfg.deathStreakWindowSec + "s)"
                    + (streakCount >= cfg.deathStreakThreshold
                       ? " &a→ TRIGGERED keepItems=" + cfg.deathStreakKeepItems + " keepXp=" + cfg.deathStreakKeepXp
                       : " &7→ not triggered")));
            if (streakCount >= cfg.deathStreakThreshold) return;
        }

        // 4. ProtectionRule — Lands
        if (plugin.isLandsEnabled() && cfg.landsEnabled) {
            LandsHook lands = plugin.getLandsHook();
            boolean inLand = lands.isInLand(target.getLocation());
            if (inLand) {
                if (!cfg.landsOverride) {
                    sender.sendMessage(plugin.parseMessage("&e[LANDS] In land, override disabled → deferred to Lands plugin"));
                    return;
                }
                boolean own = lands.isInOwnLand(target);
                sender.sendMessage(plugin.parseMessage("&e[LANDS] In " + (own ? "own" : "other") + " land"
                        + " → keepItems=" + (own ? cfg.landsOwnKeepItems : cfg.landsOtherKeepItems)
                        + " keepXp=" + (own ? cfg.landsOwnKeepXp : cfg.landsOtherKeepXp)));
                return;
            } else if (cfg.landsWildernessEnabled && !cfg.landsWildernessUseDeathCause) {
                sender.sendMessage(plugin.parseMessage("&e[LANDS-WILD] In Lands wilderness"
                        + " → keepItems=" + cfg.landsWildernessKeepItems
                        + " keepXp=" + cfg.landsWildernessKeepXp));
                return;
            }
        }

        // 4. ProtectionRule — GriefPrevention
        if (plugin.isGriefPreventionEnabled() && cfg.gpEnabled) {
            GriefPreventionHook gp = plugin.getGriefPreventionHook();
            if (gp.isInClaim(target.getLocation())) {
                boolean own = gp.isInOwnClaim(target);
                sender.sendMessage(plugin.parseMessage("&e[GP] In " + (own ? "own" : "other") + " claim"
                        + " → keepItems=" + (own ? cfg.gpOwnKeepItems : cfg.gpOtherKeepItems)
                        + " keepXp=" + (own ? cfg.gpOwnKeepXp : cfg.gpOtherKeepXp)));
                return;
            } else if (cfg.gpWildernessEnabled && !cfg.gpWildernessUseDeathCause) {
                sender.sendMessage(plugin.parseMessage("&e[GP-WILD] In GP wilderness"
                        + " → keepItems=" + cfg.gpWildernessKeepItems
                        + " keepXp=" + cfg.gpWildernessKeepXp));
                return;
            }
        }

        // 4. ProtectionRule — WorldGuard
        if (plugin.isWorldGuardEnabled() && cfg.worldGuardEnabled) {
            WorldGuardHook wg = plugin.getWorldGuardHook();
            if (wg.isInRegion(target.getLocation())) {
                boolean own = wg.isInOwnRegion(target);
                sender.sendMessage(plugin.parseMessage("&e[WG] In " + (own ? "own" : "other") + " region"
                        + " → keepItems=" + (own ? cfg.worldGuardOwnRegionKeepItems : cfg.worldGuardOtherRegionKeepItems)
                        + " keepXp=" + (own ? cfg.worldGuardOwnRegionKeepXp : cfg.worldGuardOtherRegionKeepXp)));
                return;
            } else if (cfg.worldGuardWildernessEnabled) {
                sender.sendMessage(plugin.parseMessage("&e[WG-WILD] In WG wilderness"
                        + " → keepItems=" + cfg.worldGuardWildernessKeepItems
                        + " keepXp=" + cfg.worldGuardWildernessKeepXp));
                return;
            }
        }

        // 4. ProtectionRule — Towny
        if (plugin.isTownyEnabled() && cfg.townyEnabled) {
            TownyHook towny = plugin.getTownyHook();
            if (towny.isInTown(target.getLocation())) {
                boolean resident = towny.isInOwnTown(target);
                String town = towny.getTownName(target.getLocation());
                sender.sendMessage(plugin.parseMessage("&e[TOWNY] In town " + (town != null ? town : "?")
                        + " (resident=" + resident + ")"
                        + " → keepItems=" + (resident ? cfg.townyOwnTownKeepItems : cfg.townyOtherTownKeepItems)
                        + " keepXp=" + (resident ? cfg.townyOwnTownKeepXp : cfg.townyOtherTownKeepXp)));
                return;
            } else if (cfg.townyWildernessEnabled) {
                sender.sendMessage(plugin.parseMessage("&e[TOWNY-WILD] In Towny wilderness"
                        + " → keepItems=" + cfg.townyWildernessKeepItems
                        + " keepXp=" + cfg.townyWildernessKeepXp));
                return;
            }
        }

        // 5. DeathCauseRule — cannot simulate without a real killer
        if (cfg.deathCauseEnabled) {
            sender.sendMessage(plugin.parseMessage("&7[DEATH-CAUSE] Enabled (pvp/pve)"
                    + " — cannot simulate without real death; killer unknown at this location."
                    + " PvP → keepItems=" + cfg.pvpKeepItems
                    + " | PvE → keepItems=" + cfg.pveKeepItems));
        }

        // 6. WorldTimeRule (fallback)
        long time = target.getWorld().getTime();
        boolean isDay = plugin.isTimeInRange(time, cfg.dayStart, cfg.nightStart);
        DKIConfig.WorldTimeOverride override = cfg.worldOverrides.get(target.getWorld().getName());
        boolean keepItems = isDay ? cfg.dayKeepItems : cfg.nightKeepItems;
        boolean keepXp    = isDay ? cfg.dayKeepXp    : cfg.nightKeepXp;
        if (override != null) {
            Boolean ov = isDay ? override.day() : override.night();
            if (ov != null) keepItems = ov;
        }
        sender.sendMessage(plugin.parseMessage("&7[TIME] " + (isDay ? "&aDay" : "&9Night")
                + " &7(time=" + time + ")"
                + " → keepItems=" + keepItems + " keepXp=" + keepXp));
    }

    private void showStatus(CommandSender sender) {
        DKIConfig cfg = plugin.getDKIConfig();
        sender.sendMessage(plugin.parseMessage(plugin.getMessage("status.header")));
        sender.sendMessage(plugin.parseMessage(plugin.getMessage("status.enabled")
                .replace("{value}", String.valueOf(cfg.enabled))));
        sender.sendMessage(plugin.parseMessage(plugin.getMessage("status.keep-inv-day")
                .replace("{value}", String.valueOf(cfg.dayKeepItems))));
        sender.sendMessage(plugin.parseMessage(plugin.getMessage("status.keep-inv-night")
                .replace("{value}", String.valueOf(cfg.nightKeepItems))));
        sender.sendMessage(plugin.parseMessage(plugin.getMessage("status.check-interval")
                .replace("{value}", String.valueOf(cfg.checkInterval))));

        // Rule chain summary
        sender.sendMessage(plugin.parseMessage("&7--- &eActive Rule Chain &7---"));
        xyz.superez.dynamickeepinv.rules.RuleManager rm = plugin.getRuleManager();
        if (rm != null) {
            java.util.List<String> names = rm.getRuleNames();
            if (names.isEmpty()) {
                sender.sendMessage(plugin.parseMessage("&7  (none)"));
            } else {
                for (int i = 0; i < names.size(); i++) {
                    sender.sendMessage(plugin.parseMessage("&7  " + (i + 1) + ". &f" + names.get(i)));
                }
            }
        }

        sender.sendMessage(plugin.parseMessage(plugin.getMessage("status.world-header")));
        long dayStart = cfg.dayStart;
        long nightStart = cfg.nightStart;
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
