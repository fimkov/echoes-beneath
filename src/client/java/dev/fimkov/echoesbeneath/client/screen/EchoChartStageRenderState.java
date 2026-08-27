package dev.fimkov.echoesbeneath.client.screen;

import dev.fimkov.echoesbeneath.client.chart.EchoChartModel;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

import org.jetbrains.annotations.Nullable;

// The stage is a window onto a small world, which is what picture in picture is
// for: the chart is drawn into its own texture and blitted into the panel.
public record EchoChartStageRenderState(EchoChartModel model, int x0, int x1, int y0, int y1,
		float scale, float pitch, float yaw, @Nullable ScreenRectangle scissorArea,
		@Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
	public EchoChartStageRenderState(EchoChartModel model, int x0, int x1, int y0, int y1,
			float scale, float pitch, float yaw, @Nullable ScreenRectangle scissorArea) {
		this(model, x0, x1, y0, y1, scale, pitch, yaw, scissorArea,
				PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
	}
}
