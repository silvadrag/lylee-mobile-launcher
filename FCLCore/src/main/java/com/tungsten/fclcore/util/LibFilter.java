package com.tungsten.fclcore.util;

import static com.tungsten.fclcore.util.gson.JsonUtils.GSON;

import com.tungsten.fclcore.game.Library;
import com.tungsten.fclcore.game.Version;

import java.util.ArrayList;
import java.util.List;

/**
 * Lọc/thay thư viện phụ thuộc trong JSON version game trước khi khởi chạy.
 * <p>
 * Mục đích lọc:
 * <ul>
 *   <li>Gỡ thư viện LWJGL — lúc chạy game dùng LWJGL đi kèm FCL (app_runtime/lwjgl/3.3.3, 3.4.1)</li>
 *   <li>Gỡ thư viện hỗ trợ input/livestream thời LWJGL2 (jinput-platform, twitch-platform), trên Android đã có FCL input bridge thay thế</li>
 *   <li>Thay thư viện cũ không tương thích Java 17+ / Android mới (asm, jna, oshi) bằng bản đã tương thích</li>
 * </ul>
 */
public class LibFilter {

    // asm-all 5.0.4 là jar gộp cuối cùng; asm < 5 không phân tích được class file biên dịch bằng Java 17,
    // modpack cũ (Forge 1.7.x/1.8.x) thường kèm asm 4.x, cần thay toàn bộ
    private static final String ASM_ALL_5_0_4_STRING = "{\n" +
            "      \"name\": \"org.ow2.asm:asm-all:5.0.4\",\n" +
            "      \"downloads\": {\n" +
            "        \"artifact\": {\n" +
            "          \"path\": \"org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar\",\n" +
            "          \"sha1\": \"e6244859997b3d4237a552669279780876228909\",\n" +
            "          \"url\": \"https://repo1.maven.org/maven2/org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar\"\n" +
            "        }\n" +
            "      }\n" +
            "    }";
    // JNA từ 5.13 mới hỗ trợ Android mới (hành vi System.loadLibrary đổi từ API 30+), bản cũ sẽ lỗi lúc khởi chạy
    private static final String JNA_5_13_STRING = "{\n" +
            "      \"name\": \"net.java.dev.jna:jna:5.13.0\",\n" +
            "      \"downloads\": {\n" +
            "        \"artifact\": {\n" +
            "          \"path\": \"net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar\",\n" +
            "          \"sha1\": \"1200e7ebeedbe0d10062093f32925a912020e747\",\n" +
            "          \"url\": \"https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar\"\n" +
            "        }\n" +
            "      }\n" +
            "    }";
    // oshi-core 6.2 lấy thông tin hệ thống bị lỗi trên môi trường Android/Java 17, đã sửa ở 6.3
    private static final String OSHI_6_3_STRING = "{\n" +
            "      \"name\": \"com.github.oshi:oshi-core:6.3.0\",\n" +
            "      \"downloads\": {\n" +
            "        \"artifact\": {\n" +
            "          \"path\": \"com/github/oshi/oshi-core/6.3.0/oshi-core-6.3.0.jar\",\n" +
            "          \"sha1\": \"9e98cf55be371cafdb9c70c35d04ec2a8c2b42ac\",\n" +
            "          \"url\": \"https://repo1.maven.org/maven2/com/github/oshi/oshi-core/6.3.0/oshi-core-6.3.0.jar\"\n" +
            "        }\n" +
            "      }\n" +
            "    }";

    private static final Library ASM_ALL_5_0_4 = GSON.fromJson(ASM_ALL_5_0_4_STRING, Library.class);
    private static final Library JNA_5_13 = GSON.fromJson(JNA_5_13_STRING, Library.class);
    private static final Library OSHI_6_3 = GSON.fromJson(OSHI_6_3_STRING, Library.class);

    /**
     * Lọc dependency của version, mặc định bỏ qua thư viện LWJGL
     */
    public static Version filter(Version version) {
        return version.setLibraries(filterLibs(version.getLibraries(), true));
    }

    /**
     * Lọc dependency của version, skipLwjgl quyết định có gỡ thư viện LWJGL hay không
     */
    public static Version filter(Version version, boolean skipLwjgl) {
        return version.setLibraries(filterLibs(version.getLibraries(), skipLwjgl));
    }

    public static List<Library> filterLibs(List<Library> libraries, boolean skipLwjgl) {
        ArrayList<Library> newLibraries = new ArrayList<>();
        for (Library library : libraries) {
            // Lọc thư viện LWJGL chính thức (org.lwjgl:* / org.lwjgl.lwjgl:*)
            if (skipLwjgl && library.getName().contains("org.lwjgl"))
                continue;
            // jinput-platform / twitch-platform là dependency input và livestream của LWJGL2, trên Android đã có FCL input bridge thay thế
            if (!library.getName().contains("jinput-platform") && !library.getName().contains("twitch-platform")) {
                String[] version = library.getName().split(":")[2].split("\\.");
                if (library.getArtifactId().equals("asm-all") && Integer.parseInt(version[0]) < 5) {
                    // asm < 5 không phân tích được class file Java 17, thay bằng 5.0.4
                    newLibraries.add(ASM_ALL_5_0_4);
                } else if (library.getName().startsWith("net.java.dev.jna:jna:")) {
                    if (Integer.parseInt(version[0]) >= 5 && Integer.parseInt(version[1]) >= 13) {
                        newLibraries.add(library);
                    } else {
                        // jna < 5.13 không tương thích Android mới, thay bằng 5.13.0
                        newLibraries.add(JNA_5_13);
                    }
                } else if (library.getName().startsWith("com.github.oshi:oshi-core:")) {
                    if (Integer.parseInt(version[0]) != 6 || Integer.parseInt(version[1]) != 2) {
                        newLibraries.add(library);
                    } else {
                        // oshi-core 6.2 lấy thông tin hệ thống bị lỗi trên Android, thay bằng 6.3.0
                        newLibraries.add(OSHI_6_3);
                    }
                } else {
                    newLibraries.add(library);
                }
            }
        }
        return newLibraries;
    }

}