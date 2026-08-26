package dev.fimkov.echoesbeneath.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.fimkov.echoesbeneath.client.chart.ClientChartShapes;
import dev.fimkov.echoesbeneath.client.chart.EchoChartModel;
import dev.fimkov.echoesbeneath.entity.EchoChartEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class EchoChartRenderer extends EntityRenderer<EchoChartEntity> {
	private static final ResourceLocation TEXTURE =
			ResourceLocation.withDefaultNamespace("textures/misc/white.png");
	private static final float MODEL_SIZE = 0.85F;
	private static final int ALPHA = 210;
	private static final float TILT_DEGREES = 12.0F;
	private static final float FLOOR_SHADE = 0.55F;
	private static final float WALL_SHADE = 0.78F;

	public EchoChartRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EchoChartEntity entity) {
		return TEXTURE;
	}

	@Override
	public void render(EchoChartEntity entity, float yaw, float partialTick, PoseStack poseStack,
			MultiBufferSource consumers, int light) {
		EchoChartModel model = ClientChartShapes.of(entity.getId());
		if (model == null || model.size() == 0) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.0D, entity.bob(partialTick), 0.0D);
		poseStack.mulPose(Axis.YP.rotationDegrees(entity.spin(partialTick)));
		poseStack.mulPose(Axis.XP.rotationDegrees(TILT_DEGREES));
		float scale = MODEL_SIZE / model.widest();
		poseStack.scale(scale, scale, scale);
		poseStack.translate(-model.spanX() * 0.5F, 0.0F, -model.spanZ() * 0.5F);

		VertexConsumer consumer = consumers.getBuffer(EchoRenderTypes.ECHO_TEXELS);
		Matrix4f pose = poseStack.last().pose();
		for (int index = 0; index < model.size(); index++) {
			int cell = model.cellAt(index);
			int x = EchoChartModel.cellX(cell);
			int y = EchoChartModel.cellY(cell);
			int z = EchoChartModel.cellZ(cell);
			int colour = model.colourAt(index);
			for (Direction face : Direction.values()) {
				if (model.holds(x + face.getStepX(), y + face.getStepY(), z + face.getStepZ())) {
					continue;
				}
				quad(consumer, pose, x, y, z, face, shade(colour, face));
			}
		}
		poseStack.popPose();
		super.render(entity, yaw, partialTick, poseStack, consumers, light);
	}

	private static int shade(int colour, Direction face) {
		float factor = switch (face) {
			case UP -> 1.0F;
			case DOWN -> FLOOR_SHADE;
			default -> WALL_SHADE;
		};
		int red = Mth.clamp(Math.round(((colour >> 16) & 0xFF) * factor), 0, 255);
		int green = Mth.clamp(Math.round(((colour >> 8) & 0xFF) * factor), 0, 255);
		int blue = Mth.clamp(Math.round((colour & 0xFF) * factor), 0, 255);
		return (red << 16) | (green << 8) | blue;
	}

	private static void quad(VertexConsumer consumer, Matrix4f pose, int x, int y, int z,
			Direction face, int colour) {
		float[] plane = new float[9];
		switch (face) {
			case DOWN -> plane(plane, x, y, z, 1, 0, 0, 0, 0, 1);
			case UP -> plane(plane, x, y + 1, z, 0, 0, 1, 1, 0, 0);
			case NORTH -> plane(plane, x, y, z, 0, 1, 0, 1, 0, 0);
			case SOUTH -> plane(plane, x, y, z + 1, 1, 0, 0, 0, 1, 0);
			case WEST -> plane(plane, x, y, z, 0, 0, 1, 0, 1, 0);
			case EAST -> plane(plane, x + 1, y, z, 0, 1, 0, 0, 0, 1);
		}
		int red = (colour >> 16) & 0xFF;
		int green = (colour >> 8) & 0xFF;
		int blue = colour & 0xFF;
		vertex(consumer, pose, plane[0], plane[1], plane[2], red, green, blue);
		vertex(consumer, pose, plane[0] + plane[3], plane[1] + plane[4], plane[2] + plane[5],
				red, green, blue);
		vertex(consumer, pose, plane[0] + plane[3] + plane[6], plane[1] + plane[4] + plane[7],
				plane[2] + plane[5] + plane[8], red, green, blue);
		vertex(consumer, pose, plane[0] + plane[6], plane[1] + plane[7], plane[2] + plane[8],
				red, green, blue);
	}

	private static void plane(float[] into, float x, float y, float z,
			float ax, float ay, float az, float bx, float by, float bz) {
		into[0] = x; into[1] = y; into[2] = z;
		into[3] = ax; into[4] = ay; into[5] = az;
		into[6] = bx; into[7] = by; into[8] = bz;
	}

	private static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z,
			int red, int green, int blue) {
		consumer.addVertex(pose, x, y, z).setColor(red, green, blue, ALPHA);
	}
}
