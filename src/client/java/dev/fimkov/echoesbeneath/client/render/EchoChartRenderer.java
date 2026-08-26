package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.fimkov.echoesbeneath.client.chart.ClientChartShapes;
import dev.fimkov.echoesbeneath.client.chart.EchoChartModel;
import dev.fimkov.echoesbeneath.entity.EchoChartEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class EchoChartRenderer extends EntityRenderer<EchoChartEntity> {
	private static final ResourceLocation TEXTURE =
			ResourceLocation.withDefaultNamespace("textures/misc/white.png");
	private static final float MODEL_SIZE = 0.66F;
	private static final double MODEL_REST = 0.44D;
	private static final float BASE_WIDTH = 0.74F;
	private static final float BASE_HEIGHT = 0.09F;
	private static final float STEM_WIDTH = 0.34F;
	private static final float STEM_HEIGHT = 0.20F;
	private static final float CAP_WIDTH = 0.56F;
	private static final float CAP_HEIGHT = 0.07F;
	private static final int PLINTH_LIGHT = LightTexture.pack(6, 0);

	public EchoChartRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EchoChartEntity entity) {
		return TEXTURE;
	}

	private static void renderPlinth(PoseStack poseStack, MultiBufferSource consumers) {
		slab(poseStack, consumers, Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
				0.0F, BASE_WIDTH, BASE_HEIGHT);
		slab(poseStack, consumers, Blocks.DEEPSLATE_TILES.defaultBlockState(),
				BASE_HEIGHT, STEM_WIDTH, STEM_HEIGHT);
		slab(poseStack, consumers, Blocks.CHISELED_DEEPSLATE.defaultBlockState(),
				BASE_HEIGHT + STEM_HEIGHT, CAP_WIDTH, CAP_HEIGHT);
	}

	private static void slab(PoseStack poseStack, MultiBufferSource consumers, BlockState state,
			float bottom, float width, float height) {
		poseStack.pushPose();
		poseStack.translate(0.0F, bottom, 0.0F);
		poseStack.scale(width, height, width);
		poseStack.translate(-0.5F, 0.0F, -0.5F);
		Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, consumers,
				PLINTH_LIGHT, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
	}

	@Override
	public void render(EchoChartEntity entity, float yaw, float partialTick, PoseStack poseStack,
			MultiBufferSource consumers, int light) {
		EchoChartModel model = ClientChartShapes.of(entity.getId());
		if (model == null || model.size() == 0) {
			return;
		}
		renderPlinth(poseStack, consumers);
		poseStack.pushPose();
		poseStack.translate(0.0D, MODEL_REST + entity.bob(partialTick), 0.0D);
		poseStack.mulPose(Axis.YP.rotationDegrees(entity.spin(partialTick)));
		float scale = MODEL_SIZE / model.widest();
		poseStack.scale(scale, scale, scale);
		poseStack.translate(-model.spanX() * 0.5F, -model.spanY() * 0.5F, -model.spanZ() * 0.5F);

		EchoChartGeometry.emit(model, consumers, poseStack);
		poseStack.popPose();
		super.render(entity, yaw, partialTick, poseStack, consumers, light);
	}

}
