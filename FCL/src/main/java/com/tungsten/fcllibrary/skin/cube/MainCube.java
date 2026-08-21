package com.tungsten.fcllibrary.skin.cube;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

public class MainCube {

    protected float[] offset;
    protected float[] faceVertices;
    protected float[] normalVertices;
    protected FloatBuffer vertexBuffer;
    protected FloatBuffer normalVertexBuffer;
    protected ArrayList<FloatBuffer> textureBuffers;

    /**
     * @param sizeX   Kích thước khối lập phương theo trục X
     * @param sizeY   Kích thước khối lập phương theo trục Y
     * @param sizeZ   Kích thước khối lập phương theo trục Z
     * @param offsetX Độ lệch trục X của khối trong khung cảnh
     * @param offsetY Độ lệch trục Y của khối trong khung cảnh
     * @param offsetZ Độ lệch trục Z của khối trong khung cảnh
     */
    public MainCube(float sizeX, float sizeY, float sizeZ, float offsetX, float offsetY, float offsetZ) {
        this.offset = new float[3];
        this.offset[0] = offsetX;
        this.offset[1] = offsetY;
        this.offset[2] = offsetZ;

        // Mảng tọa độ đỉnh GL (hệ tọa độ thuận tay phải)
        // Mỗi mặt 4 đỉnh, mỗi đỉnh gồm 3 thành phần x, y, z
        // Thứ tự mặt: trước (Face 0), trên (Face 1), dưới (Face 2), phải (Face 3), trái (Face 4), sau (Face 5)
        // Thứ tự đỉnh: theo chiều kim đồng hồ (nhìn từ phía ngoài mặt)
        this.faceVertices = new float[]{
                // Face 0: mặt trước (Z=+1) - nhìn từ phía trước, theo chiều kim đồng hồ
                -1.0f, -1.0f, 1.0f,  // dưới trái
                -1.0f, 1.0f, 1.0f,   // trên trái
                1.0f, 1.0f, 1.0f,    // trên phải
                1.0f, -1.0f, 1.0f,   // dưới phải

                // Face 1: mặt trên (Y=+1) - nhìn từ phía trên, theo chiều kim đồng hồ
                -1.0f, 1.0f, 1.0f,   // sau trái
                -1.0f, 1.0f, -1.0f,  // trước trái
                1.0f, 1.0f, -1.0f,   // trước phải
                1.0f, 1.0f, 1.0f,    // sau phải

                // Face 2: mặt dưới (Y=-1) - nhìn từ phía dưới, theo chiều kim đồng hồ
                1.0f, -1.0f, 1.0f,   // sau phải
                1.0f, -1.0f, -1.0f,  // trước phải
                -1.0f, -1.0f, -1.0f, // trước trái
                -1.0f, -1.0f, 1.0f,  // sau trái

                // Face 3: mặt phải (X=+1) - nhìn từ phía phải, theo chiều kim đồng hồ
                1.0f, -1.0f, 1.0f,   // sau dưới
                1.0f, 1.0f, 1.0f,    // sau trên
                1.0f, 1.0f, -1.0f,   // trước trên
                1.0f, -1.0f, -1.0f,  // trước dưới

                // Face 4: mặt trái (X=-1) - nhìn từ phía trái, theo chiều kim đồng hồ
                -1.0f, -1.0f, -1.0f, // trước dưới
                -1.0f, 1.0f, -1.0f,  // trước trên
                -1.0f, 1.0f, 1.0f,   // sau trên
                -1.0f, -1.0f, 1.0f,  // sau dưới

                // Face 5: mặt sau (Z=-1) - nhìn từ phía sau, theo chiều kim đồng hồ
                1.0f, -1.0f, -1.0f,  // dưới trái
                1.0f, 1.0f, -1.0f,   // trên trái
                -1.0f, 1.0f, -1.0f,  // trên phải
                -1.0f, -1.0f, -1.0f  // dưới phải
        };

        // Mảng vector pháp tuyến - mỗi đỉnh ứng với 1 vector pháp tuyến, dùng để tính ánh sáng
        // Hướng vector pháp tuyến: chỉ ra phía ngoài mặt, vuông góc với mặt
        this.normalVertices = new float[]{
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
        // Nhân tọa độ đỉnh đã chuẩn hóa lên kích thước thật
        for (int i = 0; i < 24; ++i) {
            this.faceVertices[i * 3] = this.faceVertices[i * 3] * sizeX / 2.0f;
            this.faceVertices[i * 3 + 1] = this.faceVertices[i * 3 + 1] * sizeY / 2.0f;
            this.faceVertices[i * 3 + 2] = this.faceVertices[i * 3 + 2] * sizeZ / 2.0f;
        }
        final ByteBuffer allocateDirectFace = ByteBuffer.allocateDirect(this.faceVertices.length * 4);
        allocateDirectFace.order(ByteOrder.nativeOrder());
        this.vertexBuffer = allocateDirectFace.asFloatBuffer();
        this.vertexBuffer.put(this.faceVertices);
        this.vertexBuffer.position(0);
        final ByteBuffer allocateDirectNormal = ByteBuffer.allocateDirect(this.normalVertices.length * 4);
        allocateDirectNormal.order(ByteOrder.nativeOrder());
        this.normalVertexBuffer = allocateDirectNormal.asFloatBuffer();
        this.normalVertexBuffer.put(this.normalVertices);
        this.normalVertexBuffer.position(0);
        this.textureBuffers = new ArrayList<>();
    }

    public FloatBuffer addTextures(float[] texture) {
        final ByteBuffer allocateDirect = ByteBuffer.allocateDirect(texture.length * 4);
        allocateDirect.order(ByteOrder.nativeOrder());
        final FloatBuffer floatBuffer = allocateDirect.asFloatBuffer();
        floatBuffer.put(texture);
        floatBuffer.position(0);
        this.textureBuffers.add(floatBuffer);
        return floatBuffer;
    }

    public void clearAllTextures() {
        this.textureBuffers.clear();
    }

    public void draw(GL10 gl10) {
        gl10.glEnable(GL10.GL_BLEND);
        gl10.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl10.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl10.glEnableClientState(GL10.GL_NORMAL_ARRAY);
        gl10.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, this.vertexBuffer);
        gl10.glNormalPointer(GL10.GL_FLOAT, 0, this.normalVertexBuffer);
        gl10.glPushMatrix();
        gl10.glTranslatef(this.offset[0], this.offset[1], this.offset[2]);
        for (int i = 0; i < this.textureBuffers.size(); ++i) {
            gl10.glTexCoordPointer(2, GL10.GL_FLOAT, 0, this.textureBuffers.get(i));
            for (int j = 0; j < 6; ++j) {
                gl10.glDrawArrays(6, j * 4, 4);
            }
        }
        gl10.glPopMatrix();
        gl10.glDisable(GL10.GL_BLEND);
        gl10.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl10.glDisableClientState(GL10.GL_NORMAL_ARRAY);
        gl10.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
