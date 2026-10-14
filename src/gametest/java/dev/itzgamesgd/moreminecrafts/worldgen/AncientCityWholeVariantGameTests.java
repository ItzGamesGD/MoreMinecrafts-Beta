package dev.itzgamesgd.moreminecrafts.worldgen;

import dev.itzgamesgd.moreminecrafts.worldgen.structure.AncientCityVariant;
import dev.itzgamesgd.moreminecrafts.worldgen.structure.AncientCityVariantResolver;
import java.lang.reflect.Method;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;

public final class AncientCityWholeVariantGameTests implements FabricGameTest {
    @GameTest
    public void cityScopedSelectionIsDeterministicAndNonGlobal(GameTestHelper context) {
        long seed = context.getLevel().getSeed();
        var firstCity = new ChunkPos(485, -348);
        AncientCityVariant expected = AncientCityVariantResolver.resolve(seed, firstCity);

        for (int i = 0; i < 256; i++) {
            context.assertTrue(
                    AncientCityVariantResolver.resolve(seed, firstCity) == expected,
                    "One Ancient City identity must never change variant");
        }

        EnumSet<AncientCityVariant> observed = EnumSet.noneOf(AncientCityVariant.class);
        for (int i = 0; i < 256; i++) {
            observed.add(AncientCityVariantResolver.resolve(seed, new ChunkPos(i * 97 - 12000, i * 131 - 9000)));
        }

        context.assertTrue(observed.size() > 1,
                "Different Ancient City identities must be capable of different variants");
        context.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
