# DynamicKeepInv - Bug Report & Fixes

## Summary
This report documents all bugs found and fixed in the DynamicKeepInv plugin during the security and code quality audit.

## Bugs Found and Fixed

### 1. **SQL Injection Vulnerability** (CRITICAL)
**Location:** `StatsManager.java` - `getIntStat()` method

**Issue:** The column name was being concatenated directly into the SQL query without validation, creating a potential SQL injection vulnerability.

```java
// Before:
String sql = "SELECT " + column + " FROM player_stats WHERE uuid = ?";
```

**Fix:** Added column name whitelist validation before constructing the query.

```java
// After:
private boolean isValidColumn(String column) {
    return column.equals("deaths_saved") || 
           column.equals("deaths_lost") || 
           column.equals("total_deaths") ||
           column.equals("last_death_saved") ||
           column.equals("economy_payment_count");
}
```

**Severity:** Critical  
**Impact:** Could allow malicious actors to execute arbitrary SQL commands if they could control the column parameter.

---

### 2. **Resource Leaks** (HIGH)
**Location:** `StatsManager.java` - Multiple query methods

**Issue:** `ResultSet` objects were not being properly closed in try-with-resources statements, leading to potential resource leaks.

```java
// Before:
try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
    ResultSet rs = pstmt.executeQuery();
    if (rs.next()) {
        return rs.getLong("last_death_time");
    }
}
```

**Fix:** Wrapped `ResultSet` in try-with-resources to ensure proper cleanup.

```java
// After:
try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
    pstmt.setString(1, uuid.toString());
    try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
            return rs.getLong("last_death_time");
        }
    }
}
```

**Affected Methods:**
- `getLastDeathTime()`
- `getLastDeathReason()`
- `getTotalEconomyPaid()`
- `getReasonSavedCount()`
- `getReasonLostCount()`
- `getIntStat()`

**Severity:** High  
**Impact:** Resource exhaustion over time, especially on servers with many players.

---

### 3. **Missing Economy Payment Tracking** (MEDIUM)
**Location:** `DeathListener.java` - `onPlayerDeath()` method

**Issue:** When players successfully paid to keep their inventory, the payment was not being recorded in the statistics database.

```java
// Before:
String msg = plugin.getMessage("economy.paid")
    .replace("{amount}", eco.format(cost));
player.sendMessage(plugin.parseMessage(msg));
if ("charge-to-bypass".equalsIgnoreCase(mode)) {
    // ... keep items logic
}
```

**Fix:** Added statistics tracking for economy payments.

```java
// After:
String msg = plugin.getMessage("economy.paid")
    .replace("{amount}", eco.format(cost));
player.sendMessage(plugin.parseMessage(msg));

// Track economy payment in stats
StatsManager stats = plugin.getStatsManager();
if (stats != null) {
    stats.recordEconomyPayment(player, cost);
}

if ("charge-to-bypass".equalsIgnoreCase(mode)) {
    // ... keep items logic
}
```

**Severity:** Medium  
**Impact:** Economy statistics were incomplete and inaccurate.

---

### 4. **Thread Safety Issues** (HIGH)
**Location:** `StatsManager.java` - All database operations

**Issue:** SQLite connection is not thread-safe, but multiple threads (especially in Folia) could access it concurrently during death events, causing race conditions and potential data corruption.

**Fix:** Added comprehensive synchronization using a dedicated lock object for all database operations.

```java
private final Object connectionLock = new Object();

public void recordDeathSaved(Player player, String reason) {
    UUID uuid = player.getUniqueId();
    
    synchronized (connectionLock) {
        ensurePlayerExists(uuid, player.getName());
        // ... database operations
    }
}
```

**Affected Methods:** All public methods that access the database connection

**Severity:** High  
**Impact:** Data corruption, inconsistent statistics, potential crashes in high-concurrency scenarios (especially on Folia servers).

---

### 5. **Potential NullPointerException** (MEDIUM)
**Location:** `DeathListener.java` - `checkProtectionPlugins()` method

**Issue:** The protection hook objects could be null even when `isLandsEnabled()` or `isGriefPreventionEnabled()` returned true, due to race conditions during plugin reload or initialization failures.

```java
// Before:
if (plugin.isLandsEnabled() && ...) {
    LandsHook lands = plugin.getLandsHook();
    boolean inLand = lands.isInLand(location); // NPE possible here
}
```

**Fix:** Added defensive null checks.

```java
// After:
if (plugin.isLandsEnabled() && ...) {
    LandsHook lands = plugin.getLandsHook();
    if (lands == null) {
        plugin.debug("Lands hook is null despite being enabled.");
        return new ProtectionResult(false, false, false, null);
    }
    boolean inLand = lands.isInLand(location);
}
```

**Severity:** Medium  
**Impact:** Could cause plugin crashes when protection plugins are disabled/reloaded during runtime.

---

### 6. **StringIndexOutOfBoundsException Risk** (LOW)
**Location:** `StatsGUI.java` - `formatReason()` method

**Issue:** Empty string reasons would cause `StringIndexOutOfBoundsException` when calling `substring(0, 1)`.

```java
// Before:
private String formatReason(String reason) {
    if (reason == null || reason.equals("none")) return "None";
    return reason.substring(0, 1).toUpperCase() + reason.substring(1).replace("-", " ");
}
```

**Fix:** Added empty string check.

```java
// After:
private String formatReason(String reason) {
    if (reason == null || reason.equals("none") || reason.isEmpty()) return "None";
    return reason.substring(0, 1).toUpperCase() + reason.substring(1).replace("-", " ");
}
```

**Severity:** Low  
**Impact:** Could cause GUI display errors if invalid reason data existed in the database.

---

## Code Quality Improvements

### Synchronization Pattern
All database operations now follow a consistent synchronization pattern:
1. Top-level methods acquire the `connectionLock`
2. Internal helper methods run without synchronization (called within synchronized blocks)
3. Clear separation between `updateReasonStatsInternal()` (internal) and public methods

### Resource Management
All database queries now properly use try-with-resources for both `PreparedStatement` and `ResultSet` objects, ensuring proper cleanup even in exception scenarios.

## Security Scan Results
**CodeQL Analysis:** 0 vulnerabilities found after fixes

## Testing Recommendations

1. **Load Testing:** Test with multiple concurrent player deaths on a Folia server
2. **Protection Plugin Integration:** Test with various protection plugin states (enabled/disabled/reloaded)
3. **Economy Integration:** Verify payment tracking with various economy providers
4. **Database Stress Test:** Test statistics system under high load
5. **Edge Cases:** Test with empty/null values in database fields

## Conclusion

All identified bugs have been fixed. The plugin now has:
- ✅ No SQL injection vulnerabilities
- ✅ Proper resource management
- ✅ Thread-safe database operations
- ✅ Defensive null checks
- ✅ Complete economy statistics tracking
- ✅ Protection against edge case exceptions

The plugin is now safer, more stable, and ready for production use on high-concurrency servers including Folia.
