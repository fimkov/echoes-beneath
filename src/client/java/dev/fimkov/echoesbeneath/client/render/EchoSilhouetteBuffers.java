package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public record EchoSilhouetteBuffers(MultiBufferSource delegate, int red, int green, int blue) implements MultiBufferSource {
	private static final int NO_LIGHT = 0;

	public static EchoSilhouetteBuffers black(MultiBufferSource delegate) {
		return new EchoSilhouetteBuffers(delegate, 0, 0, 0);
	}

	public static EchoSilhouetteBuffers tinted(MultiBufferSource delegate, int argb) {
		return new EchoSilhouetteBuffers(delegate,
				(argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		return new SilhouetteConsumer(delegate.getBuffer(renderType), red, green, blue);
	}

	private record SilhouetteConsumer(VertexConsumer delegate, int red, int green, int blue)
			implements VertexConsumer {
		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			delegate.addVertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer setColor(int ignoredRed, int ignoredGreen, int ignoredBlue, int alpha) {
			delegate.setColor(red, green, blue, alpha);
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
}
