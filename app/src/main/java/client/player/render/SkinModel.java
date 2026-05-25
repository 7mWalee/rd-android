package client.player.render;

public final class SkinModel {

    public static final float TEX_SIZE = 64f;

    public static float[][] makeFaces(int topLeftU, int topLeftV, int w, int h, int d) {
        float[][] faces = new float[6][4];

        faces[0] = norm(topLeftU + d + w, topLeftV,         topLeftU + d + 2*w, topLeftV + d);
        // TOP    (+Y) — first small rect on top row
        faces[1] = norm(topLeftU + d,     topLeftV,         topLeftU + d + w,   topLeftV + d);
        // NORTH  (-Z) — front face on side row
        faces[2] = norm(topLeftU + d,     topLeftV + d,     topLeftU + d + w,   topLeftV + d + h);
        // SOUTH  (+Z) — back face on side row
        faces[3] = norm(topLeftU + d + w + d, topLeftV + d, topLeftU + d + w + d + w, topLeftV + d + h);
        // WEST   (-X) — player's left side
        faces[4] = norm(topLeftU + d + w, topLeftV + d,     topLeftU + d + w + d, topLeftV + d + h);
        // EAST   (+X) — player's right side
        faces[5] = norm(topLeftU,         topLeftV + d,     topLeftU + d,         topLeftV + d + h);

        return faces;
    }

    private static float[] norm(int u0, int v0, int u1, int v1) {
        return new float[]{ u0 / TEX_SIZE, v0 / TEX_SIZE, u1 / TEX_SIZE, v1 / TEX_SIZE };
    }

    public static final float[][] HEAD = makeFaces( 0,  0, 8, 8, 8);
    public static final float[][] HEAD_HAT = makeFaces(32, 0, 8, 8, 8);   // outer layer

    public static final float[][] BODY = makeFaces(16, 16, 8, 12, 4);
    public static final float[][] BODY_OUTER = makeFaces(16, 32, 8, 12, 4);

    public static final float[][] R_ARM= makeFaces(40, 16, 4, 12, 4);
    public static final float[][] R_ARM_OUTER = makeFaces(40, 32, 4, 12, 4);

    public static final float[][] L_ARM = makeFaces(32, 48, 4, 12, 4);
    public static final float[][] L_ARM_OUTER = makeFaces(48, 48, 4, 12, 4);

    public static final float[][] R_LEG = makeFaces( 0, 16, 4, 12, 4);
    public static final float[][] R_LEG_OUTER = makeFaces( 0, 32, 4, 12, 4);

    public static final float[][] L_LEG = makeFaces(16, 48, 4, 12, 4);
    public static final float[][] L_LEG_OUTER = makeFaces( 0, 48, 4, 12, 4);

    private SkinModel() {}
}
