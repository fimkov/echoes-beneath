package dev.fimkov.echoesbeneath.client.mixin;

import dev.fimkov.echoesbeneath.client.render.EchoDarkness;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The lightmap lives on the gpu now, so the memory darkens the state the lightmap
// is built from rather than the finished pixels.
@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightTextureDarknessMixin {
	@Inject(method = "extract", at = @At("TAIL"))
	private void echoesBeneath$applyDarkness(LightmapRenderState state, float partialTick,
			CallbackInfo callback) {
		float darkness = EchoDarkness.darkness();

		if (darkness <= 0.0F) {
			return;
		}

		float retained = 1.0F - Math.clamp(darkness, 0.0F, 1.0F);
		state.blockFactor *= retained;
		state.skyFactor *= retained;
		state.brightness *= retained;
		state.needsUpdate = true;
	}
}
