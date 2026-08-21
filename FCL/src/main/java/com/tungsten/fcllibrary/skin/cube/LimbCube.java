package com.tungsten.fcllibrary.skin.cube;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

public class LimbCube {

    protected float[] scale;
    protected float[] offset;
    protected int[] faceIndecies;
    protected float[] faceVertices;
    protected float[] normalVertices;
    protected float[] vertices;
    protected float[] mainAngleAxis;
    protected float mainAngle;
    protected float mainMaxAngle;
    protected float mainMinAngle;
    protected float mainStepValue;
    protected float[] subAngleAxis;
    protected float subAngle;
    protected float subMaxAngle;
    protected float subMinAngle;
    protected float subStepValue;
    protected FloatBuffer vertexBuffer;
    protected FloatBuffer normalVertexBuffer;
    protected ArrayList<FloatBuffer> textureBuffers;

    /**
     * Khối lập phương chi thể có thể xoay - dùng cho tay, chân và các bộ phận
     * cần hoạt ảnh khác.
     *
     * @param scaleX        Kích thước tỉ lệ theo trục X
     * @param scaleY        Kích thước tỉ lệ theo trục Y (hướng chiều dài chi thể)
     * @param scaleZ        Kích thước tỉ lệ theo trục Z
     * @param offsetX       Độ lệch trục X trong khung cảnh
     * @param offsetY       Độ lệch trục Y trong khung cảnh
     * @param offsetZ       Độ lệch trục Z trong khung cảnh
     * @param mainAxisX     Thành phần X của trục xoay chính
     * @param mainAxisY     Thành phần Y của trục xoay chính
     * @param mainAxisZ     Thành phần Z của trục xoay chính
     * @param mainStepValue Bước tăng góc chính (mỗi khung hình)
     * @param mainMaxAngle  Góc chính tối đa
     * @param mainMinAngle  Góc chính tối thiểu
     * @param subAxisX      Thành phần X của trục xoay phụ
     * @param subAxisY      Thành phần Y của trục xoay phụ
     * @param subAxisZ      Thành phần Z của trục xoay phụ
     * @param subStepValue  Bước tăng góc phụ
     * @param subMaxAngle   Góc phụ tối đa
     * @param subMinAngle   Góc phụ tối thiểu
     */
    public LimbCube(float scaleX, float scaleY, float scaleZ, float offsetX, float offsetY, float offsetZ,
                    float mainAxisX, float mainAxisY, float mainAxisZ,
                    float mainStepValue, float mainMaxAngle, float mainMinAngle,
                    float subAxisX, float subAxisY, float subAxisZ,
                    float subStepValue, float subMaxAngle, float subMinAngle) {
        this.scale = new float[3];
        this.scale[0] = scaleX;
        this.scale[1] = scaleY;
        this.scale[2] = scaleZ;
        this.offset = new float[3];
        this.offset[0] = offsetX;
        this.offset[1] = offsetY;
        this.offset[2] = offsetZ;

        // Trục xoay chính - dùng để chi thể vung (VD tay vung trước sau)
        this.mainAngleAxis = new float[3];
        this.mainAngleAxis[0] = mainAxisX;
        this.mainAngleAxis[1] = mainAxisY;
        this.mainAngleAxis[2] = mainAxisZ;
        this.mainAngle = 0.0f;
        this.mainStepValue = mainStepValue;
        this.mainMaxAngle = mainMaxAngle;
        this.mainMinAngle = mainMinAngle;

        // Trục xoay phụ - dùng cho chuyển động phụ (VD tay vung trong ngoài)
        this.subAngleAxis = new float[3];
        this.subAngleAxis[0] = subAxisX;
        this.subAngleAxis[1] = subAxisY;
        this.subAngleAxis[2] = subAxisZ;
        this.subAngle = 0.0f;
        this.subStepValue = subStepValue;
        this.subMaxAngle = subMaxAngle;
        this.subMinAngle = subMinAngle;

        // Tọa độ đỉnh cơ bản (đã chuẩn hóa) - 12 đỉnh
        // 4 đỉnh đầu: mặt trước (Z=+1)
        // 4 đỉnh giữa: mặt sau (Z=-1)
        // 4 đỉnh cuối: lớp giữa (Y=0) - điểm khớp nối với thân người
        this.vertices = new float[]{
                -1.0f, -1.0f, 1.0f,  // đỉnh 0: trước-dưới-trái
                1.0f, -1.0f, 1.0f,   // đỉnh 1: trước-dưới-phải
                1.0f, 1.0f, 1.0f,    // đỉnh 2: trước-trên-phải
                -1.0f, 1.0f, 1.0f,   // đỉnh 3: trước-trên-trái

                -1.0f, -1.0f, -1.0f, // đỉnh 4: sau-dưới-trái
                1.0f, -1.0f, -1.0f,  // đỉnh 5: sau-dưới-phải
                1.0f, 1.0f, -1.0f,   // đỉnh 6: sau-trên-phải
                -1.0f, 1.0f, -1.0f,  // đỉnh 7: sau-trên-trái

                -1.0f, 0.0f, 1.0f,   // đỉnh 8: giữa-trước-trái (điểm khớp)
                1.0f, 0.0f, 1.0f,    // đỉnh 9: giữa-trước-phải (điểm khớp)
                1.0f, 0.0f, -1.0f,   // đỉnh 10: giữa-sau-phải (điểm khớp)
                -1.0f, 0.0f, -1.0f   // đỉnh 11: giữa-sau-trái (điểm khớp)
        };

        // Mảng chỉ số mặt - dựng 10 mặt bằng cách tham chiếu vào mảng vertices
        // Mỗi mặt gồm 4 chỉ số đỉnh, xếp theo chiều kim đồng hồ
        this.faceIndecies = new int[]{
                0, 8, 9, 1,   // mặt 0: cạnh dưới-trước
                8, 3, 2, 9,   // mặt 1: cạnh trên-trước
                3, 7, 6, 2,   // mặt 2: cạnh trên-sau
                0, 4, 5, 1,   // mặt 3: cạnh dưới-sau
                1, 9, 10, 5,  // mặt 4: cạnh dưới-phải
                9, 2, 6, 10,  // mặt 5: cạnh trên-phải
                4, 11, 8, 0,  // mặt 6: cạnh dưới-trái
                11, 7, 3, 8,  // mặt 7: cạnh trên-trái
                5, 10, 11, 4, // mặt 8: mặt đầu sau (nối với thân)
                10, 6, 7, 11  // mặt 9: mặt đầu sau phần trên
        };

        this.faceVertices = new float[this.faceIndecies.length * 3];

        // Mảng vector pháp tuyến - mỗi mặt 4 vector, tổng 10 mặt
        // mặt 0-1: mặt trước (Z=+1) - pháp tuyến hướng +Z
        // mặt 2-3: mặt trên (Y=+1) - pháp tuyến hướng +Y
        // mặt 4-5: mặt phải (X=+1) - pháp tuyến hướng +X
        // mặt 6-7: mặt trái (X=-1) - pháp tuyến hướng -X
        // mặt 8-9: mặt sau (Z=-1) - pháp tuyến hướng -Z (mặt đầu nối thân)
        this.normalVertices = new float[]{
                // mặt 0: cạnh dưới-trước (Z=+1)
                0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
                // mặt 1: cạnh trên-trước (Z=+1)
                0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,

                // mặt 2: cạnh trên-sau (Y=+1)
                0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                // mặt 3: cạnh dưới-sau (Y=-1)
                0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,

                // mặt 4: cạnh dưới-phải (X=+1)
                1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                // mặt 5: cạnh trên-phải (X=+1)
                1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,

                // mặt 6: cạnh dưới-trái (X=-1)
                -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
                // mặt 7: cạnh trên-trái (X=-1)
                -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,

                // mặt 8: mặt đầu sau phần dưới (Z=-1) - nối với thân
                0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f,
                // mặt 9: mặt đầu sau phần trên (Z=-1) - nối với thân
                0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f
        };
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

    public FloatBuffer addTextures(final float[] texture) {
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

    public void draw(final GL10 gl10, final boolean isRunning) {
        gl10.glEnable(GL10.GL_BLEND);
        gl10.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl10.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl10.glEnableClientState(GL10.GL_NORMAL_ARRAY);
        gl10.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl10.glNormalPointer(GL10.GL_FLOAT, 0, this.normalVertexBuffer);
        for (int i = 0; i < this.faceIndecies.length; ++i) {
            final int n = this.faceIndecies[i];
            final float n2 = this.vertices[n * 3 + 1] * this.scale[1] / 2.0f;
            final float n3 = this.vertices[n * 3 + 2] * this.scale[2] / 2.0f;
            this.vertexBuffer.put(i * 3, this.vertices[n * 3] * this.scale[0] / 2.0f);
            this.vertexBuffer.put(i * 3 + 1, n2);
            this.vertexBuffer.put(i * 3 + 2, n3);
        }
        gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, this.vertexBuffer);
        gl10.glPushMatrix();
        gl10.glTranslatef(this.offset[0], this.offset[1], this.offset[2]);
        if (isRunning) {
            // Áp dụng xoay chính (tay vung trước sau)
            gl10.glTranslatef(0.0f, this.scale[1] / 4.0f * 3.0f, 0.0f);
            gl10.glRotatef(this.mainAngle, this.mainAngleAxis[0], this.mainAngleAxis[1], this.mainAngleAxis[2]);
            gl10.glTranslatef(0.0f, -this.scale[1] / 4.0f * 3.0f, 0.0f);

            // Cập nhật góc chính
            this.mainAngle += this.mainStepValue;
            if (this.mainAngle >= this.mainMaxAngle) {
                this.mainStepValue *= -1.0f;
                this.mainAngle = this.mainMaxAngle;
            } else if (this.mainAngle <= this.mainMinAngle) {
                this.mainStepValue *= -1.0f;
                this.mainAngle = this.mainMinAngle;
            }
        }
        for (int i = 0; i < this.textureBuffers.size(); ++i) {
            gl10.glTexCoordPointer(2, GL10.GL_FLOAT, 0, this.textureBuffers.get(i));
            for (int j = 0; j < 10; ++j) {
                gl10.glDrawArrays(GL10.GL_TRIANGLE_FAN, j * 4, 4);
            }
        }
        gl10.glPopMatrix();
        gl10.glDisable(GL10.GL_BLEND);
        gl10.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl10.glDisableClientState(GL10.GL_NORMAL_ARRAY);
        gl10.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
