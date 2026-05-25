package client.level;

import client.*;
import client.gfx.GL;
import client.level.block.BlockRegistry;
import client.level.block.Block;
import client.player.remote.PlayerManager;
import client.phys.AABB;
import client.player.local.AndroidPlayer;
import client.player.render.PlayerRenderer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LevelRenderer implements LevelListener {
    private static final int CHUNK_SIZE = Level.CHUNK_SIZE;
    private final Set<Long> tntPositions = ConcurrentHashMap.newKeySet();

    private final Tessellator tessellator;
    private final Level level;
    private final PlayerRenderer playerRenderer;

    private final ConcurrentHashMap<Long, Chunk> renderChunks = new ConcurrentHashMap<>();

    private final Set<Long> pendingLoad   = ConcurrentHashMap.newKeySet();
    private final Set<Long> pendingUnload = ConcurrentHashMap.newKeySet();

    public LevelRenderer(Level level, int terrainTexId) {
        this.tessellator    = new Tessellator();
        this.level          = level;
        this.playerRenderer = new PlayerRenderer(terrainTexId);
        level.addListener(this);
    }

    private static long rcKey(int cx, int cy, int cz) {
        return ((long)(cx & 0x1FFFFF) << 42)
             | ((long)(cy & 0x1FFFFF) << 21)
             |  (long)(cz & 0x1FFFFF);
    }
    private static int signExtend21(long v) {
        long m = v & 0x1FFFFF;
        return (int)((m & 0x100000L) != 0 ? m | ~0x1FFFFFL : m);
    }
    private static int rcCX(long k) { return signExtend21((k >> 42) & 0x1FFFFF); }
    private static int rcCY(long k) { return signExtend21((k >> 21) & 0x1FFFFF); }
    private static int rcCZ(long k) { return signExtend21( k        & 0x1FFFFF); }

    @Override
    public void chunkLoaded(int cx, int cy, int cz) {
        pendingLoad.add(rcKey(cx, cy, cz));
    }

    @Override
    public void chunkUnloaded(int cx, int cy, int cz) {
        pendingUnload.add(rcKey(cx, cy, cz));
    }

    @Override
    public void lightColumnChanged(int x, int z, int minY, int maxY) {
        setDirty(x-1, minY-1, z-1, x+1, maxY+1, z+1);
    }

    @Override
    public void tileChanged(int x, int y, int z) {
        setDirty(x-1, y-1, z-1, x+1, y+1, z+1);

        long tntKey = packTnt(x, y, z);
        if ((level.getRawBlock(x, y, z) & 0xFF) == BlockRegistry.TNT.id) {
            tntPositions.add(tntKey);
        } else {
            tntPositions.remove(tntKey);
        }
    }

    @Override
    public void allChanged() {
        for (Chunk rc : renderChunks.values()) rc.setDirty();
    }

    private void applyPendingChunks() {
        for (java.util.Iterator<Long> it = pendingUnload.iterator(); it.hasNext(); ) {
            long key = it.next(); it.remove();
            Chunk rc = renderChunks.remove(key);
            if (rc != null) rc.dispose();
            markNeighborsDirty(rcCX(key), rcCY(key), rcCZ(key));
        }

        for (java.util.Iterator<Long> it = pendingLoad.iterator(); it.hasNext(); ) {
            long key = it.next(); it.remove();
            int cx = rcCX(key), cy = rcCY(key), cz = rcCZ(key);
            createRenderChunk(cx, cy, cz);
            markNeighborsDirty(cx, cy, cz);
        }
    }

    private void markNeighborsDirty(int cx, int cy, int cz) {
        markIfPresent(cx-1,cy,cz); markIfPresent(cx+1,cy,cz);
        markIfPresent(cx,cy-1,cz); markIfPresent(cx,cy+1,cz);
        markIfPresent(cx,cy,cz-1); markIfPresent(cx,cy,cz+1);
    }

    private void markIfPresent(int cx, int cy, int cz) {
        Chunk rc = renderChunks.get(rcKey(cx, cy, cz));
        if (rc != null) rc.setDirty();
    }

    private void createRenderChunk(int cx, int cy, int cz) {
        long key = rcKey(cx, cy, cz);
        Chunk rc = renderChunks.get(key);
        if (rc == null) {
            rc = new Chunk(level, cx, cy, cz);
            renderChunks.put(key, rc);

            byte[] data = level.getChunkData(cx, cy, cz);
            if (data != null) {
                int tntId  = BlockRegistry.TNT.id;
                int baseX  = cx * CHUNK_SIZE;
                int baseY  = cy * CHUNK_SIZE;
                int baseZ  = cz * CHUNK_SIZE;
                int idx    = 0;
                for (int ly = 0; ly < CHUNK_SIZE; ly++) {
                    for (int lz = 0; lz < CHUNK_SIZE; lz++) {
                        for (int lx = 0; lx < CHUNK_SIZE; lx++) {
                            if ((data[idx] & 0xFF) == tntId)
                                tntPositions.add(packTnt(baseX+lx, baseY+ly, baseZ+lz));
                            idx++;
                        }
                    }
                }
            }
        }
        rc.setDirty();
    }

    public void setDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int minCX = Math.floorDiv(minX, CHUNK_SIZE), maxCX = Math.floorDiv(maxX, CHUNK_SIZE);
        int minCY = Math.floorDiv(minY, CHUNK_SIZE), maxCY = Math.floorDiv(maxY, CHUNK_SIZE);
        int minCZ = Math.floorDiv(minZ, CHUNK_SIZE), maxCZ = Math.floorDiv(maxZ, CHUNK_SIZE);

        for (Map.Entry<Long, Chunk> e : renderChunks.entrySet()) {
            long k  = e.getKey();
            int cx = rcCX(k), cy = rcCY(k), cz = rcCZ(k);
            if (cx >= minCX && cx <= maxCX
             && cy >= minCY && cy <= maxCY
             && cz >= minCZ && cz <= maxCZ)
                e.getValue().setDirty();
        }
    }

    public void render(int layer) {
        if (layer == 0) {
            applyPendingChunks();
            Chunk.rebuiltThisFrame = 0;
        }
        Frustum frustum = Frustum.getFrustum();
        for (Chunk rc : renderChunks.values()) {
            if (frustum.cubeInFrustum(rc.boundingBox)) rc.render(layer);
        }
    }

    public void rebuildAll() {
        applyPendingChunks();
        for (Chunk rc : renderChunks.values()) {
            rc.rebuildNow(0);
            rc.rebuildNow(1);
        }
    }

    /**
     * Ray-cast block pick — replaces the desktop GL name-stack picking.
     * Returns the HitResult for the closest solid block face within reach,
     * or null if nothing is hit.
     */
    public HitResult pick(AndroidPlayer player, float reach) {
        double px = player.x;
        double py = player.y + 1.6;   // eye height
        double pz = player.z;
        double yaw   = Math.toRadians(player.yRotation);
        double pitch = Math.toRadians(player.xRotation);

        double dirX = -Math.sin(yaw) * Math.cos(pitch);
        double dirY = -Math.sin(pitch);
        double dirZ =  Math.cos(yaw) * Math.cos(pitch);

        HitResult best = null;
        double bestDist = reach + 1;

        int x0 = (int) Math.floor(px - reach - 1);
        int x1 = (int) Math.ceil (px + reach + 1);
        int y0 = (int) Math.floor(py - reach - 1);
        int y1 = (int) Math.ceil (py + reach + 1);
        int z0 = (int) Math.floor(pz - reach - 1);
        int z1 = (int) Math.ceil (pz + reach + 1);

        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (!level.isSolidTile(x, y, z)) continue;
                    // Check each face
                    for (int face = 0; face < 6; face++) {
                        double[] hit = rayFaceIntersect(px, py, pz, dirX, dirY, dirZ,
                                                        x, y, z, face);
                        if (hit == null) continue;
                        double dx = hit[0]-px, dy = hit[1]-py, dz = hit[2]-pz;
                        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                        if (dist < reach && dist < bestDist) {
                            bestDist = dist;
                            best = new HitResult(x, y, z, 0, face);
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Returns intersection point [x,y,z] of ray with a block face, or null. */
    private static double[] rayFaceIntersect(double ox, double oy, double oz,
                                              double dx, double dy, double dz,
                                              int bx, int by, int bz, int face) {
        // face: 0=bottom(y-), 1=top(y+), 2=north(z-), 3=south(z+), 4=west(x-), 5=east(x+)
        double t;
        double[] p = new double[3];
        switch (face) {
            case 0: if (Math.abs(dy) < 1e-9) return null; t=(by   -oy)/dy; p[0]=ox+dx*t; p[1]=by;   p[2]=oz+dz*t; if(p[0]<bx||p[0]>bx+1||p[2]<bz||p[2]>bz+1||t<0) return null; break;
            case 1: if (Math.abs(dy) < 1e-9) return null; t=(by+1 -oy)/dy; p[0]=ox+dx*t; p[1]=by+1; p[2]=oz+dz*t; if(p[0]<bx||p[0]>bx+1||p[2]<bz||p[2]>bz+1||t<0) return null; break;
            case 2: if (Math.abs(dz) < 1e-9) return null; t=(bz   -oz)/dz; p[0]=ox+dx*t; p[1]=oy+dy*t; p[2]=bz;   if(p[0]<bx||p[0]>bx+1||p[1]<by||p[1]>by+1||t<0) return null; break;
            case 3: if (Math.abs(dz) < 1e-9) return null; t=(bz+1 -oz)/dz; p[0]=ox+dx*t; p[1]=oy+dy*t; p[2]=bz+1; if(p[0]<bx||p[0]>bx+1||p[1]<by||p[1]>by+1||t<0) return null; break;
            case 4: if (Math.abs(dx) < 1e-9) return null; t=(bx   -ox)/dx; p[0]=bx;   p[1]=oy+dy*t; p[2]=oz+dz*t; if(p[1]<by||p[1]>by+1||p[2]<bz||p[2]>bz+1||t<0) return null; break;
            case 5: if (Math.abs(dx) < 1e-9) return null; t=(bx+1 -ox)/dx; p[0]=bx+1; p[1]=oy+dy*t; p[2]=oz+dz*t; if(p[1]<by||p[1]>by+1||p[2]<bz||p[2]>bz+1||t<0) return null; break;
            default: return null;
        }
        return p;
    }

    public void renderHit(HitResult hitResult) {
        int blockId = level.getRawBlock(hitResult.x, hitResult.y, hitResult.z) & 0xFF;
        Block block = BlockRegistry.get(blockId);
        if (block == null) return;

        GL.enable(GL.BLEND);
        GL.blendFunc(GL.SRC_ALPHA, GL.ONE_MINUS_SRC_ALPHA);
        GL.color4f(1f, 1f, 1f,
                (float) Math.sin(System.currentTimeMillis() / 100.0) * 0.2f + 0.4f);
        tessellator.init();
        block.renderFace(tessellator, hitResult.x, hitResult.y, hitResult.z, hitResult.face);
        tessellator.flush();
        GL.disable(GL.BLEND);
    }

    public void renderPlayers(PlayerManager playerManager) {
        playerRenderer.renderPlayers(playerManager);
    }

    public void renderSelf(AndroidPlayer p, PlayerManager playerManager) {
        playerRenderer.renderSelf(p, playerManager);
    }

    public void renderNameTags(PlayerManager playerManager, AndroidPlayer localPlayer, FontRenderer fontRenderer) {
        playerRenderer.renderNameTags(playerManager, localPlayer, fontRenderer);
    }

    private static long packTnt(int x, int y, int z) {
        return ((long)(x & 0x1FFFFF) << 42)
             | ((long)(y & 0x1FFFFF) << 21)
             |  (long)(z & 0x1FFFFF);
    }
    private static int unpackTntX(long k) { return signExtend21((k >> 42) & 0x1FFFFF); }
    private static int unpackTntY(long k) { return signExtend21((k >> 21) & 0x1FFFFF); }
    private static int unpackTntZ(long k) { return signExtend21( k        & 0x1FFFFF); }

    public void renderTntOverlay() {
        if (tntPositions.isEmpty()) return;
        float alpha = (float)(Math.sin(System.currentTimeMillis() / 150.0) * 0.5 + 0.5) * 0.8f;

        GL.enable(GL.BLEND);
        GL.blendFunc(GL.SRC_ALPHA, GL.ONE_MINUS_SRC_ALPHA);
        GL.disable(GL.TEXTURE_2D);
        GL.disable(GL.LIGHTING);
        GL.color4f(1f, 1f, 1f, alpha);

        Frustum frustum = Frustum.getFrustum();

        for (long key : tntPositions) {
            int x = unpackTntX(key);
            int y = unpackTntY(key);
            int z = unpackTntZ(key);

            if (!frustum.cubeInFrustum(x, y, z, x+1, y+1, z+1)) continue;

            float x0 = x, x1 = x+1;
            float y0 = y, y1 = y+1;
            float z0 = z, z1 = z+1;

            GL.begin(GL.QUADS);
            if (!level.isSolidTile(x, y-1, z)) { GL.vertex3f(x0,y0,z1); GL.vertex3f(x0,y0,z0); GL.vertex3f(x1,y0,z0); GL.vertex3f(x1,y0,z1); }
            if (!level.isSolidTile(x, y+1, z)) { GL.vertex3f(x1,y1,z1); GL.vertex3f(x1,y1,z0); GL.vertex3f(x0,y1,z0); GL.vertex3f(x0,y1,z1); }
            if (!level.isSolidTile(x, y, z-1)) { GL.vertex3f(x0,y1,z0); GL.vertex3f(x1,y1,z0); GL.vertex3f(x1,y0,z0); GL.vertex3f(x0,y0,z0); }
            if (!level.isSolidTile(x, y, z+1)) { GL.vertex3f(x0,y1,z1); GL.vertex3f(x0,y0,z1); GL.vertex3f(x1,y0,z1); GL.vertex3f(x1,y1,z1); }
            if (!level.isSolidTile(x-1, y, z)) { GL.vertex3f(x0,y1,z1); GL.vertex3f(x0,y1,z0); GL.vertex3f(x0,y0,z0); GL.vertex3f(x0,y0,z1); }
            if (!level.isSolidTile(x+1, y, z)) { GL.vertex3f(x1,y0,z1); GL.vertex3f(x1,y0,z0); GL.vertex3f(x1,y1,z0); GL.vertex3f(x1,y1,z1); }
            GL.end();
        }

        GL.enable(GL.TEXTURE_2D);
        GL.enable(GL.LIGHTING);
        GL.disable(GL.BLEND);
        GL.color4f(1f, 1f, 1f, 1f);
    }
}
