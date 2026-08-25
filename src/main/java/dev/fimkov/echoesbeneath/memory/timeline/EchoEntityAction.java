package dev.fimkov.echoesbeneath.memory.timeline;

public enum EchoEntityAction {
	SWING,
	HURT,
	DEATH,
	USE_ITEM,
	JUMP,
	TELEPORT,
	MOUNT,
	DISMOUNT;

	private static final EchoEntityAction[] VALUES = values();

	public static EchoEntityAction fromIndex(int index) {
		if (index < 0 || index >= VALUES.length) {
			return SWING;
		}
		return VALUES[index];
	}
}
