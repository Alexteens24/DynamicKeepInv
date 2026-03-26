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

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found! Economy features will be disabled, but plugin will continue to function.");
            enabled = false;
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No Economy provider found (e.g. Essentials, CMI)! Economy features will be disabled.");
            enabled = false;
            return false;
        }
        economy = rsp.getProvider();
        enabled = (economy != null);
        if (enabled) {
            plugin.getLogger().info("Economy hooked successfully! Provider: " + economy.getName());
        } else {
            plugin.getLogger().warning("Economy provider registration returned null. Economy features remain disabled.");
        }
        return enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean hasEnough(Player player, double amount) {
        if (!enabled) return true;
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (!enabled) return true;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        if (!enabled) return true;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public double getBalance(Player player) {
        if (!enabled) return 0;
        return economy.getBalance(player);
    }

    public String format(double amount) {
        if (!enabled) return String.valueOf(amount);
        return economy.format(amount);
    }
}
