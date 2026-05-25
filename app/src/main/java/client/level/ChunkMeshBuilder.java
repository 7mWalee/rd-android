package client.level;


import java.nio.FloatBuffer;

public final class ChunkMeshBuilder implements VertexSink {

    public static final int FLOATS_PER_VERTEX = 11;
    public static final int BYTES_PER_VERTEX = FLOATS_PER_VERTEX * 4;
    public static final int STRIDE_BYTES = BYTES_PER_VERTEX;

    // Byte offsets within a vertex.
    public static final int POS_OFFSET = 0;
    public static final int NORMAL_OFFSET = 12;
    public static final int COLOR_OFFSET = 24;
    public static final int UV_OFFSET = 36;

    private FloatBuffer buf;
    private int vertexCount;

    private float nx, ny, nz;
    private float r, g, b;
    private float u, v;

    public ChunkMeshBuilder(int initialVertexCapacity) {
        int cap = Math.max(64, initialVertexCapacity);
        this.buf = java.nio.ByteBuffer.allocateDirect(cap * FLOATS_PER_VERTEX * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
        this.vertexCount = 0;
        this.r = this.g = this.b = 1f;
        this.nx = 0; this.ny = 1; this.nz = 0;
        this.u = this.v = 0;
    }

    public void reset() {
        buf.clear();
        vertexCount = 0;
        r = g = b = 1f;
        nx = 0; ny = 1; nz = 0;
        u = v = 0;
    }

    private void ensureRoom() {
        if (buf.remaining() >= FLOATS_PER_VERTEX) return;
        int newCap = buf.capacity() * 2;
        FloatBuffer bigger = java.nio.ByteBuffer.allocateDirect(newCap * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
        buf.flip();
        bigger.put(buf);
        buf = bigger;
    }

    @Override public void normal(float nx, float ny, float nz)  { this.nx = nx; this.ny = ny; this.nz = nz; }
    @Override public void color(float r, float g, float b) { this.r = r; this.g = g; this.b = b; }
    @Override public void texture(float u, float v) { this.u = u; this.v = v; }

    @Override public void vertex(float x, float y, float z) {
        ensureRoom();
        buf.put(x).put(y).put(z);
        buf.put(nx).put(ny).put(nz);
        buf.put(r).put(g).put(b);
        buf.put(u).put(v);
        vertexCount++;
    }

    public int vertexCount() { return vertexCount; }

    public FloatBuffer drain() {
        buf.flip();
        return buf;
    }
}