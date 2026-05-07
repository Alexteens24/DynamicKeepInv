package xyz.superez.dynamickeepinv;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cached snapshot of config.yml values.
 * Eliminates repeated {@code getConfig().getXxx()} calls on the death-event hot path.
 * Created on startup and refreshed by {@link DynamicKeepInvPlugin#reloadConfig()}.
 */
public class DKIConfig {

    /**
     * Per-world overrides for schedule segments: {@code segments."<tick>"} or legacy {@code day}/{@code night}
     * (mapped to the first two milestones by sorted order).
     */
    public record WorldTimeOverride(
            Map<Long, Boolean> keepItemsByStartAt,
            Map<Long, Boolean> keepXpByStartAt,
            Boolean legacyDayKeepItems,
            Boolean legacyNightKeepItems
    ) {
        public static final WorldTimeOverride EMPTY = new WorldTimeOverride(
                Map.of(), Map.of(), null, null);

        public Boolean resolveKeepItems(long segmentStartAt, int segmentIndex) {
            Boolean v = keepItemsByStartAt.get(segmentStartAt);
            if (v != null) {
                return v;
            }
            if (segmentIndex == 0 && legacyDayKeepItems != null) {
                return legacyDayKeepItems;
            }
            if (segmentIndex == 1 && legacyNightKeepItems != null) {
                return legacyNightKeepItems;
            }
            return null;
        }

        public Boolean resolveKeepXp(long segmentStartAt) {
            return keepXpByStartAt.get(segmentStartAt);
        }
    }

    // --- Core ---
    public final boolean enabled;
    public final boolean debug;
    public final int checkInterval;

    // --- Schedule (world time milestones) ---
    public final List<ScheduleSupport.ScheduleMilestone> scheduleMilestones;

    // --- Rules (no day/night blocks — use schedule.milestones) ---
    public final boolean bypassPermissionEnabled;

    // --- Death Cause ---
    public final boolean deathCauseEnabled;
    public final boolean pvpKeepItems;
    public final boolean pvpKeepXp;
    public final boolean pveKeepItems;
    public final boolean pveKeepXp;

    // --- Worlds ---
    public final List<String> enabledWorlds;
    public final Map<String, WorldTimeOverride> worldOverrides;

    // --- Messages / Broadcast ---
    public final MessageFormatMode messageFormat;
    public final boolean broadcastEnabled;
    /** When true, notify players when the active schedule segment changes (and entering segment has announce=true). */
    public final boolean broadcastPeriodChange;
    public final boolean broadcastChat;
    public final boolean broadcastActionBar;
    public final boolean broadcastTitle;
    public final boolean broadcastSoundEnabled;
    public final String broadcastSoundDay;
    public final String broadcastSoundNight;
    public final String broadcastPermission;

    // --- Death messages ---
    public final boolean deathMsgEnabled;
    public final boolean deathMsgChat;
    public final boolean deathMsgActionBar;

    // --- Economy ---
    public final boolean economyEnabled;
    public final double economyCost;
    public final EconomyMode economyMode;
    public final long guiTimeoutSec;
    public final long guiExpireSec;

    // --- Stats ---
    public final boolean statsEnabled;

    // --- Integrations: Lands ---
    public final boolean landsEnabled;
    public final boolean landsOverride;
    public final boolean landsOwnKeepItems;
    public final boolean landsOwnKeepXp;
    public final boolean landsOtherKeepItems;
    public final boolean landsOtherKeepXp;
    public final boolean landsWildernessEnabled;
    public final boolean landsWildernessUseDeathCause;
    public final boolean landsWildernessKeepItems;
    public final boolean landsWildernessKeepXp;

    // --- Integrations: GriefPrevention ---
    public final boolean gpEnabled;
    public final boolean gpOwnKeepItems;
    public final boolean gpOwnKeepXp;
    public final boolean gpOtherKeepItems;
    public final boolean gpOtherKeepXp;
    public final boolean gpWildernessEnabled;
    public final boolean gpWildernessUseDeathCause;
    public final boolean gpWildernessKeepItems;
    public final boolean gpWildernessKeepXp;

    // --- Integrations: Graves ---
    public final boolean gravesXEnabled;
    public final boolean axGravesEnabled;
    public final boolean gravesFallbackDrop;

    // --- Integrations: WorldGuard ---
    public final boolean worldGuardEnabled;
    public final boolean worldGuardOwnRegionKeepItems;
    public final boolean worldGuardOwnRegionKeepXp;
    public final boolean worldGuardOtherRegionKeepItems;
    public final boolean worldGuardOtherRegionKeepXp;
    public final boolean worldGuardWildernessEnabled;
    public final boolean worldGuardWildernessKeepItems;
    public final boolean worldGuardWildernessKeepXp;

    // --- Integrations: Towny ---
    public final boolean townyEnabled;
    public final boolean townyOwnTownKeepItems;
    public final boolean townyOwnTownKeepXp;
    public final boolean townyOtherTownKeepItems;
    public final boolean townyOtherTownKeepXp;
    public final boolean townyWildernessEnabled;
    public final boolean townyWildernessKeepItems;
    public final boolean townyWildernessKeepXp;

    // --- MMOItems ---
    public final List<String> mmoProtectedTags;

    // --- Rules: First Death ---
    public final boolean firstDeathEnabled;
    public final boolean firstDeathKeepItems;
    public final boolean firstDeathKeepXp;

    // --- Rules: Death Streak ---
    public final boolean deathStreakEnabled;
    public final int deathStreakThreshold;
    public final int deathStreakWindowSec;
    public final boolean deathStreakKeepItems;
    public final boolean deathStreakKeepXp;

    public DKIConfig(FileConfiguration cfg) {
        String rules = ConfigReadCompat.rulesRoot(cfg);

        enabled            = ConfigReadCompat.firstBool(cfg, "plugin.enabled", "enabled", true);
        debug              = ConfigReadCompat.firstBool(cfg, "plugin.debug", "debug", false);
        checkInterval      = ConfigReadCompat.firstInt(cfg, "plugin.check-interval", "check-interval", 100);

        List<ScheduleSupport.ScheduleMilestone> loaded = ScheduleSupport.loadMilestones(cfg, rules);
        if (!ScheduleSupport.validateMilestones(loaded, null)) {
            loaded = ScheduleSupport.loadMilestones(fallbackLegacyCfg(), rules);
        }
        scheduleMilestones = List.copyOf(loaded);

        bypassPermissionEnabled = cfg.getBoolean(rules + ".bypass-permission", true);

        deathCauseEnabled  = cfg.getBoolean(rules + ".death-cause.enabled", false);
        pvpKeepItems       = cfg.getBoolean(rules + ".death-cause.pvp.keep-items", false);
        pvpKeepXp          = cfg.getBoolean(rules + ".death-cause.pvp.keep-xp", false);
        pveKeepItems       = cfg.getBoolean(rules + ".death-cause.pve.keep-items", true);
        pveKeepXp          = cfg.getBoolean(rules + ".death-cause.pve.keep-xp", true);

        enabledWorlds      = cfg.getStringList("worlds.enabled");

        ConfigurationSection overridesSection = cfg.getConfigurationSection("worlds.overrides");
        if (overridesSection != null) {
            Map<String, WorldTimeOverride> tmp = new HashMap<>();
            for (String worldName : overridesSection.getKeys(false)) {
                tmp.put(worldName, parseWorldOverride(cfg, worldName, scheduleMilestones));
            }
            worldOverrides = Collections.unmodifiableMap(tmp);
        } else {
            worldOverrides = Map.of();
        }

        String topFmt = cfg.getString("message-format");
        String nestedFmt = cfg.getString("messages.format");
        String messageFormatRaw = (topFmt != null && !topFmt.isBlank()) ? topFmt : nestedFmt;
        messageFormat          = MessageFormatMode.fromConfig(messageFormatRaw);
        broadcastEnabled       = ConfigReadCompat.firstBool(cfg, "broadcast.enabled", "messages.broadcast.enabled", true);
        if (cfg.isSet("broadcast.period-change")) {
            broadcastPeriodChange = cfg.getBoolean("broadcast.period-change");
        } else {
            boolean nd = ConfigReadCompat.firstBool(cfg, "broadcast.notify-day", "messages.broadcast.events.day-change", true);
            boolean nn = ConfigReadCompat.firstBool(cfg, "broadcast.notify-night", "messages.broadcast.events.night-change", true);
            broadcastPeriodChange = nd || nn;
        }
        broadcastChat          = ConfigReadCompat.firstBool(cfg, "broadcast.use-chat", "messages.broadcast.display.chat", true);
        broadcastActionBar     = ConfigReadCompat.firstBool(cfg, "broadcast.use-action-bar", "messages.broadcast.display.action-bar", false);
        broadcastTitle         = ConfigReadCompat.firstBool(cfg, "broadcast.use-title", "messages.broadcast.display.title", false);
        broadcastSoundEnabled  = ConfigReadCompat.firstBool(cfg, "broadcast.sound.enabled", "messages.broadcast.sound.enabled", false);
        broadcastSoundDay      = ConfigReadCompat.firstString(cfg, "broadcast.sound.day", "messages.broadcast.sound.day", "ENTITY_PLAYER_LEVELUP");
        broadcastSoundNight    = ConfigReadCompat.firstString(cfg, "broadcast.sound.night", "messages.broadcast.sound.night", "ENTITY_WITHER_SPAWN");
        broadcastPermission    = ConfigReadCompat.firstString(cfg, "broadcast.permission", "messages.broadcast.permission", "");

        deathMsgEnabled    = ConfigReadCompat.firstBool(cfg, "death-feedback.enabled", "messages.death.enabled", true);
        deathMsgChat       = ConfigReadCompat.firstBool(cfg, "death-feedback.chat", "messages.death.chat", true);
        deathMsgActionBar  = ConfigReadCompat.firstBool(cfg, "death-feedback.action-bar", "messages.death.action-bar", false);

        economyEnabled     = cfg.getBoolean("economy.enabled", false);
        economyCost        = cfg.getDouble("economy.cost", 0.0);
        economyMode        = EconomyMode.from(cfg.getString("economy.mode", "charge-to-keep"));
        guiTimeoutSec      = cfg.getLong("economy.gui.timeout", 30);
        guiExpireSec       = cfg.getLong("economy.gui.expire-time", 300);

        statsEnabled       = cfg.getBoolean("stats.enabled", true);

        landsEnabled                  = cfg.getBoolean("integrations.lands.enabled", false);
        landsOverride                 = cfg.getBoolean("integrations.lands.override-lands", false);
        landsOwnKeepItems             = cfg.getBoolean("integrations.lands.in-own-land.keep-items", false);
        landsOwnKeepXp                = cfg.getBoolean("integrations.lands.in-own-land.keep-xp", false);
        landsOtherKeepItems           = cfg.getBoolean("integrations.lands.in-other-land.keep-items", false);
        landsOtherKeepXp              = cfg.getBoolean("integrations.lands.in-other-land.keep-xp", false);
        landsWildernessEnabled        = cfg.getBoolean("integrations.lands.wilderness.enabled", false);
        landsWildernessUseDeathCause  = cfg.getBoolean("integrations.lands.wilderness.use-death-cause", false);
        landsWildernessKeepItems      = cfg.getBoolean("integrations.lands.wilderness.keep-items", false);
        landsWildernessKeepXp         = cfg.getBoolean("integrations.lands.wilderness.keep-xp", false);

        gpEnabled                  = cfg.getBoolean("integrations.griefprevention.enabled", false);
        gpOwnKeepItems             = cfg.getBoolean("integrations.griefprevention.in-own-claim.keep-items", true);
        gpOwnKeepXp                = cfg.getBoolean("integrations.griefprevention.in-own-claim.keep-xp", true);
        gpOtherKeepItems           = cfg.getBoolean("integrations.griefprevention.in-other-claim.keep-items", false);
        gpOtherKeepXp              = cfg.getBoolean("integrations.griefprevention.in-other-claim.keep-xp", false);
        gpWildernessEnabled        = cfg.getBoolean("integrations.griefprevention.wilderness.enabled", false);
        gpWildernessUseDeathCause  = cfg.getBoolean("integrations.griefprevention.wilderness.use-death-cause", false);
        gpWildernessKeepItems      = cfg.getBoolean("integrations.griefprevention.wilderness.keep-items", false);
        gpWildernessKeepXp         = cfg.getBoolean("integrations.griefprevention.wilderness.keep-xp", false);

        gravesXEnabled     = cfg.getBoolean("integrations.gravesx.enabled", false);
        axGravesEnabled    = cfg.getBoolean("integrations.axgraves.enabled", false);
        gravesFallbackDrop = cfg.getBoolean("integrations.graves.fallback-on-fail", true);

        worldGuardEnabled              = cfg.getBoolean("integrations.worldguard.enabled", false);
        worldGuardOwnRegionKeepItems   = cfg.getBoolean("integrations.worldguard.in-own-region.keep-items", true);
        worldGuardOwnRegionKeepXp      = cfg.getBoolean("integrations.worldguard.in-own-region.keep-xp", true);
        worldGuardOtherRegionKeepItems = cfg.getBoolean("integrations.worldguard.in-other-region.keep-items", false);
        worldGuardOtherRegionKeepXp    = cfg.getBoolean("integrations.worldguard.in-other-region.keep-xp", false);
        worldGuardWildernessEnabled    = cfg.getBoolean("integrations.worldguard.wilderness.enabled", false);
        worldGuardWildernessKeepItems  = cfg.getBoolean("integrations.worldguard.wilderness.keep-items", false);
        worldGuardWildernessKeepXp     = cfg.getBoolean("integrations.worldguard.wilderness.keep-xp", false);

        townyEnabled              = cfg.getBoolean("integrations.towny.enabled", false);
        townyOwnTownKeepItems     = cfg.getBoolean("integrations.towny.in-own-town.keep-items", true);
        townyOwnTownKeepXp        = cfg.getBoolean("integrations.towny.in-own-town.keep-xp", true);
        townyOtherTownKeepItems   = cfg.getBoolean("integrations.towny.in-other-town.keep-items", false);
        townyOtherTownKeepXp      = cfg.getBoolean("integrations.towny.in-other-town.keep-xp", false);
        townyWildernessEnabled    = cfg.getBoolean("integrations.towny.wilderness.enabled", false);
        townyWildernessKeepItems  = cfg.getBoolean("integrations.towny.wilderness.keep-items", false);
        townyWildernessKeepXp     = cfg.getBoolean("integrations.towny.wilderness.keep-xp", false);

        mmoProtectedTags   = ConfigReadCompat.firstStringList(cfg, "mmoitems.protected-tags", "hooks.mmoitems.protected-tags");

        firstDeathEnabled   = cfg.getBoolean(rules + ".first-death.enabled", false);
        firstDeathKeepItems = cfg.getBoolean(rules + ".first-death.keep-items", true);
        firstDeathKeepXp    = cfg.getBoolean(rules + ".first-death.keep-xp", true);

        deathStreakEnabled   = cfg.getBoolean(rules + ".streak.enabled", false);
        deathStreakThreshold = cfg.getInt(rules + ".streak.threshold", 3);
        deathStreakWindowSec = cfg.getInt(rules + ".streak.window-seconds", 300);
        deathStreakKeepItems = cfg.getBoolean(rules + ".streak.keep-items", false);
        deathStreakKeepXp    = cfg.getBoolean(rules + ".streak.keep-xp", false);
    }

    private static WorldTimeOverride parseWorldOverride(FileConfiguration cfg, String worldName,
                                                        List<ScheduleSupport.ScheduleMilestone> milestones) {
        ConfigurationSection w = cfg.getConfigurationSection("worlds.overrides." + worldName);
        if (w == null) {
            return WorldTimeOverride.EMPTY;
        }
        Map<Long, Boolean> ki = new HashMap<>();
        Map<Long, Boolean> kx = new HashMap<>();
        ConfigurationSection seg = w.getConfigurationSection("segments");
        if (seg != null) {
            for (String key : seg.getKeys(false)) {
                try {
                    long at = ScheduleSupport.normalizeWorldTime(Long.parseLong(key.trim()));
                    ConfigurationSection sub = seg.getConfigurationSection(key);
                    if (sub != null) {
                        if (sub.isSet("keep-items")) {
                            ki.put(at, sub.getBoolean("keep-items"));
                        }
                        if (sub.isSet("keep-xp")) {
                            kx.put(at, sub.getBoolean("keep-xp"));
                        }
                    }
                } catch (@SuppressWarnings("unused") NumberFormatException ignored) {
                    // ignore non-numeric keys
                }
            }
        }
        Boolean legDi = readLegacyBool(w, "day");
        Boolean legNi = readLegacyBool(w, "night");
        ConfigurationSection daySec = w.getConfigurationSection("day");
        if (daySec != null) {
            if (daySec.isSet("keep-items")) {
                legDi = daySec.getBoolean("keep-items");
            }
            if (daySec.isSet("keep-xp") && !milestones.isEmpty()) {
                kx.put(milestones.get(0).at(), daySec.getBoolean("keep-xp"));
            }
        }
        ConfigurationSection nightSec = w.getConfigurationSection("night");
        if (nightSec != null && milestones.size() >= 2) {
            if (nightSec.isSet("keep-items")) {
                legNi = nightSec.getBoolean("keep-items");
            }
            if (nightSec.isSet("keep-xp")) {
                kx.put(milestones.get(1).at(), nightSec.getBoolean("keep-xp"));
            }
        }
        if (ki.isEmpty() && kx.isEmpty() && legDi == null && legNi == null) {
            return WorldTimeOverride.EMPTY;
        }
        return new WorldTimeOverride(
                Collections.unmodifiableMap(ki),
                Collections.unmodifiableMap(kx),
                legDi, legNi);
    }

    private static Boolean readLegacyBool(ConfigurationSection w, String key) {
        if (!w.isSet(key)) {
            return null;
        }
        if (w.isBoolean(key)) {
            return w.getBoolean(key);
        }
        return null;
    }

    private static YamlConfiguration fallbackLegacyCfg() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("death-rules.day.keep-items", true);
        y.set("death-rules.day.keep-xp", true);
        y.set("death-rules.night.keep-items", false);
        y.set("death-rules.night.keep-xp", false);
        return y;
    }

    /** Short summary for status lines, e.g. {@code 0:keep,13000:drop}. */
    public String scheduleStatusSummary() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scheduleMilestones.size(); i++) {
            ScheduleSupport.ScheduleMilestone m = scheduleMilestones.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(m.at()).append(':').append(m.keepItems() ? "keep" : "drop");
        }
        return sb.toString();
    }
}
