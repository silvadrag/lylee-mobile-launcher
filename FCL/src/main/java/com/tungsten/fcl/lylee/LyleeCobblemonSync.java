package com.tungsten.fcl.lylee;

import com.tungsten.fclcore.task.FileDownloadTask;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fclcore.util.DigestUtils;
import com.tungsten.fclcore.util.gson.JsonUtils;
import com.tungsten.fclcore.util.io.NetworkUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Đồng bộ modpack Cobblemon vào 1 "bản tự do" (version) FCL đã tạo sẵn —
 * tương đương {@code UpdateService} bên launcher PC, dùng CHUNG đúng 1
 * endpoint backend thật ({@code GET /api/servers/1/manifest}, xem
 * {@code fabric-lyleelauncherAPI-mod-1.21.1/.../ApiServer.java}), không phải
 * endpoint riêng bịa ra cho mobile.
 *
 * CHỦ Ý không tự xóa file thừa/mod người chơi tự thêm ngoài manifest — bài
 * học đã rút ra bên launcher PC (xem docs/PROGRESS.md, mục "Cải tiến hệ
 * thống cài đặt/cập nhật... 2026-08-13"): tự động xóa mỗi lần đồng bộ từng
 * ép mất tùy biến của người chơi mỗi lần bấm Chơi. Muốn dọn về đúng bản gốc
 * là hành động RIÊNG, người chơi tự bấm (giống nút "Khôi phục mặc định" bên
 * PC) — chưa làm ở bản này, để dành sau nếu cần.
 */
public class LyleeCobblemonSync {

    /** Cùng domain Cloudflare Worker mà launcher PC đang dùng thật (không đổi
     *  hạ tầng riêng cho mobile) — chỉ khác path, xem MainUI/UpdateChecker. */
    public static final String MANIFEST_URL =
            "https://lylee-launcher-api.lyleelauncher.workers.dev/api/servers/1/manifest";

    private LyleeCobblemonSync() {
    }

    /**
     * @param gameDir thư mục gốc của version FCL đã tạo (VD instance
     *                "LyleeCobblemon" người chơi tự tạo qua UI FCL sẵn có,
     *                chọn đúng minecraftVersion/loaderVersion mà manifest ghi).
     * @return Task tải xong toàn bộ file thiếu/sai hash trong manifest. Gọi
     *         {@code .whenComplete()}/{@code Schedulers.androidUIThread()} như
     *         các Task khác trong app để cập nhật UI lúc xong.
     */
    public static Task<?> sync(File gameDir) {
        return Task.composeAsync(() -> {
            String json = NetworkUtils.doGet(NetworkUtils.toURL(MANIFEST_URL));
            LyleeManifest manifest = JsonUtils.GSON.fromJson(json, LyleeManifest.class);
            if (manifest == null || manifest.files == null || manifest.files.isEmpty()) {
                throw new IOException("Không lấy được danh sách file modpack từ server Lylee (manifest rỗng hoặc sai định dạng).");
            }

            List<Task<?>> downloads = new ArrayList<>();
            for (LyleeManifest.FileEntry entry : manifest.files) {
                if (entry.path == null || entry.url == null || entry.hash == null) continue;
                File local = new File(gameDir, entry.path);
                if (isUpToDate(local, entry)) continue;
                downloads.add(new FileDownloadTask(
                        NetworkUtils.toURL(entry.url),
                        local,
                        new FileDownloadTask.IntegrityCheck(normalizeHashAlgorithm(entry.hashType), entry.hash)
                ));
            }
            return Task.allOf(downloads);
        });
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
