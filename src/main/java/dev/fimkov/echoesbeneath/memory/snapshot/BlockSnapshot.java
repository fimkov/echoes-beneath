package dev.fimkov.echoesbeneath.memory.snapshot;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockSnapshot {
	private static final int SECTION_KEY_BITS = 8;
	private static final int SECTION_KEY_MASK = (1 << SECTION_KEY_BITS) - 1;
	private static final int SECTION_KEY_BIAS = 1 << (SECTION_KEY_BITS - 1);
	private static final int NBT_COMPOUND_TYPE = Tag.TAG_COMPOUND;

	private final BlockPalette palette;
	private final Int2ObjectMap<SnapshotSection> sections;
	private final int radius;
	private final int minRelativeY;
	private final int maxRelativeY;

	public BlockSnapshot(BlockPalette palette, Int2ObjectMap<SnapshotSection> sections,
			int radius, int minRelativeY, int maxRelativeY) {
		this.palette = palette;
		this.sections = sections;
		this.radius = radius;
		this.minRelativeY = minRelativeY;
		this.maxRelativeY = maxRelativeY;
	}

	public static int sectionKey(int sectionX, int sectionY, int sectionZ) {
		return ((sectionX + SECTION_KEY_BIAS) & SECTION_KEY_MASK)
				| (((sectionY + SECTION_KEY_BIAS) & SECTION_KEY_MASK) << SECTION_KEY_BITS)
				| (((sectionZ + SECTION_KEY_BIAS) & SECTION_KEY_MASK) << (SECTION_KEY_BITS * 2));
	}

	public static int sectionKeyX(int key) {
		return (key & SECTION_KEY_MASK) - SECTION_KEY_BIAS;
	}

	public static int sectionKeyY(int key) {
		return ((key >> SECTION_KEY_BITS) & SECTION_KEY_MASK) - SECTION_KEY_BIAS;
	}

	public static int sectionKeyZ(int key) {
		return ((key >> (SECTION_KEY_BITS * 2)) & SECTION_KEY_MASK) - SECTION_KEY_BIAS;
	}

	public BlockPalette palette() {
		return palette;
	}

	public Int2ObjectMap<SnapshotSection> sections() {
		return sections;
	}

	public int radius() {
		return radius;
	}

	public int minRelativeY() {
		return minRelativeY;
	}

	public int maxRelativeY() {
		return maxRelativeY;
	}

	public BlockState stateAt(int relativeX, int relativeY, int relativeZ) {
		return palette.stateAt(paletteIndexAt(relativeX, relativeY, relativeZ));
	}

	public int paletteIndexAt(int relativeX, int relativeY, int relativeZ) {
		SnapshotSection section = sections.get(sectionKey(
				Math.floorDiv(relativeX, SnapshotSection.SECTION_SIZE),
				Math.floorDiv(relativeY, SnapshotSection.SECTION_SIZE),
				Math.floorDiv(relativeZ, SnapshotSection.SECTION_SIZE)));

		if (section == null) {
			return BlockPalette.AIR_INDEX;
		}

		return section.globalIndexAt(
				Math.floorMod(relativeX, SnapshotSection.SECTION_SIZE),
				Math.floorMod(relativeY, SnapshotSection.SECTION_SIZE),
				Math.floorMod(relativeZ, SnapshotSection.SECTION_SIZE));
	}

	public int sectionCount() {
		return sections.size();
	}

	public CompoundTag toNbt() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("radius", radius);
		tag.putInt("min_y", minRelativeY);
		tag.putInt("max_y", maxRelativeY);
		tag.put("palette", palette.toNbt());
		ListTag sectionList = new ListTag();

		for (Int2ObjectMap.Entry<SnapshotSection> entry : sections.int2ObjectEntrySet()) {
			CompoundTag sectionTag = entry.getValue().toNbt();
			sectionTag.putInt("key", entry.getIntKey());
			sectionList.add(sectionTag);
		}

		tag.put("sections", sectionList);

		return tag;
	}

	public static BlockSnapshot fromNbt(CompoundTag tag, HolderGetter<Block> blockLookup) {
		BlockPalette palette = BlockPalette.fromNbt(tag.getListOrEmpty("palette"), blockLookup);
		Int2ObjectMap<SnapshotSection> sections = new Int2ObjectOpenHashMap<>();
		ListTag sectionList = tag.getListOrEmpty("sections");

		for (Tag element : sectionList) {
			CompoundTag sectionTag = (CompoundTag) element;
			sections.put(sectionTag.getIntOr("key", 0), SnapshotSection.fromNbt(sectionTag));
		}

		return new BlockSnapshot(palette, sections, tag.getIntOr("radius", 0), tag.getIntOr("min_y", 0), tag.getIntOr("max_y", 0));
	}
}
