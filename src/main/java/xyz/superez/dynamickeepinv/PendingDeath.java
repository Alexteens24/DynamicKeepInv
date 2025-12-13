package xyz.superez.dynamickeepinv;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents a pending death that requires player confirmation via GUI.
 * Stores all necessary data to restore inventory if player chooses to pay.
 */
public class PendingDeath {
    private final UUID playerId;
    private final String playerName;
    private final ItemStack[] savedInventory;
    private final ItemStack[] savedArmor;
    private final ItemStack offhandItem;
    private final int savedLevel;
    private final float savedExp;
    private final double cost;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final long timestamp;
    private final String deathReason;
    private volatile boolean guiOpen;
    private volatile boolean processed;
    
    public PendingDeath(UUID playerId, String playerName, ItemStack[] savedInventory, 
                        ItemStack[] savedArmor, ItemStack offhandItem,
                        int savedLevel, float savedExp, double cost, 
                        String worldName, double x, double y, double z, String deathReason) {
        this(playerId, playerName, savedInventory, savedArmor, offhandItem,
             savedLevel, savedExp, cost, worldName, x, y, z, deathReason, System.currentTimeMillis());
    }
    
    /**
     * Constructor with explicit timestamp (for loading from database)
     */
    public PendingDeath(UUID playerId, String playerName, ItemStack[] savedInventory, 
                        ItemStack[] savedArmor, ItemStack offhandItem,
                        int savedLevel, float savedExp, double cost, 
                        String worldName, double x, double y, double z, String deathReason, long timestamp) {
        this.playerId = playerId;
        this.playerName = playerName;
        // Deep clone all items to prevent modification
        this.savedInventory = cloneInventory(savedInventory);
        this.savedArmor = cloneInventory(savedArmor);
        this.offhandItem = offhandItem != null ? offhandItem.clone() : null;
        this.savedLevel = savedLevel;
        this.savedExp = savedExp;
        this.cost = cost;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
        this.deathReason = deathReason;
        this.guiOpen = false;
        this.processed = false;
    }
    
    /**
     * Deep clone an inventory array
     */
    private ItemStack[] cloneInventory(ItemStack[] original) {
        if (original == null) return new ItemStack[0];
        ItemStack[] cloned = new ItemStack[original.length];
        for (int i = 0; i < original.length; i++) {
            if (original[i] != null) {
                cloned[i] = original[i].clone();
            }
        }
        return cloned;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public ItemStack[] getSavedInventory() {
        return savedInventory;
    }
    
    public ItemStack[] getSavedArmor() {
        return savedArmor;
    }
    
    public ItemStack getOffhandItem() {
        return offhandItem;
    }
    
    public int getSavedLevel() {
        return savedLevel;
    }
    
    public float getSavedExp() {
        return savedExp;
    }
    
    public double getCost() {
        return cost;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public long getTimestamp() {
        return timestamp;
    }
    
    public String getDeathReason() {
        return deathReason;
    }
    
    public boolean isGuiOpen() {
        return guiOpen;
    }
    
    public void setGuiOpen(boolean guiOpen) {
        this.guiOpen = guiOpen;
    }
    
    public boolean isProcessed() {
        return processed;
    }
    
    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    /**
     * Atomically tries to mark the pending death as processed.
     * @return true if successful (was not already processed), false if already processed.
     */
    public synchronized boolean trySetProcessed() {
        if (processed) {
            return false;
        }
        processed = true;
        return true;
    }
    
    /**
     * Check if this pending death has expired
     * @param timeoutMs timeout in milliseconds
     * @return true if expired
     */
    public boolean isExpired(long timeoutMs) {
        return System.currentTimeMillis() - timestamp > timeoutMs;
    }
    
    /**
     * Get age of this pending death in milliseconds
     */
    public long getAgeMs() {
        return System.currentTimeMillis() - timestamp;
    }
    
    /**
     * Check if inventory has any items worth saving
     */
    public boolean hasItems() {
        for (ItemStack item : savedInventory) {
            if (item != null && !item.getType().isAir()) {
                return true;
            }
        }
        for (ItemStack item : savedArmor) {
            if (item != null && !item.getType().isAir()) {
                return true;
            }
        }
        if (offhandItem != null && !offhandItem.getType().isAir()) {
            return true;
        }
        return false;
    }
    
    /**
     * Count total items in inventory
     */
    public int countItems() {
        int count = 0;
        for (ItemStack item : savedInventory) {
            if (item != null && !item.getType().isAir()) {
                count++;
            }
        }
        for (ItemStack item : savedArmor) {
            if (item != null && !item.getType().isAir()) {
                count++;
            }
        }
        if (offhandItem != null && !offhandItem.getType().isAir()) {
            count++;
        }
        return count;
    }
}
