# 🐛 HƯỚNG DẪN DEBUG CHI TIẾT

## 1. DEBUG VỚI PRINT/LOG

### Cách 1: Sử dụng Logger (Recommended)
```java
// Trong plugin code
getLogger().info("Normal message");
getLogger().warning("Warning message");
getLogger().severe("Error message");

// Ví dụ debug biến
getLogger().info("Current time: " + world.getTime());
getLogger().info("Keep Inventory: " + keepInv);
```

### Cách 2: System.out.println (Chỉ dùng khi test)
```java
System.out.println("Debug: value = " + value);
```

### Cách 3: Debug Mode trong Config
```yaml
# config.yml
debug: true
```

Sau đó trong code:
```java
private void debug(String message) {
    if (getConfig().getBoolean("debug", false)) {
        getLogger().info("[DEBUG] " + message);
    }
}
```

## 2. DEBUG VỚI BREAKPOINTS (VS Code)

### Bước 1: Cài Extension
1. Mở VS Code
2. Cài đặt extension: **"Debugger for Java"**
3. Cài đặt extension: **"Test Runner for Java"**

### Bước 2: Đặt Breakpoint
1. Mở file `DynamicKeepInvPlugin.java`
2. Click vào bên trái số dòng → xuất hiện chấm đỏ
3. Ví dụ: đặt breakpoint tại dòng 50 trong hàm `checkAndUpdateKeepInventory()`

### Bước 3: Run Debug
**Cách 1: Debug Test**
1. Mở file test `DynamicKeepInvPluginTest.java`
2. Click vào icon "Debug" phía trên tên test
3. Hoặc nhấn F5

**Cách 2: Debug qua Terminal**
```powershell
mvn test -Dmaven.surefire.debug
# Sau đó attach debugger trong VS Code
```

### Bước 4: Sử dụng Debug Controls
- **F5**: Continue/Start debugging
- **F10**: Step Over (chạy qua dòng tiếp theo)
- **F11**: Step Into (nhảy vào hàm)
- **Shift+F11**: Step Out (thoát khỏi hàm)
- **Ctrl+Shift+F5**: Restart
- **Shift+F5**: Stop

### Debug Tips
```java
// Ví dụ: Debug vòng lặp
for (World world : Bukkit.getWorlds()) {
    // Đặt breakpoint ở đây
    long time = world.getTime();  // ← BREAKPOINT
    
    // Khi break, bạn có thể:
    // 1. Hover chuột lên biến để xem giá trị
    // 2. Xem Variables panel bên trái
    // 3. Thêm watch expression
}
```

## 3. CONDITIONAL BREAKPOINTS

### Cách đặt
1. Right-click vào breakpoint (chấm đỏ)
2. Chọn "Edit Breakpoint"
3. Thêm điều kiện

### Ví dụ
```java
for (World world : Bukkit.getWorlds()) {
    long time = world.getTime();
    // Breakpoint với điều kiện: time > 18000
    // Chỉ break khi time > 18000 (ban đêm)
}
```

Điều kiện: `time > 18000`

## 4. LOGPOINTS

### Cách dùng
1. Right-click vào số dòng
2. Chọn "Add Logpoint"
3. Nhập message: `Time is {time}, IsDay: {isDay}`

Logpoint in ra console mà không cần sửa code!

## 5. DEBUG UNIT TESTS

### Test Framework: JUnit 5

#### Run một test
```powershell
# Run tất cả tests
mvn test

# Run một test class
mvn test -Dtest=DynamicKeepInvPluginTest

# Run một test method
mvn test -Dtest=DynamicKeepInvPluginTest#testDayTimeKeepInventory
```

#### Debug test trong VS Code
```java
@Test
void testDayTimeKeepInventory() {
    world.setTime(6000);  // ← Đặt breakpoint ở đây
    server.getScheduler().performOneTick();
    
    Boolean keepInv = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
    assertTrue(keepInv);  // ← Hoặc ở đây
}
```

## 6. DEBUG TRONG MINECRAFT SERVER

### Cách 1: Remote Debug

#### a) Start server với debug mode
```bash
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -jar paper.jar
```

#### b) Attach từ VS Code
Thêm vào `.vscode/launch.json`:
```json
{
  "type": "java",
  "name": "Attach to Minecraft Server",
  "request": "attach",
  "hostName": "localhost",
  "port": 5005
}
```

#### c) Kết nối
1. Start server với debug mode
2. Trong VS Code, nhấn F5
3. Chọn "Attach to Minecraft Server"
4. Đặt breakpoint trong plugin code
5. Trigger event trong game (ví dụ: `/time set night`)

### Cách 2: Log Debugging
```java
@Override
public void onEnable() {
    getLogger().info("=== Plugin Starting ===");
    getLogger().info("Java Version: " + System.getProperty("java.version"));
    getLogger().info("Server Version: " + Bukkit.getVersion());
    
    saveDefaultConfig();
    getLogger().info("Config loaded: " + getConfig().getBoolean("enabled"));
    
    startChecking();
    getLogger().info("=== Plugin Started ===");
}
```

## 7. COMMON DEBUG SCENARIOS

