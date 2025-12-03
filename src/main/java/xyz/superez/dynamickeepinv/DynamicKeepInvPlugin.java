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
    private static final long ECONOMY_RETRY_DELAY_MS = 10000;
    
    private volatile EconomyManager economyManager;
    private final Object economyLock = new Object();
    private final AtomicLong nextEconomyRetryTimeMs = new AtomicLong(0L);
    private static final int CONFIG_VERSION = 2;
    
    private LandsHook landsHook;
    private GriefPreventionHook griefPreventionHook;

    @Override
    public void onEnable() {
        detectFolia();
        saveDefaultConfig();
        checkConfigVersion();
        loadMessages();
        reloadIntegrations();

        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        
        getLogger().info("DynamicKeepInv is starting... (Platform: " + (isFolia ? "Folia" : "Paper/Spigot") + ")");
        
        if (getConfig().getBoolean("enabled", true)) {
            startChecking();
            getLogger().info("DynamicKeepInv enabled! Keep inventory will change based on time.");
        } else {
            getLogger().info("DynamicKeepInv is disabled in config.");
        }
    }
    
    @Override
    public void onDisable() {
        isShuttingDown = true;
        stopChecking(true);
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
        if (!getConfig().getBoolean("advanced.economy.enabled", false)) {
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

        if (!localRef.isEnabled()) {
            long now = System.currentTimeMillis();
            if (now >= nextEconomyRetryTimeMs.get()) {
                synchronized (economyLock) {
                    if (now >= nextEconomyRetryTimeMs.get()) {
                        nextEconomyRetryTimeMs.set(now + ECONOMY_RETRY_DELAY_MS);
                        localRef.setupEconomy();
                    }
                }
            }
        }

        return localRef;
    }

    public boolean isWorldEnabled(World world) {
        List<String> enabledWorlds = getConfig().getStringList("enabled-worlds");
        if (enabledWorlds == null || enabledWorlds.isEmpty()) {
            return true;
        }
        return enabledWorlds.contains(world.getName());
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
        boolean economyEnabled = getConfig().getBoolean("advanced.economy.enabled", false);
        synchronized (economyLock) {
            economyManager = null;
            nextEconomyRetryTimeMs.set(0L);
        }
        if (economyEnabled) {
            getEconomyManager();
        }

        if (getConfig().getBoolean("advanced.protection.lands.enabled", false)) {
            landsHook = new LandsHook(this);
        } else {
            landsHook = null;
        }

        if (getConfig().getBoolean("advanced.protection.griefprevention.enabled", false)) {
            griefPreventionHook = new GriefPreventionHook(this);
        } else {
            griefPreventionHook = null;
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
        List<String> enabledWorlds = getConfig().getStringList("enabled-worlds");
        boolean keepInvDay = getConfig().getBoolean("keep-inventory-day", true);
        boolean keepInvNight = getConfig().getBoolean("keep-inventory-night", false);
        long dayStart = getConfig().getLong("day-start", 0);
        long nightStart = getConfig().getLong("night-start", 13000);
        
        long dayTrigger = getConfig().getLong("gamerule-change.day-trigger", -1);
        long nightTrigger = getConfig().getLong("gamerule-change.night-trigger", -1);
        if (dayTrigger < 0) dayTrigger = dayStart;
        if (nightTrigger < 0) nightTrigger = nightStart;

        for (World world : Bukkit.getWorlds()) {
            if (shouldSkipWorld(world, enabledWorlds)) {
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

    private boolean shouldSkipWorld(World world, List<String> enabledWorlds) {
        return !enabledWorlds.isEmpty() && !enabledWorlds.contains(world.getName());
    }

    private void processWorld(World world, boolean keepInvDay, boolean keepInvNight,
                              long dayStart, long nightStart, long dayTrigger, long nightTrigger) {
        if (isShuttingDown) {
            return;
        }
        
        long time = world.getTime();
        boolean isDay = isTimeInRange(time, dayStart, nightStart);
        boolean shouldTriggerDay = isTimeInRange(time, dayTrigger, nightTrigger);
        boolean shouldKeepInv = getWorldKeepInventory(world, shouldTriggerDay, keepInvDay, keepInvNight);

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
        if (!getConfig().getBoolean("broadcast.enabled", true)) return false;
        if (isDay && !getConfig().getBoolean("broadcast.day-change", true)) return false;
        if (!isDay && !getConfig().getBoolean("broadcast.night-change", true)) return false;
        return true;
    }

    private void sendNotifications(World world, String message, boolean isDay) {
        Component component = parseMessage(message);
        boolean chat = getConfig().getBoolean("broadcast.chat", true);
        boolean actionBar = getConfig().getBoolean("broadcast.action-bar", false);
        boolean titleEnabled = getConfig().getBoolean("broadcast.title", false);
        boolean soundEnabled = getConfig().getBoolean("broadcast.sound.enabled", false);
        String soundName = isDay ? getConfig().getString("broadcast.sound.day") : getConfig().getString("broadcast.sound.night");
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
    
    private void showStatus(CommandSender sender) {
        sender.sendMessage(parseMessage(getMessage("status.header")));
        sender.sendMessage(parseMessage(getMessage("status.enabled")
                .replace("{value}", String.valueOf(getConfig().getBoolean("enabled", true)))));
        sender.sendMessage(parseMessage(getMessage("status.keep-inv-day")
                .replace("{value}", String.valueOf(getConfig().getBoolean("keep-inventory-day", true)))));
        sender.sendMessage(parseMessage(getMessage("status.keep-inv-night")
                .replace("{value}", String.valueOf(getConfig().getBoolean("keep-inventory-night", false)))));
        sender.sendMessage(parseMessage(getMessage("status.check-interval")
                .replace("{value}", String.valueOf(getConfig().getInt("check-interval", 100)))));
        
        sender.sendMessage(parseMessage(getMessage("status.world-header")));
        long dayStart = getConfig().getLong("day-start", 0);
        long nightStart = getConfig().getLong("night-start", 13000);
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
        return isTimeInRange(time, getConfig().getLong("day-start", 0), getConfig().getLong("night-start", 13000));
    }
    
    private boolean getWorldKeepInventory(World world, boolean isDay, boolean globalKeepInvDay, boolean globalKeepInvNight) {
        String worldName = world.getName();
        String worldPath = "world-settings." + worldName;
        
        if (getConfig().contains(worldPath)) {
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
}
