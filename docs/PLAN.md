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

(Nghiên cứu đầy đủ — kiến trúc PojavLauncher, code mẫu Kotlin cho touch controls/gesture detector/control editor, PHẦN SAU (từ trang 15) là phân tích so sánh Pojav/FCL + gợi ý triển khai riêng cho FCL (đồng bộ modpack Cobblemon tự động, màn hình chính Jetpack Compose Material 3, chuyển cảnh sang Game Activity): [`docs/gemini-research-pojavlauncher-fcl-comparison.pdf`](gemini-research-pojavlauncher-fcl-comparison.pdf) — CHƯA review kỹ/chưa chạy thử, chỉ là gợi ý tham khảo lúc bắt tay code thật.)

## 2. Chọn base để fork — ĐÃ CHỐT: Fold Craft Launcher (FCL)

> **Quyết định cuối (2026-08-21)**: chọn **FCL**. Lý do: (1) kiến trúc multi-module tách UI khỏi Core rõ ràng, đúng mô hình dự tính ở mục 3; (2) license GPLv3 rõ ràng, có file thật, không vướng vùng xám như Pojav (xem mục "Xác minh license" bên dưới); (3) UI/UX đã có sẵn đầu tư kỹ (xem mục 8), gần với mức người dùng cuối mong đợi hơn. Nhánh `explore/pojavlauncher-fork` **giữ lại làm tham khảo** (đã build/chạy thành công, không xóa), không phát triển tiếp. Đã gỡ APK PojavLauncher khỏi điện thoại test (máy yếu, không cần giữ app không dùng).
>
> Quyết định có tham khảo phân tích từ Gemini (dùng [`docs/ai-comparison-prompt.md`](ai-comparison-prompt.md) làm brief) — bản thân người dùng đọc + xác nhận, không phải AI tự chọn thay.

