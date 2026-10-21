package dev.fimkov.echoesbeneath.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.fimkov.echoesbeneath.client.sound.EchoOffsetAudioStream;
import dev.fimkov.echoesbeneath.client.sound.EchoRecordSoundInstance;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SoundEngine.class)
public abstract class RecordAudioOffsetMixin {
	@ModifyExpressionValue(method = "play", at = {
			@At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundBufferLibrary;getStream(Lnet/minecraft/resources/Identifier;Z)Ljava/util/concurrent/CompletableFuture;"),
			@At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getStream(Lnet/minecraft/client/sounds/SoundBufferLibrary;Lnet/minecraft/client/resources/sounds/Sound;Z)Ljava/util/concurrent/CompletableFuture;")
	})
	private CompletableFuture<AudioStream> echoesBeneath$trimAudio(CompletableFuture<AudioStream> original,
			SoundInstance sound) {
		if (sound instanceof EchoRecordSoundInstance record && record.offsetTicks() > 0) {
			return original.thenApply(stream -> new EchoOffsetAudioStream(stream, record.offsetTicks(), record.getPitch()));
		}
		return original;
	}
}
