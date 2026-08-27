package dev.fimkov.echoesbeneath.mixin;

import dev.fimkov.echoesbeneath.recording.EchoRecordingHooks;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelParticleMixin {
	@Inject(method = "sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I",
			at = @At("HEAD"))
	private <T extends ParticleOptions> void echoesBeneath$recordParticles(T options, double x, double y,
			double z, int count, double spreadX, double spreadY, double spreadZ, double speed,
			CallbackInfoReturnable<Integer> callback) {
		if (!EchoRecordingHooks.isCapturing() || !(options instanceof SimpleParticleType)) {
			return;
		}

		Identifier typeId = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());

		if (typeId == null) {
			return;
		}

		EchoRecordingHooks.onParticle((ServerLevel) (Object) this, x, y, z, typeId, count,
				spreadX, spreadY, spreadZ);
	}
}
