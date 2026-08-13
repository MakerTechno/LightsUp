package nowebsite.makertechno.lights_up.common.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
//import dev.anvilcraft.lib.v2.rendering.ALRPostEffects;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nowebsite.makertechno.lights_up.client.glare.SpotlightGlareTracker;
import nowebsite.makertechno.lights_up.client.render.SpotlightShaderManager;
import nowebsite.makertechno.lights_up.common.block.SpotlightBlock;
import nowebsite.makertechno.lights_up.common.block.entity.SpotlightBlockEntity;
import nowebsite.makertechno.lights_up.common.block.entity.state.SpotlightRenderState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpotlightRenderer implements BlockEntityRenderer<@NotNull SpotlightBlockEntity, @NotNull SpotlightRenderState> {

    private static final int CIRCLE_SEGMENTS = 32;
    private static final int CONE_SECTIONS = 16;
    //private static final float BLOOM_CORE_RATIO = 0.7f;
    //private static final float BLOOM_INTENSITY = 1.6f;

    public SpotlightRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public SpotlightRenderState createRenderState() {
        return new SpotlightRenderState();
    }

    @Override
    public void extractRenderState(SpotlightBlockEntity blockEntity, SpotlightRenderState state, float partialTicks, Vec3 cameraPosition,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        state.red = blockEntity.getRed();
        state.green = blockEntity.getGreen();
        state.blue = blockEntity.getBlue();
        state.intensity = blockEntity.getIntensity();
        state.pitch = blockEntity.getPitch();
        state.yaw = blockEntity.getYaw();
        state.coneAngle = blockEntity.getConeAngle();

        if (blockEntity.getBlockState().getBlock() instanceof SpotlightBlock) {
            state.facing = blockEntity.getBlockState().getValue(SpotlightBlock.FACING);
        }

        float alpha = 0.6f * state.intensity;
        int ir = (int) (Math.min(1.0f, state.red) * 255);
        int ig = (int) (Math.min(1.0f, state.green) * 255);
        int ib = (int) (Math.min(1.0f, state.blue) * 255);
        int ia = (int) (Math.min(1.0f, alpha) * 255);
        state.packedColor = (ia << 24) | (ir << 16) | (ig << 8) | ib;
    }

    @Override
    public void submit(SpotlightRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {

        float beamLength = state.beamLength;
        float baseRadius = (float) (beamLength * Math.tan(Math.toRadians(state.coneAngle)));
        int color = state.packedColor;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        applyFacingRotation(poseStack, state.facing);

        Matrix4f beamMatrix = new Matrix4f(poseStack.last().pose());

        SpotlightGlareTracker.submit(
                beamMatrix.getTranslation(new Vector3f()),
                beamMatrix.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f)).normalize(),
                state.coneAngle,
                beamLength,
                state.red, state.green, state.blue, state.intensity
        );

        submitNodeCollector.submitCustomGeometry(
                poseStack,
                SpotlightShaderManager.getBeamRenderType(),
                (pose, consumer) -> renderCone(pose.pose(), consumer, beamLength, baseRadius, color, 1.6f)
                );

        poseStack.popPose();

        /*ALRPostEffects.getBloomPostEffect().drawBloomed((bloomCollector, bloomPoseStack) -> {
            bloomPoseStack.pushPose();
            bloomPoseStack.mulPose(beamMatrix);
            bloomCollector.submitCustomGeometry(
                    bloomPoseStack,
                    SpotlightShaderManager.getBeamRenderType(),
                    (pose, consumer) -> {
                        renderCone(pose.pose(), consumer, beamLength,baseRadius * BLOOM_CORE_RATIO, color, BLOOM_INTENSITY)
                    }
            );
            bloomPoseStack.popPose();
        });*///Not needed anymore ??aw
    }

    private void renderCone(Matrix4f matrix, VertexConsumer consumer,
                            float beamLength, float radius, int baseColor, float intensityScale) {

        float halfAngle = (float) Math.atan2(radius, beamLength);
        float normalY = -(float) Math.sin(halfAngle);
        float normalXZ = (float) Math.cos(halfAngle);

        float[][] circle = new float[CIRCLE_SEGMENTS][2];
        Vector3f[] viewNormals = new Vector3f[CIRCLE_SEGMENTS];

        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            float angle = (float) (2 * Math.PI * i / CIRCLE_SEGMENTS);
            float x = (float) Math.cos(angle);
            float z = (float) Math.sin(angle);

            circle[i][0] = x;
            circle[i][1] = z;

            viewNormals[i] = new Vector3f(x * normalXZ, normalY, z * normalXZ)
                    .normalize()
                    .mulDirection(matrix)
                    .normalize();
        }

        int[][] ringColors = new int[CONE_SECTIONS + 1][CIRCLE_SEGMENTS];
        for (int section = 0; section <= CONE_SECTIONS; section++) {
            float t = (float) section / CONE_SECTIONS;
            float y = beamLength * t;
            float r = radius * t;

            for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
                float intensity = vertexIntensity(matrix, viewNormals[i], circle[i][0] * r, y, circle[i][1] * r, t);
                ringColors[section][i] = withIntensity(baseColor, intensity * intensityScale);
            }
        }

        for (int section = 0; section < CONE_SECTIONS; section++) {
            float t0 = (float) section / CONE_SECTIONS;
            float t1 = (float) (section + 1) / CONE_SECTIONS;

            float y0 = beamLength * t0;
            float y1 = beamLength * t1;
            float r0 = radius * t0;
            float r1 = radius * t1;

            for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
                int nextI = (i + 1) % CIRCLE_SEGMENTS;

                float xi0 = circle[i][0] * r0, zi0 = circle[i][1] * r0;
                float xj0 = circle[nextI][0] * r0, zj0 = circle[nextI][1] * r0;
                float xi1 = circle[i][0] * r1, zi1 = circle[i][1] * r1;
                float xj1 = circle[nextI][0] * r1, zj1 = circle[nextI][1] * r1;

                int ci0 = ringColors[section][i];
                int cj0 = ringColors[section][nextI];
                int ci1 = ringColors[section + 1][i];
                int cj1 = ringColors[section + 1][nextI];

                addVertex(consumer, matrix, xi0, y0, zi0, ci0);
                addVertex(consumer, matrix, xi1, y1, zi1, ci1);
                addVertex(consumer, matrix, xj0, y0, zj0, cj0);

                addVertex(consumer, matrix, xi1, y1, zi1, ci1);
                addVertex(consumer, matrix, xj1, y1, zj1, cj1);
                addVertex(consumer, matrix, xj0, y0, zj0, cj0);
            }
        }
    }

    private float vertexIntensity(Matrix4f matrix, Vector3f viewNormal, float x, float y, float z, float t) {
        Vector3f viewPos = new Vector3f(x, y, z).mulPosition(matrix);
        float distance = viewPos.length();
        if (distance < 1.0e-4f) {
            return 0.0f;
        }
        Vector3f toCam = viewPos.mul(-1.0f / distance);

        float facing = Math.abs(viewNormal.dot(toCam));
        float core = facing * facing;

        float lengthFade = calculateAlphaAtDistance(t);
        float heightFade = 1.0f - t * 0.7f;

        return core * lengthFade * heightFade;
    }


    private float calculateAlphaAtDistance(float t) {
        if (t < 0.1f) {
            return t * 10.0f;
        } else if (t < 0.8f) {
            return 1.0f;
        } else {
            return 1.0f - (t - 0.8f) * 5.0f;
        }
    }


    private static int withIntensity(int argb, float intensity) {
        int a = Mth.clamp((int) (((argb >>> 24) & 0xFF) * intensity), 0, 255);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private void addVertex(VertexConsumer consumer, Matrix4f matrix,
                           float x, float y, float z, int color) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(color);
    }

    private void applyFacingRotation(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case UP -> {}
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case SOUTH -> poseStack.mulPose(Axis.XN.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90));
            case WEST -> poseStack.mulPose(Axis.ZN.rotationDegrees(90));
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(SpotlightBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(114);
    }

    @Override
    public boolean shouldRender(SpotlightBlockEntity blockEntity, Vec3 cameraPosition) {
        return true;
    }
}