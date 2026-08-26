package dev.fimkov.echoesbeneath.chart;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record EchoChartLook(boolean drifts, int spin, boolean motes, boolean runs, int size) {
	public static final int SPIN_STILL = 0;
	public static final int SPIN_SLOW = 1;
	public static final int SPIN_BRISK = 2;
	public static final int SPIN_CHOICES = 3;
	public static final int SIZE_SMALL = 0;
	public static final int SIZE_MEDIUM = 1;
	public static final int SIZE_LARGE = 2;
	public static final int SIZE_CHOICES = 3;

	public static final EchoChartLook DEFAULT =
			new EchoChartLook(true, SPIN_SLOW, true, true, SIZE_MEDIUM);

	public static final Codec<EchoChartLook> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("drifts").forGetter(EchoChartLook::drifts),
			Codec.intRange(0, SPIN_CHOICES - 1).fieldOf("spin").forGetter(EchoChartLook::spin),
			Codec.BOOL.fieldOf("motes").forGetter(EchoChartLook::motes),
			Codec.BOOL.fieldOf("runs").forGetter(EchoChartLook::runs),
			Codec.intRange(0, SIZE_CHOICES - 1).fieldOf("size").forGetter(EchoChartLook::size)
	).apply(instance, EchoChartLook::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, EchoChartLook> STREAM_CODEC =
			StreamCodec.of(
					(buffer, value) -> {
						buffer.writeBoolean(value.drifts);
						buffer.writeVarInt(value.spin);
						buffer.writeBoolean(value.motes);
						buffer.writeBoolean(value.runs);
						buffer.writeVarInt(value.size);
					},
					buffer -> new EchoChartLook(
							buffer.readBoolean(),
							Math.clamp(buffer.readVarInt(), 0, SPIN_CHOICES - 1),
							buffer.readBoolean(),
							buffer.readBoolean(),
							Math.clamp(buffer.readVarInt(), 0, SIZE_CHOICES - 1)));

	public EchoChartLook withDrifts(boolean value) {
		return new EchoChartLook(value, spin, motes, runs, size);
	}

	public EchoChartLook withSpin(int value) {
		return new EchoChartLook(drifts, Math.clamp(value, 0, SPIN_CHOICES - 1), motes, runs, size);
	}

	public EchoChartLook withMotes(boolean value) {
		return new EchoChartLook(drifts, spin, value, runs, size);
	}

	public EchoChartLook withRuns(boolean value) {
		return new EchoChartLook(drifts, spin, motes, value, size);
	}

	public EchoChartLook withSize(int value) {
		return new EchoChartLook(drifts, spin, motes, runs, Math.clamp(value, 0, SIZE_CHOICES - 1));
	}

	public float spinDegreesPerTick() {
		return switch (spin) {
			case SPIN_STILL -> 0.0F;
			case SPIN_BRISK -> 0.70F;
			default -> 0.22F;
		};
	}

	public float modelScale() {
		return switch (size) {
			case SIZE_SMALL -> 0.30F;
			case SIZE_LARGE -> 0.80F;
			default -> 0.50F;
		};
	}

	public float restHeight() {
		return switch (size) {
			case SIZE_SMALL -> 0.3125F;
			case SIZE_LARGE -> 0.5F;
			default -> 0.40625F;
		};
	}

	public float boxFloor() {
		return switch (size) {
			case SIZE_SMALL -> 0.0625F;
			default -> 0.0F;
		};
	}

	public float boxCeiling() {
		return switch (size) {
			case SIZE_SMALL -> 0.5625F;
			case SIZE_LARGE -> 1.0F;
			default -> 0.8125F;
		};
	}

	public float boxReach() {
		return switch (size) {
			case SIZE_SMALL -> 0.1875F;
			case SIZE_LARGE -> 0.5F;
			default -> 0.3125F;
		};
	}
}
