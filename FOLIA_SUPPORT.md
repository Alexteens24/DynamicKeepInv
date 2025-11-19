# Folia Support 🚀

## Plugin này giờ hỗ trợ cả Folia và Paper/Spigot!

### Folia là gì?
**Folia** là fork của Paper được thiết kế để chạy đa luồng (multi-threaded), giúp server có thể xử lý nhiều regions đồng thời → **Performance cực khủng** cho server có nhiều người chơi!

### Các thay đổi:

#### ✅ 1. Auto-detect Platform
Plugin tự động phát hiện đang chạy trên Folia hay Paper/Spigot:
```
[DynamicKeepInv] Folia detected! Using region-based scheduler.
[DynamicKeepInv] DynamicKeepInv is starting... (Platform: Folia)
```

#### ✅ 2. Dual Scheduler System
- **Folia**: Sử dụng `GlobalRegionScheduler` - thread-safe cho multi-region
- **Paper/Spigot**: Sử dụng `BukkitRunnable` - scheduler truyền thống

#### ✅ 3. Plugin Metadata
Đã thêm `folia-supported: true` trong `plugin.yml` để Folia nhận diện plugin tương thích.

#### ✅ 4. Updated Paper API
Nâng cấp lên Paper API 1.20.6 để có đầy đủ Folia APIs.

---

## Compatibility Matrix

| Platform | Min Version | Max Version | Status |
|----------|-------------|-------------|--------|
| **Folia** | 1.20.4+ | Latest | ✅ Full Support |
| **Paper** | 1.19.4+ | Latest | ✅ Full Support |
| **Spigot** | 1.19.4+ | Latest | ✅ Full Support |

---

## Testing

### Test trên Paper:
```bash
# Download Paper server (như bình thường)
java -jar paper.jar
```

### Test trên Folia:
```bash
# Download Folia từ: https://papermc.io/downloads/folia
# Hoặc:
curl -o folia.jar https://api.papermc.io/v2/projects/folia/versions/1.20.6/builds/latest/downloads/folia-1.20.6.jar

# Chạy Folia server
java -Xmx4G -Xms4G -jar folia.jar --nogui

# Copy plugin
copy DynamicKeepInv-1.0.0.jar plugins/
```

**Lưu ý**: Folia yêu cầu ít nhất **Java 21** (đúng với config hiện tại của mình!)

---

## Code Technical Details

### Scheduler Detection:
```java
private void detectFolia() {
    try {
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
        isFolia = true; // Folia detected!
    } catch (ClassNotFoundException e) {
        isFolia = false; // Paper/Spigot
    }
}
```

### Folia Scheduler:
```java
GlobalRegionScheduler scheduler = Bukkit.getGlobalRegionScheduler();
foliaTask = scheduler.runAtFixedRate(this, (task) -> {
    checkAndUpdateKeepInventory();
}, 1L, interval);
```

### Paper/Spigot Scheduler:
```java
checkTask = new BukkitRunnable() {
    @Override
    public void run() {
        checkAndUpdateKeepInventory();
    }
};
checkTask.runTaskTimer(this, 0L, interval);
```

---

## Performance Benefits với Folia

### Paper/Spigot (Single-threaded):
- 1 main thread xử lý tất cả
- TPS giảm khi có nhiều người chơi
- Giới hạn ~200-300 players

### Folia (Multi-threaded):
- Mỗi region có thread riêng
- TPS ổn định hơn nhiều
- Có thể support **1000+ players**!
- Regions xa nhau không ảnh hưởng lẫn nhau

---

## Migration Notes

### Từ Paper → Folia:
1. Backup server
2. Download Folia JAR
3. Thay thế Paper JAR bằng Folia JAR
4. Plugin của mình tự động hoạt động! ✨

### Từ Folia → Paper:
1. Cũng chỉ cần thay JAR
2. Plugin auto-detect và dùng scheduler phù hợp

---

## Known Limitations

Folia có một số hạn chế:
- Không tương thích với **tất cả** plugins (nhiều plugins cũ dùng main thread)
- Cần thiết kế plugin theo kiểu region-based
- Plugin của mình OK vì chỉ đọc/ghi GameRule (thread-safe)

---

## Recommended For:

### Dùng Folia khi:
✅ Server có **200+ người chơi**  
✅ Bạn có **multi-core CPU** mạnh (8+ cores)  
✅ Muốn TPS ổn định hơn  
✅ Survival/SMP server lớn  

### Dùng Paper khi:
✅ Server nhỏ (<200 players)  
✅ Dùng nhiều plugins legacy  
✅ Minigame server (cần plugins cũ)  

---

## Build Information

**JAR Location**: `target/DynamicKeepInv-1.0.0.jar`  
**Size**: ~15 KB  
**Paper API**: 1.20.6-R0.1-SNAPSHOT  
**Java**: 21 LTS  
**Folia Support**: ✅ Yes  

---

## FAQ

**Q: Plugin có chạy trên Paper cũ (1.19.4) không?**  
A: Có! Vẫn backward compatible. API 1.20.6 chỉ cần để compile, runtime 1.19+ vẫn OK.

**Q: Có cần config gì khác cho Folia không?**  
A: Không! Plugin tự động detect và dùng scheduler phù hợp.

**Q: Performance có khác biệt không?**  
A: Trên Folia, performance tốt hơn nhiều ở server lớn. Trên server nhỏ thì gần như không khác biệt.

**Q: Có thể test cả 2 platforms không?**  
A: Có! Cài cả Paper và Folia ở 2 folder khác nhau, copy plugin vào cả 2.

---

## Changelog

### v1.0.0 (Current)
- ✅ Added Folia support with GlobalRegionScheduler
- ✅ Auto-detection between Folia and Paper/Spigot
- ✅ Updated to Paper API 1.20.6
- ✅ Backward compatible with Paper 1.19.4+
- ✅ Thread-safe GameRule operations
- ✅ Vietnamese + English language support

---

**Enjoy high-performance Minecraft server với Folia! 🚀**