| | [PojavLauncher](https://github.com/TeamPojavLauncher/PojavLauncher) | [Fold Craft Launcher (FCL)](https://github.com/FCL-Team/FoldCraftLauncher) |
|---|---|---|
| Ngôn ngữ | Java, C/C++ (JNI) | Kotlin, Java, C++ |
| Điểm mạnh | Hoàn thiện nhất, cộng đồng lớn nhất, tài liệu/issue nhiều nhất để tra khi gặp bug | Giao diện hiện đại (Material You), theo nghiên cứu Gemini là "cấu trúc code sạch hơn" |
| License | **LGPLv3** (bản cũ hơn là GPLv3 — có đổi giữa các version, cần xác nhận đúng bản/tag sẽ fork) | **GPLv3** |

**Cả 2 đều copyleft** — nếu fork + phát hành app cho người chơi dùng, **bắt buộc giữ mã nguồn mở** (không thể âm thầm đóng gói lại thành closed-source). Cần đọc kỹ LICENSE của đúng version/tag định fork trước khi quyết định — LGPLv3 (Pojav bản mới) lỏng hơn GPLv3 (FCL) một chút về việc link thư viện, nhưng cả 2 đều yêu cầu công khai source code bản đã sửa.

⚠️ **Phát hiện lúc clone thử (2026-08-21)**: repo PojavLauncher (nhánh mặc định `v3_openjdk`) **không có file `LICENSE` ở gốc repo** — badge LGPLv3 trong README trỏ tới file đó nhưng GitHub API (`gh api repos/.../license`) cũng không nhận diện được license nào.

**Xác minh sâu hơn (2026-08-21, tự tay đào lịch sử git thật + đối chiếu qua `gh api`, không đoán)**: repo TỪNG có file `LICENSE` (LGPLv3, nội dung đầy đủ) — bị xóa trong đúng 1 commit **`4cac3d5fb "[Full Rewrite] MJLauncher --> PojavLauncher"`** (tác giả `WOOD6563`, xác nhận commit này có thật trên GitHub qua `gh api repos/TeamPojavLauncher/PojavLauncher/commits/4cac3d5fb...`), và chưa từng được thêm lại tới giờ. `gh api repos/.../TeamPojavLauncher/PojavLauncher` xác nhận `default_branch = v3_openjdk` và trường `license` trả về rỗng — tức đây là tình trạng THẬT của nhánh chính hiện tại, không phải do mình clone thiếu hay cache lỗi thời.

Về pháp lý: KHÔNG có license = mặc định "giữ toàn quyền" (theo luật bản quyền quốc tế Berne Convention) — nghĩa là kỹ thuật thuần túy chưa ai cấp phép fork/sửa/phát tán, dù gần như chắc chắn chỉ là quên thêm lại file (dự án có hàng trăm fork công khai, không collision gì thực tế xảy ra). Nếu sau này vẫn muốn quay lại hướng Pojav, có thể fork từ đúng commit/tag TRƯỚC `4cac3d5fb` (còn LICENSE LGPLv3 thật) thay vì nhánh `v3_openjdk` hiện tại để có giấy phép rõ ràng.

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

- `main` — tài liệu kế hoạch (thư mục này).
- `explore/fcl-fork` — **hướng đã chọn, đang phát triển tiếp** — mã nguồn FCL đầy đủ (giữ lịch sử git gốc, `git remote` tên `upstream-fcl` trỏ về `FCL-Team/FoldCraftLauncher`).
- `explore/pojavlauncher-fork` — **giữ lại làm tham khảo, KHÔNG phát triển tiếp** — đã build/chạy thành công, có thể quay lại nếu FCL gặp trở ngại kỹ thuật nghiêm trọng sau này. `git remote` tên `upstream-pojav`.
- Việc còn lại (mục 9): merge `explore/fcl-fork` vào `main` khi đã ổn định đủ để coi là "chính thức" (không cần vội — cứ code trên `explore/fcl-fork` cho tới khi có bản demo UI Lylee thật sự chạy được).

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
8. **FCL: build biến thể `debug`/`release` mặc định FAIL vì thiếu khóa ký thật** — `SigningConfig "FCLKey" is missing required property "storePassword"` (đọc từ `../key-store.jks`, mật khẩu không có trong repo — đúng vì đó là khóa release thật của team FCL, không nên có sẵn). **Sửa**: build đúng biến thể `./gradlew :FCL:assembleFordebug` — dùng `debug-key.jks` kèm mật khẩu hardcode `"FCL-Debug"` ngay trong `build.gradle.kts`, dành riêng cho dev/test không cần khóa thật.
9. **Kết nối ADB qua USB với điện thoại Samsung thật — vướng 3 lớp khác nhau, không phải chỉ thiếu driver**: (1) driver Google USB (`extras;google;usb_driver`) **không hỗ trợ Samsung** (VID `04E8` không có trong file `.inf` — đã tự kiểm tra, đúng như tài liệu Google ghi "chỉ dành cho thiết bị Google"); (2) `sdkmanager` cũng silent-fail y hệt lỗi NDK ở mục 5 khi tải gói này — phải tải tay `https://dl.google.com/android/repository/usb_driver_r13-windows.zip` rồi `pnputil /add-driver ... /install`; (3) **nguyên nhân THẬT sự** hóa ra là điện thoại chưa thực sự bật "Gỡ lỗi USB" trong Tùy chọn nhà phát triển (dù tưởng đã bật) — driver Samsung chính chủ (`ssudbus.inf`, cài qua Smart Switch) chỉ bind được và lộ ra `Samsung Android Interface` (class `AndroidUsbDeviceClass`, PID kèm `&ADB`) SAU KHI bật đúng công tắc đó. **Bài học**: khi driver đã đúng mà Device Manager vẫn "Unknown", nghi ngờ trước tiên là điện thoại chưa thật sự phát tín hiệu ADB (kiểm tra PID hiện tại của thiết bị trong Device Manager có khớp PID "...+ADB" trong file `.inf` của driver hay không) — đừng chỉ đổi driver liên tục.

**Kết luận chung**: build APK debug cho CẢ 2 project + chạy thật trên điện thoại Android thật (không cần Linux/Mac như nhiều hướng dẫn ngầm giả định) hoàn toàn khả thi trên máy dev Windows thường — nhưng cần đúng thứ tự sửa 9 lỗi trên. Làm lại từ đầu (máy khác/sau khi dọn môi trường) theo đúng thứ tự này sẽ nhanh hơn nhiều so với lần đầu dò mù.

## 8. Kết quả chạy thật trên điện thoại (2026-08-21)

✅ **Cả 2 app chạy ổn định trên Samsung Galaxy A50s (SM-A507FN, Android thật qua USB/adb) — không app nào crash:**
- **PojavLauncher** — vào thẳng `LauncherActivity`, giao diện đầy đủ tiếng Việt (Wiki/Discord, Tùy chỉnh điều khiển, Thực thi tệp .jar, Mở thư mục trò chơi), đã chọn sẵn Minecraft 1.12.2, nút "CHƠI" sẵn sàng.
- **FCL** — splash screen đẹp (nền động làng Minecraft ban đêm), hộp thoại xin quyền + màn hình chào mừng/EULA đều dịch tiếng Việt đầy đủ, nút "Đồng ý và Tiếp tục" — UI đầu tư kỹ hơn hẳn Pojav ngay từ bước onboarding đầu tiên.

Đây là bằng chứng mạnh nhất tới giờ — cả 2 không chỉ BUILD được mà còn THẬT SỰ CHẠY được trên phần cứng thật, không phải chỉ lý thuyết.

## 9. Việc cần làm tiếp

- [x] Cài Android Studio + SDK cmdline-tools + đúng platform/NDK cả 2 project cần.
- [x] Clone PojavLauncher + FCL (`--recurse-submodules`) vào scratch, đọc cấu trúc thật — xem mục 2.
- [x] Build thành công APK debug cho CẢ 2 (`app_pojavlauncher-full-debug.apk` + `FCL-fordebug-1.3.2.7-all.apk`) — xem mục 6.
- [x] Cài cả 2 APK lên máy thật, xác nhận CHẠY được (mở app, không crash, thấy màn hình chính) — xem mục 8.
- [x] Đọc kỹ LICENSE thật của Pojav (đào lịch sử git + `gh api`, không đoán) — xem mục 2, kết luận: KHÔNG có license hiệu lực trên nhánh mặc định hiện tại (bị xóa 1 commit, chưa thêm lại). FCL có LICENSE GPLv3 rõ ràng.
- [x] Nhờ Gemini phân tích/so sánh (dùng [`docs/ai-comparison-prompt.md`](ai-comparison-prompt.md)) — khuyến nghị FCL, người dùng đọc + tự quyết định chọn theo.
- [x] **Chốt hướng: Fold Craft Launcher (FCL)** — xem mục 2. Đã gỡ APK PojavLauncher khỏi điện thoại test.
- [x] Đưa mã nguồn cả 2 fork thật vào đúng nhánh `explore/...` tương ứng, giữ nguyên lịch sử git gốc qua `git remote add` + `merge --allow-unrelated-histories` (không copy phẳng) — cả 2 nhánh đã push lên GitHub thành công.
- [ ] Test thật mod Cobblemon (Forge/Fabric) trên APK FCL, đúng chip Galaxy A50s (Exynos 9611/Mali-G72) — Gemini gợi ý làm trước khi chốt, nhưng người dùng đã quyết định chốt FCL trước khi làm bước này; để dành làm sớm trong lúc bắt đầu tùy biến, phòng khi phát hiện lỗi render GPU Mali cần đổi renderer (VirGL/Zink/Holy GL4ES — xem `docs/ai-comparison-prompt.md` phần trả lời Gemini để tham khảo gợi ý cấu hình).
- [ ] Bắt đầu tùy biến UI Lylee lên trên FCL — có sẵn bản thiết kế tham khảo (Jetpack Compose Material 3: `ServerStatusCard`/`NewsBannerCarousel`/`LaunchBar`) + hướng đồng bộ modpack Cobblemon tự động (`CobblemonSyncTask` trong `FCLCore`) do Gemini soạn theo yêu cầu người dùng — xem file PDF gốc trong `docs/` (chưa được review kỹ, chỉ là gợi ý code mẫu chưa chạy thử).

## 10. Đợt dọn thương hiệu FCL còn sót + bắt đầu "Lylee Cobblemon" (2026-08-21)

Người dùng gửi 7 ảnh chụp màn hình thật trên máy, phát hiện app đã đổi màu/tên/icon
đúng nhưng vẫn còn vài chỗ nội dung/link CỦA FCL-Team sót lại. Đã sửa:

- [x] `AboutPage.java` — bỏ hẳn `about_launcher` (trỏ `fcl-team.github.io`), bỏ
  `community_discord`/`community_qq` (kênh cộng đồng của FCL, Lylee chưa có kênh
  thật để thay), và **quan trọng nhất**: bỏ link donate Afdian cá nhân của dev FCL
  (`about_sponsor`) — để nguyên sẽ khiến người chơi tưởng ủng hộ Lylee mà thực ra
  chuyển tiền cho người khác. Giữ `about_developer` (credit GPL-v3 bắt buộc, trỏ
  `github.com/FCL-Team`) và `about_source` (đổi sang `github.com/silvadrag/lylee-mobile-launcher`,
  chỉ vào xem được khi repo này được bật public).
- [x] `MainUI.java` — banner thông báo trong app (`ANNOUNCEMENT_URL`/`_CN`) trước
  trỏ thẳng vào GitHub/Gitee thật của FCL-Team (đây là chỗ user chụp ảnh thấy card
  "Thông báo: About 1.3.1.5" của FCL hiện trong app Lylee) → đổi sang placeholder
  `.../mobile/announcement_v2.txt` trên đúng domain Cloudflare Worker mà launcher PC
  đang dùng thật. Endpoint này CHƯA tồn tại phía backend nên hiện tại sẽ fail êm,
  không hiện gì — cần dựng thật ở mục việc-cần-làm bên dưới.
- [x] `UpdateChecker.java` — tương tự, `UPDATE_CHECK_URL`/`_CN` đổi từ
  `version_map.json` thật của FCL-Team sang placeholder `.../mobile/version_map.json`
  cùng domain, cũng chưa có backend thật phía sau.
- [x] Controller JSON (`00000000.json`) — sửa mô tả còn ghi "Fold Craft Launcher".
- [x] Bắt đầu phần "Lylee Cobblemon" (nối nhanh vào server) — tạo
  `FCL/src/main/java/com/tungsten/fcl/lylee/LyleeManifest.java` (DTO khớp đúng JSON
  thật của `Dtos.ManifestResponse`/`FileEntryResponse` bên mod backend) và
  `LyleeCobblemonSync.java` (engine đồng bộ: gọi CHÍNH endpoint
  `GET /api/servers/1/manifest` mà launcher PC đang dùng thật — không bịa endpoint
  riêng cho mobile — so size trước/hash SHA-1/256 sau để biết file nào cần tải lại,
  dùng `FileDownloadTask` có `IntegrityCheck` sẵn của `FCLCore`). CHỦ Ý không tự xóa
  file thừa ngoài manifest, học từ bug thật đã gặp bên PC launcher (xem
  `docs/PROGRESS.md` bên repo PC, mục cải tiến update 2026-08-13) — tự xóa từng ép
  mất mod người chơi tự thêm. Build `:FCL:assembleFordebug` qua sạch, nhưng **CHƯA
  gắn nút/màn hình nào gọi `LyleeCobblemonSync.sync()`** — mới chỉ là engine gọi
  được từ code, chưa có UI trigger thật.
- [x] Quét lại toàn bộ source bằng grep (`discord.gg|afdian|fcl-team.github.io|
  FCL-Team/FoldCraftLauncher|qq.com|Fold Craft Launcher|FoldCraftLauncher`) để bắt
  hết chỗ sót lần 1 chưa thấy, tìm ra và sửa thêm:
  - `UpdateDialog.java` — bỏ nút giữ-để-mở trang release GitHub thật của FCL-Team;
    đổi tên file/tác vụ tải bản cập nhật từ `FoldCraftLauncher.apk` → `LyleeLauncher.apk`.
  - `ControllerUploadPage.java` — ẩn nút "qq" (từng đưa thẳng vào group QQ thật của
    FCL-Team khi chia sẻ controller layout), xóa hẳn `joinQQGroup()`/`QQ_GROUP_KEY`
    không dùng nữa.
  - `ShellActivity.java`, `LauncherSettingAdapter.kt` — 2 chỗ text nhỏ còn ghi "Fold
    Craft Launcher" (dòng log shell, gợi ý placeholder ô nhập launcher name).
  - CỐ Ý KHÔNG đổi: URL tải Java runtime thật (`VersionSettingPage.kt`) và plugin
    ffmpeg thật (`ModChecker.kt`) — đây là dependency chức năng thật FCL-Team host,
    đổi mà không có server Lylee thay thế sẽ làm hỏng tính năng; link tài liệu
    hướng dẫn (`ArticleAdapter.java`, `fcl-team.github.io`) — nội dung hữu ích thật,
    không phải link cộng đồng/donate gây hiểu lầm nên giữ; tên định danh nội bộ
    (`Theme.FoldCraftLauncher`, socket name) không hiện ra người dùng; và chuỗi
    string ở các locale zh/ru/uk/pt-rBR/de chưa đổi thương hiệu (ưu tiên thấp, app
    dùng chính ở vi/en).
- Phát hiện phụ trong đợt build lại lần 2: máy build đã tự chuyển `JAVA_HOME` mặc
  định sang JDK 25 (Temurin) — AGP không hiểu, build lỗi mập mờ chỉ in ra
  `"25.0.4"`. Có JDK 21 vẫn còn cài ở
  `C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot` — build lại thành công
  khi truyền `-Dorg.gradle.java.home="...jdk-21.0.9.10-hotspot"`. Chưa sửa cấu hình
  project (`gradle.properties`) hay `JAVA_HOME` hệ thống — nếu build tay ở máy này
  báo lỗi tương tự về sau, nhớ lại đúng nguyên nhân này trước khi mất công tìm chỗ khác.

### Việc cần làm tiếp (đợt này)

- [x] Người dùng kết nối lại điện thoại — đã cài bản build lần 2, xác minh trực
  quan trên máy thật: About page (mô tả + link đúng), controller mặc định (mô tả
  đúng sau khi xóa cache cũ để app tự sinh lại từ asset mới — không phải bug),
  màu/nút trang chủ đều đúng.

## 11. Gắn UI trigger thật cho Lylee Cobblemon (2026-08-21, cùng ngày)

- [x] Thêm nút "Lylee Cobblemon" ngay trên màn hình chính (`ui_main.xml`, góc
  dưới trái, cạnh skin viewer) — bấm là chạy toàn bộ luồng "nối nhanh":
  1. `LyleeCobblemonSync.fetchManifest()` — lấy manifest thật từ backend (đã tách
     riêng khỏi `sync()` cũ để biết trước `minecraftVersion`/`loaderVersion` trước
     khi quyết định có cần tạo version mới hay không).
  2. Nếu profile hiện tại CHƯA có version tên cố định `LyleeCobblemon`: dùng
     `GameBuilder` + `VersionList.getVersion(mcVersion, loaderVersion)` (API cài đặt
     game/loader CÓ SẴN của FCL, y hệt luồng "Cài game mới" thật của app) để cài
     đúng bản Minecraft + Fabric loader server yêu cầu — KHÔNG tự chế lại logic cài
     đặt, tái dùng nguyên bộ máy Task/GameBuilder đã có.
  3. Đồng bộ file modpack qua `LyleeCobblemonSync.syncFiles()` (như cũ, không tự
     xóa file thừa).
  4. Chọn version này làm selected version, hiện dialog "Lylee Cobblemon đã sẵn
     sàng, có thể vào chơi" với nút "Chơi ngay" (gọi lại `Versions.launch()` có sẵn
     của FCL) hoặc Cancel.
  5. Lần bấm SAU (version đã tồn tại): bỏ qua bước cài, chỉ đồng bộ lại file —
     dùng để "cập nhật modpack" nhanh mỗi khi server đổi mod.
