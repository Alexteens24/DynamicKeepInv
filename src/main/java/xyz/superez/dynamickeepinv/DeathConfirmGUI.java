package xyz.superez.dynamickeepinv;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GUI for players to confirm whether to pay to keep inventory or drop items.
 * Handles all GUI interactions and timeout logic.
 */
public class DeathConfirmGUI implements Listener {
    private final DynamicKeepInvPlugin plugin;
    private static final String GUI_TITLE_TEXT = "Keep Inventory?";
    private static final Component GUI_TITLE_COMPONENT = 
            Component.text(GUI_TITLE_TEXT)
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD);
    
    // Track open GUIs and their timeout tasks
    private final Map<UUID, Object> timeoutTasks = new ConcurrentHashMap<>(); // ScheduledTask or BukkitTask
    private final Map<UUID, Long> guiOpenTime = new ConcurrentHashMap<>();
    
    private final DecimalFormat df = new DecimalFormat("#.##");
    
    // Slot positions
    private static final int GUI_SIZE = 27;
    private static final int SLOT_PAY = 11;
    private static final int SLOT_INFO = 13;
    private static final int SLOT_DROP = 15;
    private static final int SLOT_AUTO_PAY = 22;
    
    public DeathConfirmGUI(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Open the death confirmation GUI for a player
     */
    public void openGUI(Player player, PendingDeath pendingDeath) {
        if (pendingDeath == null || pendingDeath.isProcessed()) {
            plugin.debug("Cannot open GUI: pending death is null or already processed");
            return;
        }
        
        // Cancel any existing timeout
        cancelTimeout(player.getUniqueId());
        
        Inventory gui = Bukkit.createInventory(new DeathGuiHolder(), GUI_SIZE, GUI_TITLE_COMPONENT);
        
        EconomyManager eco = plugin.getEconomyManager();
        double cost = pendingDeath.getCost();
        String costFormatted = eco != null ? eco.format(cost) : "$" + df.format(cost);
        boolean canAfford = eco != null && eco.isEnabled() && eco.hasEnough(player, cost);
        
        // Fill border
        fillBorder(gui);
        
        // Pay button (green)
        Material payMaterial = canAfford ? Material.EMERALD_BLOCK : Material.BARRIER;
        String payTitle = canAfford ? "§a§lPAY - Keep Items" : "§c§lInsufficient Funds";
        List<String> payLore = new ArrayList<>();
        payLore.add("§7Cost: §6" + costFormatted);
        if (canAfford) {
            payLore.add("");
            payLore.add("§aClick to pay and keep your items!");
            payLore.add("§7Your balance: §a" + (eco != null ? eco.format(eco.getBalance(player)) : "?"));
        } else {
            payLore.add("");
            payLore.add("§cYou cannot afford this!");
            payLore.add("§7Your balance: §c" + (eco != null ? eco.format(eco.getBalance(player)) : "?"));
            payLore.add("§7Needed: §6" + costFormatted);
        }
        gui.setItem(SLOT_PAY, createItem(payMaterial, payTitle, payLore));
        
        // Info item (center)
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7You died and have §f" + pendingDeath.countItems() + " items");
        infoLore.add("§7in your inventory.");
        infoLore.add("");
        infoLore.add("§eChoose what to do:");
        infoLore.add("§a• Pay " + costFormatted + " to keep items");
        infoLore.add("§c• Drop items on the ground");
        infoLore.add("");
        long timeoutSec = plugin.getPendingDeathManager().getTimeoutMs() / 1000;
        infoLore.add("§6⚠ Timeout: §f" + timeoutSec + " seconds");
        gui.setItem(SLOT_INFO, createItem(Material.PAPER, "§e§lDeath Notice", infoLore));
        
        // Drop button (red)
        List<String> dropLore = new ArrayList<>();
        dropLore.add("§7Your items will be dropped");
        dropLore.add("§7at your current location.");
        dropLore.add("");
        dropLore.add("§cClick to drop your items!");
        gui.setItem(SLOT_DROP, createItem(Material.REDSTONE_BLOCK, "§c§lDROP - Lose Items", dropLore));
        
        // Auto-pay toggle button
        PendingDeathManager manager = plugin.getPendingDeathManager();
        boolean autoPayEnabled = manager.isAutoPayEnabled(player.getUniqueId());
        Material autoMaterial = autoPayEnabled ? Material.LIME_DYE : Material.GRAY_DYE;
        String autoTitle = autoPayEnabled ? "§a§lAuto-Pay: ON" : "§7§lAuto-Pay: OFF";
        List<String> autoLore = new ArrayList<>();
        autoLore.add("§7When enabled, you will");
        autoLore.add("§7automatically pay to keep");
        autoLore.add("§7items on future deaths.");
        autoLore.add("");
        if (autoPayEnabled) {
            autoLore.add("§aCurrently: §lENABLED");
            autoLore.add("§7Click to disable");
        } else {
            autoLore.add("§7Currently: §lDISABLED");
            autoLore.add("§aClick to enable");
        }
        gui.setItem(SLOT_AUTO_PAY, createItem(autoMaterial, autoTitle, autoLore));
        
        // Mark GUI as open
        pendingDeath.setGuiOpen(true);
        guiOpenTime.put(player.getUniqueId(), System.currentTimeMillis());
        
        // Start timeout
        startTimeout(player.getUniqueId());
        
        player.openInventory(gui);
        plugin.debug("Opened death confirm GUI for " + player.getName());
    }
    
    private void fillBorder(Inventory gui) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack inner = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < GUI_SIZE; i++) {
            // Fill all with gray first
            gui.setItem(i, inner);
        }

        // Top and bottom rows as border
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border); // Top row
            gui.setItem(GUI_SIZE - 9 + i, border); // Bottom row
        }

        // Sides
        gui.setItem(9, border);
        gui.setItem(17, border);
    }

    /**
     * Create an item with lore
     */
    private ItemStack createItem(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create a simple item without lore
     */
    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Start timeout task for a player
     */
    private void startTimeout(UUID playerId) {
        PendingDeathManager manager = plugin.getPendingDeathManager();
        long timeoutTicks = manager.getTimeoutMs() / 50; // Convert ms to ticks
        
        if (plugin.isFolia()) {
            // Use global scheduler for timing, but dispatch to player scheduler for action
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.getScheduler().run(plugin, innerTask -> handleTimeout(playerId), null);
                } else {
                    // Player offline, handle directly (will check offline player in handleTimeout)
                    handleTimeout(playerId);
                }
            }, timeoutTicks);
            timeoutTasks.put(playerId, task);
        } else {
            int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                handleTimeout(playerId);
            }, timeoutTicks).getTaskId();
            timeoutTasks.put(playerId, taskId);
        }
    }
    
    /**
     * Cancel timeout task for a player
     */
    private void cancelTimeout(UUID playerId) {
        Object task = timeoutTasks.remove(playerId);
        if (task != null) {
            if (task instanceof ScheduledTask) {
                ((ScheduledTask) task).cancel();
            } else if (task instanceof Integer) {
                Bukkit.getScheduler().cancelTask((Integer) task);
            }
        }
        guiOpenTime.remove(playerId);
    }
    
    /**
     * Handle timeout - close GUI and drop items
     */
    private void handleTimeout(UUID playerId) {
        timeoutTasks.remove(playerId);
        guiOpenTime.remove(playerId);
        
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            // Check if still in our GUI
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof DeathGuiHolder) {
                player.closeInventory();
            }
        }
        
        // Process timeout
        PendingDeathManager manager = plugin.getPendingDeathManager();
        manager.handleTimeout(playerId);
    }
    
    // ===== Event Handlers =====
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        if (!(event.getInventory().getHolder() instanceof DeathGuiHolder)) return;
        
        // Always cancel all clicks in our GUI
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        
        // Only handle clicks in top inventory
        if (slot < 0 || slot >= GUI_SIZE) return;
        
        PendingDeathManager manager = plugin.getPendingDeathManager();
        PendingDeath pending = manager.getPendingDeath(player.getUniqueId());
        
        if (pending == null || pending.isProcessed()) {
            player.closeInventory();
            return;
        }
        
        switch (slot) {
            case SLOT_PAY -> {
                // Pay button
                cancelTimeout(player.getUniqueId());
                player.closeInventory();
                
                boolean success = manager.processPayment(player);
                if (!success) {
                    // Payment failed, but pending death still exists - reopen GUI
                    PendingDeath stillPending = manager.getPendingDeath(player.getUniqueId());
                    if (stillPending != null && !stillPending.isProcessed()) {
                        // Schedule reopen
                        if (plugin.isFolia()) {
                            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> {
                                if (player.isOnline()) {
                                    openGUI(player, stillPending);
                                }
                            }, 20L);
                        } else {
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (player.isOnline()) {
                                    openGUI(player, stillPending);
                                }
                            }, 20L);
                        }
                    }
                }
            }
            case SLOT_DROP -> {
                // Drop button
                cancelTimeout(player.getUniqueId());
                player.closeInventory();
                manager.processDrop(player);
            }
            case SLOT_AUTO_PAY -> {
                // Auto-pay toggle button
                boolean newState = manager.toggleAutoPay(player.getUniqueId());
                
                // Send feedback message
                String msgKey = newState ? "economy.gui.auto-pay-enabled" : "economy.gui.auto-pay-disabled";
                String msg = plugin.getMessage(msgKey);
                player.sendMessage(plugin.parseMessage(msg));
                
                // Refresh GUI to show new state
                if (plugin.isFolia()) {
                    Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> {
                        if (player.isOnline()) {
                            PendingDeath refreshPending = manager.getPendingDeath(player.getUniqueId());
                            if (refreshPending != null && !refreshPending.isProcessed()) {
                                openGUI(player, refreshPending);
                            }
                        }
                    }, 1L);
                } else {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            PendingDeath refreshPending = manager.getPendingDeath(player.getUniqueId());
                            if (refreshPending != null && !refreshPending.isProcessed()) {
                                openGUI(player, refreshPending);
                            }
                        }
                    }, 1L);
                }
            }
            // Info slot and others - do nothing
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof DeathGuiHolder) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        if (!(event.getInventory().getHolder() instanceof DeathGuiHolder)) return;
        
        UUID playerId = player.getUniqueId();
        
        // Check if pending death still exists and not processed
        PendingDeathManager manager = plugin.getPendingDeathManager();
        PendingDeath pending = manager.getPendingDeath(playerId);
        
        if (pending != null && !pending.isProcessed() && pending.isGuiOpen()) {
            pending.setGuiOpen(false);
            
            // If closed by player (not by us), they get a warning - DON'T auto-reopen
            // They can use /dki confirm to reopen
            Long openTime = guiOpenTime.get(playerId);
            if (openTime != null) {
                long elapsed = System.currentTimeMillis() - openTime;
                long timeoutMs = plugin.getPendingDeathManager().getTimeoutMs();
                
                // If closed early (not by timeout), send warning
                if (elapsed < timeoutMs - 1000) {
                    String msg = plugin.getMessage("economy.gui.closed-warning");
                    player.sendMessage(plugin.parseMessage(msg));
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        cancelTimeout(playerId);
        
        // Mark GUI as closed
        PendingDeathManager manager = plugin.getPendingDeathManager();
        if (manager != null) {
            PendingDeath pending = manager.getPendingDeath(playerId);
            if (pending != null) {
                pending.setGuiOpen(false);
            }
            manager.handlePlayerQuit(playerId);
        }
    }
    
    /**
     * Check if player currently has our GUI open
     */
    public boolean hasGUIOpen(Player player) {
        return player.getOpenInventory().getTopInventory().getHolder() instanceof DeathGuiHolder;
    }
    
    /**
     * Get remaining time in seconds
     */
    public int getRemainingTime(UUID playerId) {
        Long openTime = guiOpenTime.get(playerId);
        if (openTime == null) return 0;
        
        long elapsed = System.currentTimeMillis() - openTime;
        long remaining = plugin.getPendingDeathManager().getTimeoutMs() - elapsed;
        return (int) Math.max(0, remaining / 1000);
    }
}
