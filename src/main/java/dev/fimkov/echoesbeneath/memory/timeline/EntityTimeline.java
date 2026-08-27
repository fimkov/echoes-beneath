package dev.fimkov.echoesbeneath.memory.timeline;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class EntityTimeline {
	private static final int NBT_STRING_TYPE = Tag.TAG_STRING;
	private static final int NBT_COMPOUND_TYPE = Tag.TAG_COMPOUND;
	public static final int NO_ITEM_INDEX = -1;

	private final List<Identifier> entityTypePalette;
	private final Object2IntMap<Identifier> entityTypeIndices;
	private final List<Identifier> itemPalette;
	private final Object2IntMap<Identifier> itemIndices;
	private final List<EntityTrack> tracks;
	private final int maxTracks;

	public EntityTimeline(int maxTracks) {
		this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), maxTracks);
	}

	private EntityTimeline(List<Identifier> entityTypePalette, List<Identifier> itemPalette,
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

	public EntityTrack createTrack(Identifier entityTypeId) {
		EntityTrack track = new EntityTrack(internEntityType(entityTypeId));
		tracks.add(track);

		return track;
	}

	public int internItem(Identifier itemId) {
		int existing = itemIndices.getInt(itemId);

		if (existing >= 0) {
			return existing;
		}

		int index = itemPalette.size();
		itemPalette.add(itemId);
		itemIndices.put(itemId, index);

		return index;
	}

	private int internEntityType(Identifier entityTypeId) {
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

	public Identifier entityTypeOf(EntityTrack track) {
		int index = track.typeIndex();

		if (index < 0 || index >= entityTypePalette.size()) {
			return Identifier.withDefaultNamespace("marker");
		}

		return entityTypePalette.get(index);
	}

	public Identifier itemAt(int index) {
		if (index < 0 || index >= itemPalette.size()) {
			return Identifier.withDefaultNamespace("air");
		}

		return itemPalette.get(index);
	}

	public boolean holdsEntityType(Identifier typeId) {
		for (EntityTrack track : tracks()) {
			if (entityTypeOf(track).equals(typeId)) {
				return true;
			}
		}

		return false;
	}

	public List<Identifier> entityTypePalette() {
		return List.copyOf(entityTypePalette);
	}

	public List<Identifier> itemPalette() {
		return List.copyOf(itemPalette);
	}

	public CompoundTag toNbt() {
		CompoundTag tag = new CompoundTag();
		tag.put("entity_types", writeIdentifiers(entityTypePalette));
		tag.put("items", writeIdentifiers(itemPalette));
		ListTag trackList = new ListTag();

		for (EntityTrack track : tracks) {
			trackList.add(track.toNbt());
		}

		tag.put("tracks", trackList);

		return tag;
	}

	private static ListTag writeIdentifiers(List<Identifier> identifiers) {
		ListTag list = new ListTag();

		for (Identifier identifier : identifiers) {
			list.add(StringTag.valueOf(identifier.toString()));
		}

		return list;
	}

	private static List<Identifier> readIdentifiers(ListTag list) {
		List<Identifier> identifiers = new ArrayList<>(list.size());

		for (Tag element : list) {
			Identifier parsed = Identifier.tryParse(element.getAsString());

			if (parsed == null) {
				throw new IllegalArgumentException("Entity timeline holds an unreadable identifier");
			}
			identifiers.add(parsed);
		}

		return identifiers;
	}

	public static EntityTimeline fromNbt(CompoundTag tag, int maxTracks) {
		List<Identifier> entityTypes = readIdentifiers(tag.getListOrEmpty("entity_types"));
		List<Identifier> items = readIdentifiers(tag.getListOrEmpty("items"));
		List<EntityTrack> tracks = new ArrayList<>();

		for (Tag element : tag.getListOrEmpty("tracks")) {
			tracks.add(EntityTrack.fromNbt((CompoundTag) element));
		}

		return new EntityTimeline(entityTypes, items, tracks, Math.max(maxTracks, tracks.size()));
	}

	public static EntityTimeline fromParts(List<Identifier> entityTypes, List<Identifier> items,
			List<EntityTrack> tracks, int maxTracks) {
		return new EntityTimeline(new ArrayList<>(entityTypes), new ArrayList<>(items),
				new ArrayList<>(tracks), Math.max(maxTracks, tracks.size()));
	}
}
