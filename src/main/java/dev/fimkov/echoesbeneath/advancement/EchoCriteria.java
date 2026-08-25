package dev.fimkov.echoesbeneath.advancement;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class EchoCriteria {
	public static final MemoryRecordedTrigger MEMORY_RECORDED = new MemoryRecordedTrigger();
	public static final MemoryPlayedTrigger MEMORY_PLAYED = new MemoryPlayedTrigger();
	public static final LensTunedTrigger LENS_TUNED = new LensTunedTrigger();
	public static final MemoryMendedTrigger MEMORY_MENDED = new MemoryMendedTrigger();

	private EchoCriteria() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.TRIGGER_TYPES, EchoesBeneath.id("memory_recorded"), MEMORY_RECORDED);
		Registry.register(BuiltInRegistries.TRIGGER_TYPES, EchoesBeneath.id("memory_played"), MEMORY_PLAYED);
		Registry.register(BuiltInRegistries.TRIGGER_TYPES, EchoesBeneath.id("lens_tuned"), LENS_TUNED);
		Registry.register(BuiltInRegistries.TRIGGER_TYPES, EchoesBeneath.id("memory_mended"), MEMORY_MENDED);
	}
}
