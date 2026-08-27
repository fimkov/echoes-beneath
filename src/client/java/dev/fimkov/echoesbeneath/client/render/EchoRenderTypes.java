package dev.fimkov.echoesbeneath.client.render;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class EchoRenderTypes {
	public static final RenderType ECHO_TEXELS = RenderType.create(
			"echoes_beneath_texels",
			RenderSetup.builder(RenderPipelines.DEBUG_QUADS)
					.sortOnUpload()
					.createRenderSetup());

	private EchoRenderTypes() {
	}
}
