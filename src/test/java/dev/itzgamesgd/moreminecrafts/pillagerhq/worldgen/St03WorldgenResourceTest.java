package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class St03WorldgenResourceTest {
    private static final Path DATA=Path.of("src/main/resources/data/moreminecrafts");
    @Test void ownerApprovedRandomSpreadAndTerrainAdaptationAreEncodedInDatapack() throws Exception {
        var set=JsonParser.parseString(Files.readString(DATA.resolve("worldgen/structure_set/st03_pillager_headquarters.json"))).getAsJsonObject();
        var p=set.getAsJsonObject("placement");
        assertEquals(66,p.get("spacing").getAsInt());assertEquals(22,p.get("separation").getAsInt());
        assertEquals(91403,p.get("salt").getAsInt());assertEquals("triangular",p.get("spread_type").getAsString());
        assertEquals(St03PlacementConfig.SPACING_CHUNKS,p.get("spacing").getAsInt());
        assertEquals(St03PlacementConfig.SEPARATION_CHUNKS,p.get("separation").getAsInt());
        var structure=JsonParser.parseString(Files.readString(DATA.resolve("worldgen/structure/st03_pillager_headquarters.json"))).getAsJsonObject();
        assertEquals("none",structure.get("terrain_adaptation").getAsString());
        var biomes=structure.getAsJsonArray("biomes").asList().stream().map(e->e.getAsString()).toList();
        assertEquals(19,biomes.size());
        assertEquals(19,Set.copyOf(biomes).size(),"no duplicate biome");
        assertEquals(St03BiomeRules.ELIGIBLE_BIOME_IDS,biomes,"structure JSON and St03BiomeRules must stay identical");
        assertEquals(Set.of("minecraft:plains","minecraft:sunflower_plains","minecraft:meadow","minecraft:forest",
                "minecraft:flower_forest","minecraft:birch_forest","minecraft:old_growth_birch_forest","minecraft:dark_forest"),
                Set.copyOf(St03BiomeRules.ORIGINAL_BIOME_IDS));
        assertEquals(Set.of("minecraft:savanna","minecraft:savanna_plateau","minecraft:taiga","minecraft:snowy_plains",
                "minecraft:snowy_taiga","minecraft:dappled_forest","minecraft:pale_garden","minecraft:old_growth_pine_taiga",
                "minecraft:old_growth_spruce_taiga","minecraft:sparse_jungle","minecraft:wooded_badlands"),
                Set.copyOf(St03BiomeRules.ADDED_BIOME_IDS));
        for(String excluded:List.of("minecraft:desert","minecraft:swamp","minecraft:jungle","minecraft:badlands",
                "minecraft:eroded_badlands","minecraft:windswept_hills","minecraft:ocean","minecraft:river","minecraft:beach",
                "minecraft:cherry_grove","minecraft:ice_spikes","minecraft:mangrove_swamp","minecraft:bamboo_jungle"))
            assertEquals(false,biomes.contains(excluded),excluded+" is not approved");
    }
    @Test void approvedNbtAssetsRemainByteIdenticalToMeasurementBranch() throws Exception {
        var hashes=JsonParser.parseString(Files.readString(Path.of("src/test/resources/st03-approved-nbt-sha256.json"))).getAsJsonObject();
        assertEquals(25,hashes.size());
        for(var e:hashes.entrySet()) {
            byte[] bytes=Files.readAllBytes(DATA.resolve("structure/st03").resolve(e.getKey()));
            assertEquals(e.getValue().getAsString(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),e.getKey());
        }
    }
}
