package xyz.superez.dynamickeepinv.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

public class AxGravesHook {
    private final DynamicKeepInvPlugin plugin;
    private boolean enabled;

    // Cache reflection objects
    private Class<?> graveClass;
    private Constructor<?> graveConstructor;
    private Method graveUpdateMethod;
    private Class<?> spawnedGravesClass;
    private Method addGraveMethod;

    public AxGravesHook(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("AxGraves") == null) {
            return false;
        }

        try {
            // Check if class exists to verify dependency and cache reflection
            graveClass = Class.forName("com.artillexstudios.axgraves.grave.Grave");
            spawnedGravesClass = Class.forName("com.artillexstudios.axgraves.grave.SpawnedGraves");

            // Constructor: Grave(Location location, OfflinePlayer player, List<ItemStack> items, int xp, long spawned)
            // Using OfflinePlayer.class as it is the most likely type for player ownership
            graveConstructor = graveClass.getConstructor(Location.class, OfflinePlayer.class, List.class, int.class, long.class);
            graveUpdateMethod = graveClass.getMethod("update");

            addGraveMethod = spawnedGravesClass.getMethod("addGrave", graveClass);

            enabled = true;
            plugin.getLogger().info("AxGraves found and hooked!");
            return true;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("AxGraves plugin found but classes missing: " + e.getMessage());
            enabled = false;
            return false;
        } catch (Throwable e) {
            plugin.getLogger().warning("Failed to hook into AxGraves: " + e.getMessage());
            enabled = false;
            return false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean createGrave(Player player, Location location, List<ItemStack> drops, int xp) {
        if (!enabled) {
            return false;
        }

        try {
            // AxGraves creates a grave and adds it to SpawnedGraves
            Object grave = graveConstructor.newInstance(
                location,
                player,
                drops,
                xp,
                System.currentTimeMillis()
            );

            addGraveMethod.invoke(null, grave);
            graveUpdateMethod.invoke(grave);
            return true;
        } catch (Throwable t) {
             plugin.getLogger().warning("Failed to create grave via AxGraves API: " + t.getMessage());
             t.printStackTrace();
             return false;
        }
    }
}
