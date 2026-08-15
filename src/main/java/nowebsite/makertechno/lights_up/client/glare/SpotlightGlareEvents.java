package nowebsite.makertechno.lights_up.client.glare;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nowebsite.makertechno.lights_up.LightsUp;

@EventBusSubscriber(modid = LightsUp.MOD_ID, value = Dist.CLIENT)
public final class SpotlightGlareEvents {

    private SpotlightGlareEvents() {
    }

    @SubscribeEvent
    public static void onRenderFrameStart(RenderFrameEvent.Pre event) {
        SpotlightGlareTracker.beginFrame();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onAfterLevel(RenderLevelStageEvent.AfterLevel event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float aspect = (float) minecraft.getWindow().getWidth() / minecraft.getWindow().getHeight();
        float deltaTicks = minecraft.getDeltaTracker().getRealtimeDeltaTicks();

        SpotlightGlareTracker.Solution solution = SpotlightGlareTracker.solve(
                event.getLevelRenderState().cameraRenderState,
                aspect,
                deltaTicks
        );
        SpotlightGlareRenderer.render(solution);
    }
}
