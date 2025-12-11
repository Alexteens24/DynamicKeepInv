package xyz.superez.dynamickeepinv;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import xyz.superez.dynamickeepinv.hooks.LandsHook;
import xyz.superez.dynamickeepinv.hooks.GriefPreventionHook;

public class DeathListener implements Listener {
    private final DynamicKeepInvPlugin plugin;

    public DeathListener(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("advanced.enabled", false)) {
            return;
        }

        Player player = event.getEntity();
        World world = player.getWorld();
        Location deathLocation = player.getLocation();

        if (!plugin.isWorldEnabled(world)) {
            return;
        }

        plugin.debug("Advanced death handling triggered for " + player.getName());
        plugin.debug("Current gamerule KEEP_INVENTORY: " + world.getGameRuleValue(org.bukkit.GameRule.KEEP_INVENTORY));
        plugin.debug("Event keepInventory before processing: " + event.getKeepInventory());

        // If gamerule keepInventory is already true, no need to process or show messages
        Boolean gameruleValue = world.getGameRuleValue(org.bukkit.GameRule.KEEP_INVENTORY);
        if (gameruleValue != null && gameruleValue) {
            plugin.debug("Gamerule KEEP_INVENTORY is true, skipping advanced death handling.");
            return;
        }

        if (plugin.getConfig().getBoolean("advanced.bypass-permission", true)) {
            if (player.hasPermission("dynamickeepinv.bypass")) {
                plugin.debug("Player " + player.getName() + " has bypass permission. Keeping inventory.");
                event.setKeepInventory(true);
                event.setKeepLevel(true);
                event.setDroppedExp(0);
                if (event.getDrops() != null) {
                    event.getDrops().clear();
                }
                trackDeathStats(player, true, true, "bypass");
                sendDeathMessage(player, true, true, "bypass");
                return;
            }
        }

        ProtectionResult protectionResult = checkProtectionPlugins(player, deathLocation);

        if (protectionResult.handled && "lands-defer".equals(protectionResult.reason)) {
            plugin.debug("Lands override disabled; deferring to Lands without altering drops.");
            return;
        }

        // Protection plugins in claimed areas have highest priority - return immediately
        if (protectionResult.handled && !protectionResult.reason.contains("wilderness")) {
            plugin.debug("Death handled by protection plugin (claimed area): keepItems=" + protectionResult.keepItems + ", keepXp=" + protectionResult.keepXp);
            applyKeepInventorySettings(event, protectionResult.keepItems, protectionResult.keepXp);
            trackDeathStats(player, protectionResult.keepItems, protectionResult.keepXp, protectionResult.reason);
            sendDeathMessage(player, protectionResult.keepItems, protectionResult.keepXp, protectionResult.reason);
            return;
        }

        // Check if wilderness should use death-cause instead
        boolean wildernessUseDeathCause = false;
        if (protectionResult.handled && protectionResult.reason.contains("wilderness")) {
            // Check which protection plugin's wilderness setting
            if (protectionResult.reason.contains("lands")) {
                wildernessUseDeathCause = plugin.getConfig().getBoolean("advanced.protection.lands.wilderness.use-death-cause", false);
            } else if (protectionResult.reason.contains("griefprevention")) {
                wildernessUseDeathCause = plugin.getConfig().getBoolean("advanced.protection.griefprevention.wilderness.use-death-cause", false);
            }
            plugin.debug("Wilderness use-death-cause: " + wildernessUseDeathCause);
        }

        // For wilderness with use-death-cause=true, skip wilderness settings and go straight to death-cause
        boolean keepItems, keepXp;
        String baseReason;

