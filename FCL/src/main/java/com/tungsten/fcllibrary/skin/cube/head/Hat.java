package com.tungsten.fcllibrary.skin.cube.head;

import com.tungsten.fcllibrary.skin.cube.MainCube;

/**
 * Khối lập phương mũ - kế thừa từ MainCube
 * Kích thước: 9x9x9 (lớn hơn đầu 1 chút, tạo hiệu ứng mũ)
 * Vị trí: trùng với đầu, vẽ đè lên trên đầu
 */
public class Hat extends MainCube {

    protected float[] hatTexCoordinates;
    
    public Hat(float scale) {
        super(9.0f * scale, 9.0f * scale, 9.0f * scale, 0.0f * scale, 12.0f * scale, 0.0f * scale);
        
        // Mảng tọa độ texture (tọa độ UV, chuẩn hóa 0-1)
        // Vùng mũ: khối 8x8 pixel góc trên phải skin (ứng phạm vi UV 0.5-0.75)
        // Thứ tự mặt: trước (Face 0), trên (Face 1), dưới (Face 2), phải (Face 3), trái (Face 4), sau (Face 5)
        this.hatTexCoordinates = new float[] {
                // Face 0: mặt trước - dòng thứ 2 vùng trên phải của skin
                0.625f, 0.25f,    // dưới trái uv(40,16)
                0.625f, 0.125f,   // trên trái uv(40,8)
                0.75f, 0.125f,    // trên phải uv(48,8)
                0.75f, 0.25f,     // dưới phải uv(48,16)

                // Face 1: mặt trên - dòng thứ 1 vùng trên phải của skin
                0.625f, 0.125f,   // trái sau uv(40,8)
                0.625f, 0.0f,     // trái trước uv(40,0)
                0.75f, 0.0f,      // phải trước uv(48,0)
                0.75f, 0.125f,    // phải sau uv(48,8)

                // Face 2: mặt dưới - vùng phải-giữa phía trên skin
                0.75f, 0.125f,    // phải sau
                0.75f, 0.0f,      // phải trước
                0.875f, 0.0f,     // trái trước
                0.875f, 0.125f,   // trái sau

                // Face 3: mặt phải - vùng ngoài cùng bên phải phía trên skin
                0.75f, 0.25f,     // dưới sau
                0.75f, 0.125f,    // trên sau
                0.875f, 0.125f,   // trên trước
                0.875f, 0.25f,    // dưới trước

                // Face 4: mặt trái - vùng phải phía trên skin
                0.5f, 0.25f,      // dưới trước
                0.5f, 0.125f,     // trên trước
                0.625f, 0.125f,   // trên sau
                0.625f, 0.25f,    // dưới sau

                // Face 5: mặt sau - cạnh ngoài cùng bên phải phía trên skin
                0.875f, 0.25f,    // dưới trái
                0.875f, 0.125f,   // trên trái
                1.0f, 0.125f,     // trên phải
                1.0f, 0.25f       // dưới phải
        };
        addTextures(this.hatTexCoordinates);
    }
}
