package xyz.superez.dynamickeepinv;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import xyz.superez.dynamickeepinv.hooks.LandsHook;
import xyz.superez.dynamickeepinv.hooks.GriefPreventionHook;
import xyz.superez.dynamickeepinv.hooks.GravesXHook;
import xyz.superez.dynamickeepinv.hooks.AxGravesHook;
import xyz.superez.dynamickeepinv.hooks.MMOItemsHook;
import xyz.superez.dynamickeepinv.rules.*;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.bukkit.Sound;

public class DynamicKeepInvPlugin extends JavaPlugin {
    public DynamicKeepInvPlugin() {
        super();
    }

    @SuppressWarnings("removal")
    protected DynamicKeepInvPlugin(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
    }


    private BukkitRunnable checkTask;
    private ScheduledTask foliaTask;
    private final Map<String, Boolean> lastWasDayMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> originalKeepInvValues = new ConcurrentHashMap<>();
    private final Map<String, Long> lastBroadcastTime = new ConcurrentHashMap<>();
    private FileConfiguration messagesConfig;
    private String lang;
    private boolean isFolia = false;
    private volatile boolean isShuttingDown = false;
    private static final long BROADCAST_COOLDOWN = 10000;
    private static final long ECONOMY_RETRY_DELAY_MS = 30000;
    private static final int ECONOMY_MAX_RETRIES = 5;

    private volatile EconomyManager economyManager;
    private final Object economyLock = new Object();
    private final AtomicLong nextEconomyRetryTimeMs = new AtomicLong(0L);
    private int economyRetryCount = 0;

    private IntegrationManager integrationManager;
    private CommandDispatcher commandDispatcher;
    private DynamicKeepInvExpansion placeholderExpansion;
    private StatsManager statsManager;
    private StatsGUI statsGUI;
    private StatsListener statsListener;
    private PendingDeathManager pendingDeathManager;
    private DeathConfirmGUI deathConfirmGUI;
    private RuleManager ruleManager;

    @Override
    public void onEnable() {
        detectFolia();
        saveDefaultConfig();
        new ConfigMigration(this).checkAndMigrate();
        loadMessages();
        integrationManager = new IntegrationManager(this);
        commandDispatcher = new CommandDispatcher(this);
        reloadIntegrations();
        setupRuleManager();

        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);

        reloadStatsSystem();

        // Register Command Completer
        getCommand("dynamickeepinv").setTabCompleter(new CommandCompleter(this));

        // Initialize GUI economy mode components
        if (getConfig().getBoolean("economy.enabled", false)
            && "gui".equalsIgnoreCase(getConfig().getString("economy.mode", "charge-to-keep"))) {
            pendingDeathManager = new PendingDeathManager(this);
            deathConfirmGUI = new DeathConfirmGUI(this);
            getLogger().info("Death confirmation GUI mode enabled!");
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = new DynamicKeepInvExpansion(this);
            placeholderExpansion.register();
            getLogger().info("PlaceholderAPI hooked!");
        }

        getLogger().info("DynamicKeepInv is starting... (Platform: " + (isFolia ? "Folia" : "Paper/Spigot") + ")");