- [x] File mới: `FCL/src/main/java/com/tungsten/fcl/lylee/LyleeCobblemonConnector.java`.
  Dùng lại nguyên UI pattern (TaskDialog, FCLAlertDialog, thông báo lỗi qua
  `VersionInstallInfoPage.alertFailureMessage`) đã có sẵn trong app, không phát
  minh lại dialog riêng.
- [x] Build `:FCL:assembleFordebug` qua sạch, **đã cài lên máy thật và bấm thử
  toàn bộ luồng end-to-end**: tải thư viện Minecraft 1.21.1 + Fabric loader thật
  qua BMCLAPI → "Cài đặt Minecraft" xong → tự chuyển sang tải đúng các mod thật
  của server (LyleeMod-1.1.jar, lyleebattlepass-1.0.jar, mega_showdown-fabric,
  NoChatReports, mcw-roofs, netherportalfix...) → panel bên phải tự đổi version
  đang chọn thành "LyleeCobblemon" → dialog sẵn sàng chơi hiện đúng. Không bấm
  "Chơi ngay" để launch thật (không cần thiết cho việc xác minh — nút đó chỉ gọi
  lại `Versions.launch()` vốn đã là tính năng ổn định có sẵn của FCL).

## 12. Backend thật cho announcement + version-check, đã deploy + test sống (2026-08-21)

