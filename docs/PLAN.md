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

⚠️ **Phát hiện lúc clone thử (2026-08-21)**: repo PojavLauncher (nhánh mặc định `v3_openjdk`) **không có file `LICENSE` ở gốc repo** — badge LGPLv3 trong README trỏ tới file đó nhưng GitHub API (`gh api repos/.../license`) cũng không nhận diện được license nào. Cần tự mở thẳng trang GitHub xác nhận lại (không tin riêng badge) trước khi coi LGPLv3 là chính thức.

**Cách quyết định**: dựng thử CẢ 2 qua 2 nhánh riêng (`explore/pojavlauncher-fork` và `explore/fcl-fork`, xem mục 4) — build thành công, chạy thử trên máy/emulator, đọc code thật để đánh giá "dễ tùy biến" tới đâu — rồi mới chọn 1 nhánh để đi tiếp làm `main`. Tránh chốt theo cảm tính sớm khi chưa build thử.

### Cấu trúc thật của PojavLauncher (đã clone + đọc, khác research doc ban đầu)

Research doc (mục 1) mô tả đơn giản hóa 5 package (`authenticator/customcontrols/fragments/tasks/utils`) — thực tế package `net.kdt.pojavlaunch` có **~28 package con** (thêm `colorselector, contracts, downloader, extra, game, imgcropper, instances, lifecycle, mirrors, modloaders, multirt, plugins, prefs, profiles, progresskeeper, render, scoped, services, value`...) — dự án trưởng thành/phức tạp hơn nhiều so với mô tả tóm lược. Dùng git submodule thật (không phải thư mục thường) cho 5 phần native: `glfw`, `mojoexec`, `sdl` (từ org `MojoLauncher`), `MobileGlues`, `NG-GL4ES` (từ `MobileGL-Dev`/`TeamPojavLauncher`) — bắt buộc `git submodule update --init --recursive` sau khi clone, không tự có sẵn.

**Thông số build xác nhận từ `app_pojavlauncher/build.gradle` thật** (không đoán):
- `compileSdk = 36`, `targetSdkVersion 36`, `minSdkVersion 21`
- `ndkVersion "29.0.14206865"` (khớp đúng 1 bản NDK có sẵn qua `android sdk list --all "ndk*"`, không phải bản mới nhất)

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

Máy dev (Windows) **trước đây chưa có** Android Studio/SDK/NDK/Gradle (chỉ có sẵn JDK cho phần mod Java backend). Đã cài **2026-08-21**:
- Android Studio qua `winget install Google.AndroidStudio` — chỉ cài IDE, KHÔNG tự có SDK/NDK.
- `poppler` (qua winget, id `oschwartz10612.Poppler`) — công cụ phụ để đọc PDF dạng ảnh/scan (không liên quan trực tiếp launcher, nhưng cần để đọc tài liệu tham khảo dạng slide).
- Android SDK Command-line Tools (tải trực tiếp từ `dl.google.com`, đã verify SHA-256) — cài vào `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest`, đặt `ANDROID_HOME`/`ANDROID_SDK_ROOT` (user-level env var).
- Qua công cụ mới `android` CLI (thay thế `sdkmanager` cũ, cú pháp gói dùng `/` thay vì `;`, VD `ndk/29.0.14206865` không phải `ndk;29.0.14206865`): `platform-tools`, `platforms/android-36`, `ndk/29.0.14206865` (khớp đúng yêu cầu thật của PojavLauncher, xem mục 2).

## 6. Việc cần làm tiếp

- [x] Cài Android Studio + SDK cmdline-tools + đúng platform/NDK PojavLauncher cần.
- [x] Clone PojavLauncher (`--recurse-submodules`) vào scratch, đọc cấu trúc thật — xem mục 2.
- [ ] Clone FCL (`--recurse-submodules`), đọc cấu trúc thật, so `build.gradle` lấy đúng SDK/NDK version cần (khác Pojav, chưa cài).
- [ ] Build thử APK rỗng cho CẢ 2 (đang làm dở PojavLauncher trước, xem log build thật thay vì đoán).
- [ ] Đọc kỹ LICENSE đúng tag sẽ fork của cả 2 (xác nhận nghĩa vụ copyleft cụ thể) — Pojav cần xác minh lại trực tiếp trên GitHub vì không thấy file LICENSE thật.
- [ ] So sánh, chốt hướng — cập nhật mục 2 + 4 tài liệu này.
