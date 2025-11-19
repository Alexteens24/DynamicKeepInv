# Hướng Dẫn Test Plugin

## Cách 1: Test Thật Trên Server (Khuyến nghị)

### Bước 1: Download PaperMC Server
```powershell
# Tạo thư mục test server
mkdir C:\dev\test-server
cd C:\dev\test-server

# Download Paper 1.21.3 (hoặc bản khác 1.19+)
# Vào https://papermc.io/downloads/paper
# Hoặc dùng lệnh:
curl -o paper.jar https://api.papermc.io/v2/projects/paper/versions/1.21.3/builds/latest/downloads/paper-1.21.3-latest.jar
```

### Bước 2: Chạy Server Lần Đầu
```powershell
# Chạy server để tạo file eula.txt
java -Xmx2G -Xms1G -jar paper.jar --nogui

# Server sẽ tắt, sửa file eula.txt
# Mở eula.txt và đổi: eula=false -> eula=true
(Get-Content eula.txt) -replace 'eula=false', 'eula=true' | Set-Content eula.txt

# Chạy lại server
java -Xmx2G -Xms1G -jar paper.jar --nogui
```

### Bước 3: Copy Plugin Vào Server
```powershell
# Tắt server (gõ "stop" trong console)
# Copy plugin vào thư mục plugins
copy C:\dev\DynamicKeepInv\target\DynamicKeepInv-1.0.0.jar C:\dev\test-server\plugins\

# Chạy lại server
java -Xmx2G -Xms1G -jar paper.jar --nogui
```

### Bước 4: Vào Game Test

1. **Mở Minecraft** (Java Edition 1.19+)
2. **Multiplayer** → **Add Server**
   - Server Address: `localhost`
   - Tên: Test Server
3. **Join server**

### Bước 5: Test Plugin

```
# Trong game, mở chat (T) và gõ lệnh:

/op TênBạn                    # Cho bản thân quyền admin
/dki status                   # Xem trạng thái
/dki toggle                   # Test bật/tắt
/time set day                 # Chuyển sang ban ngày
/time set night               # Chuyển sang ban đêm

# Test keep inventory:
/kill                         # Chết xem có mất đồ không
```

### Bước 6: Test Đổi Ngôn Ngữ

```powershell
# Tắt server
# Sửa file plugins/DynamicKeepInv/messages.yml
# Đổi: language: vi -> language: en
# Khởi động lại server
java -Xmx2G -Xms1G -jar paper.jar --nogui
```

---

## Cách 2: Test Nhanh Với Unit Tests (Đang có lỗi)

Unit tests hiện tại bị lỗi MockBukkit, nhưng bạn có thể thử:

```powershell
cd C:\dev\DynamicKeepInv
mvn test
```

**Lưu ý**: MockBukkit tests đang bị lỗi "No jar file selected", nên khuyến nghị dùng Cách 1.

---

## Kiểm Tra Debug Logs

Khi test, xem file `logs/latest.log` trong server để debug:

```powershell
# Xem log realtime
Get-Content C:\dev\test-server\logs\latest.log -Wait -Tail 20
```

Hoặc bật debug mode trong `config.yml`:
```yaml
debug: true
```

---

## Các Tình Huống Test Quan Trọng

### ✅ Test 1: Plugin load thành công
```
[Server] [DynamicKeepInv] DynamicKeepInv is starting...
[Server] [DynamicKeepInv] DynamicKeepInv enabled!
```

### ✅ Test 2: Chuyển ngày → đêm
```
/time set 0      # Ban ngày: Keep Inventory = ON
/time set 13000  # Ban đêm: Keep Inventory = OFF
```

### ✅ Test 3: Lệnh status hiển thị đúng
```
/dki status
```
Phải thấy:
- Trạng thái plugin
- Cấu hình day/night
- Thông tin các world

### ✅ Test 4: Reload config
```
# Sửa config.yml (ví dụ: đổi check-interval)
/dki reload
# Kiểm tra setting mới có áp dụng không
```

### ✅ Test 5: Enable/Disable
```
/dki disable     # Plugin ngừng hoạt động
/time set night  # Keep inventory không đổi
/dki enable      # Plugin hoạt động lại
```

### ✅ Test 6: Song ngữ Việt-Anh
```
# Sửa messages.yml: language: en
/dki reload
/dki status      # Phải thấy tiếng Anh
```

---

## Troubleshooting

### Lỗi: Plugin không load
- Kiểm tra log: `logs/latest.log`
- Đảm bảo Paper version >= 1.19.4
- Đảm bảo Java >= 21

### Lỗi: Lệnh không hoạt động
```
/op TênBạn       # Phải có quyền admin
```

### Lỗi: Keep inventory không đổi
- Kiểm tra `enabled: true` trong config.yml
- Xem log có thông báo day/night không
- Thử `/dki status` xem time của world

### Lỗi: Không thấy log day/night
- Bật debug mode: `debug: true` trong config.yml
- Reload: `/dki reload`

---

## Quick Start Script

```powershell
# Script tự động setup server test
$serverDir = "C:\dev\test-server"

# Tạo thư mục
New-Item -ItemType Directory -Force -Path $serverDir
Set-Location $serverDir

# Download Paper (thay VERSION bằng version bạn muốn)
$version = "1.21.3"
Write-Host "Downloading Paper $version..."
Invoke-WebRequest -Uri "https://api.papermc.io/v2/projects/paper/versions/$version/builds/latest/downloads/paper-$version.jar" -OutFile "paper.jar"

# Chấp nhận EULA
"eula=true" | Out-File -FilePath "eula.txt" -Encoding ASCII

# Copy plugin
New-Item -ItemType Directory -Force -Path "plugins"
Copy-Item "C:\dev\DynamicKeepInv\target\DynamicKeepInv-1.0.0.jar" "plugins\"

Write-Host "Server ready! Run: java -Xmx2G -Xms1G -jar paper.jar --nogui"
```

Lưu script trên thành `setup-test-server.ps1` và chạy:
```powershell
.\setup-test-server.ps1
```
