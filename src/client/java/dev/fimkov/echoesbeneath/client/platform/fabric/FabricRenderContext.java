package dev.fimkov.echoesbeneath.client.platform.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fimkov.echoesbeneath.client.platform.EchoRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;

public record FabricRenderContext(WorldRenderContext delegate) implements EchoRenderContext {
	@Override
	public PoseStack matrixStack() {
		return delegate.matrixStack();
	}

	@Override
	public MultiBufferSource consumers() {
		return delegate.consumers();
	}

	@Override
	public DeltaTracker tickCounter() {
		return delegate.tickCounter();
	}

	@Override
	public Frustum frustum() {
		return delegate.frustum();
	}

	@Override
	public Camera camera() {
		return delegate.camera();
	}

	@Override
	public ClientLevel world() {
		return delegate.world();
	}
}
