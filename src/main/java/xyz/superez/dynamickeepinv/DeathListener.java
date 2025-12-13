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

        // 1. Bypass Permission Check (Highest Priority)
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

        // Initialize decision variables
        boolean keepItems = false;
        boolean keepXp = false;
        String reason = null;
        boolean resolved = false;

        // 2. Protection Plugins Check
        ProtectionResult protectionResult = checkProtectionPlugins(player, deathLocation);

        if (protectionResult.handled) {
            if ("lands-defer".equals(protectionResult.reason)) {
                plugin.debug("Lands override disabled; deferring to Lands without altering drops.");
                return;
            }

            // Check for wilderness special handling
            boolean isWilderness = protectionResult.reason != null && protectionResult.reason.contains("wilderness");

            if (!isWilderness) {
                // Claimed area - strict priority over death cause
                keepItems = protectionResult.keepItems;
                keepXp = protectionResult.keepXp;
                reason = protectionResult.reason;
                resolved = true;
                plugin.debug("Death handled by protection plugin (claimed area): keepItems=" + keepItems + ", keepXp=" + keepXp);
            } else {
                // Wilderness handling
                boolean wildernessUseDeathCause = false;
                if (protectionResult.reason.contains("lands")) {
                    wildernessUseDeathCause = plugin.getConfig().getBoolean("advanced.protection.lands.wilderness.use-death-cause", false);
                } else if (protectionResult.reason.contains("griefprevention")) {
                    wildernessUseDeathCause = plugin.getConfig().getBoolean("advanced.protection.griefprevention.wilderness.use-death-cause", false);
                }

                if (!wildernessUseDeathCause) {
                    keepItems = protectionResult.keepItems;
                    keepXp = protectionResult.keepXp;
                    reason = protectionResult.reason;
                    resolved = true;
                    plugin.debug("Wilderness base settings (no death cause override): keepItems=" + keepItems + ", keepXp=" + keepXp);
                }
                // If wildernessUseDeathCause is true, we leave resolved = false to fall through to Death Cause check
            }
        }

        // 3. Death Cause Check (if not resolved by Claimed Area)
        if (!resolved && plugin.getConfig().getBoolean("advanced.death-cause.enabled", false)) {
            boolean isPvp = player.getKiller() != null;
            String causePath = isPvp ? "advanced.death-cause.pvp" : "advanced.death-cause.pve";

            // Get default values from config if available, otherwise default to false
            // Note: We use the *configured* values, not the current state, as base
            keepItems = plugin.getConfig().getBoolean(causePath + ".keep-items", false);
            keepXp = plugin.getConfig().getBoolean(causePath + ".keep-xp", false);
            reason = isPvp ? "pvp" : "pve";
            resolved = true;
            plugin.debug("Death cause enabled. isPvp=" + isPvp + ", keepItems=" + keepItems + ", keepXp=" + keepXp);
        }

        // 4. Wilderness / Time-based Fallback (if not yet resolved)
        if (!resolved) {
            long time = world.getTime();
            long dayStart = plugin.getConfig().getLong("day-start", 0);
            long nightStart = plugin.getConfig().getLong("night-start", 13000);
            boolean isDay = plugin.isTimeInRange(time, dayStart, nightStart);
            String baseReason = isDay ? "time-day" : "time-night";

            // If it was a wilderness case that deferred to death cause (but death cause was disabled/skipped)
            // we should technically fall back to wilderness settings IF we had them?
            // Actually, logical flow: Wilderness(use-death-cause=true) -> DeathCause(disabled) -> Time-based.
            // But if Wilderness had specific settings, should we use them?
            // The config implies "use-death-cause" REPLACES wilderness settings.
            // So falling back to Time-based seems correct for "Wilderness -> DeathCause -> Time".

            // However, let's just use time-based as the ultimate fallback.
            String settingPath = isDay ? "advanced.day" : "advanced.night";
            boolean defaultKeepItems = getWorldKeepInventory(world, isDay);
            keepItems = plugin.getConfig().getBoolean(settingPath + ".keep-items", defaultKeepItems);
            keepXp = plugin.getConfig().getBoolean(settingPath + ".keep-xp", defaultKeepItems);
            reason = baseReason;

            // If it was actually wilderness that brought us here via "use-death-cause", maybe note that?
            // But "time-day"/"time-night" is accurate enough as the source of the settings.
            plugin.debug("Time-based settings: Time=" + time + ", isDay=" + isDay + ", keepItems=" + keepItems + ", keepXp=" + keepXp);
        }

        final boolean baseKeepItems = keepItems;
        final boolean baseKeepXp = keepXp;
        final String baseReason = reason;

        // 5. Economy Check
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
                            // Wait, if base was drop, and we failed to pay, we stay drop.
                            // If base was keep, we wouldn't be here (shouldProcessEconomy is !keepItems).
                            // So this is correct.
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
            plugin.debug("Economy check skipped. EcoEnabled=" + plugin.getConfig().getBoolean("advanced.economy.enabled", false));
        }

        plugin.debug("Final decision: keepItems=" + keepItems + ", keepXp=" + keepXp);

        // APPLY THE SETTINGS TO THE EVENT
        applyKeepInventorySettings(event, keepItems, keepXp);
        plugin.debug("Event keepInventory after processing: " + event.getKeepInventory());

        // Check if we should create a grave (GravesX support)
        // Only if items are NOT kept (meaning they are dropped) and hook is enabled
        if (!keepItems && plugin.isGravesXEnabled()) {
            // We need to check if there are any items to put in the grave
            // applyKeepInventorySettings logic:
            // if keepItems is false:
            //   event.setKeepInventory(false);
            //   If drops were empty (e.g. gamerule keepInventory=true), it force populated event.getDrops()

            // So event.getDrops() should contain the items now.
            if (event.getDrops() != null && !event.getDrops().isEmpty()) {
                plugin.debug("GravesX enabled and items dropped. Creating grave...");
                // Create a list copy because passing event.getDrops() might be risky if we clear it later
                java.util.List<org.bukkit.inventory.ItemStack> dropsToSave = new java.util.ArrayList<>(event.getDrops());

                // Determine XP to store in grave
                // If keepXp is true, we should NOT put XP in the grave (player keeps it)
                // If keepXp is false, we put the XP in the grave
                int xpToStore = keepXp ? 0 : player.getTotalExperience();

                if (plugin.getGravesXHook().createGrave(player, deathLocation, dropsToSave, xpToStore)) {
                    // If we created a grave, we should clear the drops so they don't fall on the ground
                    event.getDrops().clear();

                    // If we stored XP in the grave, we must clear dropped XP to prevent duplication
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
        boolean economyBypass = plugin.getConfig().getBoolean("advanced.economy.enabled", false)
                && "charge-to-bypass".equalsIgnoreCase(plugin.getConfig().getString("advanced.economy.mode", "charge-to-keep"))
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
        Player player = event.getPlayer();
        PendingDeathManager pendingManager = plugin.getPendingDeathManager();

        // Preload auto-pay settings if manager exists
        if (pendingManager != null) {
            pendingManager.preloadAutoPay(player.getUniqueId());
        }

        // Check if GUI mode is enabled
        if (!plugin.getConfig().getBoolean("advanced.economy.enabled", false)) return;
        if (!"gui".equalsIgnoreCase(plugin.getConfig().getString("advanced.economy.mode", "charge-to-keep"))) return;

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
