package com.qyro.android;

import android.content.Context;
import android.graphics.Typeface;
import android.opengl.GLES11;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import client.FontRenderer;
import client.Settings;
import client.Textures;
import client.Timer;
import client.gfx.GL;
import client.level.Frustum;
import client.level.Level;
import client.level.LevelRenderer;
import client.level.SkyRenderer;
import client.net.AuthStore;
import client.net.SocketClient;
import client.player.local.AndroidPlayer;
import client.player.remote.PlayerManager;
import client.player.render.PlayerRenderer;
import client.world.WorldTime;
import global.Packets;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.FloatBuffer;

public class GameRenderer implements GLSurfaceView.Renderer {

    public static GameRenderer instance;

    private final Context ctx;
    private int width, height;

    // Connection info — stored before GL is ready
    private String pendingHost;
    private int    pendingPort;
    private String pendingUsername;

    // Game state
    public Level         level;
    public LevelRenderer levelRenderer;
    public SkyRenderer   skyRenderer;
    public PlayerManager playerManager;
    public AndroidPlayer localPlayer;
    public PlayerRenderer playerRenderer;
    public WorldTime     worldTime;
    public FontRenderer  font;

    public String  username      = "player";
    public String  connectedHost = "rd.saturn.lat";
    public int     connectedPort = 9090;
    public long    rtt           = 0;

    public volatile boolean levelReady    = false;
    public volatile boolean spawnReceived = false;
    public double spawnX, spawnY, spawnZ;

    private final Timer timer = new Timer(20f);
    private SocketClient socket;
    private Thread socketThread;

    private int terrainTex, charTex;
    private boolean initialized = false;

    private final float[] projMatrix = new float[16];
    private final float[] mvMatrix   = new float[16];
    private final float[] clipMatrix = new float[16];

    public GameRenderer(Context ctx, String host, int port, String username) {
        this.ctx = ctx;
        instance = this;
        this.pendingHost     = host;
        this.pendingPort     = port;
        this.pendingUsername = username;
        Settings.init(ctx);
        AuthStore.init(ctx);
        Textures.init(ctx);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES11.glClearColor(0.5f, 0.8f, 1f, 1f);
        GLES11.glEnable(GLES11.GL_DEPTH_TEST);
        GLES11.glEnable(GLES11.GL_TEXTURE_2D);
        GLES11.glEnable(GLES11.GL_CULL_FACE);

        try {
            Typeface tf = Typeface.createFromAsset(ctx.getAssets(), "fonts/Minecraft.ttf");
            font = new FontRenderer(tf, 16f);
        } catch (Exception e) {
            // Font optional
        }

        try { terrainTex = Textures.loadTexture("textures/terrain.png", GL.NEAREST); }
        catch (Exception e) { terrainTex = -1; }

        charTex = -1;
        try { charTex = Textures.loadTexture("textures/char.png", GL.NEAREST); }
        catch (Exception ignored) {}

        worldTime     = new WorldTime();
        playerManager = new PlayerManager();
        playerRenderer = new PlayerRenderer(charTex);
        playerRenderer.setLocalUsername(pendingUsername);
        level = new Level();

        initialized = true;

        // Now safe to connect — GL context exists
        connect(pendingHost, pendingPort, pendingUsername);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        width = w; height = h;
        GLES11.glViewport(0, 0, w, h);
        TouchInput.setSize(w, h);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!initialized) return;

        timer.advanceTime();

        if (spawnReceived && localPlayer == null && level != null) {
            localPlayer    = new AndroidPlayer(level, spawnX, spawnY, spawnZ);
            levelRenderer  = new LevelRenderer(level, terrainTex);
            skyRenderer    = new SkyRenderer();
            skyRenderer.init(ctx);
            levelReady     = true;
            spawnReceived  = false;
        }

        int[] update;
        while ((update = SocketClient.pendingBlocks.poll()) != null) {
            if (level != null) level.setTile(update[0], update[1], update[2], update[3]);
        }

        for (int i = 0; i < timer.ticks; i++) {
            if (localPlayer != null && socket != null && socket.isConnected())
                localPlayer.tick();
            if (worldTime != null) worldTime.tick();
        }

