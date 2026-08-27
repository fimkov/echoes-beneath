package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

public record EchoSilhouetteBuffers(RenderType layer, int red, int green, int blue,
		int alpha, int light, boolean fading) {
	private static final int OPAQUE = 255;
	private static final int REVEAL_BLOCK_LIGHT = 11;

	// The entity keeps whatever layer its own renderer asked for; only the colour
	// and the light are taken over.
	public static EchoSilhouetteBuffers forEntity(float reveal, float presence) {
		return of(null, reveal, presence);
	}

	public static EchoSilhouetteBuffers forBlocks(float reveal, float presence) {
		return of(RenderTypes.translucentMovingBlock(), reveal, presence);
	}

	private static EchoSilhouetteBuffers of(RenderType layer, float reveal, float presence) {
		int level = Math.round(Mth.clamp(reveal, 0.0F, 1.0F) * OPAQUE);
		int visible = Math.round(Mth.clamp(presence, 0.0F, 1.0F) * OPAQUE);

		return new EchoSilhouetteBuffers(layer, level, level, level, visible,
				LightCoordsUtil.pack(Math.round(Mth.clamp(reveal, 0.0F, 1.0F) * REVEAL_BLOCK_LIGHT), 0),
				visible < OPAQUE);
	}

	public int tint() {
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}

	public boolean isInvisible() {
		return alpha <= 0;
	}

	public VertexConsumer wrap(VertexConsumer consumer) {
		return new SilhouetteConsumer(consumer, red, green, blue, fading ? alpha : OPAQUE, light);
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

		@Override

		public VertexConsumer setLineWidth(float width) {
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

		@Override

		public VertexConsumer setLineWidth(float width) {
			delegate.setLineWidth(width);

			return this;
		}
	}
}
