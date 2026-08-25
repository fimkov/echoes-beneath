package dev.fimkov.echoesbeneath.test;

import dev.fimkov.echoesbeneath.entity.EchoLensEntity;
import dev.fimkov.echoesbeneath.entity.EchoLensMode;
import dev.fimkov.echoesbeneath.item.EchoLensStacks;
import dev.fimkov.echoesbeneath.memory.EchoFormat;
import dev.fimkov.echoesbeneath.registry.EchoItems;
import dev.fimkov.echoesbeneath.registry.EchoTags;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class EchoLensGameTest implements FabricGameTest {
	private static final BlockPos ANCHOR = new BlockPos(1, 2, 1);

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void aCraftedLensStartsWithNoMemories(GameTestHelper helper) {
		ItemStack lens = new ItemStack(EchoItems.ECHO_LENS);
		if (EchoLensStacks.references(lens).filledSlots() != 0) {
			helper.fail("a crafted lens must hold no memories");
		}
		if (!EchoLensStacks.references(lens).nextFreeSlot(
				EchoLensStacks.upgrades(lens).memorySlots()).orElseThrow().equals(0)) {
			helper.fail("a fresh lens must record into Memory I first");
		}
		if (EchoLensStacks.identityOrNull(lens) != null) {
			helper.fail("a crafted lens must not carry an identity before it is used");
		}
		if (EchoLensStacks.upgrades(lens).memorySlots() != EchoFormat.DEFAULT_MEMORY_SLOTS) {
			helper.fail("a fresh lens must start with a single memory slot");
		}
		if (EchoFormat.MAX_MEMORY_SLOTS != 3) {
			helper.fail("a fully upgraded lens must top out at three memory slots");
		}
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void aDeployedLensCarriesTheSameStack(GameTestHelper helper) {
		ItemStack lens = new ItemStack(EchoItems.ECHO_LENS);
		UUID identity = EchoLensStacks.identityOrAssign(lens);
		Vec3 position = Vec3.atCenterOf(helper.absolutePos(ANCHOR));
		EchoLensEntity deployed = new EchoLensEntity(helper.getLevel(), position, lens,
				UUID.randomUUID(), UUID.randomUUID(), EchoLensMode.RECORDING);
		helper.getLevel().addFreshEntity(deployed);

		if (!identity.equals(EchoLensStacks.identityOrNull(deployed.getLens()))) {
			helper.fail("a deployed lens must keep the identity of the stack it came from");
		}
		if (deployed.getMode() != EchoLensMode.RECORDING) {
			helper.fail("a deployed lens must be in recording mode");
		}
		ItemStack returned = deployed.takeLens();
		if (!identity.equals(EchoLensStacks.identityOrNull(returned))) {
			helper.fail("the returned stack must be the same lens");
		}
		if (!deployed.getLens().isEmpty()) {
			helper.fail("a lens can only be taken once, so no duplicate can be produced");
		}
		deployed.discard();
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void theLensEntityIsNeverRecordedOrDamaged(GameTestHelper helper) {
		ItemStack lens = new ItemStack(EchoItems.ECHO_LENS);
		Vec3 position = Vec3.atCenterOf(helper.absolutePos(ANCHOR));
		EchoLensEntity deployed = new EchoLensEntity(helper.getLevel(), position, lens,
				UUID.randomUUID(), UUID.randomUUID(), EchoLensMode.RECORDING);
		helper.getLevel().addFreshEntity(deployed);

		if (!deployed.getType().is(EchoTags.RECORDING_BLACKLIST)) {
			helper.fail("the lens entity must sit in the recording blacklist tag");
		}
		if (deployed.isPushable()) {
			helper.fail("the lens entity must not be pushable");
		}
		if (deployed.hurt(helper.getLevel().damageSources().generic(), 100.0F)) {
			helper.fail("the lens entity must not take damage");
		}
		deployed.discard();
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void displayEntitiesAreExcludedFromRecordings(GameTestHelper helper) {
		if (!EntityType.MARKER.is(EchoTags.RECORDING_BLACKLIST)) {
			helper.fail("marker entities must never be recorded");
		}
		if (!EntityType.BLOCK_DISPLAY.is(EchoTags.RECORDING_BLACKLIST)) {
			helper.fail("display entities must never be recorded");
		}
		if (EntityType.ZOMBIE.is(EchoTags.RECORDING_BLACKLIST)) {
			helper.fail("ordinary mobs must be recorded");
		}
		helper.succeed();
	}
}
