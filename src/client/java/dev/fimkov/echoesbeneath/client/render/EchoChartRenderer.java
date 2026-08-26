package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.fimkov.echoesbeneath.client.chart.ClientChartShapes;
import dev.fimkov.echoesbeneath.client.chart.EchoChartModel;
import dev.fimkov.echoesbeneath.entity.EchoChartEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class EchoChartRenderer extends EntityRenderer<EchoChartEntity> {
	private static final ResourceLocation TEXTURE =
			ResourceLocation.withDefaultNamespace("textures/misc/white.png");
	private static final float MODEL_SIZE = 0.62F;
	private static final double MODEL_REST = 0.5D;

	public EchoChartRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EchoChartEntity entity) {
		return TEXTURE;
	}

	@Override
	public void render(EchoChartEntity entity, float yaw, float partialTick, PoseStack poseStack,
			MultiBufferSource consumers, int light) {
		EchoChartModel model = ClientChartShapes.of(entity.getId());
		if (model == null || model.size() == 0) {
			return;
		}
		EchoChartAura.render(poseStack, consumers, entity.tickCount + partialTick,
				entityRenderDispatcher.cameraOrientation(), model.brightness());
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
