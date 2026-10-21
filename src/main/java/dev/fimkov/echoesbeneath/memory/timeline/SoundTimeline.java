package dev.fimkov.echoesbeneath.memory.timeline;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import dev.fimkov.echoesbeneath.memory.EchoCoordinates;

import java.util.ArrayList;
import java.util.List;

public final class SoundTimeline {
	public static final int STRIDE = 8;
	private static final int OFFSET_TICK = 0;
	private static final int OFFSET_X = 1;
	private static final int OFFSET_Y = 2;
	private static final int OFFSET_Z = 3;
	private static final int OFFSET_SOUND = 4;
	private static final int OFFSET_VOLUME = 5;
	private static final int OFFSET_PITCH = 6;
	private static final int OFFSET_CATEGORY = 7;
	private static final float MILLI = 1000.0F;
	private static final int NBT_STRING_TYPE = Tag.TAG_STRING;

	private final List<ResourceLocation> soundPalette;
	private final Object2IntMap<ResourceLocation> soundIndices;
	private final IntList data;
	private final int maxEvents;

	public SoundTimeline(int maxEvents) {
		this.soundPalette = new ArrayList<>();
		this.soundIndices = new Object2IntOpenHashMap<>();
		this.soundIndices.defaultReturnValue(-1);
		this.data = new IntArrayList();
		this.maxEvents = maxEvents;
	}

	private SoundTimeline(List<ResourceLocation> soundPalette, IntList data, int maxEvents) {
		this.soundPalette = soundPalette;
		this.soundIndices = new Object2IntOpenHashMap<>();
		this.soundIndices.defaultReturnValue(-1);

		for (int index = 0; index < soundPalette.size(); index++) {
			this.soundIndices.put(soundPalette.get(index), index);
		}

		this.data = data;
		this.maxEvents = maxEvents;
	}

	public boolean record(int tick, double relativeX, double relativeY, double relativeZ,
			ResourceLocation soundId, SoundSource source, float volume, float pitch) {
		return record(tick, relativeX, relativeY, relativeZ, soundId,
				EchoSoundCategory.indexOf(source), volume, pitch);
	}

	public boolean recordJukebox(int tick, double x, double y, double z, ResourceLocation soundId, boolean playing) {
		return record(tick, x, y, z, soundId, -EchoSoundCategory.indexOf(SoundSource.RECORDS) - 1,
				playing ? 4.0F : -1.0F, 1.0F);
	}

	private boolean record(int tick, double relativeX, double relativeY, double relativeZ,
			ResourceLocation soundId, int categoryIndex, float volume, float pitch) {
		if (size() >= maxEvents) {
			return false;
		}

		int quantizedX = EchoCoordinates.quantizePosition(relativeX);
		int quantizedY = EchoCoordinates.quantizePosition(relativeY);
		int quantizedZ = EchoCoordinates.quantizePosition(relativeZ);
		int soundIndex = internSound(soundId);
		int volumeMilli = Math.round(volume * MILLI);
		int pitchMilli = Math.round(pitch * MILLI);

		if (duplicatesLastEvent(tick, quantizedX, quantizedY, quantizedZ, soundIndex, volumeMilli, pitchMilli,
				categoryIndex)) {
			return true;
		}

		data.add(tick);
		data.add(quantizedX);
		data.add(quantizedY);
		data.add(quantizedZ);
		data.add(soundIndex);
		data.add(volumeMilli);
		data.add(pitchMilli);
		data.add(categoryIndex);

		return true;
	}

	private boolean duplicatesLastEvent(int tick, int x, int y, int z, int soundIndex, int volume, int pitch,
			int category) {
		int count = size();

		if (count == 0) {
			return false;
		}

		int last = count - 1;

		return tickAt(last) == tick
				&& data.getInt(last * STRIDE + OFFSET_X) == x
				&& data.getInt(last * STRIDE + OFFSET_Y) == y
				&& data.getInt(last * STRIDE + OFFSET_Z) == z
				&& data.getInt(last * STRIDE + OFFSET_SOUND) == soundIndex
				&& data.getInt(last * STRIDE + OFFSET_VOLUME) == volume
				&& data.getInt(last * STRIDE + OFFSET_PITCH) == pitch
				&& data.getInt(last * STRIDE + OFFSET_CATEGORY) == category;
	}

	private int internSound(ResourceLocation soundId) {
		int existing = soundIndices.getInt(soundId);

		if (existing >= 0) {
			return existing;
		}

		int index = soundPalette.size();
		soundPalette.add(soundId);
		soundIndices.put(soundId, index);

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

	public ResourceLocation soundAt(int index) {
		int soundIndex = data.getInt(index * STRIDE + OFFSET_SOUND);

		if (soundIndex < 0 || soundIndex >= soundPalette.size()) {
			return soundPalette.isEmpty() ? ResourceLocation.withDefaultNamespace("intentionally_empty") : soundPalette.getFirst();
		}

		return soundPalette.get(soundIndex);
	}

	public float volumeAt(int index) {
		return data.getInt(index * STRIDE + OFFSET_VOLUME) / MILLI;
	}

	public float pitchAt(int index) {
		return data.getInt(index * STRIDE + OFFSET_PITCH) / MILLI;
	}

	public SoundSource categoryAt(int index) {
		int category = data.getInt(index * STRIDE + OFFSET_CATEGORY);
		return EchoSoundCategory.fromIndex(category < 0 ? -category - 1 : category);
	}

	public boolean isJukeboxAt(int index) {
		return data.getInt(index * STRIDE + OFFSET_CATEGORY) < 0 && categoryAt(index) == SoundSource.RECORDS;
	}

	public boolean isStopAt(int index) {
		return isJukeboxAt(index) && volumeAt(index) < 0.0F;
	}

	public float waveRadiusAt(int index) {
		return SoundWaveStrength.radiusFor(categoryAt(index), volumeAt(index));
	}

	public List<ResourceLocation> soundPalette() {
		return List.copyOf(soundPalette);
	}

	public int[] rawData() {
		return data.toIntArray();
	}

	public CompoundTag toNbt() {
		CompoundTag tag = new CompoundTag();
		ListTag palette = new ListTag();

		for (ResourceLocation soundId : soundPalette) {
			palette.add(StringTag.valueOf(soundId.toString()));
		}

		tag.put("palette", palette);
		tag.putIntArray("events", data.toIntArray());

		return tag;
	}

	public static SoundTimeline fromNbt(CompoundTag tag, int maxEvents) {
		List<ResourceLocation> palette = new ArrayList<>();

		for (Tag element : tag.getList("palette", Tag.TAG_STRING)) {
			ResourceLocation parsed = ResourceLocation.tryParse(element.getAsString());

			if (parsed == null) {
				throw new IllegalArgumentException("Sound palette holds an unreadable identifier");
			}
			palette.add(parsed);
		}

		int[] raw = tag.getIntArray("events");

		if (raw.length % STRIDE != 0) {
			throw new IllegalArgumentException("Sound timeline length " + raw.length + " is not a multiple of " + STRIDE);
		}

		return new SoundTimeline(palette, new IntArrayList(raw), maxEvents);
	}

	public static SoundTimeline fromParts(List<ResourceLocation> palette, int[] raw, int maxEvents) {
		if (raw.length % STRIDE != 0) {
			throw new IllegalArgumentException("Sound timeline length " + raw.length + " is not a multiple of " + STRIDE);
		}

		return new SoundTimeline(new ArrayList<>(palette), new IntArrayList(raw), maxEvents);
	}
}
