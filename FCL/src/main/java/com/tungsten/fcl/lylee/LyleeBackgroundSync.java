package com.tungsten.fcl.lylee;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import com.google.gson.reflect.TypeToken;
import com.tungsten.fclauncher.utils.FCLPath;
import com.tungsten.fclcore.task.FileDownloadTask;
import com.tungsten.fclcore.task.Schedulers;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fclcore.task.TaskExecutor;
import com.tungsten.fclcore.util.io.HttpRequest;
import com.tungsten.fclcore.util.io.NetworkUtils;
import com.tungsten.fcllibrary.component.theme.ThemeEngine;

import java.io.File;
import java.util.List;

/**
 * Nền toàn app tùy chỉnh qua URL server (xem docs/PLAN.md) — cùng
 * {@code GET /api/servers} mà {@link LyleeServerStatus} đã dùng (field mới
 * {@code backgroundImageUrl} trên ServerProfile). Tải về + áp dụng qua
 * {@link ThemeEngine#applyAndSave} có sẵn (đúng cơ chế "nền tùy chỉnh cục bộ"
 * launcher đã có, chỉ khác nguồn ảnh là URL server thay vì người dùng tự chọn
 * file) — MainActivity đã có sẵn {@code themeRefreshListener} tự vẽ lại nền
 * ngay khi theme đổi, không cần code thêm gì phía hiển thị.
 * <p>
 * Best-effort, im lặng bỏ qua mọi lỗi — không có nền tùy chỉnh vẫn dùng đúng
 * ảnh mặc định đóng gói sẵn như trước giờ, không chặn gì khác. Chỉ tải lại khi
 * URL đổi so với lần áp dụng gần nhất (đánh dấu trong SharedPreferences) —
 * tránh tải lại mỗi lần mở app trong khi ảnh không đổi (file cục bộ đã có sẵn,
 * ThemeEngine.setupThemeEngine tự nạp lại từ đó mỗi lần khởi động).
 */
public final class LyleeBackgroundSync {

    private static final String URL = "https://lylee-launcher-api.lyleelauncher.workers.dev/api/servers";
    private static final int SERVER_PROFILE_ID = 1;
    private static final String PREF_KEY = "lylee_background_applied_url";

    private LyleeBackgroundSync() {
    }

    private static final class ServerProfileResponse {
        int serverProfileId;
        String backgroundImageUrl;
    }

    public static void applyServerBackground(Context context, View view) {
        Task.supplyAsync(() -> {
                    try {
                        List<ServerProfileResponse> servers = HttpRequest.GET(URL)
                                .getJson(new TypeToken<List<ServerProfileResponse>>() {
                                });
                        if (servers == null) return null;
                        return servers.stream()
                                .filter(s -> s.serverProfileId == SERVER_PROFILE_ID)
                                .findFirst()
                                .map(s -> s.backgroundImageUrl)
                                .orElse(null);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .thenAcceptAsync(Schedulers.androidUIThread(), url -> {
                    if (url == null || url.isBlank()) return;
                    SharedPreferences prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE);
                    if (url.equals(prefs.getString(PREF_KEY, null))) return;
                    downloadAndApply(context, view, url, prefs);
                }).start();
    }

    private static void downloadAndApply(Context context, View view, String url, SharedPreferences prefs) {
        Schedulers.androidUIThread().execute(() -> {
            TaskExecutor executor = Task.composeAsync(() -> {
                File dest = new File(FCLPath.CACHE_DIR, "lylee_background.png");
                FileDownloadTask task = new FileDownloadTask(NetworkUtils.toURL(url), dest);
                return task.whenComplete(Schedulers.androidUIThread(), exception -> {
                    if (exception == null) {
                        ThemeEngine.getInstance().applyAndSave(context, view, dest.getAbsolutePath(), dest.getAbsolutePath());
                        prefs.edit().putString(PREF_KEY, url).apply();
                    }
                });
            }).executor();
            executor.start();
        });
    }
}
