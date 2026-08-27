package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.fimkov.echoesbeneath.client.chart.EchoChartModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.joml.Matrix4f;

// Fluids carry no block model, so a charted pool would be a hole in the middle
// of the memory. Every wet cell gets a plain cube in the fluid's own texture,
// which is all the detail survives the downsampling anyway.
public final class EchoChartFluid {
	private static final int WATER_TINT = 0x3F76E4;
	private static final int ALPHA = 0xCC;

	private EchoChartFluid() {
	}

	public static void emit(EchoChartModel model, SubmitNodeCollector submits, PoseStack poseStack,
			BlockState state, int cell, int light) {
		FluidState fluid = state.getFluidState();

		if (fluid.isEmpty()) {
			return;
		}

		FluidModel look = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid);
		TextureAtlasSprite sprite = look.stillMaterial().sprite();
		int tint = fluid.is(FluidTags.WATER) ? WATER_TINT : 0xFFFFFF;
		int x = EchoChartModel.cellX(cell);
		int y = EchoChartModel.cellY(cell);
		int z = EchoChartModel.cellZ(cell);

		submits.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), (pose, consumer) -> {
			Matrix4f matrix = pose.pose();

			if (!model.blocks(x, y - 1, z) && !model.wet(x, y - 1, z)) {
				face(consumer, matrix, sprite, tint, light, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, -1, 0);
			}

			if (!model.blocks(x, y + 1, z) && !model.wet(x, y + 1, z)) {
				face(consumer, matrix, sprite, tint, light, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0);
			}

			if (!model.blocks(x, y, z - 1) && !model.wet(x, y, z - 1)) {
				face(consumer, matrix, sprite, tint, light, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, -1);
			}

			if (!model.blocks(x, y, z + 1) && !model.wet(x, y, z + 1)) {
				face(consumer, matrix, sprite, tint, light, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1);
			}

			if (!model.blocks(x - 1, y, z) && !model.wet(x - 1, y, z)) {
				face(consumer, matrix, sprite, tint, light, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, -1, 0, 0);
			}

			if (!model.blocks(x + 1, y, z) && !model.wet(x + 1, y, z)) {
				face(consumer, matrix, sprite, tint, light, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0);
			}
		});
	}

	// Drawn from both sides, because a memory is something you can walk into.
	private static void face(VertexConsumer consumer, Matrix4f pose, TextureAtlasSprite sprite,
			int tint, int light,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3,
			float normalX, float normalY, float normalZ) {
		corner(consumer, pose, sprite, tint, light, x0, y0, z0, sprite.getU0(), sprite.getV0(), normalX, normalY, normalZ);
		corner(consumer, pose, sprite, tint, light, x1, y1, z1, sprite.getU0(), sprite.getV1(), normalX, normalY, normalZ);
		corner(consumer, pose, sprite, tint, light, x2, y2, z2, sprite.getU1(), sprite.getV1(), normalX, normalY, normalZ);
		corner(consumer, pose, sprite, tint, light, x3, y3, z3, sprite.getU1(), sprite.getV0(), normalX, normalY, normalZ);

		corner(consumer, pose, sprite, tint, light, x3, y3, z3, sprite.getU1(), sprite.getV0(), -normalX, -normalY, -normalZ);
		corner(consumer, pose, sprite, tint, light, x2, y2, z2, sprite.getU1(), sprite.getV1(), -normalX, -normalY, -normalZ);
		corner(consumer, pose, sprite, tint, light, x1, y1, z1, sprite.getU0(), sprite.getV1(), -normalX, -normalY, -normalZ);
		corner(consumer, pose, sprite, tint, light, x0, y0, z0, sprite.getU0(), sprite.getV0(), -normalX, -normalY, -normalZ);
	}

	private static void corner(VertexConsumer consumer, Matrix4f pose, TextureAtlasSprite sprite,
			int tint, int light, float x, float y, float z, float u, float v,
			float normalX, float normalY, float normalZ) {
		consumer.addVertex(pose, x, y, z)
				.setColor((tint >> 16) & 0xFF, (tint >> 8) & 0xFF, tint & 0xFF, ALPHA)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(normalX, normalY, normalZ);
	}
}
