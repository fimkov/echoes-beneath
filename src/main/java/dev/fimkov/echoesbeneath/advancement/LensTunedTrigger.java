package dev.fimkov.echoesbeneath.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.menu.ResonanceTableUpgrade;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class LensTunedTrigger extends SimpleCriterionTrigger<LensTunedTrigger.Instance> {
	@Override
	public Codec<Instance> codec() {
		return Instance.CODEC;
	}

	public void trigger(ServerPlayer player, EchoLensUpgrades upgrades) {
		trigger(player, instance -> instance.matches(upgrades));
	}

	public record Instance(Optional<ContextAwarePredicate> player, MinMaxBounds.Ints vision,
			Optional<Boolean> everyTrackMaxed) implements SimpleInstance {
		public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
				MinMaxBounds.Ints.CODEC.optionalFieldOf("vision", MinMaxBounds.Ints.ANY)
						.forGetter(Instance::vision),
				Codec.BOOL.optionalFieldOf("every_track_maxed").forGetter(Instance::everyTrackMaxed)
		).apply(instance, Instance::new));

		public boolean matches(EchoLensUpgrades upgrades) {
			if (!vision.matches(upgrades.vision())) {
				return false;
			}
			if (everyTrackMaxed.isEmpty()) {
				return true;
			}
			boolean maxed = true;
			for (ResonanceTableUpgrade upgrade : ResonanceTableUpgrade.all()) {
				maxed &= upgrade.stepsOf(upgrades) >= upgrade.maxSteps();
			}
			return everyTrackMaxed.get() == maxed;
		}
	}
}
