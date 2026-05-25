package com.qyro.android;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class GameView extends GLSurfaceView {

    private final GameRenderer renderer;

    public GameView(Context context, String host, int port, String username) {
        super(context);
        setEGLContextClientVersion(1);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        renderer = new GameRenderer(context, host, port, username);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        TouchInput.onTouch(event);
        return true;
    }

    public GameRenderer getRenderer() { return renderer; }
}
