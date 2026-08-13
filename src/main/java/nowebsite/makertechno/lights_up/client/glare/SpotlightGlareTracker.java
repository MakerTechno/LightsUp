package nowebsite.makertechno.lights_up.client.glare;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public final class SpotlightGlareTracker {
    public static final int MAX_GLARES = 4;

    /** 锥心衰减指数，越大则只有非常正对光轴时才会亮 */
    private static final float CONE_FALLOFF_POWER = 0.1f;
    /** 致盲强度曲线的指数*/
    private static final float WHITEOUT_POWER = 5f;
    /** 致盲强度上限*/
    private static final float WHITEOUT_SCALE = 1.8f;
    /** 光晕的基础半径*/
    private static final float HALO_BASE_RADIUS = 0.08f;
    private static final float HALO_RADIUS_GROWTH = 0.55f;
    /** 光晕整体强度 */
    private static final float HALO_SCALE = 1.4f;
    private static final float ADAPT_RISE = 1f;
    private static final float ADAPT_FALL = 0.5f;
    private static final boolean OCCLUSION_TEST = true;

    private static final List<Source> SOURCES = new ArrayList<>();
    private static final Solution SOLUTION = new Solution();
    private static float smoothedWhiteout = 0.0f;

    private SpotlightGlareTracker() {
    }

    /**
     * @param relPos      光源相对相机的位置
     * @param beamDir     光束轴方向（单位向量，世界空间；方向不受相机平移影响）
     * @param cosHalfCone 半锥角的余弦
     * @param beamLength  光柱长度
     */
    private record Source(Vector3f relPos, Vector3f beamDir, float cosHalfCone, float beamLength,
                          float red, float green, float blue, float intensity) {
    }

    public static final class Solution {
        public final float[] positions = new float[MAX_GLARES * 4];
        public final float[] colors = new float[MAX_GLARES * 4];
        public int count;
        public float whiteout;
        public float aspect = 1.0f;

        public boolean isEmpty() {
            return count == 0 && whiteout <= 0.001f;
        }
    }

    public static void beginFrame() {
        SOURCES.clear();
    }

    public static void submit(Vector3f relPos, Vector3f beamDir, float coneAngle, float beamLength,
                              float red, float green, float blue, float intensity) {
        if (SOURCES.size() >= 64 || intensity <= 0.0f) {
            return;
        }
        float cosHalfCone = (float) Math.cos(Math.toRadians(coneAngle));
        SOURCES.add(new Source(new Vector3f(relPos), new Vector3f(beamDir), cosHalfCone, beamLength,
                red, green, blue, intensity));
    }

    public static Solution solve(CameraRenderState camera, float aspect, float deltaTicks) {
        Solution out = SOLUTION;
        out.count = 0;
        out.aspect = aspect;

        float rawWhiteout = 0.0f;

        if (!SOURCES.isEmpty()) {
            Matrix4f viewProj = new Matrix4f(camera.projectionMatrix).mul(camera.viewRotationMatrix);
            Vector4f clip = new Vector4f();
            Vector3f toEye = new Vector3f();

            for (Source source : SOURCES) {
                float distance = source.relPos.length();
                if (distance < 1.0e-4f || distance > source.beamLength) {
                    continue; // 站在灯里、或者已经超出光柱
                }

                source.relPos.mul(-1.0f / distance, toEye);
                float axisAlign = source.beamDir.dot(toEye);
                if (axisAlign <= source.cosHalfCone) {
                    continue; // 不在光锥里
                }

                float coneFactor = (axisAlign - source.cosHalfCone) / (1.0f - source.cosHalfCone);
                coneFactor = (float) Math.pow(coneFactor, CONE_FALLOFF_POWER);
                float distanceFactor = 1.0f - distance / source.beamLength;

                float strength = coneFactor * distanceFactor * source.intensity;
                if (strength <= 0.002f) {
                    continue;
                }

                if (OCCLUSION_TEST && isOccluded(camera.pos, source.relPos)) {
                    continue;
                }
                clip.set(source.relPos.x, source.relPos.y, source.relPos.z, 1.0f).mul(viewProj);
                if (clip.w <= 1.0e-4f) {
                    // 光源在相机背后
                    rawWhiteout = Math.max(rawWhiteout, strength * 0.15f);
                    continue;
                }
                float ndcX = clip.x / clip.w;
                float ndcY = clip.y / clip.w;

                // 光源越靠近屏幕中心致盲越强
                float offCenter = Mth.sqrt(ndcX * ndcX + ndcY * ndcY);
                float lookFactor = 1.0f - Mth.clamp(offCenter / 1.4f, 0.0f, 1.0f);

                rawWhiteout = Math.max(rawWhiteout, strength * lookFactor);
                insert(out, ndcX, ndcY, strength, source);
            }
        }

        float target = (float) Math.pow(Mth.clamp(rawWhiteout, 0.0f, 1.0f), WHITEOUT_POWER) * WHITEOUT_SCALE;
        float rate = target > smoothedWhiteout ? ADAPT_RISE : ADAPT_FALL;
        float blend = 1.0f - (float) Math.exp(-rate * Math.max(deltaTicks, 0.0f));
        smoothedWhiteout += (target - smoothedWhiteout) * blend;
        if (smoothedWhiteout < 1.0e-4f) {
            smoothedWhiteout = 0.0f;
        }
        out.whiteout = smoothedWhiteout;

        return out;
    }

    private static void insert(Solution out, float ndcX, float ndcY, float strength, Source source) {
        int slot = out.count;
        if (slot >= MAX_GLARES) {
            int weakest = 0;
            for (int i = 1; i < MAX_GLARES; i++) {
                if (out.positions[i * 4 + 2] < out.positions[weakest * 4 + 2]) {
                    weakest = i;
                }
            }
            if (out.positions[weakest * 4 + 2] >= strength) {
                return;
            }
            slot = weakest;
        } else {
            out.count++;
        }

        float radius = HALO_BASE_RADIUS + strength * HALO_RADIUS_GROWTH;
        out.positions[slot * 4] = ndcX;
        out.positions[slot * 4 + 1] = ndcY;
        out.positions[slot * 4 + 2] = strength * HALO_SCALE;
        out.positions[slot * 4 + 3] = radius;

        out.colors[slot * 4] = source.red;
        out.colors[slot * 4 + 1] = source.green;
        out.colors[slot * 4 + 2] = source.blue;
        out.colors[slot * 4 + 3] = 0.0f;
    }

    private static boolean isOccluded(Vec3 cameraPos, Vector3f relPos) {
        Level level = Minecraft.getInstance().level;
        if (level == null || Minecraft.getInstance().player == null) {
            return false;
        }
        Vec3 lightPos = cameraPos.add(relPos.x, relPos.y, relPos.z);
        HitResult hit = level.clip(new ClipContext(
                cameraPos, lightPos,
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                Minecraft.getInstance().player
        ));
        return hit.getType() != HitResult.Type.MISS;
    }
}
