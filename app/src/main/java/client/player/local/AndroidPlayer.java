package client.player.local;

import android.opengl.GLES11;
import client.level.Level;
import client.net.SocketClient;
import client.phys.AABB;
import com.qyro.android.TouchInput;
import global.Packets;

import java.io.IOException;
import java.util.List;

public class AndroidPlayer {

    public double x, y, z;
    public double prevX, prevY, prevZ;
    public double motionX, motionY, motionZ;
    public float  xRotation, yRotation;
    public double height = 1.8, width = 0.6;
    public AABB   boundingBox;
    public boolean onGround = false;
    public boolean flying   = false;

    private final Level level;
    private static final float SENS   = 0.25f;
    private static final float SPEED  = 0.06f;
    private static final float JUMP   = 0.12f;
    private static final float GRAV   = 0.005f;

    public AndroidPlayer(Level level, double x, double y, double z) {
        this.level = level;
        this.x = x; this.y = y; this.z = z;
        boundingBox = new AABB(x-width/2, y-height, z-width/2, x+width/2, y, z+width/2);
    }

    public void tick() {
        prevX = x; prevY = y; prevZ = z;

        // Look
        yRotation  += TouchInput.lookDX * SENS;
        xRotation  -= TouchInput.lookDY * SENS;
        xRotation   = Math.max(-90, Math.min(90, xRotation));
        TouchInput.resetDeltas();

        float forward = -TouchInput.joystickZ;
        float strafe  =  TouchInput.joystickX;

        if (TouchInput.fly) flying = !flying;

        if (flying) {
            motionY = 0;
            moveRelative(strafe, forward, 0.15f);
            if (TouchInput.jump) motionY =  0.15f;
            move(motionX, motionY, motionZ);
            motionX *= 0.6f; motionY *= 0.6f; motionZ *= 0.6f;
        } else {
            if (TouchInput.jump && onGround) motionY = JUMP;
            moveRelative(strafe, forward, onGround ? SPEED : 0.01f);
            motionY -= GRAV;
            move(motionX, motionY, motionZ);
            motionX *= 0.91f; motionY *= 0.98f; motionZ *= 0.91f;
            if (onGround) { motionX *= 0.6f; motionZ *= 0.6f; }
        }

        try {
            SocketClient.sendPos(Packets.POS, x, y, z, yRotation, xRotation, 0);
        } catch (IOException ignored) {}
    }

    private void moveRelative(float strafe, float forward, float speed) {
        float d = strafe*strafe + forward*forward;
        if (d < 0.01f) { motionX *= 0.91f; motionZ *= 0.91f; return; }
        d = speed / (float)Math.sqrt(d);
        strafe *= d; forward *= d;
        double sin = Math.sin(Math.toRadians(yRotation));
        double cos = Math.cos(Math.toRadians(yRotation));
        motionX += forward*cos + strafe*sin;
        motionZ += forward*sin - strafe*cos;
    }

    public void move(double mx, double my, double mz) {
        double py = my;
        List<AABB> cubes = level.getCubes(boundingBox.expand(mx, my, mz));
        for (AABB c : cubes) my = c.clipYCollide(boundingBox, my);
        boundingBox.move(0, my, 0);
        for (AABB c : cubes) mz = c.clipZCollide(boundingBox, mz);
        boundingBox.move(0, 0, mz);
        for (AABB c : cubes) mx = c.clipXCollide(boundingBox, mx);
        boundingBox.move(mx, 0, 0);
        onGround = py < 0 && my != py;
        if (mx != motionX) motionX = 0;
        if (my != motionY) motionY = 0;
        if (mz != motionZ) motionZ = 0;
        x = (boundingBox.minX + boundingBox.maxX) / 2.0;
        y = boundingBox.minY + height;
        z = (boundingBox.minZ + boundingBox.maxZ) / 2.0;
    }

    public void forcePosition(double nx, double ny, double nz) {
        x = nx; y = ny; z = nz;
        boundingBox = new AABB(x-width/2, y-height, z-width/2, x+width/2, y, z+width/2);
    }

    public void setupCamera(float pt) {
        float ix = (float)(prevX + (x-prevX)*pt);
        float iy = (float)(prevY + (y-prevY)*pt);
        float iz = (float)(prevZ + (z-prevZ)*pt);
        GLES11.glRotatef(xRotation, 1, 0, 0);
        GLES11.glRotatef(yRotation, 0, 1, 0);
        GLES11.glTranslatef(-ix, -(iy + 1.6f), -iz);
    }
}