- [x] `URL` trong `MainUI.java`/`UpdateChecker.java` đổi từ placeholder
  (`mobile/announcement_v2.txt`, `mobile/version_map.json`) sang endpoint thật:
  `GET /api/mobile/announcement` và `GET /api/mobile/version-check` — cùng domain
  Cloudflare Worker/mod thật launcher PC đang dùng.
- [x] Bên `fabric-lyleelauncherAPI-mod-1.21.1`: thêm 3 route mới trong
  `ApiServer.java` — `GET /api/mobile/announcement` (tái dùng NGUYÊN bảng
  `Announcement` có sẵn, không đổi schema, chỉ bọc lại đúng shape
  `Announcement.java` mobile mong đợi), `GET /api/mobile/version-check` (đọc bảng
  mới `MobileClientVersion`), `POST /api/admin/mobile-version` (JWT admin, publish
  bản mới). Chi tiết DTO/DB xem `Dtos.java`/`Database.java` cùng commit.
- [x] Bảng mới `MobileClientVersion` — migration mục 8 trong
  `docs/database/migrations-applied-2026-08.sql` (repo PC) — KHÔNG đụng bảng nào
  khác. Người dùng tự chạy qua HeidiSQL trên DB thật.
- [x] `LyleeAdminTool`: thêm tab **📱 Mobile** (song song tab 🚀 Launcher hiện có)
  để publish bản version mobile mới — `MainWindow.xaml`/`.xaml.cs`, `Models/Dtos.cs`,
  `Services/AdminApiService.cs`. Đã build lại + publish bản `.exe` mới vào
  `publish/LyleeAdminTool/` (bản cũ 09/08 không có tab này).
- [x] **Deploy thật + test sống trên server Cobblemon đang chạy**: người dùng tự
  build jar (`./gradlew build` → `build/libs/lyleelauncherapi-1.0.0.jar`, đã verify
  jar chứa đủ 3 route mới), upload đè vào `mods/` trên Lilypad, Restart server.
  Xuất bản thử 1 bản test (`type=release`, `versionCode=1328`,
  `versionName=9.9.9-test`) qua tab Mobile — mở lại app trên điện thoại thật, hộp
  thoại "Đã phát hiện phiên bản có thể cập nhật" hiện ĐÚNG y hệt dữ liệu vừa xuất
  bản. Đồng thời phát hiện `/api/mobile/announcement` CŨNG chạy sống luôn (thấy
  banner thông báo thật "RAID BOSS ETERNAL FLOETTE" đã có sẵn trong DB, tự hiện
  lên app mobile mà không cần đăng gì thêm).
- [ ] **Vấn đề phát sinh, chưa xử lý**: thông báo tái dùng từ bảng `Announcement`
  hiện NGUYÊN VĂN mã XAML (`<FlowDocument PagePadding=...>`) trên mobile thay vì
  text đọc được — vì nội dung được soạn qua RichTextBox (WPF FlowDocument) bên
  AdminTool cho PC render, còn mobile chỉ hiện text thuần nên bị lộ mã ra ngoài.
  Cần xử lý sau (VD: bóc text thuần từ XAML phía server trước khi trả về mobile,
  hoặc tách kênh soạn thông báo riêng cho mobile) — không chặn, không phải lỗi
  code mới, là giới hạn thật của việc dùng chung 1 bảng cho 2 nền tảng khác kiểu
  nội dung.
- Người dùng tự xóa bản test `9.9.9-test` khỏi `MobileClientVersion` sau khi xác
  nhận xong (không cần giữ lại, chỉ để test đường ống hoạt động).

### Việc cần làm tiếp

- [ ] Xử lý vấn đề thông báo hiện mã XAML thô trên mobile (xem mục 12 ở trên).
- [ ] Thiết kế lại UI (mục người dùng yêu cầu, chưa bắt đầu — mới chỉ xong phần
  màu/tên/icon/splash/dọn link + 1 nút Cobblemon, chưa đổi layout/màn hình nào).
  Khi làm, cân nhắc nâng nút "Lylee Cobblemon" hiện tại (tạm bợ, chỉ 1 nút góc màn
  hình chính) lên thành 1 tab/màn hình riêng đúng tinh thần "2 tab Cobblemon +
  Instances tự do" bên PC.
- [x] Giải thích cho người chơi hiểu bấm lại nút = cập nhật (không cài lại từ
  đầu) — đã code (tiêu đề dialog đổi theo trạng thái + giữ nút hiện Info) và **xác
  minh trên máy thật (2026-08-21)**: giữ nút "Lylee Cobblemon" khi đã cài hiện
  đúng "Lylee Cobblemon đã cài rồi. Bấm lại chỉ tải mới các file mod thay đổi từ
  server — không cài lại từ đầu, không xóa file bạn tự thêm."
