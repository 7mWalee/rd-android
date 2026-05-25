package client.gfx;

import android.opengl.GLES11;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ByteOrder;

/**
 * Android OpenGL ES 1.1 wrapper that matches the desktop client.gfx.GL API exactly.
 */
public final class GL {
    private GL() {}

    // Primitives
    public static final int QUADS              = 0x0007; // emulated
    public static final int TRIANGLES          = GLES11.GL_TRIANGLES;
    public static final int TRIANGLE_FAN       = GLES11.GL_TRIANGLE_FAN;
    public static final int TRIANGLE_STRIP     = GLES11.GL_TRIANGLE_STRIP;
    public static final int LINES              = GLES11.GL_LINES;
    public static final int LINE_LOOP          = GLES11.GL_LINE_LOOP;
    public static final int LINE_STRIP         = GLES11.GL_LINE_STRIP;
    public static final int POINTS             = GLES11.GL_POINTS;

    // Data types
    public static final int FLOAT              = GLES11.GL_FLOAT;
    public static final int UNSIGNED_BYTE      = GLES11.GL_UNSIGNED_BYTE;
    public static final int UNSIGNED_INT       = 0x1405; // GL_UNSIGNED_INT (not in GLES11)

    // Capabilities
    public static final int TEXTURE_2D         = GLES11.GL_TEXTURE_2D;
    public static final int BLEND              = GLES11.GL_BLEND;
    public static final int DEPTH_TEST         = GLES11.GL_DEPTH_TEST;
    public static final int CULL_FACE          = GLES11.GL_CULL_FACE;
    public static final int LIGHTING           = GLES11.GL_LIGHTING;
    public static final int LIGHT0             = GLES11.GL_LIGHT0;
    public static final int COLOR_MATERIAL     = GLES11.GL_COLOR_MATERIAL;
    public static final int FOG                = GLES11.GL_FOG;
    public static final int ALPHA_TEST         = GLES11.GL_ALPHA_TEST;
    public static final int FRONT              = GLES11.GL_FRONT;
    public static final int AMBIENT_AND_DIFFUSE= GLES11.GL_AMBIENT_AND_DIFFUSE;
    public static final int LIGHT_MODEL_AMBIENT= GLES11.GL_LIGHT_MODEL_AMBIENT;

    // Blend factors
    public static final int SRC_ALPHA          = GLES11.GL_SRC_ALPHA;
    public static final int ONE_MINUS_SRC_ALPHA= GLES11.GL_ONE_MINUS_SRC_ALPHA;
    public static final int ONE                = GLES11.GL_ONE;
    public static final int ZERO               = GLES11.GL_ZERO;
    public static final int CURRENT_BIT        = 0x00000001;

    // Depth/shading
    public static final int LEQUAL             = GLES11.GL_LEQUAL;
    public static final int LESS               = GLES11.GL_LESS;
    public static final int ALWAYS             = GLES11.GL_ALWAYS;
    public static final int SMOOTH             = GLES11.GL_SMOOTH;
    public static final int FLAT               = GLES11.GL_FLAT;
    public static final int GREATER            = GLES11.GL_GREATER;

    // Clear/buffer bits
    public static final int COLOR_BUFFER_BIT   = GLES11.GL_COLOR_BUFFER_BIT;
    public static final int DEPTH_BUFFER_BIT   = GLES11.GL_DEPTH_BUFFER_BIT;
    public static final int STENCIL_BUFFER_BIT = GLES11.GL_STENCIL_BUFFER_BIT;

    // Client state
    public static final int VERTEX_ARRAY       = GLES11.GL_VERTEX_ARRAY;
    public static final int NORMAL_ARRAY       = GLES11.GL_NORMAL_ARRAY;
    public static final int COLOR_ARRAY        = GLES11.GL_COLOR_ARRAY;
    public static final int TEXTURE_COORD_ARRAY= GLES11.GL_TEXTURE_COORD_ARRAY;

