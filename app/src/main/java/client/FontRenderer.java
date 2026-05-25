package client;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import client.gfx.GL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FontRenderer {

    private static final int FIRST_CHAR = 32;
    private static final int LAST_CHAR  = 126;

    private int textureId;
    private final int[] charWidth  = new int[128];
    private int charHeight;
    private int atlasWidth, atlasHeight;

    public FontRenderer(Typeface typeface, float sizePx) {
        buildAtlas(typeface, sizePx);
    }

    private void buildAtlas(Typeface typeface, float sizePx) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(typeface);
        paint.setTextSize(sizePx);
        paint.setColor(Color.WHITE);

        Paint.FontMetricsInt fm = paint.getFontMetricsInt();
        charHeight = fm.descent - fm.ascent;
        int ascent = -fm.ascent;

        int cols = 16;
        int rows = (int) Math.ceil((LAST_CHAR - FIRST_CHAR + 1) / (float) cols);
        atlasWidth  = cols * (int)(sizePx + 4);
        atlasHeight = rows * charHeight;

        Bitmap bmp = Bitmap.createBitmap(atlasWidth, atlasHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        int x = 0, y = 0, index = 0;
        for (int c = FIRST_CHAR; c <= LAST_CHAR; c++) {
            String s = String.valueOf((char) c);
            charWidth[c] = (int) paint.measureText(s) + 1;
            canvas.drawText(s, x, y + ascent, paint);
            x += (atlasWidth / cols);
            index++;
            if (index % cols == 0) { x = 0; y += charHeight; }
        }

        int w = bmp.getWidth(), h = bmp.getHeight();
        int[] pixels = new int[w * h];
        bmp.getPixels(pixels, 0, w, 0, 0, w, h);
        bmp.recycle();

        ByteBuffer buf = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder());
        for (int px : pixels) {
            buf.put((byte)((px >> 16) & 0xFF));
            buf.put((byte)((px >>  8) & 0xFF));
            buf.put((byte)( px        & 0xFF));
            buf.put((byte)((px >> 24) & 0xFF));
        }
        buf.flip();

        textureId = GL.genTexture();
        GL.bindTexture(textureId);
        GL.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.NEAREST);
        GL.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.NEAREST);
        GL.texImage2D(GL.TEXTURE_2D, 0, GL.RGBA, w, h, 0, GL.RGBA, GL.UNSIGNED_BYTE, buf);
    }

    public void drawString(String text, int x, int y) {
        GL.bindTexture(textureId);
        android.opengl.GLES11.glEnableClientState(android.opengl.GLES11.GL_VERTEX_ARRAY);
        android.opengl.GLES11.glEnableClientState(android.opengl.GLES11.GL_TEXTURE_COORD_ARRAY);

        int cx   = x;
        int colW = atlasWidth / 16;
        for (char c : text.toCharArray()) {
            if (c < FIRST_CHAR || c > LAST_CHAR) continue;
            int idx = c - FIRST_CHAR;
            int tx  = (idx % 16) * colW;
            int ty  = (idx / 16) * charHeight;
            int w   = charWidth[c];

            float u0 = (float) tx             / atlasWidth;
            float v0 = (float) ty             / atlasHeight;
            float u1 = (float)(tx + w)        / atlasWidth;
            float v1 = (float)(ty + charHeight)/ atlasHeight;

            float[] verts = { cx, y,  cx+w, y,  cx+w, y+charHeight,  cx, y+charHeight };
            float[] uvs   = { u0, v0, u1,   v0, u1,   v1,            u0, v1 };

            ByteBuffer vb = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
            vb.asFloatBuffer().put(verts); vb.position(0);
            ByteBuffer tb = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
            tb.asFloatBuffer().put(uvs);   tb.position(0);

            android.opengl.GLES11.glVertexPointer(2, android.opengl.GLES11.GL_FLOAT, 0, vb);
            android.opengl.GLES11.glTexCoordPointer(2, android.opengl.GLES11.GL_FLOAT, 0, tb);
            android.opengl.GLES11.glDrawArrays(android.opengl.GLES11.GL_TRIANGLE_FAN, 0, 4);

            cx += w;
        }
        android.opengl.GLES11.glDisableClientState(android.opengl.GLES11.GL_TEXTURE_COORD_ARRAY);
        android.opengl.GLES11.glDisableClientState(android.opengl.GLES11.GL_VERTEX_ARRAY);
    }

    /** Draw with optional drop shadow. */
    public void drawString(String text, int x, int y, boolean shadow) {
        if (shadow) {
            GL.color4f(0f, 0f, 0f, 1f);
            drawString(text, x + 2, y + 2);
        }
        GL.color4f(1f, 1f, 1f, 1f);
        drawString(text, x, y);
    }

    /**
     * Draw with an ARGB packed int color (same format as android.graphics.Color).
     * e.g. 0xFFFF0000 = opaque red, 0xFF00FF00 = opaque green.
     */
    public void drawString(String text, int x, int y, int argbColor, boolean shadow) {
        if (shadow) {
            GL.color4f(0f, 0f, 0f, 1f);
            drawString(text, x + 2, y + 2);
        }
        float r = ((argbColor >> 16) & 0xFF) / 255f;
        float g = ((argbColor >>  8) & 0xFF) / 255f;
        float b = ( argbColor        & 0xFF) / 255f;
        float a = ((argbColor >> 24) & 0xFF) / 255f;
        GL.color4f(r, g, b, a);
        drawString(text, x, y);
    }

    /** Draw with an ARGB packed int color, no shadow. */
    public void drawString(String text, int x, int y, int argbColor) {
        drawString(text, x, y, argbColor, false);
    }

    public int getStringWidth(String text) {
        int w = 0;
        for (char c : text.toCharArray()) {
            if (c >= FIRST_CHAR && c <= LAST_CHAR) w += charWidth[c];
        }
        return w;
    }

    public int getStringHeight() { return charHeight; }
}
