package dev.fimkov.echoesbeneath.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class MemoryRecordedTrigger extends SimpleCriterionTrigger<MemoryRecordedTrigger.Instance> {
	@Override
	public Codec<Instance> codec() {
		return Instance.CODEC;
	}

	public void trigger(ServerPlayer player, int sounds, int filledSlots, int durationTicks,
			Collection<Identifier> heard) {
		trigger(player, instance -> instance.matches(sounds, filledSlots, durationTicks, heard));
	}

	public record Instance(Optional<ContextAwarePredicate> player, MinMaxBounds.Ints sounds,
			MinMaxBounds.Ints filledSlots, MinMaxBounds.Ints durationTicks,
			List<Identifier> heardAny) implements SimpleInstance {
		public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
				MinMaxBounds.Ints.CODEC.optionalFieldOf("sounds", MinMaxBounds.Ints.ANY)
						.forGetter(Instance::sounds),
				MinMaxBounds.Ints.CODEC.optionalFieldOf("filled_slots", MinMaxBounds.Ints.ANY)
						.forGetter(Instance::filledSlots),
				MinMaxBounds.Ints.CODEC.optionalFieldOf("duration_ticks", MinMaxBounds.Ints.ANY)
						.forGetter(Instance::durationTicks),
				Identifier.CODEC.listOf().optionalFieldOf("heard_any", List.of())
						.forGetter(Instance::heardAny)
		).apply(instance, Instance::new));

		public boolean matches(int recordedSounds, int slots, int ticks,

				Collection<Identifier> heard) {
			if (!sounds.matches(recordedSounds) || !filledSlots.matches(slots)
					|| !durationTicks.matches(ticks)) {
				return false;
			}
			return heardAny.isEmpty() || heardAny.stream().anyMatch(heard::contains);
		}
	}
}
