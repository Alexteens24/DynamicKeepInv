package xyz.superez.dynamickeepinv;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Cached snapshot of config.yml values.
 * Eliminates repeated {@code getConfig().getXxx()} calls on the death-event hot path.
 * Created on startup and refreshed by {@link DynamicKeepInvPlugin#reloadConfig()}.
 */
public class DKIConfig {

    // --- Core ---
    public final boolean enabled;
    public final boolean debug;
    public final int checkInterval;

    // --- Time ---
    public final long dayStart;
    public final long nightStart;
    public final long dayTrigger;
    public final long nightTrigger;

    // --- Rules ---
    public final boolean dayKeepItems;
    public final boolean dayKeepXp;
    public final boolean nightKeepItems;
    public final boolean nightKeepXp;
    public final boolean bypassPermissionEnabled;

    // --- Death Cause ---
    public final boolean deathCauseEnabled;
    public final boolean pvpKeepItems;
    public final boolean pvpKeepXp;
    public final boolean pveKeepItems;
    public final boolean pveKeepXp;

    // --- Worlds ---
    public final List<String> enabledWorlds;

    // --- Messages / Broadcast ---
    public final boolean broadcastEnabled;
    public final boolean broadcastDayChange;
    public final boolean broadcastNightChange;
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

    // --- Rules: Biome ---
    public final boolean biomeRuleEnabled;

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
        // Core
        enabled            = cfg.getBoolean("enabled", true);
        debug              = cfg.getBoolean("debug", false);
        checkInterval      = cfg.getInt("check-interval", 100);

        // Time
        dayStart           = cfg.getLong("time.day-start", 0);
        nightStart         = cfg.getLong("time.night-start", 13000);
        long rawDayTrig    = cfg.getLong("time.triggers.day", -1);
        long rawNightTrig  = cfg.getLong("time.triggers.night", -1);
        dayTrigger         = rawDayTrig  < 0 ? dayStart  : rawDayTrig;
        nightTrigger       = rawNightTrig < 0 ? nightStart : rawNightTrig;

        // Rules
        dayKeepItems       = cfg.getBoolean("rules.day.keep-items",   true);
        dayKeepXp          = cfg.getBoolean("rules.day.keep-xp",     true);
        nightKeepItems     = cfg.getBoolean("rules.night.keep-items", false);
        nightKeepXp        = cfg.getBoolean("rules.night.keep-xp",   false);
        bypassPermissionEnabled = cfg.getBoolean("rules.bypass-permission", true);

        // Death cause
        deathCauseEnabled  = cfg.getBoolean("rules.death-cause.enabled", false);
        pvpKeepItems       = cfg.getBoolean("rules.death-cause.pvp.keep-items", false);
        pvpKeepXp          = cfg.getBoolean("rules.death-cause.pvp.keep-xp",   false);
        pveKeepItems       = cfg.getBoolean("rules.death-cause.pve.keep-items", true);
        pveKeepXp          = cfg.getBoolean("rules.death-cause.pve.keep-xp",   true);

        // Worlds
        enabledWorlds      = cfg.getStringList("worlds.enabled");

        // Broadcast
        broadcastEnabled       = cfg.getBoolean("messages.broadcast.enabled",            true);
        broadcastDayChange     = cfg.getBoolean("messages.broadcast.events.day-change",  true);
        broadcastNightChange   = cfg.getBoolean("messages.broadcast.events.night-change",true);
        broadcastChat          = cfg.getBoolean("messages.broadcast.display.chat",       true);
        broadcastActionBar     = cfg.getBoolean("messages.broadcast.display.action-bar", false);
        broadcastTitle         = cfg.getBoolean("messages.broadcast.display.title",      false);
        broadcastSoundEnabled  = cfg.getBoolean("messages.broadcast.sound.enabled",      false);
        broadcastSoundDay      = cfg.getString("messages.broadcast.sound.day",  "ENTITY_PLAYER_LEVELUP");
        broadcastSoundNight    = cfg.getString("messages.broadcast.sound.night","ENTITY_WITHER_SPAWN");
        broadcastPermission    = cfg.getString("messages.broadcast.permission", "");

        // Death messages
        deathMsgEnabled    = cfg.getBoolean("messages.death.enabled",    true);
        deathMsgChat       = cfg.getBoolean("messages.death.chat",       true);
        deathMsgActionBar  = cfg.getBoolean("messages.death.action-bar", false);

        // Economy
        economyEnabled     = cfg.getBoolean("economy.enabled", false);
        economyCost        = cfg.getDouble("economy.cost",    0.0);
        economyMode        = EconomyMode.from(cfg.getString("economy.mode", "charge-to-keep"));
        guiTimeoutSec      = cfg.getLong("economy.gui.timeout",     30);
        guiExpireSec       = cfg.getLong("economy.gui.expire-time", 300);

        // Stats
        statsEnabled       = cfg.getBoolean("stats.enabled", true);

        // Lands
        landsEnabled                  = cfg.getBoolean("integrations.lands.enabled",              false);
        landsOverride                 = cfg.getBoolean("integrations.lands.override-lands",        false);
        landsOwnKeepItems             = cfg.getBoolean("integrations.lands.in-own-land.keep-items", false);
        landsOwnKeepXp                = cfg.getBoolean("integrations.lands.in-own-land.keep-xp",   false);
        landsOtherKeepItems           = cfg.getBoolean("integrations.lands.in-other-land.keep-items",false);
        landsOtherKeepXp              = cfg.getBoolean("integrations.lands.in-other-land.keep-xp",  false);
        landsWildernessEnabled        = cfg.getBoolean("integrations.lands.wilderness.enabled",      false);
        landsWildernessUseDeathCause  = cfg.getBoolean("integrations.lands.wilderness.use-death-cause",false);
        landsWildernessKeepItems      = cfg.getBoolean("integrations.lands.wilderness.keep-items",   false);
        landsWildernessKeepXp         = cfg.getBoolean("integrations.lands.wilderness.keep-xp",     false);

        // GriefPrevention
        gpEnabled                  = cfg.getBoolean("integrations.griefprevention.enabled",                false);
        gpOwnKeepItems             = cfg.getBoolean("integrations.griefprevention.in-own-claim.keep-items", true);
        gpOwnKeepXp                = cfg.getBoolean("integrations.griefprevention.in-own-claim.keep-xp",   true);
        gpOtherKeepItems           = cfg.getBoolean("integrations.griefprevention.in-other-claim.keep-items",false);
        gpOtherKeepXp              = cfg.getBoolean("integrations.griefprevention.in-other-claim.keep-xp",  false);
        gpWildernessEnabled        = cfg.getBoolean("integrations.griefprevention.wilderness.enabled",       false);
        gpWildernessUseDeathCause  = cfg.getBoolean("integrations.griefprevention.wilderness.use-death-cause",false);
        gpWildernessKeepItems      = cfg.getBoolean("integrations.griefprevention.wilderness.keep-items",    false);
        gpWildernessKeepXp         = cfg.getBoolean("integrations.griefprevention.wilderness.keep-xp",      false);

        // Graves
        gravesXEnabled     = cfg.getBoolean("integrations.gravesx.enabled",  false);
        axGravesEnabled    = cfg.getBoolean("integrations.axgraves.enabled",  false);
        gravesFallbackDrop = cfg.getBoolean("integrations.graves.fallback-on-fail", true);

        // WorldGuard
        worldGuardEnabled              = cfg.getBoolean("integrations.worldguard.enabled",                   false);
        worldGuardOwnRegionKeepItems   = cfg.getBoolean("integrations.worldguard.in-own-region.keep-items",  true);
        worldGuardOwnRegionKeepXp      = cfg.getBoolean("integrations.worldguard.in-own-region.keep-xp",    true);
        worldGuardOtherRegionKeepItems = cfg.getBoolean("integrations.worldguard.in-other-region.keep-items",false);
        worldGuardOtherRegionKeepXp    = cfg.getBoolean("integrations.worldguard.in-other-region.keep-xp",  false);

        // Towny
        townyEnabled              = cfg.getBoolean("integrations.towny.enabled",                    false);
        townyOwnTownKeepItems     = cfg.getBoolean("integrations.towny.in-own-town.keep-items",     true);
        townyOwnTownKeepXp        = cfg.getBoolean("integrations.towny.in-own-town.keep-xp",       true);
        townyOtherTownKeepItems   = cfg.getBoolean("integrations.towny.in-other-town.keep-items",   false);
        townyOtherTownKeepXp      = cfg.getBoolean("integrations.towny.in-other-town.keep-xp",     false);
        townyWildernessEnabled    = cfg.getBoolean("integrations.towny.wilderness.enabled",         false);
        townyWildernessKeepItems  = cfg.getBoolean("integrations.towny.wilderness.keep-items",      false);
        townyWildernessKeepXp     = cfg.getBoolean("integrations.towny.wilderness.keep-xp",        false);

        // MMOItems
        mmoProtectedTags   = cfg.getStringList("hooks.mmoitems.protected-tags");

        // Biome rule
        biomeRuleEnabled   = cfg.getBoolean("rules.biome.enabled", false);

        // First death
        firstDeathEnabled  = cfg.getBoolean("rules.first-death.enabled",    false);
        firstDeathKeepItems = cfg.getBoolean("rules.first-death.keep-items", true);
        firstDeathKeepXp   = cfg.getBoolean("rules.first-death.keep-xp",    true);

        // Death streak
        deathStreakEnabled    = cfg.getBoolean("rules.streak.enabled",       false);
        deathStreakThreshold  = cfg.getInt("rules.streak.threshold",         3);
        deathStreakWindowSec  = cfg.getInt("rules.streak.window-seconds",    300);
        deathStreakKeepItems  = cfg.getBoolean("rules.streak.keep-items",    false);
        deathStreakKeepXp     = cfg.getBoolean("rules.streak.keep-xp",      false);
    }
}
