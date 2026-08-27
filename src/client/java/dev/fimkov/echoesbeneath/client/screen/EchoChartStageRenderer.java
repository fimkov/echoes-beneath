package dev.fimkov.echoesbeneath.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Minecraft;
import com.mojang.math.Axis;
import dev.fimkov.echoesbeneath.client.chart.EchoChartModel;
import dev.fimkov.echoesbeneath.client.render.EchoChartGeometry;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;

public class EchoChartStageRenderer extends PictureInPictureRenderer<EchoChartStageRenderState> {
	@Override
	public Class<EchoChartStageRenderState> getRenderStateClass() {
		return EchoChartStageRenderState.class;
	}

	@Override
	protected String getTextureLabel() {
		return "echoes_beneath chart stage";
	}

	@Override
	protected void renderToTexture(EchoChartStageRenderState state, PoseStack poseStack,
			SubmitNodeCollector collector) {
		// Without a lighting entry the blocks come out unlit, which in this panel
		// means invisible.
		Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
		EchoChartModel model = state.model();
		poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch()));
		poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw()));
		poseStack.translate(-model.spanX() * 0.5F, -model.spanY() * 0.5F, -model.spanZ() * 0.5F);
		EchoChartGeometry.emit(model, collector, poseStack);
	}
}