- [ ] Chưa có APK thật nào được host ở `_mobile/` trên server — bản test hiện tại
  chỉ xác nhận đường ống hoạt động (DB → API → app), chưa test tải/cài thật.

## 13. Thiết kế lại UI — Bước 1+2+3(1 phần): đồng bộ màu với launcher PC (2026-08-21)

Người dùng chọn hướng "giống phong cách PC launcher". Khảo sát code thật của PC
(MainWindow.xaml, docs/UI_DESIGN_GUIDELINES.md) trước khi đổi, không đoán:
nền phân 3 tầng `#141414` (sidebar) < `#1a1a1a` (nền chính) < `#252525` (card),
hồng `#EC5990` CHỈ dùng cho CTA/trạng thái active, bo góc chuẩn 4/6/8px.

- [x] **Bước 1 — Trang chủ**: sidebar đổi từ phủ đặc hồng (`ThemeEngine.color`)
  sang nền tối `#141414` (`R.color.sidebar_bg`, mới thêm) — `MainActivity.kt`.
  Đã kiểm tra kỹ cơ chế tint icon (`autoTint`/`dkColor` trong `ThemeData.kt`)
  trước khi đổi, xác nhận không làm icon biến mất (tính toán độc lập với màu nền
  thật, chỉ phụ thuộc độ sáng của màu accent). **Xác minh trên máy thật**: sidebar
  tối, icon home đang chọn nổi bật đúng màu hồng.
- [x] **Bước 2 — Thẻ thông báo trang chủ**: `MainUI.java` đổi tint từ
  `ThemeEngine.color` sang `R.color.card_bg` (`#252525`, mới thêm). Không xác
  minh lại được trực quan lần này (không có thông báo nào đang hiển thị lúc test,
  không tự đăng thông báo giả vì bảng dùng chung thật với launcher PC) — code đã
  soát kỹ, rủi ro thấp.
- [x] **Bước 3 (1 phần) — Phát hiện pattern hệ thống**: cách phủ hồng đặc KHÔNG
  chỉ ở trang chủ — lặp lại ở **~35 file layout khắp app** qua nhiều cơ chế khác
  nhau. Thay vì sửa từng file, tìm và sửa tại gốc:
  - `FCLLinearLayout.java` (`auto_linear_background_tint`) + `FCLTextView.java`
    (`auto_text_background_tint`) — 2 thuộc tính XML dùng chung ở **23 file**
    (About, Download, Controller, Modpack...) — sửa 1 lần ở class component,
    không đụng từng file XML.
  - `FCLTabLayout.java` (`follow_theme`) — nền thanh tab (VD 3 tab trong Cài đặt)
    đổi từ hồng nhạt sang `card_bg`.
  - `LauncherSettingAdapter.kt`/`VersionSettingAdapter.kt` — màu nền từng dòng
    trong 2 RecyclerView cài đặt, đổi trực tiếp sang `card_bg` (không còn phụ
    thuộc theme nên bỏ luôn cơ chế `registerEvent`/`unregisterEvent`).
  - **Xác minh trên máy thật ở 2 màn khác nhau**: Cài đặt (cả 3 tab: Trò chơi
    Toàn cục/Trình khởi chạy/Giới thiệu) và Tải mod — mọi card/tab/checkbox đều
    nền tối, hồng chỉ còn ở toggle/checkbox/progress bar/tab đang chọn/nút CTA.
- [x] **Bước 3 (hoàn tất) — 12 file còn lại**: khảo sát từng file, phân loại rõ
  ràng trước khi sửa để không phá vỡ chỗ dùng hồng HỢP LÝ:
  - Sửa thêm 2 class dùng chung (`FCLConstraintLayout.kt`, `FCLAppBarLayout.kt` —
    cùng pattern `auto_tint` như `FCLLinearLayout` đã sửa ở bước trước),
    `FCLCheckedTextView.java` (mỗi dòng trong spinner dropdown từng phủ hồng cả
    dòng), và 9 file class trang/adapter riêng lẻ (thanh tìm kiếm, khung info,
    danh sách installer item...) — mỗi chỗ chỉ 1 dòng gọi
    `setBackgroundTintList(theme.ltColor)`.
  - `DatapackListAdapter`/`LocalModListAdapter`: GIỮ NGUYÊN màu hồng cho dòng
    ĐANG ĐƯỢC CHỌN (đúng tinh thần "accent chỉ báo trạng thái active", giống tab/
    icon nav đang chọn), chỉ đổi màu dòng CHƯA chọn từ hồng nhạt sang `card_bg`.
  - CHỦ Ý KHÔNG đụng: màu ripple khi bấm (`FCLButton`/`FCLImageButton`/
    `FCLMenuView`), màu toggle/checkbox/viền khi focus ô nhập
    (`FCLSwitch`/`FCLCheckBox`/`FCLEditText`), tag nhỏ loại phiên bản
    (`RemoteVersionListAdapter`), icon file/thư mục trong File Browser, và
    trang tự chọn màu theme (đều là dùng hồng ĐÚNG chỗ, không phải bug).
  - **Xác minh trên máy thật thêm 3 màn**: Điều khiển (Controller), Quản lý (cả
    5 tab con: Cài đặt Trò chơi/Quản lý/Trình tải Mod/Quản lý Mods/Thế giới) —
    tất cả nền tối đúng, không còn mảng hồng đặc nào ngoài ý muốn.

## 14. Nâng "Lylee Cobblemon" từ nút góc màn hình lên tab riêng (2026-08-21)

- [x] Thêm icon mới (biểu tượng server) vào thanh nav trái, ngay dưới Home —
  `activity_main.xml`. Thêm vào cuối danh sách UI trong `UIManager.kt` (vị trí
  8, KHÔNG chèn giữa để tránh phải đánh số lại `accountUI`(6)/`versionUI`(7)
  đang viết chết ở nhiều chỗ trong `MainActivity.kt`).
- [x] `LyleeCobblemonUI.java` (mới) + `ui_lylee_cobblemon.xml` (mới) — trang
  riêng kiểu "hero + CTA" giống PC: tiêu đề, mô tả, dòng trạng thái (đã cài hay
  chưa), 1 nút CTA đổi nhãn theo trạng thái ("Kết nối ngay"/"Cập nhật modpack").
  CHỦ Ý không viết lại logic — nút gọi thẳng `LyleeCobblemonConnector.connect()`
  đã kiểm chứng hoạt động từ mục 11, đây chỉ là lớp trình bày mới.
- [x] Dọn nút cũ khỏi `ui_main.xml`/`MainUI.java` (field, findViewById,
  listener, import) — không còn trùng lặp với tab mới.
