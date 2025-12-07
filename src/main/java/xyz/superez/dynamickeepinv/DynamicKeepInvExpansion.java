package xyz.superez.dynamickeepinv;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DynamicKeepInvExpansion extends PlaceholderExpansion {
    
    private final DynamicKeepInvPlugin plugin;
    
    public DynamicKeepInvExpansion(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "dynamickeepinv";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "SuperEZ";
    }
    
    @Override
    public @NotNull String getVersion() {
        return "1.0.14";
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        World world = player.getWorld();
        
        // %dynamickeepinv_enabled%
        if (params.equalsIgnoreCase("enabled")) {
            return plugin.getConfig().getBoolean("enabled", true) ? "true" : "false";
        }
        
        // %dynamickeepinv_keepinventory%
        if (params.equalsIgnoreCase("keepinventory")) {
            Boolean value = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
            return value != null && value ? "true" : "false";
        }
        
        // %dynamickeepinv_keepinventory_formatted%
        if (params.equalsIgnoreCase("keepinventory_formatted")) {
            Boolean value = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
            return value != null && value ? "ON" : "OFF";
        }
        
        // %dynamickeepinv_time%
        if (params.equalsIgnoreCase("time")) {
            return String.valueOf(world.getTime());
        }
        
        // %dynamickeepinv_isday%
        if (params.equalsIgnoreCase("isday")) {
            long time = world.getTime();
            long dayStart = plugin.getConfig().getLong("day-start", 0);
            long nightStart = plugin.getConfig().getLong("night-start", 13000);
            return plugin.isTimeInRange(time, dayStart, nightStart) ? "true" : "false";
        }
        
        // %dynamickeepinv_isnight%
        if (params.equalsIgnoreCase("isnight")) {
            long time = world.getTime();
            long dayStart = plugin.getConfig().getLong("day-start", 0);
            long nightStart = plugin.getConfig().getLong("night-start", 13000);
            return plugin.isTimeInRange(time, dayStart, nightStart) ? "false" : "true";
        }
        
        // %dynamickeepinv_period%
        if (params.equalsIgnoreCase("period")) {
            long time = world.getTime();
            long dayStart = plugin.getConfig().getLong("day-start", 0);
            long nightStart = plugin.getConfig().getLong("night-start", 13000);
            return plugin.isTimeInRange(time, dayStart, nightStart) ? "Day" : "Night";
        }
        
        // %dynamickeepinv_period_<lang>% (dynamickeepinv_period_vi, dynamickeepinv_period_en)
        if (params.toLowerCase().startsWith("period_")) {
            String lang = params.substring(7).toLowerCase();
            long time = world.getTime();
            long dayStart = plugin.getConfig().getLong("day-start", 0);
            long nightStart = plugin.getConfig().getLong("night-start", 13000);
            boolean isDay = plugin.isTimeInRange(time, dayStart, nightStart);
            
            if (lang.equals("vi")) {
                return isDay ? "Ngày" : "Đêm";
            } else {
                return isDay ? "Day" : "Night";
            }
        }
        
        // %dynamickeepinv_world%
        if (params.equalsIgnoreCase("world")) {
            return world.getName();
        }
        
        // %dynamickeepinv_world_enabled%
        if (params.equalsIgnoreCase("world_enabled")) {
            return plugin.isWorldEnabled(world) ? "true" : "false";
        }
        
        // %dynamickeepinv_has_bypass%
        if (params.equalsIgnoreCase("has_bypass")) {
            return player.hasPermission("dynamickeepinv.bypass") ? "true" : "false";
        }
        
        // %dynamickeepinv_advanced_enabled%
        if (params.equalsIgnoreCase("advanced_enabled")) {
            return plugin.getConfig().getBoolean("advanced.enabled", false) ? "true" : "false";
        }
        
        // %dynamickeepinv_economy_enabled%
        if (params.equalsIgnoreCase("economy_enabled")) {
            return plugin.getConfig().getBoolean("advanced.economy.enabled", false) ? "true" : "false";
        }
        
        // %dynamickeepinv_economy_cost%
        if (params.equalsIgnoreCase("economy_cost")) {
            return String.valueOf(plugin.getConfig().getDouble("advanced.economy.cost", 0.0));
        }
        
        // %dynamickeepinv_lands_enabled%
        if (params.equalsIgnoreCase("lands_enabled")) {
            return plugin.getConfig().getBoolean("advanced.protection.lands.enabled", false) ? "true" : "false";
        }
        
        // %dynamickeepinv_gp_enabled%
        if (params.equalsIgnoreCase("gp_enabled")) {
            return plugin.getConfig().getBoolean("advanced.protection.griefprevention.enabled", false) ? "true" : "false";
        }
        
        if (params.equalsIgnoreCase("stats_enabled")) {
            return plugin.getConfig().getBoolean("stats.enabled", true) ? "true" : "false";
        }
        
        if (params.equalsIgnoreCase("stats_deaths_saved")) {
            StatsManager stats = plugin.getStatsManager();
            return stats != null ? String.valueOf(stats.getDeathsSaved(player.getUniqueId())) : "0";
        }
        
        if (params.equalsIgnoreCase("stats_deaths_lost")) {
            StatsManager stats = plugin.getStatsManager();
            return stats != null ? String.valueOf(stats.getDeathsLost(player.getUniqueId())) : "0";
        }
        
        if (params.equalsIgnoreCase("stats_total_deaths")) {
            StatsManager stats = plugin.getStatsManager();
            return stats != null ? String.valueOf(stats.getTotalDeaths(player.getUniqueId())) : "0";
        }
        
        if (params.equalsIgnoreCase("stats_save_rate")) {
            StatsManager stats = plugin.getStatsManager();
            if (stats != null) {
                double rate = stats.getSaveRate(player.getUniqueId());
                return String.format("%.1f%%", rate);
            }
            return "0%";
        }
        
        if (params.equalsIgnoreCase("stats_economy_paid")) {
            StatsManager stats = plugin.getStatsManager();
            return stats != null ? String.format("%.2f", stats.getTotalEconomyPaid(player.getUniqueId())) : "0";
        }
        
        if (params.equalsIgnoreCase("stats_global_saved")) {
            StatsManager stats = plugin.getStatsManager();
            return stats != null ? String.valueOf(stats.getGlobalDeathsSaved()) : "0";
        }
        
        if (params.equalsIgnoreCase("stats_global_lost")) {
            StatsManager stats = plugin.getStatsManager();
            return stats != null ? String.valueOf(stats.getGlobalDeathsLost()) : "0";
        }
        
        if (params.equalsIgnoreCase("stats_global_rate")) {
            StatsManager stats = plugin.getStatsManager();
            if (stats != null) {
                double rate = stats.getGlobalSaveRate();
                return String.format("%.1f%%", rate);
            }
            return "0%";
        }
        
        return null;
    }
}
