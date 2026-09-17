package com.tungsten.fcl.lylee;

import com.tungsten.fclcore.task.FileDownloadTask;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fclcore.util.DigestUtils;
import com.tungsten.fclcore.util.gson.JsonUtils;
import com.tungsten.fclcore.util.io.NetworkUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Đồng bộ modpack Cobblemon vào 1 "bản tự do" (version) FCL đã tạo sẵn —
 * tương đương {@code UpdateService} bên launcher PC, dùng CHUNG đúng 1
 * endpoint backend thật ({@code GET /api/servers/1/manifest}, xem
 * {@code fabric-lyleelauncherAPI-mod-1.21.1/.../ApiServer.java}).
 *
 * Cải tiến cơ chế cập nhật:
 * 1. Tải tuần tự từng file (tránh mở hàng chục socket song song làm nghẽn mạng/đơ máy trên mobile).
 * 2. Tự động kiểm tra và xóa các file modpack cũ/đổi tên/bị gỡ bỏ dựa trên:
 *    - Lịch sử manifest đã cài (.lylee-installed-manifest.json).
 *    - Danh sách các mod cũ lỗi thời của phiên bản trước (Cobblemon 1.7.3) để tránh xung đột dependency.
 * 3. Bảo vệ tuyệt đối các mod riêng/tùy biến do người chơi tự thêm (chỉ xóa file từng do launcher chính thức cài).
 */
public class LyleeCobblemonSync {

    public static final String MANIFEST_URL =
            "https://lylee-launcher-api.silvadrag2006.workers.dev/api/servers/1/manifest";

    public static final String INSTALLED_MANIFEST_FILE = ".lylee-installed-manifest.json";

    /**
     * Danh sách modpack cũ từ phiên bản Cobblemon 1.7.3 đã bị gỡ hoặc đổi tên ở 1.8.0.
     * Dùng để dọn sạch ngay cả trên những máy chưa từng lưu .lylee-installed-manifest.json trước đó.
     */
    private static final List<String> KNOWN_LEGACY_MODS = Arrays.asList(
            "mods/Cobblemon-fabric-1.7.3+1.21.1.jar",
            "mods/cobblecuisine-2.0.1-1.7-rc1.jar",
            "mods/CobbleFurnies-fabric-1.1.jar",
            "mods/cobblemon-economy-0.0.17.jar",
            "mods/cobblemon-escape-rope-fabric-1.0.0.jar",
            "mods/cobblenav-fabric-2.2.5.jar",
            "mods/LyleeClient-1.0.0.jar",
            "mods/mega_showdown-fabric-1.9.7+1.7.3+1.21.1.jar",
            "mods/rctapi-fabric-1.21.1-0.15.2-beta.jar",
            "mods/tbcs-fabric-1.21.1-0.13.0-beta (1).jar",
            "mods/tbcs-fabric-1.21.1-0.13.0-beta.jar",
            "mods/tmcraft-1.4.18+1.7.3.jar",
            "mods/zamega-fabric-1.7.5-hotfix.jar"
    );

    public static class InstalledManifestState {
        public List<String> installedFiles;
    }

    private LyleeCobblemonSync() {
    }

    public static Task<?> sync(File gameDir) {
        return Task.composeAsync(() -> fetchManifest().thenComposeAsync(manifest -> syncFiles(gameDir, manifest)));
    }

    public static Task<LyleeManifest> fetchManifest() {
        return Task.supplyAsync(() -> {
            String json = NetworkUtils.doGet(NetworkUtils.toURL(MANIFEST_URL));
            LyleeManifest manifest = JsonUtils.GSON.fromJson(json, LyleeManifest.class);
            if (manifest == null || manifest.files == null || manifest.files.isEmpty()) {
                throw new IOException("Không lấy được danh sách file modpack từ server Lylee (manifest rỗng hoặc sai định dạng).");
            }
            return manifest;
        });
    }

    /**
     * Đồng bộ file theo manifest:
     * - Dọn dẹp các mod cũ thuộc modpack phiên bản trước.
     * - Tải tuần tự các file còn thiếu hoặc sai hash.
     * - Lưu lại manifest đã cài đặt.
     */
    public static Task<?> syncFiles(File gameDir, LyleeManifest manifest) {
        return Task.composeAsync(() -> {
            // 1. Dọn dẹp file cũ an toàn (không đụng mod riêng của người chơi)
            cleanupOldFiles(gameDir, manifest);

            // 2. Lọc ra danh sách file cần tải
            List<LyleeManifest.FileEntry> toDownload = new ArrayList<>();
            for (LyleeManifest.FileEntry entry : manifest.files) {
                if (entry.path == null || entry.url == null || entry.hash == null) continue;
                File local = new File(gameDir, entry.path);
                if (isUpToDate(local, entry)) continue;
                toDownload.add(entry);
            }

            if (toDownload.isEmpty()) {
                saveInstalledManifest(gameDir, manifest);
                return Task.completed(null);
            }

            // 3. Tải tuần tự từng file (chuỗi Task thenComposeAsync) để không làm nghẽn mạng và quá tải mobile
            Task<?> chain = Task.completed(null);
            for (LyleeManifest.FileEntry entry : toDownload) {
                String fileUrl = entry.url;
                if (fileUrl.contains("lylee-launcher-api.lyleelauncher.workers.dev")) {
                    fileUrl = fileUrl.replace("lylee-launcher-api.lyleelauncher.workers.dev", "lylee-launcher-api.silvadrag2006.workers.dev");
                }
                File local = new File(gameDir, entry.path);
                FileDownloadTask downloadTask = new FileDownloadTask(
                        NetworkUtils.toURL(fileUrl),
                        local,
                        new FileDownloadTask.IntegrityCheck(normalizeHashAlgorithm(entry.hashType), entry.hash)
                );
                chain = chain.thenComposeAsync(() -> downloadTask);
            }

            // 4. Khi toàn bộ file tải xong thành công, lưu lại trạng thái manifest
            return chain.thenRunAsync(() -> saveInstalledManifest(gameDir, manifest));
        });
    }

