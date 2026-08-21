package com.tungsten.fcllibrary.skin.cube.arm.normal;

import com.tungsten.fcllibrary.skin.cube.LimbCube;

/**
 * Khối lập phương tay trái thể hình thường - kế thừa từ LimbCube
 * Kích thước: 4x12x4 (kích thước tay chuẩn Minecraft)
 * Vị trí: lệch trục X 6 (bên trái thân), lệch trục Y 2 (thẳng đáy thân)
 * Xoay: trục xoay chính là trục X, điều khiển tay vung (ngược chiều tay phải)
 */
public class LeftArm extends LimbCube {

    protected float[] leftArmTexCoordinates;
    
    public LeftArm(float scale) {
        super(4.0f * scale, 12.0f * scale, 4.0f * scale, 6.0f * scale, 2.0f * scale, 0.0f * scale,
                1.0f, 0.0f, 0.0f,     // Trục xoay chính: trục X (tay vung trước sau)
                -0.5f, 10.0f, -10.0f, // Góc chính: bước -0.5 (ngược chiều tay phải)
                0.0f, 1.0f, 0.0f,     // Trục xoay phụ: trục Y (tay vung trong ngoài)
                0.333f, 20.0f, -20.0f); // Góc phụ: bước 0.333
        
        // Mảng tọa độ texture (tọa độ UV, chuẩn hóa 0-1)
        // Vùng tay trái: vùng dưới bên trái skin (ứng UV ngang 0.5-0.75, dọc 0.75-1.0)
        this.leftArmTexCoordinates = new float[] {
                0.5625f, 1.0f, 0.5625f, 0.90625f, 0.625f, 0.90625f, 0.625f, 1.0f,
                0.5625f, 0.90625f, 0.5625f, 0.8125f, 0.625f, 0.8125f, 0.625f, 0.90625f,
                0.5625f, 0.8125f, 0.5625f, 0.75f, 0.625f, 0.75f, 0.625f, 0.8125f,
                0.625f, 0.8125f, 0.625f, 0.75f, 0.6875f, 0.75f, 0.6875f, 0.8125f,
                0.625f, 1.0f, 0.625f, 0.90625f, 0.6875f, 0.90625f, 0.6875f, 1.0f,
                0.625f, 0.90625f, 0.625f, 0.8125f, 0.6875f, 0.8125f, 0.6875f, 0.90625f,
                0.5f, 1.0f, 0.5f, 0.90625f, 0.5625f, 0.90625f, 0.5625f, 1.0f,
                0.5f, 0.90625f, 0.5f, 0.8125f, 0.5625f, 0.8125f, 0.5625f, 0.90625f,
                0.6875f, 1.0f, 0.6875f, 0.90625f, 0.75f, 0.90625f, 0.75f, 1.0f,
                0.6875f, 0.90625f, 0.6875f, 0.8125f, 0.75f, 0.8125f, 0.75f, 0.90625f
        };
        addTextures(this.leftArmTexCoordinates);
    }
}
