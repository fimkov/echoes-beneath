package dev.fimkov.echoesbeneath;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoStructureTemplateTest {
	private static final String TABLE = "echoes_beneath:resonance_table";
	private static final String AIR = "minecraft:air";

	private static final String[] TEMPLATES = {
			"echo_listening_post/post_1", "echo_listening_post/post_2",
			"collapsed_resonance_shrine/shrine_1", "collapsed_resonance_shrine/shrine_2",
			"surface_echo_ruin/ruin_1", "surface_echo_ruin/ruin_2",
			"resonance_vault/vault_1", "resonance_vault/vault_2",
	};

	private record Placed(Map<List<Integer>, String> blocks, List<List<Integer>> tables) {
	}

	private static Placed read(String name) {
		try (InputStream stream = EchoStructureTemplateTest.class

				.getResourceAsStream("/data/echoes_beneath/structure/" + name + ".nbt")) {
			assertNotNull(stream, name + " is missing");
			CompoundTag root = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
			ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
			List<String> names = new ArrayList<>();

			for (int index = 0; index < palette.size(); index++) {
				names.add(palette.getCompound(index).getString("Name"));
			}
			Map<List<Integer>, String> blocks = new HashMap<>();
			List<List<Integer>> tables = new ArrayList<>();
			ListTag placed = root.getList("blocks", Tag.TAG_COMPOUND);

			for (int index = 0; index < placed.size(); index++) {
				CompoundTag entry = placed.getCompound(index);
				ListTag position = entry.getList("pos", Tag.TAG_INT);
				List<Integer> key = List.of(position.getInt(0), position.getInt(1), position.getInt(2));
				String block = names.get(entry.getInt("state"));
				blocks.put(key, block);

				if (block.equals(TABLE)) {
					tables.add(key);
				}
			}
			return new Placed(blocks, tables);
		} catch (Exception failure) {
			throw new IllegalStateException(name, failure);
		}
	}

	@Test
	@DisplayName("every structure in the mod carries exactly one resonance table")
	void everyStructureCarriesOneTable() {
		for (String name : TEMPLATES) {
			assertEquals(1, read(name).tables().size(), name + " does not hold exactly one table");
		}
	}

	@Test
	@DisplayName("a table's own level is cleared to air, not left to the world")
	void tablesStandClear() {
		List<String> failures = new ArrayList<>();

		for (String name : TEMPLATES) {
			Placed placed = read(name);
			List<Integer> table = placed.tables().getFirst();

			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dz == 0) {
						continue;
					}
					List<Integer> key = List.of(table.get(0) + dx, table.get(1), table.get(2) + dz);
					String neighbour = placed.blocks().get(key);

					if (neighbour == null) {
						failures.add(name + " leaves " + key + " to the world instead of clearing it");
					} else if (!neighbour.equals(AIR)) {
						failures.add(name + " has " + neighbour + " at " + key);
					}
				}
			}
		}

		assertTrue(failures.isEmpty(), String.join("; ", failures));
	}

	@Test
	@DisplayName("the shrine seals its dome so nothing outside can pour in")
	void shrineDomeIsSealed() {
		for (String name : new String[] {"collapsed_resonance_shrine/shrine_1",
				"collapsed_resonance_shrine/shrine_2"}) {
			Placed placed = read(name);
			int width = 13;
			int height = 9;
			List<String> open = new ArrayList<>();

			for (Map.Entry<List<Integer>, String> entry : placed.blocks().entrySet()) {
				if (!entry.getValue().equals(AIR)) {
					continue;
				}
				List<Integer> at = entry.getKey();

				for (int[] step : new int[][] {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
					int nx = at.get(0) + step[0];
					int ny = at.get(1) + step[1];
					int nz = at.get(2) + step[2];

					if (nx < 0 || ny < 0 || nz < 0 || nx >= width || ny >= height || nz >= width) {
						continue;
					}
					if (!placed.blocks().containsKey(List.of(nx, ny, nz))) {
						open.add(name + " opens to the world at " + nx + " " + ny + " " + nz);
					}
				}
			}
			assertTrue(open.isEmpty(), String.join("; ", open.subList(0, Math.min(4, open.size()))));
		}
	}
}
