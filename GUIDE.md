# 🎮 HƯỚNG DẪN SỬ DỤNG VÀ TEST PLUGIN

## ✅ Plugin đã được build thành công!

File JAR: `target/DynamicKeepInv-1.0.0.jar`

---

## 📦 CÀI ĐẶT VÀO SERVER

### Bước 1: Copy plugin vào server
```powershell
# Copy file JAR vào thư mục plugins của server Minecraft
Copy-Item "target\DynamicKeepInv-1.0.0.jar" "đường_dẫn_server\plugins\"
```

### Bước 2: Restart server
- Start hoặc restart server Minecraft (Paper/Spigot 1.21.3+)
- Plugin sẽ tự động tạo file `config.yml` trong `plugins/DynamicKeepInv/`

---

## 🎯 CÁCH HOẠT ĐỘNG

### Tự động
- **Ban ngày (0-12999 ticks)**: Keep Inventory = ON
- **Ban đêm (13000-23999 ticks)**: Keep Inventory = OFF
- Plugin kiểm tra mỗi 5 giây (100 ticks)

### Trong game
```
Ban ngày → Chết → Giữ đồ ✅
Ban đêm → Chết → Mất đồ ❌
```

---

## 🧪 TEST PLUGIN TRONG SERVER

### Test 1: Kiểm tra trạng thái
```
/dki status
```
Kết quả sẽ hiện:
- Enabled: true/false
- Keep Inv (Day): true
- Keep Inv (Night): false
- World status hiện tại

### Test 2: Test ban ngày
```
1. /time set day          # Set về ban ngày
2. /dki status            # Kiểm tra - Keep Inventory ON
3. Chết thử              # Vẫn giữ đồ
```

### Test 3: Test ban đêm
```
1. /time set night        # Set về ban đêm
2. /dki status            # Kiểm tra - Keep Inventory OFF
3. Chết thử              # Mất đồ
```

### Test 4: Test chuyển đổi tự động
```
1. /time set 12900        # Gần sunset
2. Đợi vài giây          # Plugin tự động chuyển
3. /dki status            # Kiểm tra thay đổi
```

### Test 5: Toggle plugin
```
/dki toggle               # Tắt plugin
# Chết thử - hành vi mặc định của server
/dki toggle               # Bật lại plugin
```

### Test 6: Reload config
```
# Sửa config.yml (ví dụ: check-interval: 200)
/dki reload               # Reload config không cần restart server
```

---

## ⚙️ TÙY CHỈNH CONFIG

File: `plugins/DynamicKeepInv/config.yml`

### Ví dụ 1: Đổi tốc độ kiểm tra
```yaml
check-interval: 200  # 200 ticks = 10 giây (chậm hơn, ít lag hơn)
```

### Ví dụ 2: Chỉ áp dụng cho world chính
```yaml
enabled-worlds:
  - world
# Không áp dụng cho nether, end
```

### Ví dụ 3: Bật keep inventory cả ngày lẫn đêm
```yaml
keep-inventory-day: true
keep-inventory-night: true
# Plugin vẫn chạy nhưng luôn ON
```

### Ví dụ 4: Custom thời gian
```yaml
day-start: 23000    # Ngày bắt đầu từ tick 23000
night-start: 12000  # Đêm bắt đầu từ tick 12000
# Đảo ngược ngày/đêm!
```

---

## 🐛 DEBUG VÀ TROUBLESHOOTING

### Bật debug mode
```yaml
# config.yml
debug: true
```

Reload plugin:
```
/dki reload
```

### Xem logs
Logs sẽ in ra trong console server:
```
[DynamicKeepInv] World 'world': Day detected. Keep Inventory is now ON
[DynamicKeepInv] [DEBUG] World: world, Time: 6000, IsDay: true, KeepInv: true
```

### Common Issues

#### Issue 1: Plugin không load
**Triệu chứng:** Không thấy plugin trong `/plugins`

