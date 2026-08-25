package dev.fimkov.echoesbeneath.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.fimkov.echoesbeneath.memory.EchoFormat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record EchoLensUpgrades(int memorySlots, int radius, int sensitivity, int vision) {
	public static final int MIN_RADIUS = 16;
	public static final int MAX_RADIUS = 64;
	public static final int RADIUS_STEP = 8;
	public static final int MAX_SENSITIVITY = 4;
	public static final int MAX_VISION = 4;

	public static final EchoLensUpgrades DEFAULT =
			new EchoLensUpgrades(EchoFormat.DEFAULT_MEMORY_SLOTS, MIN_RADIUS, 0, 0);

	public static final Codec<EchoLensUpgrades> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, EchoFormat.MAX_MEMORY_SLOTS).fieldOf("memory_slots").forGetter(EchoLensUpgrades::memorySlots),
			Codec.intRange(MIN_RADIUS, MAX_RADIUS).fieldOf("radius").forGetter(EchoLensUpgrades::radius),
			Codec.intRange(0, MAX_SENSITIVITY).fieldOf("sensitivity").forGetter(EchoLensUpgrades::sensitivity),
			Codec.intRange(0, MAX_VISION).fieldOf("clarity").forGetter(EchoLensUpgrades::vision)
	).apply(instance, EchoLensUpgrades::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, EchoLensUpgrades> STREAM_CODEC = StreamCodec.of(
			(buffer, value) -> {
				buffer.writeVarInt(value.memorySlots);
				buffer.writeVarInt(value.radius);
				buffer.writeVarInt(value.sensitivity);
				buffer.writeVarInt(value.vision);
			},
			buffer -> new EchoLensUpgrades(
					Math.clamp(buffer.readVarInt(), 1, EchoFormat.MAX_MEMORY_SLOTS),
					Math.clamp(buffer.readVarInt(), MIN_RADIUS, MAX_RADIUS),
					Math.clamp(buffer.readVarInt(), 0, MAX_SENSITIVITY),
					Math.clamp(buffer.readVarInt(), 0, MAX_VISION)));

	public static EchoLensUpgrades highestOf(EchoLensUpgrades first, EchoLensUpgrades second) {
		return new EchoLensUpgrades(
				Math.max(first.memorySlots, second.memorySlots),
				Math.max(first.radius, second.radius),
				Math.max(first.sensitivity, second.sensitivity),
				Math.max(first.vision, second.vision));
	}

	public boolean canAddMemorySlot() {
		return memorySlots < EchoFormat.MAX_MEMORY_SLOTS;
	}

	public boolean canWidenRadius() {
		return radius < MAX_RADIUS;
	}

	public boolean canSharpenSensitivity() {
		return sensitivity < MAX_SENSITIVITY;
	}

	public boolean canClearFurther() {
		return vision < MAX_VISION;
	}

	public EchoLensUpgrades withMemorySlot() {
		return new EchoLensUpgrades(memorySlots + 1, radius, sensitivity, vision);
	}

	public EchoLensUpgrades withRadius() {
		return new EchoLensUpgrades(memorySlots, Math.min(MAX_RADIUS, radius + RADIUS_STEP), sensitivity, vision);
	}

	public EchoLensUpgrades withSensitivity() {
		return new EchoLensUpgrades(memorySlots, radius, sensitivity + 1, vision);
	}

	public EchoLensUpgrades withVision() {
		return new EchoLensUpgrades(memorySlots, radius, sensitivity, vision + 1);
	}

	public float visionFraction() {
		return (float) vision / MAX_VISION;
	}

	public float sensitivityFactor() {
		return 1.0F - sensitivity * 0.18F;
	}
}
