package dev.fimkov.echoesbeneath.client.mixin;

import dev.fimkov.echoesbeneath.client.render.EchoDarkness;
import org.joml.Vector3f;
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

		// Sky and ambient go, block light stays. That keeps the lens, the chart and
		// the boundary particles alight while the world itself falls dark, which is
		// what the old single bright texel used to do.
		float retained = 1.0F - Math.clamp(darkness, 0.0F, 1.0F);
		state.skyFactor *= retained;
		state.ambientColor = state.ambientColor.mul(retained, new Vector3f());
		// Underground there is no sky and no ambient to take, and the player's own
		// brightness pulls what is left back into view. Night vision would undo the
		// whole thing on its own.
		state.brightness *= retained;
		state.nightVisionEffectIntensity *= retained;
		state.needsUpdate = true;
	}
}
