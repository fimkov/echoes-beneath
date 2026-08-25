package dev.fimkov.echoesbeneath.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import dev.fimkov.echoesbeneath.client.render.EchoDarkness;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

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
		Optional<ClientPlaybackSession> session = EchoDarkness.enclosingSession();
		if (session.isEmpty()) {
			return;
		}
		RenderSystem.setShaderFogStart(EchoDarkness.fogStartFor(session.get()));
		RenderSystem.setShaderFogEnd(EchoDarkness.fogEndFor(session.get()));
		RenderSystem.setShaderFogShape(com.mojang.blaze3d.shaders.FogShape.SPHERE);
	}

	@Inject(method = "setupColor", at = @At("TAIL"))
	private static void echoesBeneath$blackenFogColor(Camera camera, float partialTick, ClientLevel level,
			int renderDistanceChunks, float darkenWorldAmount, CallbackInfo callback) {
		if (!EchoDarkness.isViewerInsideRegion()) {
			return;
		}
		fogRed = 0.0F;
		fogGreen = 0.0F;
		fogBlue = 0.0F;
		RenderSystem.setShaderFogColor(0.0F, 0.0F, 0.0F);
		RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
	}
}