    // Texture filtering
    public static final int NEAREST            = GLES11.GL_NEAREST;
    public static final int LINEAR             = GLES11.GL_LINEAR;
    public static final int LINEAR_MIPMAP_LINEAR = GLES11.GL_LINEAR_MIPMAP_LINEAR;
    public static final int NEAREST_MIPMAP_NEAREST = GLES11.GL_NEAREST_MIPMAP_NEAREST;
    public static final int TEXTURE_MIN_FILTER = GLES11.GL_TEXTURE_MIN_FILTER;
    public static final int TEXTURE_MAG_FILTER = GLES11.GL_TEXTURE_MAG_FILTER;
    public static final int TEXTURE_WRAP_S     = GLES11.GL_TEXTURE_WRAP_S;
    public static final int TEXTURE_WRAP_T     = GLES11.GL_TEXTURE_WRAP_T;
    public static final int REPEAT             = GLES11.GL_REPEAT;
    public static final int CLAMP_TO_EDGE      = GLES11.GL_CLAMP_TO_EDGE;

    // Texture formats
    public static final int RGBA               = GLES11.GL_RGBA;
    public static final int RGB                = GLES11.GL_RGB;

    // Matrix modes
    public static final int MODELVIEW          = GLES11.GL_MODELVIEW;
    public static final int PROJECTION         = GLES11.GL_PROJECTION;

    // Fog
    public static final int FOG_COLOR          = GLES11.GL_FOG_COLOR;
    public static final int FOG_START          = GLES11.GL_FOG_START;
    public static final int FOG_END            = GLES11.GL_FOG_END;
    public static final int FOG_MODE           = GLES11.GL_FOG_MODE;
    public static final int FOG_DENSITY        = GLES11.GL_FOG_DENSITY;
    public static final int LINEAR_FOG         = GLES11.GL_LINEAR;
    public static final int EXP                = GLES11.GL_EXP;

    // VBO (ARB maps to standard in GLES11)
    public static final int ARRAY_BUFFER       = GLES11.GL_ARRAY_BUFFER;
    public static final int ELEMENT_ARRAY_BUFFER = GLES11.GL_ELEMENT_ARRAY_BUFFER;
    public static final int STATIC_DRAW        = GLES11.GL_STATIC_DRAW;
    public static final int DYNAMIC_DRAW       = GLES11.GL_DYNAMIC_DRAW;

    // ── State ──────────────────────────────────────────────────────────────────

    public static void enable(int cap)                  { GLES11.glEnable(cap); }
    public static void disable(int cap)                 { GLES11.glDisable(cap); }
    public static void enableClientState(int s)         { GLES11.glEnableClientState(s); }
    public static void disableClientState(int s)        { GLES11.glDisableClientState(s); }
    public static void blendFunc(int s, int d)          { GLES11.glBlendFunc(s, d); }
    public static void depthMask(boolean f)             { GLES11.glDepthMask(f); }
    public static void depthFunc(int f)                 { GLES11.glDepthFunc(f); }
    public static void alphaFunc(int f, float r)        { GLES11.glAlphaFunc(f, r); }
    public static void cullFace(int m)                  { GLES11.glCullFace(m); }
    public static void lineWidth(float w)               { GLES11.glLineWidth(w); }
    public static void shadeModel(int m)                { GLES11.glShadeModel(m); }
    public static void clearColor(float r,float g,float b,float a) { GLES11.glClearColor(r,g,b,a); }
    public static void clear(int mask)                  { GLES11.glClear(mask); }
    public static void viewport(int x,int y,int w,int h){ GLES11.glViewport(x,y,w,h); }
    public static void color4f(float r,float g,float b,float a) { GLES11.glColor4f(r,g,b,a); }
    public static void color3f(float r,float g,float b) { GLES11.glColor4f(r,g,b,1f); }
    public static void lightf(int l,int p,float v)      { GLES11.glLightf(l,p,v); }
    public static void lightfv(int l,int p,FloatBuffer b){ GLES11.glLightfv(l,p,b); }
    public static void lightModelfv(int p,FloatBuffer b) { GLES11.glLightModelfv(p,b); }
    public static void colorMaterial(int f,int m)       { /* glColorMaterial not in GLES11 */ }

