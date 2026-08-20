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
- Android SDK Command-line Tools (tải trực tiếp từ `dl.google.com`, đã verify SHA-256) — **cài ở `D:\Android\Sdk`, KHÔNG phải `%LOCALAPPDATA%` mặc định** (xem lý do ở mục 7 — ổ C: gần đầy). `ANDROID_HOME`/`ANDROID_SDK_ROOT`/`GRADLE_USER_HOME` đều đặt user-level env var trỏ sang D:.
- Qua công cụ mới `android` CLI (thay thế `sdkmanager` cũ, cú pháp gói dùng `/` thay vì `;`, VD `ndk/29.0.14206865` không phải `ndk;29.0.14206865`): `platform-tools`, `platforms/android-36`.
- 3 bản NDK khác nhau (mỗi project/submodule pin 1 bản riêng, không dùng chung được): `29.0.14206865` (PojavLauncher chính), `28.2.13676358` (submodule NG-GL4ES của Pojav), `27.3.13750724`/`27.0.12077973` (FCLauncher — xem mục 7 về lý do cài tay bản NDK 28.2 thay vì qua trình quản lý).

## 6. Trạng thái build thật (cập nhật liên tục)

- ✅ **PojavLauncher: BUILD SUCCESSFUL (2026-08-21)** — `./gradlew :app_pojavlauncher:assembleDebug` chạy sạch trong **4m12s**, ra 2 file APK thật (`app_pojavlauncher-full-debug.apk` 106MB, `app_pojavlauncher-noruntime-debug.apk` 78MB) tại `app_pojavlauncher/build/outputs/apk/`.
- ✅ **Fold Craft Launcher: BUILD SUCCESSFUL (2026-08-21)** — biến thể `debug` mặc định FAIL vì cần khóa ký thật (`SigningConfig "FCLKey" is missing required property "storePassword"` — file `../key-store.jks` cần mật khẩu ngoài, không có trong repo, đúng như vậy vì đó phải là khóa release thật của team FCL). Repo có sẵn 1 biến thể riêng cho việc build/test không cần khóa thật: `./gradlew :FCL:assembleFordebug` (dùng `debug-key.jks` kèm mật khẩu hardcode `"FCL-Debug"` ngay trong `build.gradle.kts`) — chạy `1m06s` (tái dùng cache compile native từ lần chạy `debug` bị fail), ra `FCL-fordebug-1.3.2.7-all.apk` **341MB** (biến thể "all" gộp mọi kiến trúc CPU — bản release thật chắc chắn có APK tách riêng theo ABI nhỏ hơn nhiều, chưa kiểm tra).
- ⚠️ Cả 2 mới xác nhận **BUILD được**, CHƯA cài lên máy/emulator thật để xác nhận **CHẠY được** — đó là bước tiếp theo cho bên nào được chọn.

### So sánh sau khi build thật cả 2 (khác hẳn so với chỉ đọc mô tả)

| | PojavLauncher | Fold Craft Launcher |
|---|---|---|
| Thời gian build sạch (máy này) | 4m12s | 6m24s (lần đầu, có compile native) |
| Kích thước APK debug | 78-106MB (2 biến thể full/noruntime) | 341MB (biến thể "all", chưa tối ưu theo ABI) |
| LICENSE thật trong repo | **Không thấy** — badge README claim LGPLv3 nhưng không có file LICENSE ở gốc, cần tự xác minh trên GitHub | **Có** file `LICENSE` thật, nội dung GPLv3 đầy đủ, rõ ràng |
| Trở ngại build gặp phải | Symlink Unix bị hỏng khi checkout trên Windows (xem mục 7 mục 6) — phải tự sửa bằng NTFS junction | Không gặp lỗi kiến trúc nào — chỉ nhầm biến thể build (lỗi thao tác của mình, không phải lỗi project) |
| Ngôn ngữ/code | Java + C/C++ thuần, 1 file `CMakeLists.txt` gọn | Kotlin + Java + C, dùng Gradle version catalog (`libs.versions.toml`) — cách tổ chức hiện đại hơn |
| Ghi chú code | Tiếng Anh | 1 phần comment tiếng Trung (team maintain chính có vẻ là người Trung Quốc) — cần lưu ý khi đọc sâu/báo issue upstream |
| Biến thể build sẵn có | `full` (kèm runtime) / `noruntime` | `release` (khóa thật) / `debug` (khóa thật) / **`fordebug`** (khóa giả lập, dành đúng cho việc này) |

