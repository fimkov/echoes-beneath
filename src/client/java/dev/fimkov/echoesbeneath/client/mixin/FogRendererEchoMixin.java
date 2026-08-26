package dev.fimkov.echoesbeneath.client.mixin;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.fimkov.echoesbeneath.client.render.EchoDarkness;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogRendererEchoMixin {
	@Shadow
	private static float fogRed;

	@Shadow
	private static float fogGreen;

	@Shadow
	private static float fogBlue;

	@Inject(method = "setupFog", at = @At("TAIL"))
	private static void echoesBeneath$tightenFog(Camera camera, FogRenderer.FogMode fogMode,
			float renderDistance, boolean thickFog, float partialTick, CallbackInfo callback) {
		float darkness = EchoDarkness.darkness();

		if (darkness <= 0.0F) {
			return;
		}

		RenderSystem.setShaderFogStart(blend(RenderSystem.getShaderFogStart(),
				EchoDarkness.regionFogStart(), darkness));
		RenderSystem.setShaderFogEnd(blend(RenderSystem.getShaderFogEnd(),
				EchoDarkness.regionFogEnd(), darkness));
		RenderSystem.setShaderFogShape(FogShape.SPHERE);
	}

	@Inject(method = "setupColor", at = @At("TAIL"))
	private static void echoesBeneath$blackenFogColor(Camera camera, float partialTick, ClientLevel level,
			int renderDistanceChunks, float darkenWorldAmount, CallbackInfo callback) {
		float darkness = EchoDarkness.darkness();

		if (darkness <= 0.0F) {
			return;
		}

		float retained = 1.0F - darkness;
		fogRed *= retained;
		fogGreen *= retained;
		fogBlue *= retained;
		RenderSystem.setShaderFogColor(fogRed, fogGreen, fogBlue);
		RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 1.0F);
	}

	private static float blend(float from, float to, float amount) {
		return from + (to - from) * amount;
	}
}
