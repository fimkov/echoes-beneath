package dev.fimkov.echoesbeneath;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fimkov.echoesbeneath.client.render.EchoSilhouetteBuffers;
import dev.fimkov.echoesbeneath.client.render.EchoSilhouetteCollector;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoSilhouetteCollectorTest {
	private static final EchoSilhouetteBuffers LOOK =
			new EchoSilhouetteBuffers(null, 12, 34, 56, 255, 7, false);

	@Test
	void entityLayersCanSelectAVanillaRenderOrder() {
		SubmitNodeStorage storage = new SubmitNodeStorage();
		SubmitNodeCollector silhouette = new EchoSilhouetteCollector(storage, LOOK);
		assertFalse(storage.order(1) instanceof SubmitNodeCollector);
		EchoSilhouetteCollector ordered = (EchoSilhouetteCollector) silhouette.order(1);
		assertSame(storage.order(1), ordered.delegate());
		assertSame(LOOK, ordered.look());
	}

	@Test
	void reorderingKeepsTheRootCollectorAndChartStyle() {
		SubmitNodeStorage storage = new SubmitNodeStorage();
		SubmitNodeCollector silhouette = new EchoSilhouetteCollector(storage, LOOK, true);
		EchoSilhouetteCollector ordered = (EchoSilhouetteCollector) silhouette.order(1);
		EchoSilhouetteCollector reordered = (EchoSilhouetteCollector) ordered.order(2);
		assertSame(storage.order(2), reordered.delegate());
		assertSame(storage, reordered.root());
		assertTrue(reordered.textureless());
	}

	@Test
	void orderedEntityLayersKeepTheirSilhouetteTintAndLight() {
		AtomicReference<Object[]> submitted = new AtomicReference<>();
		OrderedSubmitNodeCollector layer = (OrderedSubmitNodeCollector) Proxy.newProxyInstance(
				getClass().getClassLoader(), new Class<?>[]{OrderedSubmitNodeCollector.class},
				(proxy, method, args) -> {
					if (method.getName().equals("submitModel")) {
						submitted.set(args);
					}
					return null;
				});
		SubmitNodeCollector root = (SubmitNodeCollector) Proxy.newProxyInstance(
				getClass().getClassLoader(), new Class<?>[]{SubmitNodeCollector.class},
				(proxy, method, args) -> {
					if (method.getName().equals("order")) {
						return layer;
					}
					throw new AssertionError("Layer submission bypassed its render order");
				});
		new EchoSilhouetteCollector(root, LOOK).order(1).submitModel(
				null, new Object(), new PoseStack(), null, 123, 456, 789, null, 0, null);
		assertNotNull(submitted.get());
		assertEquals(LOOK.light(), submitted.get()[4]);
		assertEquals(456, submitted.get()[5]);
		assertEquals(LOOK.tint(), submitted.get()[6]);
	}
}
