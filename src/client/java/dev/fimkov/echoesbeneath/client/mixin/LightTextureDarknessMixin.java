package dev.fimkov.echoesbeneath.client.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import dev.fimkov.echoesbeneath.client.render.EchoDarkness;
import dev.fimkov.echoesbeneath.client.render.EchoLightTextureAccess;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public abstract class LightTextureDarknessMixin implements EchoLightTextureAccess {
	private static final int LIGHT_LEVELS = 16;

	@Shadow
	@Final
	private NativeImage lightPixels;

	@Shadow
	@Final
	private DynamicTexture lightTexture;

	@Shadow
	private boolean updateLightTexture;

	@Shadow
	public abstract void updateLightTexture(float partialTick);

	@Override
	public void echoesBeneath$restoreNaturalLight(float partialTick) {
		this.updateLightTexture = true;
		updateLightTexture(partialTick);
	}

	@Inject(method = "updateLightTexture", at = @At("TAIL"))
	private void echoesBeneath$voidLightInsideRegion(float partialTick, CallbackInfo callback) {
		if (!EchoDarkness.shouldDarken()) {
			return;
		}
		for (int skyLevel = 0; skyLevel < LIGHT_LEVELS; skyLevel++) {
			for (int blockLevel = 0; blockLevel < LIGHT_LEVELS; blockLevel++) {
				lightPixels.setPixelRGBA(blockLevel, skyLevel, EchoDarkness.VOID_LIGHT_PIXEL);
			}
		}
		lightTexture.upload();
	}
}
