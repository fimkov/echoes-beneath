package dev.fimkov.echoesbeneath.client.mixin;

import dev.fimkov.echoesbeneath.client.render.EchoDarkness;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Fog is one set of values handed to the shader now, so pulling it in and
// draining its colour both happen in the same place.
@Mixin(FogRenderer.class)
public abstract class FogRendererEchoMixin {
	@Inject(method = "setupFog", at = @At("RETURN"))
	private void echoesBeneath$tightenFog(Camera camera, int renderDistance, DeltaTracker deltaTracker,
			float darkenWorldAmount, ClientLevel level, CallbackInfoReturnable<FogData> callback) {
		float darkness = EchoDarkness.darkness();

		if (darkness <= 0.0F) {
			return;
		}

		FogData data = callback.getReturnValue();
		float start = EchoDarkness.regionFogStart();
		float end = EchoDarkness.regionFogEnd();
		data.environmentalStart = blend(data.environmentalStart, start, darkness);
		data.environmentalEnd = blend(data.environmentalEnd, end, darkness);
		data.renderDistanceStart = blend(data.renderDistanceStart, start, darkness);
		data.renderDistanceEnd = blend(data.renderDistanceEnd, end, darkness);
		data.skyEnd = blend(data.skyEnd, end, darkness);
		data.cloudEnd = blend(data.cloudEnd, end, darkness);

		float retained = 1.0F - darkness;
		data.color.mul(retained, retained, retained, 1.0F);
	}

	private static float blend(float from, float to, float amount) {
		return from + (to - from) * amount;
	}
}
