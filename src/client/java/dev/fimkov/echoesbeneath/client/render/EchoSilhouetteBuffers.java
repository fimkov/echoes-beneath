package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public record EchoSilhouetteBuffers(MultiBufferSource delegate, RenderType layer,
		int red, int green, int blue, int alpha, int light, boolean fading) implements MultiBufferSource {
	private static final int OPAQUE = 255;
	private static final int REVEAL_BLOCK_LIGHT = 11;

	public static EchoSilhouetteBuffers forEntity(MultiBufferSource delegate, ResourceLocation texture,
			float reveal, float presence) {
		return of(delegate, RenderType.entityTranslucent(texture), reveal, presence);
	}

	public static EchoSilhouetteBuffers forBlocks(MultiBufferSource delegate, float reveal, float presence) {
		return of(delegate, RenderType.translucent(), reveal, presence);
	}

	private static EchoSilhouetteBuffers of(MultiBufferSource delegate, RenderType layer,
			float reveal, float presence) {
		int level = Math.round(Mth.clamp(reveal, 0.0F, 1.0F) * OPAQUE);
		int visible = Math.round(Mth.clamp(presence, 0.0F, 1.0F) * OPAQUE);
		return new EchoSilhouetteBuffers(delegate, layer, level, level, level, visible,
				LightTexture.pack(Math.round(Mth.clamp(reveal, 0.0F, 1.0F) * REVEAL_BLOCK_LIGHT), 0),
				visible < OPAQUE);
	}

	public boolean isInvisible() {
		return alpha <= 0;
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		if (!fading) {
			return new SilhouetteConsumer(delegate.getBuffer(renderType), red, green, blue, OPAQUE, light);
		}
		if (renderType.format() != layer.format()) {
			return DISCARD;
		}
		return new SilhouetteConsumer(delegate.getBuffer(layer), red, green, blue, alpha, light);
	}

	private static final VertexConsumer DISCARD = new VertexConsumer() {
		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			return this;
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) {
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			return this;
		}
	};

	private record SilhouetteConsumer(VertexConsumer delegate, int red, int green, int blue,
			int alpha, int light) implements VertexConsumer {
		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			delegate.addVertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer setColor(int sourceRed, int sourceGreen, int sourceBlue, int sourceAlpha) {
			int shade = Math.max(sourceRed, Math.max(sourceGreen, sourceBlue));
			delegate.setColor(red * shade / OPAQUE, green * shade / OPAQUE, blue * shade / OPAQUE,
					alpha * sourceAlpha / OPAQUE);
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
			delegate.setUv2(light & 0xFFFF, (light >> 16) & 0xFFFF);
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			delegate.setNormal(x, y, z);
			return this;
		}
	}
}
