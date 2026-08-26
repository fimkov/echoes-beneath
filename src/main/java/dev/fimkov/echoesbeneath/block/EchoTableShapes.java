package dev.fimkov.echoesbeneath.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class EchoTableShapes {
	public static final double[][] RESONANCE_BOXES = {
			{0, 0, 0, 16, 10, 16},
			{0, 10, 0, 16, 12, 3},
			{0, 10, 13, 16, 12, 16},
			{0, 10, 3, 3, 12, 13},
			{13, 10, 3, 16, 12, 13},
			{3, 10, 3, 5, 12, 5},
			{11, 10, 3, 13, 12, 5},
			{3, 10, 11, 5, 12, 13},
			{11, 10, 11, 13, 12, 13}};

	public static final double[][] SOUNDING_BOXES = {
			{0, 0, 0, 16, 12, 16},
			{2, 12, 2, 14, 13, 14}};

	public static final VoxelShape RESONANCE = of(RESONANCE_BOXES);
	public static final VoxelShape SOUNDING = of(SOUNDING_BOXES);

	private EchoTableShapes() {
	}

	private static VoxelShape of(double[][] boxes) {
		VoxelShape shape = Shapes.empty();
		for (double[] box : boxes) {
			shape = Shapes.or(shape, Block.box(box[0], box[1], box[2], box[3], box[4], box[5]));
		}
		return shape;
	}
}
