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
    private volatile int economyRetryCount = 0;
    private static final int CONFIG_VERSION = 5;

    private LandsHook landsHook;
    private GriefPreventionHook griefPreventionHook;
    private GravesXHook gravesXHook;
    private AxGravesHook axGravesHook;
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
        checkConfigVersion();
        loadMessages();
        reloadIntegrations();
        setupRuleManager();

        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);

        reloadStatsSystem();

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

    private void checkConfigVersion() {
        int currentVersion = getConfig().getInt("config-version", 0);
        if (currentVersion < CONFIG_VERSION) {
            getLogger().warning("Config outdated (v" + currentVersion + " -> v" + CONFIG_VERSION + "). Some new options may be missing.");
            getLogger().warning("Consider regenerating config.yml or adding missing options manually.");
            getConfig().set("config-version", CONFIG_VERSION);
            saveConfig();
        }
    }

    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        lang = messagesConfig.getString("language", "vi");
    }

    private void reloadIntegrations() {
        boolean economyEnabled = getConfig().getBoolean("economy.enabled", false);
        synchronized (economyLock) {
            economyManager = null;
            nextEconomyRetryTimeMs.set(0L);
            economyRetryCount = 0; // Reset retry counter on reload
        }
        if (economyEnabled) {
            getEconomyManager();
        }

        if (getConfig().getBoolean("integrations.lands.enabled", false)) {
            landsHook = new LandsHook(this);
        } else {
            landsHook = null;
        }

        if (getConfig().getBoolean("integrations.griefprevention.enabled", false)) {
            griefPreventionHook = new GriefPreventionHook(this);
        } else {
            griefPreventionHook = null;
        }

        if (getConfig().getBoolean("integrations.gravesx.enabled", false)) {
            if (Bukkit.getPluginManager().getPlugin("GravesX") != null) {
                gravesXHook = new GravesXHook(this);
                if (!gravesXHook.setup()) {
                    gravesXHook = null; // Failed to setup
                }
            } else {
                getLogger().warning("GravesX integration enabled in config, but GravesX plugin not found!");
                gravesXHook = null;
            }
        } else {
            gravesXHook = null;
        }

        if (getConfig().getBoolean("integrations.axgraves.enabled", false)) {
            if (Bukkit.getPluginManager().getPlugin("AxGraves") != null) {
                axGravesHook = new AxGravesHook(this);
                if (!axGravesHook.setup()) {
                    axGravesHook = null; // Failed to setup
                }
            } else {
                getLogger().warning("AxGraves integration enabled in config, but AxGraves plugin not found!");
                axGravesHook = null;
            }
        } else {
            axGravesHook = null;
        }
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

    private void startChecking() {
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

    private void stopChecking(boolean resetGameRules) {
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
        if (args.length > 0 && args[0].equalsIgnoreCase("stats")) {
            return handleStatsCommand(sender, args);
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            return handleConfirmCommand(sender);
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("autopay")) {
            return handleAutoPayCommand(sender);
        }

        if (!sender.hasPermission("dynamickeepinv.admin")) {
            sender.sendMessage(parseMessage(getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(parseMessage(getMessage("help.header")));
            sender.sendMessage(parseMessage(getMessage("help.status")));
            sender.sendMessage(parseMessage(getMessage("help.reload")));
            sender.sendMessage(parseMessage(getMessage("help.enable")));
            sender.sendMessage(parseMessage(getMessage("help.disable")));
            sender.sendMessage(parseMessage(getMessage("help.toggle")));
            sender.sendMessage(parseMessage(getMessage("help.stats")));
            sender.sendMessage(parseMessage(getMessage("help.autopay")));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status":
                showStatus(sender);
                break;

            case "reload":
                reloadConfig();
                loadMessages();
                reloadIntegrations();
                reloadPendingDeathManager();
                reloadStatsSystem();
                if (getConfig().getBoolean("enabled", true)) {
                    startChecking();
                } else {
                    stopChecking(true);
                }
                sender.sendMessage(parseMessage(getMessage("commands.reload")));
                break;

            case "enable":
                getConfig().set("enabled", true);
                saveConfig();
                startChecking();
                sender.sendMessage(parseMessage(getMessage("commands.enabled")));
                break;

            case "disable":
                getConfig().set("enabled", false);
                saveConfig();
                stopChecking(true);
                sender.sendMessage(parseMessage(getMessage("commands.disabled")));
                break;

            case "toggle":
                boolean newState = !getConfig().getBoolean("enabled", true);
                getConfig().set("enabled", newState);
                saveConfig();

                if (newState) {
                    startChecking();
                    sender.sendMessage(parseMessage(getMessage("commands.enabled")));
                } else {
                    stopChecking(true);
                    sender.sendMessage(parseMessage(getMessage("commands.disabled")));
                }
                break;

            default:
                sender.sendMessage(parseMessage(getMessage("commands.unknown")));
        }

        return true;
    }

    private boolean handleStatsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(parseMessage("&cThis command can only be used by players!"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("dynamickeepinv.stats")) {
            sender.sendMessage(parseMessage(getMessage("no-permission")));
            return true;
        }

        if (statsGUI == null || statsManager == null) {
            sender.sendMessage(parseMessage(getMessage("stats.disabled")));
            return true;
        }

        if (args.length >= 2) {
            if (player.hasPermission("dynamickeepinv.stats.others")) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    statsGUI.openStats(player, target.getUniqueId(), target.getName());
                } else {
                    org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(args[1]);
                    if (offlineTarget.hasPlayedBefore() || offlineTarget.isOnline()) {
                        String displayName = offlineTarget.getName() != null ? offlineTarget.getName() : args[1];
                        statsGUI.openStats(player, offlineTarget.getUniqueId(), displayName);
                    } else {
                        sender.sendMessage(parseMessage(getMessage("stats.player-not-found").replace("{player}", args[1])));
                    }
                }
            } else {
                sender.sendMessage(parseMessage(getMessage("no-permission")));
            }
        } else {
            statsGUI.openStats(player);
        }

        return true;
    }

    private boolean handleConfirmCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(parseMessage("&cThis command can only be used by players!"));
            return true;
        }

        // Check if GUI mode is enabled
        if (!getConfig().getBoolean("economy.enabled", false)
            || !"gui".equalsIgnoreCase(getConfig().getString("economy.mode", "charge-to-keep"))) {
            sender.sendMessage(parseMessage("&cDeath confirmation GUI is not enabled! Set economy mode to 'gui' in config."));
            return true;
        }

        if (pendingDeathManager == null || deathConfirmGUI == null) {
            sender.sendMessage(parseMessage("&cDeath confirmation GUI is not enabled!"));
            return true;
        }

        PendingDeath pending = pendingDeathManager.getPendingDeath(player.getUniqueId());
        if (pending == null || pending.isProcessed()) {
            sender.sendMessage(parseMessage("&eYou don't have a pending death to confirm."));
            return true;
        }

        deathConfirmGUI.openGUI(player, pending);
        return true;
    }

    private boolean handleAutoPayCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(parseMessage("&cThis command can only be used by players!"));
            return true;
        }

        // Check if GUI mode is enabled
        if (!getConfig().getBoolean("economy.enabled", false)
            || !"gui".equalsIgnoreCase(getConfig().getString("economy.mode", "charge-to-keep"))) {
            sender.sendMessage(parseMessage("&cAuto-pay requires GUI economy mode! Set economy mode to 'gui' in config."));
            return true;
        }

        if (pendingDeathManager == null) {
            sender.sendMessage(parseMessage("&cDeath confirmation GUI is not enabled!"));
            return true;
        }

        boolean newState = pendingDeathManager.toggleAutoPay(player.getUniqueId());
        String msgKey = newState ? "economy.gui.auto-pay-enabled" : "economy.gui.auto-pay-disabled";
        String msg = getMessage(msgKey);
        player.sendMessage(parseMessage(msg));
        return true;
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

    private void reloadStatsSystem() {
        cleanupStatsSystem();

        if (getConfig().getBoolean("stats.enabled", true)) {
            statsManager = new StatsManager(this);
            statsGUI = new StatsGUI(this);
            statsListener = new StatsListener(this);
            getServer().getPluginManager().registerEvents(statsListener, this);
            getLogger().info("Stats system enabled with SQLite database!");
        }
    }

    private void reloadPendingDeathManager() {
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

    private void showStatus(CommandSender sender) {
        sender.sendMessage(parseMessage(getMessage("status.header")));
        sender.sendMessage(parseMessage(getMessage("status.enabled")
                .replace("{value}", String.valueOf(getConfig().getBoolean("enabled", true)))));
        sender.sendMessage(parseMessage(getMessage("status.keep-inv-day")
                .replace("{value}", String.valueOf(getConfig().getBoolean("rules.day.keep-items", true)))));
        sender.sendMessage(parseMessage(getMessage("status.keep-inv-night")
                .replace("{value}", String.valueOf(getConfig().getBoolean("rules.night.keep-items", false)))));
        sender.sendMessage(parseMessage(getMessage("status.check-interval")
                .replace("{value}", String.valueOf(getConfig().getInt("check-interval", 100)))));

        sender.sendMessage(parseMessage(getMessage("status.world-header")));
        long dayStart = getConfig().getLong("time.day-start", 0);
        long nightStart = getConfig().getLong("time.night-start", 13000);
        for (World world : Bukkit.getWorlds()) {
            long time = world.getTime();
            boolean isDay = isTimeInRange(time, dayStart, nightStart);
            Boolean keepInv = world.getGameRuleValue(GameRule.KEEP_INVENTORY);

            String period = isDay ? getMessage("status.day") : getMessage("status.night");
            String status = (keepInv != null && keepInv) ? getMessage("status.on") : getMessage("status.off");

            String worldInfo = getMessage("status.world-info")
                    .replace("{world}", world.getName())
                    .replace("{time}", String.valueOf(time))
                    .replace("{period}", period)
                    .replace("{status}", status);

            sender.sendMessage(parseMessage(worldInfo));
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
            // Note: The structure in new config for overrides is:
            // worlds:
            //   overrides:
            //     world_nether:
            //       keep-inventory-day: false
            String timePath = isDay ? ".keep-inventory-day" : ".keep-inventory-night";
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
        return landsHook;
    }

    public GriefPreventionHook getGriefPreventionHook() {
        return griefPreventionHook;
    }

    public boolean isLandsEnabled() {
        return landsHook != null && landsHook.isAvailable();
    }

    public boolean isGriefPreventionEnabled() {
        return griefPreventionHook != null && griefPreventionHook.isAvailable();
    }

    public GravesXHook getGravesXHook() {
        return gravesXHook;
    }

    public boolean isGravesXEnabled() {
        return gravesXHook != null && gravesXHook.isEnabled();
    }

    public AxGravesHook getAxGravesHook() {
        return axGravesHook;
    }

    public boolean isAxGravesEnabled() {
        return axGravesHook != null && axGravesHook.isEnabled();
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
