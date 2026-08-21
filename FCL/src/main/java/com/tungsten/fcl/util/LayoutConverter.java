package com.tungsten.fcl.util;

import android.os.Build;

/**
 * Gọi qua JNI tới thư viện native {@code libcc.so} biên dịch từ project control-converter
 * để chuyển đổi giữa layout điều khiển của FCL và layout ZalithLauncher2 (ZL2).
 *
 * Nguồn thư viện native: d:\project\control-converter\go (hiện thực bằng Go, biên dịch c-shared)
 * Vị trí đóng gói: jniLibs/arm64-v8a/libcc.so
 * Gọi qua JNI để tránh vấn đề tương thích của cách thực thi lệnh trên Android.
 */
public final class LayoutConverter {

    static {
        System.loadLibrary("cc");
    }

    private LayoutConverter() {
    }

    /** Thiết bị hiện tại có hỗ trợ chạy chuyển đổi không (chỉ arm64-v8a). */
    public static boolean isSupported() {
        for (String abi : Build.SUPPORTED_ABIS) {
            if ("arm64-v8a".equals(abi)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Phương thức native JNI: chuyển JSON layout điều khiển FCL sang định dạng ZL2.
     *
     * @param inputPath  Đường dẫn file JSON layout điều khiển FCL
     * @param outputPath Đường dẫn file JSON ZL2 xuất ra sau khi chuyển đổi
     * @return Chuyển đổi thành công trả về null; thất bại trả về thông báo lỗi
     */
    private static native String convertFclToZl2Native(String inputPath, String outputPath);

    /**
     * Chuyển JSON layout điều khiển FCL sang định dạng ZL2.
     * <p>Chạy đồng bộ chặn luồng, bên gọi cần thực thi ở luồng nền.
     *
     * @param input  File JSON layout điều khiển FCL
     * @param output File JSON ZL2 xuất ra sau khi chuyển đổi
     * @return Chuyển đổi thành công trả về null; thất bại trả về thông báo lỗi
     */
    public static String convertFclToZl2(java.io.File input, java.io.File output) {
        try {
            return convertFclToZl2Native(input.getAbsolutePath(), output.getAbsolutePath());
        } catch (Throwable t) {
            return t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }
}
