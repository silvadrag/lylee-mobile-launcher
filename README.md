# Lylee Mobile Launcher

App Android chạy **Minecraft: Java Edition** (không phải Bedrock) cho server Lylee Cobblemon — bản đồng hành của [Lylee Launcher (PC)](https://github.com/silvadrag/lyleeproject).

> Dự án mới bắt đầu (2026-08-21), chưa có code chạy được. Xem [`docs/PLAN.md`](docs/PLAN.md) để biết kiến trúc dự tính, hướng tiếp cận, và trạng thái từng nhánh thử nghiệm.

## Vì sao khác hẳn launcher PC

Trên PC, launcher chỉ cần tải file rồi gọi `java -jar`. Android không hỗ trợ chạy JVM/Desktop OpenGL kiểu đó — cần cả 1 tầng JRE build riêng cho ARM + tầng dịch đồ họa (OpenGL → OpenGL ES/Vulkan) + JNI bridge. Đây là bài toán engineering **rất khác** so với launcher PC, xem chi tiết trong `docs/PLAN.md`.

## Trạng thái

- [ ] Chọn base fork (PojavLauncher hay Fold Craft Launcher)
- [ ] Dựng được build rỗng chạy trên máy thật/emulator
- [ ] Tùy biến UI theo phong cách Lylee (đồng bộ màu/branding với launcher PC)
- [ ] Kết nối vào cùng backend (`fabric-lyleelauncherAPI-mod-1.21.1`) — tài khoản, whitelist, danh sách server
