package com.tungsten.fcllibrary.skin.cube.leg;

import com.tungsten.fcllibrary.skin.cube.LimbCube;

/**
 * Khối lập phương áo khoác/lớp phủ chân phải - kế thừa từ LimbCube
 * Kích thước: lớn hơn chân phải 1 chút (4.24x12.71x4.24), tạo hiệu ứng giày
 * Vị trí: trùng với chân phải, vẽ đè lên trên chân phải
 */
public class RightLegOverlay extends LimbCube {
    
    protected float[] rightLegOverlayTexCoordinates;
    
    public RightLegOverlay(float scale) {
        super(4.2352943f * scale, 12.705883f * scale, 4.2352943f * scale, -2.0f * scale, -10.0f * scale, 0.0f * scale,
                1.0f, 0.0f, 0.0f,    // Trục xoay chính: trục X
                -1.5f, 30.0f, -30.0f, // Tham số góc chính
                0.0f, 1.0f, 0.0f,    // Trục xoay phụ: trục Y
                0.5f, 30.0f, -30.0f); // Tham số góc phụ
        
        // Mảng tọa độ texture (tọa độ UV, chuẩn hóa 0-1)
        // Vùng áo khoác chân phải: vùng giữa bên trái skin (ứng UV ngang 0-0.25, dọc 0.5-0.75)
        this.rightLegOverlayTexCoordinates = new float[] {
                0.0625f, 0.75f, 0.0625f, 0.65625f, 0.125f, 0.65625f, 0.125f, 0.75f,
                0.0625f, 0.65625f, 0.0625f, 0.5625f, 0.125f, 0.5625f, 0.125f, 0.65625f,
                0.0625f, 0.5625f, 0.0625f, 0.5f, 0.125f, 0.5f, 0.125f, 0.5625f,
                0.125f, 0.5625f, 0.125f, 0.5f, 0.1875f, 0.5f, 0.1875f, 0.5625f,
                0.125f, 0.75f, 0.125f, 0.65625f, 0.1875f, 0.65625f, 0.1875f, 0.75f,
                0.125f, 0.65625f, 0.125f, 0.5625f, 0.1875f, 0.5625f, 0.1875f, 0.65625f,
                0.0f, 0.75f, 0.0f, 0.65625f, 0.0625f, 0.65625f, 0.0625f, 0.75f,
                0.0f, 0.65625f, 0.0f, 0.5625f, 0.0625f, 0.5625f, 0.0625f, 0.65625f,
                0.1875f, 0.75f, 0.1875f, 0.65625f, 0.25f, 0.65625f, 0.25f, 0.75f,
                0.1875f, 0.65625f, 0.1875f, 0.5625f, 0.25f, 0.5625f, 0.25f, 0.65625f
        };
        addTextures(this.rightLegOverlayTexCoordinates);
    }
}
