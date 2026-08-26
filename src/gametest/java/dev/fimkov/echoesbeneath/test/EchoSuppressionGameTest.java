package dev.fimkov.echoesbeneath.test;

import dev.fimkov.echoesbeneath.playback.suppression.EchoSuppressedEntity;
import dev.fimkov.echoesbeneath.playback.suppression.HostileMobSuppressionManager;
import dev.fimkov.echoesbeneath.playback.suppression.SuppressedMobState;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

public class EchoSuppressionGameTest implements FabricGameTest {
	private static final BlockPos SPAWN = new BlockPos(1, 2, 1);
	private static final float WOUNDED_HEALTH = 12.0F;
	private static final int EFFECT_DURATION_TICKS = 400;
	private static final int FIRE_TICKS = 60;
	private static final double POSITION_TOLERANCE = 0.05D;

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void hostileMobIsSuppressedAndFullyRestored(GameTestHelper helper) {
		HostileMobSuppressionManager manager =
				new HostileMobSuppressionManager(helper.getLevel().getServer());
		Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, SPAWN);
		zombie.setHealth(WOUNDED_HEALTH);
		zombie.setRemainingFireTicks(FIRE_TICKS);
		zombie.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION_TICKS));
		Vec3 originalPosition = zombie.position();

		SuppressedMobState state = manager.suppress(zombie)
				.orElseThrow(() -> new AssertionError("the zombie was not suppressed"));

		if (!zombie.isInvisible()) {
			helper.fail("a suppressed mob must be invisible", zombie);
		}

		if (!zombie.isSilent()) {
			helper.fail("a suppressed mob must be silent", zombie);
		}

		if (!zombie.isInvulnerable()) {
			helper.fail("a suppressed mob must be invulnerable", zombie);
		}

		if (!zombie.isNoAi()) {
			helper.fail("a suppressed mob must have no AI", zombie);
		}

		if (zombie.isPushable()) {
			helper.fail("a suppressed mob must not collide", zombie);
		}

		if (zombie.isPickable()) {
			helper.fail("a suppressed mob must not be targetable", zombie);
		}

		if (!(zombie instanceof EchoSuppressedEntity marked) || !marked.echoesBeneath$isSuppressed()) {
			helper.fail("a suppressed mob must carry the suppression marker", zombie);
		}

		manager.restoreEntity(zombie, state);

		if (zombie.isInvisible() || zombie.isSilent() || zombie.isInvulnerable() || zombie.isNoAi()) {
			helper.fail("a restored mob must return to its recorded flags", zombie);
		}

		if (zombie.getHealth() != WOUNDED_HEALTH) {
			helper.fail("a restored mob must keep its health, found " + zombie.getHealth(), zombie);
		}

		if (zombie.getRemainingFireTicks() != FIRE_TICKS) {
			helper.fail("a restored mob must keep its fire ticks", zombie);
		}

		if (!zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
			helper.fail("a restored mob must keep its effects", zombie);
		}

		if (zombie.position().distanceTo(originalPosition) > POSITION_TOLERANCE) {
			helper.fail("a restored mob must return to its recorded position", zombie);
		}

		if (zombie instanceof EchoSuppressedEntity restored && restored.echoesBeneath$isSuppressed()) {
			helper.fail("a restored mob must lose the suppression marker", zombie);
		}

		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void emergencyRecoveryReleasesEverySuppressedMob(GameTestHelper helper) {
		HostileMobSuppressionManager manager =
				new HostileMobSuppressionManager(helper.getLevel().getServer());
		Zombie first = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, SPAWN);
		Zombie second = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, SPAWN.offset(2, 0, 0));
		manager.suppress(first);
		manager.suppress(second);

		if (manager.pendingCount() < 2) {
			helper.fail("both mobs should be remembered for recovery");
		}

		manager.recoverEverything();

		if (first.isInvisible() || second.isInvisible()) {
			helper.fail("recovery must make every suppressed mob visible again");
		}

		if (manager.pendingCount() != 0) {
			helper.fail("recovery must clear the pending suppression store");
		}

		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void passiveMobsAreNeverSuppressed(GameTestHelper helper) {
		if (HostileMobSuppressionManager.isSuppressible(helper.spawn(EntityType.COW, SPAWN))) {
			helper.fail("a cow must not be treated as a suppressible hostile mob");
		}

		if (!HostileMobSuppressionManager.isSuppressible(

				helper.spawnWithNoFreeWill(EntityType.ZOMBIE, SPAWN.offset(2, 0, 0)))) {
			helper.fail("a zombie must be treated as a suppressible hostile mob");
		}

		helper.succeed();
	}
}
