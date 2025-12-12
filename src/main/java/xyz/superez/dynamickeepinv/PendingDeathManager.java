package xyz.superez.dynamickeepinv;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.sql.*;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages pending deaths that require player confirmation via GUI.
 * Handles memory storage and database persistence for crash recovery.
 */
public class PendingDeathManager {
    private final DynamicKeepInvPlugin plugin;
    private final Map<UUID, PendingDeath> pendingDeaths = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> autoPayCache = new ConcurrentHashMap<>(); // Cache for auto-pay settings
    private Connection connection;
    private final ExecutorService asyncExecutor;
    private volatile boolean isShuttingDown = false;
    private final Object dbLock = new Object();
    private Object cleanupTaskHandle; // ScheduledTask (Folia) or BukkitTask
    
    // Config values
    private long timeoutMs = 30000; // 30 seconds default
    private long expireMs = 300000; // 5 minutes max storage
    
    public PendingDeathManager(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
        this.asyncExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DynamicKeepInv-PendingDeath");
            t.setDaemon(true);
            return t;
        });
        loadConfig();
        initDatabase();
        loadPendingDeathsFromDB();
        startCleanupTask();
    }
    
    private void loadConfig() {
        timeoutMs = plugin.getConfig().getLong("advanced.economy.gui.timeout", 30) * 1000L;
        expireMs = plugin.getConfig().getLong("advanced.economy.gui.expire-time", 300) * 1000L;
    }
    
    private void initDatabase() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "pending_deaths.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            synchronized (dbLock) {
                connection = DriverManager.getConnection(url);
                
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(
                        "CREATE TABLE IF NOT EXISTS pending_deaths (" +
                        "player_uuid TEXT PRIMARY KEY," +
                        "player_name TEXT," +
                        "inventory_data TEXT," +
                        "armor_data TEXT," +
                        "offhand_data TEXT," +
                        "level INTEGER," +
                        "exp REAL," +
                        "cost REAL," +
                        "world_name TEXT," +
                        "x REAL," +
                        "y REAL," +
                        "z REAL," +
                        "death_reason TEXT," +
                        "timestamp INTEGER)"
                    );

                    // Check if we need to add coordinate columns to existing table
                    try {
                        ResultSet rs = stmt.executeQuery("SELECT * FROM pending_deaths LIMIT 1");
                        ResultSetMetaData rsmd = rs.getMetaData();
                        boolean hasX = false;
                        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                            if ("x".equalsIgnoreCase(rsmd.getColumnName(i))) {
                                hasX = true;
                                break;
                            }
                        }
                        rs.close();

                        if (!hasX) {
                            plugin.getLogger().info("Updating pending_deaths table with coordinate columns...");
                            stmt.execute("ALTER TABLE pending_deaths ADD COLUMN x REAL DEFAULT 0");
                            stmt.execute("ALTER TABLE pending_deaths ADD COLUMN y REAL DEFAULT 0");
                            stmt.execute("ALTER TABLE pending_deaths ADD COLUMN z REAL DEFAULT 0");
                        }
                    } catch (SQLException ignored) {
                        // Table might be empty or just created
                    }
                    
                    // Player settings table for auto-pay
                    stmt.execute(
                        "CREATE TABLE IF NOT EXISTS player_settings (" +
                        "player_uuid TEXT PRIMARY KEY," +
                        "auto_pay INTEGER DEFAULT 0)"
                    );
                }
            }
            plugin.getLogger().info("Pending deaths database initialized!");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize pending deaths database!", e);
        }
    }
    
    private boolean isConnectionValid() {
        synchronized (dbLock) {
            try {
                return connection != null && !connection.isClosed();
            } catch (SQLException e) {
                return false;
            }
        }
    }
    
    /**
     * Store a pending death for a player
     */
    public void addPendingDeath(PendingDeath pendingDeath) {
        UUID playerId = pendingDeath.getPlayerId();
        
        // If player already has pending death, drop old items first
        PendingDeath old = pendingDeaths.remove(playerId);
        if (old != null && !old.isProcessed()) {
            plugin.debug("Player " + pendingDeath.getPlayerName() + " had existing pending death, dropping old items");
            dropItemsForPendingDeath(old);
        }
        
        pendingDeaths.put(playerId, pendingDeath);
        savePendingDeathToDB(pendingDeath);
        plugin.debug("Added pending death for " + pendingDeath.getPlayerName() + " with cost " + pendingDeath.getCost());
    }
    
    /**
     * Get pending death for a player
     */
    public PendingDeath getPendingDeath(UUID playerId) {
        return pendingDeaths.get(playerId);
    }
    
    /**
     * Check if player has pending death
     */
    public boolean hasPendingDeath(UUID playerId) {
        PendingDeath pending = pendingDeaths.get(playerId);
        return pending != null && !pending.isProcessed();
    }
    
    /**
     * Process player's choice to pay
     * @return true if payment successful and inventory restored
     */
    public boolean processPayment(Player player) {
        PendingDeath pending = pendingDeaths.get(player.getUniqueId());
        if (pending == null || pending.isProcessed()) {
            return false;
        }
        
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null || !eco.isEnabled()) {
            plugin.debug("Economy not available for payment processing");
            return false;
        }
        
        double cost = pending.getCost();
        
        // Re-check balance
        if (!eco.hasEnough(player, cost)) {
            String msg = plugin.getMessage("economy.gui.insufficient-now")
                    .replace("{amount}", eco.format(cost));
            player.sendMessage(plugin.parseMessage(msg));
            return false;
        }
        
        // Process payment
        if (!eco.withdraw(player, cost)) {
            String msg = plugin.getMessage("economy.gui.payment-failed");
            player.sendMessage(plugin.parseMessage(msg));
            return false;
        }
        
        // Restore inventory
        restoreInventory(player, pending);
        
        // Record stats
        StatsManager stats = plugin.getStatsManager();
        if (stats != null) {
            stats.recordEconomyPayment(player, cost);
            stats.recordDeathSaved(player, pending.getDeathReason());
        }
        
        // Mark as processed and clean up
        pending.setProcessed(true);
        pendingDeaths.remove(player.getUniqueId());
        deletePendingDeathFromDB(player.getUniqueId());
        
        String msg = plugin.getMessage("economy.gui.paid")
                .replace("{amount}", eco.format(cost));
        player.sendMessage(plugin.parseMessage(msg));
        
        plugin.debug("Player " + player.getName() + " paid " + cost + " to keep inventory");
        return true;
    }
    
    /**
     * Process player's choice to drop items
     */
    public void processDrop(Player player) {
        PendingDeath pending = pendingDeaths.get(player.getUniqueId());
        if (pending == null || pending.isProcessed()) {
            return;
        }
        
        dropItemsForPendingDeath(pending);
        
        // Record stats
        StatsManager stats = plugin.getStatsManager();
        if (stats != null) {
            stats.recordDeathLost(player, pending.getDeathReason());
        }
        
        // Mark as processed and clean up
        pending.setProcessed(true);
        pendingDeaths.remove(player.getUniqueId());
        deletePendingDeathFromDB(player.getUniqueId());
        
        String msg = plugin.getMessage("economy.gui.dropped");
        player.sendMessage(plugin.parseMessage(msg));
        
        plugin.debug("Player " + player.getName() + " chose to drop items");
    }
    
    /**
     * Handle timeout - auto drop items
     */
    public void handleTimeout(UUID playerId) {
        PendingDeath pending = pendingDeaths.get(playerId);
        if (pending == null || pending.isProcessed()) {
            return;
        }
        
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            String msg = plugin.getMessage("economy.gui.timeout");
            player.sendMessage(plugin.parseMessage(msg));
        }
        
        dropItemsForPendingDeath(pending);
        
        // Record stats
        StatsManager stats = plugin.getStatsManager();
        if (stats != null && player != null) {
            stats.recordDeathLost(player, pending.getDeathReason());
        }
        
        pending.setProcessed(true);
        pendingDeaths.remove(playerId);
        deletePendingDeathFromDB(playerId);
        
        plugin.debug("Pending death for " + pending.getPlayerName() + " timed out, items dropped");
    }
    
    /**
     * Restore inventory to player
     */
    private void restoreInventory(Player player, PendingDeath pending) {
        player.getInventory().setContents(pending.getSavedInventory());
        player.getInventory().setArmorContents(pending.getSavedArmor());
        if (pending.getOffhandItem() != null) {
            player.getInventory().setItemInOffHand(pending.getOffhandItem());
        }
        player.setLevel(pending.getSavedLevel());
        player.setExp(pending.getSavedExp());
        player.updateInventory();
    }
    
    /**
     * Drop items at death location
     */
    private void dropItemsForPendingDeath(PendingDeath pending) {
        World world = Bukkit.getWorld(pending.getWorldName());
        if (world == null) {
            plugin.getLogger().warning("Cannot drop items for " + pending.getPlayerName() + ": world " + pending.getWorldName() + " not found");
            return;
        }

        Location dropLocation;
        // Use stored coordinates if available (not 0,0,0)
        if (pending.getX() != 0 || pending.getY() != 0 || pending.getZ() != 0) {
            dropLocation = new Location(world, pending.getX(), pending.getY(), pending.getZ());
        } else {
            // Fallback for old data: try player location or spawn
            Player player = Bukkit.getPlayer(pending.getPlayerId());
            if (player != null && player.isOnline()) {
                dropLocation = player.getLocation();
            } else {
                dropLocation = world.getSpawnLocation();
            }
        }
        
        Player player = Bukkit.getPlayer(pending.getPlayerId());

        if (plugin.isFolia()) {
            // In Folia, we must drop items on the region thread
            Runnable dropTask = () -> performDrop(dropLocation, pending, player);
            if (player != null && player.isOnline()) {
                player.getScheduler().run(plugin, task -> dropTask.run(), null);
            } else {
                Bukkit.getRegionScheduler().execute(plugin, dropLocation, dropTask);
            }
        } else {
            performDrop(dropLocation, pending, player);
        }
    }

    private void performDrop(Location dropLocation, PendingDeath pending, Player player) {
        // Check for GravesX integration
        if (player != null && plugin.isGravesXEnabled()) {
            List<ItemStack> drops = new ArrayList<>();

            for (ItemStack item : pending.getSavedInventory()) {
                if (item != null && !item.getType().isAir() && !hasVanishingCurse(item)) {
                    drops.add(item);
                }
            }
            for (ItemStack item : pending.getSavedArmor()) {
                if (item != null && !item.getType().isAir() && !hasVanishingCurse(item)) {
                    drops.add(item);
                }
            }
            if (pending.getOffhandItem() != null && !pending.getOffhandItem().getType().isAir() && !hasVanishingCurse(pending.getOffhandItem())) {
                drops.add(pending.getOffhandItem());
            }

            // Calculate XP
            int xp = calculateTotalExperience(pending.getSavedLevel(), pending.getSavedExp());

            if (!drops.isEmpty() || xp > 0) {
                if (plugin.getGravesXHook().createGrave(player, dropLocation, drops, xp)) {
                    plugin.debug("Grave created for pending death of " + pending.getPlayerName());
                    return; // Grave created, skip natural drops
                }
            }
        }

        // Drop all items
        for (ItemStack item : pending.getSavedInventory()) {
            if (item != null && !item.getType().isAir()) {
                if (hasVanishingCurse(item)) continue; // Skip vanishing curse
                dropLocation.getWorld().dropItemNaturally(dropLocation, item);
            }
        }
        for (ItemStack item : pending.getSavedArmor()) {
            if (item != null && !item.getType().isAir()) {
                if (hasVanishingCurse(item)) continue; // Skip vanishing curse
                dropLocation.getWorld().dropItemNaturally(dropLocation, item);
            }
        }
        if (pending.getOffhandItem() != null && !pending.getOffhandItem().getType().isAir()) {
            if (!hasVanishingCurse(pending.getOffhandItem())) {
                dropLocation.getWorld().dropItemNaturally(dropLocation, pending.getOffhandItem());
            }
        }
        
        // Drop XP orbs
        if (pending.getSavedLevel() > 0) {
            int expToDrop = Math.min(pending.getSavedLevel() * 7, 100);
            dropLocation.getWorld().spawn(dropLocation, org.bukkit.entity.ExperienceOrb.class, 
                orb -> orb.setExperience(expToDrop));
        }
    }

    private int calculateTotalExperience(int level, float expPercentage) {
        int exp = 0;

        // Calculate XP for current level progress
        int expAtLevel;
        if (level >= 31) {
            expAtLevel = 9 * level - 158;
        } else if (level >= 16) {
            expAtLevel = 5 * level - 38;
        } else {
            expAtLevel = 2 * level + 7;
        }
        exp += Math.round(expAtLevel * expPercentage);

        // Calculate XP for past levels
        for (int i = 0; i < level; i++) {
            if (i >= 31) {
                exp += 9 * i - 158;
            } else if (i >= 16) {
                exp += 5 * i - 38;
            } else {
                exp += 2 * i + 7;
            }
        }

        return exp;
    }
    
    private boolean hasVanishingCurse(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().hasEnchant(org.bukkit.enchantments.Enchantment.VANISHING_CURSE);
    }

    /**
     * Handle player disconnect - keep pending death in DB for reconnect
     */
    public void handlePlayerQuit(UUID playerId) {
        PendingDeath pending = pendingDeaths.get(playerId);
        if (pending != null && !pending.isProcessed()) {
            pending.setGuiOpen(false);
            // Keep in memory and DB for reconnect
            plugin.debug("Player " + pending.getPlayerName() + " disconnected with pending death");
        }
    }
    
    /**
     * Handle player reconnect - check for pending death
     */
    public PendingDeath handlePlayerJoin(UUID playerId) {
        PendingDeath pending = pendingDeaths.get(playerId);
        if (pending != null && !pending.isProcessed()) {
            // Check if expired
            if (pending.isExpired(expireMs)) {
                plugin.debug("Pending death for player " + pending.getPlayerName() + " expired during disconnect");
                handleTimeout(playerId);
                return null;
            }
            return pending;
        }
        return null;
    }
    
    // ===== Auto-Pay Settings =====
    
    /**
     * Check if player has auto-pay enabled
     */
    public boolean isAutoPayEnabled(UUID playerId) {
        // Check cache first
        Boolean cached = autoPayCache.get(playerId);
        if (cached != null) {
            return cached;
        }
        
        // Load from database
        boolean autoPay = loadAutoPayFromDB(playerId);
        autoPayCache.put(playerId, autoPay);
        return autoPay;
    }
    
    /**
     * Set auto-pay preference for a player
     */
    public void setAutoPay(UUID playerId, boolean enabled) {
        autoPayCache.put(playerId, enabled);
        saveAutoPayToDB(playerId, enabled);
        plugin.debug("Set auto-pay for " + playerId + " to " + enabled);
    }
    
    /**
     * Toggle auto-pay for a player
     * @return new state
     */
    public boolean toggleAutoPay(UUID playerId) {
        boolean current = isAutoPayEnabled(playerId);
        boolean newState = !current;
        setAutoPay(playerId, newState);
        return newState;
    }
    
    private boolean loadAutoPayFromDB(UUID playerId) {
        if (!isConnectionValid()) return false;
        
        String sql = "SELECT auto_pay FROM player_settings WHERE player_uuid = ?";
        synchronized (dbLock) {
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerId.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("auto_pay") == 1;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load auto-pay setting!", e);
            }
        }
        return false;
    }
    
    private void saveAutoPayToDB(UUID playerId, boolean enabled) {
        if (isShuttingDown || !isConnectionValid()) return;
        
        asyncExecutor.execute(() -> {
            if (isShuttingDown) return;
            
            String sql = "INSERT OR REPLACE INTO player_settings (player_uuid, auto_pay) VALUES (?, ?)";
            synchronized (dbLock) {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, playerId.toString());
                    pstmt.setInt(2, enabled ? 1 : 0);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save auto-pay setting!", e);
                }
            }
        });
    }
    
    /**
     * Process auto-pay for a player (called when they die with auto-pay enabled)
     * @return true if auto-pay succeeded, false if should show GUI
     */
    public boolean processAutoPay(Player player, PendingDeath pending) {
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null || !eco.isEnabled()) {
            plugin.debug("Auto-pay failed: Economy not available");
            return false;
        }
        
        double cost = pending.getCost();
        
        // Check balance
        if (!eco.hasEnough(player, cost)) {
            plugin.debug("Auto-pay failed: Player " + player.getName() + " doesn't have enough money");
            // Not enough money - will show GUI instead
            return false;
        }
        
        // Process payment
        if (!eco.withdraw(player, cost)) {
            plugin.debug("Auto-pay failed: Withdrawal failed for " + player.getName());
            return false;
        }
        
        // Restore inventory
        restoreInventory(player, pending);
        
        // Record stats
        StatsManager stats = plugin.getStatsManager();
        if (stats != null) {
            stats.recordEconomyPayment(player, cost);
            stats.recordDeathSaved(player, pending.getDeathReason());
        }
        
        // Mark as processed and clean up
        pending.setProcessed(true);
        pendingDeaths.remove(player.getUniqueId());
        deletePendingDeathFromDB(player.getUniqueId());
        
        // Send message
        String msg = plugin.getMessage("economy.gui.auto-paid")
                .replace("{amount}", eco.format(cost));
        player.sendMessage(plugin.parseMessage(msg));
        
        plugin.debug("Auto-pay successful for " + player.getName() + ", charged " + cost);
        return true;
    }
    
    // ===== Database Operations =====
    
    private void savePendingDeathToDB(PendingDeath pending) {
        if (isShuttingDown || !isConnectionValid()) return;
        
        asyncExecutor.execute(() -> {
            String sql = "INSERT OR REPLACE INTO pending_deaths " +
                        "(player_uuid, player_name, inventory_data, armor_data, offhand_data, " +
                        "level, exp, cost, world_name, x, y, z, death_reason, timestamp) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            synchronized (dbLock) {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, pending.getPlayerId().toString());
                    pstmt.setString(2, pending.getPlayerName());
                    pstmt.setString(3, serializeItems(pending.getSavedInventory()));
                    pstmt.setString(4, serializeItems(pending.getSavedArmor()));
                    pstmt.setString(5, pending.getOffhandItem() != null ? 
                            serializeItems(new ItemStack[]{pending.getOffhandItem()}) : "");
                    pstmt.setInt(6, pending.getSavedLevel());
                    pstmt.setFloat(7, pending.getSavedExp());
                    pstmt.setDouble(8, pending.getCost());
                    pstmt.setString(9, pending.getWorldName());
                    pstmt.setDouble(10, pending.getX());
                    pstmt.setDouble(11, pending.getY());
                    pstmt.setDouble(12, pending.getZ());
                    pstmt.setString(13, pending.getDeathReason());
                    pstmt.setLong(14, pending.getTimestamp());
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save pending death!", e);
                }
            }
        });
    }
    
    private void deletePendingDeathFromDB(UUID playerId) {
        if (isShuttingDown || !isConnectionValid()) return;
        
        asyncExecutor.execute(() -> {
            String sql = "DELETE FROM pending_deaths WHERE player_uuid = ?";
            synchronized (dbLock) {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, playerId.toString());
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to delete pending death!", e);
                }
            }
        });
    }
    
    private void loadPendingDeathsFromDB() {
        if (!isConnectionValid()) return;
        
        String sql = "SELECT * FROM pending_deaths";
        synchronized (dbLock) {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                int loaded = 0;
                int expired = 0;
                
                while (rs.next()) {
                    long timestamp = rs.getLong("timestamp");
                    
                    // Check if expired
                    if (System.currentTimeMillis() - timestamp > expireMs) {
                        expired++;
                        continue;
                    }
                    
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    String playerName = rs.getString("player_name");
                    ItemStack[] inventory = deserializeItems(rs.getString("inventory_data"));
                    ItemStack[] armor = deserializeItems(rs.getString("armor_data"));
                    String offhandData = rs.getString("offhand_data");
                    ItemStack offhand = null;
                    if (offhandData != null && !offhandData.isEmpty()) {
                        ItemStack[] offhandArr = deserializeItems(offhandData);
                        if (offhandArr.length > 0) {
                            offhand = offhandArr[0];
                        }
                    }
                    int level = rs.getInt("level");
                    float exp = rs.getFloat("exp");
                    double cost = rs.getDouble("cost");
                    String worldName = rs.getString("world_name");

                    // Load coordinates (handling old DBs where columns might be missing/defaulted)
                    double x = 0, y = 0, z = 0;
                    try {
                        x = rs.getDouble("x");
                        y = rs.getDouble("y");
                        z = rs.getDouble("z");
                    } catch (SQLException ignored) {
                        // Columns might not exist in result set if we just added them but didn't refresh connection
                    }

                    String deathReason = rs.getString("death_reason");
                    
                    PendingDeath pending = new PendingDeath(
                        playerId, playerName, inventory, armor, offhand,
                        level, exp, cost, worldName, x, y, z, deathReason, timestamp
                    );
                    
                    pendingDeaths.put(playerId, pending);
                    loaded++;
                }
                
                if (loaded > 0) {
                    plugin.getLogger().info("Loaded " + loaded + " pending deaths from database");
                }
                if (expired > 0) {
                    plugin.getLogger().info("Cleaned up " + expired + " expired pending deaths");
                    cleanExpiredFromDB();
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load pending deaths!", e);
            }
        }
    }
    
    private void cleanExpiredFromDB() {
        if (!isConnectionValid()) return;
        
        asyncExecutor.execute(() -> {
            if (isShuttingDown) return;
            
            long cutoff = System.currentTimeMillis() - expireMs;
            String sql = "DELETE FROM pending_deaths WHERE timestamp < ?";
            
            synchronized (dbLock) {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setLong(1, cutoff);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to clean expired pending deaths!", e);
                }
            }
        });
    }
    
    // ===== Item Serialization =====
    
    private String serializeItems(ItemStack[] items) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {
                boos.writeInt(items.length);
                for (ItemStack item : items) {
                    boos.writeObject(item);
                }
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to serialize items!", e);
            return "";
        }
    }
    
    private ItemStack[] deserializeItems(String data) {
        if (data == null || data.isEmpty()) {
            return new ItemStack[0];
        }
        
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            try (BukkitObjectInputStream bois = new BukkitObjectInputStream(bais)) {
                int length = bois.readInt();
                ItemStack[] items = new ItemStack[length];
                for (int i = 0; i < length; i++) {
                    items[i] = (ItemStack) bois.readObject();
                }
                return items;
            }
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to deserialize items!", e);
            return new ItemStack[0];
        }
    }
    
    // ===== Cleanup Task =====
    
    private void startCleanupTask() {
        // Run cleanup every 30 seconds
        if (plugin.isFolia()) {
            cleanupTaskHandle = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
                if (isShuttingDown) return;
                cleanupExpiredPendingDeaths();
            }, 600L, 600L); // 30 seconds
        } else {
            org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (isShuttingDown) return;
                cleanupExpiredPendingDeaths();
            }, 600L, 600L);
            cleanupTaskHandle = task;
        }
    }
    
    private void cleanupExpiredPendingDeaths() {
        // Collect expired UUIDs first to avoid ConcurrentModificationException
        java.util.List<UUID> expiredIds = new java.util.ArrayList<>();
        for (Map.Entry<UUID, PendingDeath> entry : pendingDeaths.entrySet()) {
            PendingDeath pending = entry.getValue();
            if (!pending.isProcessed() && pending.isExpired(expireMs)) {
                expiredIds.add(entry.getKey());
            }
        }
        // Now handle timeouts
        for (UUID playerId : expiredIds) {
            handleTimeout(playerId);
        }
    }
    
    // ===== Getters =====
    
    public long getTimeoutMs() {
        return timeoutMs;
    }
    
    public long getExpireMs() {
        return expireMs;
    }
    
    public int getPendingCount() {
        return pendingDeaths.size();
    }
    
    // ===== Shutdown =====
    
    public void close() {
        isShuttingDown = true;

        // Cancel cleanup task if scheduled
        Object handle = cleanupTaskHandle;
        if (handle != null) {
            if (handle instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask) {
                foliaTask.cancel();
            } else if (handle instanceof org.bukkit.scheduler.BukkitTask bukkitTask) {
                bukkitTask.cancel();
            }
            cleanupTaskHandle = null;
        }
        
        // Close open GUIs, but preserve pending deaths in DB
        for (PendingDeath pending : pendingDeaths.values()) {
            if (!pending.isProcessed() && pending.isGuiOpen()) {
                Player player = Bukkit.getPlayer(pending.getPlayerId());
                if (player != null && player.isOnline()) {
                    player.closeInventory();
                }
            }
        }
        pendingDeaths.clear();
        
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        synchronized (dbLock) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not close pending deaths database!", e);
            }
        }
    }
}
