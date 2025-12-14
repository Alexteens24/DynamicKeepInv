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
import xyz.superez.dynamickeepinv.rules.RuleResult;
import xyz.superez.dynamickeepinv.rules.RuleManager;

public class DeathListener implements Listener {
    private final DynamicKeepInvPlugin plugin;

    public DeathListener(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // The "advanced.enabled" check was removed in the new config.
        // We assume core "enabled" is sufficient, or maybe we check "rules.enabled"?
        // But the user just wanted cleaner structure.
        // The top level "enabled" controls the plugin's main time task.
        // But DeathListener should probably respect "enabled" too.
        // The old config had "advanced.enabled" inside advanced section.
        // I removed it in my new config structure assuming the main enabled or just having rules implies enabled.
        // Let's check "enabled" top level.
        if (!plugin.getConfig().getBoolean("enabled", true)) {
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

        // Evaluate rules via RuleManager
        RuleManager ruleManager = plugin.getRuleManager();
        if (ruleManager == null) {
            plugin.getLogger().severe("RuleManager is null! Aborting death handling.");
            return;
        }

        RuleResult result = ruleManager.evaluate(event);
        if (result == null) {
            plugin.debug("No rule matched! Using defaults (DROP).");
            result = new RuleResult(false, false, "unknown");
        }

        // Handle deferral (e.g. Lands-defer)
        if ("lands-defer".equals(result.reason())) {
            plugin.debug("Rule deferred to Lands without altering drops.");
            return;
        }

        boolean keepItems = result.keepItems();
        boolean keepXp = result.keepXp();
        String reason = result.reason();

        plugin.debug("Rule matched: " + reason + ", keepItems=" + keepItems + ", keepXp=" + keepXp);

        final boolean baseKeepItems = keepItems;
        final boolean baseKeepXp = keepXp;
        final String baseReason = reason;

        // 5. Economy Check
        if (plugin.getConfig().getBoolean("economy.enabled", false)) {
            double cost = plugin.getConfig().getDouble("economy.cost", 0.0);
            String mode = plugin.getConfig().getString("economy.mode", "charge-to-keep");
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
                // In bypass mode, we charge if the player WOULD lose items (to bypass the loss)
                shouldProcessEconomy = !keepItems || !keepXp;
                plugin.debug("Bypass mode check: keepItems=" + keepItems + ", keepXp=" + keepXp + ", shouldProcess=" + shouldProcessEconomy);
            } else {
                // In charge-to-keep mode, we charge if the player WOULD keep items (as a fee for keeping)
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
                            plugin.debug("Bypass mode: Player cannot afford, using original keep settings (DROP).");
                            // Failed to pay for bypass -> use base setting (which was drop)
                            keepItems = baseKeepItems;
                            keepXp = baseKeepXp;
                        } else {
                            // Charge-to-keep: Failed to pay fee -> lose items
                            plugin.debug("Charge-to-keep: Player cannot afford fee, dropping items.");
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
                            // If charge-to-keep, we successfully paid fee, so we keep items (keepItems is already true)
                        }
                    }
                } else {
                    plugin.debug("Economy provider not found or disabled.");
                }
            }
        } else {
            plugin.debug("Economy check skipped. EcoEnabled=" + plugin.getConfig().getBoolean("economy.enabled", false));
        }

        plugin.debug("Final decision: keepItems=" + keepItems + ", keepXp=" + keepXp);

        // APPLY THE SETTINGS TO THE EVENT
        applyKeepInventorySettings(event, keepItems, keepXp);
        plugin.debug("Event keepInventory after processing: " + event.getKeepInventory());

        // Check if we should create a grave (GravesX support)
        // Only if items are NOT kept (meaning they are dropped) and hook is enabled
        if (!keepItems && plugin.isGravesXEnabled()) {
            if (event.getDrops() != null && !event.getDrops().isEmpty()) {
                plugin.debug("GravesX enabled and items dropped. Creating grave...");
                // Create a list copy because passing event.getDrops() might be risky if we clear it later
                java.util.List<org.bukkit.inventory.ItemStack> dropsToSave = new java.util.ArrayList<>(event.getDrops());

                // Determine XP to store in grave
                int xpToStore = keepXp ? 0 : player.getTotalExperience();

                if (plugin.getGravesXHook().createGrave(player, deathLocation, dropsToSave, xpToStore)) {
                    event.getDrops().clear();
                    if (!keepXp) {
                        event.setDroppedExp(0);
                    }
                    plugin.debug("Grave created with " + dropsToSave.size() + " items and " + xpToStore + " XP.");
                } else {
                     plugin.debug("Failed to create grave, items will drop normally.");
                }
            }
        }

        String reasonFinal;
        boolean economyBypass = plugin.getConfig().getBoolean("economy.enabled", false)
                && "charge-to-bypass".equalsIgnoreCase(plugin.getConfig().getString("economy.mode", "charge-to-keep"))
                && (keepItems || keepXp)
                && (!baseKeepItems || !baseKeepXp);

        if (economyBypass) {
            reasonFinal = "economy-bypass";
        } else {
            reasonFinal = baseReason;
        }

        trackDeathStats(player, keepItems, keepXp, reasonFinal);
        sendDeathMessage(player, keepItems, keepXp, reasonFinal);

        plugin.debug("Event keepInventory FINAL: " + event.getKeepInventory());
    }

    private void sendDeathMessage(Player player, boolean keepItems, boolean keepXp, String reason) {
        if (!plugin.getConfig().getBoolean("messages.death.enabled", true)) {
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

        if (plugin.getConfig().getBoolean("messages.death.chat", true)) {
            player.sendMessage(plugin.parseMessage(message));
        }

        if (plugin.getConfig().getBoolean("messages.death.action-bar", false)) {
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

        // Normalize reason for stats
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

    // ===== GUI Mode Event Handlers =====

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Check if GUI mode is enabled
        if (!plugin.getConfig().getBoolean("economy.enabled", false)) return;
        if (!"gui".equalsIgnoreCase(plugin.getConfig().getString("economy.mode", "charge-to-keep"))) return;

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
        Player player = event.getPlayer();
        PendingDeathManager pendingManager = plugin.getPendingDeathManager();

        // Preload auto-pay settings if manager exists
        if (pendingManager != null) {
            pendingManager.preloadAutoPay(player.getUniqueId());
        }

        // Check if GUI mode is enabled
        if (!plugin.getConfig().getBoolean("economy.enabled", false)) return;
        if (!"gui".equalsIgnoreCase(plugin.getConfig().getString("economy.mode", "charge-to-keep"))) return;

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
