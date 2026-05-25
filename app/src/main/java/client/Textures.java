package client;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import client.gfx.GL;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Textures {

    private static Context context;

    public static void init(Context ctx) {
        context = ctx;
    }

    public static int loadTexture(String resourceName, int filterMode) {
        // Strip leading slash if present
        String assetPath = resourceName.startsWith("/") ? resourceName.substring(1) : resourceName;

        Bitmap bitmap = null;
        try (InputStream is = context.getAssets().open(assetPath)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitmap = BitmapFactory.decodeStream(is, null, opts);
        } catch (IOException e) {
            throw new RuntimeException("Could not load texture: " + resourceName, e);
        }

        if (bitmap == null) throw new RuntimeException("Null bitmap: " + resourceName);

        int width  = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Extract pixels as RGBA bytes
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        bitmap.recycle();

        // Convert ARGB to RGBA byte order
        ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
        for (int px : pixels) {
            buf.put((byte)((px >> 16) & 0xFF)); // R
            buf.put((byte)((px >>  8) & 0xFF)); // G
            buf.put((byte)( px        & 0xFF)); // B
            buf.put((byte)((px >> 24) & 0xFF)); // A
        }
        buf.flip();

        int id = GL.genTexture();
        GL.bindTexture(id);
        GL.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, filterMode);
        GL.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, filterMode);
        GL.texImage2D(GL.TEXTURE_2D, 0, GL.RGBA, width, height, 0, GL.RGBA, GL.UNSIGNED_BYTE, buf);

        return id;
    }

    public static int loadTextureFromBytes(byte[] pngBytes, int filterMode) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.length, opts);
        if (bitmap == null) return -1;

        int width  = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        bitmap.recycle();

        ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
        for (int px : pixels) {
            buf.put((byte)((px >> 16) & 0xFF));
            buf.put((byte)((px >>  8) & 0xFF));
            buf.put((byte)( px        & 0xFF));
            buf.put((byte)((px >> 24) & 0xFF));
        }
        buf.flip();

        int id = GL.genTexture();
        GL.bindTexture(id);
        GL.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, filterMode);
        GL.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, filterMode);
        GL.texImage2D(GL.TEXTURE_2D, 0, GL.RGBA, width, height, 0, GL.RGBA, GL.UNSIGNED_BYTE, buf);

        return id;
    }

    public static void bind(int id) {
        GL.bindTexture(id);
    }
}
