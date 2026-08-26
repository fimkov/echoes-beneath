package dev.fimkov.echoesbeneath.memory.timeline;

import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.nbt.CompoundTag;

public final class EntityTrack {
	public static final int KEYFRAME_STRIDE = 8;
	public static final int ACTION_STRIDE = 2;
	public static final int EQUIPMENT_STRIDE = 3;

	private static final int OFFSET_TICK = 0;
	private static final int OFFSET_X = 1;
	private static final int OFFSET_Y = 2;
	private static final int OFFSET_Z = 3;
	private static final int OFFSET_YAW = 4;
	private static final int OFFSET_PITCH = 5;
	private static final int OFFSET_HEAD_YAW = 6;
	private static final int OFFSET_FLAGS = 7;

	private static final int FLAG_ON_GROUND = 1;
	private static final int POSE_SHIFT = 1;
	private static final int POSE_MASK = 0x1F;
	private static final int FLAG_BABY = 1 << 6;

	private final int typeIndex;
	private final IntList keyframes;
	private final IntList actions;
	private final IntList equipment;

	public EntityTrack(int typeIndex) {
		this(typeIndex, new IntArrayList(), new IntArrayList(), new IntArrayList());
	}

	private EntityTrack(int typeIndex, IntList keyframes, IntList actions, IntList equipment) {
		this.typeIndex = typeIndex;
		this.keyframes = keyframes;
		this.actions = actions;
		this.equipment = equipment;
	}

	public int typeIndex() {
		return typeIndex;
	}

	public void addKeyframe(int tick, double relativeX, double relativeY, double relativeZ,
			float yaw, float pitch, float headYaw, boolean onGround, int poseOrdinal, boolean baby) {
		keyframes.add(tick);
		keyframes.add(EchoCoordinates.quantizePosition(relativeX));
		keyframes.add(EchoCoordinates.quantizePosition(relativeY));
		keyframes.add(EchoCoordinates.quantizePosition(relativeZ));
		keyframes.add(EchoCoordinates.quantizeRotation(yaw));
		keyframes.add(EchoCoordinates.quantizeRotation(pitch));
		keyframes.add(EchoCoordinates.quantizeRotation(headYaw));
		keyframes.add(packFlags(onGround, poseOrdinal, baby));
	}

	private static int packFlags(boolean onGround, int poseOrdinal, boolean baby) {
		int flags = onGround ? FLAG_ON_GROUND : 0;
		flags |= (poseOrdinal & POSE_MASK) << POSE_SHIFT;

		if (baby) {
			flags |= FLAG_BABY;
		}

		return flags;
	}

	public void addAction(int tick, EchoEntityAction action) {
		actions.add(tick);
		actions.add(action.ordinal());
	}

	public void addEquipment(int tick, int slotIndex, int itemIndex) {
		equipment.add(tick);
		equipment.add(slotIndex);
		equipment.add(itemIndex);
	}

	public int keyframeCount() {
		return keyframes.size() / KEYFRAME_STRIDE;
	}

	public int keyframeTick(int index) {
		return keyframes.getInt(index * KEYFRAME_STRIDE + OFFSET_TICK);
	}

	public double keyframeX(int index) {
		return EchoCoordinates.dequantizePosition(keyframes.getInt(index * KEYFRAME_STRIDE + OFFSET_X));
	}

	public double keyframeY(int index) {
		return EchoCoordinates.dequantizePosition(keyframes.getInt(index * KEYFRAME_STRIDE + OFFSET_Y));
	}

	public double keyframeZ(int index) {
		return EchoCoordinates.dequantizePosition(keyframes.getInt(index * KEYFRAME_STRIDE + OFFSET_Z));
	}

	public float keyframeYaw(int index) {
		return EchoCoordinates.dequantizeRotation(keyframes.getInt(index * KEYFRAME_STRIDE + OFFSET_YAW));
	}

	public float keyframePitch(int index) {
		return EchoCoordinates.dequantizeRotation(keyframes.getInt(index * KEYFRAME_STRIDE + OFFSET_PITCH));
	}

	public float keyframeHeadYaw(int index) {
		return EchoCoordinates.dequantizeRotation(keyframes.getInt(index * KEYFRAME_STRIDE + OFFSET_HEAD_YAW));
	}

	public boolean keyframeOnGround(int index) {
		return (keyframes.getInt(index * KEYFRAME_STRIDE + OFFSET_FLAGS) & FLAG_ON_GROUND) != 0;
	}

	public int keyframePose(int index) {
		return (keyframes.getInt(index * KEYFRAME_STRIDE + OFFSET_FLAGS) >> POSE_SHIFT) & POSE_MASK;
	}

	public boolean keyframeBaby(int index) {
		return (keyframes.getInt(index * KEYFRAME_STRIDE + OFFSET_FLAGS) & FLAG_BABY) != 0;
	}

	public int actionCount() {
		return actions.size() / ACTION_STRIDE;
	}

	public int actionTick(int index) {
		return actions.getInt(index * ACTION_STRIDE);
	}

	public EchoEntityAction actionAt(int index) {
		return EchoEntityAction.fromIndex(actions.getInt(index * ACTION_STRIDE + 1));
	}

	public int equipmentCount() {
		return equipment.size() / EQUIPMENT_STRIDE;
	}

	public int equipmentTick(int index) {
		return equipment.getInt(index * EQUIPMENT_STRIDE);
	}

	public int equipmentSlot(int index) {
		return equipment.getInt(index * EQUIPMENT_STRIDE + 1);
	}

	public int equipmentItem(int index) {
		return equipment.getInt(index * EQUIPMENT_STRIDE + 2);
	}

	public int lastKeyframeTick() {
		int count = keyframeCount();

		return count == 0 ? -1 : keyframeTick(count - 1);
	}

	public CompoundTag toNbt() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("type", typeIndex);
		tag.putIntArray("keyframes", keyframes.toIntArray());
		tag.putIntArray("actions", actions.toIntArray());
		tag.putIntArray("equipment", equipment.toIntArray());

		return tag;
	}

	public static EntityTrack fromNbt(CompoundTag tag) {
		int[] keyframeData = tag.getIntArray("keyframes");
		int[] actionData = tag.getIntArray("actions");
		int[] equipmentData = tag.getIntArray("equipment");

		if (keyframeData.length % KEYFRAME_STRIDE != 0
				|| actionData.length % ACTION_STRIDE != 0

				|| equipmentData.length % EQUIPMENT_STRIDE != 0) {
			throw new IllegalArgumentException("Entity track arrays have inconsistent lengths");
		}

		return new EntityTrack(tag.getInt("type"), new IntArrayList(keyframeData),
				new IntArrayList(actionData), new IntArrayList(equipmentData));
	}
}
