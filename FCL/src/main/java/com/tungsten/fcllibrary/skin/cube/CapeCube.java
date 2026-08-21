package com.tungsten.fcllibrary.skin.cube;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

/**
 * Lớp khối lập phương áo choàng - dùng để vẽ áo choàng người chơi
 * Áo choàng là 1 mặt phẳng chữ nhật mỏng, rủ xuống từ lưng người chơi
 */
public class CapeCube {

    protected float[] scale;           // Tham số tỉ lệ [x, y, z]
    protected float[] offset;          // Độ lệch vị trí [x, y, z]
    protected float[] faceVertices;    // Mảng tọa độ đỉnh
    protected float[] normalVertices;  // Mảng vector pháp tuyến
    protected FloatBuffer vertexBuffer;           // Buffer đỉnh
    protected FloatBuffer normalVertexBuffer;     // Buffer vector pháp tuyến
    protected ArrayList<FloatBuffer> textureBuffers; // Danh sách buffer texture

    /**
     * Khởi tạo khối lập phương áo choàng
     * 
     * @param sizeX Chiều rộng áo choàng (kích thước trục X)
     * @param sizeY Chiều cao áo choàng (kích thước trục Y, chiều dọc)
     * @param sizeZ Độ dày áo choàng (kích thước trục Z, thường rất nhỏ)
     * @param offsetX Độ lệch trục X
     * @param offsetY Độ lệch trục Y
     * @param offsetZ Độ lệch trục Z
     */
    public CapeCube(float sizeX, float sizeY, float sizeZ, float offsetX, float offsetY, float offsetZ) {
        this.scale = new float[3];
        this.scale[0] = sizeX;
        this.scale[1] = sizeY;
        this.scale[2] = sizeZ;
        this.offset = new float[3];
        this.offset[0] = offsetX;
        this.offset[1] = offsetY;
        this.offset[2] = offsetZ;
        
        // Mảng tọa độ đỉnh GL (hệ tọa độ thuận tay phải)
        // Mỗi mặt 4 đỉnh, mỗi đỉnh gồm 3 thành phần x, y, z
        // Thứ tự mặt: trước (Face 0), trên (Face 1), dưới (Face 2), phải (Face 3), trái (Face 4), sau (Face 5)
        this.faceVertices = new float[] {
                // Face 0: mặt trước (Z=+1)
                -1.0f, -1.0f, 1.0f,
                -1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                1.0f, -1.0f, 1.0f,

                // Face 1: mặt trên (Y=+1)
                -1.0f, 1.0f, 1.0f,
                -1.0f, 1.0f, -1.0f,
                1.0f, 1.0f, -1.0f,
                1.0f, 1.0f, 1.0f,

                // Face 2: mặt dưới (Y=-1)
                1.0f, -1.0f, 1.0f,
                1.0f, -1.0f, -1.0f,
                -1.0f, -1.0f, -1.0f,
                -1.0f, -1.0f, 1.0f,

                // Face 3: mặt phải (X=+1)
                1.0f, -1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, -1.0f,
                1.0f, -1.0f, -1.0f,

                // Face 4: mặt trái (X=-1)
                -1.0f, -1.0f, -1.0f,
                -1.0f, 1.0f, -1.0f,
                -1.0f, 1.0f, 1.0f,
                -1.0f, -1.0f, 1.0f,

                // Face 5: mặt sau (Z=-1)
                1.0f, -1.0f, -1.0f,
                1.0f, 1.0f, -1.0f,
                -1.0f, 1.0f, -1.0f,
                -1.0f, -1.0f, -1.0f
        };
        
        // Mảng vector pháp tuyến - 4 đỉnh mỗi mặt dùng chung 1 hướng pháp tuyến
        // Vector pháp tuyến hướng ra ngoài mặt, dùng để tính ánh sáng
        this.normalVertices = new float[] {
                // Face 0: mặt trước - pháp tuyến hướng +Z
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,

                // Face 1: mặt trên - pháp tuyến hướng +Y
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,

                // Face 2: mặt dưới - pháp tuyến hướng -Y
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,

                // Face 3: mặt phải - pháp tuyến hướng +X
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,

                // Face 4: mặt trái - pháp tuyến hướng -X
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,

                // Face 5: mặt sau - pháp tuyến hướng -Z
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f
        };
        
        // Điều chỉnh tọa độ đỉnh theo tham số tỉ lệ
        for (int i = 0; i < 24; ++i) {
            this.faceVertices[i * 3    ] = this.faceVertices[i * 3    ] * this.scale[0] / 2.0f;
            this.faceVertices[i * 3 + 1] = this.faceVertices[i * 3 + 1] * this.scale[1] / 2.0f;
            this.faceVertices[i * 3 + 2] = this.faceVertices[i * 3 + 2] * this.scale[2] / 2.0f;
        }
        
        // Tạo buffer đỉnh
        final ByteBuffer allocateDirectFace = ByteBuffer.allocateDirect(this.faceVertices.length * 4);
        allocateDirectFace.order(ByteOrder.nativeOrder());
        this.vertexBuffer = allocateDirectFace.asFloatBuffer();
        this.vertexBuffer.put(this.faceVertices);
        this.vertexBuffer.position(0);
        
        // Tạo buffer vector pháp tuyến
        final ByteBuffer allocateDirectNormal = ByteBuffer.allocateDirect(this.normalVertices.length * 4);
        allocateDirectNormal.order(ByteOrder.nativeOrder());
        this.normalVertexBuffer = allocateDirectNormal.asFloatBuffer();
        this.normalVertexBuffer.put(this.normalVertices);
        this.normalVertexBuffer.position(0);
        
        this.textureBuffers = new ArrayList<>();
    }

