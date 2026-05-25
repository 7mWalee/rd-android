package client.level;

public interface VertexSink {
    void normal(float x, float y, float z);
    void color(float r, float g, float b);
    void texture(float u, float v);
    void vertex(float x, float y, float z);
}