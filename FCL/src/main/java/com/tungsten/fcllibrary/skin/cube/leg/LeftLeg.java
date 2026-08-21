package com.tungsten.fcllibrary.skin.cube.leg;

import com.tungsten.fcllibrary.skin.cube.LimbCube;

/**
 * Khối lập phương chân trái - kế thừa từ LimbCube
 * Kích thước: 4x12x4 (kích thước chân chuẩn Minecraft)
 * Vị trí: lệch trục X 2 (bên trái thân), lệch trục Y -10 (dưới thân)
 * Xoay: trục xoay chính là trục X, điều khiển chân vung (hoạt ảnh đi bộ)
 */
public class LeftLeg extends LimbCube {

    protected float[] leftLegTexCoordinates;
    
    public LeftLeg(float scale) {
        super(4.0f * scale, 12.0f * scale, 4.0f * scale, 2.0f * scale, -10.0f * scale, 0.0f * scale,
                1.0f, 0.0f, 0.0f,    // Trục xoay chính: trục X (chân vung)
                1.5f, 30.0f, -30.0f,  // Góc chính: bước 1.5 (ngược chiều chân phải)
                0.0f, 1.0f, 0.0f,    // Trục xoay phụ: trục Y (chân vung trong ngoài)
                -0.5f, 30.0f, -30.0f); // Góc phụ: bước -0.5
        
        // Mảng tọa độ texture (tọa độ UV, chuẩn hóa 0-1)
        // Vùng chân trái: vùng dưới giữa skin (ứng UV ngang 0.25-0.5, dọc 0.75-1.0)
        this.leftLegTexCoordinates = new float[] {
                0.3125f, 1.0f, 0.3125f, 0.90625f, 0.375f, 0.90625f, 0.375f, 1.0f,
                0.3125f, 0.90625f, 0.3125f, 0.8125f, 0.375f, 0.8125f, 0.375f, 0.90625f,
                0.3125f, 0.8125f, 0.3125f, 0.75f, 0.375f, 0.75f, 0.375f, 0.8125f,
                0.375f, 0.8125f, 0.375f, 0.75f, 0.4375f, 0.75f, 0.4375f, 0.8125f,
                0.375f, 1.0f, 0.375f, 0.90625f, 0.4375f, 0.90625f, 0.4375f, 1.0f,
                0.375f, 0.90625f, 0.375f, 0.8125f, 0.4375f, 0.8125f, 0.4375f, 0.90625f,
                0.25f, 1.0f, 0.25f, 0.90625f, 0.3125f, 0.90625f, 0.3125f, 1.0f,
                0.25f, 0.90625f, 0.25f, 0.8125f, 0.3125f, 0.8125f, 0.3125f, 0.90625f,
                0.4375f, 1.0f, 0.4375f, 0.90625f, 0.5f, 0.90625f, 0.5f, 1.0f,
                0.4375f, 0.90625f, 0.4375f, 0.8125f, 0.5f, 0.8125f, 0.5f, 0.90625f
        };
        addTextures(this.leftLegTexCoordinates);
    }
}
