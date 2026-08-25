package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public record EchoSilhouetteBuffers(MultiBufferSource delegate) implements MultiBufferSource {
	private static final int SILHOUETTE_RED = 0;
	private static final int SILHOUETTE_GREEN = 0;
	private static final int SILHOUETTE_BLUE = 0;
	private static final int OPAQUE = 255;
	private static final int NO_LIGHT = 0;

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		return new SilhouetteConsumer(delegate.getBuffer(renderType));
	}

	private record SilhouetteConsumer(VertexConsumer delegate) implements VertexConsumer {
		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			delegate.addVertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) {
			delegate.setColor(SILHOUETTE_RED, SILHOUETTE_GREEN, SILHOUETTE_BLUE, alpha);
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			delegate.setUv(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			delegate.setUv1(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			delegate.setUv2(NO_LIGHT, NO_LIGHT);
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			delegate.setNormal(x, y, z);
			return this;
		}
	}

	public static int opaqueAlpha() {
		return OPAQUE;
	}
}
