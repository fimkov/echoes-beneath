package dev.fimkov.echoesbeneath.client.render;

public interface EchoLightTextureAccess {
	void echoesBeneath$applyDarkness(float darkness, float partialTick);

	void echoesBeneath$restoreNaturalLight(float partialTick);
}
