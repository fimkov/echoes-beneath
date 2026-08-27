package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.fimkov.echoesbeneath.entity.EchoLensEntity;
import dev.fimkov.echoesbeneath.entity.EchoLensMode;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class EchoLensEntityRenderer extends EntityRenderer<EchoLensEntity, EchoLensRenderState> {
	private static final float HOVER_BOB_AMPLITUDE = 0.08F;
	private static final float HOVER_BOB_SPEED = 0.08F;
	private static final float RENDER_SCALE = 1.15F;
	private static final int FULL_BRIGHT = 0x00F000F0;
	private static final float NUTATION_SPEED = 0.031F;
	private static final float NUTATION_DEGREES = 7.0F;
	private static final float BREATH_SPEED = 0.055F;
	private static final float BREATH_AMOUNT = 0.045F;
	private static final int HEARTBEAT_TICKS = 60;
	private static final float HEARTBEAT_DECAY = 7.0F;
	private static final float HEARTBEAT_AMOUNT = 0.13F;

	private final ItemModelResolver items;

	public EchoLensEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.items = context.getItemModelResolver();
		this.shadowRadius = 0.0F;
	}

	@Override
	public EchoLensRenderState createRenderState() {
		return new EchoLensRenderState();
	}

	@Override
	public void extractRenderState(EchoLensEntity entity, EchoLensRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		ItemStack stack = entity.getLens();
		state.hasLens = !stack.isEmpty();

		if (!state.hasLens) {
			state.lens.clear();

			return;
		}

		float age = entity.tickCount + partialTick;
		state.spin = entity.getSpin();
		state.bob = (float) Math.sin(age * HOVER_BOB_SPEED) * HOVER_BOB_AMPLITUDE;
		state.lean = (float) Math.sin(age * NUTATION_SPEED) * NUTATION_DEGREES;
		state.scale = RENDER_SCALE * (1.0F + (float) Math.sin(age * BREATH_SPEED) * BREATH_AMOUNT
				+ heartbeat(entity, age));
		items.updateForNonLiving(state.lens, stack, ItemDisplayContext.GROUND, entity);
	}

	@Override
	public void submit(EchoLensRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera) {
		if (!state.hasLens) {
			return;
		}

		poseStack.pushPose();
		poseStack.translate(0.0D, state.bob + 0.25D, 0.0D);
		poseStack.mulPose(Axis.YP.rotationDegrees(state.spin));
		poseStack.mulPose(Axis.XP.rotationDegrees(state.lean));
		poseStack.scale(state.scale, state.scale, state.scale);
		state.lens.submit(poseStack, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
				state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	private static float heartbeat(EchoLensEntity entity, float age) {
		if (entity.getMode() != EchoLensMode.RECORDING || entity.isArriving()) {
			return 0.0F;
		}

		float sinceBeat = (age % HEARTBEAT_TICKS) / HEARTBEAT_TICKS;

		return (float) Math.exp(-sinceBeat * HEARTBEAT_DECAY) * HEARTBEAT_AMOUNT;
	}
}