- [x] **Xác minh trên máy thật**: icon server hiện đúng vị trí, bấm vào hiện
  đúng trang riêng với text "Đã cài đặt trên máy này." + nút "Cập nhật modpack"
  (đúng vì Cobblemon đã cài từ lúc test mục 11), icon nav tô hồng đúng khi được
  chọn, nút cũ trên trang chủ đã biến mất.

## 15. Sửa thông báo mobile hiện mã XAML thô (2026-08-21)

- [x] `Database.getLatestActiveAnnouncementForMobile` (repo mod) — thêm
  `xamlToPlainText()`: bóc nội dung XAML FlowDocument (PC AdminTool soạn qua
  RichTextBox) về text thuần trước khi trả cho mobile. Đi qua DOM XML thật
  (không regex mù) để không vỡ khi thuộc tính chứa `<`/`>`, xuống dòng ở ranh
  giới mỗi `Paragraph`, ảnh/control khác tự bỏ qua vì không có text. XAML lỗi
  định dạng thì fallback bóc thẻ bằng regex thay vì làm hỏng cả announcement.
  Test tay bằng mẫu XAML giống hệt announcement thật đã thấy trên máy (ảnh bị
  bỏ đúng, xuống dòng đúng, entity `&lt;` giải mã đúng, input lỗi vẫn fallback
  ra được text đọc được) — xem `git log` commit liên quan.
- [x] **Đã deploy (2026-08-21)**: build `lyleelauncherapi-1.0.0.jar` (đã xác
  nhận có `xamlToPlainText` trong `Database.class`), người dùng tự tay upload
  lên Lilypad `mods/` và restart server. Server đã load bản mod mới.

### Việc cần làm tiếp

- Không còn việc treo từ mục 15/16 — cả fix XAML lẫn đợt dịch comment đều đã
  deploy/commit xong.

## 16. Dịch toàn bộ comment tiếng Trung sang tiếng Việt (2026-08-21)

- [x] Quét toàn bộ codebase (`grep -rlP '[\x{4e00}-\x{9fff}]'`, không tính
  `/build/`) — dịch hết comment/Javadoc/KDoc tiếng Trung sang tiếng Việt,
  gồm cả code gốc của FCL-Team (không chỉ phần Lylee tự viết), trải khắp
  4 module (`FCL`, `FCLCore`, `FCLauncher`, `ZipFileSystem`), file layout/
  values XML, và bộ `androidTest`.
- [x] Quy trình mỗi đợt dịch: viết dict Python (chuỗi Trung → chuỗi Việt) →
  chạy thay thế → **bắt buộc grep lại** từng file sau khi chạy để bắt phần
  Trung còn sót do dict key chỉ khớp một phần chuỗi dài hơn (đã bắt được vài
  lần lỗi kiểu này khi tự-check "no MISSING keys" của script không đủ tin
  cậy).
- [x] Cố ý **giữ nguyên** các nội dung tiếng Trung hợp lệ, không dịch:
  `values-zh/strings.xml` và `values-zh-rHK/strings.xml` (bản dịch tiếng
  Trung thật cho người dùng chọn ngôn ngữ đó), tên ngôn ngữ tự thân trong bộ
  chọn ngôn ngữ (`简体中文`, `繁體中文（香港）`), và tên riêng trang
  "MC百科" (`mcmod`, đã đánh dấu `translatable="false"` sẵn từ trước).
- [x] Build `FCL:assembleFordebug` full thành công sau khi dịch xong toàn
  bộ (resource + Kotlin + Java compile OK) — không có thiết bị nào kết nối
  lúc này nên chưa test trực tiếp trên máy, chỉ xác nhận qua build.
- [x] Commit `b525ccd57` — gộp phần còn lại (phần skin/cube renderer đã
  dịch + commit riêng trước đó, xem `c5409196e`).

## 17. Nút chuông xem lại tin tức + ẩn kiểu nhắc nhở mỗi ngày (2026-08-21)

- [x] **Backend** (`fabric-lyleelauncherAPI-mod-1.21.1`, commit `94d425d`) —
  thêm `Database.getRecentActiveAnnouncementsForMobile` +
  `GET /api/mobile/announcements` (số nhiều, khác `/announcement` số ít chỉ
  trả 1 tin mới nhất): trả danh sách tối đa 50 tin đang active/còn hạn, cùng
  shape + cùng bóc XAML với endpoint cũ.
- [x] **Mobile** (commit `00941486c`) — nút chuông 🔔 luôn hiện ở góc trên-
  phải khu vực tin tức Trang chủ (kể cả khi không có tin nào đang active),
  mở `AnnouncementHistoryDialog` liệt kê toàn bộ tin từ endpoint mới.
  - Vấp 1 lần: định làm màn hình riêng qua `FCLPage`/`showTempPage` (giống
    các trang phụ khác), nhưng `MainUI` (Trang chủ) là `FCLCommonUI` đơn
    trang — gọi `showTempPage` trên UI của TAB KHÁC (`ManageUI`) không hiện
    gì vì contentView của tab đó chưa được `ViewPager2` gắn vào cây view
    đang hiển thị. Sửa bằng cách dùng `FCLDialog` (nền tảng của `TaskDialog`
    có sẵn) — dialog độc lập với tab đang mở, đã test lại đúng trên máy.
  - `Announcement.hide()`/`shouldDisplay()` đổi ý nghĩa: bấm "Ẩn" giờ chỉ
    tắt card **trong ngày hôm đó** (lưu kèm ngày ẩn vào SharedPreferences),
    hôm sau tự hiện lại nếu tin còn hạn — thay vì mất hẳn tới khi có tin
    mới hơn như trước.
- [x] Test trên máy thật (`R58M93RVPXH`): card tiêu đề hiện đúng text thuần
  (không còn XAML thô), nút chuông mở dialog đúng, trạng thái rỗng
  ("Chưa có tin tức nào.") hiện đúng vì server **chưa** deploy endpoint mới.
- [ ] **Phát hiện phụ, chưa sửa**: thông báo thật (RAID BOSS ETERNAL FLOETTE
  và cả 3 tin cũ khác) đều là `FlowDocument` chỉ chứa 1 `<Image>`, không có
  chữ — sau khi bóc XAML, phần thân card mobile trống trơn (chỉ còn tiêu
  đề). Đây là giới hạn có sẵn của tính năng thông báo mobile (chỉ hiện text,
  chưa hỗ trợ hiện ảnh), không phải lỗi do đợt sửa XAML gây ra. Người dùng
  chọn "thêm ảnh vào mobile sau" — không làm ngay, ghi lại làm việc cần làm
  tiếp bên dưới.

