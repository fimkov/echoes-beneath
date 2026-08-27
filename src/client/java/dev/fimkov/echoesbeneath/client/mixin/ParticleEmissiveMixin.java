package dev.fimkov.echoesbeneath.client.mixin;

import dev.fimkov.echoesbeneath.client.render.EchoDarkness;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.LightCoordsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Particles are the one thing that keeps its own light while the memory is dark,
// otherwise a replay swallows them whole.
@Mixin(Particle.class)
public abstract class ParticleEmissiveMixin {
	@Inject(method = "getLightCoords", at = @At("HEAD"), cancellable = true)
	private void echoesBeneath$keepParticlesLit(float partialTick, CallbackInfoReturnable<Integer> callback) {
		if (EchoDarkness.isTransitionActive()) {
			callback.setReturnValue(LightCoordsUtil.FULL_BRIGHT);
		}
	}
}