        if (protectionResult.handled && !wildernessUseDeathCause) {
            // Wilderness with fixed settings - use as base values
            keepItems = protectionResult.keepItems;
            keepXp = protectionResult.keepXp;
            baseReason = protectionResult.reason;
            plugin.debug("Wilderness base settings: keepItems=" + keepItems + ", keepXp=" + keepXp);
        } else if (protectionResult.handled && wildernessUseDeathCause) {
            // Wilderness but defer to death-cause - use time-based as fallback
            long time = world.getTime();
            long dayStart = plugin.getConfig().getLong("day-start", 0);
            long nightStart = plugin.getConfig().getLong("night-start", 13000);
            boolean isDay = plugin.isTimeInRange(time, dayStart, nightStart);

            String settingPath = isDay ? "advanced.day" : "advanced.night";
            boolean defaultKeepItems = getWorldKeepInventory(world, isDay);
            keepItems = plugin.getConfig().getBoolean(settingPath + ".keep-items", defaultKeepItems);
            keepXp = plugin.getConfig().getBoolean(settingPath + ".keep-xp", defaultKeepItems);
            baseReason = isDay ? "time-day" : "time-night";
            plugin.debug("Wilderness with use-death-cause=true, using time-based: Time=" + time + ", isDay=" + isDay + ", keepItems=" + keepItems + ", keepXp=" + keepXp);
        } else {
            // No protection plugin handling - use time-based settings
            long time = world.getTime();
            long dayStart = plugin.getConfig().getLong("day-start", 0);
            long nightStart = plugin.getConfig().getLong("night-start", 13000);
            boolean isDay = plugin.isTimeInRange(time, dayStart, nightStart);

            String settingPath = isDay ? "advanced.day" : "advanced.night";
            boolean defaultKeepItems = getWorldKeepInventory(world, isDay);
            keepItems = plugin.getConfig().getBoolean(settingPath + ".keep-items", defaultKeepItems);
            keepXp = plugin.getConfig().getBoolean(settingPath + ".keep-xp", defaultKeepItems);
            baseReason = isDay ? "time-day" : "time-night";
            plugin.debug("Time-based settings: Time=" + time + ", isDay=" + isDay + ", keepItems=" + keepItems + ", keepXp=" + keepXp);
        }

        // Death cause can override wilderness (with use-death-cause) and time-based settings
        if (plugin.getConfig().getBoolean("advanced.death-cause.enabled", false)) {
            boolean isPvp = player.getKiller() != null;
            String causePath = isPvp ? "advanced.death-cause.pvp" : "advanced.death-cause.pve";
            plugin.debug("Death cause enabled. isPvp=" + isPvp + " (cause: " + (isPvp ? "PvP" : "PvE") + ")");

            boolean oldKeepItems = keepItems;
            boolean oldKeepXp = keepXp;
            keepItems = plugin.getConfig().getBoolean(causePath + ".keep-items", keepItems);
            keepXp = plugin.getConfig().getBoolean(causePath + ".keep-xp", keepXp);

            if (oldKeepItems != keepItems || oldKeepXp != keepXp) {
                plugin.debug("Death cause OVERRIDE: keepItems " + oldKeepItems + " -> " + keepItems + ", keepXp " + oldKeepXp + " -> " + keepXp);
                baseReason = isPvp ? "pvp" : "pve";
            }
        }

        final boolean baseKeepItems = keepItems;
        final boolean baseKeepXp = keepXp;

