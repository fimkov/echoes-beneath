package dev.fimkov.echoesbeneath.memory.storage;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.config.EchoConfig;
import dev.fimkov.echoesbeneath.memory.EchoMemoryRecord;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EchoMemoryStorage {
	private static final String DIRECTORY_NAME = "echoes_beneath";
	private static final String FILE_SUFFIX = ".echo";
	private static final String TEMPORARY_SUFFIX = ".echo.tmp";
	private static final long MAX_RECORD_BYTES = 48L * 1024L * 1024L;
	private static final int CACHE_CAPACITY = 4;
	private static final int MAX_BLOCK_EVENTS = 65536;

	private final Path root;
	private final HolderGetter<Block> blockLookup;
	private final Map<UUID, EchoMemoryRecord> cache;

	public EchoMemoryStorage(Path root, HolderGetter<Block> blockLookup) {
		this.root = root;
		this.blockLookup = blockLookup;

		this.cache = new LinkedHashMap<>(CACHE_CAPACITY, 0.75F, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<UUID, EchoMemoryRecord> eldest) {
				return size() > CACHE_CAPACITY;
			}
		};
	}

	public static EchoMemoryStorage forServer(MinecraftServer server) {
		Path root = server.getWorldPath(LevelResource.ROOT).resolve(DIRECTORY_NAME);

		return new EchoMemoryStorage(root, server.registryAccess().<Block>lookupOrThrow(Registries.BLOCK));
	}

	public Path root() {
		return root;
	}

	private Path fileFor(UUID id) {
		return root.resolve(id + FILE_SUFFIX);
	}

	public boolean exists(UUID id) {
		return cache.containsKey(id) || Files.isRegularFile(fileFor(id));
	}

	public synchronized boolean save(EchoMemoryRecord record) {
		record.validate();
		Path target = fileFor(record.id());
		Path temporary = root.resolve(record.id() + TEMPORARY_SUFFIX);

		try {
			Files.createDirectories(root);
			NbtIo.writeCompressed(record.toNbt(), temporary);
			long written = Files.size(temporary);

			if (written > MAX_RECORD_BYTES) {
				Files.deleteIfExists(temporary);
				EchoesBeneath.LOGGER.error("Refusing to store memory {}: {} bytes exceeds the {} byte limit",
						record.id(), written, MAX_RECORD_BYTES);

				return false;
			}
			Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			cache.put(record.id(), record);

			return true;
		} catch (IOException failure) {
			EchoesBeneath.LOGGER.error("Unable to store memory {}", record.id(), failure);
			deleteQuietly(temporary);

			return false;
		}
	}

	public synchronized Optional<EchoMemoryRecord> load(UUID id) {
		EchoMemoryRecord cached = cache.get(id);

		if (cached != null) {
			return Optional.of(cached);
		}

		Path source = fileFor(id);

		if (!Files.isRegularFile(source)) {
			return Optional.empty();
		}

		try {
			CompoundTag tag = NbtIo.readCompressed(source, NbtAccounter.create(MAX_RECORD_BYTES));
			EchoMemoryRecord record = EchoMemoryRecord.fromNbt(tag, blockLookup, MAX_BLOCK_EVENTS,
					EchoConfig.values().maxRecordedEntities(), EchoConfig.values().maxSoundEvents(),
					EchoConfig.values().maxParticleEvents());
			record.validate();
			cache.put(id, record);

			return Optional.of(record);
		} catch (IOException | RuntimeException failure) {
			EchoesBeneath.LOGGER.error("Memory {} could not be read and will be ignored", id, failure);

			return Optional.empty();
		}
	}

	public synchronized boolean delete(UUID id) {
		cache.remove(id);

		try {
			return Files.deleteIfExists(fileFor(id));
		} catch (IOException failure) {
			EchoesBeneath.LOGGER.error("Unable to delete memory {}", id, failure);

			return false;
		}
	}

	public synchronized void evict(UUID id) {
		cache.remove(id);
	}

	public synchronized void clearCache() {
		cache.clear();
	}

	private void deleteQuietly(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
			EchoesBeneath.LOGGER.debug("Leftover temporary memory file at {}", path);
		}
	}
}
