package com.tungsten.fcllibrary.skin.cube.body;

import com.tungsten.fcllibrary.skin.cube.MainCube;

/**
 * Khối lập phương thân - kế thừa từ MainCube
 * Kích thước: 8x12x4 (kích thước thân chuẩn Minecraft)
 * Vị trí: lệch trục Y 2 (phía trên chân)
 */
public class Body extends MainCube {

    protected float[] bodyTexCoordinates;

    public Body(float scale) {
        super(8.0f * scale, 12.0f * scale, 4.0f * scale, 0.0f * scale, 2.0f * scale, 0.0f * scale);

        // Mảng tọa độ texture (tọa độ UV, chuẩn hóa 0-1)
        // Vùng thân: khối 8x12 pixel giữa phía trên skin (ứng UV ngang 0.25-0.4375, dọc 0.25-0.5)
        // Thứ tự mặt: trước (Face 0), trên (Face 1), dưới (Face 2), phải (Face 3), trái (Face 4), sau (Face 5)
        this.bodyTexCoordinates = new float[]{
                // Face 0: mặt trước - vùng giữa phía trên skin
                0.3125f, 0.5f,      // dưới trái uv(20,32)
                0.3125f, 0.3125f,   // trên trái uv(20,20)
                0.4375f, 0.3125f,   // trên phải uv(28,20)
                0.4375f, 0.5f,      // dưới phải uv(28,32)

                // Face 1: mặt trên - vùng phía trên skin
                0.3125f, 0.3125f,   // trái sau uv(20,20)
                0.3125f, 0.25f,     // trái trước uv(20,16)
                0.4375f, 0.25f,     // phải trước uv(28,16)
                0.4375f, 0.3125f,   // phải sau uv(28,20)

                // Face 2: mặt dưới - vùng giữa-phải phía trên skin
                0.4375f, 0.3125f,   // phải sau
                0.4375f, 0.25f,     // phải trước
                0.5625f, 0.25f,     // trái trước
                0.5625f, 0.3125f,   // trái sau

                // Face 3: mặt phải - vùng bên phải skin
                0.4375f, 0.5f,      // dưới sau
                0.4375f, 0.3125f,   // trên sau
                0.5f, 0.3125f,      // trên trước
                0.5f, 0.5f,         // dưới trước

                // Face 4: mặt trái - vùng bên trái skin
                0.25f, 0.5f,        // dưới trước
                0.25f, 0.3125f,     // trên trước
                0.3125f, 0.3125f,   // trên sau
                0.3125f, 0.5f,      // dưới sau

                // Face 5: mặt sau - vùng giữa-phải skin
                0.5f, 0.5f,         // dưới trái
                0.5f, 0.3125f,      // trên trái
                0.625f, 0.3125f,    // trên phải
                0.625f, 0.5f        // dưới phải
        };
        addTextures(this.bodyTexCoordinates);
    }
}
