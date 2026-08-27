package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class EchoRenderTypes extends RenderType {
	private static final int BUFFER_SIZE = 1536;

	public static final RenderType ECHO_TEXELS = RenderType.create(
			"echoes_beneath_texels",
			DefaultVertexFormat.POSITION_COLOR,
			VertexFormat.Mode.QUADS,
			BUFFER_SIZE,
			RenderType.CompositeState.builder()
					.setShaderState(POSITION_COLOR_SHADER)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
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
