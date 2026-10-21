package dev.fimkov.echoesbeneath.mixin;

import dev.fimkov.echoesbeneath.runtime.EchoServerState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxEchoRecordMixin {
	@Inject(method = "setTheItem", at = @At("TAIL"))
	private void echoesBeneath$insertRecord(ItemStack stack, CallbackInfo callback) {
		EchoServerState state = EchoServerState.current();
		if (state != null) {
			state.records().jukeboxChanged((JukeboxBlockEntity) (Object) this);
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private static void echoesBeneath$resumeRecord(Level level, BlockPos position, BlockState block,
			JukeboxBlockEntity box, CallbackInfo callback) {
		EchoServerState state = EchoServerState.current();
		if (state != null && level instanceof ServerLevel) {
			state.records().observeJukebox(box);
		}
	}
}
