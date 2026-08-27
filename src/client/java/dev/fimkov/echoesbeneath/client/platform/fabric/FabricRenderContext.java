package dev.fimkov.echoesbeneath.client.platform.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fimkov.echoesbeneath.client.platform.EchoRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public record FabricRenderContext(LevelRenderContext delegate) implements EchoRenderContext {
	@Override
	public PoseStack matrixStack() {
		return delegate.poseStack();
	}

	@Override
	public SubmitNodeCollector submits() {
		return delegate.submitNodeCollector();
	}

	@Override
	public DeltaTracker tickCounter() {
		return Minecraft.getInstance().getDeltaTracker();
	}

	@Override
	public CameraRenderState camera() {
		return delegate.levelState().cameraRenderState;
	}

	@Override
	public ClientLevel world() {
		return Minecraft.getInstance().level;
	}
}
