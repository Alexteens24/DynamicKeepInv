package xyz.superez.dynamickeepinv;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    private final DynamicKeepInvPlugin plugin;
    private Economy economy = null;
    private boolean enabled = false;

    public EconomyManager(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }

    public void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found! Economy features will be disabled, but plugin will continue to function.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No Economy provider found (e.g. Essentials, CMI)! Economy features will be disabled.");
            return;
        }
        economy = rsp.getProvider();
        enabled = (economy != null);
        if (enabled) {
            plugin.getLogger().info("Economy hooked successfully! Provider: " + economy.getName());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean hasEnough(Player player, double amount) {
        if (!enabled) return true; // If eco disabled, treat as having enough (free)
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (!enabled) return true; // If eco disabled, treat as success (free)
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
    
    public String format(double amount) {
        if (!enabled) return String.valueOf(amount);
        return economy.format(amount);
    }
}
