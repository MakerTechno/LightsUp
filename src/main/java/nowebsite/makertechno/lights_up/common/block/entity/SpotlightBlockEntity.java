package nowebsite.makertechno.lights_up.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nowebsite.makertechno.lights_up.common.init.LUBlockReg;

public class SpotlightBlockEntity extends BlockEntity {
    // 颜色控制
    private float red = 1.0f;
    private float green = 1.0f;
    private float blue = 1.0f;
    private float intensity = 1.0f;
    // 角度控制 (度)
    private float pitch = 30.0f;  // 俯仰角
    private float yaw = 0.0f;     // 偏航角
    private float coneAngle = 20.0f; // 锥角 (度)

    public SpotlightBlockEntity(BlockPos pos, BlockState state) {
        super(LUBlockReg.SPOTLIGHT_BE.get(), pos, state);
    }

    // Getters / Setters
    public float getRed() { return red; }
    public float getGreen() { return green; }
    public float getBlue() { return blue; }
    public float getIntensity() { return intensity; }
    public float getPitch() { return pitch; }
    public float getYaw() { return yaw; }
    public float getConeAngle() { return coneAngle; }

    public void setColor(float r, float g, float b) {
        this.red = r;
        this.green = g;
        this.blue = b;
    }

    public void setDirection(float pitch, float yaw) {
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public void setConeAngle(float angle) {
        this.coneAngle = Math.clamp(angle, 5.0f, 60.0f);
    }
}