- [x] **Đã deploy (2026-08-22)**: jar mới (gộp fix XAML mục 15 + endpoint
  danh sách mục 17) đã build → upload `mods/` → Restart. Xác minh sống:
  `GET /api/mobile/announcements` trả đủ 4 thông báo (id 2-5) đúng shape.
- [x] Sửa vị trí nút chuông (2026-08-22): tiêu đề thông báo dài (VD "RAID
  BOSS ETERNAL FLOETTE") có thể lấn vào icon khi nó neo ở góc card — dời
  nút chuông ra góc trên-phải TOÀN MÀN HÌNH (tách khỏi cột 40% của card),
  không còn đè bất kể tiêu đề dài cỡ nào. Test lại trên máy thật, OK.

### Việc cần làm tiếp

- [ ] Thêm hỗ trợ hiện ảnh trong thông báo mobile (card Trang chủ +
  `AnnouncementHistoryDialog`) — cần cả backend (trả kèm URL ảnh, XAML hiện
  đang bóc bỏ hẳn thẻ `<Image>`) lẫn mobile (tải + hiện `ImageView` trong
  card/dialog). Chưa bắt đầu, người dùng xác nhận làm sau.
- [ ] **Ý tưởng mới (2026-08-22): kết bạn/chat mobile ↔ PC.** Backend đã có
  sẵn gần như đầy đủ từ đợt làm PC launcher (`8d4d456`, `6083ee2`) —
  `ApiServer.java` đã có nguyên bộ route friends (gửi/nhận lời mời, chặn,
  xóa bạn — `/api/players/{username}/friends...`) và messages (gửi tin, thu
  hồi, sửa, react, đang gõ, đính kèm file — `/api/players/{username}/messages...`).
  Mobile hiện **chưa có gì** (không 1 file friend/chat nào trong repo mobile
  — đã grep xác nhận). Việc chính khi làm là xây UI mobile mới (list bạn
  bè, màn chat, polling/typing...) nối vào API có sẵn — không cần động
  backend. Là feature lớn, người dùng xác nhận để dành riêng 1 phiên,
  chưa bắt đầu.

## 18. Test thật "Chơi ngay" Lylee Cobblemon — phát hiện RAM không đủ trên máy thấp cấp (2026-08-22)

Người dùng hỏi app đã "xài được chưa" — rà lại thấy mục 9 vẫn còn 1 dòng
`[ ]` treo từ đầu dự án: **chưa từng thật sự bấm "Chơi ngay" để vào game**
(mục 11 chỉ test tới bước cài đặt/đồng bộ mod, cố tình dừng trước bước
launch). Bấm thử thật trên máy test (Galaxy A50s, Exynos 9611/Mali-G72,
3.67GB RAM) — đây là lần đầu.

- [x] **Luồng cài đặt + tải mod**: chạy hoàn hảo (đã xác nhận từ mục 11).
- [x] **Bấm "Chơi ngay" lần 1**: game khởi chạy thật (Java check → resolve
  dependencies → login → boot), qua được màn welcome "SERVER LYLEE
  COBBLEMON" nhưng **treo hẳn** ở bước `Loading animations...` (log
  `FCL Debug`) — CPU 168%, RAM hệ thống tụt còn **14MB trống** trước khi
  mình chủ động force-stop để bảo vệ máy.
- [x] Điều tra nguyên nhân — đọc code thật, không đoán:
  - `MemoryUtils.findBestRAMAllocation()`: máy ≤6144MB RAM → sàn tối đa
    **1GB** heap (64-bit). Máy test 3.67GB rơi đúng vào mức này.
  - `FCLGameRepository.getAllocatedMemory()`: mức cấp THẬT lúc launch =
    80% RAM **đang trống tại thời điểm đó** (trừ 384MB dự phòng), miễn là
    lớn hơn sàn — nghĩa là con số 1GB không cố định, phụ thuộc RAM trống
    lúc bấm nút.
- [x] **Giả thuyết "reboot máy sẽ giúp"** — test trực tiếp: reboot xong RAM
  trống nhảy 342MB → 2.24GB, nhưng chỉ riêng launcher UI (skin viewer,
  home screen...) đã ăn lại gần hết trước khi Minecraft kịp khởi chạy →
  vẫn tụt về đúng 1GB heap. **Bấm "Chơi ngay" lần 2 (sau reboot) vẫn treo
  ở ĐÚNG bước `Loading animations...`** — xác nhận đây là bottleneck lặp
  lại được, không phải rác app nền ngẫu nhiên. Máy phục hồi ngay (RAM
  trống nhảy 14MB → 1.4-1.5GB) sau mỗi lần force-stop — máy không hỏng gì,
  chỉ đúng lúc app chạy là cạn RAM.
- [x] **Kết luận**: không phải bug launcher — công thức tự tính RAM vốn cố
  tình bảo thủ để không crash trên diện rộng máy yếu. Vấn đề thật là
  **modpack Cobblemon (animation hàng trăm Pokémon) cần nhiều RAM hơn máy
  dưới ~6GB có thể đáp ứng** — khớp với khuyến nghị ≥4GB heap của server
  PC (PC không bị giới hạn bởi RAM hệ điều hành di động + native overhead
  như mobile).
- [x] **Đã làm — cảnh báo RAM trước khi tải** (không đụng backend, rủi ro
  thấp, theo lựa chọn của người dùng trong 3 hướng được đề xuất): sửa
  `LyleeCobblemonConnector.connect()` — kiểm tra
  `MemoryUtils.getTotalDeviceMemory()` trước khi gọi `fetchManifest()`,
  nếu dưới **6144MB** (đúng mốc `findBestRAMAllocation` dùng để nhảy từ
  1GB lên 2GB, không bịa số mới) thì hiện `FCLAlertDialog` (AlertLevel.ALERT)
  báo rõ RAM máy + lý do + cho chọn "Vẫn tiếp tục" hoặc hủy — tránh người
  chơi mất công tải cả GB dữ liệu rồi mới biết máy không chạy nổi. Test
  thật trên máy: hiện đúng "Máy bạn có khoảng 3,5 GB RAM..." + cả 2 nhánh
  Cancel/Vẫn tiếp tục đều hoạt động đúng.

### Việc cần làm tiếp

- [ ] 2 hướng còn lại CHƯA làm (người dùng chọn hướng cảnh báo trước, để
  dành 2 hướng này nếu cần sau): (a) thử ép tay mức cấp RAM cao hơn cho
  riêng bản LyleeCobblemon (rủi ro thật — có thể khiến OOM-crash ngay thay
  vì treo từ từ như hiện tại, cần test kỹ trước khi cân nhắc); (b) chấp
  nhận giới hạn, không sửa thêm gì.
