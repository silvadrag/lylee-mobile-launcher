package com.tungsten.fcl.lylee;

import com.google.gson.reflect.TypeToken;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fclcore.util.io.HttpRequest;

import java.util.List;

/**
 * Số người chơi đang online server Lylee Cobblemon — cùng {@code GET /api/servers}
 * mà launcher PC đã dùng thật ({@code ServerStatusService.cs}), lần đầu bên
 * mobile gọi tới (xem docs/PLAN.md mục 19). Trả về danh sách MỌI server, ở
 * đây chỉ lọc lấy đúng {@link LyleeCobblemonConnector#VERSION_NAME} tương ứng
 * (serverProfileId cố định = 1, xem {@link LyleeCobblemonSync#MANIFEST_URL}).
 */
public final class LyleeServerStatus {

    private static final String URL = "https://lylee-launcher-api.lyleelauncher.workers.dev/api/servers";
    private static final int SERVER_PROFILE_ID = 1;

    private LyleeServerStatus() {
    }

    private static final class ServerProfileResponse {
        int serverProfileId;
        String name;
        String description;
        Integer onlinePlayers;
    }

    /** @return số người chơi online, null nếu server không báo số liệu hoặc lỗi mạng — không ném exception. */
    public static Task<Integer> fetchOnlinePlayers() {
        return Task.supplyAsync(() -> {
            try {
                List<ServerProfileResponse> servers = HttpRequest.GET(URL)
                        .getJson(new TypeToken<List<ServerProfileResponse>>() {
                        });
                if (servers == null) return null;
                return servers.stream()
                        .filter(s -> s.serverProfileId == SERVER_PROFILE_ID)
                        .findFirst()
                        .map(s -> s.onlinePlayers)
                        .orElse(null);
            } catch (Exception e) {
                return null;
            }
        });
    }
}
