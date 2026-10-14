package dev.itzgamesgd.moreminecrafts.worldgen;

import dev.itzgamesgd.moreminecrafts.worldgen.structure.AncientCityVariant;
import dev.itzgamesgd.moreminecrafts.worldgen.structure.AncientCityVariantResolver;
import java.util.EnumSet;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AncientCityVariantResolverTest {
    @Test
    void sameCityIsDeterministic() {
        long seed = 2632026L;
        ChunkPos city = new ChunkPos(485, -348);
        AncientCityVariant first = AncientCityVariantResolver.resolve(seed, city);
        for (int i = 0; i < 100; i++) {
            assertEquals(first, AncientCityVariantResolver.resolve(seed, city));
        }
    }

    @Test
    void queryOrderCannotChangeSelection() {
        long seed = 2632026L;
        ChunkPos a = new ChunkPos(485, -348);
        ChunkPos b = new ChunkPos(194, -423);

        AncientCityVariant aFirst = AncientCityVariantResolver.resolve(seed, a);
        AncientCityVariant bFirst = AncientCityVariantResolver.resolve(seed, b);

        for (int i = 0; i < 64; i++) {
            assertEquals(bFirst, AncientCityVariantResolver.resolve(seed, b));
            assertEquals(aFirst, AncientCityVariantResolver.resolve(seed, a));
        }
    }

    @Test
    void differentCitiesCanProduceDifferentVariants() {
        long seed = 2632026L;
        EnumSet<AncientCityVariant> observed = EnumSet.noneOf(AncientCityVariant.class);
        for (int x = -32; x <= 32; x++) {
            for (int z = -32; z <= 32; z++) {
                observed.add(AncientCityVariantResolver.resolve(seed, new ChunkPos(x * 37, z * 41)));
            }
        }
        assertEquals(EnumSet.allOf(AncientCityVariant.class), observed);
    }

    @Test
    void cityIdentityUsesStructureStartChunkOnly() {
        ChunkPos city = new ChunkPos(-12345, 67890);
        assertEquals(city.pack(), AncientCityVariantResolver.cityIdentity(city));
    }
}
