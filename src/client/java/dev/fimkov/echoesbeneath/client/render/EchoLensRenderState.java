package dev.fimkov.echoesbeneath.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class EchoLensRenderState extends EntityRenderState {
	public final ItemStackRenderState lens = new ItemStackRenderState();
	public boolean hasLens;
	public float spin;
	public float bob;
	public float lean;
	public float scale;
}
