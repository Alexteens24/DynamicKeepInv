package xyz.superez.dynamickeepinv;

import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class StatsManager {
    private final DynamicKeepInvPlugin plugin;
    private Connection connection;
    private final ExecutorService asyncExecutor;
    private volatile boolean isShuttingDown = false;
    private final Object dbLock = new Object();

    // Cache for online players
    private final java.util.Map<UUID, PlayerStatsData> statsCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<UUID> knownPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // Cache for global stats
    private volatile int cachedGlobalDeathsSaved = 0;
    private volatile int cachedGlobalDeathsLost = 0;
    private volatile long lastGlobalStatsUpdate = 0;
    private static final long GLOBAL_STATS_CACHE_DURATION = 60000; // 1 minute

    public StatsManager(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
        this.asyncExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DynamicKeepInv-Stats");
            t.setDaemon(true);
            return t;
        });
        initDatabase();

        // Load stats for online players (in case of reload)
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            loadStats(p.getUniqueId());
        }

        // Initial load of global stats
        refreshGlobalStats();
    }

    private void initDatabase() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "stats.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            synchronized (dbLock) {
                connection = DriverManager.getConnection(url);

                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(
                        "CREATE TABLE IF NOT EXISTS player_stats (" +
                        "uuid TEXT PRIMARY KEY," +
                        "player_name TEXT," +
                        "deaths_saved INTEGER DEFAULT 0," +
                        "deaths_lost INTEGER DEFAULT 0," +
                        "total_deaths INTEGER DEFAULT 0," +
                        "last_death_time INTEGER DEFAULT 0," +
                        "last_death_reason TEXT DEFAULT 'none'," +
                        "last_death_saved INTEGER DEFAULT 0," +
                        "economy_total_paid REAL DEFAULT 0," +
                        "economy_payment_count INTEGER DEFAULT 0)"
                    );

                    stmt.execute(
                        "CREATE TABLE IF NOT EXISTS death_reasons (" +
                        "uuid TEXT," +
                        "reason TEXT," +
                        "saved_count INTEGER DEFAULT 0," +
                        "lost_count INTEGER DEFAULT 0," +
                        "PRIMARY KEY (uuid, reason))"
                    );
                }
            }

            plugin.getLogger().info("SQLite database initialized!");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize SQLite database!", e);
        }
    }

    public void close() {
        isShuttingDown = true;
        statsCache.clear();
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        try {
            synchronized (dbLock) {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not close database connection!", e);
        }
    }

    public java.util.concurrent.CompletableFuture<Void> loadStats(UUID uuid) {
        if (isShuttingDown) return java.util.concurrent.CompletableFuture.completedFuture(null);
        return java.util.concurrent.CompletableFuture.runAsync(() -> {
            PlayerStatsData data = fetchStats(uuid);
            statsCache.put(uuid, data);
        }, asyncExecutor);
    }

    public void unloadStats(UUID uuid) {
        statsCache.remove(uuid);
    }

    private PlayerStatsData fetchStats(UUID uuid) {
        PlayerStatsData data = new PlayerStatsData();
        if (!isConnectionValid()) return data;

        synchronized (dbLock) {
            try {
                // Fetch basic stats
                try (PreparedStatement pstmt = connection.prepareStatement("SELECT * FROM player_stats WHERE uuid = ?")) {
                    pstmt.setString(1, uuid.toString());
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            data.deathsSaved = rs.getInt("deaths_saved");
                            data.deathsLost = rs.getInt("deaths_lost");
                            data.totalDeaths = rs.getInt("total_deaths");
                            data.lastDeathTime = rs.getLong("last_death_time");
                            data.lastDeathReason = rs.getString("last_death_reason");
                            data.lastDeathSaved = rs.getInt("last_death_saved") == 1;
                            data.economyTotalPaid = rs.getDouble("economy_total_paid");
                            data.economyPaymentCount = rs.getInt("economy_payment_count");
                            knownPlayers.add(uuid);
                        }
                    }
                }

                // Fetch reason breakdown
                try (PreparedStatement pstmt = connection.prepareStatement("SELECT reason, saved_count, lost_count FROM death_reasons WHERE uuid = ?")) {
                    pstmt.setString(1, uuid.toString());
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            String reason = rs.getString("reason");
                            data.reasonSavedCount.put(reason, rs.getInt("saved_count"));
                            data.reasonLostCount.put(reason, rs.getInt("lost_count"));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error fetching stats for " + uuid, e);
            }
        }
        return data;
    }

    private void ensurePlayerExists(UUID uuid, String playerName) {
        if (!isConnectionValid()) return;
        if (knownPlayers.contains(uuid)) return;

        String sql = "INSERT OR IGNORE INTO player_stats (uuid, player_name) VALUES (?, ?)";
        synchronized (dbLock) {
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, playerName);
                pstmt.executeUpdate();
                knownPlayers.add(uuid);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error!", e);
            }
        }
    }

    private boolean isConnectionValid() {
        synchronized (dbLock) {
            try {
                return connection != null && !connection.isClosed();
            } catch (SQLException e) {
                return false;
            }
        }
    }

    public void recordDeathSaved(Player player, String reason) {
        if (isShuttingDown || !isConnectionValid()) return;
        final UUID uuid = player.getUniqueId();
        final String playerName = player.getName();
        final long time = System.currentTimeMillis();

        asyncExecutor.execute(() -> {
            if (isShuttingDown) return;
            ensurePlayerExists(uuid, playerName);

            String sql = "UPDATE player_stats SET " +
                         "deaths_saved = deaths_saved + 1, " +
                         "total_deaths = total_deaths + 1, " +
                         "last_death_time = ?, " +
                         "last_death_reason = ?, " +
                         "last_death_saved = 1, " +
                         "player_name = ? " +
                         "WHERE uuid = ?";

            synchronized (dbLock) {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setLong(1, time);
                    pstmt.setString(2, reason);
                    pstmt.setString(3, playerName);
                    pstmt.setString(4, uuid.toString());
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Database error!", e);
                }
            }

            updateReasonStats(uuid, reason, true);

            // Update cache in memory
            PlayerStatsData cached = statsCache.get(uuid);
            if (cached != null) {
                cached.incrementSaved(time, reason);
                cached.incrementReason(reason, true);
            }

            // Invalidate global stats cache so it updates next time (or optimistically update)
            synchronized(this) {
                cachedGlobalDeathsSaved++;
            }
        });
    }

    public void recordDeathLost(Player player, String reason) {
        if (isShuttingDown || !isConnectionValid()) return;
        final UUID uuid = player.getUniqueId();
        final String playerName = player.getName();
        final long time = System.currentTimeMillis();

        asyncExecutor.execute(() -> {
            if (isShuttingDown) return;
            ensurePlayerExists(uuid, playerName);

            String sql = "UPDATE player_stats SET " +
                         "deaths_lost = deaths_lost + 1, " +
                         "total_deaths = total_deaths + 1, " +
                         "last_death_time = ?, " +
                         "last_death_reason = ?, " +
                         "last_death_saved = 0, " +
                         "player_name = ? " +
                         "WHERE uuid = ?";

            synchronized (dbLock) {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setLong(1, time);
                    pstmt.setString(2, reason);
                    pstmt.setString(3, playerName);
                    pstmt.setString(4, uuid.toString());
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Database error!", e);
                }
            }

            updateReasonStats(uuid, reason, false);

            // Update cache in memory
            PlayerStatsData cached = statsCache.get(uuid);
            if (cached != null) {
                cached.incrementLost(time, reason);
                cached.incrementReason(reason, false);
            }

            // Invalidate global stats cache so it updates next time (or optimistically update)
            synchronized(this) {
                cachedGlobalDeathsLost++;
            }
        });
    }

    private void updateReasonStats(UUID uuid, String reason, boolean saved) {
        if (!isConnectionValid()) return;
        String insertSql = "INSERT OR IGNORE INTO death_reasons (uuid, reason) VALUES (?, ?)";
        synchronized (dbLock) {
            try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, reason);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error!", e);
            }
        }

        String updateSql = saved
            ? "UPDATE death_reasons SET saved_count = saved_count + 1 WHERE uuid = ? AND reason = ?"
            : "UPDATE death_reasons SET lost_count = lost_count + 1 WHERE uuid = ? AND reason = ?";

        synchronized (dbLock) {
            try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, reason);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error!", e);
            }
        }
    }

    public void recordEconomyPayment(Player player, double amount) {
        if (isShuttingDown || !isConnectionValid()) return;
        final UUID uuid = player.getUniqueId();
        final String playerName = player.getName();

        asyncExecutor.execute(() -> {
            if (isShuttingDown) return;
            ensurePlayerExists(uuid, playerName);

            String sql = "UPDATE player_stats SET " +
                         "economy_total_paid = economy_total_paid + ?, " +
                         "economy_payment_count = economy_payment_count + 1 " +
                         "WHERE uuid = ?";

            synchronized (dbLock) {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setDouble(1, amount);
                    pstmt.setString(2, uuid.toString());
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Database error!", e);
                }
            }

            // Update cache in memory
            PlayerStatsData cached = statsCache.get(uuid);
            if (cached != null) {
                cached.addEconomy(amount);
            }
        });
    }

    private PlayerStatsData getCachedOrLoad(UUID uuid) {
        PlayerStatsData data = statsCache.get(uuid);
        if (data != null) return data;

        // Synchronous fallback (should be avoided in main thread if possible, but required for PAPI/GUI if cache miss)
        // ideally PAPI should be tolerant or we just return 0.
        // For offline targets in GUI, we should probably fetch async.
        return fetchStats(uuid);
    }

    public int getDeathsSaved(UUID uuid) {
        return getCachedOrLoad(uuid).deathsSaved;
    }

    public int getDeathsLost(UUID uuid) {
        return getCachedOrLoad(uuid).deathsLost;
    }

    public int getTotalDeaths(UUID uuid) {
        return getCachedOrLoad(uuid).totalDeaths;
    }

    public long getLastDeathTime(UUID uuid) {
        return getCachedOrLoad(uuid).lastDeathTime;
    }

    public String getLastDeathReason(UUID uuid) {
        return getCachedOrLoad(uuid).lastDeathReason;
    }

    public boolean wasLastDeathSaved(UUID uuid) {
        return getCachedOrLoad(uuid).lastDeathSaved;
    }

    public double getTotalEconomyPaid(UUID uuid) {
        return getCachedOrLoad(uuid).economyTotalPaid;
    }

    public int getEconomyPaymentCount(UUID uuid) {
        return getCachedOrLoad(uuid).economyPaymentCount;
    }

    public int getReasonSavedCount(UUID uuid, String reason) {
        return getCachedOrLoad(uuid).reasonSavedCount.getOrDefault(reason, 0);
    }

    public int getReasonLostCount(UUID uuid, String reason) {
        return getCachedOrLoad(uuid).reasonLostCount.getOrDefault(reason, 0);
    }

    public double getSaveRate(UUID uuid) {
        int total = getTotalDeaths(uuid);
        if (total == 0) return 0.0;
        return (double) getDeathsSaved(uuid) / total * 100.0;
    }

    public void resetPlayerStats(UUID uuid) {
        if (!isConnectionValid()) return;
        synchronized (dbLock) {
            try {
                try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM player_stats WHERE uuid = ?")) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.executeUpdate();
                }
                try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM death_reasons WHERE uuid = ?")) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.executeUpdate();
                }
                knownPlayers.remove(uuid);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error!", e);
            }
        }
        unloadStats(uuid);
    }

    private void refreshGlobalStats() {
        if (isShuttingDown || !isConnectionValid()) return;

        asyncExecutor.execute(() -> {
            int saved = 0;
            int lost = 0;

            synchronized (dbLock) {
                try {
                     try (Statement stmt = connection.createStatement();
                          ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(deaths_saved), 0) FROM player_stats")) {
                         if (rs.next()) saved = rs.getInt(1);
                     }
                     try (Statement stmt = connection.createStatement();
                          ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(deaths_lost), 0) FROM player_stats")) {
                         if (rs.next()) lost = rs.getInt(1);
                     }
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Database error fetching global stats!", e);
                }
            }

            synchronized (this) {
                cachedGlobalDeathsSaved = saved;
                cachedGlobalDeathsLost = lost;
                lastGlobalStatsUpdate = System.currentTimeMillis();
            }
        });
    }

    public int getGlobalDeathsSaved() {
        if (System.currentTimeMillis() - lastGlobalStatsUpdate > GLOBAL_STATS_CACHE_DURATION) {
            refreshGlobalStats();
        }
        return cachedGlobalDeathsSaved;
    }

    public int getGlobalDeathsLost() {
         if (System.currentTimeMillis() - lastGlobalStatsUpdate > GLOBAL_STATS_CACHE_DURATION) {
            refreshGlobalStats();
        }
        return cachedGlobalDeathsLost;
    }

    public int getGlobalTotalDeaths() {
        return getGlobalDeathsSaved() + getGlobalDeathsLost();
    }

    public double getGlobalSaveRate() {
        int total = getGlobalTotalDeaths();
        if (total == 0) return 0.0;
        return (double) getGlobalDeathsSaved() / total * 100.0;
    }

    // Data class to hold player stats
    public static class PlayerStatsData {
        public volatile int deathsSaved = 0;
        public volatile int deathsLost = 0;
        public volatile int totalDeaths = 0;
        public volatile long lastDeathTime = 0;
        public volatile String lastDeathReason = "none";
        public volatile boolean lastDeathSaved = false;
        public volatile double economyTotalPaid = 0;
        public volatile int economyPaymentCount = 0;
        public final java.util.Map<String, Integer> reasonSavedCount = new java.util.concurrent.ConcurrentHashMap<>();
        public final java.util.Map<String, Integer> reasonLostCount = new java.util.concurrent.ConcurrentHashMap<>();

        public synchronized void incrementSaved(long time, String reason) {
            deathsSaved++;
            totalDeaths++;
            lastDeathTime = time;
            lastDeathReason = reason;
            lastDeathSaved = true;
        }

        public synchronized void incrementLost(long time, String reason) {
            deathsLost++;
            totalDeaths++;
            lastDeathTime = time;
            lastDeathReason = reason;
            lastDeathSaved = false;
        }

        public synchronized void addEconomy(double amount) {
            economyTotalPaid += amount;
            economyPaymentCount++;
        }

        public void incrementReason(String reason, boolean saved) {
            if (saved) {
                reasonSavedCount.merge(reason, 1, (oldVal, newVal) -> oldVal + newVal);
            } else {
                reasonLostCount.merge(reason, 1, (oldVal, newVal) -> oldVal + newVal);
            }
        }
    }
}
