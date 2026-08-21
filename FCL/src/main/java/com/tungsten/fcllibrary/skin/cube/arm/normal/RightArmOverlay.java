package com.tungsten.fcllibrary.skin.cube.arm.normal;

import com.tungsten.fcllibrary.skin.cube.LimbCube;

/**
 * Khối lập phương áo khoác/lớp phủ tay phải thể hình thường - kế thừa từ LimbCube
 * Kích thước: lớn hơn tay phải 1 chút (4.24x12.71x4.24), tạo hiệu ứng áo khoác
 * Vị trí: trùng với tay phải, vẽ đè lên trên tay phải
 */
public class RightArmOverlay extends LimbCube {

    protected float[] rightArmOverlayTexCoordinates;
    
    public RightArmOverlay(float scale) {
        super(4.2352943f * scale, 12.705883f * scale, 4.2352943f * scale, -6.0f * scale, 2.0f * scale, 0.0f * scale,
                1.0f, 0.0f, 0.0f,    // Trục xoay chính: trục X
                0.5f, 10.0f, -10.0f, // Tham số góc chính
                0.0f, 1.0f, 0.0f,    // Trục xoay phụ: trục Y
                -0.333f, 20.0f, -20.0f); // Tham số góc phụ
        
        // Mảng tọa độ texture (tọa độ UV, chuẩn hóa 0-1)
        // Vùng áo khoác tay phải: vùng dưới bên phải skin (ứng UV ngang 0.625-0.875, dọc 0.5-0.75)
        this.rightArmOverlayTexCoordinates = new float[] {
                0.6875f, 0.75f, 0.6875f, 0.65625f, 0.75f, 0.65625f, 0.75f, 0.75f,
                0.6875f, 0.65625f, 0.6875f, 0.5625f, 0.75f, 0.5625f, 0.75f, 0.65625f,
                0.6875f, 0.5625f, 0.6875f, 0.5f, 0.75f, 0.5f, 0.75f, 0.5625f,
                0.75f, 0.5625f, 0.75f, 0.5f, 0.8125f, 0.5f, 0.8125f, 0.5625f,
                0.75f, 0.75f, 0.75f, 0.65625f, 0.8125f, 0.65625f, 0.8125f, 0.75f,
                0.75f, 0.65625f, 0.75f, 0.5625f, 0.8125f, 0.5625f, 0.8125f, 0.65625f,
                0.625f, 0.75f, 0.625f, 0.65625f, 0.6875f, 0.65625f, 0.6875f, 0.75f,
                0.625f, 0.65625f, 0.625f, 0.5625f, 0.6875f, 0.5625f, 0.6875f, 0.65625f,
                0.8125f, 0.75f, 0.8125f, 0.65625f, 0.875f, 0.65625f, 0.875f, 0.75f,
                0.8125f, 0.65625f, 0.8125f, 0.5625f, 0.875f, 0.5625f, 0.875f, 0.65625f
        };
        addTextures(this.rightArmOverlayTexCoordinates);
    }
}
