package client.net;




import global.Packets;
import client.level.Level;


import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SocketClient implements Runnable {
    private final String host;
    private final int port;
    private final String username;
    /** Identifier used in the .auth file ("host:port"). */
    private final String serverId;
    private Socket socket;
    private static DataOutputStream out;
    private DataInputStream in;
    public boolean authenticated;

    public static final ConcurrentLinkedQueue<int[]> pendingBlocks = new ConcurrentLinkedQueue<>();

    private static final Object writeLock = new Object();

    public SocketClient(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.serverId = host + ":" + port;
    }


    @Override
    public void run() {
        try {
            socket = new Socket(host, port);

            in  = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), 8192));


            String storedToken = AuthStore.getToken(serverId, username);
            if (storedToken == null) storedToken = "";

            out.writeByte(Packets.AUTH_REQUEST);
            out.writeUTF(username);
            out.writeUTF(storedToken);
            out.flush();


            byte response = in.readByte();

            if (response != Packets.AUTH_SUCCESS) {
                if (response == Packets.AUTH_FAILED) {
                    String reason = in.readUTF();
                }
                System.err.println("Authentication failed!");
                socket.close();
                return;
            }

            System.out.println("Authenticated successfully as " + username);
            authenticated = true;

            uploadSkinIfPresent();

            sendRenderDistance(client.Settings.getRenderDistance());

            out.writeByte(Packets.REQUEST_LEVEL);
            out.flush();


            while (true) {
                byte packetId = in.readByte();

                switch (packetId) {

                    case Packets.AUTH_TOKEN: {
                        String newToken = in.readUTF();
                        AuthStore.saveToken(serverId, username, newToken);
                        System.out.println("Saved new auth token for " + username + " on " + serverId);
                        break;
                    }

                    case Packets.CHUNK_DATA: {
                        int cx = in.readInt();
                        int cy = in.readInt();
                        int cz = in.readInt();
                        byte[] data = new byte[16 * 16 * 16];
                        in.readFully(data);
                        Level level = com.qyro.android.GameRenderer.instance.level;
                        if (level != null) {
                            level.loadChunk(cx, cy, cz, data);
                            if (!com.qyro.android.GameRenderer.instance.levelReady) com.qyro.android.GameRenderer.instance.levelReady = true;
                        }
                        break;
                    }

                    case Packets.CHUNK_UNLOAD: {
                        int cx = in.readInt();
                        int cy = in.readInt();
                        int cz = in.readInt();
                        Level level = com.qyro.android.GameRenderer.instance.level;
                        if (level != null) level.unloadChunk(cx, cy, cz);
                        break;
                    }

                    case Packets.BLOCK_PLACE: {
                        int x = in.readInt(), y = in.readInt(), z = in.readInt();
                        int id = in.readByte() & 0xFF;
                        pendingBlocks.add(new int[]{x, y, z, id});
                        break;
                    }

                    case Packets.BLOCK_BREAK: {
                        int x = in.readInt(), y = in.readInt(), z = in.readInt();
                        pendingBlocks.add(new int[]{x, y, z, 0});
                        break;
                    }

                    case Packets.SET_POS: {
                        double x = in.readDouble(), y = in.readDouble(), z = in.readDouble();
                        com.qyro.android.GameRenderer.instance.spawnX = x;
                        com.qyro.android.GameRenderer.instance.spawnY = y;
                        com.qyro.android.GameRenderer.instance.spawnZ = z;
                        com.qyro.android.GameRenderer.instance.spawnReceived = true;
                        if (com.qyro.android.GameRenderer.instance.localPlayer != null) com.qyro.android.GameRenderer.instance.localPlayer.forcePosition(x, y, z);
                        break;
                    }

                    case Packets.POS: {
                        String uname = in.readUTF();
                        double x = in.readDouble(), y = in.readDouble(), z = in.readDouble();
                        float yaw = in.readFloat();
                        float pitch = in.readFloat();
                        com.qyro.android.GameRenderer.instance.playerManager.updatePlayer(uname, x, y, z, yaw, pitch);
                        break;
                    }

                    case Packets.PING_INFO: {
                        String uname = in.readUTF();
                        int pingMs = in.readInt();
                        com.qyro.android.GameRenderer.instance.playerManager.updatePing(uname, pingMs);
                        break;
                    }

                    case Packets.CHAT: {
                        String author  = in.readUTF();
                        String message = in.readUTF();
                        System.out.println("[CHAT] " + author + ": " + message);
                        break;
                    }

                    case Packets.KEEPALIVE: {
                        long time = in.readLong();
                        boolean isResponse = in.readBoolean();
                        if (isResponse) {
                            com.qyro.android.GameRenderer.instance.rtt = System.currentTimeMillis() - time;
                        } else {
                            SocketClient.sendKeepaliveResponse(time);
                        }
                        break;
                    }

                    case Packets.CONNECTION: {
                        int type  = in.readInt();
                        String uname = in.readUTF();
                        System.out.println("[CONNECT] " + uname + " type=" + type);
                        if (type == 1) {
                            com.qyro.android.GameRenderer.instance.playerManager.removePlayer(uname);
                        } else {
                            // sendPosition not implemented
                        }
                        break;
                    }

                    case Packets.SKIN_DATA: {
                        String uname = in.readUTF();
                        int len = in.readInt();
                        byte[] png = new byte[len];
                        in.readFully(png);
                        com.qyro.android.GameRenderer.instance.playerManager.setPendingSkin(uname, png);
                        break;
                    }

                    case Packets.TIME_OF_DAY: {
                        float fraction = in.readFloat();
                        long  cycleLen = in.readLong();
                        client.world.WorldTime.syncFromServer(fraction, cycleLen);
                        break;
                    }

                    default:
                        throw new IOException("Unknown packet id: " + packetId + " stream desynced, closing connection");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            if (false) {
                System.err.println("Disconnected.");
            }
        }
    }


    public static void sendBlock(int packet, int x, int y, int z, int blockId) throws IOException {
        synchronized (writeLock) {
            out.writeByte(packet);
            out.writeInt(x); out.writeInt(y); out.writeInt(z);
            if (packet == Packets.BLOCK_PLACE) out.writeByte(blockId);
            out.flush();
        }
    }

    public static void sendBlock(int packet, int x, int y, int z) throws IOException {
        sendBlock(packet, x, y, z, 0);
    }

    public static void sendPos(int packet, double x, double y, double z, float yaw, float pitch, int ping)
            throws IOException {
        synchronized (writeLock) {
            out.writeByte(packet);
            out.writeDouble(x); out.writeDouble(y); out.writeDouble(z);
            out.writeFloat(yaw); out.writeFloat(pitch); out.writeInt(ping);
            out.flush();
        }
    }

    public static void sendKeepalive(long timestamp) throws IOException {
        synchronized (writeLock) {
            out.writeByte(Packets.KEEPALIVE);
            out.writeLong(timestamp);
            out.writeBoolean(false);
            out.flush();
        }
    }

    public static void sendRenderDistance(int chunks) {
        if (out == null) return;
        try {
            synchronized (writeLock) {
                out.writeByte(Packets.CLIENT_RENDER_DISTANCE);
                out.writeInt(chunks);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("sendRenderDistance failed: " + e.getMessage());
        }
    }

    public void disconnect() {
        authenticated = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}
        try {
            if (out != null) out.close();
        } catch (IOException ignored) {}
    }

    public static void sendKeepaliveResponse(long serverTimestamp) throws IOException {
        synchronized (writeLock) {
            out.writeByte(Packets.KEEPALIVE);
            out.writeLong(serverTimestamp);
            out.writeBoolean(true);
            out.flush();
        }
    }

    public static void sendChat(String author, String message) throws IOException {
        synchronized (writeLock) {
            out.writeByte(Packets.CHAT);
            out.writeUTF(author);
            out.writeUTF(message);
            out.flush();
        }
    }

    public static void sendSkin(byte[] png) throws IOException {
        synchronized (writeLock) {
            out.writeByte(Packets.SKIN_UPLOAD);
            out.writeInt(png.length);
            out.write(png);
            out.flush();
        }
    }

    private void uploadSkinIfPresent() {
        Path p = Paths.get("skins/skin.png");
        if (!Files.exists(p)) return;
        try {
            byte[] png = Files.readAllBytes(p);
            sendSkin(png);
            System.out.println("Uploaded skin from " + p + " (" + png.length + " bytes)");
        } catch (IOException e) {
            System.err.println("Failed to read/upload rd-skin.png: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}