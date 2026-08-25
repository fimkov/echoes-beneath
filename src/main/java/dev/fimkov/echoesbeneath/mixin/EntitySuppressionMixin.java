package dev.fimkov.echoesbeneath.mixin;

import dev.fimkov.echoesbeneath.playback.suppression.EchoSuppressedEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntitySuppressionMixin implements EchoSuppressedEntity {
	@Unique
	private boolean echoesBeneath$suppressed;

	@Override
	public boolean echoesBeneath$isSuppressed() {
		return echoesBeneath$suppressed;
	}

	@Override
	public void echoesBeneath$setSuppressed(boolean suppressed) {
		this.echoesBeneath$suppressed = suppressed;
	}

	@Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
	private void echoesBeneath$blockPushWhileSuppressed(CallbackInfoReturnable<Boolean> callback) {
		if (echoesBeneath$suppressed) {
			callback.setReturnValue(false);
		}
	}

	@Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
	private void echoesBeneath$blockPickWhileSuppressed(CallbackInfoReturnable<Boolean> callback) {
		if (echoesBeneath$suppressed) {
			callback.setReturnValue(false);
		}
	}
}
