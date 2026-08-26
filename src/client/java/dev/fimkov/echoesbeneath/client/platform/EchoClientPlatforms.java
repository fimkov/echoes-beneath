package dev.fimkov.echoesbeneath.client.platform;

public final class EchoClientPlatforms {
	private static volatile EchoClientHooks current;

	private EchoClientPlatforms() {
	}

	public static void install(EchoClientHooks hooks) {
		current = hooks;
	}

	public static EchoClientHooks get() {
		EchoClientHooks hooks = current;
		if (hooks == null) {
			throw new IllegalStateException("No client loader hooks have been installed");
		}
		return hooks;
	}
}
