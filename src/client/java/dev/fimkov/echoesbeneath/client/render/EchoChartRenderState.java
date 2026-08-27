package dev.fimkov.echoesbeneath.client.render;

import dev.fimkov.echoesbeneath.chart.EchoChartLook;
import dev.fimkov.echoesbeneath.client.chart.EchoChartModel;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

import org.jetbrains.annotations.Nullable;

public class EchoChartRenderState extends BlockEntityRenderState {
	public boolean drawn;

	@Nullable
	public EchoChartModel model;

	public EchoChartLook look = EchoChartLook.DEFAULT;
	public float age;
	public int turn;
}
