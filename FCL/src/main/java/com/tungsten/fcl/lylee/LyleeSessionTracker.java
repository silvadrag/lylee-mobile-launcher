package com.tungsten.fcl.lylee;

import com.tungsten.fclcore.task.Task;
import com.tungsten.fclcore.util.io.HttpRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Ghi nhận thời gian chơi server Lylee Cobblemon — cùng API session/playtime
 * mà launcher PC đã dùng thật ({@code GameLauncherService.cs}), lần đầu bên
 * mobile gọi tới (xem docs/PLAN.md mục 19). CHỈ áp dụng cho version
 * {@link LyleeCobblemonConnector#VERSION_NAME} (đúng tinh thần PC: bản tự do/
 * instance khác không thuộc server Lylee thì không tính giờ).
 * <p>
 * Cố tình best-effort: lỗi mạng lúc start/end không được chặn việc chơi game
 * — mobile hiện chưa có hạ tầng JWT player (chưa có đăng nhập Google), nên
 * trường hợp username bị "khóa" (đã nhận qua PC/Google) sẽ chỉ fail-êm ở đây
 * thay vì chặn launch như PC đang làm (PC có JWT thật để tự vượt qua khóa đó).
 */
public final class LyleeSessionTracker {

    private static final String BASE_URL = "https://lylee-launcher-api.lyleelauncher.workers.dev";
    private static final int SERVER_PROFILE_ID = 1;

    private LyleeSessionTracker() {
    }

    private static String encodeUsername(String username) {
        try {
            return URLEncoder.encode(username, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return username;
        }
    }

    public static final class SessionStartResponse {
        public long sessionId;
    }

    private static final class SessionStartRequest {
        final int serverProfileId;
        final String launcherVersion;

        SessionStartRequest(int serverProfileId, String launcherVersion) {
            this.serverProfileId = serverProfileId;
            this.launcherVersion = launcherVersion;
        }
    }

    private static final class PlaytimeResponse {
        public long totalSeconds;
    }

    /** @return sessionId nếu gọi thành công, null nếu lỗi (không ném exception ra ngoài — best-effort). */
    public static Long start(String username, String launcherVersion) {
        try {
            SessionStartResponse res = HttpRequest.POST(BASE_URL + "/api/players/" + encodeUsername(username) + "/session/start")
                    .json(new SessionStartRequest(SERVER_PROFILE_ID, launcherVersion))
                    .getJson(SessionStartResponse.class);
            return res == null ? null : res.sessionId;
        } catch (Exception e) {
            return null;
        }
    }

    /** Gọi trong background thread (VD onDestroy của JVMActivity) — không quăng exception. */
    public static void end(String username, long sessionId) {
        try {
            HttpRequest.POST(BASE_URL + "/api/players/" + encodeUsername(username) + "/session/" + sessionId + "/end")
                    .json("{}")
                    .getString();
        } catch (Exception ignore) {
        }
    }

    /** Tổng thời gian chơi (giây), null nếu lỗi mạng — dùng Task để gọi async từ UI. */
    public static Task<Long> fetchTotalPlaytimeSeconds(String username) {
        return Task.supplyAsync(() -> {
            try {
                PlaytimeResponse res = HttpRequest.GET(BASE_URL + "/api/players/" + encodeUsername(username) + "/playtime")
                        .getJson(PlaytimeResponse.class);
                return res == null ? null : res.totalSeconds;
            } catch (Exception e) {
                return null;
            }
        });
    }
}
