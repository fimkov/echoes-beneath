package dev.fimkov.echoesbeneath.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record EchoMemoryReferences(List<EchoMemoryReference> entries) {
	public static final EchoMemoryReferences EMPTY = new EchoMemoryReferences(List.of());

	public static final Codec<EchoMemoryReferences> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			EchoMemoryReference.CODEC.listOf().fieldOf("entries").forGetter(EchoMemoryReferences::entries)
	).apply(instance, EchoMemoryReferences::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, EchoMemoryReferences> STREAM_CODEC = StreamCodec.of(
			(buffer, value) -> {
				buffer.writeVarInt(value.entries.size());
				for (EchoMemoryReference reference : value.entries) {
					EchoMemoryReference.STREAM_CODEC.encode(buffer, reference);
				}
			},
			buffer -> {
				int size = Math.min(buffer.readVarInt(), EchoFormat.MEMORY_SLOT_COUNT);
				List<EchoMemoryReference> decoded = new ArrayList<>(size);
				for (int index = 0; index < size; index++) {
					decoded.add(EchoMemoryReference.STREAM_CODEC.decode(buffer));
				}
				return new EchoMemoryReferences(List.copyOf(decoded));
			});

	public EchoMemoryReferences {
		entries = List.copyOf(entries);
	}

	public int filledSlots() {
		return entries.size();
	}

	public boolean isFull() {
		return entries.size() >= EchoFormat.MEMORY_SLOT_COUNT;
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public Optional<Integer> nextFreeSlot() {
		for (int slot = 0; slot < EchoFormat.MEMORY_SLOT_COUNT; slot++) {
			if (findBySlot(slot).isEmpty()) {
				return Optional.of(slot);
			}
		}
		return Optional.empty();
	}

	public Optional<EchoMemoryReference> findBySlot(int slot) {
		for (EchoMemoryReference reference : entries) {
			if (reference.slot() == slot) {
				return Optional.of(reference);
			}
		}
		return Optional.empty();
	}

	public Optional<EchoMemoryReference> findById(UUID memoryId) {
		for (EchoMemoryReference reference : entries) {
			if (reference.memoryId().equals(memoryId)) {
				return Optional.of(reference);
			}
		}
		return Optional.empty();
	}

	public EchoMemoryReferences withAdded(EchoMemoryReference reference) {
		if (findBySlot(reference.slot()).isPresent()) {
			throw new IllegalStateException("Memory slot " + reference.slot() + " is already sealed");
		}
		List<EchoMemoryReference> merged = new ArrayList<>(entries);
		merged.add(reference);
		merged.sort(java.util.Comparator.comparingInt(EchoMemoryReference::slot));
		return new EchoMemoryReferences(List.copyOf(merged));
	}

	public EchoMemoryReferences withRemoved(UUID memoryId) {
		List<EchoMemoryReference> remaining = new ArrayList<>(entries.size());
		for (EchoMemoryReference reference : entries) {
			if (!reference.memoryId().equals(memoryId)) {
				remaining.add(reference);
			}
		}
		return new EchoMemoryReferences(List.copyOf(remaining));
	}
}
