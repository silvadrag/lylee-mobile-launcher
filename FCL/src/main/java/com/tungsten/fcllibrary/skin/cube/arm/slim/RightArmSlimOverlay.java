package com.tungsten.fcllibrary.skin.cube.arm.slim;

import com.tungsten.fcllibrary.skin.cube.LimbCube;

/**
 * Khối lập phương áo khoác/lớp phủ tay phải thể hình mảnh - kế thừa từ LimbCube
 * Kích thước: lớn hơn tay phải mảnh 1 chút, tạo hiệu ứng áo khoác
 * Vị trí: trùng với tay phải mảnh, vẽ đè lên trên tay phải
 */
public class RightArmSlimOverlay extends LimbCube {

    protected float[] rightArmSlimOverlayTexCoordinates;
    
    public RightArmSlimOverlay(float scale) {
        super(3.1764708f * scale, 12.705883f * scale, 4.2352943f * scale, -5.5f * scale, 2.0f * scale, 0.0f * scale,
                1.0f, 0.0f, 0.0f,    // Trục xoay chính: trục X
                0.5f, 10.0f, -10.0f, // Tham số góc chính
                0.0f, 1.0f, 0.0f,    // Trục xoay phụ: trục Y
                -0.333f, 20.0f, -20.0f); // Tham số góc phụ
        
        // Mảng tọa độ texture (tọa độ UV, chuẩn hóa 0-1)
        this.rightArmSlimOverlayTexCoordinates = new float[] {
                0.6875f, 0.75f, 0.6875f, 0.65625f, 0.734375f, 0.65625f, 0.734375f, 0.75f,
                0.6875f, 0.65625f, 0.6875f, 0.5625f, 0.734375f, 0.5625f, 0.734375f, 0.65625f,
                0.6875f, 0.5625f, 0.6875f, 0.5f, 0.734375f, 0.5f, 0.734375f, 0.5625f,
                0.734375f, 0.5625f, 0.734375f, 0.5f, 0.78125f, 0.5f, 0.78125f, 0.5625f,
                0.734375f, 0.75f, 0.734375f, 0.65625f, 0.796875f, 0.65625f, 0.796875f, 0.75f,
                0.734375f, 0.65625f, 0.734375f, 0.5625f, 0.796875f, 0.5625f, 0.796875f, 0.65625f,
                0.625f, 0.75f, 0.625f, 0.65625f, 0.6875f, 0.65625f, 0.6875f, 0.75f,
                0.625f, 0.65625f, 0.625f, 0.5625f, 0.6875f, 0.5625f, 0.6875f, 0.65625f,
                0.796875f, 1.0f, 0.796875f, 0.90625f, 0.84375f, 0.90625f, 0.84375f, 1.0f,
                0.796875f, 0.90625f, 0.796875f, 0.8125f, 0.84375f, 0.8125f, 0.84375f, 0.90625f
        };
        addTextures(this.rightArmSlimOverlayTexCoordinates);
    }
}
