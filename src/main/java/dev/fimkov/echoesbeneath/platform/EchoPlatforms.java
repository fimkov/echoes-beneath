package dev.fimkov.echoesbeneath.platform;

import java.nio.file.Path;

public final class EchoPlatforms {
	private static volatile EchoPlatform current;

	private EchoPlatforms() {
	}

	public static void install(EchoPlatform platform) {
		current = platform;
	}

	public static EchoPlatform get() {
		EchoPlatform platform = current;
		if (platform == null) {
			throw new IllegalStateException("No loader platform has been installed");
		}
		return platform;
	}

	public static Path configDirectory() {
		return get().configDirectory();
	}
}
