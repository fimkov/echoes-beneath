package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.entity.EchoLensEntity;
import dev.fimkov.echoesbeneath.entity.EchoLensMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class EchoLensEntityRenderer extends EntityRenderer<EchoLensEntity> {
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

	private final ItemRenderer itemRenderer;

	public EchoLensEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemRenderer = context.getItemRenderer();
		this.shadowRadius = 0.0F;
	}

	@Override
	public void render(EchoLensEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
			MultiBufferSource buffers, int packedLight) {
		ItemStack stack = entity.getLens();

		if (stack.isEmpty()) {
			return;
		}

		float age = entity.tickCount + partialTick;
		float bob = (float) Math.sin(age * HOVER_BOB_SPEED) * HOVER_BOB_AMPLITUDE;
		float lean = (float) Math.sin(age * NUTATION_SPEED) * NUTATION_DEGREES;
		float scale = RENDER_SCALE * (1.0F + (float) Math.sin(age * BREATH_SPEED) * BREATH_AMOUNT
				+ heartbeat(entity, age));
		poseStack.pushPose();
		poseStack.translate(0.0D, bob + 0.25D, 0.0D);
		poseStack.mulPose(Axis.YP.rotationDegrees(entity.getSpin()));
		poseStack.mulPose(Axis.XP.rotationDegrees(lean));
		poseStack.scale(scale, scale, scale);
		itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, FULL_BRIGHT,
				OverlayTexture.NO_OVERLAY,
				poseStack, buffers, entity.level(), entity.getId());
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	private static float heartbeat(EchoLensEntity entity, float age) {
		if (entity.getMode() != EchoLensMode.RECORDING || entity.isArriving()) {
			return 0.0F;
		}

		float sinceBeat = (age % HEARTBEAT_TICKS) / HEARTBEAT_TICKS;

		return (float) Math.exp(-sinceBeat * HEARTBEAT_DECAY) * HEARTBEAT_AMOUNT;
	}

	@Override
	public Identifier getTextureLocation(EchoLensEntity entity) {
		return EchoesBeneath.id("textures/item/echo_lens.png");
	}
}
