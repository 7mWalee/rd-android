package client.level;

import client.gfx.GL;


import java.nio.FloatBuffer;

public class Tessellator implements VertexSink {
    private static final int MAX_VERTICES = 100000;
    private final FloatBuffer vertexBuffer = java.nio.ByteBuffer.allocateDirect(MAX_VERTICES * 3 * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
    private final FloatBuffer textureCoordinateBuffer = java.nio.ByteBuffer.allocateDirect(MAX_VERTICES * 2 * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
    private final FloatBuffer colorBuffer = java.nio.ByteBuffer.allocateDirect(MAX_VERTICES * 3 * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
    private final FloatBuffer normalBuffer = java.nio.ByteBuffer.allocateDirect(MAX_VERTICES * 3 * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
    private int vertices = 0;
    private boolean hasTexture = false;
    private float textureU, textureV;
    private boolean hasColor = false;
    private float red, green, blue;
    private boolean hasNormal = false;
    private float normalX, normalY, normalZ;

    public void init() {
        clear();
    }

    @Override
    public void vertex(float x, float y, float z) {
        vertexBuffer.put(vertices * 3,     x);
        vertexBuffer.put(vertices * 3 + 1, y);
        vertexBuffer.put(vertices * 3 + 2, z);
        if (hasTexture) {
            textureCoordinateBuffer.put(vertices * 2,     textureU);
            textureCoordinateBuffer.put(vertices * 2 + 1, textureV);
        }
        if (hasColor) {
            colorBuffer.put(vertices * 3,     red);
            colorBuffer.put(vertices * 3 + 1, green);
            colorBuffer.put(vertices * 3 + 2, blue);
        }
        if (hasNormal) {
            normalBuffer.put(vertices * 3,     normalX);
            normalBuffer.put(vertices * 3 + 1, normalY);
            normalBuffer.put(vertices * 3 + 2, normalZ);
        }
        vertices++;
        if (vertices == MAX_VERTICES) flush();
    }

    @Override
    public void texture(float u, float v) {
        hasTexture = true;
        textureU = u;
        textureV = v;
    }

    @Override
    public void color(float r, float g, float b) {
        hasColor = true;
        red = r;
        green = g;
        blue = b;
    }

    @Override
    public void normal(float nx, float ny, float nz) {
        hasNormal = true;
        normalX = nx;
        normalY = ny;
        normalZ = nz;
    }

    public void flush() {
        vertexBuffer.flip();
        textureCoordinateBuffer.flip();
        colorBuffer.flip();
        normalBuffer.flip();
        GL.vertexPointer(3, 0, vertexBuffer);
        if (hasTexture) GL.texCoordPointer(2, 0, textureCoordinateBuffer);
        if (hasColor)   GL.colorPointer(3, 0, colorBuffer);
        if (hasNormal)  GL.normalPointer(0, normalBuffer);
        GL.enableClientState(GL.VERTEX_ARRAY);
        if (hasTexture) GL.enableClientState(GL.TEXTURE_COORD_ARRAY);
        if (hasColor)   GL.enableClientState(GL.COLOR_ARRAY);
        if (hasNormal)  GL.enableClientState(GL.NORMAL_ARRAY);
        GL.drawArrays(GL.QUADS, 0, vertices);
        GL.disableClientState(GL.VERTEX_ARRAY);
        if (hasTexture) GL.disableClientState(GL.TEXTURE_COORD_ARRAY);
        if (hasColor)   GL.disableClientState(GL.COLOR_ARRAY);
        if (hasNormal)  GL.disableClientState(GL.NORMAL_ARRAY);
        clear();
    }

    private void clear() {
        vertexBuffer.clear();
        textureCoordinateBuffer.clear();
        colorBuffer.clear();
        normalBuffer.clear();
        vertices = 0;
        hasTexture = false;
        hasColor = false;
        hasNormal = false;
    }
}