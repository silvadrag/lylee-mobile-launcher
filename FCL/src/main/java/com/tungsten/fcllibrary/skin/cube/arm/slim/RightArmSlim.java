package com.tungsten.fcllibrary.skin.cube.arm.slim;

import com.tungsten.fcllibrary.skin.cube.LimbCube;

/**
 * Khối lập phương tay phải thể hình mảnh - kế thừa từ LimbCube
 * Kích thước: 3x12x4 (hẹp hơn tay thể hình thường 1 pixel)
 * Vị trí: lệch trục X -5.5 (gần thân hơn thể hình thường), lệch trục Y 2
 * Xoay: trục xoay chính là trục X, điều khiển tay vung
 */
public class RightArmSlim extends LimbCube {

    protected float[] rightArmSlimTexCoordinates;
    
    public RightArmSlim(float scale) {
        super(3.0f * scale, 12.0f * scale, 4.0f * scale, -5.5f * scale, 2.0f * scale, 0.0f * scale,
                1.0f, 0.0f, 0.0f,    // Trục xoay chính: trục X
                0.5f, 10.0f, -10.0f, // Tham số góc chính
                0.0f, 1.0f, 0.0f,    // Trục xoay phụ: trục Y
                -0.333f, 20.0f, -20.0f); // Tham số góc phụ
        
        // Mảng tọa độ texture (tọa độ UV, chuẩn hóa 0-1)
        // Tay phải mảnh dùng vùng texture nén, khoảng cách UV nhỏ hơn
        this.rightArmSlimTexCoordinates = new float[] {
                0.6875f, 0.5f, 0.6875f, 0.40625f, 0.734375f, 0.40625f, 0.734375f, 0.5f,
                0.6875f, 0.40625f, 0.6875f, 0.3125f, 0.734375f, 0.3125f, 0.734375f, 0.40625f,
                0.6875f, 0.3125f, 0.6875f, 0.25f, 0.734375f, 0.25f, 0.734375f, 0.3125f,
                0.734375f, 0.3125f, 0.734375f, 0.25f, 0.78125f, 0.25f, 0.78125f, 0.3125f,
                0.734375f, 0.5f, 0.734375f, 0.40625f, 0.796875f, 0.40625f, 0.796875f, 0.5f,
                0.734375f, 0.40625f, 0.734375f, 0.3125f, 0.796875f, 0.3125f, 0.796875f, 0.40625f,
                0.625f, 0.5f, 0.625f, 0.40625f, 0.6875f, 0.40625f, 0.6875f, 0.5f,
                0.625f, 0.40625f, 0.625f, 0.3125f, 0.6875f, 0.3125f, 0.6875f, 0.40625f,
                0.796875f, 0.5f, 0.796875f, 0.40625f, 0.84375f, 0.40625f, 0.84375f, 0.5f,
                0.796875f, 0.40625f, 0.796875f, 0.3125f, 0.84375f, 0.3125f, 0.84375f, 0.40625f
        };
        addTextures(this.rightArmSlimTexCoordinates);
    }
}
