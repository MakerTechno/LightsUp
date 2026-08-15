package nowebsite.makertechno.lights_up.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import nowebsite.makertechno.lights_up.LightsUp;

public class SpotlightPipelines {

    public static RenderPipeline SPOTLIGHT_BEAM;
    public static RenderPipeline SPOTLIGHT_GLARE;

    public static void register(RegisterRenderPipelinesEvent event) {
        SPOTLIGHT_GLARE = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath(LightsUp.MOD_ID, "pipeline/spotlight_glare"))
                .withVertexShader(Identifier.fromNamespaceAndPath(LightsUp.MOD_ID, "core/spotlight_glare"))
                .withFragmentShader(Identifier.fromNamespaceAndPath(LightsUp.MOD_ID, "core/spotlight_glare"))
                .withUniform("GlareParameters", UniformType.UNIFORM_BUFFER)
                .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
                .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
                .withCull(false)
                .build();
        event.registerPipeline(SPOTLIGHT_GLARE);

        SPOTLIGHT_BEAM = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(LightsUp.MOD_ID, "pipeline/spotlight_beam"))
                .withVertexShader("core/rendertype_lightning")
                .withFragmentShader("core/rendertype_lightning")
                .withCull(false)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build();
        event.registerPipeline(
            SPOTLIGHT_BEAM
        );
    }

    public static RenderType createBeamRenderType() {
        RenderSetup setup = RenderSetup.builder(SPOTLIGHT_BEAM).createRenderSetup();
        return RenderType.create("spotlight_beam", setup);
    }
}