package nowebsite.makertechno.lights_up.client.render;

import net.minecraft.client.renderer.rendertype.RenderType;

public class SpotlightShaderManager {

    private static RenderType cachedBeamRenderType = null;

    public static RenderType getBeamRenderType() {
        if (cachedBeamRenderType == null) {
            cachedBeamRenderType = SpotlightPipelines.createBeamRenderType();
        }
        return cachedBeamRenderType;
    }
}