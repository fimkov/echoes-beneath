package dev.fimkov.echoesbeneath.client.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public interface EchoRenderContext {
	PoseStack matrixStack();

	SubmitNodeCollector submits();

	DeltaTracker tickCounter();

	CameraRenderState camera();

	ClientLevel world();
}
