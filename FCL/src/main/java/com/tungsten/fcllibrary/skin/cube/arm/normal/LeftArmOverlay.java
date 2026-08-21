package com.tungsten.fcllibrary.skin.cube.arm.normal;

import com.tungsten.fcllibrary.skin.cube.LimbCube;

/**
 * Khối lập phương áo khoác/lớp phủ tay trái thể hình thường - kế thừa từ LimbCube
 * Kích thước: lớn hơn tay trái 1 chút (4.24x12.71x4.24), tạo hiệu ứng áo khoác
 * Vị trí: trùng với tay trái, vẽ đè lên trên tay trái
 */
public class LeftArmOverlay extends LimbCube {

    protected float[] leftArmOverlayTexCoordinates;
    
    public LeftArmOverlay(float scale) {
        super(4.2352943f * scale, 12.705883f * scale, 4.2352943f * scale, 6.0f * scale, 2.0f * scale, 0.0f * scale,
                1.0f, 0.0f, 0.0f,    // Trục xoay chính: trục X
                -0.5f, 10.0f, -10.0f, // Tham số góc chính
                0.0f, 1.0f, 0.0f,     // Trục xoay phụ: trục Y
                0.333f, 20.0f, -20.0f); // Tham số góc phụ
        
        // Mảng tọa độ texture (tọa độ UV, chuẩn hóa 0-1)
        // Vùng áo khoác tay trái: vùng dưới bên phải skin (ứng UV ngang 0.75-1.0, dọc 0.75-1.0)
        this.leftArmOverlayTexCoordinates = new float[] {
                0.8125f, 1.0f, 0.8125f, 0.90625f, 0.875f, 0.90625f, 0.875f, 1.0f,
                0.8125f, 0.90625f, 0.8125f, 0.8125f, 0.875f, 0.8125f, 0.875f, 0.90625f,
                0.8125f, 0.8125f, 0.8125f, 0.75f, 0.875f, 0.75f, 0.875f, 0.8125f,
                0.875f, 0.8125f, 0.875f, 0.75f, 0.9375f, 0.75f, 0.9375f, 0.8125f,
                0.875f, 1.0f, 0.875f, 0.90625f, 0.9375f, 0.90625f, 0.9375f, 1.0f,
                0.875f, 0.90625f, 0.875f, 0.8125f, 0.9375f, 0.8125f, 0.9375f, 0.90625f,
                0.75f, 1.0f, 0.75f, 0.90625f, 0.8125f, 0.90625f, 0.8125f, 1.0f,
                0.75f, 0.90625f, 0.75f, 0.8125f, 0.8125f, 0.8125f, 0.8125f, 0.90625f,
                0.9375f, 1.0f, 0.9375f, 0.90625f, 1.0f, 0.90625f, 1.0f, 1.0f,
                0.9375f, 0.90625f, 0.9375f, 0.8125f, 1.0f, 0.8125f, 1.0f, 0.90625f
        };
        addTextures(this.leftArmOverlayTexCoordinates);
    }
}
