package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fimkov.echoesbeneath.EchoesBeneath;
import dev.fimkov.echoesbeneath.entity.EchoLensEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Axis;

public class EchoLensEntityRenderer extends EntityRenderer<EchoLensEntity> {
	private static final float HOVER_BOB_AMPLITUDE = 0.08F;
	private static final float HOVER_BOB_SPEED = 0.08F;
	private static final float RENDER_SCALE = 1.15F;
	private static final int FULL_BRIGHT = 0x00F000F0;

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
		float bob = (float) Math.sin((entity.tickCount + partialTick) * HOVER_BOB_SPEED) * HOVER_BOB_AMPLITUDE;
		poseStack.pushPose();
		poseStack.translate(0.0D, bob + 0.25D, 0.0D);
		poseStack.mulPose(Axis.YP.rotationDegrees(entity.getSpin()));
		poseStack.scale(RENDER_SCALE, RENDER_SCALE, RENDER_SCALE);
		itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, FULL_BRIGHT,
				net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
				poseStack, buffers, entity.level(), entity.getId());
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(EchoLensEntity entity) {
		return EchoesBeneath.id("textures/item/echo_lens.png");
	}
}
