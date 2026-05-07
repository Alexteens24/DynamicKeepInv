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
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        DKIConfig cfg = plugin.getDKIConfig();
        World world = player.getWorld();
        String lower = params.toLowerCase();

        // %dynamickeepinv_enabled%
        if (lower.equals("enabled")) {
            return cfg.enabled ? "true" : "false";
        }

        // %dynamickeepinv_keepinventory%
        if (lower.equals("keepinventory")) {
            Boolean value = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
            return value != null && value ? "true" : "false";
        }

        // %dynamickeepinv_keepinventory_formatted%
        if (lower.equals("keepinventory_formatted")) {
            Boolean value = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
            return value != null && value ? "ON" : "OFF";
        }

        // %dynamickeepinv_time%
        if (lower.equals("time")) {
            return String.valueOf(world.getTime());
        }

        // %dynamickeepinv_isday% — true when world time is in the first segment (lowest milestone at); legacy alias
        if (lower.equals("isday")) {
            return plugin.scheduleSegmentIndex(world.getTime()) == 0 ? "true" : "false";
        }

        // %dynamickeepinv_isnight%
        if (lower.equals("isnight")) {
            return plugin.scheduleSegmentIndex(world.getTime()) == 0 ? "false" : "true";
        }

        // %dynamickeepinv_period% — active segment index
        if (lower.equals("period")) {
            return String.valueOf(plugin.scheduleSegmentIndex(world.getTime()));
        }

        // %dynamickeepinv_period_<lang>%  (kept for compatibility; same as period)
        if (lower.startsWith("period_")) {
            return String.valueOf(plugin.scheduleSegmentIndex(world.getTime()));
        }

        // %dynamickeepinv_world%
        if (lower.equals("world")) {
            return world.getName();
        }

        // %dynamickeepinv_world_enabled%
        if (lower.equals("world_enabled")) {
            return plugin.isWorldEnabled(world) ? "true" : "false";
        }

        // %dynamickeepinv_has_bypass%
        if (lower.equals("has_bypass")) {
            return player.hasPermission("dynamickeepinv.bypass") ? "true" : "false";
        }

        // %dynamickeepinv_advanced_enabled% (deprecated alias for enabled)
        if (lower.equals("advanced_enabled")) {
            return cfg.enabled ? "true" : "false";
        }

        // %dynamickeepinv_economy_enabled%
        if (lower.equals("economy_enabled")) {
            return cfg.economyEnabled ? "true" : "false";
        }

        // %dynamickeepinv_economy_cost%
        if (lower.equals("economy_cost")) {
            return String.valueOf(cfg.economyCost);
        }

        // %dynamickeepinv_lands_enabled%
        if (lower.equals("lands_enabled")) {
            return cfg.landsEnabled ? "true" : "false";
        }

        // %dynamickeepinv_gp_enabled%
        if (lower.equals("gp_enabled")) {
            return cfg.gpEnabled ? "true" : "false";
        }

        // --- Stats placeholders ---

        if (lower.equals("stats_enabled")) {
            return cfg.statsEnabled ? "true" : "false";
        }

        StatsManager stats = plugin.getStatsManager();

        if (lower.equals("stats_deaths_saved")) {
            return stats != null ? String.valueOf(stats.getDeathsSaved(player.getUniqueId())) : "0";
        }

        if (lower.equals("stats_deaths_lost")) {
            return stats != null ? String.valueOf(stats.getDeathsLost(player.getUniqueId())) : "0";
        }

        if (lower.equals("stats_total_deaths")) {
            return stats != null ? String.valueOf(stats.getTotalDeaths(player.getUniqueId())) : "0";
        }

        if (lower.equals("stats_save_rate")) {
            return stats != null ? String.format("%.1f%%", stats.getSaveRate(player.getUniqueId())) : "0%";
        }

        if (lower.equals("stats_economy_paid")) {
            return stats != null ? String.format("%.2f", stats.getTotalEconomyPaid(player.getUniqueId())) : "0";
        }

        if (lower.equals("stats_global_saved")) {
            return stats != null ? String.valueOf(stats.getGlobalDeathsSaved()) : "0";
        }

        if (lower.equals("stats_global_lost")) {
            return stats != null ? String.valueOf(stats.getGlobalDeathsLost()) : "0";
        }

        if (lower.equals("stats_global_rate")) {
            return stats != null ? String.format("%.1f%%", stats.getGlobalSaveRate()) : "0%";
        }

        return null;
    }
}
