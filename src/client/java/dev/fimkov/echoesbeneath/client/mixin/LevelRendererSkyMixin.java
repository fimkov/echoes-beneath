package dev.fimkov.echoesbeneath.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fimkov.echoesbeneath.client.render.EchoDarkness;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererSkyMixin {
	@Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
	private void echoesBeneath$hideSkyInsideRegion(Matrix4f frustumMatrix, Matrix4f projectionMatrix,
			float partialTick, Camera camera, boolean isFoggy, Runnable skyFogSetup, CallbackInfo callback) {
		if (EchoDarkness.shouldHideSky()) {
			callback.cancel();
		}
	}

	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	private void echoesBeneath$hideCloudsInsideRegion(PoseStack poseStack,
			Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick,
			double cameraX, double cameraY, double cameraZ, CallbackInfo callback) {
		if (EchoDarkness.shouldHideSky()) {
			callback.cancel();
		}
	}
}
