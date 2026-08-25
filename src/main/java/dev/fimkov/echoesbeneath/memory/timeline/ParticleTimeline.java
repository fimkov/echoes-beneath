package dev.fimkov.echoesbeneath.memory.timeline;

import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class ParticleTimeline {
	public static final int STRIDE = 9;
	private static final int OFFSET_TICK = 0;
	private static final int OFFSET_X = 1;
	private static final int OFFSET_Y = 2;
	private static final int OFFSET_Z = 3;
	private static final int OFFSET_TYPE = 4;
	private static final int OFFSET_COUNT = 5;
	private static final int OFFSET_SPREAD_X = 6;
	private static final int OFFSET_SPREAD_Y = 7;
	private static final int OFFSET_SPREAD_Z = 8;
	private static final float MILLI = 1000.0F;
	private static final int MAX_COUNT_PER_EVENT = 64;
	private static final int NBT_STRING_TYPE = Tag.TAG_STRING;

	private final List<ResourceLocation> typePalette;
	private final Object2IntMap<ResourceLocation> typeIndices;
	private final IntList data;
	private final int maxEvents;

	public ParticleTimeline(int maxEvents) {
		this.typePalette = new ArrayList<>();
		this.typeIndices = new Object2IntOpenHashMap<>();
		this.typeIndices.defaultReturnValue(-1);
		this.data = new IntArrayList();
		this.maxEvents = maxEvents;
	}

	private ParticleTimeline(List<ResourceLocation> typePalette, IntList data, int maxEvents) {
		this.typePalette = typePalette;
		this.typeIndices = new Object2IntOpenHashMap<>();
		this.typeIndices.defaultReturnValue(-1);
		for (int index = 0; index < typePalette.size(); index++) {
			this.typeIndices.put(typePalette.get(index), index);
		}
		this.data = data;
		this.maxEvents = maxEvents;
	}

	public boolean record(int tick, double relativeX, double relativeY, double relativeZ,
			ResourceLocation typeId, int count, double spreadX, double spreadY, double spreadZ) {
		if (size() >= maxEvents) {
			return false;
		}
		int quantizedX = EchoCoordinates.quantizePosition(relativeX);
		int quantizedY = EchoCoordinates.quantizePosition(relativeY);
		int quantizedZ = EchoCoordinates.quantizePosition(relativeZ);
		int typeIndex = internType(typeId);
		if (duplicatesLastEvent(tick, quantizedX, quantizedY, quantizedZ, typeIndex)) {
			return true;
		}
		data.add(tick);
		data.add(quantizedX);
		data.add(quantizedY);
		data.add(quantizedZ);
		data.add(typeIndex);
		data.add(Math.clamp(count, 1, MAX_COUNT_PER_EVENT));
		data.add(Math.round((float) spreadX * MILLI));
		data.add(Math.round((float) spreadY * MILLI));
		data.add(Math.round((float) spreadZ * MILLI));
		return true;
	}

	private boolean duplicatesLastEvent(int tick, int x, int y, int z, int typeIndex) {
		int count = size();
		if (count == 0) {
			return false;
		}
		int last = count - 1;
		return tickAt(last) == tick
				&& data.getInt(last * STRIDE + OFFSET_X) == x
				&& data.getInt(last * STRIDE + OFFSET_Y) == y
				&& data.getInt(last * STRIDE + OFFSET_Z) == z
				&& data.getInt(last * STRIDE + OFFSET_TYPE) == typeIndex;
	}

	private int internType(ResourceLocation typeId) {
		int existing = typeIndices.getInt(typeId);
		if (existing >= 0) {
			return existing;
		}
		int index = typePalette.size();
		typePalette.add(typeId);
		typeIndices.put(typeId, index);
		return index;
	}

	public int size() {
		return data.size() / STRIDE;
	}

	public int tickAt(int index) {
		return data.getInt(index * STRIDE + OFFSET_TICK);
	}

	public double relativeXAt(int index) {
		return EchoCoordinates.dequantizePosition(data.getInt(index * STRIDE + OFFSET_X));
	}

	public double relativeYAt(int index) {
		return EchoCoordinates.dequantizePosition(data.getInt(index * STRIDE + OFFSET_Y));
	}

	public double relativeZAt(int index) {
		return EchoCoordinates.dequantizePosition(data.getInt(index * STRIDE + OFFSET_Z));
	}

	public ResourceLocation typeAt(int index) {
		int typeIndex = data.getInt(index * STRIDE + OFFSET_TYPE);
		if (typeIndex < 0 || typeIndex >= typePalette.size()) {
			return typePalette.isEmpty()
					? ResourceLocation.withDefaultNamespace("intentionally_empty")
					: typePalette.getFirst();
		}
		return typePalette.get(typeIndex);
	}

	public int countAt(int index) {
		return data.getInt(index * STRIDE + OFFSET_COUNT);
	}

	public double spreadXAt(int index) {
		return data.getInt(index * STRIDE + OFFSET_SPREAD_X) / MILLI;
	}

	public double spreadYAt(int index) {
		return data.getInt(index * STRIDE + OFFSET_SPREAD_Y) / MILLI;
	}

	public double spreadZAt(int index) {
		return data.getInt(index * STRIDE + OFFSET_SPREAD_Z) / MILLI;
	}

	public List<ResourceLocation> typePalette() {
		return List.copyOf(typePalette);
	}

	public int[] rawData() {
		return data.toIntArray();
	}

	public CompoundTag toNbt() {
		CompoundTag tag = new CompoundTag();
		ListTag palette = new ListTag();
		for (ResourceLocation typeId : typePalette) {
			palette.add(StringTag.valueOf(typeId.toString()));
		}
		tag.put("palette", palette);
		tag.putIntArray("events", data.toIntArray());
		return tag;
	}

	public static ParticleTimeline fromNbt(CompoundTag tag, int maxEvents) {
		List<ResourceLocation> palette = new ArrayList<>();
		for (Tag element : tag.getList("palette", NBT_STRING_TYPE)) {
			ResourceLocation parsed = ResourceLocation.tryParse(element.getAsString());
			if (parsed == null) {
				throw new IllegalArgumentException("Particle palette holds an unreadable identifier");
			}
			palette.add(parsed);
		}
		return fromParts(palette, tag.getIntArray("events"), maxEvents);
	}

	public static ParticleTimeline fromParts(List<ResourceLocation> palette, int[] raw, int maxEvents) {
		if (raw.length % STRIDE != 0) {
			throw new IllegalArgumentException(
					"Particle timeline length " + raw.length + " is not a multiple of " + STRIDE);
		}
		return new ParticleTimeline(new ArrayList<>(palette), new IntArrayList(raw), maxEvents);
	}
}
