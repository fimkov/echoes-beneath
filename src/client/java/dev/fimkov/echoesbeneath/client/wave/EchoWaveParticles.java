package dev.fimkov.echoesbeneath.client.wave;

import dev.fimkov.echoesbeneath.client.render.EchoFaceGeometry;
import dev.fimkov.echoesbeneath.client.render.EchoSectionMesh;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

public final class EchoWaveParticles {
	private static final int PARTICLE_BUDGET_PER_TICK = 38;
	private static final int FACE_SAMPLES_PER_SECTION = 26;
	private static final int ACCENT_EVERY = 7;
	private static final float CHARGE_ROLL_SCALE = 0.6F;
	private static final double NO_MOTION = 0.0D;

	private EchoWaveParticles() {
	}

	public static void emit(ClientLevel level, BlockPos center,
			Map<Integer, EchoSectionMesh> meshes, List<EchoWave> waves, int currentTick) {
		if (waves.isEmpty() || meshes.isEmpty()) {
			return;
		}
		RandomSource random = level.random;
		float[] basis = new float[9];
		int spawned = 0;

		for (Map.Entry<Integer, EchoSectionMesh> entry : meshes.entrySet()) {
			if (spawned >= PARTICLE_BUDGET_PER_TICK) {
				return;
			}
			EchoSectionMesh mesh = entry.getValue();
			int[] faces = mesh.faces();
			if (faces.length == 0) {
				continue;
			}
			for (int sample = 0; sample < FACE_SAMPLES_PER_SECTION; sample++) {
				if (spawned >= PARTICLE_BUDGET_PER_TICK) {
					return;
				}
				int packedFace = faces[random.nextInt(faces.length)];
				EchoFaceGeometry.faceBasis(packedFace, mesh.originX(), mesh.originY(), mesh.originZ(), basis);
				double x = center.getX() + basis[0] + (basis[3] + basis[6]) * 0.5D;
				double y = center.getY() + basis[1] + (basis[4] + basis[7]) * 0.5D;
				double z = center.getZ() + basis[2] + (basis[5] + basis[8]) * 0.5D;
				if (!touchedByWave(waves, currentTick, x, y, z)) {
					continue;
				}
				ParticleOptions particle = spawned % ACCENT_EVERY == 0
						? ParticleTypes.SCULK_SOUL
						: new SculkChargeParticleOptions(random.nextFloat() * CHARGE_ROLL_SCALE);
				level.addParticle(particle, x, y, z, NO_MOTION, NO_MOTION, NO_MOTION);
				spawned++;
			}
		}
	}

	private static boolean touchedByWave(List<EchoWave> waves, int currentTick, double x, double y, double z) {
		Vec3 point = new Vec3(x, y, z);
		for (EchoWave wave : waves) {
			float elapsed = wave.elapsedAt(currentTick, 0.0F);
			if (wave.depthOf(wave.origin().distanceTo(point), elapsed) >= 0.0F) {
				return true;
			}
		}
		return false;
	}
}