### Scenario 1: Keep Inventory không đổi
```java
private void checkAndUpdateKeepInventory() {
    getLogger().info("=== CHECK START ===");
    
    for (World world : Bukkit.getWorlds()) {
        long time = world.getTime();
        getLogger().info("World: " + world.getName() + ", Time: " + time);
        
        boolean isDay = time >= 0 && time < 13000;
        getLogger().info("IsDay: " + isDay);
        
        boolean shouldKeepInv = isDay ? keepInvDay : keepInvNight;
        getLogger().info("Should KeepInv: " + shouldKeepInv);
        
        Boolean current = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
        getLogger().info("Current KeepInv: " + current);
        
        if (current != shouldKeepInv) {
            world.setGameRule(GameRule.KEEP_INVENTORY, shouldKeepInv);
            getLogger().info("CHANGED to: " + shouldKeepInv);
        }
    }
    
    getLogger().info("=== CHECK END ===");
}
```

### Scenario 2: Plugin không load
```java
@Override
public void onEnable() {
    try {
        getLogger().info("Step 1: Saving config...");
        saveDefaultConfig();
        
        getLogger().info("Step 2: Reading config...");
        boolean enabled = getConfig().getBoolean("enabled");
        getLogger().info("Enabled: " + enabled);
        
        getLogger().info("Step 3: Starting task...");
        startChecking();
        
        getLogger().info("Plugin enabled successfully!");
    } catch (Exception e) {
        getLogger().severe("Error during enable: " + e.getMessage());
        e.printStackTrace();
    }
}
```

### Scenario 3: Commands không hoạt động
```java
@Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    getLogger().info("Command received!");
    getLogger().info("Sender: " + sender.getName());
    getLogger().info("Command: " + command.getName());
    getLogger().info("Args: " + Arrays.toString(args));
    getLogger().info("Has permission: " + sender.hasPermission("dynamickeepinv.admin"));
    
    // ... rest of command code
}
```

## 8. WATCH VARIABLES

Trong Debug mode, thêm watches:
1. Click vào tab "Watch" trong Debug panel
2. Click "+" để add expression
3. Ví dụ watches:
   - `world.getTime()`
   - `world.getName()`
   - `getConfig().getBoolean("enabled")`
   - `Bukkit.getWorlds().size()`

## 9. EXCEPTION BREAKPOINTS

Trong VS Code Debug:
1. Mở Breakpoints panel
2. Click "Add Exception Breakpoint"
3. Chọn loại exception (ví dụ: `NullPointerException`)
4. Debug sẽ tự động break khi có exception này

## 10. PERFORMANCE DEBUGGING

### Đo thời gian thực thi
```java
long startTime = System.nanoTime();

// Code cần đo
checkAndUpdateKeepInventory();

long endTime = System.nanoTime();
long duration = (endTime - startTime) / 1_000_000; // ms
getLogger().info("Execution time: " + duration + "ms");
```

### Memory usage
```java
Runtime runtime = Runtime.getRuntime();
long memory = runtime.totalMemory() - runtime.freeMemory();
getLogger().info("Memory used: " + (memory / 1024 / 1024) + "MB");
```

## 11. DEBUGGING TIPS

### ✅ DO's
- Đặt breakpoint ở nơi bạn nghi ngờ có bug
- Sử dụng debug mode trong config khi develop
- Viết unit tests trước khi debug
- Check logs thường xuyên
- Sử dụng meaningful log messages

### ❌ DON'Ts
- Để debug mode ON khi production
- Quá nhiều breakpoints (làm chậm)
- Ignore warnings
- Debug mà không đọc stack trace
- Không commit debug code lên git

## 12. STACK TRACE READING

Khi có lỗi:
```
java.lang.NullPointerException: Cannot invoke "org.bukkit.World.getTime()" because "world" is null
    at xyz.superez.dynamickeepinv.DynamicKeepInvPlugin.checkAndUpdateKeepInventory(DynamicKeepInvPlugin.java:75)
    at xyz.superez.dynamickeepinv.DynamicKeepInvPlugin$1.run(DynamicKeepInvPlugin.java:45)
```

Đọc:
1. **Dòng 1**: Loại lỗi + mô tả → `world` bị null
2. **Dòng 2**: Nơi xảy ra → `DynamicKeepInvPlugin.java:75`
3. **Dòng 3**: Ai gọi → `DynamicKeepInvPlugin$1.run:45`

Fix: Check null trước khi dùng
```java
if (world != null) {
    long time = world.getTime();
}
```

## 13. MOCK TESTING

Trong test, sử dụng MockBukkit:
```java
@Test
void testWithDebug() {
    // Setup
    world.setTime(6000);
    System.out.println("Time set to: " + world.getTime());
    
    // Execute
    server.getScheduler().performOneTick();
    System.out.println("Tick performed");
    
    // Verify
    Boolean keepInv = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
    System.out.println("KeepInv: " + keepInv);
    
    // Assert với message rõ ràng
    assertTrue(keepInv, "Keep inventory should be ON at time 6000 (day)");
}
```

## 14. DEBUGGING CHECKLIST

Khi có bug:
- [ ] Đọc error message đầy đủ
- [ ] Check logs server
- [ ] Bật debug mode
- [ ] Thêm log points
- [ ] Đặt breakpoints
- [ ] Step through code
- [ ] Check variables
- [ ] Verify assumptions
- [ ] Test fix
- [ ] Remove debug code

Happy Debugging! 🐛✨