**Giải quyết:**
```
1. Kiểm tra server version (cần Paper/Spigot 1.21.3+)
2. Kiểm tra Java version (cần Java 21)
3. Xem logs: [DynamicKeepInv] enabling...
4. Kiểm tra permissions
```

#### Issue 2: Keep Inventory không đổi
**Triệu chứng:** Luôn ON hoặc luôn OFF

**Giải quyết:**
```
1. /dki status                    # Kiểm tra enabled
2. Kiểm tra config.yml
3. Bật debug mode
4. /dki reload
5. Kiểm tra enabled-worlds (có đúng world không?)
```

#### Issue 3: Commands không hoạt động
**Triệu chứng:** "Unknown command"

**Giải quyết:**
```
1. Kiểm tra plugin loaded: /plugins
2. Kiểm tra permission: dynamickeepinv.admin
3. Thử lại: /dynamickeepinv status
```

---

## 📊 PERFORMANCE

### Tài nguyên sử dụng
- **CPU**: Rất thấp (chỉ check mỗi 5 giây)
- **RAM**: < 5MB
- **TPS Impact**: Không đáng kể

### Tối ưu cho server lớn
```yaml
# Tăng interval để giảm CPU usage
check-interval: 200  # 10 giây thay vì 5 giây

# Chỉ áp dụng cho world cụ thể
enabled-worlds:
  - world
```

---

## 🔥 TIPS & TRICKS

### Tip 1: Thông báo cho người chơi
Thêm plugin thông báo khi chuyển ngày/đêm:
```
Plugin gợi ý: BroadcastNotifier, TitleManager
```

### Tip 2: Kết hợp với plugin khác
```
- WorldGuard: Chỉ áp dụng trong region cụ thể
- Multiverse: Khác nhau cho mỗi world
```

### Tip 3: Custom thời gian độc đáo
```yaml
# Ví dụ: Keep Inventory chỉ giữa trưa
day-start: 5000      # 11:00 AM
night-start: 7000    # 1:00 PM
```

---

## 📝 CHANGELOG

### Version 1.0.0
- ✅ Auto toggle Keep Inventory theo ngày/đêm
- ✅ Support nhiều worlds
- ✅ Commands quản lý
- ✅ Config có thể reload
- ✅ Debug mode
- ✅ Support MC 1.21.3+

---

## 🚀 NEXT STEPS

### Học thêm về plugin development
1. Đọc Paper API docs: https://docs.papermc.io/
2. Tham gia Discord: Paper Discord
3. Xem code của plugin khác

### Nâng cấp plugin này
Bạn có thể thêm:
- [ ] Thông báo cho người chơi khi chuyển mode
- [ ] Permission riêng cho từng người chơi
- [ ] Database lưu thống kê
- [ ] API cho plugin khác sử dụng
- [ ] PlaceholderAPI support
- [ ] Particle effects khi chuyển mode

---

## 📚 TÀI LIỆU THAM KHẢO

- **README.md**: Tổng quan plugin
- **DEBUG_GUIDE.md**: Hướng dẫn debug chi tiết
- **Source code**: Trong `src/main/java/`
- **Tests**: Trong `src/test/java/`

---

## 💡 HỌC DEBUG HIỆU QUẢ

### Debug trong development (code)
```java
// Thêm log
getLogger().info("Checking world: " + world.getName());

// Đặt breakpoint trong VS Code
// F5 để debug tests
```

### Debug trong production (server)
```yaml
# config.yml
debug: true
```

```
# Console logs
[DynamicKeepInv] [DEBUG] World: world, Time: 6000
```

---

## ⭐ THÀNH CÔNG!

Plugin của bạn đang hoạt động! 🎉

Commands để nhớ:
- `/dki status` - Xem trạng thái
- `/dki toggle` - Bật/tắt
- `/dki reload` - Reload config

Happy coding! 🚀
