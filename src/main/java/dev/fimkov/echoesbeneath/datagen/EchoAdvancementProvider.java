package dev.fimkov.echoesbeneath.datagen;

import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.advancement.EchoCriteria;
import dev.fimkov.echoesbeneath.advancement.LensTunedTrigger;
import dev.fimkov.echoesbeneath.advancement.MemoryPlayedTrigger;
import dev.fimkov.echoesbeneath.advancement.MemoryRecordedTrigger;
import dev.fimkov.echoesbeneath.item.EchoLensUpgrades;
import dev.fimkov.echoesbeneath.memory.EchoFormat;
import dev.fimkov.echoesbeneath.registry.EchoBlocks;
import dev.fimkov.echoesbeneath.registry.EchoItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class EchoAdvancementProvider extends FabricAdvancementProvider {
	private static final int FULL_RECORDING_TICKS = 2200;

	private static final List<ResourceLocation> WARDEN_SOUNDS = List.of(
			ResourceLocation.withDefaultNamespace("entity.warden.roar"),
			ResourceLocation.withDefaultNamespace("entity.warden.heartbeat"),
			ResourceLocation.withDefaultNamespace("entity.warden.listening_angry"),
			ResourceLocation.withDefaultNamespace("entity.warden.sonic_boom"),
			ResourceLocation.withDefaultNamespace("entity.warden.angry"));

	public EchoAdvancementProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
		super(output, lookup);
	}

	private static Component title(String name) {
		return Component.translatable("advancements.echoes_beneath." + name + ".title");
	}

	private static Component description(String name) {
		return Component.translatable("advancements.echoes_beneath." + name + ".description");
	}

	private static Criterion<MemoryRecordedTrigger.Instance> recorded(MinMaxBounds.Ints sounds,
			MinMaxBounds.Ints slots, MinMaxBounds.Ints duration, List<ResourceLocation> heardAny) {
		return EchoCriteria.MEMORY_RECORDED.createCriterion(
				new MemoryRecordedTrigger.Instance(Optional.empty(), sounds, slots, duration, heardAny));
	}

	private static Criterion<MemoryPlayedTrigger.Instance> played(Optional<Boolean> watched,
			Optional<Boolean> sawSomeoneElse, Optional<Boolean> stoodInside, MinMaxBounds.Ints vision) {
		return EchoCriteria.MEMORY_PLAYED.createCriterion(new MemoryPlayedTrigger.Instance(
				Optional.empty(), watched, sawSomeoneElse, stoodInside, vision));
	}

	private static Criterion<LensTunedTrigger.Instance> tuned(MinMaxBounds.Ints vision,
			Optional<Boolean> everyTrackMaxed) {
		return EchoCriteria.LENS_TUNED.createCriterion(
				new LensTunedTrigger.Instance(Optional.empty(), vision, everyTrackMaxed));
	}

	private static Advancement.Builder step(ItemLike icon, String name, AdvancementType type) {
		return Advancement.Builder.advancement()
				.display(icon, title(name), description(name), null, type, true, true, false);
	}

	@Override
	public void generateAdvancement(HolderLookup.Provider lookup, Consumer<AdvancementHolder> consumer) {
		AdvancementHolder amethyst = Advancement.Builder.advancement()
				.display(Items.AMETHYST_SHARD, title("amethyst"), description("amethyst"),
						ResourceLocation.withDefaultNamespace("textures/block/sculk.png"),
						AdvancementType.TASK, true, true, false)
				.addCriterion("has_amethyst", InventoryChangeTrigger.TriggerInstance.hasItems(Items.AMETHYST_SHARD))
				.save(consumer, EchoesBeneath.id("amethyst").toString());

		AdvancementHolder echoShard = step(Items.ECHO_SHARD, "echo_shard", AdvancementType.TASK)
				.parent(amethyst)
				.addCriterion("has_echo_shard", InventoryChangeTrigger.TriggerInstance.hasItems(Items.ECHO_SHARD))
				.save(consumer, EchoesBeneath.id("echo_shard").toString());

		AdvancementHolder lens = step(EchoItems.ECHO_LENS, "echo_lens", AdvancementType.TASK)
				.parent(echoShard)
				.addCriterion("has_lens", InventoryChangeTrigger.TriggerInstance.hasItems(EchoItems.ECHO_LENS))
				.save(consumer, EchoesBeneath.id("echo_lens").toString());

		AdvancementHolder firstRecording = step(EchoItems.ECHO_LENS, "first_recording", AdvancementType.TASK)
				.parent(lens)
				.addCriterion("recorded", recorded(MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, List.of()))
				.save(consumer, EchoesBeneath.id("first_recording").toString());

		AdvancementHolder firstPlayback = step(Items.SCULK_SHRIEKER, "first_playback", AdvancementType.TASK)
				.parent(firstRecording)
				.addCriterion("played", played(Optional.empty(), Optional.empty(), Optional.empty(),
						MinMaxBounds.Ints.ANY))
				.save(consumer, EchoesBeneath.id("first_playback").toString());

		AdvancementHolder table = step(EchoBlocks.RESONANCE_TABLE_ITEM, "resonance_table", AdvancementType.TASK)
				.parent(firstPlayback)
				.addCriterion("has_table",
						InventoryChangeTrigger.TriggerInstance.hasItems(EchoBlocks.RESONANCE_TABLE_ITEM))
				.save(consumer, EchoesBeneath.id("resonance_table").toString());

		AdvancementHolder firstUpgrade = step(Items.AMETHYST_BLOCK, "first_upgrade", AdvancementType.TASK)
				.parent(table)
				.addCriterion("tuned", tuned(MinMaxBounds.Ints.ANY, Optional.empty()))
				.save(consumer, EchoesBeneath.id("first_upgrade").toString());

		step(Items.SCULK_SENSOR, "hold_still", AdvancementType.TASK)
				.parent(firstPlayback)
				.addCriterion("watched", played(Optional.of(true), Optional.empty(), Optional.empty(),
						MinMaxBounds.Ints.ANY))
				.save(consumer, EchoesBeneath.id("hold_still").toString());

		step(Items.PLAYER_HEAD, "witness", AdvancementType.TASK)
				.parent(firstPlayback)
				.addCriterion("saw_someone", played(Optional.empty(), Optional.of(true), Optional.empty(),
						MinMaxBounds.Ints.ANY))
				.save(consumer, EchoesBeneath.id("witness").toString());

		step(Items.IRON_PICKAXE, "the_other_way", AdvancementType.CHALLENGE)
				.parent(firstPlayback)
				.addCriterion("stood_inside", played(Optional.empty(), Optional.empty(), Optional.of(true),
						MinMaxBounds.Ints.ANY))
				.save(consumer, EchoesBeneath.id("the_other_way").toString());

		step(Items.SCULK_VEIN, "growing_sculk", AdvancementType.CHALLENGE)
				.parent(firstRecording)
				.addCriterion("silent", recorded(MinMaxBounds.Ints.exactly(0), MinMaxBounds.Ints.ANY,
						MinMaxBounds.Ints.atLeast(FULL_RECORDING_TICKS), List.of()))
				.save(consumer, EchoesBeneath.id("growing_sculk").toString());

		step(Items.ECHO_SHARD, "thunder_below", AdvancementType.CHALLENGE)
				.parent(firstRecording)
				.addCriterion("heard_warden", recorded(MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, WARDEN_SOUNDS))
				.save(consumer, EchoesBeneath.id("thunder_below").toString());

		step(EchoItems.ECHO_LENS, "three_times", AdvancementType.GOAL)
				.parent(firstUpgrade)
				.addCriterion("filled_every_slot", recorded(MinMaxBounds.Ints.ANY,
						MinMaxBounds.Ints.atLeast(EchoFormat.MAX_MEMORY_SLOTS), MinMaxBounds.Ints.ANY,
						List.of()))
				.save(consumer, EchoesBeneath.id("three_times").toString());

		step(Items.SPYGLASS, "echolocation", AdvancementType.GOAL)
				.parent(firstUpgrade)
				.addCriterion("saw_it_all", played(Optional.empty(), Optional.empty(), Optional.empty(),
						MinMaxBounds.Ints.atLeast(EchoLensUpgrades.MAX_VISION)))
				.save(consumer, EchoesBeneath.id("echolocation").toString());

		AdvancementHolder sounding = step(EchoBlocks.SOUNDING_TABLE_ITEM, "sounding_table",
						AdvancementType.TASK)
				.parent(firstPlayback)
				.addCriterion("has_sounding_table",
						InventoryChangeTrigger.TriggerInstance.hasItems(EchoBlocks.SOUNDING_TABLE_ITEM))
				.save(consumer, EchoesBeneath.id("sounding_table").toString());

		AdvancementHolder chart = step(EchoItems.ECHO_CHART, "echo_chart", AdvancementType.GOAL)
				.parent(sounding)
				.addCriterion("has_chart",
						InventoryChangeTrigger.TriggerInstance.hasItems(EchoItems.ECHO_CHART))
				.save(consumer, EchoesBeneath.id("echo_chart").toString());

		step(EchoItems.ECHO_CHART, "set_it_down", AdvancementType.GOAL)
				.parent(chart)
				.addCriterion("played_record", net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(
						net.minecraft.advancements.critereon.LocationPredicate.Builder.location().setBlock(
								net.minecraft.advancements.critereon.BlockPredicate.Builder.block().of(net.minecraft.world.level.block.Blocks.JUKEBOX)),
						net.minecraft.advancements.critereon.ItemPredicate.Builder.item().of(EchoItems.ECHO_CHART)))
				.save(consumer, EchoesBeneath.id("set_it_down").toString());

		step(Items.AMETHYST_CLUSTER, "tuner", AdvancementType.CHALLENGE)
				.parent(firstUpgrade)
				.addCriterion("maxed_everything", tuned(MinMaxBounds.Ints.ANY, Optional.of(true)))
				.save(consumer, EchoesBeneath.id("tuner").toString());
	}
}
