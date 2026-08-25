package dev.fimkov.echoesbeneath.mixin;

import dev.fimkov.echoesbeneath.recording.EchoRecordingHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkBlockChangeMixin {
	@Shadow
	public abstract Level getLevel();

	@Inject(method = "setBlockState", at = @At("RETURN"))
	private void echoesBeneath$recordBlockChange(BlockPos position, BlockState state, boolean isMoving,
			CallbackInfoReturnable<BlockState> callback) {
		if (!EchoRecordingHooks.isCapturing()) {
			return;
		}
		BlockState previous = callback.getReturnValue();
		if (previous == null || previous == state) {
			return;
		}
		EchoRecordingHooks.onBlockChanged(getLevel(), position, previous, state);
	}
}
