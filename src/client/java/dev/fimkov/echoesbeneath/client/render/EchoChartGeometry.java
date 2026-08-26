package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fimkov.echoesbeneath.client.chart.EchoChartModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class EchoChartGeometry {
	private static final int FULL_BLOCK_LIGHT = 15;

	private EchoChartGeometry() {
	}

	public static void emit(EchoChartModel model, MultiBufferSource consumers, PoseStack poseStack) {
		BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
		int light = LightTexture.pack(Math.round(FULL_BLOCK_LIGHT * model.brightness()), 0);
		int budget = dev.fimkov.echoesbeneath.client.config.EchoClientConfig.values().chartBlockBudget();
		int drawn = 0;
		for (int index = 0; index < model.size() && drawn < budget; index++) {
			int cell = model.cellAt(index);
			if (model.isBuried(cell)) {
				continue;
			}
			poseStack.pushPose();
			poseStack.translate(EchoChartModel.cellX(cell), EchoChartModel.cellY(cell),
					EchoChartModel.cellZ(cell));
			blocks.renderSingleBlock(model.stateAt(index), poseStack, consumers, light,
					OverlayTexture.NO_OVERLAY);
			poseStack.popPose();
			drawn++;
		}
	}
}
