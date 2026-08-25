package dev.fimkov.echoesbeneath.item;

import dev.fimkov.echoesbeneath.EchoTranslations;
import dev.fimkov.echoesbeneath.memory.EchoMemoryLocator;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReference;
import dev.fimkov.echoesbeneath.memory.EchoMemoryReferences;
import dev.fimkov.echoesbeneath.memory.EchoTextFormat;
import dev.fimkov.echoesbeneath.recording.EchoRecordingHooks;
import dev.fimkov.echoesbeneath.registry.EchoSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class EchoLensResonance {
	private static final int PARTICLE_INTERVAL_TICKS = 25;
	private static final int SIGNAL_INTERVAL_TICKS = 120;
	private static final int PARTICLE_COUNT = 2;
	private static final double PARTICLE_SPREAD = 0.4D;
	private static final float RESONANCE_VOLUME = 0.25F;
	private static final float RESONANCE_PITCH = 1.6F;

	private EchoLensResonance() {
	}

	public static void tick(ItemStack stack, Level level, Entity holder, boolean selected) {
		if (!selected || level.isClientSide() || !(holder instanceof ServerPlayer player)) {
			return;
		}
		EchoMemoryReferences references = EchoLensStacks.references(stack);
		if (references.isEmpty()) {
			return;
		}
		long gameTime = level.getGameTime();
		boolean particleBeat = gameTime % PARTICLE_INTERVAL_TICKS == 0;
		boolean signalBeat = gameTime % SIGNAL_INTERVAL_TICKS == 0;
		if (!particleBeat && !signalBeat) {
			return;
		}
		Optional<EchoMemoryReference> resonating = EchoMemoryLocator.resonating(
				references, level.dimension(), player.blockPosition());
		if (resonating.isEmpty()) {
			return;
		}
		ServerLevel serverLevel = player.serverLevel();
		if (particleBeat) {
			serverLevel.sendParticles(player, ParticleTypes.SCULK_SOUL, false,
					player.getX(), player.getEyeY(), player.getZ(),
					PARTICLE_COUNT, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPREAD, 0.0D);
		}
		if (signalBeat) {
			announce(serverLevel, player, resonating.get());
		}
	}

	private static void announce(ServerLevel serverLevel, ServerPlayer player, EchoMemoryReference reference) {
		EchoRecordingHooks.runWithoutCapture(() -> serverLevel.playSound(null,
				reference.center().getX() + 0.5D, reference.center().getY() + 0.5D, reference.center().getZ() + 0.5D,
				EchoSounds.RESONANCE_PING, SoundSource.AMBIENT, RESONANCE_VOLUME, RESONANCE_PITCH));
	}
}
