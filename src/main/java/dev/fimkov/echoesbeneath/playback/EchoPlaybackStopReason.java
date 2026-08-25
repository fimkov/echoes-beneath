package dev.fimkov.echoesbeneath.playback;

public enum EchoPlaybackStopReason {
	TIMELINE_COMPLETE,
	OWNER_REQUEST,
	OWNER_LEFT,
	SERVER_STOPPING,
	DIMENSION_UNLOADED,
	LENS_LOST,
	FAILURE
}