    /**
     * Thêm buffer tọa độ texture
     * @param texture Mảng tọa độ texture
     * @return Buffer texture vừa tạo
     */
    public FloatBuffer addTextures(final float[] texture) {
        final ByteBuffer allocateDirect = ByteBuffer.allocateDirect(texture.length * 4);
        allocateDirect.order(ByteOrder.nativeOrder());
        final FloatBuffer floatBuffer = allocateDirect.asFloatBuffer();
        floatBuffer.put(texture);
        floatBuffer.position(0);
        this.textureBuffers.add(floatBuffer);
        return floatBuffer;
    }

    /**
     * Xóa hết buffer texture
     */
    public void clearAllTextures() {
        this.textureBuffers.clear();
    }

    /**
     * Vẽ áo choàng
     * @param gl10 Ngữ cảnh OpenGL ES 1.0
     */
    public void draw(final GL10 gl10) {
        // Bật chế độ blend (dùng cho hiệu ứng trong suốt)
        gl10.glEnable(GL10.GL_BLEND);
        gl10.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
        
        // Bật mảng đỉnh, mảng pháp tuyến và mảng tọa độ texture
        gl10.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl10.glEnableClientState(GL10.GL_NORMAL_ARRAY);
        gl10.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        
        // Thiết lập con trỏ đỉnh và con trỏ pháp tuyến
        gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, this.vertexBuffer);
        gl10.glNormalPointer(GL10.GL_FLOAT, 0, this.normalVertexBuffer);
        
        // Lưu ma trận hiện tại rồi áp dụng biến đổi
        gl10.glPushMatrix();
        gl10.glTranslatef(this.offset[0], this.offset[1], this.offset[2]);
        
        // Hiệu ứng nghiêng áo choàng: xoay 12 độ quanh trục X
        gl10.glTranslatef(0.0f, this.scale[1] / 4.0f * 3.0f, 0.0f);
        gl10.glRotatef(12f, 1f, 0f, 0f);
        gl10.glTranslatef(0.0f, -this.scale[1] / 4.0f * 3.0f, 0.0f);
        
        // Vẽ tất cả lớp texture
        for (int i = 0; i < this.textureBuffers.size(); ++i) {
            gl10.glTexCoordPointer(2, GL10.GL_FLOAT, 0, this.textureBuffers.get(i));
            for (int j = 0; j < 6; ++j) {
                gl10.glDrawArrays(GL10.GL_TRIANGLE_FAN, j * 4, 4);
            }
        }
        
        // Khôi phục ma trận và tắt các trạng thái liên quan
        gl10.glPopMatrix();
        gl10.glDisable(GL10.GL_BLEND);
        gl10.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl10.glDisableClientState(GL10.GL_NORMAL_ARRAY);
        gl10.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
