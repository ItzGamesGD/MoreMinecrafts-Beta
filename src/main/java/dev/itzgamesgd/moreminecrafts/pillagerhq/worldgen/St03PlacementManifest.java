package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;

/**
 * Loads the 34 fixed ST-03 placements and 25 module definitions from
 * {@code data/moreminecrafts/st03_placements.json}.
 */
public final class St03PlacementManifest {
    private static final String RESOURCE_PATH = "/data/moreminecrafts/st03_placements.json";

    public record ModuleDef(String id, String file, int sizeX, int sizeY, int sizeZ, boolean includeAir, int order) {
        public Identifier templateId() {
            return Identifier.fromNamespaceAndPath("moreminecrafts", "st03/" + file);
        }
    }

    public record Placement(String placementId, ModuleDef module, int x0, int z0, int x1, int z1, int localTurns) {
        /** Pivot corner in the q=0 112x112 frame matching Minecraft's Rotation origin semantics. */
        public int pivotX() {
            return switch (Math.floorMod(localTurns, 4)) {
                case 1, 2 -> x1;
                default -> x0;
            };
        }

        public int pivotZ() {
            return switch (Math.floorMod(localTurns, 4)) {
                case 2, 3 -> z1;
                default -> z0;
            };
        }

        public Rotation effectiveRotation(Rotation compoundRotation) {
            int total = (localTurns + rotationTurns(compoundRotation)) % 4;
            return turnsToRotation(total);
        }

        public BlockPos worldPlacePos(BlockPos compoundOrigin, Rotation compoundRotation) {
            int[] xz = rotateLocalIn112(pivotX(), pivotZ(), compoundRotation);
            return compoundOrigin.offset(xz[0], 0, xz[1]);
        }
    }

    private static volatile List<Placement> cachedPlacements;

    private St03PlacementManifest() {}

    public static List<Placement> placements() {
        List<Placement> existing = cachedPlacements;
        if (existing != null) {
            return existing;
        }
        synchronized (St03PlacementManifest.class) {
            if (cachedPlacements == null) {
                cachedPlacements = loadFromResource();
            }
            return cachedPlacements;
        }
    }

    public static int rotationTurns(Rotation rotation) {
        return switch (rotation) {
            case NONE -> 0;
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
        };
    }

    public static Rotation turnsToRotation(int turns) {
        return switch (Math.floorMod(turns, 4)) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** Transforms a local (x, z) inside the 112x112 box [0..111] under whole-compound rotation. */
    public static int[] rotateLocalIn112(int lx, int lz, Rotation rotation) {
        int max = St03PlacementConfig.BOX_SIZE - 1;
        return switch (rotation) {
            case NONE -> new int[]{lx, lz};
            case CLOCKWISE_90 -> new int[]{max - lz, lx};
            case CLOCKWISE_180 -> new int[]{max - lx, max - lz};
            case COUNTERCLOCKWISE_90 -> new int[]{lz, max - lx};
        };
    }

    private static List<Placement> loadFromResource() {
        try (InputStream in = St03PlacementManifest.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource: " + RESOURCE_PATH);
            }
            JsonObject root = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
            Map<String, ModuleDef> modules = new HashMap<>();
            for (JsonElement elem : root.getAsJsonArray("modules")) {
                JsonObject m = elem.getAsJsonObject();
                JsonArray size = m.getAsJsonArray("size");
                String id = m.get("id").getAsString();
                modules.put(id, new ModuleDef(
                        id,
                        m.get("file").getAsString(),
                        size.get(0).getAsInt(),
                        size.get(1).getAsInt(),
                        size.get(2).getAsInt(),
                        "include".equals(m.get("air").getAsString()),
                        m.get("order").getAsInt()
                ));
            }
            List<Placement> list = new ArrayList<>();
            for (JsonElement elem : root.getAsJsonArray("placements")) {
                JsonObject p = elem.getAsJsonObject();
                String pid = p.get("placement").getAsString();
                String mid = p.get("module").getAsString();
                JsonArray rect = p.getAsJsonArray("rect");
                int turns = p.get("local_turns").getAsInt();
                ModuleDef def = modules.get(mid);
                if (def == null) {
                    throw new IllegalStateException("Unknown module id in st03_placements.json: " + mid);
                }
                list.add(new Placement(
                        pid,
                        def,
                        rect.get(0).getAsInt(),
                        rect.get(1).getAsInt(),
                        rect.get(2).getAsInt(),
                        rect.get(3).getAsInt(),
                        turns
                ));
            }
            list.sort(Comparator.comparingInt((Placement p) -> p.module().order()).thenComparing(Placement::placementId));
            return List.copyOf(list);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load " + RESOURCE_PATH, e);
        }
    }
}
