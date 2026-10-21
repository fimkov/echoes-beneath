package dev.fimkov.echoesbeneath.client.mixin;

import dev.fimkov.echoesbeneath.client.render.EchoDarkness;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelAmbienceMixin {
	@Inject(method = "animateTick", at = @At("HEAD"), cancellable = true)
	private void echoesBeneath$holdBackAmbience(int x, int y, int z, CallbackInfo callback) {
		if (EchoDarkness.isViewerInsideRegion()) {
			callback.cancel();
		}
	}
}
