package client.level;

import client.phys.AABB;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * View-frustum culler.
 *
 * On Android / OpenGL ES there is no glGetFloat(GL_PROJECTION_MATRIX) —
 * the matrices are write-only from Java.  Instead the GameRenderer keeps
 * the current combined (projection × modelview) clip matrix up-to-date
 * and hands it to us via {@link #setClipMatrix(float[])}.
 */
public class Frustum {

    public static final int RIGHT  = 0;
    public static final int LEFT   = 1;
    public static final int BOTTOM = 2;
    public static final int TOP    = 3;
    public static final int BACK   = 4;
    public static final int FRONT  = 5;

    public static final int A = 0;
    public static final int B = 1;
    public static final int C = 2;
    public static final int D = 3;

    private static final Frustum frustum = new Frustum();

    float[][] m_Frustum = new float[6][4];

    // Current clip matrix supplied by the renderer
    private static float[] sClip = new float[16];

    /** Called by the renderer each frame after setting up projection + modelview. */
    public static void setClipMatrix(float[] clip) {
        System.arraycopy(clip, 0, sClip, 0, 16);
    }

    public static Frustum getFrustum() {
        frustum.calculateFrustum();
        return frustum;
    }

    public void normalizePlane(float[][] f, int side) {
        float mag = (float) Math.sqrt(
                f[side][A] * f[side][A] +
                f[side][B] * f[side][B] +
                f[side][C] * f[side][C]);
        f[side][A] /= mag;
        f[side][B] /= mag;
        f[side][C] /= mag;
        f[side][D] /= mag;
    }

    public void calculateFrustum() {
        float[] c = sClip;

        m_Frustum[RIGHT][A]  = c[3]  - c[0];
        m_Frustum[RIGHT][B]  = c[7]  - c[4];
        m_Frustum[RIGHT][C]  = c[11] - c[8];
        m_Frustum[RIGHT][D]  = c[15] - c[12];
        normalizePlane(m_Frustum, RIGHT);

        m_Frustum[LEFT][A]   = c[3]  + c[0];
        m_Frustum[LEFT][B]   = c[7]  + c[4];
        m_Frustum[LEFT][C]   = c[11] + c[8];
        m_Frustum[LEFT][D]   = c[15] + c[12];
        normalizePlane(m_Frustum, LEFT);

        m_Frustum[BOTTOM][A] = c[3]  + c[1];
        m_Frustum[BOTTOM][B] = c[7]  + c[5];
        m_Frustum[BOTTOM][C] = c[11] + c[9];
        m_Frustum[BOTTOM][D] = c[15] + c[13];
        normalizePlane(m_Frustum, BOTTOM);

        m_Frustum[TOP][A]    = c[3]  - c[1];
        m_Frustum[TOP][B]    = c[7]  - c[5];
        m_Frustum[TOP][C]    = c[11] - c[9];
        m_Frustum[TOP][D]    = c[15] - c[13];
        normalizePlane(m_Frustum, TOP);

        m_Frustum[BACK][A]   = c[3]  - c[2];
        m_Frustum[BACK][B]   = c[7]  - c[6];
        m_Frustum[BACK][C]   = c[11] - c[10];
        m_Frustum[BACK][D]   = c[15] - c[14];
        normalizePlane(m_Frustum, BACK);

        m_Frustum[FRONT][A]  = c[3]  + c[2];
        m_Frustum[FRONT][B]  = c[7]  + c[6];
        m_Frustum[FRONT][C]  = c[11] + c[10];
        m_Frustum[FRONT][D]  = c[15] + c[14];
        normalizePlane(m_Frustum, FRONT);
    }

    public boolean pointInFrustum(float x, float y, float z) {
        for (int i = 0; i < 6; i++) {
            if (m_Frustum[i][A]*x + m_Frustum[i][B]*y + m_Frustum[i][C]*z + m_Frustum[i][D] <= 0)
                return false;
        }
        return true;
    }

    public boolean sphereInFrustum(float x, float y, float z, float radius) {
        for (int i = 0; i < 6; i++) {
            if (m_Frustum[i][A]*x + m_Frustum[i][B]*y + m_Frustum[i][C]*z + m_Frustum[i][D] <= -radius)
                return false;
        }
        return true;
    }

    public boolean cubeInFrustum(float minX, float minY, float minZ,
                                  float maxX, float maxY, float maxZ) {
        for (int i = 0; i < 6; i++) {
            if (m_Frustum[i][A]*minX + m_Frustum[i][B]*minY + m_Frustum[i][C]*minZ + m_Frustum[i][D] > 0) continue;
            if (m_Frustum[i][A]*maxX + m_Frustum[i][B]*minY + m_Frustum[i][C]*minZ + m_Frustum[i][D] > 0) continue;
            if (m_Frustum[i][A]*minX + m_Frustum[i][B]*maxY + m_Frustum[i][C]*minZ + m_Frustum[i][D] > 0) continue;
            if (m_Frustum[i][A]*maxX + m_Frustum[i][B]*maxY + m_Frustum[i][C]*minZ + m_Frustum[i][D] > 0) continue;
            if (m_Frustum[i][A]*minX + m_Frustum[i][B]*minY + m_Frustum[i][C]*maxZ + m_Frustum[i][D] > 0) continue;
            if (m_Frustum[i][A]*maxX + m_Frustum[i][B]*minY + m_Frustum[i][C]*maxZ + m_Frustum[i][D] > 0) continue;
            if (m_Frustum[i][A]*minX + m_Frustum[i][B]*maxY + m_Frustum[i][C]*maxZ + m_Frustum[i][D] > 0) continue;
            if (m_Frustum[i][A]*maxX + m_Frustum[i][B]*maxY + m_Frustum[i][C]*maxZ + m_Frustum[i][D] > 0) continue;
            return false;
        }
        return true;
    }

    public boolean cubeInFrustum(AABB aabb) {
        return cubeInFrustum((float)aabb.minX, (float)aabb.minY, (float)aabb.minZ,
                             (float)aabb.maxX, (float)aabb.maxY, (float)aabb.maxZ);
    }
}
