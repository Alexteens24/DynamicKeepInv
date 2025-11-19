# Server Setup Guide - DynamicKeepInv

## Quick Start

### Bước 1: Download Server JAR
Chọn 1 trong 2:

#### Option A: Paper (Recommended cho hầu hết server)
```powershell
# Download Paper 1.21.3
Invoke-WebRequest -Uri "https://api.papermc.io/v2/projects/paper/versions/1.21.3/builds/latest/downloads/paper-1.21.3.jar" -OutFile "server.jar"
```

#### Option B: Folia (Cho server lớn, high-performance)
```powershell
# Download Folia 1.20.6
Invoke-WebRequest -Uri "https://api.papermc.io/v2/projects/folia/versions/1.20.6/builds/latest/downloads/folia-1.20.6.jar" -OutFile "server.jar"
```

### Bước 2: Chạy Server

#### Windows:
```powershell
# Dùng script tối ưu (khuyến nghị)
.\start-server.bat

# Hoặc dùng PowerShell script
.\start-server.ps1

# Hoặc chạy trực tiếp
java -Xmx4G -Xms2G -jar server.jar --nogui
```

#### Linux/Mac:
```bash
# Cho phép execute
chmod +x start-server.sh

# Chạy
./start-server.sh
```

### Bước 3: Chấp Nhận EULA
Server sẽ tắt ngay lần đầu. Mở file `eula.txt` và đổi:
```
eula=false
```
Thành:
```
eula=true
```

### Bước 4: Copy Plugin
```powershell
# Tạo folder plugins nếu chưa có
mkdir plugins

# Copy plugin vào
copy target\DynamicKeepInv-1.0.0.jar plugins\
```

### Bước 5: Khởi Động Lại Server
```powershell
.\start-server.bat
```

---

## JVM Flags Explained

Script sử dụng **Aikar's Flags** - bộ flags tối ưu nhất cho Minecraft:

### Memory Allocation
```
-Xms2G    # RAM khởi động: 2GB
-Xmx4G    # RAM tối đa: 4GB
```

**Lưu ý**: Điều chỉnh theo RAM server của bạn:
- Server nhỏ (1-10 players): `-Xms1G -Xmx2G`
- Server vừa (10-50 players): `-Xms2G -Xmx4G`
- Server lớn (50-200 players): `-Xms4G -Xmx8G`
- Server khủng (200+ players): `-Xms8G -Xmx16G`

### Garbage Collection (G1GC)
```
-XX:+UseG1GC                          # Dùng G1 garbage collector
-XX:MaxGCPauseMillis=200              # GC pause tối đa 200ms
-XX:+ParallelRefProcEnabled           # Song song xử lý references
```

### G1 Tuning
```
-XX:G1NewSizePercent=30               # Young generation tối thiểu 30%
-XX:G1MaxNewSizePercent=40            # Young generation tối đa 40%
-XX:G1HeapRegionSize=8M               # Mỗi region 8MB
-XX:G1ReservePercent=20               # Dự trữ 20% heap
-XX:InitiatingHeapOccupancyPercent=15 # GC khi heap đầy 15%
```

### Performance Optimizations
```
-XX:+AlwaysPreTouch       # Pre-allocate memory ngay từ đầu
-XX:+DisableExplicitGC    # Disable System.gc() calls
-XX:+PerfDisableSharedMem # Tắt shared memory cho perf monitoring
```

---

## Tùy Chỉnh Server

### Server Properties
Sửa file `server.properties`:
```properties
# Cơ bản
server-port=25565
max-players=20
difficulty=normal
gamemode=survival
pvp=true

# Performance
view-distance=10          # Giảm xuống 8 nếu lag
simulation-distance=10    # Giảm xuống 8 nếu lag

# Whitelist (khuyến nghị cho server riêng tư)
white-list=false
enforce-whitelist=false
```

### Paper/Folia Configuration
File `config/paper-global.yml`:
```yaml
timings:
  enabled: true  # Bật để debug performance

async-chunks:
  threads: -1    # Auto-detect CPU cores

misc:
  fix-target-selector-tag-completion: true
  update-folder: update
```

---

## Plugin Configuration

File `plugins/DynamicKeepInv/config.yml`:
```yaml
# Bật/tắt plugin
enabled: true

# Keep inventory settings
keep-inventory-day: true    # Ban ngày: giữ đồ
keep-inventory-night: false # Ban đêm: mất đồ

# Thời gian (Minecraft ticks)
day-start: 0        # 6:00 AM
night-start: 13000  # 7:00 PM

# Kiểm tra mỗi bao nhiêu ticks (100 ticks = 5 giây)
check-interval: 100

# Debug mode
debug: false

# Chỉ áp dụng cho worlds cụ thể (để trống = tất cả worlds)
enabled-worlds: []
# enabled-worlds:
#   - world
#   - world_nether
```

File `plugins/DynamicKeepInv/messages.yml`:
```yaml
# Đổi ngôn ngữ: vi (Tiếng Việt) hoặc en (English)
language: vi
```

---

## Commands & Permissions