    // ── Matrix ─────────────────────────────────────────────────────────────────

    public static void matrixMode(int m)        { GLES11.glMatrixMode(m); }
    public static void loadIdentity()           { GLES11.glLoadIdentity(); }
    public static void pushMatrix()             { GLES11.glPushMatrix(); }
    public static void popMatrix()              { GLES11.glPopMatrix(); }
    public static void translatef(float x,float y,float z) { GLES11.glTranslatef(x,y,z); }
    public static void rotatef(float a,float x,float y,float z) { GLES11.glRotatef(a,x,y,z); }
    public static void scalef(float x,float y,float z) { GLES11.glScalef(x,y,z); }
    public static void ortho(double l,double r,double b,double t,double n,double f) {
        GLES11.glOrthof((float)l,(float)r,(float)b,(float)t,(float)n,(float)f);
    }
    public static void frustum(double l,double r,double b,double t,double n,float f) {
        GLES11.glFrustumf((float)l,(float)r,(float)b,(float)t,(float)n,f);
    }
    public static void loadMatrixf(FloatBuffer m) { GLES11.glLoadMatrixf(m); }
    public static void perspective(float fovy,float aspect,float near,float far) {
        float top   = near * (float)Math.tan(Math.toRadians(fovy/2));
        float right = top  * aspect;
        frustum(-right, right, -top, top, near, far);
    }

    // ── Fog ────────────────────────────────────────────────────────────────────

    public static void fogf(int p,float v)       { GLES11.glFogf(p,v); }
    public static void fogfv(int p,FloatBuffer b) { GLES11.glFogfv(p,b); }
    public static void fogi(int p,int v)          { GLES11.glFogx(p,v); }

    // ── Texture ────────────────────────────────────────────────────────────────

    public static int genTexture() {
        int[] t = new int[1]; GLES11.glGenTextures(1,t,0); return t[0];
    }
    public static void bindTexture(int id)       { GLES11.glBindTexture(GLES11.GL_TEXTURE_2D,id); }
    public static void texParameteri(int t,int p,int v) { GLES11.glTexParameterx(t,p,v); }
    public static void texImage2D(int t,int l,int fmt,int w,int h,int b,int f,int type,ByteBuffer px) {
        GLES11.glTexImage2D(t,l,fmt,w,h,b,f,type,px);
    }
    public static void generateMipmap(int target) {
        // GLES11 doesn't have glGenerateMipmap — use GL_GENERATE_MIPMAP hint
        GLES11.glTexParameterx(target, GLES11.GL_GENERATE_MIPMAP, GLES11.GL_TRUE);
    }

    // ── Vertex arrays ──────────────────────────────────────────────────────────

    public static void vertexPointer(int s,int st,FloatBuffer p)   { GLES11.glVertexPointer(s,GLES11.GL_FLOAT,st,p); }
    public static void texCoordPointer(int s,int st,FloatBuffer p)  { GLES11.glTexCoordPointer(s,GLES11.GL_FLOAT,st,p); }
    public static void colorPointer(int s,int st,FloatBuffer p)     { GLES11.glColorPointer(s,GLES11.GL_FLOAT,st,p); }
    public static void normalPointer(int st,FloatBuffer p)          { GLES11.glNormalPointer(GLES11.GL_FLOAT,st,p); }
    public static void drawArrays(int mode,int first,int count) {
        if (mode == QUADS) {
            // Convert quads to triangles
            int quads = count / 4;
            for (int i = 0; i < quads; i++) {
                // Each quad = 2 triangles: [0,1,2] [0,2,3]
                GLES11.glDrawArrays(GLES11.GL_TRIANGLES, first + i*4, 3);
                // Shift vertex buffer for second triangle — approximate
                GLES11.glDrawArrays(GLES11.GL_TRIANGLE_FAN, first + i*4, 4);
            }
        } else {
            GLES11.glDrawArrays(mode, first, count);
        }
    }

    // ── VBO ────────────────────────────────────────────────────────────────────

