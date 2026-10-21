package dev.fimkov.echoesbeneath.memory.timeline;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class EntityTimeline {
	private static final int NBT_STRING_TYPE = Tag.TAG_STRING;
	private static final int NBT_COMPOUND_TYPE = Tag.TAG_COMPOUND;
	public static final int NO_ITEM_INDEX = -1;

	private final List<ResourceLocation> entityTypePalette;
	private final Object2IntMap<ResourceLocation> entityTypeIndices;
	private final List<ResourceLocation> itemPalette;
	private final Object2IntMap<ResourceLocation> itemIndices;
	private final List<EntityTrack> tracks;
	private final int maxTracks;

	public EntityTimeline(int maxTracks) {
		this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), maxTracks);
	}

	private EntityTimeline(List<ResourceLocation> entityTypePalette, List<ResourceLocation> itemPalette,
			List<EntityTrack> tracks, int maxTracks) {
		this.entityTypePalette = entityTypePalette;
		this.itemPalette = itemPalette;
		this.tracks = tracks;
		this.maxTracks = maxTracks;
		this.entityTypeIndices = new Object2IntOpenHashMap<>();
		this.entityTypeIndices.defaultReturnValue(-1);
		this.itemIndices = new Object2IntOpenHashMap<>();
		this.itemIndices.defaultReturnValue(-1);

		for (int index = 0; index < entityTypePalette.size(); index++) {
			this.entityTypeIndices.put(entityTypePalette.get(index), index);
		}

		for (int index = 0; index < itemPalette.size(); index++) {
			this.itemIndices.put(itemPalette.get(index), index);
		}
	}

	public boolean hasCapacity() {
		return tracks.size() < maxTracks;
	}

	public EntityTrack createTrack(ResourceLocation entityTypeId) {
		EntityTrack track = new EntityTrack(internEntityType(entityTypeId));
		tracks.add(track);

		return track;
	}

	public int internItem(ResourceLocation itemId) {
		int existing = itemIndices.getInt(itemId);

		if (existing >= 0) {
			return existing;
		}

		int index = itemPalette.size();
		itemPalette.add(itemId);
		itemIndices.put(itemId, index);

		return index;
	}

	private int internEntityType(ResourceLocation entityTypeId) {
		int existing = entityTypeIndices.getInt(entityTypeId);

		if (existing >= 0) {
			return existing;
		}

		int index = entityTypePalette.size();
		entityTypePalette.add(entityTypeId);
		entityTypeIndices.put(entityTypeId, index);

		return index;
	}

	public List<EntityTrack> tracks() {
		return List.copyOf(tracks);
	}

	public int trackCount() {
		return tracks.size();
	}

	public int totalKeyframes() {
		int total = 0;

		for (EntityTrack track : tracks) {
			total += track.keyframeCount();
		}

		return total;
	}

	public ResourceLocation entityTypeOf(EntityTrack track) {
		int index = track.typeIndex();

		if (index < 0 || index >= entityTypePalette.size()) {
			return ResourceLocation.withDefaultNamespace("marker");
		}

		return entityTypePalette.get(index);
	}

	public ResourceLocation itemAt(int index) {
		if (index < 0 || index >= itemPalette.size()) {
			return ResourceLocation.withDefaultNamespace("air");
		}

		return itemPalette.get(index);
	}

	public boolean holdsEntityType(ResourceLocation typeId) {
		for (EntityTrack track : tracks()) {
			if (entityTypeOf(track).equals(typeId)) {
				return true;
			}
		}

		return false;
	}

	public List<ResourceLocation> entityTypePalette() {
		return List.copyOf(entityTypePalette);
	}

	public List<ResourceLocation> itemPalette() {
		return List.copyOf(itemPalette);
	}

	public CompoundTag toNbt() {
		CompoundTag tag = new CompoundTag();
		tag.put("entity_types", writeResourceLocations(entityTypePalette));
		tag.put("items", writeResourceLocations(itemPalette));
		ListTag trackList = new ListTag();

		for (EntityTrack track : tracks) {
			trackList.add(track.toNbt());
		}

		tag.put("tracks", trackList);

		return tag;
	}

	private static ListTag writeResourceLocations(List<ResourceLocation> identifiers) {
		ListTag list = new ListTag();

		for (ResourceLocation identifier : identifiers) {
			list.add(StringTag.valueOf(identifier.toString()));
		}

		return list;
	}

	private static List<ResourceLocation> readResourceLocations(ListTag list) {
		List<ResourceLocation> identifiers = new ArrayList<>(list.size());

		for (Tag element : list) {
			ResourceLocation parsed = ResourceLocation.tryParse(element.getAsString());

			if (parsed == null) {
				throw new IllegalArgumentException("Entity timeline holds an unreadable identifier");
			}
			identifiers.add(parsed);
		}

		return identifiers;
	}

	public static EntityTimeline fromNbt(CompoundTag tag, int maxTracks) {
		List<ResourceLocation> entityTypes = readResourceLocations(tag.getList("entity_types", NBT_STRING_TYPE));
		List<ResourceLocation> items = readResourceLocations(tag.getList("items", NBT_STRING_TYPE));
		List<EntityTrack> tracks = new ArrayList<>();

		for (Tag element : tag.getList("tracks", NBT_COMPOUND_TYPE)) {
			tracks.add(EntityTrack.fromNbt((CompoundTag) element));
		}

		return new EntityTimeline(entityTypes, items, tracks, Math.max(maxTracks, tracks.size()));
	}

	public static EntityTimeline fromParts(List<ResourceLocation> entityTypes, List<ResourceLocation> items,
			List<EntityTrack> tracks, int maxTracks) {
		return new EntityTimeline(new ArrayList<>(entityTypes), new ArrayList<>(items),
				new ArrayList<>(tracks), Math.max(maxTracks, tracks.size()));
	}
}
