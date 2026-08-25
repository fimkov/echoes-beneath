package dev.fimkov.echoesbeneath.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class MemoryMendedTrigger extends SimpleCriterionTrigger<MemoryMendedTrigger.Instance> {
	@Override
	public Codec<Instance> codec() {
		return Instance.CODEC;
	}

	public void trigger(ServerPlayer player, BlockState restored) {
		trigger(player, instance -> instance.matches(restored));
	}

	public record Instance(Optional<ContextAwarePredicate> player) implements SimpleInstance {
		public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player)
		).apply(instance, Instance::new));

		public boolean matches(BlockState restored) {
			return !restored.isAir();
		}
	}
}
