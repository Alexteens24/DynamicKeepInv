package xyz.superez.dynamickeepinv.hooks;

import org.bukkit.inventory.ItemStack;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

import java.lang.reflect.Method;

public class MMOItemsHook {
    private final DynamicKeepInvPlugin plugin;
    private Class<?> nbtItemClass;
    private Method getMethod;
    private Method hasTagMethod;

    public MMOItemsHook(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
        setupReflection();
    }

    private void setupReflection() {
        try {
            // Try newer MythicLib location first
            nbtItemClass = Class.forName("io.lumine.mythic.lib.api.item.NBTItem");
        } catch (ClassNotFoundException e) {
            try {
                // Try older MMOItems/MMOLib location
                nbtItemClass = Class.forName("net.Indyuce.mmoitems.api.item.NBTItem");
            } catch (ClassNotFoundException e2) {
                // Try even older location if needed or just fail gracefully
                plugin.getLogger().warning("Could not find MMOItems NBTItem class. Soulbound support will be disabled.");
                return;
            }
        }

        try {
            // public static NBTItem get(ItemStack item)
            getMethod = nbtItemClass.getMethod("get", ItemStack.class);
            // public boolean hasTag(String tag)
            hasTagMethod = nbtItemClass.getMethod("hasTag", String.class);
        } catch (NoSuchMethodException e) {
            plugin.getLogger().warning("Could not find NBTItem methods: " + e.getMessage());
            nbtItemClass = null; // Disable usage
        }
    }

    public boolean isSoulbound(ItemStack item) {
        if (nbtItemClass == null || item == null) return false;

        try {
            Object nbtItem = getMethod.invoke(null, item);
            return (boolean) hasTagMethod.invoke(nbtItem, "MMOITEMS_SOULBOUND");
        } catch (Exception e) {
            plugin.debug("Error checking soulbound status: " + e.getMessage());
            return false;
        }
    }
}
