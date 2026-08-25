package dev.fimkov.echoesbeneath.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class MemoryRecordedTrigger extends SimpleCriterionTrigger<MemoryRecordedTrigger.Instance> {
	@Override
	public Codec<Instance> codec() {
		return Instance.CODEC;
	}

	public void trigger(ServerPlayer player, int sounds, int filledSlots, Collection<ResourceLocation> heard) {
		trigger(player, instance -> instance.matches(sounds, filledSlots, heard));
	}

	public record Instance(Optional<ContextAwarePredicate> player, MinMaxBounds.Ints sounds,
			MinMaxBounds.Ints filledSlots, List<ResourceLocation> heardAny) implements SimpleInstance {
		public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
				MinMaxBounds.Ints.CODEC.optionalFieldOf("sounds", MinMaxBounds.Ints.ANY)
						.forGetter(Instance::sounds),
				MinMaxBounds.Ints.CODEC.optionalFieldOf("filled_slots", MinMaxBounds.Ints.ANY)
						.forGetter(Instance::filledSlots),
				ResourceLocation.CODEC.listOf().optionalFieldOf("heard_any", List.of())
						.forGetter(Instance::heardAny)
		).apply(instance, Instance::new));

		public boolean matches(int recordedSounds, int slots, Collection<ResourceLocation> heard) {
			if (!sounds.matches(recordedSounds) || !filledSlots.matches(slots)) {
				return false;
			}
			return heardAny.isEmpty() || heardAny.stream().anyMatch(heard::contains);
		}
	}
}
