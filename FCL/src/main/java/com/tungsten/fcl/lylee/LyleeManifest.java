package com.tungsten.fcl.lylee;

import java.util.List;

/**
 * DTO khớp CHÍNH XÁC {@code Dtos.ManifestResponse}/{@code Dtos.FileEntryResponse}
 * phía backend Java thật (fabric-lyleelauncherAPI-mod-1.21.1, endpoint
 * {@code GET /api/servers/{id}/manifest}) — CÙNG 1 endpoint launcher PC (WPF)
 * đang dùng thật, không phải endpoint riêng bịa ra cho mobile. Tên field phải
 * khớp đúng tên JSON (Gson mặc định dùng tên field, không có @SerializedName).
 */
public class LyleeManifest {

    public String launcherVersion;
    public String minecraftVersion;
    public String loaderType;
    public String loaderVersion;
    public String javaArgs;
    public List<FileEntry> files;
    public String launcherDownloadUrl;

    public static class FileEntry {
        public String path;
        public String url;
        public String hash;
        public String hashType;
        public long size;
    }
}
