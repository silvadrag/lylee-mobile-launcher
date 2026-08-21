package com.tungsten.fcllibrary.skin.cube.arm.normal;

import com.tungsten.fcllibrary.skin.cube.LimbCube;

/**
 * Khối lập phương tay phải thể hình thường - kế thừa từ LimbCube
 * Kích thước: 4x12x4 (kích thước tay chuẩn Minecraft)
 * Vị trí: lệch trục X -6 (bên phải thân), lệch trục Y 2 (thẳng đáy thân)
 * Xoay: trục xoay chính là trục X, điều khiển tay vung
 */
public class RightArm extends LimbCube {

    protected float[] rightArmTexCoordinates;

    public RightArm(float scale) {
        super(4.0f * scale, 12.0f * scale, 4.0f * scale, -6.0f * scale, 2.0f * scale, 0.0f * scale,
                1.0f, 0.0f, 0.0f,    // Trục xoay chính: trục X (tay vung trước sau)
                0.5f, 10.0f, -10.0f, // Góc chính: bước 0.5, phạm vi -10 đến 10 độ
                0.0f, 1.0f, 0.0f,    // Trục xoay phụ: trục Y (tay vung trong ngoài)
                -0.333f, 20.0f, -20.0f); // Góc phụ: bước -0.333, phạm vi -20 đến 20 độ

        // Mảng tọa độ texture (tọa độ UV, chuẩn hóa 0-1)
        // Vùng tay phải: vùng giữa nửa phải skin (ứng UV ngang 0.625-0.875, dọc 0.25-0.5)
        // LimbCube dùng 10 mặt, mỗi mặt 4 đỉnh, mỗi đỉnh 2 tọa độ (u, v)
        this.rightArmTexCoordinates = new float[]{
                0.6875f, 0.5f, 0.6875f, 0.40625f, 0.75f, 0.40625f, 0.75f, 0.5f,
                0.6875f, 0.40625f, 0.6875f, 0.3125f, 0.75f, 0.3125f, 0.75f, 0.40625f,
                0.6875f, 0.3125f, 0.6875f, 0.25f, 0.75f, 0.25f, 0.75f, 0.3125f,
                0.75f, 0.3125f, 0.75f, 0.25f, 0.8125f, 0.25f, 0.8125f, 0.3125f,
                0.75f, 0.5f, 0.75f, 0.40625f, 0.8125f, 0.40625f, 0.8125f, 0.5f,
                0.75f, 0.40625f, 0.75f, 0.3125f, 0.8125f, 0.3125f, 0.8125f, 0.40625f,
                0.625f, 0.5f, 0.625f, 0.40625f, 0.6875f, 0.40625f, 0.6875f, 0.5f,
                0.625f, 0.40625f, 0.625f, 0.3125f, 0.6875f, 0.3125f, 0.6875f, 0.40625f,
                0.8125f, 0.5f, 0.8125f, 0.40625f, 0.875f, 0.40625f, 0.875f, 0.5f,
                0.8125f, 0.40625f, 0.8125f, 0.3125f, 0.875f, 0.3125f, 0.875f, 0.40625f
        };
        addTextures(this.rightArmTexCoordinates);
    }
}