        render(timer.partialTicks);
    }

    private void buildClipMatrix() {
        FloatBuffer fb = GL.newFloatBuffer(16);
        GLES11.glGetFloatv(GLES11.GL_PROJECTION_MATRIX, fb);
        fb.get(projMatrix); fb.position(0);
        GLES11.glGetFloatv(GLES11.GL_MODELVIEW_MATRIX, fb);
        fb.get(mvMatrix);
        Matrix.multiplyMM(clipMatrix, 0, projMatrix, 0, mvMatrix, 0);
        Frustum.setClipMatrix(clipMatrix);
    }

    private void render(float pt) {
        GLES11.glClear(GLES11.GL_COLOR_BUFFER_BIT | GLES11.GL_DEPTH_BUFFER_BIT);

        if (!levelReady || localPlayer == null) {
            renderConnecting();
            return;
        }

        GLES11.glMatrixMode(GLES11.GL_PROJECTION);
        GLES11.glLoadIdentity();
        GL.perspective(70f, (float) width / height, 0.05f, 1000f);
        GLES11.glMatrixMode(GLES11.GL_MODELVIEW);
        GLES11.glLoadIdentity();

        localPlayer.setupCamera(pt);
        buildClipMatrix();

        if (skyRenderer != null) skyRenderer.render(localPlayer, worldTime, pt);

        if (terrainTex != -1) {
            GLES11.glEnable(GLES11.GL_TEXTURE_2D);
            Textures.bind(terrainTex);
        }
        if (levelRenderer != null) levelRenderer.render(1);

        playerRenderer.render(playerManager, pt);

        renderHUD();
        TouchHUD.render(width, height, font);
    }

    private void renderConnecting() {
        GLES11.glMatrixMode(GLES11.GL_PROJECTION); GLES11.glLoadIdentity();
        GLES11.glOrthof(0, width, height, 0, -1, 1);
        GLES11.glMatrixMode(GLES11.GL_MODELVIEW); GLES11.glLoadIdentity();
        GLES11.glDisable(GLES11.GL_DEPTH_TEST);
        GLES11.glDisable(GLES11.GL_TEXTURE_2D);
        GLES11.glColor4f(0.1f, 0.1f, 0.15f, 1f);

        float[] v = {0, 0, width, 0, width, height, 0, height};
        java.nio.FloatBuffer fb = java.nio.ByteBuffer.allocateDirect(32)
                .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(v).position(0);
        GLES11.glEnableClientState(GLES11.GL_VERTEX_ARRAY);
        GLES11.glVertexPointer(2, GLES11.GL_FLOAT, 0, fb);
        GLES11.glDrawArrays(GLES11.GL_TRIANGLE_FAN, 0, 4);
        GLES11.glDisableClientState(GLES11.GL_VERTEX_ARRAY);

        if (font != null) {
            GLES11.glEnable(GLES11.GL_TEXTURE_2D);
            GLES11.glEnable(GLES11.GL_BLEND);
            GLES11.glBlendFunc(GLES11.GL_SRC_ALPHA, GLES11.GL_ONE_MINUS_SRC_ALPHA);
            GL.color4f(1f, 1f, 1f, 1f);
            String msg = socket != null && socket.authenticated ? "Waiting for chunks..." : "Connecting...";
            font.drawString(msg, width / 2 - font.getStringWidth(msg) / 2, height / 2, true);
            GLES11.glDisable(GLES11.GL_BLEND);
        }
        GLES11.glEnable(GLES11.GL_DEPTH_TEST);
    }

    private void renderHUD() {
        GLES11.glMatrixMode(GLES11.GL_PROJECTION); GLES11.glPushMatrix(); GLES11.glLoadIdentity();
        GLES11.glOrthof(0, width, height, 0, -1, 1);
        GLES11.glMatrixMode(GLES11.GL_MODELVIEW); GLES11.glPushMatrix(); GLES11.glLoadIdentity();
        GLES11.glDisable(GLES11.GL_DEPTH_TEST);
        GLES11.glEnable(GLES11.GL_BLEND);
        GLES11.glBlendFunc(GLES11.GL_SRC_ALPHA, GLES11.GL_ONE_MINUS_SRC_ALPHA);

        GLES11.glDisable(GLES11.GL_TEXTURE_2D);
        GLES11.glColor4f(1f, 1f, 1f, 0.8f);
        int cx = width / 2, cy = height / 2, cs = 12;
        float[] lines = {cx - cs, cy, cx + cs, cy, cx, cy - cs, cx, cy + cs};
        java.nio.FloatBuffer fb = java.nio.ByteBuffer.allocateDirect(32)
                .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(lines).position(0);
        GLES11.glEnableClientState(GLES11.GL_VERTEX_ARRAY);
        GLES11.glVertexPointer(2, GLES11.GL_FLOAT, 0, fb);
        GLES11.glDrawArrays(GLES11.GL_LINES, 0, 4);
        GLES11.glDisableClientState(GLES11.GL_VERTEX_ARRAY);

        if (localPlayer != null && font != null) {
            GLES11.glEnable(GLES11.GL_TEXTURE_2D);
            GLES11.glColor4f(1f, 1f, 1f, 1f);
            String pos = String.format("X:%.1f Y:%.1f Z:%.1f", localPlayer.x, localPlayer.y, localPlayer.z);
            font.drawString(pos, 4, 4, true);
        }

        GLES11.glDisable(GLES11.GL_BLEND);
        GLES11.glEnable(GLES11.GL_DEPTH_TEST);
        GLES11.glPopMatrix();
        GLES11.glMatrixMode(GLES11.GL_PROJECTION); GLES11.glPopMatrix();
        GLES11.glMatrixMode(GLES11.GL_MODELVIEW);
    }

    public void connect(String host, int port, String user) {
        connectedHost = host; connectedPort = port; username = user;
        Settings.setString("username", user);
        Settings.setString("host", host);
        Settings.setString("port", String.valueOf(port));

        if (socketThread != null && socketThread.isAlive()) {
            if (socket != null) socket.disconnect();
        }
        levelReady = false; spawnReceived = false; localPlayer = null;
        level = new Level();

        socket = new SocketClient(host, port, user);
        socketThread = new Thread(socket, "SocketClient");
        socketThread.setDaemon(true);
        socketThread.start();
    }

    public FontRenderer getClientFont() { return font; }
}
