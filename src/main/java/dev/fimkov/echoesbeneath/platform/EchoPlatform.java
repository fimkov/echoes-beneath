package dev.fimkov.echoesbeneath.platform;

import java.nio.file.Path;

public interface EchoPlatform {
	Path configDirectory();

	String name();
}
