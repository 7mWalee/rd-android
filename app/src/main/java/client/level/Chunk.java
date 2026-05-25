package client.level;

import client.Textures;
import client.gfx.GL;
import client.level.block.BlockRegistry;
import client.level.block.Block;
import client.phys.AABB;

import java.nio.FloatBuffer;

public class Chunk {

    private static final int TEXTURE = Textures.loadTexture("/client/textures/terrain.png", GL.NEAREST);

    public static int rebuiltThisFrame;
    public static int updates;
    private static final int MAX_REBUILDS_PER_FRAME = 8;

    private final Level level;

    public final AABB boundingBox;
    public final int chunkX;
    public final int chunkY;
    public final int chunkZ;
    final int minX, minY, minZ, maxX, maxY, maxZ;

    private final int[] vboIds = new int[2];
    private final int[] vertexCounts = new int[2];

    private boolean dirty = true;
    private final boolean[] built = new boolean[2];

    public Chunk(Level level, int chunkX, int chunkY, int chunkZ) {
        this.level = level;
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        this.minX = chunkX * Level.CHUNK_SIZE;
        this.minY = chunkY * Level.CHUNK_SIZE;
        this.minZ = chunkZ * Level.CHUNK_SIZE;
        this.maxX = this.minX + Level.CHUNK_SIZE;
        this.maxY = this.minY + Level.CHUNK_SIZE;
        this.maxZ = this.minZ + Level.CHUNK_SIZE;
        this.boundingBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private final ChunkMeshBuilder builder = new ChunkMeshBuilder(1024);

    private void compile(int layer) {
        updates++;

        builder.reset();

        for (int x = this.minX; x < this.maxX; x++) {
            for (int y = this.minY; y < this.maxY; y++) {
                for (int z = this.minZ; z < this.maxZ; z++) {
                    int blockId = this.level.getRawBlock(x, y, z) & 0xFF;
                    if (blockId == 0) continue;
                    Block block = BlockRegistry.get(blockId);
                    if (block == null) continue;
                    block.render(builder, this.level, layer, x, y, z);
                }
            }
        }

        int vc = builder.vertexCount();
        vertexCounts[layer] = vc;

        if (vc == 0) {
            if (vboIds[layer] != 0) {
                GL.deleteBuffer(vboIds[layer]);
                vboIds[layer] = 0;
            }
            built[layer] = true;
            return;
        }

        FloatBuffer data = builder.drain();

        if (vboIds[layer] == 0) {
            vboIds[layer] = GL.genBuffer();
        }
        GL.bindBuffer(GL.ARRAY_BUFFER, vboIds[layer]);
        GL.bufferData(GL.ARRAY_BUFFER, data, GL.STATIC_DRAW);
        GL.bindBuffer(GL.ARRAY_BUFFER, 0);

        built[layer] = true;
    }

    public void render(int layer) {
        if (this.dirty) {
            if (rebuiltThisFrame >= MAX_REBUILDS_PER_FRAME) return;

            compile(0);
            compile(1);
            this.dirty = false;

            rebuiltThisFrame++;
        }

        if (!built[layer]) return;
        int vc = vertexCounts[layer];
        if (vc == 0) return;

        GL.enable(GL.TEXTURE_2D);
        Textures.bind(TEXTURE);

        GL.bindBuffer(GL.ARRAY_BUFFER, vboIds[layer]);

        GL.enableClientState(GL.VERTEX_ARRAY);
        GL.enableClientState(GL.NORMAL_ARRAY);
        GL.enableClientState(GL.COLOR_ARRAY);
        GL.enableClientState(GL.TEXTURE_COORD_ARRAY);

        GL.vertexPointer  (3, ChunkMeshBuilder.STRIDE_BYTES, ChunkMeshBuilder.POS_OFFSET);
        GL.normalPointer  (   ChunkMeshBuilder.STRIDE_BYTES, ChunkMeshBuilder.NORMAL_OFFSET);
        GL.colorPointer   (3, ChunkMeshBuilder.STRIDE_BYTES, ChunkMeshBuilder.COLOR_OFFSET);
        GL.texCoordPointer(2, ChunkMeshBuilder.STRIDE_BYTES, ChunkMeshBuilder.UV_OFFSET);

        GL.drawArrays(GL.QUADS, 0, vc);

        GL.disableClientState(GL.TEXTURE_COORD_ARRAY);
        GL.disableClientState(GL.COLOR_ARRAY);
        GL.disableClientState(GL.NORMAL_ARRAY);
        GL.disableClientState(GL.VERTEX_ARRAY);

        GL.bindBuffer(GL.ARRAY_BUFFER, 0);
        GL.disable(GL.TEXTURE_2D);
    }

    public void rebuildNow(int layer) {
        compile(layer);
        this.dirty = false;
    }

    public void setDirty() { this.dirty = true; }

    public void dispose() {
        for (int i = 0; i < vboIds.length; i++) {
            if (vboIds[i] != 0) {
                GL.deleteBuffer(vboIds[i]);
                vboIds[i] = 0;
            }
            built[i] = false;
            vertexCounts[i] = 0;
        }
    }
}