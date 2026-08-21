package com.tungsten.fcllibrary.skin.cube.head;

import com.tungsten.fcllibrary.skin.cube.MainCube;

/**
 * Khối lập phương đầu - kế thừa từ MainCube
 * Kích thước: 8x8x8 (kích thước đầu chuẩn Minecraft)
 * Vị trí: lệch trục Y 12 (phía trên thân)
 */
public class Head extends MainCube {

    protected float[] headTexCoordinates;
    
    public Head(float scale) {
        super(8.0f * scale, 8.0f * scale, 8.0f * scale, 0.0f * scale, 12.0f * scale, 0.0f * scale);
        
        // Mảng tọa độ texture (tọa độ UV, chuẩn hóa 0-1)
        // Định dạng texture skin Minecraft: 64x64 pixel
        // Vùng đầu: khối 8x8 pixel góc trên trái (ứng phạm vi UV 0.125-0.25)
        // Mỗi mặt 4 đỉnh, mỗi đỉnh 2 tọa độ (u, v), thứ tự đỉnh khớp với faceVertices
        // Thứ tự mặt: trước (Face 0), trên (Face 1), dưới (Face 2), phải (Face 3), trái (Face 4), sau (Face 5)
        this.headTexCoordinates = new float[] {
                // Face 0: mặt trước - dòng thứ 2 góc trên trái skin (8-16px)
                0.125f, 0.25f,    // dưới trái uv(8,16)
                0.125f, 0.125f,   // trên trái uv(8,8)
                0.25f, 0.125f,    // trên phải uv(16,8)
                0.25f, 0.25f,     // dưới phải uv(16,16)

                // Face 1: mặt trên - dòng thứ 1 góc trên trái skin (0-8px)
                0.125f, 0.125f,   // trái sau uv(8,8)
                0.125f, 0.0f,     // trái trước uv(8,0)
                0.25f, 0.0f,      // phải trước uv(16,0)
                0.25f, 0.125f,    // phải sau uv(16,8)

                // Face 2: mặt dưới - vùng giữa phía trên skin
                0.25f, 0.125f,    // phải sau
                0.25f, 0.0f,      // phải trước
                0.375f, 0.0f,     // trái trước
                0.375f, 0.125f,   // trái sau

                // Face 3: mặt phải - vùng phải phía trên skin
                0.25f, 0.25f,     // dưới sau
                0.25f, 0.125f,    // trên sau
                0.375f, 0.125f,   // trên trước
                0.375f, 0.25f,    // dưới trước

                // Face 4: mặt trái - vùng trái phía trên skin
                0.0f, 0.25f,      // dưới trước
                0.0f, 0.125f,     // trên trước
                0.125f, 0.125f,   // trên sau
                0.125f, 0.25f,    // dưới sau

                // Face 5: mặt sau - vùng giữa-phải phía trên skin
                0.375f, 0.25f,    // dưới trái
                0.375f, 0.125f,   // trên trái
                0.5f, 0.125f,     // trên phải
                0.5f, 0.25f       // dưới phải
        };
        addTextures(this.headTexCoordinates);
    }
}
