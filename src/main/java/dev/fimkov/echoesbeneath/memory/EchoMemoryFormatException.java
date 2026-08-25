package dev.fimkov.echoesbeneath.memory;

public class EchoMemoryFormatException extends RuntimeException {
	public EchoMemoryFormatException(String message) {
		super(message);
	}

	public EchoMemoryFormatException(String message, Throwable cause) {
		super(message, cause);
	}
}