    public static int genBuffer() {
        int[] b = new int[1]; GLES11.glGenBuffers(1,b,0); return b[0];
    }
    public static void bindBuffer(int t,int id)          { GLES11.glBindBuffer(t,id); }
    public static void bufferData(int t,ByteBuffer d,int u) { GLES11.glBufferData(t,d.capacity(),d,u); }
    public static void bufferData(int t,FloatBuffer d,int u) {
        // Convert FloatBuffer to ByteBuffer for GLES11
        d.rewind();
        ByteBuffer bb = ByteBuffer.allocateDirect(d.remaining() * 4).order(ByteOrder.nativeOrder());
        bb.asFloatBuffer().put(d);
        bb.rewind();
        GLES11.glBufferData(t, bb.capacity(), bb, u);
    }
    public static void deleteBuffer(int id) {
        int[] b = {id}; GLES11.glDeleteBuffers(1,b,0);
    }
    public static void vertexPointer(int s,int st,int offset)   { GLES11.glVertexPointer(s,GLES11.GL_FLOAT,st,offset); }
    public static void texCoordPointer(int s,int st,int offset) { GLES11.glTexCoordPointer(s,GLES11.GL_FLOAT,st,offset); }
    public static void colorPointer(int s,int st,int offset)    { GLES11.glColorPointer(s,GLES11.GL_FLOAT,st,offset); }
    public static void normalPointer(int st,int offset)         { GLES11.glNormalPointer(GLES11.GL_FLOAT,st,offset); }
    public static void drawArrays(int mode,int first,int count,boolean vbo) {
        GLES11.glDrawArrays(mode == QUADS ? GLES11.GL_TRIANGLE_STRIP : mode, first, count);
    }

    // ── Misc ───────────────────────────────────────────────────────────────────

    public static void getIntegerv(int p,IntBuffer d) { GLES11.glGetIntegerv(p,d); }
    public static FloatBuffer newFloatBuffer(int n) {
        return ByteBuffer.allocateDirect(n*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    // ── Immediate-mode emulation (glBegin/glEnd/glVertex3f) ────────────────────
    // Android GLES has no immediate mode; we buffer vertices and flush on end().

    private static final int IM_MAX_VERTS = 4096;
    private static final float[] imVerts = new float[IM_MAX_VERTS * 3];
    private static int imMode  = -1;
    private static int imCount = 0;

    private static final FloatBuffer imBuf =
        ByteBuffer.allocateDirect(IM_MAX_VERTS * 3 * 4)
                  .order(ByteOrder.nativeOrder()).asFloatBuffer();

    public static void begin(int mode) {
        imMode  = mode;
        imCount = 0;
    }

    public static void vertex3f(float x, float y, float z) {
        if (imCount >= IM_MAX_VERTS) return;
        int base = imCount * 3;
        imVerts[base]     = x;
        imVerts[base + 1] = y;
        imVerts[base + 2] = z;
        imCount++;
    }

    public static void end() {
        if (imMode < 0 || imCount == 0) return;
        imBuf.position(0);
        imBuf.put(imVerts, 0, imCount * 3);
        imBuf.position(0);

        GLES11.glEnableClientState(GLES11.GL_VERTEX_ARRAY);
        GLES11.glVertexPointer(3, GLES11.GL_FLOAT, 0, imBuf);

        int glMode;
        if (imMode == QUADS) {
            // Each group of 4 verts = one quad = 2 triangles via TRIANGLE_FAN
            int quads = imCount / 4;
            for (int q = 0; q < quads; q++) {
                GLES11.glDrawArrays(GLES11.GL_TRIANGLE_FAN, q * 4, 4);
            }
        } else {
            GLES11.glDrawArrays(imMode, 0, imCount);
        }

        GLES11.glDisableClientState(GLES11.GL_VERTEX_ARRAY);
        imMode  = -1;
        imCount = 0;
    }

    // No-ops for desktop pick methods (GL name stack doesn't exist on GLES)
    public static void initNames() {}
    public static void pushName(int name) {}
    public static void popName() {}
}
