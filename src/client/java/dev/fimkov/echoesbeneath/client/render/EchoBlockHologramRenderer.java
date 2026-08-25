package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fimkov.echoesbeneath.client.config.EchoClientConfig;
import dev.fimkov.echoesbeneath.client.playback.ClientPlaybackSession;
import dev.fimkov.echoesbeneath.memory.EchoCoordinates;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockPalette;
import dev.fimkov.echoesbeneath.memory.snapshot.BlockSnapshot;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EchoBlockHologramRenderer {
	private static final int SCAN_HALF = 14;
	private static final int RESCAN_TICKS = 40;
	private static final double RESCAN_MOVE = 8.0D;

	private static ClientPlaybackSession scannedSession;
	private static final IntArrayList VANISHED = new IntArrayList();
	private static long lastScanTime = Long.MIN_VALUE;
	private static Vec3 lastScanOrigin = Vec3.ZERO;

	private EchoBlockHologramRenderer() {
	}

	public static void forget() {
		scannedSession = null;
		VANISHED.clear();
		lastScanTime = Long.MIN_VALUE;
	}

	public static void render(WorldRenderContext context, ClientPlaybackSession session, Vec3 cameraPosition) {
		Minecraft client = Minecraft.getInstance();
		Level level = client.level;
		if (context.consumers() == null || level == null) {
			return;
		}
		int budget = EchoClientConfig.values().hologramBlockBudget();
		double reach = EchoClientConfig.values().hologramDistance();
		if (budget <= 0 || !EchoClientConfig.values().showBlockHolograms()) {
			return;
		}
		BlockSnapshot snapshot = session.data().snapshot();
		EchoBlockDeltaState deltas = session.blockDeltas();
		BlockPalette palette = snapshot.palette();
		BlockRenderDispatcher blocks = client.getBlockRenderer();
		float presence = EchoDarkness.transitionProgress();
		if (presence <= 0.0F) {
			return;
		}
		EchoSilhouetteBuffers buffers = EchoSilhouetteBuffers.forBlocks(context.consumers(),
				EchoReveal.of(session), presence);
		BlockPos center = session.region().center();
		PoseStack poseStack = context.matrixStack();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		int drawn = 0;

		for (int packed : deltas.trackedPositions()) {
			if (drawn >= budget) {
				break;
			}
			int index = deltas.currentStates().getOrDefault(packed, BlockPalette.AIR_INDEX);
			if (index == BlockPalette.AIR_INDEX) {
				continue;
			}
			drawn += draw(context, level, blocks, buffers, poseStack, cursor, center, cameraPosition,
					packed, palette.stateAt(index), reach);
		}

		rescanIfStale(level, snapshot, deltas, center, cameraPosition, session, budget);
		for (int packed : VANISHED) {
			if (drawn >= budget) {
				break;
			}
			int index = snapshot.paletteIndexAt(EchoCoordinates.unpackX(packed),
					EchoCoordinates.unpackY(packed), EchoCoordinates.unpackZ(packed));
			if (index == BlockPalette.AIR_INDEX) {
				continue;
			}
			drawn += draw(context, level, blocks, buffers, poseStack, cursor, center, cameraPosition,
					packed, palette.stateAt(index), reach);
		}
	}

	private static int draw(WorldRenderContext context, Level level, BlockRenderDispatcher blocks,
			EchoSilhouetteBuffers buffers, PoseStack poseStack, BlockPos.MutableBlockPos cursor,
			BlockPos center, Vec3 cameraPosition, int packed, BlockState remembered, double reach) {
		if (remembered.isAir()) {
			return 0;
		}
		cursor.set(center.getX() + EchoCoordinates.unpackX(packed),
				center.getY() + EchoCoordinates.unpackY(packed),
				center.getZ() + EchoCoordinates.unpackZ(packed));
		if (cameraPosition.distanceToSqr(cursor.getX() + 0.5D, cursor.getY() + 0.5D, cursor.getZ() + 0.5D)
				> reach * reach) {
			return 0;
		}
		if (level.getBlockState(cursor) == remembered) {
			return 0;
		}
		if (context.frustum() != null && !context.frustum().isVisible(new AABB(cursor))) {
			return 0;
		}
		poseStack.pushPose();
		poseStack.translate(cursor.getX() - cameraPosition.x,
				cursor.getY() - cameraPosition.y, cursor.getZ() - cameraPosition.z);
		blocks.renderSingleBlock(remembered, poseStack, buffers, 0, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
		return 1;
	}

	private static void rescanIfStale(Level level, BlockSnapshot snapshot, EchoBlockDeltaState deltas,
			BlockPos center, Vec3 cameraPosition, ClientPlaybackSession session, int budget) {
		long now = level.getGameTime();
		boolean stale = session != scannedSession
				|| now - lastScanTime >= RESCAN_TICKS
				|| cameraPosition.distanceToSqr(lastScanOrigin) > RESCAN_MOVE * RESCAN_MOVE;
		if (!stale) {
			return;
		}
		scannedSession = session;
		lastScanTime = now;
		lastScanOrigin = cameraPosition;
		VANISHED.clear();

		int originX = (int) Math.floor(cameraPosition.x) - center.getX();
		int originY = (int) Math.floor(cameraPosition.y) - center.getY();
		int originZ = (int) Math.floor(cameraPosition.z) - center.getZ();
		int radius = snapshot.radius();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dy = -SCAN_HALF; dy <= SCAN_HALF; dy++) {
			int relativeY = originY + dy;
			if (relativeY < snapshot.minRelativeY() || relativeY > snapshot.maxRelativeY()) {
				continue;
			}
			for (int dx = -SCAN_HALF; dx <= SCAN_HALF; dx++) {
				int relativeX = originX + dx;
				if (Math.abs(relativeX) > radius) {
					continue;
				}
				for (int dz = -SCAN_HALF; dz <= SCAN_HALF; dz++) {
					int relativeZ = originZ + dz;
					if (Math.abs(relativeZ) > radius
							|| relativeX * relativeX + relativeZ * relativeZ > radius * radius) {
						continue;
					}
					int packed = EchoCoordinates.packRelative(relativeX, relativeY, relativeZ);
					if (deltas.trackedPositions().contains(packed)) {
						continue;
					}
					int index = snapshot.paletteIndexAt(relativeX, relativeY, relativeZ);
					if (index == BlockPalette.AIR_INDEX) {
						continue;
					}
					cursor.set(center.getX() + relativeX, center.getY() + relativeY, center.getZ() + relativeZ);
					if (level.getBlockState(cursor) == snapshot.palette().stateAt(index)) {
						continue;
					}
					VANISHED.add(packed);
					if (VANISHED.size() >= budget) {
						return;
					}
				}
			}
		}
	}
}
