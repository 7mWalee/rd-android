package client.level;

public final class FaceTextures {

    public static final int BOTTOM = 0;
    public static final int TOP = 1;
    public static final int NORTH = 2;
    public static final int SOUTH = 3;
    public static final int WEST = 4;
    public static final int EAST = 5;

    private final int[] cols = new int[6];

    private FaceTextures(int b, int t, int n, int s, int w, int e) {
        cols[BOTTOM] = b;
        cols[TOP] = t;
        cols[NORTH] = n;
        cols[SOUTH] = s;
        cols[WEST] = w;
        cols[EAST] = e;
    }

    public static FaceTextures uniform(int col) {
        return new FaceTextures(col, col, col, col, col, col);
    }

    public static FaceTextures column(int top, int side, int bottom) {
        return new FaceTextures(bottom, top, side, side, side, side);
    }

    public static FaceTextures perFace(int bottom, int top, int north, int south, int west, int east) {
        return new FaceTextures(bottom, top, north, south, west, east);
    }

    public int col(int face) {
        return cols[face];
    }
}