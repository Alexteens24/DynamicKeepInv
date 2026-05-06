package xyz.superez.dynamickeepinv.service;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

import java.util.List;

/**
 * Centralises grave creation logic (GravesX → AxGraves fallback).
 *
 * Previously duplicated in:
 *   - DeathListener.onPlayerDeath (items not kept on real death)
 *   - PendingDeathManager.performDrop (GUI-mode timeout / player chose to drop)
 */
public class GraveService {

    private final DynamicKeepInvPlugin plugin;

    public GraveService(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Attempts to create a grave at {@code location} containing {@code drops} and {@code xp}.
     * Tries GravesX first; falls back to AxGraves if GravesX is unavailable or fails.
     *
     * @return true if a grave was successfully created (caller should clear drops / xp from event)
     */
    public boolean tryCreate(Player player, Location location, List<ItemStack> drops, int xp) {
        if (plugin.isGravesXEnabled()) {
            if (plugin.getGravesXHook().createGrave(player, location, drops, xp)) {
                plugin.debug("Grave created via GravesX for " + player.getName()
                        + " (" + drops.size() + " items, " + xp + " xp)");
                return true;
            }
            plugin.debug("GravesX grave creation failed for " + player.getName() + ", trying AxGraves...");
        }

        if (plugin.isAxGravesEnabled()) {
            if (plugin.getAxGravesHook().createGrave(player, location, drops, xp)) {
                plugin.debug("Grave created via AxGraves for " + player.getName()
                        + " (" + drops.size() + " items, " + xp + " xp)");
                return true;
            }
            plugin.debug("AxGraves grave creation failed for " + player.getName());
        }

        return false;
    }

    /** Returns true if at least one graves integration is active. */
    public boolean isAnyEnabled() {
        return plugin.isGravesXEnabled() || plugin.isAxGravesEnabled();
    }
}
