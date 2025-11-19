# DynamicKeepInv - Minecraft Plugin

Plugin Minecraft tự động bật Keep Inventory ban ngày và tắt ban đêm.

## 📋 Tính năng

- ✅ Tự động bật/tắt Keep Inventory dựa trên chu kỳ ngày/đêm
- ✅ Hỗ trợ Minecraft 1.21.3 và các phiên bản mới
- ✅ Hỗ trợ nhiều worlds
- ✅ Có thể cấu hình thời gian và hành vi
- ✅ Commands để quản lý
- ✅ Debug mode

## 🛠️ Cài đặt

1. Build plugin:
```bash
mvn clean package
```

2. Copy file `target/DynamicKeepInv-1.0.0.jar` vào thư mục `plugins/` của server

3. Restart server

## ⚙️ Cấu hình (config.yml)

```yaml
# Bật/tắt plugin
enabled: true

# Keep inventory ban ngày
keep-inventory-day: true

# Keep inventory ban đêm
keep-inventory-night: false

# Kiểm tra mỗi bao nhiêu ticks (20 ticks = 1 giây)
check-interval: 100

# Worlds được áp dụng (để trống = tất cả)
enabled-worlds: []

# Debug mode
debug: false
```

## 📝 Commands

| Command | Mô tả | Permission |
|---------|-------|------------|
| `/dki` hoặc `/dynamickeepinv` | Hiển thị help | `dynamickeepinv.admin` |
| `/dki status` | Xem trạng thái plugin | `dynamickeepinv.admin` |
| `/dki reload` | Reload config | `dynamickeepinv.admin` |
| `/dki toggle` | Bật/tắt plugin | `dynamickeepinv.admin` |

## 🧪 Testing - Hướng dẫn Test Plugin

### 1. Chạy Unit Tests

```bash
mvn test
```

Tests sẽ kiểm tra:
- ✅ Plugin load thành công
- ✅ Config đọc đúng
- ✅ Ban ngày bật Keep Inventory
- ✅ Ban đêm tắt Keep Inventory
- ✅ Chuyển đổi ngày/đêm hoạt động
- ✅ Commands hoạt động
- ✅ Hỗ trợ nhiều worlds

### 2. Xem kết quả test chi tiết

Khi chạy test, bạn sẽ thấy output như:

```
=== Test Setup Complete ===
Running: testPluginLoads
✓ Plugin loaded successfully
=== Test Cleanup Complete ===
```

### 3. Test trong server thật

#### a) Cài đặt plugin lên server test
```bash
# Build plugin
mvn clean package

# Copy vào server
cp target/DynamicKeepInv-1.0.0.jar /path/to/server/plugins/
```

#### b) Test commands trong game
```
/dki status          # Xem trạng thái
/time set day        # Set về ban ngày -> Keep Inv ON
/time set night      # Set về ban đêm -> Keep Inv OFF
/dki toggle          # Tắt plugin
/dki reload          # Reload config
```

## 🐛 Debug - Hướng dẫn Debug

### 1. Bật Debug Mode

Sửa `config.yml`:
```yaml
debug: true
```

Reload plugin:
```
/dki reload
```

### 2. Xem logs trong console

Debug logs sẽ hiện trong console server:
```
[DynamicKeepInv] [DEBUG] Started checking task with interval: 100 ticks
[DynamicKeepInv] [DEBUG] World: world, Time: 6000, IsDay: true, KeepInv: true
[DynamicKeepInv] [DEBUG] World: world, Time: 18000, IsDay: false, KeepInv: false
```

### 3. Debug trong VS Code

#### a) Cài đặt Extension Debugger for Java

#### b) Tạo file `.vscode/launch.json`:
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Debug Tests",
      "request": "launch",
      "mainClass": "",
      "projectName": "DynamicKeepInv",
      "cwd": "${workspaceFolder}",
      "console": "integratedTerminal",
      "args": "",
      "vmArgs": "-ea"
    }
  ]
}
```

#### c) Đặt breakpoints
1. Mở file `DynamicKeepInvPlugin.java`
2. Click vào bên trái số dòng để đặt breakpoint (chấm đỏ)
3. Ví dụ: đặt breakpoint tại dòng `checkAndUpdateKeepInventory()`

#### d) Run test với debug
```bash
mvn test -Dmaven.surefire.debug
```

Hoặc trong VS Code:
- F5 để start debugging
- F10 để step over
- F11 để step into
- F9 để toggle breakpoint

### 4. Debug với Print Statements

Thêm log vào code:
```java
getLogger().info("Current time: " + world.getTime());
getLogger().info("Is day: " + isDay);
getLogger().info("Keep Inventory: " + shouldKeepInv);
```

### 5. Common Issues và Cách Fix

#### Issue: Plugin không load
**Debug:**
```bash
# Kiểm tra logs khi start server
tail -f logs/latest.log
```
**Fix:** Kiểm tra `plugin.yml` và main class name

#### Issue: Keep Inventory không đổi
**Debug:**
```java
// Thêm log trong checkAndUpdateKeepInventory()
getLogger().info("Checking world: " + world.getName());
getLogger().info("Time: " + time + ", IsDay: " + isDay);
```
**Fix:** Kiểm tra `check-interval` và world name trong config

#### Issue: Commands không hoạt động
**Debug:**
```java
// Trong onCommand()
getLogger().info("Command received: " + command.getName());
getLogger().info("Args: " + Arrays.toString(args));
```
**Fix:** Kiểm tra permissions và command syntax

### 6. Performance Testing

Chạy performance test:
```bash
mvn test -Dtest=DynamicKeepInvPluginTest#testPerformance
```

### 7. Memory Profiling

Thêm vào VM args:
```bash
-Xmx512M -Xms256M -XX:+PrintGCDetails
```

## 📚 Học thêm về Testing

### Các loại test trong project:

1. **Unit Test**: Test từng phần riêng lẻ
   - `testPluginLoads()` - Test plugin load
   - `testConfigDefaults()` - Test config

2. **Integration Test**: Test tích hợp giữa các thành phần
   - `testDayTimeKeepInventory()` - Test ngày/đêm + gamerule
   - `testMultipleWorlds()` - Test nhiều worlds

3. **Functional Test**: Test chức năng tổng thể
   - `testDayToNightTransition()` - Test chuyển đổi

### Assertions thường dùng:

```java
assertTrue(condition);           // Kiểm tra true
assertFalse(condition);          // Kiểm tra false
assertEquals(expected, actual);  // Kiểm tra bằng nhau
assertNotNull(object);           // Kiểm tra không null
assertNotEquals(a, b);           // Kiểm tra khác nhau
```

## 🎯 Tips Debug hiệu quả

1. **Sử dụng breakpoints thông minh**
   - Conditional breakpoints: chỉ break khi điều kiện đúng
   - Logpoints: in log mà không cần sửa code

2. **Đọc stack trace**
   - Dòng trên cùng: nơi xảy ra lỗi
   - Đọc từ dưới lên: theo dõi luồng thực thi

3. **Sử dụng debug mode trong config**
   - Bật debug khi develop
   - Tắt debug khi production

4. **Test từng phần nhỏ**
   - Tạo test cho mỗi function
   - Dễ tìm bug hơn

## 🔧 Requirements

- Java 21
- Paper/Spigot 1.21.3+
- Maven

## 📄 License

MIT License

## 👤 Author

SuperEZ