**Nhận xét sơ bộ** (chưa phải quyết định cuối — cần chạy thử thật trên máy/emulator trước): FCL có tổ chức Gradle hiện đại hơn (version catalog, tách biến thể build rõ ràng có sẵn "fordebug" cho đúng nhu cầu dev) và license rõ ràng hơn hẳn (có file LICENSE thật). PojavLauncher có build nhanh hơn, APK gọn hơn, nhưng vướng license chưa xác minh + lỗi symlink phải tự vá. Cả 2 đều đủ trưởng thành để cân nhắc nghiêm túc.

## 7. Nhật ký dựng môi trường build thật (2026-08-21) — các lỗi gặp + cách sửa

Loạt lỗi thật gặp phải khi build lần đầu trên máy Windows chưa từng làm Android bao giờ — ghi lại chi tiết vì admin tool Java (mod backend) cũng chạy trên máy này, dễ tái diễn khi build lại sau này hoặc trên máy khác:

1. **`local.properties` viết path Windows kiểu `C:\Users\...` làm hỏng SDK path** — dấu `\U` trong `\Users` bị Java Properties parser hiểu nhầm thành escape unicode (`\uXXXX`), AGP báo lỗi mơ hồ `IOException: The filename, directory name, or volume label syntax is incorrect` ngay từ bước "configure project", trỏ sâu vào `SdkLocator.kt` chứ không phải lỗi thật của Pojav. **Sửa**: luôn dùng dấu `/` trong `local.properties` (`sdk.dir=D:/Android/Sdk`), Gradle/AGP trên Windows chấp nhận cả 2 kiểu nhưng `/` an toàn tuyệt đối, không cần escape.
2. **Ổ C: gần đầy (99%, chỉ còn 1.9GB) giữa chừng cài đặt** — Android Studio + SDK cmdline-tools + NDK 795MB + Gradle distribution/cache đều mặc định đổ vào `%LOCALAPPDATA%`/`%USERPROFILE%\.gradle` (ổ C:). Gradle build fail nửa chừng với `IOException: There is not enough space on the disk`. **Sửa**: chuyển hẳn `ANDROID_HOME` sang `D:\Android\Sdk` và `GRADLE_USER_HOME` sang `D:\gradle-home` (ổ D: rảnh 46GB) — dọn lại được ~14GB trên C:. **Bài học**: kiểm tra `df -h` TRƯỚC khi cài bất cứ gì nặng, không đợi tới lúc build fail mới biết.
3. **Windows MAX_PATH (260 ký tự) làm hỏng `git index` giữa chừng checkout** — clone lần đầu vào đường dẫn scratch quá sâu (`...\AppData\Local\Temp\claude\...\scratchpad\...`, đã hơn 140 ký tự trước khi tới path riêng của Pojav) khiến 1 số file "Filename too long" lúc checkout, để lại `.git/index` bị lỗi (`git status` báo TOÀN BỘ file bị "deleted" dù file vẫn còn trên đĩa). **Sửa**: `git config --global core.longpaths true` + luôn clone vào đường dẫn ngắn (`D:\dev\...`), không dùng thư mục scratch/temp sâu cho việc clone repo lớn.
4. **`android` CLI mới âm thầm bỏ qua cài NDK nếu license chưa accept — không báo lỗi rõ** — khác `sdkmanager` cũ (hỏi `Accept? (y/N)` rõ ràng), công cụ `android sdk install` mới chạy xong "thành công" (exit code không nhất quán) nhưng thư mục NDK trống trơn (chỉ có `.installer/`, không có `source.properties`) — build sau đó báo `[CXX1101] NDK ... did not have a source.properties file`. Phát hiện qua `sdkmanager --licenses` thấy còn khoản chưa accept. **Sửa**: `yes | sdkmanager --licenses` chấp nhận hết TRƯỚC khi cài package mới qua công cụ nào cũng được.
5. **1 bản NDK (28.2.13676358) vẫn cài lỗi dù đã accept license — dùng cách tải tay** — `sdkmanager`/`android sdk install` liên tục fail giữa chừng riêng với đúng bản này (không rõ nguyên nhân gốc, có thể do CDN tạm thời). **Sửa vòng qua**: tải thẳng file zip từ `https://dl.google.com/android/repository/android-ndk-r28c-windows.zip` (link + SHA1 lấy từ [trang chính thức NDK Unsupported Downloads](https://github.com/android/ndk/wiki/Unsupported-Downloads), đã verify checksum khớp trước khi dùng), tự giải nén vào đúng `D:\Android\Sdk\ndk\28.2.13676358\`.
6. **Symlink Unix thật bị git-on-Windows checkout thành file text vô dụng** — PojavLauncher dùng symlink thật (`core.symlinks` mặc định `false` trên Windows) để nối `app_pojavlauncher/src/main/jni/{glfw,mojoexec,sdl}` sang 3 submodule ở gốc repo. Trên Windows, mỗi "symlink" đó chỉ còn là file text 15-20 byte chứa CHUỖI đường dẫn (VD `../../../../glfw/`), không phải liên kết thật — CMake báo `add_subdirectory given source "glfw" which is not an existing directory`. **Sửa**: xóa 3 file placeholder đó, tạo lại bằng NTFS junction thật (`New-Item -ItemType Junction`, KHÔNG cần quyền admin, khác symlink `mklink /D` cần admin) trỏ đúng tới submodule thật. Cần soát lại y hệt nếu FCL cũng dùng pattern symlink tương tự.
7. **Java compiler 21 cảnh báo (không phải lỗi) khi compile source/target Java 8** — Pojav vẫn target bytecode Java 8 dù build bằng JDK 21, ra warning "deprecated support" — không chặn build, bỏ qua được.

**Kết luận chung**: build APK debug PojavLauncher hoàn toàn khả thi trên máy dev thường (không cần Linux/Mac như nhiều hướng dẫn ngầm giả định), nhưng cần đúng thứ tự sửa 6 lỗi trên — nếu làm lại từ đầu (máy khác/sau khi dọn môi trường), làm theo đúng thứ tự này sẽ nhanh hơn nhiều so với lần đầu dò mù.

## 8. Việc cần làm tiếp

- [x] Cài Android Studio + SDK cmdline-tools + đúng platform/NDK cả 2 project cần.
- [x] Clone PojavLauncher + FCL (`--recurse-submodules`) vào scratch, đọc cấu trúc thật — xem mục 2.
- [x] Build thành công APK debug cho CẢ 2 (`app_pojavlauncher-full-debug.apk` + `FCL-fordebug-1.3.2.7-all.apk`) — xem mục 6.
- [ ] Cài cả 2 APK lên máy/emulator thật, xác nhận CHẠY được (mở app, không crash, thấy màn hình chính) — build thành công chỉ là bước đầu, đây mới là phép thử thật để chọn hướng.
- [ ] Đọc kỹ LICENSE đúng tag sẽ fork của cả 2 (xác nhận nghĩa vụ copyleft cụ thể) — Pojav cần xác minh lại trực tiếp trên GitHub vì không thấy file LICENSE thật, FCL đã có sẵn LICENSE rõ ràng.
- [ ] **Chốt hướng** (cần quyết định của người dùng, không tự chọn) — dựa trên mục 6 (so sánh) + kết quả chạy thử thật.
- [ ] Sau khi chốt: đưa mã nguồn fork thật vào đúng nhánh `explore/...` tương ứng (hiện source chỉ đang nằm ở `D:\dev\pojav`/`D:\dev\fcl` ngoài git, chưa commit) — cân nhắc giữ lịch sử git gốc (thêm remote + merge) thay vì copy phẳng, để dễ kéo update từ upstream sau này.
