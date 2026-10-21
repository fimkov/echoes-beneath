package dev.fimkov.echoesbeneath.mixin;

import net.minecraft.world.entity.Entity;
import dev.fimkov.echoesbeneath.recording.EchoRecordingHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelEventMixin {
	private static final int BLOCK_DESTROYED = 2001;
	private static final float BREAK_VOLUME_SHARE = 0.8F;
	private static final float BREAK_PITCH_SHARE = 0.9F;

	@Inject(method = "levelEvent(Lnet/minecraft/world/entity/Entity;ILnet/minecraft/core/BlockPos;I)V",
			at = @At("HEAD"))
	private void echoesBeneath$recordLevelEvent(Entity except, int type, BlockPos position, int data,
			CallbackInfo callback) {
		if (!EchoRecordingHooks.isCapturing()) {
			return;
		}
		ServerLevel level = (ServerLevel) (Object) this;
		if (type == LevelEvent.SOUND_PLAY_JUKEBOX_SONG) {
			var song = level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).byId(data);
			if (song != null && !song.soundEvent().value().location()
					.equals(Identifier.withDefaultNamespace("intentionally_empty"))) {
				EchoRecordingHooks.onJukebox(level, position, song.soundEvent().value().location(), true);
			}
			return;
		}
		if (type == LevelEvent.SOUND_STOP_JUKEBOX_SONG) {
			EchoRecordingHooks.onJukebox(level, position, Identifier.withDefaultNamespace("intentionally_empty"), false);
			return;
		}
		if (type != BLOCK_DESTROYED) {
			return;
		}

		BlockState broken = Block.stateById(data);

		if (broken.isAir()) {
			return;
		}

		SoundType sounds = broken.getSoundType();
		SoundEvent event = sounds.getBreakSound();
		var id = BuiltInRegistries.SOUND_EVENT.getKey(event);

		if (id == null) {
			return;
		}

		EchoRecordingHooks.onSound((ServerLevel) (Object) this,
				position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D,
				id, SoundSource.BLOCKS, sounds.getVolume() * BREAK_VOLUME_SHARE,
				sounds.getPitch() * BREAK_PITCH_SHARE);
	}
}