        if (getConfig().getBoolean("enabled", true)) {
            startChecking();
            List<String> enabledWorlds = getConfig().getStringList("worlds.enabled");
            if (!enabledWorlds.isEmpty()) {
                getLogger().info("Enabled worlds: " + String.join(", ", enabledWorlds));
            } else {
                getLogger().info("Enabled for all worlds (empty enabled-worlds list).");
            }
            getLogger().info("DynamicKeepInv enabled! Keep inventory will change based on time.");
        } else {
            getLogger().info("DynamicKeepInv is disabled in config.");
        }
    }

    @Override
    public void onDisable() {
        isShuttingDown = true;
        stopChecking(true);
        if (pendingDeathManager != null) {
            pendingDeathManager.close();
        }
        cleanupStatsSystem();
        getLogger().info("DynamicKeepInv has been disabled.");
    }

    public void handleWorldUnload(World world) {
        String worldName = world.getName();

        if (originalKeepInvValues.containsKey(worldName)) {
            Boolean originalValue = originalKeepInvValues.get(worldName);
            if (originalValue != null) {
                world.setGameRule(GameRule.KEEP_INVENTORY, originalValue);
                debug("Restored keepInventory for unloading world " + worldName + " to " + originalValue);
            }
            originalKeepInvValues.remove(worldName);
        }

        lastWasDayMap.remove(worldName);
        lastBroadcastTime.remove(worldName);
    }

    public EconomyManager getEconomyManager() {
        if (!getConfig().getBoolean("economy.enabled", false)) {
            return economyManager;
        }

        EconomyManager localRef = economyManager;
        if (localRef == null) {
            synchronized (economyLock) {
                localRef = economyManager;
                if (localRef == null) {
                    localRef = new EconomyManager(this);
                    localRef.setupEconomy();
                    economyManager = localRef;
                }
            }
        }

        if (!localRef.isEnabled() && economyRetryCount < ECONOMY_MAX_RETRIES) {
            long now = System.currentTimeMillis();
            if (now >= nextEconomyRetryTimeMs.get()) {
                synchronized (economyLock) {
                    if (now >= nextEconomyRetryTimeMs.get() && economyRetryCount < ECONOMY_MAX_RETRIES) {
                        nextEconomyRetryTimeMs.set(now + ECONOMY_RETRY_DELAY_MS);
                        economyRetryCount++;
                        debug("Economy retry attempt " + economyRetryCount + "/" + ECONOMY_MAX_RETRIES);
                        if (localRef.setupEconomy()) {
                            economyRetryCount = 0; // Reset on success
                        } else if (economyRetryCount >= ECONOMY_MAX_RETRIES) {
                            getLogger().warning("Economy setup failed after " + ECONOMY_MAX_RETRIES + " attempts. Economy features disabled.");
                        }
                    }
                }
            }
        }

        return localRef;
    }

    public boolean isWorldEnabled(World world) {
        List<String> enabledWorlds = getConfig().getStringList("worlds.enabled");
        if (enabledWorlds == null || enabledWorlds.isEmpty()) {
            return true;
        }

        String worldName = world.getName();
        for (String enabled : enabledWorlds) {
            if (enabled.equalsIgnoreCase(worldName)) {
                return true;
            }
        }
        return false;
    }

    private void detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
            getLogger().info("Folia detected! Using region-based scheduler.");
        } catch (ClassNotFoundException e) {
            isFolia = false;
            getLogger().info("Paper/Spigot detected! Using standard scheduler.");
        }
    }

    void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        lang = messagesConfig.getString("language", "en");
    }

    void reloadIntegrations() {
        boolean economyEnabled = getConfig().getBoolean("economy.enabled", false);
        synchronized (economyLock) {
            economyManager = null;
            nextEconomyRetryTimeMs.set(0L);
            economyRetryCount = 0;
        }
        if (economyEnabled) {
            getEconomyManager();
        }
        integrationManager.reload();
    }

    public String getMessage(String path) {
        String message = messagesConfig.getString("messages." + lang + "." + path);
        if (message == null) {
            message = messagesConfig.getString("messages.en." + path);
        }
        if (message == null) {
            getLogger().warning("Missing message key: messages." + lang + "." + path);
            getLogger().warning("Available keys: " + messagesConfig.getConfigurationSection("messages." + lang + ".status"));
            message = "Missing message: " + path;
        }
        return message;
    }

    public Component parseMessage(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    void startChecking() {
        stopChecking(false);

        int interval = getConfig().getInt("check-interval", 100);

        if (isFolia) {
            GlobalRegionScheduler scheduler = Bukkit.getGlobalRegionScheduler();
            foliaTask = scheduler.runAtFixedRate(this, (task) -> {
                checkAndUpdateKeepInventory();
            }, 1L, interval);
            debug("Started Folia checking task with interval: " + interval + " ticks");
        } else {
            checkTask = new BukkitRunnable() {
                @Override
                public void run() {
                    checkAndUpdateKeepInventory();
                }
            };
            checkTask.runTaskTimer(this, 0L, interval);
            debug("Started Bukkit checking task with interval: " + interval + " ticks");
        }
    }

    void stopChecking(boolean resetGameRules) {
        if (isFolia) {
            if (foliaTask != null) {
                foliaTask.cancel();
                foliaTask = null;
            }
        } else {
            if (checkTask != null) {
                checkTask.cancel();
                checkTask = null;
            }
        }

        if (resetGameRules) {
            restoreOriginalGamerules();
        }
    }

    private void checkAndUpdateKeepInventory() {
        boolean keepInvDay = getConfig().getBoolean("rules.day.keep-items", true);
        boolean keepInvNight = getConfig().getBoolean("rules.night.keep-items", false);
        long dayStart = getConfig().getLong("time.day-start", 0);
        long nightStart = getConfig().getLong("time.night-start", 13000);

        long dayTrigger = getConfig().getLong("time.triggers.day", -1);
        long nightTrigger = getConfig().getLong("time.triggers.night", -1);
        if (dayTrigger < 0) dayTrigger = dayStart;
        if (nightTrigger < 0) nightTrigger = nightStart;

        for (World world : Bukkit.getWorlds()) {
            if (!isWorldEnabled(world)) {
                continue;
            }

            final long finalDayTrigger = dayTrigger;
            final long finalNightTrigger = nightTrigger;
            if (isFolia) {
                final World currentWorld = world;
                Bukkit.getRegionScheduler().execute(this, currentWorld.getSpawnLocation(), () ->
                        processWorld(currentWorld, keepInvDay, keepInvNight, dayStart, nightStart, finalDayTrigger, finalNightTrigger));
            } else {
                processWorld(world, keepInvDay, keepInvNight, dayStart, nightStart, dayTrigger, nightTrigger);
            }
        }
    }

    private void processWorld(World world, boolean keepInvDay, boolean keepInvNight,
                              long dayStart, long nightStart, long dayTrigger, long nightTrigger) {
        if (isShuttingDown) {
            return;
        }

        long time = world.getTime();
        boolean isDay = isTimeInRange(time, dayStart, nightStart);
        boolean shouldTriggerDay = isTimeInRange(time, dayTrigger, nightTrigger);
        boolean shouldKeepInv = getWorldKeepInventory(world, isDay, keepInvDay, keepInvNight);

        Boolean currentKeepInv = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
        if (currentKeepInv == null || currentKeepInv != shouldKeepInv) {
            rememberOriginalGameRule(world, currentKeepInv);

            debug(String.format("Updating World: %s, Time: %d, IsDay: %s, TriggerDay: %s, KeepInv: %s -> %s",
                    world.getName(), time, isDay, shouldTriggerDay, currentKeepInv, shouldKeepInv));

            world.setGameRule(GameRule.KEEP_INVENTORY, shouldKeepInv);

            Boolean lastWasDay = lastWasDayMap.get(world.getName());
            if (lastWasDay != null && lastWasDay != isDay) {
                String msgKey = isDay ? "game.day-detected" : "game.night-detected";
                String message = getMessage(msgKey);
                if (message != null && !message.startsWith("Missing message:")) {
                    message = message.replace("{world}", world.getName());
                    getLogger().info(LegacyComponentSerializer.legacyAmpersand().serialize(parseMessage(message)));

                    if (shouldBroadcast(isDay)) {
                        long currentTime = System.currentTimeMillis();
                        Long lastTime = lastBroadcastTime.get(world.getName());
                        if (lastTime == null || (currentTime - lastTime) >= BROADCAST_COOLDOWN) {
                            lastBroadcastTime.put(world.getName(), currentTime);
                            sendNotifications(world, message, isDay);
                        }
                    }
                }
            }
        }

        lastWasDayMap.put(world.getName(), isDay);
    }

    private boolean shouldBroadcast(boolean isDay) {
        if (!getConfig().getBoolean("messages.broadcast.enabled", true)) return false;
        if (isDay && !getConfig().getBoolean("messages.broadcast.events.day-change", true)) return false;
        if (!isDay && !getConfig().getBoolean("messages.broadcast.events.night-change", true)) return false;
        return true;
    }

    private void sendNotifications(World world, String message, boolean isDay) {
        Component component = parseMessage(message);
        boolean chat = getConfig().getBoolean("messages.broadcast.display.chat", true);
        boolean actionBar = getConfig().getBoolean("messages.broadcast.display.action-bar", false);
        boolean titleEnabled = getConfig().getBoolean("messages.broadcast.display.title", false);
        boolean soundEnabled = getConfig().getBoolean("messages.broadcast.sound.enabled", false);
        String soundName = isDay ? getConfig().getString("messages.broadcast.sound.day") : getConfig().getString("messages.broadcast.sound.night");
        Sound sound = null;

        if (soundEnabled) {
            try {
                sound = Sound.valueOf(soundName);
            } catch (Exception e) {
                getLogger().warning("Invalid sound name: " + soundName);
                soundEnabled = false;
            }
        }

        final Sound finalSound = sound;
        final boolean finalSoundEnabled = soundEnabled;

        for (Player p : new java.util.ArrayList<>(world.getPlayers())) {
            Runnable notificationTask = () -> {
                if (chat) p.sendMessage(component);
                if (actionBar) p.sendActionBar(component);
                if (titleEnabled) {
                    Title title = Title.title(
                        Component.empty(),
                        component,
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
                    );
                    p.showTitle(title);
                }
                if (finalSoundEnabled && finalSound != null) {
                    p.playSound(p.getLocation(), finalSound, 1.0f, 1.0f);
                }
            };

            if (isFolia) {
                p.getScheduler().execute(this, notificationTask, null, 1L);
            } else {
                notificationTask.run();
            }
        }
    }

    private void rememberOriginalGameRule(World world, Boolean currentValue) {
        if (world == null) {
            return;
        }

        String worldName = world.getName();
        if (originalKeepInvValues.containsKey(worldName)) {
            return;
        }

        Boolean valueToStore = currentValue;
        if (valueToStore == null) {
            valueToStore = Boolean.FALSE;
            debug("World " + worldName + " reported null keepInventory, defaulting to false for restoration");
        }

        originalKeepInvValues.put(worldName, valueToStore);
    }

    private void restoreOriginalGamerules() {
        for (World world : Bukkit.getWorlds()) {
            if (world == null) continue;

            String worldName = world.getName();
            if (!originalKeepInvValues.containsKey(worldName)) {
                continue;
            }

            Boolean originalValue = originalKeepInvValues.get(worldName);
            if (originalValue != null) {
                try {
                    if (isFolia && !isShuttingDown) {
                        Bukkit.getRegionScheduler().execute(this, world.getSpawnLocation(), () -> {
                            world.setGameRule(GameRule.KEEP_INVENTORY, originalValue);
                            getLogger().info("Restored keepInventory for " + worldName + " to " + originalValue);
                        });
                    } else {
                        world.setGameRule(GameRule.KEEP_INVENTORY, originalValue);
                        getLogger().info("Restored keepInventory for " + worldName + " to " + originalValue);
                    }
                } catch (Exception e) {
                    getLogger().warning("Failed to restore gamerule for " + worldName + ": " + e.getMessage());
                }
            } else {
                debug("Original value for " + worldName + " was null, leaving unchanged");
            }
        }

        originalKeepInvValues.clear();
        lastWasDayMap.clear();
        lastBroadcastTime.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return commandDispatcher.dispatch(sender, command, label, args);
    }

    private void cleanupStatsSystem() {
        if (statsManager != null) {
            statsManager.close();
            statsManager = null;
        }
        if (statsGUI != null) {
            statsGUI.unregister();
            statsGUI = null;
        }
        if (statsListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(statsListener);
            statsListener = null;
        }
    }

    void reloadStatsSystem() {
        cleanupStatsSystem();

        if (getConfig().getBoolean("stats.enabled", true)) {
            statsManager = new StatsManager(this);
            statsGUI = new StatsGUI(this);
            statsListener = new StatsListener(this);
            getServer().getPluginManager().registerEvents(statsListener, this);
            getLogger().info("Stats system enabled with SQLite database!");
        }
    }

    void reloadPendingDeathManager() {
        // Close old manager if exists
        if (pendingDeathManager != null) {
            pendingDeathManager.close();
            pendingDeathManager = null;
        }
        // Unregister old GUI listener if exists
        if (deathConfirmGUI != null) {
            org.bukkit.event.HandlerList.unregisterAll(deathConfirmGUI);
            deathConfirmGUI = null;
        }

        // Initialize if GUI mode enabled
        if (getConfig().getBoolean("economy.enabled", false)
            && "gui".equalsIgnoreCase(getConfig().getString("economy.mode", "charge-to-keep"))) {
            pendingDeathManager = new PendingDeathManager(this);
            deathConfirmGUI = new DeathConfirmGUI(this);
            getLogger().info("Death confirmation GUI mode enabled!");
        }
    }

    public boolean isTimeInRange(long time, long rangeStart, long rangeEnd) {
        long normalizedTime = ((time % 24000L) + 24000L) % 24000L;
        long start = ((rangeStart % 24000L) + 24000L) % 24000L;
        long end = ((rangeEnd % 24000L) + 24000L) % 24000L;

        if (start == end) {
            return true;
        }

        if (start < end) {
            return normalizedTime >= start && normalizedTime < end;
        }
        return normalizedTime >= start || normalizedTime < end;
    }

    public boolean isDayTime(long time) {
        return isTimeInRange(time, getConfig().getLong("time.day-start", 0), getConfig().getLong("time.night-start", 13000));
    }

    private boolean getWorldKeepInventory(World world, boolean isDay, boolean globalKeepInvDay, boolean globalKeepInvNight) {
        String worldName = world.getName();
        String worldPath = "worlds.overrides." + worldName;

        if (getConfig().contains(worldPath)) {
            String timePath = isDay ? ".day" : ".night";
            if (getConfig().contains(worldPath + timePath)) {
                return getConfig().getBoolean(worldPath + timePath);
            }
        }

        // Fallback to global settings
        return isDay ? globalKeepInvDay : globalKeepInvNight;
    }

    public void debug(String message) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().log(Level.INFO, "[DEBUG] " + message);
        }
    }

    private void setupRuleManager() {
        ruleManager = new RuleManager(this);
        ruleManager.registerRule(new BypassPermissionRule());
        ruleManager.registerRule(new ProtectionRule());
        ruleManager.registerRule(new DeathCauseRule());
        ruleManager.registerRule(new WorldTimeRule());
    }

    public RuleManager getRuleManager() {
        return ruleManager;
    }

    public LandsHook getLandsHook() {
        return integrationManager.getLandsHook();
    }

    public GriefPreventionHook getGriefPreventionHook() {
        return integrationManager.getGriefPreventionHook();
    }

    public boolean isLandsEnabled() {
        return integrationManager.isLandsEnabled();
    }

    public boolean isGriefPreventionEnabled() {
        return integrationManager.isGriefPreventionEnabled();
    }

    public GravesXHook getGravesXHook() {
        return integrationManager.getGravesXHook();
    }

    public boolean isGravesXEnabled() {
        return integrationManager.isGravesXEnabled();
    }

    public AxGravesHook getAxGravesHook() {
        return integrationManager.getAxGravesHook();
    }

    public boolean isAxGravesEnabled() {
        return integrationManager.isAxGravesEnabled();
    }

    public MMOItemsHook getMMOItemsHook() {
        return integrationManager.getMMOItemsHook();
    }

    public boolean isMMOItemsEnabled() {
        return integrationManager.isMMOItemsEnabled();
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public StatsGUI getStatsGUI() {
        return statsGUI;
    }

    public PendingDeathManager getPendingDeathManager() {
        return pendingDeathManager;
    }

    public DeathConfirmGUI getDeathConfirmGUI() {
        return deathConfirmGUI;
    }

    public boolean isFolia() {
        return isFolia;
    }
}
