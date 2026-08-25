package dev.fimkov.echoesbeneath.recording;

public enum EchoRecordingStopReason {
	DURATION_REACHED(true),
	OWNER_REQUEST(true),
	CHUNKS_UNLOADED(true),
	SERVER_STOPPING(true),
	DIMENSION_UNLOADED(true),
	LENS_LOST(false),
	FAILURE(false);

	private final boolean sealsMemory;

	EchoRecordingStopReason(boolean sealsMemory) {
		this.sealsMemory = sealsMemory;
	}

	public boolean sealsMemory() {
		return sealsMemory;
	}
}
