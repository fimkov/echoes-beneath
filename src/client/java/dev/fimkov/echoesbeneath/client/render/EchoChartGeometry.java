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
			// Submissions are collected and drawn later, so every cell needs a state
			// of its own rather than a shared one that keeps being overwritten.
			BlockModelRenderState modelState = new BlockModelRenderState();
			blocks.update(modelState, model.stateAt(index), BlockDisplayContext.create());
			modelState.submit(poseStack, submits, light, OverlayTexture.NO_OVERLAY, 0);
			EchoChartFluid.emit(model, submits, poseStack, model.stateAt(index), cell, light);
			poseStack.popPose();
			drawn++;
		}
	}
}
