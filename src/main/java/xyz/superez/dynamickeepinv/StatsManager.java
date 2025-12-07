package xyz.superez.dynamickeepinv;

import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class StatsManager {
    private final DynamicKeepInvPlugin plugin;
    private Connection connection;
    private final Object connectionLock = new Object();
    
    public StatsManager(DynamicKeepInvPlugin plugin) {
        this.plugin = plugin;
        initDatabase();
    }
    
    private void initDatabase() {
        synchronized (connectionLock) {
            try {
                File dbFile = new File(plugin.getDataFolder(), "stats.db");
                String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
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
            
            plugin.getLogger().info("SQLite database initialized!");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize SQLite database!", e);
        }
        }
    }
    
    public void close() {
        synchronized (connectionLock) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not close database connection!", e);
            }
        }
    }
    
    private void ensurePlayerExists(UUID uuid, String playerName) {
        String sql = "INSERT OR IGNORE INTO player_stats (uuid, player_name) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, playerName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database error!", e);
        }
    }
    
    public void recordDeathSaved(Player player, String reason) {
        UUID uuid = player.getUniqueId();
        
        synchronized (connectionLock) {
            ensurePlayerExists(uuid, player.getName());
            
            String sql = "UPDATE player_stats SET " +
                         "deaths_saved = deaths_saved + 1, " +
                         "total_deaths = total_deaths + 1, " +
                         "last_death_time = ?, " +
                         "last_death_reason = ?, " +
                         "last_death_saved = 1, " +
                         "player_name = ? " +
                         "WHERE uuid = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, System.currentTimeMillis());
                pstmt.setString(2, reason);
                pstmt.setString(3, player.getName());
                pstmt.setString(4, uuid.toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error!", e);
            }
            
            updateReasonStatsInternal(uuid, reason, true);
        }
    }
    
    public void recordDeathLost(Player player, String reason) {
        UUID uuid = player.getUniqueId();
        
        synchronized (connectionLock) {
            ensurePlayerExists(uuid, player.getName());
            
            String sql = "UPDATE player_stats SET " +
                         "deaths_lost = deaths_lost + 1, " +
                         "total_deaths = total_deaths + 1, " +
                         "last_death_time = ?, " +
                         "last_death_reason = ?, " +
                         "last_death_saved = 0, " +
                         "player_name = ? " +
                         "WHERE uuid = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, System.currentTimeMillis());
                pstmt.setString(2, reason);
                pstmt.setString(3, player.getName());
                pstmt.setString(4, uuid.toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error!", e);
            }
            
            updateReasonStatsInternal(uuid, reason, false);
        }
    }
    
    /**
     * Internal method to update death reason statistics.
     * MUST be called from within a synchronized(connectionLock) block.
     * 
     * @param uuid Player UUID
     * @param reason Death reason
     * @param saved Whether the death was saved or lost
     */
    private void updateReasonStatsInternal(UUID uuid, String reason, boolean saved) {
        String insertSql = "INSERT OR IGNORE INTO death_reasons (uuid, reason) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, reason);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database error!", e);
        }
        
        String updateSql = saved 
            ? "UPDATE death_reasons SET saved_count = saved_count + 1 WHERE uuid = ? AND reason = ?"
            : "UPDATE death_reasons SET lost_count = lost_count + 1 WHERE uuid = ? AND reason = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, reason);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database error!", e);
        }
    }
    
    public void recordEconomyPayment(Player player, double amount) {
        UUID uuid = player.getUniqueId();
        
        synchronized (connectionLock) {
            ensurePlayerExists(uuid, player.getName());
            
            String sql = "UPDATE player_stats SET " +
                         "economy_total_paid = economy_total_paid + ?, " +
                         "economy_payment_count = economy_payment_count + 1 " +
                         "WHERE uuid = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setDouble(1, amount);
                pstmt.setString(2, uuid.toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error!", e);
            }
        }
    }
    
    public int getDeathsSaved(UUID uuid) {
        return getIntStat(uuid, "deaths_saved");
    }
    
    public int getDeathsLost(UUID uuid) {
        return getIntStat(uuid, "deaths_lost");
    }
    
    public int getTotalDeaths(UUID uuid) {
        return getIntStat(uuid, "total_deaths");
    }
    
    public long getLastDeathTime(UUID uuid) {
        synchronized (connectionLock) {
            String sql = "SELECT last_death_time FROM player_stats WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("last_death_time");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error!", e);
            }
            return 0;
        }
    }
    
    public String getLastDeathReason(UUID uuid) {
        synchronized (connectionLock) {
            String sql = "SELECT last_death_reason FROM player_stats WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("last_death_reason");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error!", e);
            }
            return "none";
        }
    }
    
    public boolean wasLastDeathSaved(UUID uuid) {
        return getIntStat(uuid, "last_death_saved") == 1;
    }
    
    public double getTotalEconomyPaid(UUID uuid) {
        synchronized (connectionLock) {
            String sql = "SELECT economy_total_paid FROM player_stats WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("economy_total_paid");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error!", e);
            }
            return 0;
        }
    }
    
    public int getEconomyPaymentCount(UUID uuid) {
        return getIntStat(uuid, "economy_payment_count");
    }
    
    public int getReasonSavedCount(UUID uuid, String reason) {
        synchronized (connectionLock) {
            String sql = "SELECT saved_count FROM death_reasons WHERE uuid = ? AND reason = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, reason);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("saved_count");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error!", e);
            }
            return 0;
        }
    }
    
    public int getReasonLostCount(UUID uuid, String reason) {
        synchronized (connectionLock) {
            String sql = "SELECT lost_count FROM death_reasons WHERE uuid = ? AND reason = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, reason);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("lost_count");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error!", e);
            }
            return 0;
        }
    }
    
    private int getIntStat(UUID uuid, String column) {
        synchronized (connectionLock) {
            // Whitelist allowed column names to prevent SQL injection
            if (!isValidColumn(column)) {
                plugin.getLogger().log(Level.WARNING, "Invalid column name requested: " + column);
                return 0;
            }
            
            String sql = "SELECT " + column + " FROM player_stats WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(column);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error!", e);
            }
            return 0;
        }
    }
    
    private boolean isValidColumn(String column) {
        // Whitelist of valid column names
        return column.equals("deaths_saved") || 
               column.equals("deaths_lost") || 
               column.equals("total_deaths") ||
               column.equals("last_death_saved") ||
               column.equals("economy_payment_count");
    }
    
    public double getSaveRate(UUID uuid) {
        int total = getTotalDeaths(uuid);
        if (total == 0) return 0.0;
        return (double) getDeathsSaved(uuid) / total * 100.0;
    }
    
    public void resetPlayerStats(UUID uuid) {
        synchronized (connectionLock) {
            try {
                try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM player_stats WHERE uuid = ?")) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.executeUpdate();
                }
                try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM death_reasons WHERE uuid = ?")) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error!", e);
            }
        }
    }
    
    public int getGlobalDeathsSaved() {
        synchronized (connectionLock) {
            return getGlobalDeathsSavedInternal();
        }
    }
    
    public int getGlobalDeathsLost() {
        synchronized (connectionLock) {
            return getGlobalDeathsLostInternal();
        }
    }
    
    public int getGlobalTotalDeaths() {
        synchronized (connectionLock) {
            return getGlobalDeathsSavedInternal() + getGlobalDeathsLostInternal();
        }
    }
    
    public double getGlobalSaveRate() {
        synchronized (connectionLock) {
            int saved = getGlobalDeathsSavedInternal();
            int lost = getGlobalDeathsLostInternal();
            int total = saved + lost;
            if (total == 0) return 0.0;
            return (double) saved / total * 100.0;
        }
    }
    
    /**
     * Internal method to get global deaths saved count.
     * MUST be called from within a synchronized(connectionLock) block.
     */
    private int getGlobalDeathsSavedInternal() {
        String sql = "SELECT COALESCE(SUM(deaths_saved), 0) FROM player_stats";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database error!", e);
        }
        return 0;
    }
    
    /**
     * Internal method to get global deaths lost count.
     * MUST be called from within a synchronized(connectionLock) block.
     */
    private int getGlobalDeathsLostInternal() {
        String sql = "SELECT COALESCE(SUM(deaths_lost), 0) FROM player_stats";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database error!", e);
        }
        return 0;
    }
}
