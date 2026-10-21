package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

// Everything a puppet submits is recoloured on the way through, which is what
// turns a normally lit model into a flat silhouette.
public record EchoSilhouetteCollector(SubmitNodeCollector root, OrderedSubmitNodeCollector delegate,
		EchoSilhouetteBuffers look, boolean textureless)
		implements SubmitNodeCollector {
	private static final Direction[] FACES = Direction.values();

	public EchoSilhouetteCollector(SubmitNodeCollector delegate, EchoSilhouetteBuffers look) {
		this(delegate, delegate, look, false);
	}

	public EchoSilhouetteCollector(SubmitNodeCollector delegate, EchoSilhouetteBuffers look, boolean textureless) {
		this(delegate, delegate, look, textureless);
	}

	@Override
	public SubmitNodeCollector order(int order) {
		return new EchoSilhouetteCollector(root, root.order(order), look, textureless);
	}

	@Override
	public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
			int light, int overlay, int colour, TextureAtlasSprite sprite, int outline,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		delegate.submitModel(model, state, poseStack, layerFor(renderType), look.light(), overlay,
				look.tint(), sprite, outline, crumbling);
	}

	@Override
	public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts,
			int[] tints, int light, int overlay, int outline) {
		if (textureless) {
			delegate.submitCustomGeometry(poseStack, EchoRenderTypes.ECHO_TEXELS, (pose, consumer) -> {
				for (BlockStateModelPart part : parts) {
					for (int side = 0; side <= FACES.length; side++) {
						Direction direction = side == FACES.length ? null : FACES[side];
						for (BakedQuad quad : part.getQuads(direction)) {
							for (int corner = 0; corner < 4; corner++) {
								var vertex = quad.position(corner);
								consumer.addVertex(pose.pose(), vertex.x(), vertex.y(), vertex.z())
										.setColor(0, 0, 0, 255);
								}
						}
					}
				}
			});
			return;
		}
		delegate.submitBlockModel(poseStack, layerFor(renderType), parts, tints, look.light(), overlay, outline);
	}

	@Override
	public void submitCustomGeometry(PoseStack poseStack, RenderType renderType,
			SubmitNodeCollector.CustomGeometryRenderer renderer) {
		delegate.submitCustomGeometry(poseStack, renderType,
				(pose, consumer) -> renderer.render(pose, look.wrap(consumer)));
	}

	private RenderType layerFor(RenderType submitted) {
		return look.layer() == null ? submitted : look.layer();
	}

	// The rest is decoration a silhouette has no use for, so it is dropped rather
	// than forwarded.
	@Override
	public void submitShadow(PoseStack poseStack, float alpha, List<EntityRenderState.ShadowPiece> pieces) {
	}

	@Override
	public void submitNameTag(PoseStack poseStack, Vec3 position, int light, Component name,
			boolean discrete, int outline, CameraRenderState camera) {
	}

	@Override
	public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text,
			boolean dropShadow, Font.DisplayMode mode, int light, int colour, int backdrop, int outline) {
	}

	@Override
	public void submitFlame(PoseStack poseStack, EntityRenderState state, org.joml.Quaternionf rotation) {
	}

	@Override
	public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leash) {
	}

	@Override
	public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState state, int light) {
	}

	@Override
	public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int light) {
	}

	@Override
	public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType,
			int colour, float lineWidth, boolean depthTest) {
	}

	@Override
	public void submitItem(PoseStack poseStack, ItemDisplayContext context, int light, int overlay,
			int outline, int[] tints, List<BakedQuad> quads, ItemStackRenderState.FoilType foil) {
	}

	@Override
	public void submitQuadParticleGroup(QuadParticleRenderState state) {
	}

	@Override
	public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera,
			boolean depthTest) {
	}
}
