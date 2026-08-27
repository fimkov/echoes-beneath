package dev.fimkov.echoesbeneath.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class MemoryPlayedTrigger extends SimpleCriterionTrigger<MemoryPlayedTrigger.Instance> {
	@Override
	public Codec<Instance> codec() {
		return Instance.CODEC;
	}

	public void trigger(ServerPlayer player, Situation situation) {
		trigger(player, instance -> instance.matches(situation));
	}

	public record Situation(boolean watchedToTheEnd, boolean sawSomeoneElse, boolean stoodInsideLostBlock,
			int vision) {
	}

	public record Instance(Optional<ContextAwarePredicate> player, Optional<Boolean> watchedToTheEnd,
			Optional<Boolean> sawSomeoneElse, Optional<Boolean> stoodInsideLostBlock,
			MinMaxBounds.Ints vision) implements SimpleInstance {
		public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
				Codec.BOOL.optionalFieldOf("watched_to_the_end").forGetter(Instance::watchedToTheEnd),
				Codec.BOOL.optionalFieldOf("saw_someone_else").forGetter(Instance::sawSomeoneElse),
				Codec.BOOL.optionalFieldOf("stood_inside_lost_block").forGetter(Instance::stoodInsideLostBlock),
				MinMaxBounds.Ints.CODEC.optionalFieldOf("vision", MinMaxBounds.Ints.ANY)
						.forGetter(Instance::vision)
		).apply(instance, Instance::new));

		public boolean matches(Situation situation) {
			return matchesFlag(watchedToTheEnd, situation.watchedToTheEnd())
					&& matchesFlag(sawSomeoneElse, situation.sawSomeoneElse())
					&& matchesFlag(stoodInsideLostBlock, situation.stoodInsideLostBlock())
					&& vision.matches(situation.vision());
		}

		private static boolean matchesFlag(Optional<Boolean> expected, boolean actual) {
			return expected.isEmpty() || expected.get() == actual;
		}
	}
}
