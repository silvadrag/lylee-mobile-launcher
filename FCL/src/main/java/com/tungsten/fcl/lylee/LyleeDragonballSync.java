package com.tungsten.fcl.lylee;

import com.tungsten.fclcore.task.Task;
import com.tungsten.fclcore.util.gson.JsonUtils;
import com.tungsten.fclcore.util.io.NetworkUtils;

import java.io.File;
import java.io.IOException;

/**
 * Đồng bộ modpack Lylee Dragonball (serverProfileId = 4) vào version "LyleeDragonball"
 * của FCL — dùng chung endpoint backend thật (GET /api/servers/4/manifest).
 * Hỗ trợ tự động các thư mục mods, config, resourcepacks, shaderpacks và plugins.
 */
public class LyleeDragonballSync {

    public static final String MANIFEST_URL =
            "https://lylee-launcher-api.silvadrag2006.workers.dev/api/servers/2/manifest";

    private LyleeDragonballSync() {
    }

    /**
     * @param gameDir thư mục gốc của version FCL "LyleeDragonball"
     * @return Task tải xong toàn bộ file thiếu/sai hash trong manifest.
     */
    public static Task<?> sync(File gameDir) {
        return Task.composeAsync(() -> fetchManifest().thenComposeAsync(manifest -> syncFiles(gameDir, manifest)));
    }

    /** Chỉ lấy manifest từ server profile 4, không tải file. */
    public static Task<LyleeManifest> fetchManifest() {
        return Task.supplyAsync(() -> {
            String json = NetworkUtils.doGet(NetworkUtils.toURL(MANIFEST_URL));
            LyleeManifest manifest = JsonUtils.GSON.fromJson(json, LyleeManifest.class);
            if (manifest == null || manifest.files == null || manifest.files.isEmpty()) {
                throw new IOException("Không lấy được danh sách file modpack từ server Lylee Dragonball (manifest rỗng hoặc sai định dạng).");
            }
            return manifest;
        });
    }

    /** Tái sử dụng cơ chế so sánh hash và download song song từ LyleeCobblemonSync */
    public static Task<?> syncFiles(File gameDir, LyleeManifest manifest) {
        return LyleeCobblemonSync.syncFiles(gameDir, manifest);
    }
}
