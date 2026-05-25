package client.level;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11;
import android.opengl.GLUtils;
import client.Textures;
import client.world.WorldTime;
import client.player.local.AndroidPlayer;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public final class SkyRenderer {
    private static final float SKY_RADIUS = 100f;
    private static final float SUN_SIZE  = 15f;
    private static final float MOON_SIZE = 10f;
    private static final int MOON_COLS = 4;
    private static final int MOON_ROWS = 2;
    private static final int MOON_PHASES = MOON_COLS * MOON_ROWS;

    private int sunTexture  = -1;
    private int moonTexture = -1;

    private long cyclesElapsedSinceStart = 0;
    private float lastFraction = -1f;

    // Reusable quad vertex buffer: 4 verts * (3 pos + 2 uv) floats
    private final FloatBuffer quadBuf;

    public SkyRenderer() {
        quadBuf = ByteBuffer.allocateDirect(4 * 5 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    public void init(android.content.Context ctx) {
        sunTexture  = loadTexture(ctx, "client/textures/sun.png");
        moonTexture = loadTexture(ctx, "client/textures/moon_phases.png");
    }

    public void render(AndroidPlayer localPlayer, WorldTime worldTime, float pt) {
        if (sunTexture == -1 && moonTexture == -1) return;

        float f = WorldTime.fraction();

        if (lastFraction >= 0f && f < lastFraction - 0.5f) {
            cyclesElapsedSinceStart++;
        }
        lastFraction = f;
        float sunAlpha  = WorldTime.sunStrength();
        float moonAlpha = 1f - sunAlpha;

        GLES11.glPushMatrix();

        if (localPlayer != null) {
            GLES11.glTranslatef((float) localPlayer.x,
                                (float) localPlayer.y,
                                (float) localPlayer.z);
        }

        boolean lightingWasOn     = GLES11.glIsEnabled(GLES11.GL_LIGHTING);
        boolean colorMaterialOn   = GLES11.glIsEnabled(GLES11.GL_COLOR_MATERIAL);
        if (lightingWasOn)   GLES11.glDisable(GLES11.GL_LIGHTING);
        if (colorMaterialOn) GLES11.glDisable(GLES11.GL_COLOR_MATERIAL);

        GLES11.glDisable(GLES11.GL_FOG);
        GLES11.glDisable(GLES11.GL_CULL_FACE);
        GLES11.glDisable(GLES11.GL_DEPTH_TEST);
        GLES11.glEnable(GLES11.GL_BLEND);
        GLES11.glBlendFunc(GLES11.GL_ONE, GLES11.GL_ONE);
        GLES11.glDepthMask(false);
        GLES11.glEnable(GLES11.GL_TEXTURE_2D);
        GLES11.glTexEnvf(GLES11.GL_TEXTURE_ENV, GLES11.GL_TEXTURE_ENV_MODE, GLES11.GL_MODULATE);

        GLES11.glEnableClientState(GLES11.GL_VERTEX_ARRAY);
        GLES11.glEnableClientState(GLES11.GL_TEXTURE_COORD_ARRAY);

        if (sunTexture != -1 && sunAlpha > 0.001f) {
            GLES11.glPushMatrix();
            GLES11.glRotatef(360f * (f - 0.5f), 0f, 0f, 1f);
            Textures.bind(sunTexture);
            GLES11.glColor4f(1f, 1f, 1f, sunAlpha);
            drawQuad(SUN_SIZE, 0f, 0f, 1f, 1f);
            GLES11.glPopMatrix();
        }

        if (moonTexture != -1 && moonAlpha > 0.001f) {
            GLES11.glPushMatrix();
            GLES11.glRotatef(360f * (f - 0.5f) + 180f, 0f, 0f, 1f);
            Textures.bind(moonTexture);
            int phaseIdx = (int) Math.floorMod(cyclesElapsedSinceStart, (long) MOON_PHASES);
            int col = phaseIdx % MOON_COLS;
            int row = phaseIdx / MOON_COLS;
            float u0 = col       / (float) MOON_COLS;
            float u1 = (col + 1) / (float) MOON_COLS;
            float v0 = row       / (float) MOON_ROWS;
            float v1 = (row + 1) / (float) MOON_ROWS;
            GLES11.glColor4f(1f, 1f, 1f, moonAlpha);
            drawQuad(MOON_SIZE, u0, v0, u1, v1);
            GLES11.glPopMatrix();
        }

        GLES11.glDisableClientState(GLES11.GL_TEXTURE_COORD_ARRAY);
        GLES11.glDisableClientState(GLES11.GL_VERTEX_ARRAY);

        GLES11.glColor4f(1f, 1f, 1f, 1f);
        GLES11.glDepthMask(true);
        GLES11.glEnable(GLES11.GL_DEPTH_TEST);
        GLES11.glBlendFunc(GLES11.GL_SRC_ALPHA, GLES11.GL_ONE_MINUS_SRC_ALPHA);
        GLES11.glDisable(GLES11.GL_BLEND);
        GLES11.glEnable(GLES11.GL_CULL_FACE);
        if (colorMaterialOn) GLES11.glEnable(GLES11.GL_COLOR_MATERIAL);
        if (lightingWasOn)   GLES11.glEnable(GLES11.GL_LIGHTING);

        GLES11.glPopMatrix();
    }

    private void drawQuad(float size, float u0, float v0, float u1, float v1) {
        float y = SKY_RADIUS;
        // interleaved: x,y,z, u,v
        float[] data = {
            -size, y, -size,  u0, v0,
             size, y, -size,  u1, v0,
             size, y,  size,  u1, v1,
            -size, y,  size,  u0, v1,
        };
        quadBuf.position(0);
        quadBuf.put(data);
        quadBuf.position(0);

        // Stride = 5 floats * 4 bytes = 20
        FloatBuffer posBuf = quadBuf.duplicate();
        posBuf.position(0);
        FloatBuffer uvBuf  = quadBuf.duplicate();
        uvBuf.position(3);

        GLES11.glVertexPointer(3, GLES11.GL_FLOAT, 20, posBuf);
        GLES11.glTexCoordPointer(2, GLES11.GL_FLOAT, 20, uvBuf);
        GLES11.glDrawArrays(GLES11.GL_TRIANGLE_FAN, 0, 4);
    }

    private static int loadTexture(android.content.Context ctx, String assetPath) {
        try (InputStream in = ctx.getAssets().open(assetPath)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bmp = BitmapFactory.decodeStream(in, null, opts);
            if (bmp == null) {
                System.err.println("Sky texture not found: " + assetPath);
                return -1;
            }

            int w = bmp.getWidth(), h = bmp.getHeight();
            int[] pixels = new int[w * h];
            bmp.getPixels(pixels, 0, w, 0, 0, w, h);
            bmp.recycle();

            // ARGB -> RGBA byte conversion
            ByteBuffer buf = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder());
            for (int px : pixels) {
                buf.put((byte)((px >> 16) & 0xFF)); // R
                buf.put((byte)((px >>  8) & 0xFF)); // G
                buf.put((byte)( px        & 0xFF)); // B
                buf.put((byte)((px >> 24) & 0xFF)); // A
            }
            buf.flip();

            int[] ids = new int[1];
            GLES11.glGenTextures(1, ids, 0);
            int id = ids[0];
            GLES11.glBindTexture(GLES11.GL_TEXTURE_2D, id);
            GLES11.glTexParameterx(GLES11.GL_TEXTURE_2D, GLES11.GL_TEXTURE_MIN_FILTER, GLES11.GL_NEAREST);
            GLES11.glTexParameterx(GLES11.GL_TEXTURE_2D, GLES11.GL_TEXTURE_MAG_FILTER, GLES11.GL_NEAREST);
            GLES11.glTexParameterx(GLES11.GL_TEXTURE_2D, GLES11.GL_TEXTURE_WRAP_S, GLES11.GL_CLAMP_TO_EDGE);
            GLES11.glTexParameterx(GLES11.GL_TEXTURE_2D, GLES11.GL_TEXTURE_WRAP_T, GLES11.GL_CLAMP_TO_EDGE);
            GLES11.glTexImage2D(GLES11.GL_TEXTURE_2D, 0, GLES11.GL_RGBA, w, h, 0,
                    GLES11.GL_RGBA, GLES11.GL_UNSIGNED_BYTE, buf);
            return id;
        } catch (Exception e) {
            System.err.println("Failed to load sky texture " + assetPath + ": " + e.getMessage());
            return -1;
        }
    }
}
