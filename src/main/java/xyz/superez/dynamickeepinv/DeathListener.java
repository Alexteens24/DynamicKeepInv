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
import xyz.superez.dynamickeepinv.rules.*;

public class DeathListener implements Listener {
    private final DynamicKeepInvPlugin plugin;
    private final RuleManager ruleManager;

    public DeathListener(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
        this.ruleManager = new RuleManager(plugin);

        // Register Rules in Order
        ruleManager.registerRule(new BypassPermissionRule(plugin));
        ruleManager.registerRule(new ProtectionRule(plugin));
        ruleManager.registerRule(new DeathCauseRule(plugin));
        ruleManager.registerRule(new WorldTimeRule(plugin));
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

        // Evaluate Rules
        RuleResult result = ruleManager.evaluate(player, event);

        if (result == null) {
            plugin.debug("No rule matched, this shouldn't happen as TimeRule is fallback.");
            return;
        }

        if (result.reason() != null && result.reason().equals("lands-defer")) {
            plugin.debug("Lands override disabled; deferring to Lands without altering drops.");
            return;
        }

        boolean keepItems = result.keepItems();
        boolean keepXp = result.keepXp();
        String reason = result.reason();

        final boolean baseKeepItems = keepItems;
        final boolean baseKeepXp = keepXp;
        final String baseReason = reason;

        plugin.debug("Base decision: keepItems=" + keepItems + ", keepXp=" + keepXp + ", reason=" + reason);

        // 5. Economy Check
        // Using local variables that can be effectively final or modified
        boolean finalKeepItems = keepItems;
        boolean finalKeepXp = keepXp;
        String finalReason = reason;

        if (plugin.getConfig().getBoolean("advanced.economy.enabled", false)) {
            double cost = plugin.getConfig().getDouble("advanced.economy.cost", 0.0);
            String mode = plugin.getConfig().getString("advanced.economy.mode", "charge-to-keep");
            plugin.debug("Economy enabled. Cost=" + cost + ", Mode=" + mode);

            // GUI mode - save inventory and show confirmation GUI on respawn
            if ("gui".equalsIgnoreCase(mode) && cost > 0 && !baseKeepItems) {
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
                            deathLocation.getX(),
                            deathLocation.getY(),
                            deathLocation.getZ(),
                            baseReason
                        );

                        // Only save if there are items worth saving
                        if (pendingDeath.hasItems() || player.getLevel() > 0) {
                            pendingManager.addPendingDeath(pendingDeath);

                            // IMPORTANT: Cancel drops and disable keepInventory
                            event.setKeepInventory(false);
                            event.setKeepLevel(false);
                            event.getDrops().clear();
                            event.setDroppedExp(0);

                            plugin.debug("Saved pending death with " + pendingDeath.countItems() + " items, cost=" + cost);
                            return; // Don't send death message here - will send in GUI
                        } else {
                            plugin.debug("GUI mode: No items or XP to save, skipping GUI");
                        }
                    } else {
                        plugin.debug("PendingDeathManager is null, falling back to normal mode");
                    }
                } else {
                    plugin.debug("Economy not available for GUI mode, forcing drops");
                    finalKeepItems = false;
                    finalKeepXp = false;
                }
            }

            boolean shouldProcessEconomy = false;
            if ("charge-to-bypass".equalsIgnoreCase(mode)) {
                // In bypass mode, we charge if the player WOULD lose items (to bypass the loss)
                shouldProcessEconomy = !finalKeepItems || !finalKeepXp;
                plugin.debug("Bypass mode check: keepItems=" + finalKeepItems + ", keepXp=" + finalKeepXp + ", shouldProcess=" + shouldProcessEconomy);
            } else {
                // In charge-to-keep mode, we charge if the player WOULD keep items (as a fee for keeping)
                shouldProcessEconomy = finalKeepItems || finalKeepXp;
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
                            plugin.debug("Bypass mode: Player cannot afford, using original keep settings (DROP).");
                            finalKeepItems = baseKeepItems;
                            finalKeepXp = baseKeepXp;
                        } else {
                            // Charge-to-keep: Failed to pay fee -> lose items
                            plugin.debug("Charge-to-keep: Player cannot afford fee, dropping items.");
                            finalKeepItems = false;
                            finalKeepXp = false;
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
                                finalKeepItems = baseKeepItems;
                                finalKeepXp = baseKeepXp;
                            } else {
                                finalKeepItems = false;
                                finalKeepXp = false;
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
                                finalKeepItems = true;
                                finalKeepXp = true;
                            }
                            // If charge-to-keep, we successfully paid fee, so we keep items (finalKeepItems is already true)
                        }
                    }
                } else {
                    plugin.debug("Economy provider not found or disabled.");
                }
            }
        } else {
            plugin.debug("Economy check skipped. EcoEnabled=" + plugin.getConfig().getBoolean("advanced.economy.enabled", false));
        }

        plugin.debug("Final decision: keepItems=" + finalKeepItems + ", keepXp=" + finalKeepXp);

        // APPLY THE SETTINGS TO THE EVENT
        applyKeepInventorySettings(event, finalKeepItems, finalKeepXp);
        plugin.debug("Event keepInventory after processing: " + event.getKeepInventory());

        // Check if we should create a grave (GravesX support)
        if (!finalKeepItems && plugin.isGravesXEnabled()) {
            if (event.getDrops() != null && !event.getDrops().isEmpty()) {
                plugin.debug("GravesX enabled and items dropped. Creating grave...");
                java.util.List<org.bukkit.inventory.ItemStack> dropsToSave = new java.util.ArrayList<>(event.getDrops());

                int xpToStore = finalKeepXp ? 0 : player.getTotalExperience();

                if (plugin.getGravesXHook().createGrave(player, deathLocation, dropsToSave, xpToStore)) {
                    event.getDrops().clear();
                    if (!finalKeepXp) {
                        event.setDroppedExp(0);
                    }
                    plugin.debug("Grave created with " + dropsToSave.size() + " items and " + xpToStore + " XP.");
                } else {
                     plugin.debug("Failed to create grave, items will drop normally.");
                }
            }
        }

        String reasonFinal;
        boolean economyBypass = plugin.getConfig().getBoolean("advanced.economy.enabled", false)
                && "charge-to-bypass".equalsIgnoreCase(plugin.getConfig().getString("advanced.economy.mode", "charge-to-keep"))
                && (finalKeepItems || finalKeepXp)
                && (!baseKeepItems || !baseKeepXp);

        if (economyBypass) {
            reasonFinal = "economy-bypass";
        } else {
            reasonFinal = baseReason;
        }

        trackDeathStats(player, finalKeepItems, finalKeepXp, reasonFinal);
        sendDeathMessage(player, finalKeepItems, finalKeepXp, reasonFinal);

        plugin.debug("Event keepInventory FINAL: " + event.getKeepInventory());
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

        String simpleReason = "unknown";
        if (reason != null) {
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
            } else if (reason.equals("bypass")) {
                simpleReason = "bypass";
            } else if (reason.equals("economy-bypass")) {
                simpleReason = "economy";
            }
        }

        if (keepItems || keepXp) {
            stats.recordDeathSaved(player, simpleReason);
        } else {
            stats.recordDeathLost(player, simpleReason);
        }
    }

    // ===== GUI Mode Event Handlers =====

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfig().getBoolean("advanced.economy.enabled", false)) return;
        if (!"gui".equalsIgnoreCase(plugin.getConfig().getString("advanced.economy.mode", "charge-to-keep"))) return;

        Player player = event.getPlayer();
        PendingDeathManager pendingManager = plugin.getPendingDeathManager();
        if (pendingManager == null) return;

        PendingDeath pending = pendingManager.getPendingDeath(player.getUniqueId());
        if (pending == null || pending.isProcessed()) return;

        plugin.debug("Player " + player.getName() + " respawned with pending death, opening GUI");

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
        Player player = event.getPlayer();
        PendingDeathManager pendingManager = plugin.getPendingDeathManager();

        if (pendingManager != null) {
            pendingManager.preloadAutoPay(player.getUniqueId());
        }

        if (!plugin.getConfig().getBoolean("advanced.economy.enabled", false)) return;
        if (!"gui".equalsIgnoreCase(plugin.getConfig().getString("advanced.economy.mode", "charge-to-keep"))) return;

        if (pendingManager == null) return;

        PendingDeath pending = pendingManager.handlePlayerJoin(player.getUniqueId());
        if (pending == null) return;

        plugin.debug("Player " + player.getName() + " joined with pending death from before disconnect");

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

        if (pendingManager.isAutoPayEnabled(player.getUniqueId())) {
            plugin.debug("Player " + player.getName() + " has auto-pay enabled, attempting auto-pay");

            if (pendingManager.processAutoPay(player, pending)) {
                plugin.debug("Auto-pay successful for " + player.getName());
                return;
            }

            plugin.debug("Auto-pay failed for " + player.getName() + ", showing GUI instead");
        }

        DeathConfirmGUI gui = plugin.getDeathConfirmGUI();
        if (gui != null) {
            gui.openGUI(player, pending);
        }
    }
}
