package dev.itzgamesgd.moreminecrafts.worldgenprobe.st01p8a;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.boss.TombGuardianEntities;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ST-01 P8-A live placement probe (validation only; inert unless {@code -Dmm.st01.p8a.out} is set).
 *
 * <p>Phase {@code place}: loads {@code moreminecrafts:pyramid/royal_hall} through the game's StructureTemplateManager,
 * places it at the approved origin with {@code StructureTemplate.placeInWorld}, asserts the placed world against an
 * independent parse of the committed NBT, asserts PRIMARY chest / Guardian / route facts in the live world, saves, and
 * records a SHA-256 over every block state in the volume.
 *
 * <p>Phase {@code reload}: starts a new server process on the saved world without placing anything, reasserts the same
 * facts and compares the state hash with the value recorded by the {@code place} phase.
 *
 * <p>Every assertion is reported with its evidence; nothing is relaxed. The process halts after writing the JSON result.
 */
public final class RoyalHallPlacementProbe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st01-p8a-probe");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static final Identifier TEMPLATE_ID = Identifier.fromNamespaceAndPath("moreminecrafts", "pyramid/royal_hall");
    static final String NBT_RESOURCE = "data/moreminecrafts/structure/pyramid/royal_hall.nbt";
    /** Origin of the 39x20x39 reservation (P2/P3/P6/P8-A verifier MIN_CORNER); local (x,y,z) maps to origin+(x,y,z). */
    static final BlockPos ORIGIN = new BlockPos(-40, -24, 2);
    static final BlockPos CHEST_LEFT = new BlockPos(-21, -20, 33);
    static final BlockPos CHEST_RIGHT = new BlockPos(-20, -20, 33);
    static final BlockPos GUARDIAN_3B = new BlockPos(-21, -22, 26);
    static final int SIZE_X = 39, SIZE_Y = 20, SIZE_Z = 39;
    static final double PLAYER_WIDTH = 0.6, PLAYER_HEIGHT = 1.8, STEP_UP = 0.6, STEP_DOWN = 1.0;

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.st01.p8a.out");
        if (out == null) return;
        String phase = System.getProperty("mm.st01.p8a.phase", "place");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("phase", phase);
            try {
                run(server, phase, result);
            } catch (Throwable t) {
                LOG.error("ST01 P8-A probe failed", t);
                result.put("error", t.toString());
                StringBuilder sb = new StringBuilder();
                for (StackTraceElement e : t.getStackTrace()) sb.append(e).append('\n');
                result.put("error_stack", sb.toString());
            }
            try {
                Path p = Path.of(out);
                if (p.getParent() != null) Files.createDirectories(p.getParent());
                Files.writeString(p, GSON.toJson(result), StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOG.error("could not write probe output", e);
            }
            try {
                server.saveEverything(true, true, true);
            } catch (Throwable ignored) {
                // a save failure is recorded by the reload phase (hash mismatch / missing chest)
            }
            Runtime.getRuntime().halt(0);
        });
    }

    // ------------------------------------------------------------------------------------------------------------------

    private static final class Checks {
        final List<Map<String, Object>> list = new ArrayList<>();

        void add(String id, String title, boolean ok, String evidence) {
            add(id, title, ok ? "PASS" : "FAIL", evidence);
        }

        void add(String id, String title, String verdict, String evidence) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("title", title);
            m.put("verdict", verdict);
            m.put("evidence", evidence);
            list.add(m);
        }

        boolean allPass() {
            return list.stream().allMatch(m -> "PASS".equals(m.get("verdict")));
        }
    }

    /** Independent parse of the committed NBT: block states keyed by world position (origin applied). */
    static Map<BlockPos, BlockState> parseNbt(MinecraftServer server, CompoundTag[] sizeOut) throws IOException {
        CompoundTag root;
        try (InputStream in = RoyalHallPlacementProbe.class.getClassLoader().getResourceAsStream(NBT_RESOURCE)) {
            if (in == null) throw new IOException("resource missing: " + NBT_RESOURCE);
            root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        }
        HolderGetter<Block> blocks = server.registryAccess().lookupOrThrow(Registries.BLOCK);
        ListTag paletteTag = root.getListOrEmpty("palette");
        List<BlockState> palette = new ArrayList<>();
        for (int i = 0; i < paletteTag.size(); i++) palette.add(NbtUtils.readBlockState(blocks, paletteTag.getCompoundOrEmpty(i)));
        Map<BlockPos, BlockState> out = new HashMap<>();
        ListTag entries = root.getListOrEmpty("blocks");
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag e = entries.getCompoundOrEmpty(i);
            ListTag pos = e.getListOrEmpty("pos");
            BlockPos local = new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0));
            out.put(ORIGIN.offset(local), palette.get(e.getIntOr("state", -1)));
        }
        if (sizeOut != null) sizeOut[0] = root;
        return out;
    }

    static String sha256(Map<BlockPos, BlockState> states) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        List<BlockPos> keys = new ArrayList<>(states.keySet());
        keys.sort((a, b) -> a.asLong() < b.asLong() ? -1 : a.asLong() > b.asLong() ? 1 : 0);
        for (BlockPos p : keys) {
            md.update((p.getX() + "," + p.getY() + "," + p.getZ() + "=" + states.get(p) + "\n").getBytes(StandardCharsets.UTF_8));
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static void run(MinecraftServer server, String phase, Map<String, Object> result) throws Exception {
        ServerLevel level = server.overworld();
        boolean place = "place".equals(phase);
        Checks checks = new Checks();

        // ---- 1. template load + placement (place phase only) -------------------------------------------------------
        if (place) {
            StructureTemplate template = level.getStructureTemplateManager().get(TEMPLATE_ID).orElse(null);
            checks.add("P8A_LIVE_TEMPLATE_LOAD", "Template loads from the game's StructureTemplateManager",
                    template != null, template == null ? "missing " + TEMPLATE_ID : "loaded " + TEMPLATE_ID);
            if (template == null) throw new IllegalStateException("template not loaded");
            var size = template.getSize();
            checks.add("P8A_LIVE_TEMPLATE_SIZE", "Template size equals the 39x20x39 reservation",
                    size.getX() == SIZE_X && size.getY() == SIZE_Y && size.getZ() == SIZE_Z,
                    "size=" + size.getX() + "x" + size.getY() + "x" + size.getZ());
            boolean placed = template.placeInWorld(level, ORIGIN, ORIGIN, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
            checks.add("P8A_LIVE_PLACE_RETURN", "StructureTemplate.placeInWorld returns true at origin " + ORIGIN,
                    placed, "returned=" + placed + " origin=" + ORIGIN + " rotation=NONE mirror=NONE");
        }

        // ---- 2. voxel comparison against the independent NBT parse -------------------------------------------------
        Map<BlockPos, BlockState> expected = parseNbt(server, null);
        Map<BlockPos, BlockState> actual = new HashMap<>();
        Map<String, Integer> mismatchByPair = new LinkedHashMap<>();
        int mismatches = 0;
        List<String> mismatchSamples = new ArrayList<>();
        Map<String, Integer> materials = new LinkedHashMap<>();
        for (var e : expected.entrySet()) {
            BlockPos p = e.getKey();
            BlockState want = e.getValue();
            BlockState got = level.getBlockState(p);
            actual.put(p, got);
            materials.merge(BlockStateNames.id(got), 1, Integer::sum);
            if (!want.equals(got)) {
                mismatches++;
                mismatchByPair.merge(BlockStateNames.id(want) + " -> " + BlockStateNames.id(got), 1, Integer::sum);
                if (mismatchSamples.size() < 12) mismatchSamples.add(p + " want=" + want + " got=" + got);
            }
        }
        checks.add("P8A_LIVE_VOXEL_MATCH", "Every block in the 39x20x39 volume matches the committed NBT (exact BlockState)",
                mismatches == 0,
                "volume=" + expected.size() + " mismatches=" + mismatches + (mismatchByPair.isEmpty() ? "" : " pairs=" + mismatchByPair)
                        + (mismatchSamples.isEmpty() ? "" : " samples=" + mismatchSamples));
        result.put("voxel_mismatches", mismatches);
        result.put("voxel_mismatch_pairs", mismatchByPair);
        result.put("materials_in_world", materials);
        result.put("state_sha256", sha256(actual));

        // ---- 3. PRIMARY B chest: states, pairing, container, obstruction ------------------------------------------
        BlockState left = level.getBlockState(CHEST_LEFT), right = level.getBlockState(CHEST_RIGHT);
        boolean leftIsChest = left.is(Blocks.CHEST), rightIsChest = right.is(Blocks.CHEST);
        boolean leftState = left.getValue(ChestBlock.FACING).getName().equals("north") && left.getValue(ChestBlock.TYPE) == net.minecraft.world.level.block.state.properties.ChestType.LEFT;
        boolean rightState = right.getValue(ChestBlock.FACING).getName().equals("north") && right.getValue(ChestBlock.TYPE) == net.minecraft.world.level.block.state.properties.ChestType.RIGHT;
        checks.add("P8A_LIVE_CHEST_STATES", "PRIMARY B halves: minecraft:chest, facing=north, type left at (-21,-20,33) / right at (-20,-20,33)",
                leftIsChest && rightIsChest && leftState && rightState,
                "left=" + left + " right=" + right);

        BlockEntity leftBe = level.getBlockEntity(CHEST_LEFT), rightBe = level.getBlockEntity(CHEST_RIGHT);
        checks.add("P8A_LIVE_CHEST_BLOCK_ENTITIES", "Both halves have a ChestBlockEntity",
                leftBe instanceof ChestBlockEntity && rightBe instanceof ChestBlockEntity,
                "left=" + (leftBe == null ? "null" : leftBe.getClass().getSimpleName()) + " right=" + (rightBe == null ? "null" : rightBe.getClass().getSimpleName()));

        boolean paired = ChestBlock.getConnectedBlockPos(CHEST_LEFT, left).equals(CHEST_RIGHT)
                && ChestBlock.getConnectedBlockPos(CHEST_RIGHT, right).equals(CHEST_LEFT);
        // Vanilla maps type=right -> BlockType.FIRST and type=left -> BlockType.SECOND; the two halves must be complementary, neither SINGLE.
        boolean typesOk = ChestBlock.getBlockType(left) == DoubleBlockCombiner.BlockType.SECOND
                && ChestBlock.getBlockType(right) == DoubleBlockCombiner.BlockType.FIRST;
        checks.add("P8A_LIVE_CHEST_PAIRING", "Vanilla ChestBlock pairing: halves point at each other and combine as a double chest (left=SECOND, right=FIRST)",
                paired && typesOk,
                "connected(left)=" + ChestBlock.getConnectedBlockPos(CHEST_LEFT, left) + " connected(right)=" + ChestBlock.getConnectedBlockPos(CHEST_RIGHT, right)
                        + " blockType(left)=" + ChestBlock.getBlockType(left) + " blockType(right)=" + ChestBlock.getBlockType(right));

        Container container = leftIsChest ? ChestBlock.getContainer((ChestBlock) left.getBlock(), left, level, CHEST_LEFT, false) : null;
        int size = container == null ? -1 : container.getContainerSize();
        checks.add("P8A_LIVE_CHEST_CONTAINER_54", "Chest inventory resolves to one 54-slot container from either half",
                container != null && size == 54,
                "ChestBlock.getContainer(left)=" + (container == null ? "null" : container.getClass().getSimpleName() + " size=" + size));
        Container fromRight = ChestBlock.getContainer((ChestBlock) right.getBlock(), right, level, CHEST_RIGHT, false);
        checks.add("P8A_LIVE_CHEST_CONTAINER_FROM_RIGHT", "Right half resolves to the same 54-slot double container",
                fromRight != null && fromRight.getContainerSize() == 54 && fromRight.getClass().equals(container == null ? null : container.getClass()),
                "ChestBlock.getContainer(right)=" + (fromRight == null ? "null" : fromRight.getClass().getSimpleName() + " size=" + fromRight.getContainerSize()));

        boolean blockedLeft = ChestBlock.isChestBlockedAt(level, CHEST_LEFT), blockedRight = ChestBlock.isChestBlockedAt(level, CHEST_RIGHT);
        checks.add("P8A_LIVE_CHEST_NOT_OBSTRUCTED", "No solid block above either half (vanilla opening obstruction check passes)",
                !blockedLeft && !blockedRight,
                "isChestBlockedAt left=" + blockedLeft + " right=" + blockedRight + " above(left)=" + level.getBlockState(CHEST_LEFT.above())
                        + " above(right)=" + level.getBlockState(CHEST_RIGHT.above()));

        // ---- 4. Guardian 3B anchor: floor, 3x5x3 clearance, headroom, entity collision ---------------------------
        BlockPos floor = GUARDIAN_3B.below();
        BlockState floorState = level.getBlockState(floor);
        boolean floorSturdy = floorState.isFaceSturdy(level, floor, Direction.UP);
        checks.add("P8A_LIVE_GUARDIAN_FLOOR", "Guardian 3B anchor (-21,-22,26) stands on a sturdy floor at y=-23",
                floorSturdy, "floor=" + floorState + " sturdyUp=" + floorSturdy);

        int air3x5x3 = 0, total3x5x3 = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 4; dy++) for (int dz = -1; dz <= 1; dz++) {
            total3x5x3++;
            if (level.getBlockState(GUARDIAN_3B.offset(dx, dy, dz)).isAir()) air3x5x3++;
        }
        checks.add("P8A_LIVE_GUARDIAN_CLEARANCE_3x5x3", "3x5x3 clearance above the Guardian 3B anchor is all air",
                air3x5x3 == total3x5x3, "air=" + air3x5x3 + "/" + total3x5x3 + " (x -22..-20, y -22..-18, z 25..27)");

        int headroom = 0;
        for (int y = GUARDIAN_3B.getY(); y < ORIGIN.getY() + SIZE_Y; y++) {
            if (!level.getBlockState(new BlockPos(GUARDIAN_3B.getX(), y, GUARDIAN_3B.getZ())).isAir()) break;
            headroom++;
        }
        checks.add("P8A_LIVE_GUARDIAN_HEADROOM", "Air headroom above the Guardian 3B anchor is at least 10 blocks",
                headroom >= 10, "contiguous air blocks from anchor upward (to template top): " + headroom);

        AABB guardianBox = TombGuardianEntities.TOMB_GUARDIAN.getDimensions().makeBoundingBox(new Vec3(GUARDIAN_3B.getX() + 0.5, GUARDIAN_3B.getY(), GUARDIAN_3B.getZ() + 0.5));
        boolean guardianFree = level.noCollision(guardianBox);
        checks.add("P8A_LIVE_GUARDIAN_COLLISION", "Tomb Guardian entity box at the anchor has no collision with the structure",
                guardianFree, "box=" + guardianBox + " noCollision=" + guardianFree
                        + " dims=" + TombGuardianEntities.TOMB_GUARDIAN.getDimensions());

        // ---- 5. player routes: vanilla PathNavigation (real game pathfinder, 0.6 x 1.95 walker with step-up) -------------
        BlockPos doorway = new BlockPos(-21, -22, 3);
        int doorOpen = 0;
        for (int dy = -22; dy <= -20; dy++) for (int dx = -22; dx <= -20; dx++) {
            if (level.getBlockState(new BlockPos(dx, dy, 3)).isAir()) doorOpen++;
        }
        checks.add("P8A_LIVE_ROUTE_DOORWAY", "Doorway opening (x -22..-20, y -22..-20, z 2..4) is clear air in the live world",
                doorOpen == 9, "doorway open cells at z=3: " + doorOpen + "/9");
        Nav arena = navigate(level, doorway, GUARDIAN_3B);
        checks.add("P8A_LIVE_ROUTE_ARENA", "Vanilla pathfinding from the doorway reaches the Guardian 3B arena anchor (-21,-22,26)",
                arena.reachable ? "PASS" : "FAIL", arena.describe());
        Nav chest = navigate(level, doorway, new BlockPos(-21, -20, 32));
        checks.add("P8A_LIVE_ROUTE_CHEST", "Vanilla pathfinding from the doorway reaches the cell north of the PRIMARY chest (-21,-20,32)",
                verdictFor(chest), chest.describe());
        Nav dais = navigate(level, doorway, new BlockPos(-17, -20, 34));
        checks.add("P8A_LIVE_ROUTE_DAIS", "Vanilla pathfinding from the doorway reaches the dais top (-17,-20,34) without a 1-block jump",
                verdictFor(dais), dais.describe());
        int stairsExact = 0, stairsNear = 0, stairsTotal = 0;
        double worstStairRise = 0;
        for (int x = -25; x <= -17; x++) for (int z = 30; z <= 31; z++) {
            stairsTotal++;
            Nav n = navigate(level, doorway, new BlockPos(x, -21, z));
            if (n.reachable) stairsExact++;
            if (n.near) stairsNear++;
            worstStairRise = Math.max(worstStairRise, n.maxRise);
        }
        String stairVerdict = stairsExact == stairsTotal && worstStairRise <= 0.6 + 1e-6 ? "PASS"
                : stairsNear == stairsTotal ? "PARTIAL" : "FAIL";
        checks.add("P8A_LIVE_ROUTE_STAIRS", "Vanilla pathfinding reaches the top of all 18 frontal dais stair cells (x -25..-17, z 30..31)",
                stairVerdict, "exact=" + stairsExact + "/" + stairsTotal + " withinTolerance=" + stairsNear + "/" + stairsTotal
                        + " max node rise=" + worstStairRise);
        result.put("route", Map.of("arena", arena.describe(), "chest", chest.describe(), "dais", dais.describe(),
                "stairs_exact", stairsExact + "/" + stairsTotal));

        // ---- 6. save/reload persistence -------------------------------------------------------------------------------
        if (place) {
            String hashFile = System.getProperty("mm.st01.p8a.hash");
            if (hashFile != null) Files.writeString(Path.of(hashFile), (String) result.get("state_sha256"), StandardCharsets.UTF_8);
            result.put("placed_hash_recorded", hashFile != null);
        } else {
            Object recorded = readRecordedHash();
            boolean same = recorded != null && recorded.equals(result.get("state_sha256"));
            checks.add("P8A_LIVE_RELOAD_HASH", "Reloaded world block states hash equals the hash recorded before save",
                    same, "recorded=" + recorded + " reloaded=" + result.get("state_sha256"));
        }

        result.put("checks", checks.list);
        result.put("all_pass", checks.allPass());
        LOG.info("ST01 P8-A probe phase={} all_pass={}", phase, checks.allPass());
    }

    // The place phase writes its hash to a sidecar file next to the output; the reload phase reads it back.
    static Object readRecordedHash() {
        String f = System.getProperty("mm.st01.p8a.hash");
        if (f == null) return null;
        try {
            return Files.readString(Path.of(f)).trim();
        } catch (IOException e) {
            return null;
        }
    }

    /** One vanilla navigation query: reachable, path node count, and the largest upward node step on the path (metres). */
    record Nav(boolean reachable, boolean near, int nodes, double maxRise, BlockPos target, String note) {
        String describe() {
            return "target=" + target + " exact=" + reachable + " withinTolerance=" + near + " nodes=" + nodes + " maxRise=" + maxRise + " [" + note + "]";
        }
    }

    /**
     * Vanilla pathfinding (Mob navigation) from {@code from} to {@code to}, using a live zombie-sized walker standing at the
     * source. A node step above 0.6 m is a jump (1-block ledge), which a player can do but a plain walk cannot.
     */
    static Nav navigate(ServerLevel level, BlockPos from, BlockPos to) {
        var walker = new net.minecraft.world.entity.monster.zombie.Zombie(level);
        walker.setPos(from.getX() + 0.5, from.getY(), from.getZ() + 0.5);
        // PathNavigation.canUpdatePath() requires a grounded mob; the walker is never ticked, so declare it grounded on its floor.
        walker.setOnGround(true);
        StringBuilder dbg = new StringBuilder();
        dbg.append("fromChunkLoaded=").append(level.hasChunk(from.getX() >> 4, from.getZ() >> 4))
           .append(" toChunkLoaded=").append(level.hasChunk(to.getX() >> 4, to.getZ() >> 4))
           .append(" onGround=").append(walker.onGround())
           .append(" followRange=").append(walker.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE))
           .append(" nav=").append(walker.getNavigation().getClass().getSimpleName());
        var self = walker.getNavigation().createPath(from, 0);
        dbg.append(" selfPath=").append(self == null ? "null" : self.getNodeCount() + "/" + self.canReach());
        var acc = walker.getNavigation().createPath(to, 8);
        dbg.append(" acc8Path=").append(acc == null ? "null" : acc.getNodeCount() + "/" + acc.canReach());
        var path = walker.getNavigation().createPath(to, 0);
        dbg.append(" acc0Path=").append(path == null ? "null" : path.getNodeCount() + "/" + path.canReach());
        boolean near = acc != null && acc.canReach();
        if (path == null) return new Nav(false, near, 0, 0, to, dbg.toString());
        double maxRise = 0;
        for (int i = 1; i < path.getNodeCount(); i++) {
            maxRise = Math.max(maxRise, path.getNode(i).y - path.getNode(i - 1).y);
        }
        return new Nav(path.canReach(), near, path.getNodeCount(), maxRise, to, dbg.toString());
    }

    /**
     * PASS: exact target reached by navigation with no node rise above 0.6 (walk).
     * PARTIAL: exact target reached only with a >0.6 node rise, or the exact cell is not reached but a point within tolerance is.
     * FAIL: neither. The result is navigation evidence only; live player movement is judged by the client capture.
     */
    static String verdictFor(Nav n) {
        if (n.reachable) return n.maxRise <= 0.6 + 1e-6 ? "PASS" : "PARTIAL";
        return n.near ? "PARTIAL" : "FAIL";
    }

    /** Readable block ids without properties (for material counts). */
    static final class BlockStateNames {
        static String id(BlockState s) {
            return BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        }
    }
}
