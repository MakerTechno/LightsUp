package nowebsite.makertechno.lights_up.client.glare;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import nowebsite.makertechno.lights_up.client.render.SpotlightPipelines;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.OptionalInt;


public final class SpotlightGlareRenderer {

    private static final int UBO_SIZE = 16 + SpotlightGlareTracker.MAX_GLARES * 16 * 2;
    private static final int POS_OFFSET = 16;
    private static final int COLOR_OFFSET = POS_OFFSET + SpotlightGlareTracker.MAX_GLARES * 16;
    private static final int INDEX_COUNT = 6;

    private static GpuBuffer parameterUBO;
    private static GpuBuffer vertexBuffer;
    private static ByteBuffer scratch;

    private SpotlightGlareRenderer() {
    }

    public static void render(SpotlightGlareTracker.Solution solution) {
        if (solution.isEmpty() || Minecraft.getInstance().getMainRenderTarget().getColorTextureView() == null) {
            return;
        }

        GpuDevice device = RenderSystem.getDevice();
        CommandEncoder commandEncoder = device.createCommandEncoder();
        ensureResources(device, commandEncoder);

        uploadParameters(commandEncoder, solution);

        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer indexBuffer = indices.getBuffer(INDEX_COUNT);

        try (RenderPass pass = commandEncoder.createRenderPass(
                () -> "Spotlight Glare",
                Minecraft.getInstance().getMainRenderTarget().getColorTextureView(),
                OptionalInt.empty()
        )) {
            pass.setPipeline(SpotlightPipelines.SPOTLIGHT_GLARE);
            pass.setUniform("GlareParameters", parameterUBO);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.setIndexBuffer(indexBuffer, indices.type());
            pass.drawIndexed(0, 0, INDEX_COUNT, 1);
        }
    }

    private static void uploadParameters(CommandEncoder commandEncoder, SpotlightGlareTracker.Solution solution) {
        ByteBuffer buffer = scratch;
        buffer.clear();

        // 眩光数据: aspect, whiteout, count, reserved
        buffer.putFloat(0, solution.aspect);
        buffer.putFloat(4, solution.whiteout);
        buffer.putFloat(8, solution.count);
        buffer.putFloat(12, 0.0f);

        for (int i = 0; i < SpotlightGlareTracker.MAX_GLARES; i++) {
            int base = i * 16;
            boolean active = i < solution.count;
            for (int c = 0; c < 4; c++) {
                float pos = active ? solution.positions[i * 4 + c] : 0.0f;
                float col = active ? solution.colors[i * 4 + c] : 0.0f;
                buffer.putFloat(POS_OFFSET + base + c * 4, pos);
                buffer.putFloat(COLOR_OFFSET + base + c * 4, col);
            }
        }

        buffer.position(0).limit(UBO_SIZE);
        commandEncoder.writeToBuffer(parameterUBO.slice(), buffer);
    }

    private static void ensureResources(GpuDevice device, CommandEncoder commandEncoder) {
        if (parameterUBO != null) {
            return;
        }

        parameterUBO = device.createBuffer(
                () -> "SpotlightGlare ParameterUBO",
                GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
                UBO_SIZE
        );
        scratch = ByteBuffer.allocateDirect(UBO_SIZE).order(ByteOrder.nativeOrder());

        ByteBuffer vertices = ByteBuffer.allocateDirect(4 * 3 * Float.BYTES).order(ByteOrder.nativeOrder());
        putVertex(vertices, -1.0f, -1.0f);
        putVertex(vertices, -1.0f, 1.0f);
        putVertex(vertices, 1.0f, 1.0f);
        putVertex(vertices, 1.0f, -1.0f);
        vertices.flip();

        vertexBuffer = device.createBuffer(
                () -> "SpotlightGlare VertexBuffer",
                GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_VERTEX,
                4 * 3 * Float.BYTES
        );
        commandEncoder.writeToBuffer(vertexBuffer.slice(), vertices);
    }

    private static void putVertex(ByteBuffer buffer, float x, float y) {
        buffer.putFloat(x).putFloat(y).putFloat(0.0f);
    }
}