        if (plugin.getConfig().getBoolean("advanced.economy.enabled", false)) {
            double cost = plugin.getConfig().getDouble("advanced.economy.cost", 0.0);
            String mode = plugin.getConfig().getString("advanced.economy.mode", "charge-to-keep");
            plugin.debug("Economy enabled. Cost=" + cost + ", Mode=" + mode);

            // GUI mode - save inventory and show confirmation GUI on respawn
            if ("gui".equalsIgnoreCase(mode) && cost > 0) {
                EconomyManager eco = plugin.getEconomyManager();
                if (eco != null && eco.isEnabled()) {
                    plugin.debug("GUI mode: Saving inventory for confirmation GUI");

                    // Save inventory to pending death
                    PendingDeathManager pendingManager = plugin.getPendingDeathManager();
                    if (pendingManager != null) {
                        PendingDeath pendingDeath = new PendingDeath(
                            player.getUniqueId(),
                            player.getName(),
                            player.getInventory().getContents(),
                            player.getInventory().getArmorContents(),
                            player.getInventory().getItemInOffHand(),
                            player.getLevel(),
                            player.getExp(),
                            cost,
                            world.getName(),
                            baseReason
                        );

                        // Only save if there are items worth saving
                        if (pendingDeath.hasItems() || player.getLevel() > 0) {
                            pendingManager.addPendingDeath(pendingDeath);

                            // IMPORTANT: Cancel drops and disable keepInventory
                            // This removes items from the player (standard death) but prevents them from dropping
                            // We've already saved the inventory in PendingDeathManager
                            event.setKeepInventory(false);
                            event.setKeepLevel(false);
                            event.getDrops().clear();
                            event.setDroppedExp(0);

                            // No need to schedule inventory clear as keepInventory=false will handle it

                            plugin.debug("Saved pending death with " + pendingDeath.countItems() + " items, cost=" + cost);

                            // Don't send death message here - will send in GUI
                            return;
                        } else {
                            plugin.debug("GUI mode: No items or XP to save, skipping GUI");
                        }
                    } else {
                        plugin.debug("PendingDeathManager is null, falling back to normal mode");
                    }
                } else {
                    plugin.debug("Economy not available for GUI mode, forcing drops");
                    // Economy unavailable while GUI mode requested -> force drop to avoid free keep
                    keepItems = false;
                    keepXp = false;
                }
            }

            boolean shouldProcessEconomy = false;
            if ("charge-to-bypass".equalsIgnoreCase(mode)) {
                shouldProcessEconomy = !keepItems || !keepXp;
                plugin.debug("Bypass mode check: keepItems=" + keepItems + ", keepXp=" + keepXp + ", shouldProcess=" + shouldProcessEconomy);
            } else {
                shouldProcessEconomy = keepItems || keepXp;
            }

            if (cost > 0 && shouldProcessEconomy) {
                EconomyManager eco = plugin.getEconomyManager();
                if (eco == null) {
                    plugin.debug("EconomyManager is null, skipping economy check.");
                } else if (eco.isEnabled()) {
                    if (!eco.hasEnough(player, cost)) {
                        plugin.debug("Player " + player.getName() + " does not have enough money.");
                        String msg = plugin.getMessage("economy.not-enough-money")
                                .replace("{amount}", eco.format(cost));
                        player.sendMessage(plugin.parseMessage(msg));
                        if ("charge-to-bypass".equalsIgnoreCase(mode)) {
                            plugin.debug("Bypass mode: Player cannot afford, using original keep settings.");
                            keepItems = baseKeepItems;
                            keepXp = baseKeepXp;
                        } else {
                            keepItems = false;
                            keepXp = false;
                        }
                    } else {
                        plugin.debug("Charging player " + player.getName() + " " + cost);
                        boolean success = eco.withdraw(player, cost);
                        if (!success) {
                            plugin.debug("Economy withdrawal failed for player " + player.getName() + ".");
                            String msg = plugin.getMessage("economy.payment-failed")
                                .replace("{amount}", eco.format(cost));
                            player.sendMessage(plugin.parseMessage(msg));
                            if ("charge-to-bypass".equalsIgnoreCase(mode)) {
                                plugin.debug("Bypass mode: Payment failed, reverting to original keep settings.");
                                keepItems = baseKeepItems;
                                keepXp = baseKeepXp;
                            } else {
                                keepItems = false;
                                keepXp = false;
                            }
                        } else {
                            String msg = plugin.getMessage("economy.paid")
                                .replace("{amount}", eco.format(cost));
                            player.sendMessage(plugin.parseMessage(msg));
                            StatsManager stats = plugin.getStatsManager();
                            if (stats != null) {
                                stats.recordEconomyPayment(player, cost);
                            }
                            if ("charge-to-bypass".equalsIgnoreCase(mode)) {
                                plugin.debug("Bypass mode: Payment successful, keeping items.");
                                keepItems = true;
                                keepXp = true;
                            }
                        }
                    }
                } else {
                    plugin.debug("Economy provider not found or disabled.");
                }
            }
        } else {
            plugin.debug("Economy check skipped. EcoEnabled=" + plugin.getConfig().getBoolean("advanced.economy.enabled", false));
        }

        plugin.debug("Final decision: keepItems=" + keepItems + ", keepXp=" + keepXp);
        plugin.debug("Event keepInventory after processing: " + event.getKeepInventory());
        applyKeepInventorySettings(event, keepItems, keepXp);

        String reason;
        boolean economyBypass = plugin.getConfig().getBoolean("advanced.economy.enabled", false)
                && "charge-to-bypass".equalsIgnoreCase(plugin.getConfig().getString("advanced.economy.mode", "charge-to-keep"))
                && (keepItems || keepXp)
                && (!baseKeepItems || !baseKeepXp);

        if (economyBypass) {
            reason = "economy-bypass";
        } else {
            reason = baseReason;
        }

        trackDeathStats(player, keepItems, keepXp, reason);
        sendDeathMessage(player, keepItems, keepXp, reason);

