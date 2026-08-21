# Prompt để nhờ AI khác phân tích/so sánh PojavLauncher vs Fold Craft Launcher

> Copy toàn bộ nội dung dưới đây gửi cho AI khác (ChatGPT/Gemini...). Đã tự chứa đủ ngữ cảnh, không cần AI đó truy cập GitHub thật (dù có link tham khảo).

---

## Bối cảnh

Tôi đang làm launcher Android để chơi **Minecraft: Java Edition** (không phải Bedrock) cho server Cobblemon riêng của tôi. Tôi đã có sẵn 1 launcher PC (WPF/.NET) hoàn chỉnh, giờ muốn làm bản đồng hành trên điện thoại.

Chạy Minecraft Java trên Android không thể viết từ đầu (cần build lại OpenJDK cho ARM + tầng dịch đồ họa Desktop OpenGL → OpenGL ES/Vulkan + JNI bridge — công sức hàng tháng/năm). Nên hướng đi là **fork 1 project mã nguồn mở đã hoàn thiện phần khó đó**, rồi tùy biến giao diện/tính năng lên trên (đổi màu/logo theo Lylee, nối vào server/backend riêng, tự động chọn modpack thay vì để người chơi chọn version tay...).

Tôi đã tự tay clone, build, và **cài chạy thử thật cả 2 ứng viai trên điện thoại Samsung Galaxy A50s** — cả 2 đều build thành công và chạy không crash. Giờ cần chọn 1 để đi tiếp, không tự viết code thêm cho tới khi chọn xong.

## 2 ứng viên

### 1. PojavLauncher
- Repo: https://github.com/TeamPojavLauncher/PojavLauncher (nhánh `v3_openjdk`)
- Ngôn ngữ: Java + C/C++ (JNI), package chính `net.kdt.pojavlaunch`
- Dự án lâu đời/nổi tiếng nhất thế giới về mảng này, cộng đồng lớn, nhiều issue/tài liệu có sẵn để tra cứu khi gặp bug
- Cấu trúc: ~28 package con (`authenticator`, `customcontrols`, `downloader`, `instances`, `modloaders`, `multirt`, `render`, `services`...) — khá đồ sộ, trưởng thành
- Dùng git submodule thật cho phần native: `glfw`, `mojoexec`, `sdl` (org MojoLauncher), `MobileGlues`, `NG-GL4ES`
- Build: `compileSdk 36`, `minSdk 21`, `ndkVersion 29.0.14206865`, Gradle 8.14.3
- Kết quả build thật: `assembleDebug` chạy sạch trong ~4-18 phút (tùy máy đã cache hay chưa), ra APK `full` (106MB, kèm sẵn runtime) và `noruntime` (78MB)
- Chạy thật trên điện thoại: vào thẳng màn hình `LauncherActivity`, giao diện đầy đủ tiếng Việt sẵn (đa ngôn ngữ qua Crowdin), đã chọn sẵn Minecraft 1.12.2, nút "CHƠI" hoạt động
- **License: KHÔNG chắc chắn** — README có badge "LGPL v3" nhưng khi tự kiểm tra thật (`gh api repos/.../license`) thì **không tìm thấy file LICENSE nào ở gốc repo** cả — cần người tự vào GitHub xác minh trực tiếp trước khi coi đây là chính thức.

### 2. Fold Craft Launcher (FCL)
- Repo: https://github.com/FCL-Team/FoldCraftLauncher (nhánh `main`)
- Ngôn ngữ: Kotlin + Java + C/C++, package chính `com.tungsten.fcl`
- Team chính có vẻ là người Trung Quốc (1 phần comment code bằng tiếng Trung, VD `// 子项目名以数字开头会导致...`)
- Cấu trúc module: `FCL` (app chính), `FCLCore`, `FCLauncher`, `Terracotta`, `ZipFileSystem`, `LWJGL` (nhiều bản LWJGL 3.3.3/3.4.1 song song) — dùng Gradle version catalog (`libs.versions.toml`), cách tổ chức hiện đại hơn Pojav
- Không dùng git submodule cho jni (native code nằm trực tiếp trong `FCLauncher/src/main/jni/`)
- Build: `compileSdk 35`, `minSdk 26` (Pojav minSdk 21 → hỗ trợ máy cũ hơn), `targetSdk 34`, `ndkVersion 27.0.12077973`
- Có 3 biến thể build: `release`/`debug` (cần khóa ký thật `key-store.jks`, không có trong repo) và **`fordebug`** (dùng `debug-key.jks` mật khẩu hardcode sẵn, đúng cho việc dev/test không cần khóa thật)
- Kết quả build thật: `:FCL:assembleFordebug` chạy sạch (~1-6 phút tùy cache), ra APK 341MB (biến thể "all" gộp mọi kiến trúc CPU trong 1 file — bản thật production chắc tách theo ABI sẽ nhỏ hơn nhiều)
- Chạy thật trên điện thoại: splash screen có ảnh nền động (làng Minecraft ban đêm), hộp thoại xin quyền + màn hình chào mừng/EULA đều dịch tiếng Việt đầy đủ, nút "Đồng ý và Tiếp tục" — **UI đầu tư rõ ràng kỹ hơn hẳn** ngay từ bước onboarding đầu tiên so với Pojav
- **License: RÕ RÀNG** — có file `LICENSE` thật ở gốc repo, nội dung đầy đủ **GPLv3**

## Ràng buộc pháp lý cần AI lưu ý khi phân tích

Cả 2 đều **copyleft** (LGPLv3/GPLv3) — nếu fork rồi phát hành app cho người chơi dùng thật, **bắt buộc giữ mã nguồn mở** của phần đã sửa, không thể âm thầm đóng gói lại thành closed-source. GPLv3 (FCL) có phần chặt hơn LGPLv3 một chút về việc link thư viện tĩnh, nhưng cả 2 đều yêu cầu công khai source.

## Yêu cầu phân tích

1. **Đánh giá độ rủi ro license của từng bên** cho use-case: app phát hành (kể cả nội bộ/private) cho người chơi 1 server Minecraft riêng dùng, không thương mại hóa. Pojav thiếu file LICENSE rõ ràng có phải vấn đề thật không, hay chỉ là badge README lỗi thời?
2. **Đánh giá độ khó tùy biến UI** dựa trên cấu trúc code đã mô tả — cấu trúc module nào (28 package phẳng của Pojav vs multi-module + version catalog của FCL) dễ "cắm" 1 lớp UI riêng lên trên (theo mô hình: giữ nguyên Core xử lý JRE/native/tải game, chỉ thay UI) hơn?
3. **Đánh giá độ ổn định lâu dài**: cộng đồng lớn (Pojav) vs code hiện đại/tổ chức tốt hơn nhưng ít người dùng hơn (FCL) — bên nào ít rủi ro "dự án chết"/khó tìm người hỗ trợ khi gặp bug hiếm hơn?
4. **Gợi ý cụ thể** nếu phải chọn 1: chọn bên nào và vì sao, nêu rõ đánh đổi (trade-off) chính. Nếu có thể, gợi ý thêm câu hỏi/thứ cần tự kiểm tra thêm trước khi chốt hẳn (VD tự đọc kỹ LICENSE thật của Pojav trên GitHub) mà bản thân AI không tự xác minh được vì không có quyền truy cập thật.

Trả lời ngắn gọn, có cấu trúc rõ (không cần lặp lại thông tin tôi đã cho ở trên), tập trung vào phần phân tích/khuyến nghị.
