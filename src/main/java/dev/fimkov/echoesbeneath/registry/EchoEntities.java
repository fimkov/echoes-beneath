package dev.fimkov.echoesbeneath.registry;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.entity.EchoLensEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class EchoEntities {
	private static final float LENS_SIZE = 0.6F;
	private static final int TRACKING_RANGE_CHUNKS = 8;
	private static final int UPDATE_INTERVAL_TICKS = 2;

	public static final EntityType<EchoLensEntity> ECHO_LENS = EntityType.Builder
			.<EchoLensEntity>of(EchoLensEntity::new, MobCategory.MISC)
			.sized(LENS_SIZE, LENS_SIZE)
			.noSummon()
			.clientTrackingRange(TRACKING_RANGE_CHUNKS)
			.updateInterval(UPDATE_INTERVAL_TICKS)
			.build("echo_lens");

	private EchoEntities() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ENTITY_TYPE, EchoesBeneath.id("echo_lens"), ECHO_LENS);
	}
}