- [ ] Phát hiện phụ không liên quan RAM: mod Simple Voice Chat lỗi native
  lib `liblame4j.so` (`UnsatisfiedLinkError: libm.so.6 not found` — thư
  viện glibc không có trên Android/Bionic) — không crash game (tự fallback
  "Using Cloth Config GUI"), nhưng tính năng voice chat khả năng không
  hoạt động trên mobile. Chưa điều tra sâu, chưa fix.

## 19. So sánh tính năng PC ↔ Mobile + bổ sung playtime/trạng thái server (2026-08-22)

Người dùng yêu cầu so sánh 2 launcher (đọc code thật cả 2 phía qua agent
research, không đoán) để tìm chỗ có thể "liên kết" 2 nền tảng — bổ sung phần
thiếu nếu khả thi và có lợi.

### Bảng so sánh

| Tính năng | PC | Mobile (trước mục này) |
|---|---|---|
| Trạng thái server (online/số người chơi) | Có (`ServerStatusService.cs` → `GET /api/servers`) | Không có |
| Thời gian chơi (playtime) | Có (đọc + hiện) | Không có |
| Kết bạn/chat | Có, đầy đủ (list/lời mời/chặn/chat gõ-đang-nhập/react/thu hồi) | Không có |
| Đăng nhập Google | Có (khóa username, không bắt buộc để chơi) | Không có |
| Cài modpack tự do (import `.mrpack`) | Có UI riêng | Máy FCL gốc hỗ trợ kỹ thuật, chưa có UI |
| Nhiều tin tức/lịch sử thông báo | Có (`NewsCardsPagerControl`, đa announcement) | Có (mục 17, `AnnouncementHistoryDialog`) — NGANG NHAU, không phải khoảng trống |
| Cảnh báo cấu hình yếu trước khi tải | Không có | Có (mục 18) |

Ưu tiên đã chọn: **playtime + trạng thái server trước** (rẻ, backend PC đã
chứng minh hoạt động, đúng nghĩa "liên kết" — chơi PC hay mobile cộng dồn
chung 1 con số). Kết bạn/chat, Google login, cài modpack tự do: để dành
phiên riêng (feature lớn hơn nhiều).

### Đã làm

- [x] **`LyleeSessionTracker.java`** (mới) — gọi `POST /api/players/{u}/session/start`
  trước khi mở `JVMActivity` (trong `LauncherHelper.launch0()`, chạy trên
  luồng nền của `thenAcceptAsync`, không phải UI thread), sessionId truyền
  qua `Intent` extra `LYLEE_SESSION_ID`; `JVMActivity.onDestroy()` gọi
  `POST .../session/{id}/end` trên thread riêng (onDestroy luôn được gọi
  bất kể thoát tay/crash/back — điểm chốt tin cậy nhất cho "phiên chơi kết
  thúc"). **CHỈ áp dụng cho version `LyleeCobblemonConnector.VERSION_NAME`**
  — đúng tinh thần PC (bản tự do/instance khác không tính giờ). Best-effort
  triệt để: mọi lỗi mạng chỉ trả `null`/im lặng bỏ qua, không chặn launch —
  mobile chưa có hạ tầng JWT player (chưa đăng nhập Google) nên không tự
  vượt qua được trường hợp "username đã khóa" như PC, chấp nhận fail-êm.
- [x] Trang Lylee Cobblemon (`LyleeCobblemonUI.java`) hiện thêm **tổng thời
  gian chơi** (`GET /playtime`, ẩn hẳn nếu 0/lỗi — không hiện "0 phút" gây
  hiểu nhầm) và **số người đang online** (`LyleeServerStatus.java`, dùng
  chung `GET /api/servers` với PC, lọc đúng `serverProfileId=1`).
- [x] Build `FCL:assembleFordebug` sạch. **Chưa test trên máy thật** — điện
  thoại không kết nối lúc code xong, để dành lần sau.

### Phát hiện: lỗi 500 khi gọi session/start — khả năng đã âm thầm hỏng playtime trên CẢ PC

- [x] Test tay `POST /api/players/claude_test_session/session/start` trên
  server thật → **lỗi 500** ("Lỗi máy chủ nội bộ"). `GET /playtime` và
  `GET /claim-status` cùng username lại chạy bình thường (Player table
  khỏe) — cô lập được: lỗi nằm đúng ở bước `INSERT INTO PlayerSession`.
- [x] Bảng `PlayerSession` **có trong `schema.mysql.sql`** (kèm comment
  gốc: "phát hiện DB không có gì thay đổi dù người chơi thật đã vào game" —
  tức triệu chứng NÀY đã từng bị để ý nhưng có thể bị chẩn đoán sai nguyên
  nhân trước đây) nhưng **không xuất hiện trong `migrations-applied-2026-08.sql`**
  — nhiều khả năng bảng chưa từng được tạo thật trên DB.
- [x] **Quan trọng**: PC (`SessionService.cs`) coi MỌI lỗi HTTP (kể cả 500)
  là "Unavailable" rồi im lặng tiếp tục launch — nghĩa là nếu đúng bảng
  chưa tồn tại, **PC cũng đã ghi nhận playtime = 0 âm thầm bấy lâu nay**,
  không ai biết vì lỗi không bao giờ hiện ra cho người chơi hay admin thấy.
- [x] Đã chuẩn bị script sửa (`CREATE TABLE IF NOT EXISTS PlayerSession...`,
  đúng y hệt định nghĩa trong `schema.mysql.sql`, an toàn nếu bảng lỡ đã
  có) — người dùng tự chạy qua HeidiSQL trên DB thật, sẽ báo lại kết quả.

### Việc cần làm tiếp

- [ ] Xác nhận đã chạy xong script sửa `PlayerSession`, test lại
  `session/start` trả về `sessionId` thay vì lỗi 500.
- [ ] Test trên máy thật: cài bản Cobblemon → Chơi ngay → xem trang Lylee
  Cobblemon có tự hiện "Tổng thời gian chơi" sau khi thoát game không.
- [ ] Cân nhắc thêm vào `migrations-applied-2026-08.sql` sau khi xác nhận
  script chạy đúng, để tránh lặp lại tình trạng "có trong schema nhưng
  không ai biết đã áp dụng thật hay chưa".
- [ ] Các mục lớn còn lại từ bảng so sánh (chưa làm, để dành phiên riêng):
  kết bạn/chat mobile, đăng nhập Google mobile, UI cài modpack tự do trên
  mobile, cảnh báo cấu hình yếu trên PC.
