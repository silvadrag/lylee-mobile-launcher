package com.tungsten.fcllibrary.skin.cube.leg;

import com.tungsten.fcllibrary.skin.cube.LimbCube;

/**
 * Khối lập phương áo khoác/lớp phủ chân trái - kế thừa từ LimbCube
 * Kích thước: lớn hơn chân trái 1 chút (4.24x12.71x4.24), tạo hiệu ứng giày
 * Vị trí: trùng với chân trái, vẽ đè lên trên chân trái
 */
public class LeftLegOverlay extends LimbCube {

    protected float[] leftLegOverlayTexCoordinates;
    
    public LeftLegOverlay(float scale) {
        super(4.2352943f * scale, 12.705883f * scale, 4.2352943f * scale, 2.0f * scale, -10.0f * scale, 0.0f * scale,
                1.0f, 0.0f, 0.0f,    // Trục xoay chính: trục X
                1.5f, 30.0f, -30.0f,  // Tham số góc chính
                0.0f, 1.0f, 0.0f,    // Trục xoay phụ: trục Y
                -0.5f, 30.0f, -30.0f); // Tham số góc phụ
        
        // Mảng tọa độ texture (tọa độ UV, chuẩn hóa 0-1)
        // Vùng áo khoác chân trái: vùng dưới bên trái skin (ứng UV ngang 0-0.25, dọc 0.75-1.0)
        this.leftLegOverlayTexCoordinates = new float[] {
                0.0625f, 1.0f, 0.0625f, 0.90625f, 0.125f, 0.90625f, 0.125f, 1.0f,
                0.0625f, 0.90625f, 0.0625f, 0.8125f, 0.125f, 0.8125f, 0.125f, 0.90625f,
                0.0625f, 0.8125f, 0.0625f, 0.75f, 0.125f, 0.75f, 0.125f, 0.8125f,
                0.125f, 0.8125f, 0.125f, 0.75f, 0.1875f, 0.75f, 0.1875f, 0.8125f,
                0.125f, 1.0f, 0.125f, 0.90625f, 0.1875f, 0.90625f, 0.1875f, 1.0f,
                0.125f, 0.90625f, 0.125f, 0.8125f, 0.1875f, 0.8125f, 0.1875f, 0.90625f,
                0.0f, 1.0f, 0.0f, 0.90625f, 0.0625f, 0.90625f, 0.0625f, 1.0f,
                0.0f, 0.90625f, 0.0f, 0.8125f, 0.0625f, 0.8125f, 0.0625f, 0.90625f,
                0.1875f, 1.0f, 0.1875f, 0.90625f, 0.25f, 0.90625f, 0.25f, 1.0f,
                0.1875f, 0.90625f, 0.1875f, 0.8125f, 0.25f, 0.8125f, 0.25f, 0.90625f
        };
        addTextures(this.leftLegOverlayTexCoordinates);
    }
}