    /**
     * Dọn dẹp file cũ:
     * 1. So sánh với .lylee-installed-manifest.json của lần cài trước: file nào có trong bản trước mà không còn trong bản mới -> Xóa.
     * 2. Quét danh sách KNOWN_LEGACY_MODS để dọn file rác từ 1.7.3 cho những máy chưa có file state.
     * 3. Tuyệt đối KHÔNG xóa các file mod ngoài người chơi tự cài thêm.
     */
    public static void cleanupOldFiles(File gameDir, LyleeManifest newManifest) {
        if (!gameDir.isDirectory()) return;

        Set<String> newPaths = new HashSet<>();
        for (LyleeManifest.FileEntry entry : newManifest.files) {
            if (entry.path != null) {
                newPaths.add(normalizeRelativePath(entry.path));
            }
        }

        // Bước 1: Kiểm tra lịch sử cài đặt đã lưu (.lylee-installed-manifest.json)
        File stateFile = new File(gameDir, INSTALLED_MANIFEST_FILE);
        if (stateFile.isFile()) {
            try {
                String json = new String(Files.readAllBytes(stateFile.toPath()), StandardCharsets.UTF_8);
                InstalledManifestState state = JsonUtils.GSON.fromJson(json, InstalledManifestState.class);
                if (state != null && state.installedFiles != null) {
                    for (String oldPath : state.installedFiles) {
                        String norm = normalizeRelativePath(oldPath);
                        if (!newPaths.contains(norm)) {
                            // File từng do launcher cài nhưng không còn trong modpack mới -> Xóa
                            File oldFile = new File(gameDir, oldPath);
                            if (oldFile.isFile()) {
                                try { oldFile.delete(); } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Bước 2: Quét danh sách mod cũ đã biết từ Cobblemon 1.7.3 (cứu các máy chưa có file state)
        for (String legacyPath : KNOWN_LEGACY_MODS) {
            String norm = normalizeRelativePath(legacyPath);
            if (!newPaths.contains(norm)) {
                File legacyFile = new File(gameDir, legacyPath);
                if (legacyFile.isFile()) {
                    try { legacyFile.delete(); } catch (Exception ignored) {}
                }
            }
        }
    }

    /**
     * Lưu danh sách các file chính thức của modpack vừa cài vào .lylee-installed-manifest.json.
     */
    public static void saveInstalledManifest(File gameDir, LyleeManifest manifest) {
        try {
            InstalledManifestState state = new InstalledManifestState();
            state.installedFiles = new ArrayList<>();
            for (LyleeManifest.FileEntry entry : manifest.files) {
                if (entry.path != null) {
                    state.installedFiles.add(entry.path.replace('\\', '/'));
                }
            }
            File stateFile = new File(gameDir, INSTALLED_MANIFEST_FILE);
            Files.write(stateFile.toPath(), JsonUtils.GSON.toJson(state).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private static String normalizeRelativePath(String path) {
        return path.replace('\\', '/').trim().toLowerCase(Locale.ROOT);
    }

    /** So sánh kích thước trước (rẻ) rồi mới tính hash thật (tốn CPU) —
     *  cùng chiến lược UpdateService bên PC dùng để tránh hash hàng trăm file
     *  không cần thiết mỗi lần mở app. */
    private static boolean isUpToDate(File local, LyleeManifest.FileEntry entry) {
        if (!local.isFile()) return false;
        if (entry.size > 0 && local.length() != entry.size) return false;
        try {
            String actual = DigestUtils.digestToString(normalizeHashAlgorithm(entry.hashType), local.toPath());
            return actual.equalsIgnoreCase(entry.hash);
        } catch (IOException e) {
            return false;
        }
    }

    /** Backend Java trả "SHA1" (không có dấu gạch) — {@link java.security.MessageDigest}
     *  cần đúng tên chuẩn "SHA-1"/"SHA-256". */
    private static String normalizeHashAlgorithm(String hashType) {
        if (hashType == null) return "SHA-1";
        switch (hashType.toUpperCase(Locale.ROOT).replace("-", "")) {
            case "SHA1":
                return "SHA-1";
            case "SHA256":
                return "SHA-256";
            case "MD5":
                return "MD5";
            default:
                return hashType;
        }
    }
}
