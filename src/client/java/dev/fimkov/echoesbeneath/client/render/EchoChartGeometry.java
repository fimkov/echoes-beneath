package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fimkov.echoesbeneath.client.chart.EchoChartModel;
import dev.fimkov.echoesbeneath.client.config.EchoClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class EchoChartGeometry {
	private static final int FULL_BLOCK_LIGHT = 15;

	private EchoChartGeometry() {
	}

	public static void emit(EchoChartModel model, SubmitNodeCollector submits, PoseStack poseStack) {
		BlockModelResolver blocks = new BlockModelResolver(Minecraft.getInstance().getModelManager());
		BlockModelRenderState modelState = new BlockModelRenderState();
		int light = LightCoordsUtil.pack(Math.round(FULL_BLOCK_LIGHT * model.brightness()), 0);
		int budget = EchoClientConfig.values().chartBlockBudget();
		int drawn = 0;

		for (int index = 0; index < model.size() && drawn < budget; index++) {
			int cell = model.cellAt(index);

			if (model.isBuried(cell)) {
				continue;
			}
			poseStack.pushPose();
			poseStack.translate(EchoChartModel.cellX(cell), EchoChartModel.cellY(cell),
					EchoChartModel.cellZ(cell));
			blocks.update(modelState, model.stateAt(index), BlockDisplayContext.create());
			modelState.submit(poseStack, submits, light, OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
			drawn++;
		}
	}
}
