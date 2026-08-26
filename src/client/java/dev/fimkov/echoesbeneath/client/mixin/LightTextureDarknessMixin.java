package dev.fimkov.echoesbeneath.client.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import dev.fimkov.echoesbeneath.client.render.EchoDarkness;
import dev.fimkov.echoesbeneath.client.render.EchoLightTextureAccess;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LightTexture.class)
public abstract class LightTextureDarknessMixin implements EchoLightTextureAccess {
	private static final int LIGHT_LEVELS = 16;
	private static final int CHANNEL_MASK = 0xFF;
	private static final int ALPHA_MASK = 0xFF000000;

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
	public void echoesBeneath$applyDarkness(float darkness, float partialTick) {
		echoesBeneath$restoreNaturalLight(partialTick);
		float retained = 1.0F - Math.clamp(darkness, 0.0F, 1.0F);

		for (int skyLevel = 0; skyLevel < LIGHT_LEVELS; skyLevel++) {
			for (int blockLevel = 0; blockLevel < LIGHT_LEVELS; blockLevel++) {
				if (blockLevel == EchoDarkness.EMISSIVE_TEXEL && skyLevel == EchoDarkness.EMISSIVE_TEXEL) {
					continue;
				}
				lightPixels.setPixelRGBA(blockLevel, skyLevel,
						dimmed(lightPixels.getPixelRGBA(blockLevel, skyLevel), retained));
			}
		}

		lightTexture.upload();
	}

	private static int dimmed(int packed, float retained) {
		int first = Math.round((packed & CHANNEL_MASK) * retained);
		int second = Math.round(((packed >> 8) & CHANNEL_MASK) * retained);
		int third = Math.round(((packed >> 16) & CHANNEL_MASK) * retained);

		return (packed & ALPHA_MASK) | (third << 16) | (second << 8) | first;
	}

	@Override
	public void echoesBeneath$restoreNaturalLight(float partialTick) {
		this.updateLightTexture = true;
		updateLightTexture(partialTick);
	}
}
