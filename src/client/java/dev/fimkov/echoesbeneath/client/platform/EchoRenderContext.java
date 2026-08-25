package dev.fimkov.echoesbeneath.client.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;

public interface EchoRenderContext {
	PoseStack matrixStack();

	MultiBufferSource consumers();

	DeltaTracker tickCounter();

	Frustum frustum();

	Camera camera();

	ClientLevel world();
}
