package dev.fimkov.echoesbeneath.client.mixin;

import dev.fimkov.echoesbeneath.client.render.EchoDarkness;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SingleQuadParticle.class)
public abstract class ParticleEmissiveMixin {
	@Redirect(
			method = "renderRotatedQuad(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Quaternionf;FFFF)V",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/particle/SingleQuadParticle;getLightColor(F)I"))
	private int echoesBeneath$keepParticlesLit(SingleQuadParticle particle, float partialTick) {
		if (EchoDarkness.isTransitionActive()) {
			return LightTexture.FULL_BRIGHT;
		}
		return particle.getLightColor(partialTick);
	}
}
