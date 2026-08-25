package dev.fimkov.echoesbeneath.client.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import dev.fimkov.echoesbeneath.client.render.EchoDarkness;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public abstract class LightTextureDarknessMixin {
	@Shadow
	@Final
	private NativeImage lightPixels;

	@Shadow
	@Final
	private DynamicTexture lightTexture;

	@Inject(method = "updateLightTexture", at = @At("TAIL"))
	private void echoesBeneath$dimInsideRegion(float partialTick, CallbackInfo callback) {
		if (!EchoDarkness.isViewerInsideRegion()) {
			return;
		}
		for (int skyLevel = 0; skyLevel < 16; skyLevel++) {
			for (int blockLevel = 0; blockLevel < 16; blockLevel++) {
				boolean interfaceTexel = blockLevel == EchoDarkness.FULL_BRIGHT_TEXEL
						&& skyLevel == EchoDarkness.FULL_BRIGHT_TEXEL;
				if (interfaceTexel) {
					continue;
				}
				lightPixels.setPixelRGBA(blockLevel, skyLevel, EchoDarkness.SILHOUETTE_LIGHT_PIXEL);
			}
		}
		lightTexture.upload();
	}
}
