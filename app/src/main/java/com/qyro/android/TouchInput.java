package com.qyro.android;

import android.view.MotionEvent;

public class TouchInput {

    // Joystick (left side)
    public static float  joystickX = 0, joystickZ = 0;
    private static float jcx, jcy;
    private static int   jPid = -1;

    // Look (right-center drag)
    private static float lpx, lpy;
    private static int   lPid = -1;
    public static float  lookDX = 0, lookDY = 0;

    // Buttons (right edge)
    public static volatile boolean jump    = false;
    public static volatile boolean attack  = false;
    public static volatile boolean place   = false;
    public static volatile boolean menu    = false;
    public static volatile boolean fly     = false;

    private static int sw, sh;

    public static void setSize(int w, int h) { sw = w; sh = h; }

    public static void onTouch(MotionEvent e) {
        int action = e.getActionMasked();
        int idx    = e.getActionIndex();
        int pid    = e.getPointerId(idx);
        float x    = e.getX(idx);
        float y    = e.getY(idx);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                handleDown(pid, x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++)
                    handleMove(e.getPointerId(i), e.getX(i), e.getY(i));
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                handleUp(pid, x, y);
                break;
        }
    }

    private static void handleDown(int pid, float x, float y) {
        // Left 35% = joystick
        if (x < sw * 0.35f) {
            jPid = pid; jcx = x; jcy = y;
            joystickX = 0; joystickZ = 0;
        }
        // Middle = look drag
        else if (x < sw * 0.72f) {
            lPid = pid; lpx = x; lpy = y;
        }
        // Right = buttons
        else {
            float btnH = sh / 5f;
            if      (y < btnH)   menu   = true;
            else if (y < btnH*2) jump   = true;
            else if (y < btnH*3) fly    = true;
            else if (y < btnH*4) attack = true;
            else                 place  = true;
        }
    }

    private static void handleMove(int pid, float x, float y) {
        if (pid == jPid) {
            float r  = sw * 0.12f;
            float dx = x - jcx, dy = y - jcy;
            float d  = (float) Math.sqrt(dx*dx + dy*dy);
            if (d > r) { dx = dx/d*r; dy = dy/d*r; }
            joystickX = dx / r;
            joystickZ = dy / r;
        } else if (pid == lPid) {
            lookDX = x - lpx;
            lookDY = y - lpy;
            lpx = x; lpy = y;
        }
    }

    private static void handleUp(int pid, float x, float y) {
        if (pid == jPid) { jPid = -1; joystickX = 0; joystickZ = 0; }
        if (pid == lPid) { lPid = -1; lookDX = 0; lookDY = 0; }
        // Reset button state
        jump = attack = place = menu = fly = false;
    }

    public static void resetDeltas() { lookDX = 0; lookDY = 0; }
}
