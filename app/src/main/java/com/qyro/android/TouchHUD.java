package com.qyro.android;

import android.opengl.GLES11;
import client.FontRenderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class TouchHUD {

    public static void render(int sw, int sh, FontRenderer font) {
        GLES11.glMatrixMode(GLES11.GL_PROJECTION); GLES11.glPushMatrix(); GLES11.glLoadIdentity();
        GLES11.glOrthof(0, sw, sh, 0, -1, 1);
        GLES11.glMatrixMode(GLES11.GL_MODELVIEW); GLES11.glPushMatrix(); GLES11.glLoadIdentity();
        GLES11.glDisable(GLES11.GL_DEPTH_TEST);
        GLES11.glEnable(GLES11.GL_BLEND);
        GLES11.glBlendFunc(GLES11.GL_SRC_ALPHA, GLES11.GL_ONE_MINUS_SRC_ALPHA);
        GLES11.glDisable(GLES11.GL_TEXTURE_2D);
        GLES11.glEnableClientState(GLES11.GL_VERTEX_ARRAY);

        // Joystick base
        int jcx = sw/8, jcy = sh - sh/5, jr = sh/7;
        GLES11.glColor4f(1f,1f,1f,0.15f);
        drawCircle(jcx, jcy, jr);
        GLES11.glColor4f(1f,1f,1f,0.3f);
        strokeCircle(jcx, jcy, jr);

        // Joystick knob
        int kx = jcx + (int)(TouchInput.joystickX * jr * 0.6f);
        int ky = jcy + (int)(TouchInput.joystickZ * jr * 0.6f);
        GLES11.glColor4f(1f,1f,1f,0.5f);
        drawCircle(kx, ky, jr/3);

        // Buttons (right edge)
        int bw = sw/9, bh = sh/5, bx = sw - bw - 8;
        String[] labels = {"MENU","JUMP","FLY","ATK","PLACE"};
        boolean[] states = {
            TouchInput.menu, TouchInput.jump, TouchInput.fly,
            TouchInput.attack, TouchInput.place
        };
        for (int i = 0; i < 5; i++) {
            int by = i * bh + 4;
            if (states[i]) GLES11.glColor4f(0.4f,0.4f,1f,0.75f);
            else            GLES11.glColor4f(0.1f,0.1f,0.15f,0.55f);
            fillRect(bx, by, bx+bw, by+bh-4);
            GLES11.glColor4f(1f,1f,1f,0.4f);
            strokeRect(bx, by, bx+bw, by+bh-4);
        }

        GLES11.glDisableClientState(GLES11.GL_VERTEX_ARRAY);

        // Button labels
        if (font != null) {
            GLES11.glEnable(GLES11.GL_TEXTURE_2D);
            GLES11.glColor4f(1f,1f,1f,0.9f);
            for (int i = 0; i < labels.length; i++) {
                int by = i * bh + 4;
                int lw = font.getStringWidth(labels[i]);
                int lh = font.getStringHeight();
                font.drawString(labels[i], bx + (bw-lw)/2, by + (bh-4-lh)/2, true);
            }
        }

        GLES11.glDisable(GLES11.GL_BLEND);
        GLES11.glEnable(GLES11.GL_DEPTH_TEST);
        GLES11.glDisableClientState(GLES11.GL_VERTEX_ARRAY);
        GLES11.glPopMatrix();
        GLES11.glMatrixMode(GLES11.GL_PROJECTION); GLES11.glPopMatrix();
        GLES11.glMatrixMode(GLES11.GL_MODELVIEW);
    }

    private static void drawCircle(int cx, int cy, int r) {
        int segs = 32;
        float[] v = new float[(segs+2)*2];
        v[0] = cx; v[1] = cy;
        for (int i = 0; i <= segs; i++) {
            double a = 2*Math.PI*i/segs;
            v[(i+1)*2]   = cx + (float)(r*Math.cos(a));
            v[(i+1)*2+1] = cy + (float)(r*Math.sin(a));
        }
        putAndDraw(v, GLES11.GL_TRIANGLE_FAN);
    }

    private static void strokeCircle(int cx, int cy, int r) {
        int segs = 32;
        float[] v = new float[segs*2];
        for (int i = 0; i < segs; i++) {
            double a = 2*Math.PI*i/segs;
            v[i*2]   = cx + (float)(r*Math.cos(a));
            v[i*2+1] = cy + (float)(r*Math.sin(a));
        }
        putAndDraw(v, GLES11.GL_LINE_LOOP);
    }

    private static void fillRect(int x1,int y1,int x2,int y2) {
        putAndDraw(new float[]{x1,y1, x2,y1, x2,y2, x1,y2}, GLES11.GL_TRIANGLE_FAN);
    }

    private static void strokeRect(int x1,int y1,int x2,int y2) {
        putAndDraw(new float[]{x1,y1, x2,y1, x2,y2, x1,y2}, GLES11.GL_LINE_LOOP);
    }

    private static void putAndDraw(float[] v, int mode) {
        FloatBuffer fb = ByteBuffer.allocateDirect(v.length*4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(v).position(0);
        GLES11.glVertexPointer(2, GLES11.GL_FLOAT, 0, fb);
        GLES11.glDrawArrays(mode, 0, v.length/2);
    }
}