        plugin.debug("Event keepInventory FINAL: " + event.getKeepInventory());
    }

    private boolean getWorldKeepInventory(World world, boolean isDay) {
        String worldName = world.getName();
        String worldPath = "world-settings." + worldName;

        if (plugin.getConfig().contains(worldPath)) {
            String timePath = isDay ? ".keep-inventory-day" : ".keep-inventory-night";
            if (plugin.getConfig().contains(worldPath + timePath)) {
                return plugin.getConfig().getBoolean(worldPath + timePath);
            }
        }

        // Fallback to global settings
        return isDay
            ? plugin.getConfig().getBoolean("keep-inventory-day", true)
            : plugin.getConfig().getBoolean("keep-inventory-night", false);
    }

    private void sendDeathMessage(Player player, boolean keepItems, boolean keepXp, String reason) {
        if (!plugin.getConfig().getBoolean("advanced.death-message.enabled", true)) {
            return;
        }

        String messageKey;
        if (keepItems && keepXp) {
            messageKey = "death.keep-all";
        } else if (keepItems) {
            messageKey = "death.keep-items";
        } else if (keepXp) {
            messageKey = "death.keep-xp";
        } else {
            messageKey = "death.lost-all";
        }

        if ("bypass".equals(reason)) {
            messageKey = "death.bypass";
        }

        String message = plugin.getMessage(messageKey);
        String reasonMsg = plugin.getMessage("death." + reason);
        if (reasonMsg != null && !reasonMsg.startsWith("Missing message:")) {
            message = message + " " + reasonMsg;
        }

        if (plugin.getConfig().getBoolean("advanced.death-message.chat", true)) {
            player.sendMessage(plugin.parseMessage(message));
        }

        if (plugin.getConfig().getBoolean("advanced.death-message.action-bar", false)) {
            player.sendActionBar(plugin.parseMessage(message));
        }
    }

    private void trackDeathStats(Player player, boolean keepItems, boolean keepXp, String reason) {
        if (!plugin.getConfig().getBoolean("stats.enabled", true)) {
            return;
        }

        StatsManager stats = plugin.getStatsManager();
        if (stats == null) {
            return;
        }

        String simpleReason = reason;
        if (reason.contains("time-day") || reason.contains("day")) {
            simpleReason = "day";
        } else if (reason.contains("time-night") || reason.contains("night")) {
            simpleReason = "night";
        } else if (reason.contains("pvp")) {
            simpleReason = "pvp";
        } else if (reason.contains("pve")) {
            simpleReason = "pve";
        } else if (reason.contains("lands")) {
            simpleReason = "lands";
        } else if (reason.contains("gp")) {
            simpleReason = "griefprevention";
        }

        if (keepItems || keepXp) {
            stats.recordDeathSaved(player, simpleReason);
        } else {
            stats.recordDeathLost(player, simpleReason);
        }
    }

    private ProtectionResult checkProtectionPlugins(Player player, Location location) {
        plugin.debug("Checking protection plugins...");
        plugin.debug("Lands hook available: " + plugin.isLandsEnabled() + ", Config enabled: " + plugin.getConfig().getBoolean("advanced.protection.lands.enabled", false));

        if (plugin.isLandsEnabled() && plugin.getConfig().getBoolean("advanced.protection.lands.enabled", false)) {
            LandsHook lands = plugin.getLandsHook();
            boolean inLand = lands.isInLand(location);
            boolean overrideLands = plugin.getConfig().getBoolean("advanced.protection.lands.override-lands", false);
            plugin.debug("Player in Lands area: " + inLand + ", Override Lands settings: " + overrideLands);

            if (inLand) {
                if (!overrideLands) {
                    plugin.debug("In land but override-lands=false, letting Lands handle it.");
                    return new ProtectionResult(true, false, false, "lands-defer");
                }

                plugin.debug("Player died in a Lands area: " + lands.getLandName(location));

                boolean isOwnLand = lands.isInOwnLand(player);
                String configPath = isOwnLand ? "advanced.protection.lands.in-own-land" : "advanced.protection.lands.in-other-land";

                if (plugin.getConfig().contains(configPath)) {
                    boolean keepItems = plugin.getConfig().getBoolean(configPath + ".keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean(configPath + ".keep-xp", false);
                    plugin.debug("Lands settings for " + (isOwnLand ? "own" : "other") + " land: keepItems=" + keepItems + ", keepXp=" + keepXp);
                    String reason = isOwnLand ? "lands-own" : "lands-other";
                    return new ProtectionResult(true, keepItems, keepXp, reason);
                }
            } else {
                plugin.debug("Player died in WILDERNESS (outside any land)");
                if (plugin.getConfig().getBoolean("advanced.protection.lands.wilderness.enabled", false)) {
                    boolean keepItems = plugin.getConfig().getBoolean("advanced.protection.lands.wilderness.keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean("advanced.protection.lands.wilderness.keep-xp", false);
                    plugin.debug("Lands wilderness settings: keepItems=" + keepItems + ", keepXp=" + keepXp);
                    return new ProtectionResult(true, keepItems, keepXp, "lands-wilderness");
                }
            }
        }

        if (plugin.isGriefPreventionEnabled() && plugin.getConfig().getBoolean("advanced.protection.griefprevention.enabled", false)) {
            GriefPreventionHook gp = plugin.getGriefPreventionHook();
            if (gp.isInClaim(location)) {
                plugin.debug("Player died in a GriefPrevention claim owned by: " + gp.getClaimOwnerName(location));

                boolean isOwnClaim = gp.isInOwnClaim(player);
                String configPath = isOwnClaim ? "advanced.protection.griefprevention.in-own-claim" : "advanced.protection.griefprevention.in-other-claim";

                if (plugin.getConfig().contains(configPath)) {
                    boolean keepItems = plugin.getConfig().getBoolean(configPath + ".keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean(configPath + ".keep-xp", false);
                    plugin.debug("GriefPrevention settings for " + (isOwnClaim ? "own" : "other") + " claim: keepItems=" + keepItems + ", keepXp=" + keepXp);
                    String reason = isOwnClaim ? "gp-own" : "gp-other";
                    return new ProtectionResult(true, keepItems, keepXp, reason);
                }
            } else {
                if (plugin.getConfig().getBoolean("advanced.protection.griefprevention.wilderness.enabled", false)) {
                    boolean keepItems = plugin.getConfig().getBoolean("advanced.protection.griefprevention.wilderness.keep-items", false);
                    boolean keepXp = plugin.getConfig().getBoolean("advanced.protection.griefprevention.wilderness.keep-xp", false);
                    plugin.debug("GriefPrevention wilderness settings: keepItems=" + keepItems + ", keepXp=" + keepXp);
                    return new ProtectionResult(true, keepItems, keepXp, "gp-wilderness");
                }
            }
        }

        return new ProtectionResult(false, false, false, null);
    }

    private void applyKeepInventorySettings(PlayerDeathEvent event, boolean keepItems, boolean keepXp) {
        Player player = event.getEntity();
        plugin.debug("applyKeepInventorySettings: keepItems=" + keepItems + ", keepXp=" + keepXp);
        plugin.debug("Current drops size: " + (event.getDrops() != null ? event.getDrops().size() : "null"));
        plugin.debug("Current keepInventory: " + event.getKeepInventory());

        if (keepItems) {
            event.setKeepInventory(true);
            if (event.getDrops() != null) {
                event.getDrops().clear();
            }
            plugin.debug("Set to KEEP inventory");
        } else {
            event.setKeepInventory(false);

            Boolean gameruleKeepInv = player.getWorld().getGameRuleValue(org.bukkit.GameRule.KEEP_INVENTORY);
            boolean wasKeepingInventory = gameruleKeepInv != null && gameruleKeepInv;
            if (event.getDrops() != null && event.getDrops().isEmpty() && wasKeepingInventory) {
                plugin.debug("Drops empty and gamerule was keepInventory=true, forcing inventory to drops...");
                int addedItems = 0;
                for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
                    if (item != null && !item.getType().isAir()) {
                        event.getDrops().add(item.clone());
                        addedItems++;
                    }
                }
                for (org.bukkit.inventory.ItemStack item : player.getInventory().getArmorContents()) {
                    if (item != null && !item.getType().isAir()) {
                        event.getDrops().add(item.clone());
                        addedItems++;
                    }
                }
                org.bukkit.inventory.ItemStack offhand = player.getInventory().getItemInOffHand();
                if (offhand != null && !offhand.getType().isAir()) {
                    event.getDrops().add(offhand.clone());
                    addedItems++;
                }
                plugin.debug("Added " + addedItems + " items to drops");
                player.getInventory().clear();
            } else {
                int dropSize = (event.getDrops() != null) ? event.getDrops().size() : 0;
                plugin.debug("Drops already exist (" + dropSize + " items), skipping force drop");
            }
            plugin.debug("Set to DROP inventory");
        }

        if (keepXp) {
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        } else {
            event.setKeepLevel(false);
            if (event.getDroppedExp() == 0) {
                int level = player.getLevel();
                int exp = Math.min(level * 7, 100);
                event.setDroppedExp(exp);
            }
        }
    }

    private static class ProtectionResult {
        final boolean handled;
        final boolean keepItems;
        final boolean keepXp;
        final String reason;

        ProtectionResult(boolean handled, boolean keepItems, boolean keepXp, String reason) {
            this.handled = handled;
            this.keepItems = keepItems;
            this.keepXp = keepXp;
            this.reason = reason;
        }
    }

    // ===== GUI Mode Event Handlers =====

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Check if GUI mode is enabled
        if (!plugin.getConfig().getBoolean("advanced.economy.enabled", false)) return;
        if (!"gui".equalsIgnoreCase(plugin.getConfig().getString("advanced.economy.mode", "charge-to-keep"))) return;

        Player player = event.getPlayer();
        PendingDeathManager pendingManager = plugin.getPendingDeathManager();
        if (pendingManager == null) return;

        PendingDeath pending = pendingManager.getPendingDeath(player.getUniqueId());
        if (pending == null || pending.isProcessed()) return;

        plugin.debug("Player " + player.getName() + " respawned with pending death, opening GUI");

        // Schedule GUI open 1 tick later (for Folia compatibility and to ensure player is fully spawned)
        if (plugin.isFolia()) {
            player.getScheduler().runDelayed(plugin, task -> {
                if (player.isOnline()) {
                    openDeathConfirmGUI(player, pending);
                }
            }, null, 2L);
        } else {
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    openDeathConfirmGUI(player, pending);
                }
            }, 2L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Check if GUI mode is enabled
        if (!plugin.getConfig().getBoolean("advanced.economy.enabled", false)) return;
        if (!"gui".equalsIgnoreCase(plugin.getConfig().getString("advanced.economy.mode", "charge-to-keep"))) return;

        Player player = event.getPlayer();
        PendingDeathManager pendingManager = plugin.getPendingDeathManager();
        if (pendingManager == null) return;

        // Check for pending death from before disconnect
        PendingDeath pending = pendingManager.handlePlayerJoin(player.getUniqueId());
        if (pending == null) return;

        plugin.debug("Player " + player.getName() + " joined with pending death from before disconnect");

        // Schedule GUI open after a short delay
        if (plugin.isFolia()) {
            player.getScheduler().runDelayed(plugin, task -> {
                if (player.isOnline()) {
                    String msg = plugin.getMessage("economy.gui.rejoin-notice");
                    player.sendMessage(plugin.parseMessage(msg));
                    openDeathConfirmGUI(player, pending);
                }
            }, null, 40L); // 2 seconds
        } else {
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    String msg = plugin.getMessage("economy.gui.rejoin-notice");
                    player.sendMessage(plugin.parseMessage(msg));
                    openDeathConfirmGUI(player, pending);
                }
            }, 40L);
        }
    }

    private void openDeathConfirmGUI(Player player, PendingDeath pending) {
        PendingDeathManager pendingManager = plugin.getPendingDeathManager();
        if (pendingManager == null) return;

        // Check if auto-pay is enabled for this player
        if (pendingManager.isAutoPayEnabled(player.getUniqueId())) {
            plugin.debug("Player " + player.getName() + " has auto-pay enabled, attempting auto-pay");

            // Try to process auto-pay
            if (pendingManager.processAutoPay(player, pending)) {
                plugin.debug("Auto-pay successful for " + player.getName());
                return; // Success - no need to show GUI
            }

            // Auto-pay failed (not enough money) - fall through to show GUI
            plugin.debug("Auto-pay failed for " + player.getName() + ", showing GUI instead");
        }

        // Show GUI
        DeathConfirmGUI gui = plugin.getDeathConfirmGUI();
        if (gui != null) {
            gui.openGUI(player, pending);
        }
    }
}
