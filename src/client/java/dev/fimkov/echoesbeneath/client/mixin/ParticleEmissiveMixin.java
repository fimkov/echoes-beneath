package dev.fimkov.echoesbeneath.client.mixin;

import dev.fimkov.echoesbeneath.client.render.EchoDarkness;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Particle.class)
public abstract class ParticleEmissiveMixin {
	@Inject(method = "getLightColor", at = @At("HEAD"), cancellable = true)
	private void echoesBeneath$keepParticlesLit(float partialTick, CallbackInfoReturnable<Integer> callback) {
		if (EchoDarkness.isViewerInsideRegion()) {
			callback.setReturnValue(LightTexture.FULL_BRIGHT);
		}
	}
}
