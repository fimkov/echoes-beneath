package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class EchoRenderTypes extends RenderType {
	private static final int BUFFER_SIZE = 1536;
	private static final ResourceLocation WAVE_TEXTURE = EchoesBeneath.id("textures/effect/echo_wave.png");

	public static final RenderType ECHO_WAVE = RenderType.create(
			"echoes_beneath_wave",
			DefaultVertexFormat.POSITION_TEX_COLOR,
			VertexFormat.Mode.QUADS,
			BUFFER_SIZE,
			RenderType.CompositeState.builder()
					.setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
					.setTextureState(new TextureStateShard(WAVE_TEXTURE, false, false))
					.setTransparencyState(ADDITIVE_TRANSPARENCY)
					.setCullState(CULL)
					.setDepthTestState(LEQUAL_DEPTH_TEST)
					.setWriteMaskState(COLOR_WRITE)
					.setLayeringState(VIEW_OFFSET_Z_LAYERING)
					.createCompositeState(false));

	private EchoRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
			boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
		super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
	}
}
