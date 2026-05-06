package xyz.superez.dynamickeepinv.service;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import xyz.superez.dynamickeepinv.DynamicKeepInvPlugin;

import java.util.Iterator;
import java.util.List;

/**
 * Centralises all MMOItems soulbound-item handling.
 *
 * Soulbound items must never be dropped on death; instead they are moved to
 * {@link PlayerDeathEvent#getItemsToKeep()} so Bukkit restores them after respawn.
 *
 * Three distinct call-sites previously duplicated this logic inside DeathListener:
 *   1. GUI mode – filter inventory arrays before snapshotting into PendingDeath
 *   2. Regular drop path – filter the event drop list
 *   3. Force-drop path – rebuild drops from inventory when gamerule was keepInventory=true
 */
public class SoulboundService {

    private final DynamicKeepInvPlugin plugin;

    public SoulboundService(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    /** Returns true if the item carries a soulbound tag (requires MMOItems). */
    public boolean isSoulbound(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (!plugin.isMMOItemsEnabled()) return false;
        return plugin.getMMOItemsHook().isSoulbound(item);
    }

    /**
     * GUI-mode filter: iterates {@code src}, moves soulbound items into
     * {@code keepList} (they will be given back after respawn), and copies
     * non-soulbound items into {@code saveArray} at the same index.
     *
     * @return number of soulbound items found and kept
     */
    public int filterArrayToKeep(ItemStack[] src, List<ItemStack> keepList, ItemStack[] saveArray) {
        int kept = 0;
        for (int i = 0; i < src.length; i++) {
            ItemStack item = src[i];
            if (item == null || item.getType().isAir()) continue;
            if (isSoulbound(item)) {
                keepList.add(item.clone());
                kept++;
            } else {
                saveArray[i] = item.clone();
            }
        }
        return kept;
    }

    /**
     * Regular drop-path filter: removes soulbound items from the event drop
     * list and moves them to {@link PlayerDeathEvent#getItemsToKeep()}.
     *
     * @return number of soulbound items rescued
     */
    public int filterDropsInPlace(PlayerDeathEvent event) {
        List<ItemStack> drops = event.getDrops();
        if (drops == null || drops.isEmpty()) return 0;

        int saved = 0;
        Iterator<ItemStack> it = drops.iterator();
        while (it.hasNext()) {
            ItemStack drop = it.next();
            if (isSoulbound(drop)) {
                it.remove();
                event.getItemsToKeep().add(drop);
                saved++;
            }
        }
        return saved;
    }

    /**
     * Force-drop path: called when the gamerule was keepInventory=true but the
     * plugin decided the player should drop.  The event drops list will be
     * empty, so we rebuild it from the player's live inventory contents,
     * skipping soulbound and Curse-of-Vanishing items.
     */
    public void populateForcedDrops(PlayerDeathEvent event, Player player) {
        int added = 0;
        int savedSoulbound = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            if (item.hasItemMeta() && item.getItemMeta().hasEnchant(Enchantment.VANISHING_CURSE)) continue;
            if (isSoulbound(item)) {
                event.getItemsToKeep().add(item.clone());
                savedSoulbound++;
            } else {
                event.getDrops().add(item.clone());
                added++;
            }
        }
        player.getInventory().clear();
        plugin.debug("Force-drop: added " + added + " items to drops, kept " + savedSoulbound + " soulbound");
    }
}
