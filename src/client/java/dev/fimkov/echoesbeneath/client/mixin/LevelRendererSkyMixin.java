package dev.fimkov.echoesbeneath.client.mixin;

import dev.fimkov.echoesbeneath.client.render.EchoDarkness;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The sky is extracted into a state before it is drawn, so a memory blanks that
// state rather than cancelling the draw.
@Mixin(SkyRenderer.class)
public abstract class LevelRendererSkyMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void echoesBeneath$hideSkyInsideRegion(net.minecraft.client.multiplayer.ClientLevel level,
			float partialTick, net.minecraft.client.Camera camera, SkyRenderState state,
			CallbackInfo callback) {
		if (!EchoDarkness.shouldHideSky()) {
			return;
		}

		state.skybox = DimensionType.Skybox.NONE;
		state.shouldRenderDarkDisc = true;
		state.starBrightness = 0.0F;
		state.rainBrightness = 0.0F;
		state.skyColor = 0;
		state.sunriseAndSunsetColor = 0;
		state.endFlashIntensity = 0.0F;
	}
}
