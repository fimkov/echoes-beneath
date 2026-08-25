package dev.fimkov.echoesbeneath.mixin;

import dev.fimkov.echoesbeneath.recording.EchoRecordingHooks;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelSoundMixin {
	@Inject(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
			at = @At("HEAD"))
	private void echoesBeneath$recordPositionedSound(Player except, double x, double y, double z,
			Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed, CallbackInfo callback) {
		if (!EchoRecordingHooks.isCapturing()) {
			return;
		}
		EchoRecordingHooks.onSound((ServerLevel) (Object) this, x, y, z,
				sound.value().getLocation(), source, volume, pitch);
	}

	@Inject(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
			at = @At("HEAD"))
	private void echoesBeneath$recordEntitySound(Player except, Entity source, Holder<SoundEvent> sound,
			SoundSource category, float volume, float pitch, long seed, CallbackInfo callback) {
		if (!EchoRecordingHooks.isCapturing()) {
			return;
		}
		EchoRecordingHooks.onSound((ServerLevel) (Object) this, source.getX(), source.getY(), source.getZ(),
				sound.value().getLocation(), category, volume, pitch);
	}
}
