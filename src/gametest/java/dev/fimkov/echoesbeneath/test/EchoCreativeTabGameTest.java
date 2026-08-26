package dev.fimkov.echoesbeneath.test;

import dev.fimkov.echoesbeneath.registry.EchoBlocks;
import dev.fimkov.echoesbeneath.registry.EchoItemGroups;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ItemLike;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class EchoCreativeTabGameTest implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void everyBlockIsReachable(GameTestHelper helper) {
		List<ItemLike> shown = EchoItemGroups.contents();
		List<String> missing = new ArrayList<>();

		for (Field field : EchoBlocks.class.getDeclaredFields()) {
			if (!Modifier.isPublic(field.getModifiers()) || !Modifier.isStatic(field.getModifiers())
					|| !BlockItem.class.isAssignableFrom(field.getType())) {
				continue;
			}
			try {
				if (!shown.contains((ItemLike) field.get(null))) {
					missing.add(field.getName());
				}
			} catch (IllegalAccessException unreachable) {
				helper.fail("could not read " + field.getName());
			}
		}

		if (!missing.isEmpty()) {
			helper.fail("not in the creative tab: " + missing);
		}

		helper.succeed();
	}
}
