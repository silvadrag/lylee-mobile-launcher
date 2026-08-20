# Kế hoạch — Lylee Mobile Launcher

> Bắt đầu 2026-08-21. Tài liệu này đóng vai trò như `PROGRESS.md`/`FUTURE_PLAN.md` bên [Lylee Launcher (PC)](https://github.com/silvadrag/lyleeproject) — ghi lại quyết định kiến trúc + lý do, cập nhật dần theo tiến độ thật.

## 1. Vì sao không viết từ đầu

Chạy Minecraft Java Edition trên Android đòi hỏi 4 tầng, khác hẳn launcher PC (chỉ cần gọi `java -jar`):

```
1. Android UI Layer (Kotlin/Java) — Account Auth | Version Downloader | Touch Control UI
2. Launcher Core & Native Bridge   — Launch Process | Lib Loading | JNI JRE Invoker
3. Mobile OpenJDK (ARM)            — JVM Runtime | Minecraft Process | Mod Loaders
4. Graphics & Input Bridge         — LWJGL/GLFW Stub | GL4ES/Zink (OpenGL → GLES/Vulkan) | OpenAL
```

Tầng 3-4 (build lại OpenJDK cho ARM, dịch Desktop OpenGL sang OpenGL ES/Vulkan qua GL4ES/Zink, viết JNI bridge cho LWJGL/GLFW) là bài toán kỹ thuật mất hàng tháng-năm nếu tự viết. **Quyết định: fork 1 project mã nguồn mở đã hoàn thiện 2 tầng dưới, chỉ tự viết/tùy biến tầng UI (1) và có thể một phần Core (2).**

(Nghiên cứu đầy đủ, kèm code mẫu Kotlin cho touch controls/gesture detector/control editor: [`docs/gemini-research-pojavlauncher-architecture.pdf`](gemini-research-pojavlauncher-architecture.pdf).)

## 2. Chọn base để fork — CHƯA CHỐT

| | [PojavLauncher](https://github.com/TeamPojavLauncher/PojavLauncher) | [Fold Craft Launcher (FCL)](https://github.com/FCL-Team/FoldCraftLauncher) |
|---|---|---|
| Ngôn ngữ | Java, C/C++ (JNI) | Kotlin, Java, C++ |
| Điểm mạnh | Hoàn thiện nhất, cộng đồng lớn nhất, tài liệu/issue nhiều nhất để tra khi gặp bug | Giao diện hiện đại (Material You), theo nghiên cứu Gemini là "cấu trúc code sạch hơn" |
| License | **LGPLv3** (bản cũ hơn là GPLv3 — có đổi giữa các version, cần xác nhận đúng bản/tag sẽ fork) | **GPLv3** |

**Cả 2 đều copyleft** — nếu fork + phát hành app cho người chơi dùng, **bắt buộc giữ mã nguồn mở** (không thể âm thầm đóng gói lại thành closed-source). Cần đọc kỹ LICENSE của đúng version/tag định fork trước khi quyết định — LGPLv3 (Pojav bản mới) lỏng hơn GPLv3 (FCL) một chút về việc link thư viện, nhưng cả 2 đều yêu cầu công khai source code bản đã sửa.

**Cách quyết định**: dựng thử CẢ 2 qua 2 nhánh riêng (`explore/pojavlauncher-fork` và `explore/fcl-fork`, xem mục 4) — build thành công, chạy thử trên máy/emulator, đọc code thật để đánh giá "dễ tùy biến" tới đâu — rồi mới chọn 1 nhánh để đi tiếp làm `main`. Tránh chốt theo cảm tính SỚM khi corn chưa build thử.

## 3. Mô hình tích hợp dự tính (Cách 2 trong nghiên cứu — Xây UI riêng + gọi Core)

```
┌─────────────────────────────────────────────┐
│ UI Lylee (Module app riêng)                  │
│ - Server list đồng bộ theme PC launcher      │
│ - Đăng nhập (dùng chung backend/JWT hiện có) │
│ - Modpack Cobblemon (tự động, không cần chọn)│
└──────────────────┬────────────────────────────┘
                    │ dependency lên module core (đổi
                    │ application → library)
┌──────────────────▼────────────────────────────┐
│ Pojav/FCL Core (Module thư viện)             │
│ - JRE Extractor, Native Bridge, Touch Engine │
└───────────────────────────────────────────────┘
```

Ưu tiên vì: giữ nguyên UI/branding Lylee (đồng bộ launcher PC), tách biệt rõ "code của mình" khỏi "code fork" — dễ merge update từ upstream sau này hơn so với sửa trực tiếp vào UI gốc của Pojav/FCL.

**Chưa quyết định chắc** — có thể đổi sang Cách 1 (fork thẳng, sửa UI tại chỗ) nếu mô hình library-module gặp trở ngại kỹ thuật thật (VD Gradle module graph của Pojav/FCL không tách library dễ dàng như dự tính). Đánh giá lại sau khi build thử ở mục 2.

## 4. Cấu trúc nhánh (branch)

- `main` — chỉ chứa tài liệu kế hoạch (thư mục này) cho tới khi chọn xong hướng ở mục 2. KHÔNG code thử nghiệm trực tiếp trên `main`.
- `explore/pojavlauncher-fork` — clone/fork PojavLauncher, build thử, đọc cấu trúc module thật.
- `explore/fcl-fork` — clone/fork Fold Craft Launcher, build thử, đọc cấu trúc module thật.
- Sau khi chọn xong 1 hướng: merge nhánh đó vào `main`, xóa nhánh còn lại (hoặc giữ làm tham khảo, tùy lúc đó).

## 5. Môi trường dev

Máy dev (Windows) **trước đây chưa có** Android Studio/SDK/NDK/Gradle (chỉ có sẵn JDK cho phần mod Java backend). Đã cài Android Studio (kèm SDK Manager) qua `winget install Google.AndroidStudio` ngày 2026-08-21 — cần tự mở Android Studio ít nhất 1 lần để hoàn tất cài SDK/NDK components qua wizard (winget chỉ cài IDE, chưa tự động tải SDK/NDK components).

## 6. Việc cần làm tiếp

- [ ] Mở Android Studio lần đầu, hoàn tất SDK/NDK setup wizard.
- [ ] Clone PojavLauncher vào `explore/pojavlauncher-fork`, thử build APK rỗng.
- [ ] Clone FCL vào `explore/fcl-fork`, thử build APK rỗng.
- [ ] Đọc kỹ LICENSE đúng tag sẽ fork của cả 2 (xác nhận nghĩa vụ copyleft cụ thể).
- [ ] So sánh, chốt hướng — cập nhật mục 2 + 4 tài liệu này.
