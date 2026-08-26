package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.fimkov.echoesbeneath.blockentity.EchoChartBlockEntity;
import dev.fimkov.echoesbeneath.chart.EchoChartLook;
import dev.fimkov.echoesbeneath.client.chart.ClientChartShapes;
import dev.fimkov.echoesbeneath.client.chart.EchoChartModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;

public class EchoChartBlockRenderer implements BlockEntityRenderer<EchoChartBlockEntity> {
	private static final double MODEL_REST = 0.55D;
	private static final float BOB_PERIOD_TICKS = 120.0F;
	private static final double BOB_HEIGHT = 0.045D;
	private static final float DEGREES_PER_STEP = 360.0F / EchoChartBlockEntity.TURN_STEPS;

	public EchoChartBlockRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(EchoChartBlockEntity chart, float partialTick, PoseStack poseStack,
			MultiBufferSource consumers, int light, int overlay) {
		EchoChartModel model = ClientChartShapes.of(chart.getBlockPos().asLong());
		if (model == null) {
			ClientChartShapes.ask(chart.getBlockPos());
			return;
		}
		if (model.size() == 0) {
			return;
		}
		EchoChartLook look = chart.look();
		float age = chart.age(partialTick);
		poseStack.pushPose();
		poseStack.translate(0.5D, 0.0D, 0.5D);
		if (look.runs() || look.motes()) {
			EchoChartAura.render(poseStack, consumers, age, look.motes(), look.runs(),
					Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation(),
					model.brightness());
		}
		double lift = MODEL_REST + (look.drifts() ? bob(age) : 0.0D);
		poseStack.translate(0.0D, lift, 0.0D);
		poseStack.mulPose(Axis.YP.rotationDegrees(
				chart.turn() * DEGREES_PER_STEP + age * look.spinDegreesPerTick()));
		float scale = look.modelScale() / model.widest();
		poseStack.scale(scale, scale, scale);
		poseStack.translate(-model.spanX() * 0.5F, -model.spanY() * 0.5F, -model.spanZ() * 0.5F);
		EchoChartGeometry.emit(model, consumers, poseStack);
		poseStack.popPose();
	}

	private static double bob(float age) {
		return Mth.sin(age / BOB_PERIOD_TICKS * Mth.TWO_PI) * BOB_HEIGHT;
	}

	@Override
	public boolean shouldRenderOffScreen(EchoChartBlockEntity chart) {
		return true;
	}
}
