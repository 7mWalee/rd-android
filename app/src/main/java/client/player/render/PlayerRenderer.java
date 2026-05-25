package client.player.render;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11;
import client.FontRenderer;
import client.level.Tessellator;
import client.Textures;
import client.player.local.AndroidPlayer;
import client.player.remote.PlayerManager;
import client.player.remote.RemotePlayer;
import client.world.WorldTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayerRenderer {
    private static final float SWING_SPEED    = 0.33f;
    private static final float SWING_AMP      = 0.9f;
    private static final float SWING_EASE     = 0.4f;
    private static final float MOVE_THRESHOLD = 0.05f;

    private final Tessellator tessellator;
    private final int charTexId;

    // A local RemotePlayer used to animate our own player
    private final RemotePlayer selfRemotePlayer = new RemotePlayer("self", 0, 0, 0f, 0, 0);

    private String localUsername = "";

    public PlayerRenderer(int charTexId) {
        this.tessellator = new Tessellator();
        this.charTexId   = charTexId;
    }

    private static void tintColor(Tessellator t, float r, float g, float b) {
        float[] tint  = WorldTime.ambientLight();
        final float floor = 0.12f;
        float tr = Math.max(tint[0], floor);
        float tg = Math.max(tint[1], floor);
        float tb = Math.max(tint[2], floor);
        t.color(r * tr, g * tg, b * tb);
    }

    public void render(PlayerManager playerManager, float pt) {
        renderPlayers(playerManager);
    }

    public void renderPlayers(PlayerManager playerManager) {
        long now = System.currentTimeMillis();

        List<Map.Entry<String, RemotePlayer>> snapshot;
        synchronized (playerManager) {
            snapshot = new ArrayList<>(playerManager.getPlayers().entrySet());
        }

        beginPlayerRender();

        for (Map.Entry<String, RemotePlayer> entry : snapshot) {
            if (localUsername != null && localUsername.equalsIgnoreCase(entry.getKey())) continue;
            RemotePlayer pos = entry.getValue();
            uploadPendingSkin(pos);
            updateAnimation(pos, now);
            renderOnePlayer(pos.x, pos.y, pos.z, pos.yaw, pos.limbSwing,
                    pos.limbSwingAmount, pos.pitch, pos.skinTextureId);
        }

        endPlayerRender();
    }

    public void setLocalUsername(String name) {
        this.localUsername = name;
    }

    public void renderSelf(AndroidPlayer p, PlayerManager playerManager) {
        selfRemotePlayer.x     = p.x;
        selfRemotePlayer.y     = p.y;
        selfRemotePlayer.z     = p.z;
        selfRemotePlayer.yaw   = p.yRotation;
        selfRemotePlayer.pitch = p.xRotation;

        RemotePlayer mine;
        synchronized (playerManager) {
            mine = playerManager.getPlayers().get(localUsername);
        }
        if (mine != null) {
            if (mine.pendingSkinPng != null) {
                selfRemotePlayer.pendingSkinPng = mine.pendingSkinPng;
                mine.pendingSkinPng = null;
            }
            if (mine.skinTextureId != -1) {
                selfRemotePlayer.skinTextureId = mine.skinTextureId;
            }
        }
        uploadPendingSkin(selfRemotePlayer);

        long now = System.currentTimeMillis();
        updateAnimation(selfRemotePlayer, now);

        beginPlayerRender();
        renderOnePlayer(p.x, p.y, p.z, p.yRotation,
                selfRemotePlayer.limbSwing, selfRemotePlayer.limbSwingAmount,
                selfRemotePlayer.pitch, selfRemotePlayer.skinTextureId);
        endPlayerRender();
    }

    public void renderNameTags(PlayerManager playerManager, AndroidPlayer localPlayer,
                               FontRenderer fontRenderer) {
        GLES11.glEnable(GLES11.GL_DEPTH_TEST);
        GLES11.glEnable(GLES11.GL_BLEND);
        GLES11.glBlendFunc(GLES11.GL_SRC_ALPHA, GLES11.GL_ONE_MINUS_SRC_ALPHA);
        GLES11.glEnable(GLES11.GL_TEXTURE_2D);
        GLES11.glDisable(GLES11.GL_FOG);
        GLES11.glDepthMask(false);
        GLES11.glDisable(GLES11.GL_CULL_FACE);

        for (Map.Entry<String, RemotePlayer> entry : playerManager.getPlayers().entrySet()) {
            String name = entry.getKey();
            if (localUsername != null && localUsername.equalsIgnoreCase(name)) continue;

            RemotePlayer pos = entry.getValue();

            GLES11.glPushMatrix();
            GLES11.glTranslatef((float) pos.x, (float)(pos.y + 0.7), (float) pos.z);
            GLES11.glRotatef(-localPlayer.yRotation, 0f, 1f, 0f);
            GLES11.glRotatef( localPlayer.xRotation, 1f, 0f, 0f);

            float scale = 0.015f;
            GLES11.glScalef(scale, -scale, scale);

            int tw = fontRenderer.getStringWidth(name);
            int th = fontRenderer.getStringHeight();
            int xo = -tw / 2;

            GLES11.glDisable(GLES11.GL_TEXTURE_2D);
            GLES11.glColor4f(0f, 0f, 0f, 0.25f);

            // Draw background quad using vertex array
            float[] verts = { xo-2, -1,  xo+tw+2, -1,  xo+tw+2, th+1,  xo-2, th+1 };
            java.nio.FloatBuffer fb = java.nio.ByteBuffer.allocateDirect(32)
                    .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
            fb.put(verts).position(0);
            GLES11.glEnableClientState(GLES11.GL_VERTEX_ARRAY);
            GLES11.glVertexPointer(2, GLES11.GL_FLOAT, 0, fb);
            GLES11.glDrawArrays(GLES11.GL_TRIANGLE_FAN, 0, 4);
            GLES11.glDisableClientState(GLES11.GL_VERTEX_ARRAY);

            GLES11.glEnable(GLES11.GL_TEXTURE_2D);
            GLES11.glColor4f(1f, 1f, 1f, 1f);
            fontRenderer.drawString(name, xo, 0, true);
            Textures.bind(0);
            GLES11.glPopMatrix();
        }

        GLES11.glDepthMask(true);
        GLES11.glEnable(GLES11.GL_CULL_FACE);
        GLES11.glEnable(GLES11.GL_FOG);
        GLES11.glDisable(GLES11.GL_BLEND);
    }

    private void beginPlayerRender() {
        GLES11.glDisable(GLES11.GL_CULL_FACE);
        GLES11.glDisable(GLES11.GL_FOG);
        GLES11.glEnable(GLES11.GL_BLEND);
        GLES11.glBlendFunc(GLES11.GL_SRC_ALPHA, GLES11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void endPlayerRender() {
        Textures.bind(0);
        GLES11.glDisable(GLES11.GL_BLEND);
        GLES11.glEnable(GLES11.GL_CULL_FACE);
        GLES11.glEnable(GLES11.GL_FOG);
        GLES11.glEnable(GLES11.GL_TEXTURE_2D);
    }

    private void renderOnePlayer(double x, double y, double z, float yaw,
                                 float limbSwing, float limbSwingAmount,
                                 float pitch, int skinTextureId) {
        bindPlayerSkin(skinTextureId);
        GLES11.glPushMatrix();
        GLES11.glTranslatef((float) x, (float) y - 1.62f, (float) z);
        GLES11.glRotatef(-yaw, 0f, 1f, 0f);
        renderPlayerModel(limbSwing, limbSwingAmount, pitch, skinTextureId);
        GLES11.glPopMatrix();
    }

    private void uploadPendingSkin(RemotePlayer pos) {
        byte[] png = pos.pendingSkinPng;
        if (png == null) return;
        pos.pendingSkinPng = null;

        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap img = BitmapFactory.decodeByteArray(png, 0, png.length, opts);
            if (img == null) {
                System.err.println("Skin upload failed: decodeByteArray returned null");
                return;
            }

            int w = img.getWidth(), h = img.getHeight();
            if (!((w == 64 && h == 64) || (w == 64 && h == 32))) {
                System.err.println("Skin upload rejected: bad size " + w + "x" + h);
                img.recycle();
                return;
            }

            int[] pixels = new int[w * h];
            img.getPixels(pixels, 0, w, 0, 0, w, h);
            img.recycle();
            sanitizeInnerLayer(pixels, w, h);

            // ARGB -> RGBA
            java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocateDirect(w * h * 4)
                    .order(java.nio.ByteOrder.nativeOrder());
            for (int px : pixels) {
                buf.put((byte)((px >> 16) & 0xFF));
                buf.put((byte)((px >>  8) & 0xFF));
                buf.put((byte)( px        & 0xFF));
                buf.put((byte)((px >> 24) & 0xFF));
            }
            buf.flip();

            if (pos.skinTextureId == -1) {
                int[] ids = new int[1];
                GLES11.glGenTextures(1, ids, 0);
                pos.skinTextureId = ids[0];
            }
            GLES11.glBindTexture(GLES11.GL_TEXTURE_2D, pos.skinTextureId);
            GLES11.glTexParameterx(GLES11.GL_TEXTURE_2D, GLES11.GL_TEXTURE_MIN_FILTER, GLES11.GL_NEAREST);
            GLES11.glTexParameterx(GLES11.GL_TEXTURE_2D, GLES11.GL_TEXTURE_MAG_FILTER, GLES11.GL_NEAREST);
            GLES11.glTexImage2D(GLES11.GL_TEXTURE_2D, 0, GLES11.GL_RGBA, w, h, 0,
                    GLES11.GL_RGBA, GLES11.GL_UNSIGNED_BYTE, buf);

        } catch (Exception e) {
            System.err.println("Skin decode/upload failed: " + e.getMessage());
        }
    }

    private static final int[][] INNER_REGIONS_64x64 = {
            {  0,  0, 32, 16 },
            { 16, 16, 40, 32 },
            { 40, 16, 56, 32 },
            {  0, 16, 16, 32 },
            { 32, 48, 48, 64 },
            { 16, 48, 32, 64 },
    };

    private static void sanitizeInnerLayer(int[] pixels, int w, int h) {
        if (w != 64 || (h != 64 && h != 32)) return;
        for (int[] r : INNER_REGIONS_64x64) {
            int x0 = r[0], y0 = r[1], x1 = r[2], y1 = Math.min(r[3], h);
            if (y0 >= h) continue;
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    int idx = y * w + x;
                    int p = pixels[idx];
                    int a = (p >>> 24) & 0xFF;
                    if (a < 128)       pixels[idx] = 0xFFFFFFFF;
                    else if (a < 255)  pixels[idx] = 0xFF000000 | (p & 0x00FFFFFF);
                }
            }
        }
    }

    private static void updateAnimation(RemotePlayer pos, long now) {
        if (pos.lastAnimTime == 0) {
            pos.lastAnimTime = now;
            pos.prevAnimX = pos.x;
            pos.prevAnimZ = pos.z;
            return;
        }
        double dx = pos.x - pos.prevAnimX, dz = pos.z - pos.prevAnimZ;
        double moved = Math.sqrt(dx*dx + dz*dz);
        float target = (moved > MOVE_THRESHOLD) ? 1f : 0f;
        pos.limbSwingAmount += (target - pos.limbSwingAmount) * SWING_EASE;
        pos.limbSwing       += SWING_SPEED * pos.limbSwingAmount;
        pos.prevAnimX    = pos.x;
        pos.prevAnimZ    = pos.z;
        pos.lastAnimTime = now;
    }

    private void bindPlayerSkin(int skinTextureId) {
        if (skinTextureId != -1) {
            GLES11.glEnable(GLES11.GL_TEXTURE_2D);
            Textures.bind(skinTextureId);
        } else {
            GLES11.glDisable(GLES11.GL_TEXTURE_2D);
        }
    }

    private void renderPlayerModel(float limbSwing, float limbSwingAmount,
                                   float pitch, int skin) {
        float swing    = (float) Math.sin(limbSwing) * SWING_AMP * limbSwingAmount;
        float swingDeg = (float) Math.toDegrees(swing);
        boolean textured = skin != -1;

        tessellator.init();
        if (textured) renderSkinBox(-0.25f,0.60f,-0.125f, 0.25f,1.35f,0.125f, SkinModel.BODY);
        else          renderBox   (-0.25f,0.60f,-0.125f, 0.25f,1.35f,0.125f, 0.22f,0.40f,0.75f);
        tessellator.flush();

        if (textured) {
            GLES11.glDepthMask(false);
            tessellator.init();
            renderSkinBoxScaled(-0.25f,0.60f,-0.125f, 0.25f,1.35f,0.125f, SkinModel.BODY_OUTER, 0.03125f);
            tessellator.flush();
            GLES11.glDepthMask(true);
        }

        renderHead(textured ? skin : -1, pitch);

        renderLimbBox( 0.00f,0.00f,-0.125f, 0.25f,0.60f,0.125f,
                textured?SkinModel.R_LEG:null, textured?SkinModel.R_LEG_OUTER:null,
                0.15f,0.25f,0.55f, 0.60f,  swingDeg);
        renderLimbBox(-0.25f,0.00f,-0.125f, 0.00f,0.60f,0.125f,
                textured?SkinModel.L_LEG:null, textured?SkinModel.L_LEG_OUTER:null,
                0.15f,0.25f,0.55f, 0.60f, -swingDeg);
        renderLimbBox( 0.25f,0.60f,-0.125f, 0.4375f,1.35f,0.125f,
                textured?SkinModel.R_ARM:null, textured?SkinModel.R_ARM_OUTER:null,
                0.85f,0.65f,0.50f, 1.35f, -swingDeg);
        renderLimbBox(-0.4375f,0.60f,-0.125f, -0.25f,1.35f,0.125f,
                textured?SkinModel.L_ARM:null, textured?SkinModel.L_ARM_OUTER:null,
                0.85f,0.65f,0.50f, 1.35f,  swingDeg);
    }

    private void renderHead(int skin, float pitch) {
        float pivotY = 1.35f;
        float p = Math.max(-90f, Math.min(90f, pitch));

        GLES11.glPushMatrix();
        GLES11.glTranslatef(0f, pivotY, 0f);
        GLES11.glRotatef(-p, 1f, 0f, 0f);
        GLES11.glTranslatef(0f, -pivotY, 0f);

        tessellator.init();
        if (skin != -1) renderSkinBox(-0.25f,1.35f,-0.25f, 0.25f,1.85f,0.25f, SkinModel.HEAD);
        else          { renderBox   (-0.25f,1.35f,-0.25f, 0.25f,1.85f,0.25f, 0.85f,0.65f,0.50f); renderPlayerFace(); }
        tessellator.flush();

        if (skin != -1) {
            GLES11.glDepthMask(false);
            tessellator.init();
            renderSkinBoxScaled(-0.25f,1.35f,-0.25f, 0.25f,1.85f,0.25f, SkinModel.HEAD_HAT, 0.0625f);
            tessellator.flush();
            GLES11.glDepthMask(true);
        }
        GLES11.glPopMatrix();
    }

    private void renderLimbBox(float x0, float y0, float z0,
                               float x1, float y1, float z1,
                               float[][] uvBase, float[][] uvOuter,
                               float r, float g, float b,
                               float pivotY, float angleDeg) {
        GLES11.glPushMatrix();
        GLES11.glTranslatef(0f, pivotY, 0f);
        GLES11.glRotatef(angleDeg, 1f, 0f, 0f);
        GLES11.glTranslatef(0f, -pivotY, 0f);

        tessellator.init();
        if (uvBase != null) renderSkinBox(x0,y0,z0,x1,y1,z1, uvBase);
        else                renderBox(x0,y0,z0,x1,y1,z1, r,g,b);
        tessellator.flush();

        if (uvOuter != null) {
            GLES11.glDepthMask(false);
            tessellator.init();
            renderSkinBoxScaled(x0,y0,z0,x1,y1,z1, uvOuter, 0.03125f);
            tessellator.flush();
            GLES11.glDepthMask(true);
        }
        GLES11.glPopMatrix();
    }

    private void renderPlayerFace() {
        float z = -0.251f;
        renderFaceQuad(-0.15f,1.72f,-0.03f,1.65f, z, 1f,   1f,   1f);
        renderFaceQuad( 0.03f,1.72f, 0.15f,1.65f, z, 1f,   1f,   1f);
        renderFaceQuad(-0.13f,1.70f,-0.07f,1.66f, z, 0.08f,0.08f,0.08f);
        renderFaceQuad( 0.07f,1.70f, 0.13f,1.66f, z, 0.08f,0.08f,0.08f);
        renderFaceQuad(-0.10f,1.47f, 0.10f,1.44f, z, 0.25f,0.08f,0.08f);
    }

    private void renderFaceQuad(float x0,float y0,float x1,float y1,float z,float r,float g,float b) {
        tintColor(tessellator,r,g,b);
        tessellator.vertex(x0,y0,z); tessellator.vertex(x1,y0,z);
        tessellator.vertex(x1,y1,z); tessellator.vertex(x0,y1,z);
    }

    private void renderBox(float x0,float y0,float z0,float x1,float y1,float z1,float r,float g,float b) {
        tintColor(tessellator,r,g,b);
        tessellator.vertex(x0,y0,z0); tessellator.vertex(x1,y0,z0); tessellator.vertex(x1,y0,z1); tessellator.vertex(x0,y0,z1);
        tessellator.vertex(x0,y1,z0); tessellator.vertex(x1,y1,z0); tessellator.vertex(x1,y1,z1); tessellator.vertex(x0,y1,z1);
        tessellator.vertex(x0,y0,z0); tessellator.vertex(x1,y0,z0); tessellator.vertex(x1,y1,z0); tessellator.vertex(x0,y1,z0);
        tessellator.vertex(x0,y0,z1); tessellator.vertex(x1,y0,z1); tessellator.vertex(x1,y1,z1); tessellator.vertex(x0,y1,z1);
        tessellator.vertex(x0,y0,z0); tessellator.vertex(x0,y1,z0); tessellator.vertex(x0,y1,z1); tessellator.vertex(x0,y0,z1);
        tessellator.vertex(x1,y0,z0); tessellator.vertex(x1,y1,z0); tessellator.vertex(x1,y1,z1); tessellator.vertex(x1,y0,z1);
    }

    private void renderSkinBox(float x0,float y0,float z0,float x1,float y1,float z1,float[][] uv) {
        tintColor(tessellator,1f,1f,1f);
        for (int f = 0; f < 6; f++) {
            float u0=uv[f][0],v0=uv[f][1],u1=uv[f][2],v1=uv[f][3];
            switch (f) {
                case 0: tessellator.texture(u0,v0);tessellator.vertex(x0,y0,z0); tessellator.texture(u1,v0);tessellator.vertex(x1,y0,z0); tessellator.texture(u1,v1);tessellator.vertex(x1,y0,z1); tessellator.texture(u0,v1);tessellator.vertex(x0,y0,z1); break;
                case 1: tessellator.texture(u0,v1);tessellator.vertex(x0,y1,z0); tessellator.texture(u1,v1);tessellator.vertex(x1,y1,z0); tessellator.texture(u1,v0);tessellator.vertex(x1,y1,z1); tessellator.texture(u0,v0);tessellator.vertex(x0,y1,z1); break;
                case 2: tessellator.texture(u1,v1);tessellator.vertex(x0,y0,z0); tessellator.texture(u0,v1);tessellator.vertex(x1,y0,z0); tessellator.texture(u0,v0);tessellator.vertex(x1,y1,z0); tessellator.texture(u1,v0);tessellator.vertex(x0,y1,z0); break;
                case 3: tessellator.texture(u0,v1);tessellator.vertex(x0,y0,z1); tessellator.texture(u1,v1);tessellator.vertex(x1,y0,z1); tessellator.texture(u1,v0);tessellator.vertex(x1,y1,z1); tessellator.texture(u0,v0);tessellator.vertex(x0,y1,z1); break;
                case 4: tessellator.texture(u0,v1);tessellator.vertex(x0,y0,z0); tessellator.texture(u0,v0);tessellator.vertex(x0,y1,z0); tessellator.texture(u1,v0);tessellator.vertex(x0,y1,z1); tessellator.texture(u1,v1);tessellator.vertex(x0,y0,z1); break;
                case 5: tessellator.texture(u1,v1);tessellator.vertex(x1,y0,z0); tessellator.texture(u1,v0);tessellator.vertex(x1,y1,z0); tessellator.texture(u0,v0);tessellator.vertex(x1,y1,z1); tessellator.texture(u0,v1);tessellator.vertex(x1,y0,z1); break;
            }
        }
    }

    private void renderSkinBoxScaled(float x0,float y0,float z0,float x1,float y1,float z1,float[][] uv,float grow) {
        renderSkinBox(x0-grow,y0-grow,z0-grow,x1+grow,y1+grow,z1+grow,uv);
    }
}
