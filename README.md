# Lylee Mobile Launcher

App Android chạy **Minecraft: Java Edition** (không phải Bedrock) cho server Lylee Cobblemon — bản đồng hành chính thức của [Lylee Launcher (PC)](https://github.com/silvadrag/lyleeproject).

Dự án được xây dựng dựa trên bản fork của **Fold Craft Launcher (FCL)**, tích hợp sâu vào hệ sinh thái backend và công cụ quản trị **LyleeAdminTool**.

Chi tiết kiến trúc, nhật ký phát triển và giải pháp kỹ thuật xem tại [`docs/PLAN.md`](docs/PLAN.md).

---

## ✨ Tính năng nổi bật

- 🎮 **Chơi Minecraft Java 1.21.1+ trên Android:** Hỗ trợ Fabric Loader, Cobblemon và các mod tối ưu hóa đồ họa (Sodium, Lithium, Iris...).
- 🔄 **Đồng bộ Modpack tự động (`LyleeCobblemonSync`):** Tự động so khớp hash và kích thước file từ Server Manifest, chỉ tải các file thiếu hoặc cập nhật.
- 📱 **Hỗ trợ đa kiến trúc (5 biến thể APK):**
  - `all` (Bản đa năng cho mọi thiết bị Android)
  - `arm64-v8a` (Tối ưu cho điện thoại Android 64-bit hiện đại)
  - `armeabi-v7a` (Điện thoại 32-bit đời cũ)
  - `x86` / `x86_64` (Giả lập PC / Chromebook / WSA)
- 🚀 **Tự động cập nhật In-App:** Kiểm tra và thông báo cập nhật bản mới trực tiếp từ backend qua `UpdateChecker`.
- ⚡ **Tối ưu hóa mạng di động (v1.3.2.11):** 
  - Timeout 30 giây chống rớt gói.
  - Tự động thử lại (Auto-Retry 5 lần) kèm khoảng nghỉ giãn cách (exponential backoff).
  - Giới hạn 3 - 6 luồng tải song song tránh nghẽn socket chip mạng di động.
  - Tự động ẩn IP/Port máy chủ (`[server]`) bảo vệ an toàn thông tin.
- 📰 **Banner tin tức trượt mượt mà:** Hiển thị thông báo, sự kiện từ server với bộ đệm ảnh chống giật lag.

---

## 🏗️ Kiến trúc hệ thống

```
┌────────────────────────────────────────────────────────┐
│               Lylee Mobile Launcher (FCL)              │
│  - Android UI (Kotlin/Java)                            │
│  - FakeFX & Caciocavallo Layer (HMCL Core)             │
│  - Modpack Sync & In-app Update Checker                │
└───────────────────────────┬────────────────────────────┘
                            │ HTTPS (JSON API & Manifest)
                            ▼
┌────────────────────────────────────────────────────────┐
│            Cloudflare Worker Proxy (CDN)               │
│     lylee-launcher-api.silvadrag2006.workers.dev       │
└───────────────────────────┬────────────────────────────┘
                            │ HTTP
                            ▼
┌────────────────────────────────────────────────────────┐
│         Backend Server (16GB RAM / 5 vCPU)             │
│  - Minecraft Server 1.21.1 (Cobblemon)                 │
│  - Fabric Mod API: fabric-lyleelauncherAPI-mod-1.21.1  │
│  - MySQL Database + Static File Server (/files/...)    │
└────────────────────────────────────────────────────────┘
```

---

## 📌 Trạng thái dự án

- [x] **Chọn base fork:** Hoàn tất chọn Fold Craft Launcher (FCL) vì kiến trúc mở, UI hiện đại và khả năng patch mod tốt.
- [x] **Build & kiểm thử:** Đã build và chạy mượt mà trên các thiết bị Android thật (ARM64, Android 14/15/16).
- [x] **Tùy biến UI & Branding:** Hoàn thiện giao diện phong cách Lylee Launcher, đồng bộ với bản PC.
- [x] **Tích hợp Backend API:** Kết nối trơn tru với `fabric-lyleelauncherAPI-mod-1.21.1` và Cloudflare Worker.
- [x] **Đồng bộ Modpack Cobblemon:** Cơ chế tải và kiểm tra toàn vẹn hash file theo Server Manifest.
- [x] **In-App Update System:** Xuất bản và phân phối APK qua tab Mobile của `LyleeAdminTool`.
- [x] **Vá lỗi mạng & Ổn định hóa (v1.3.2.11):** Sửa triệt để lỗi `SocketTimeoutException`, thêm retry, bảo vệ IP.

---

## 🔨 Hướng dẫn Build từ mã nguồn

### Yêu cầu:
- **JDK:** OpenJDK 21 (ví dụ Eclipse Adoptium JDK 21).
- **Android SDK:** Compile SDK 36, Target SDK 36.

### Lệnh build (PowerShell / Terminal):
```powershell
# Đặt biến môi trường Java 21
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Build bản All-in-one:
.\gradlew.bat :FCL:assembleRelease -Darch=all

# Build riêng bản ARM64:
.\gradlew.bat :FCL:assembleRelease -Darch=arm64
```
Các file APK xuất ra tại thư mục `FCL/build/outputs/apk/release/` hoặc `release-collected/`.
