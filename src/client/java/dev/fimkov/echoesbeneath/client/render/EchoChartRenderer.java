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
import org.joml.Matrix4f;

public class EchoChartRenderer extends EntityRenderer<EchoChartEntity> {
	private static final ResourceLocation TEXTURE =
			ResourceLocation.withDefaultNamespace("textures/misc/white.png");
	private static final float MODEL_SIZE = 0.85F;
	private static final float TILT_DEGREES = 12.0F;

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
		poseStack.pushPose();
		poseStack.translate(0.0D, entity.bob(partialTick), 0.0D);
		poseStack.mulPose(Axis.YP.rotationDegrees(entity.spin(partialTick)));
		poseStack.mulPose(Axis.XP.rotationDegrees(TILT_DEGREES));
		float scale = MODEL_SIZE / model.widest();
		poseStack.scale(scale, scale, scale);
		poseStack.translate(-model.spanX() * 0.5F, 0.0F, -model.spanZ() * 0.5F);

		EchoChartGeometry.emit(model, consumers, poseStack.last().pose());
		poseStack.popPose();
		super.render(entity, yaw, partialTick, poseStack, consumers, light);
	}

}
