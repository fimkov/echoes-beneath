package dev.fimkov.echoesbeneath.mixin;

import dev.fimkov.echoesbeneath.playback.suppression.EchoSuppressedEntity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySuppressionMixin {
	@Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
	private void echoesBeneath$blockPushWhileSuppressed(CallbackInfoReturnable<Boolean> callback) {
		if (((EchoSuppressedEntity) this).echoesBeneath$isSuppressed()) {
			callback.setReturnValue(false);
		}
	}

	@Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
	private void echoesBeneath$blockPickWhileSuppressed(CallbackInfoReturnable<Boolean> callback) {
		if (((EchoSuppressedEntity) this).echoesBeneath$isSuppressed()) {
			callback.setReturnValue(false);
		}
	}
}