### Commands
```
/dki status   # Xem trạng thái plugin
/dki reload   # Reload config
/dki enable   # Bật plugin
/dki disable  # Tắt plugin
/dki toggle   # Chuyển đổi on/off
```

### Permissions
File `plugins/LuckPerms/...` hoặc trong game:
```
/lp group admin permission set dynamickeepinv.admin true
```

Hoặc dùng OP:
```
/op TênBạn
```

---

## Testing Plugin

### Test 1: Plugin Load
Xem console khi server start:
```
[DynamicKeepInv] Paper/Spigot detected! Using standard scheduler.
[DynamicKeepInv] DynamicKeepInv is starting... (Platform: Paper)
[DynamicKeepInv] DynamicKeepInv enabled!
```

### Test 2: Commands
```
/dki status
```
Phải thấy:
- Enabled: true
- Keep Inv (Day): true
- Keep Inv (Night): false
- World status

### Test 3: Day/Night Cycle
```
/time set day     # Chuyển sang ban ngày
/gamerule keepInventory  # Check = true

/time set night   # Chuyển sang ban đêm
/gamerule keepInventory  # Check = false
```

### Test 4: Chết Mất Đồ
```
/time set day
/kill

# Ban ngày: Không mất đồ
# Ban đêm: Mất đồ
```

---

## Troubleshooting

### Lỗi: Server không start
```
Error: Unable to access jarfile server.jar
```
**Fix**: Download và đổi tên thành `server.jar`

### Lỗi: Java version
```
Unsupported class file major version 65
```
**Fix**: Cần Java 21. Download tại: https://adoptium.net/

### Lỗi: Plugin không load
```
Could not load 'plugins/DynamicKeepInv-1.0.0.jar'
```
**Fix**: 
1. Check Java >= 21
2. Check Paper version >= 1.19.4
3. Xem file log: `logs/latest.log`

### Lỗi: Lệnh không hoạt động
```
Unknown command
```
**Fix**: Phải có quyền admin: `/op TênBạn`

### Lỗi: Keep inventory không đổi
**Debug checklist**:
1. `/dki status` - Check enabled = true
2. Bật debug mode trong config.yml
3. `/dki reload`
4. Xem logs: `logs/latest.log`

---

## Performance Monitoring

### Timings Report
```
/timings on
# Chơi 5-10 phút
/timings paste
```
Mở link để xem performance analysis.

### TPS Check
```
/tps
```
Nên thấy: **20.0 TPS** = server chạy mượt

Nếu TPS < 20:
- Giảm view-distance
- Giảm simulation-distance  
- Tối ưu plugins khác
- Nâng cấp RAM/CPU

---

## Backup & Updates

### Backup Server
```powershell
# Tắt server
stop

# Backup
$date = Get-Date -Format "yyyy-MM-dd_HH-mm"
Compress-Archive -Path . -DestinationPath "backup_$date.zip"
```

### Update Plugin
```powershell
# Build plugin mới
cd C:\dev\DynamicKeepInv
mvn clean package '-Dmaven.test.skip=true'

# Tắt server, thay JAR
copy target\DynamicKeepInv-1.0.0.jar plugins\ -Force

# Khởi động lại server
```

### Update Server (Paper/Folia)
```powershell
# Backup server.jar cũ
copy server.jar server.jar.old

# Download version mới, đổi tên thành server.jar
# Khởi động lại
```

---

## Advanced: Multiple Test Servers

Tạo nhiều server để test:
```powershell
# Server 1: Paper
mkdir test-paper
cd test-paper
# Download Paper thành server.jar
copy ..\DynamicKeepInv-1.0.0.jar plugins\

# Server 2: Folia  
mkdir test-folia
cd test-folia
# Download Folia thành server.jar
copy ..\DynamicKeepInv-1.0.0.jar plugins\
```

Sửa `server.properties` để dùng port khác:
```
server-port=25566  # Server 2
```

---

## File Structure

```
📁 Server Root
├── 📄 server.jar              # Paper/Folia JAR
├── 📄 start-server.bat        # Windows startup script
├── 📄 start-server.ps1        # PowerShell startup script
├── 📄 start-server.sh         # Linux/Mac startup script
├── 📄 eula.txt                # EULA agreement
├── 📄 server.properties       # Server config
├── 📁 plugins/
│   ├── 📄 DynamicKeepInv-1.0.0.jar
│   └── 📁 DynamicKeepInv/
│       ├── 📄 config.yml      # Plugin config
│       └── 📄 messages.yml    # Language config
├── 📁 world/                  # Overworld
├── 📁 world_nether/           # Nether
├── 📁 world_the_end/          # The End
├── 📁 logs/                   # Server logs
└── 📁 config/                 # Paper/Folia configs
```

---

## Support

### Resources
- **Paper Docs**: https://docs.papermc.io/
- **Folia Docs**: https://docs.papermc.io/folia
- **Aikar's Flags**: https://aikar.co/2018/07/02/tuning-the-jvm-g1gc-garbage-collector-flags-for-minecraft/

### Common Links
- Download Paper: https://papermc.io/downloads/paper
- Download Folia: https://papermc.io/downloads/folia
- Download Java 21: https://adoptium.net/

---

**Good luck với server! 🚀**
