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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

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
    private static final long BROADCAST_COOLDOWN = 2000;
    
    @Override
    public void onEnable() {
        detectFolia();
        saveDefaultConfig();
        loadMessages();
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        
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
    
    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        lang = messagesConfig.getString("language", "vi");
    }
    
    private String getMessage(String path) {
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
    
    private Component parseMessage(String message) {
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

        for (World world : Bukkit.getWorlds()) {
            if (shouldSkipWorld(world, enabledWorlds)) {
                continue;
            }

            if (isFolia) {
                final World currentWorld = world;
                Bukkit.getRegionScheduler().execute(this, currentWorld.getSpawnLocation(), () ->
                        processWorld(currentWorld, keepInvDay, keepInvNight, dayStart, nightStart));
            } else {
                processWorld(world, keepInvDay, keepInvNight, dayStart, nightStart);
            }
        }
    }

    private boolean shouldSkipWorld(World world, List<String> enabledWorlds) {
        return !enabledWorlds.isEmpty() && !enabledWorlds.contains(world.getName());
    }

    private void processWorld(World world, boolean keepInvDay, boolean keepInvNight,
                              long dayStart, long nightStart) {
        if (isShuttingDown) {
            return;
        }
        
        long time = world.getTime();
        boolean isDay = time >= dayStart && time < nightStart;
        boolean shouldKeepInv = isDay ? keepInvDay : keepInvNight;

        Boolean currentKeepInv = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
        if (currentKeepInv == null || currentKeepInv != shouldKeepInv) {
            rememberOriginalGameRule(world, currentKeepInv);
            
            debug(String.format("Updating World: %s, Time: %d, IsDay: %s, KeepInv: %s -> %s",
                    world.getName(), time, isDay, currentKeepInv, shouldKeepInv));
            
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
                            Component broadcastMsg = parseMessage(message);
                            Bukkit.broadcast(broadcastMsg);
                        }
                    }
                }
            }
        }

        lastWasDayMap.put(world.getName(), isDay);
    }

    private void rememberOriginalGameRule(World world, Boolean currentValue) {
        originalKeepInvValues.computeIfAbsent(world.getName(), k -> currentValue);
    }

    private void restoreOriginalGamerules() {
        for (World world : Bukkit.getWorlds()) {
            String worldName = world.getName();
            if (!originalKeepInvValues.containsKey(worldName)) {
                continue;
            }

            Boolean originalValue = originalKeepInvValues.get(worldName);
            if (originalValue != null) {
                if (isFolia && !isShuttingDown) {
                    Bukkit.getRegionScheduler().execute(this, world.getSpawnLocation(), () -> {
                        world.setGameRule(GameRule.KEEP_INVENTORY, originalValue);
                        getLogger().info("Restored keepInventory for " + worldName + " to " + originalValue);
                    });
                } else {
                    world.setGameRule(GameRule.KEEP_INVENTORY, originalValue);
                    getLogger().info("Restored keepInventory for " + worldName + " to " + originalValue);
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
        for (World world : Bukkit.getWorlds()) {
            long time = world.getTime();
            boolean isDay = time >= getConfig().getLong("day-start", 0) && 
                           time < getConfig().getLong("night-start", 13000);
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
    
    private boolean shouldBroadcast(boolean isDay) {
        if (!getConfig().getBoolean("broadcast.enabled", true)) {
            return false;
        }
        return isDay ? getConfig().getBoolean("broadcast.day-change", true) 
                     : getConfig().getBoolean("broadcast.night-change", true);
    }
    
    private void debug(String message) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().log(Level.INFO, "[DEBUG] " + message);
        }
    }
}
