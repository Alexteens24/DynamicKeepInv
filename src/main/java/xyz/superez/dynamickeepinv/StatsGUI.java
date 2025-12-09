package xyz.superez.dynamickeepinv;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class StatsGUI implements Listener {
    private final DynamicKeepInvPlugin plugin;
    private static final String GUI_TITLE_TEXT = "DynamicKeepInv Stats";
    private static final net.kyori.adventure.text.Component GUI_TITLE_COMPONENT =
            net.kyori.adventure.text.Component.text(GUI_TITLE_TEXT)
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD);
    private final DecimalFormat df = new DecimalFormat("#.##");
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    
    public StatsGUI(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void openStats(Player player) {
        openStats(player, player.getUniqueId(), player.getName());
    }
    
    public void openStats(Player viewer, UUID targetUUID, String targetName) {
        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE_COMPONENT);
        StatsManager stats = plugin.getStatsManager();
        
        fillBorder(gui);
        
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUUID));
        skullMeta.displayName(Component.text(targetName).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        List<Component> headLore = new ArrayList<>();
        headLore.add(Component.text("Player Statistics").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        skullMeta.lore(headLore);
        playerHead.setItemMeta(skullMeta);
        gui.setItem(4, playerHead);
        
        int totalDeaths = stats.getTotalDeaths(targetUUID);
        int deathsSaved = stats.getDeathsSaved(targetUUID);
        int deathsLost = stats.getDeathsLost(targetUUID);
        double saveRate = stats.getSaveRate(targetUUID);
        
        ItemStack totalItem = createItem(Material.SKELETON_SKULL, 
            "§c§lTotal Deaths", 
            "§7Deaths tracked: §f" + totalDeaths);
        gui.setItem(20, totalItem);
        
        ItemStack savedItem = createItem(Material.TOTEM_OF_UNDYING, 
            "§a§lDeaths Saved", 
            "§7Inventory kept: §a" + deathsSaved,
            "§7Times your items were saved!");
        gui.setItem(21, savedItem);
        
        ItemStack lostItem = createItem(Material.BARRIER, 
            "§c§lDeaths Lost", 
            "§7Inventory lost: §c" + deathsLost,
            "§7Times your items dropped!");
        gui.setItem(22, lostItem);
        
        Material rateIcon = saveRate >= 50 ? Material.EMERALD : Material.REDSTONE;
        String rateColor = saveRate >= 75 ? "§a" : (saveRate >= 50 ? "§e" : "§c");
        ItemStack rateItem = createItem(rateIcon, 
            "§6§lSave Rate", 
            "§7Rate: " + rateColor + df.format(saveRate) + "%",
            "§7Percentage of deaths saved");
        gui.setItem(23, rateItem);
        
        ItemStack progressItem = createProgressBar(saveRate);
        gui.setItem(24, progressItem);
        
        if (plugin.getConfig().getBoolean("advanced.economy.enabled", false)) {
            double totalPaid = stats.getTotalEconomyPaid(targetUUID);
            int paymentCount = stats.getEconomyPaymentCount(targetUUID);
            ItemStack economyItem = createItem(Material.GOLD_INGOT,
                "§e§lEconomy Stats",
                "§7Total paid: §6$" + df.format(totalPaid),
                "§7Payments: §f" + paymentCount);
            gui.setItem(30, economyItem);
        }
        
        long lastDeathTime = stats.getLastDeathTime(targetUUID);
        String lastReason = stats.getLastDeathReason(targetUUID);
        boolean lastSaved = stats.wasLastDeathSaved(targetUUID);
        
        String timeStr = lastDeathTime > 0 ? sdf.format(new Date(lastDeathTime)) : "Never";
        String savedStr = lastSaved ? "§aYes" : "§cNo";
        ItemStack lastDeathItem = createItem(Material.CLOCK,
            "§b§lLast Death",
            "§7Time: §f" + timeStr,
            "§7Reason: §f" + formatReason(lastReason),
            "§7Saved: " + savedStr);
        gui.setItem(31, lastDeathItem);
        
        int dayDeaths = stats.getReasonSavedCount(targetUUID, "day") + stats.getReasonLostCount(targetUUID, "day");
        int nightDeaths = stats.getReasonSavedCount(targetUUID, "night") + stats.getReasonLostCount(targetUUID, "night");
        int pvpDeaths = stats.getReasonSavedCount(targetUUID, "pvp") + stats.getReasonLostCount(targetUUID, "pvp");
        
        ItemStack breakdownItem = createItem(Material.BOOK,
            "§d§lDeath Breakdown",
            "§7During Day: §e" + dayDeaths,
            "§7During Night: §8" + nightDeaths,
            "§7PvP Deaths: §c" + pvpDeaths,
            "§7Bypass: §b" + (stats.getReasonSavedCount(targetUUID, "bypass")));
        gui.setItem(32, breakdownItem);
        
        int globalSaved = stats.getGlobalDeathsSaved();
        int globalLost = stats.getGlobalDeathsLost();
        double globalRate = stats.getGlobalSaveRate();
        ItemStack globalItem = createItem(Material.NETHER_STAR,
            "§5§lServer Stats",
            "§7Total saved: §a" + globalSaved,
            "§7Total lost: §c" + globalLost,
            "§7Global save rate: §e" + df.format(globalRate) + "%");
        gui.setItem(40, globalItem);
        
        ItemStack closeItem = createItem(Material.RED_STAINED_GLASS_PANE,
            "§c§lClose",
            "§7Click to close");
        gui.setItem(44, closeItem);
        
        viewer.openInventory(gui);
    }
    
    private void fillBorder(Inventory gui) {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.text(" "));
        border.setItemMeta(meta);
        
        int[] borderSlots = {0,1,2,3,5,6,7,8,9,17,18,26,27,35,36,37,38,39,41,42,43};
        for (int slot : borderSlots) {
            gui.setItem(slot, border);
        }
    }
    
    private ItemStack createItem(Material material, String name, String... loreLines) {
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
    
    private ItemStack createProgressBar(double percentage) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§6§lProgress Bar").decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        int filled = (int) (percentage / 10);
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("§a█");
            } else {
                bar.append("§7░");
            }
        }
        bar.append("§8]");
        lore.add(Component.text(bar.toString()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("§7" + df.format(percentage) + "% save rate").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private String formatReason(String reason) {
        if (reason == null || reason.equals("none")) return "None";
        return reason.substring(0, 1).toUpperCase() + reason.substring(1).replace("-", " ");
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Component title = event.getView().title();
        if (title == null) return;
        
        String plainTitle = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(title);
        if (!plainTitle.equals(GUI_TITLE_TEXT)) return;
        
        event.setCancelled(true);
        
        if (event.getSlot() == 44) {
            event.getWhoClicked().closeInventory();
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Component title = event.getView().title();
        if (title == null) return;
        
        String plainTitle = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(title);
        if (plainTitle.equals(GUI_TITLE_TEXT)) {
            event.setCancelled(true);
        }
    }
}
