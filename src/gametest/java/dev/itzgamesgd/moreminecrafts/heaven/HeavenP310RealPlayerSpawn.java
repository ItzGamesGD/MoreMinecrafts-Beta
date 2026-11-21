package dev.itzgamesgd.moreminecrafts.heaven;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyIslandHabitat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/**
 * P3.10 opt-in real-player natural-spawn check ({@code -Dmoreminecrafts.heaven.p310=true}). A REAL client connects to a dedicated server whose world
 * (seed 42, same as the probe sites in {@code -Dmm.heaven.sitefile}) has the heaven pools enabled per {@code -Dmoreminecrafts.heaven.p310packs}.
 * For every listed site the player stands ~30 blocks from the island at midnight (survival rules: the player is NOT spectator), the island chunks
 * are loaded by that player, then (a) NaturalSpawner.spawnCategoryForChunk(MONSTER) is called N times per island chunk with the real candidate rules
 * and (b) the server's own per-tick natural spawning runs for {@code ticks} ticks, counting monsters that appear inside the island box.
 * Output JSON: {@code -Dmm.heaven.p310.out}.
 */
public final class HeavenP310RealPlayerSpawn implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        if (!Boolean.getBoolean("moreminecrafts.heaven.p310")) return;
        Map<String, Object> doc = new LinkedHashMap<>(); Map<String, Object> rows = new LinkedHashMap<>(); doc.put("sites", rows);
        doc.put("packs", String.valueOf(System.getProperty("moreminecrafts.heaven.p310packs")));
        try {
            JsonObject sites = JsonParser.parseString(Files.readString(Path.of(System.getProperty("mm.heaven.sitefile")))).getAsJsonObject();
            String[] want = System.getProperty("mm.heaven.p310.sites", "meadow,meadow#2,meadow#3,forest,forest#1,forest#3,mountain_meadow").split(",");
            int attempts = Integer.getInteger("mm.heaven.p310.attempts", 300), ticks = Integer.getInteger("mm.heaven.p310.ticks", 600);
            Properties props = new Properties();
            props.setProperty("online-mode", "false"); props.setProperty("view-distance", "8"); props.setProperty("simulation-distance", "8"); props.setProperty("max-tick-time", "-1");
            props.setProperty("spawn-animals", "false"); props.setProperty("spawn-monsters", "true"); props.setProperty("difficulty", "normal");
            try (TestDedicatedServerContext server = ctx.worldBuilder().setUseConsistentSettings(false).adjustSettings(st -> st.setSeed("42")).createServer(props);
                 var connection = server.connect()) {
                server.runCommand("gamemode survival @a"); server.runCommand("gamerule spawn_monsters true");
                server.runCommand("time set midnight"); server.runCommand("weather clear");
                for (String target : want) {
                    if (!sites.has(target)) { rows.put(target, "site missing"); continue; }
                    String[] xz = sites.get(target).getAsString().split(","); int sx = Integer.parseInt(xz[0]), sz = Integer.parseInt(xz[1]);
                    String base = target.split("#")[0];
                    // generate the island by moving the player next to it (production path: player proximity)
                    server.runCommand("tp @a " + (sx + 30) + " 200 " + sz); server.runCommand("effect give @a minecraft:resistance 1000000 4 true");
                    ctx.waitTicks(80);
                    Map<String, Object> row = new LinkedHashMap<>(); rows.put(target, row);
                    Map<String, Object> r = server.computeOnServer(s -> measure(s, base, sx, sz, attempts, row));
                    ctx.waitTicks(ticks);
                    Map<String, Object> natural = server.computeOnServer(s -> naturalCount(s, row));
                    row.putAll(natural);
                    MoreMinecrafts.LOGGER.info("HEAVEN_P310 {} {}", target, row);
                }
            }
        } catch (Throwable t) { doc.put("error", t.toString()); t.printStackTrace(); }
        try {
            Path out = Path.of(System.getProperty("mm.heaven.p310.out")); Files.createDirectories(out.toAbsolutePath().getParent());
            Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(doc));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static final Map<String, BoundingBox> BOXES = new LinkedHashMap<>();

    private static Map<String, Object> measure(net.minecraft.server.MinecraftServer server, String base, int sx, int sz, int attempts, Map<String, Object> row) {
        ServerLevel level = server.overworld();
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE); Identifier sid = Identifier.parse("moreminecrafts:sky_island_" + base);
        int cx = sx >> 4, cz = sz >> 4; level.getChunk(cx, cz, ChunkStatus.FULL, true);
        BoundingBox box = null;
        for (var st : level.structureManager().startsForStructure(cx, cz, x -> registry.getKey(x) != null && registry.getKey(x).equals(sid))) box = st.getBoundingBox();
        if (box == null) { row.put("error", "no start"); return row; }
        BOXES.put(sx + "," + sz, box); row.put("box", box.getXSpan() + "x" + box.getYSpan() + "x" + box.getZSpan());
        BoundingBox fb = box; Map<String, Integer> byType = new TreeMap<>(); int[] onHab = {0}, total = {0};
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
            var chunk = level.getChunk(x, z);
            for (int i = 0; i < attempts; i++)
                NaturalSpawner.spawnCategoryForChunk(MobCategory.MONSTER, level, chunk, (type, lvl, pos, ch) -> true, (mob, ch) -> {
                    total[0]++; byType.merge(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath(), 1, Integer::sum);
                    if (SkyIslandHabitat.find(level, mob.blockPosition()).isPresent()) onHab[0]++; mob.discard(); });
        }
        row.put("player_present", !level.players().isEmpty()); row.put("spawner_attempts_per_chunk", attempts);
        row.put("spawned_total", total[0]); row.put("spawned_on_habitat", onHab[0]); row.put("spawned_by_type", byType);
        return row;
    }

    private static Map<String, Object> naturalCount(net.minecraft.server.MinecraftServer server, Map<String, Object> row) {
        ServerLevel level = server.overworld(); Map<String, Object> out = new LinkedHashMap<>();
        BoundingBox box = BOXES.values().stream().reduce((a, b) -> b).orElse(null); if (box == null) return out;
        AABB a = new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1).inflate(2);
        Map<String, Integer> byType = new TreeMap<>(); int n = 0;
        for (Entity e : level.getEntities((Entity) null, a, x -> x instanceof Mob)) { n++; byType.merge(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath(), 1, Integer::sum); e.discard(); }
        out.put("natural_tick_mobs_in_box", n); out.put("natural_tick_by_type", byType); return out;
    }
}
