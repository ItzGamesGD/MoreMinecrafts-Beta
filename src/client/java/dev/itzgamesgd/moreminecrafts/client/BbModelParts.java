package dev.itzgamesgd.moreminecrafts.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.core.Direction;

/**
 * Builds a {@link ModelPart} tree from the bone JSON exported by scripts/bbmodel-capture/convert.py (Blockbench Bedrock
 * models with per-face UV, which {@code CubeListBuilder} cannot express).
 *
 * <p>Bedrock to Java: x and y are negated and the model is lifted so the feet sit at y = 24 (the vanilla layout),
 * z is unchanged; east/west faces swap with the x flip and rotations about x and y change sign. Every bone is a direct
 * child of the root at its pivot, so vanilla humanoid animation (head, body, arms, legs) works on the bones.
 */
final class BbModelParts {
    private BbModelParts() {}

    /** Root with the exported bones as children; {@code withHat} adds the empty "hat" child vanilla humanoid models read. */
    static ModelPart build(String name, boolean withHat) {
        try (InputStream in = BbModelParts.class.getResourceAsStream("/assets/moreminecrafts/bbmodel/" + name + ".json")) {
            if (in == null) throw new IllegalStateException("missing bbmodel/" + name + ".json");
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            int w = json.get("w").getAsInt(), h = json.get("h").getAsInt();
            Map<String, ModelPart> bones = new LinkedHashMap<>();
            for (JsonElement boneElement : json.getAsJsonArray("bones")) {
                JsonObject bone = boneElement.getAsJsonObject();
                float[] pivot = array(bone.getAsJsonArray("pivot"));
                float px = -pivot[0], py = 24.0F - pivot[1], pz = pivot[2];
                Map<String, ModelPart> children = new LinkedHashMap<>();
                int index = 0;
                for (JsonElement element : bone.getAsJsonArray("elements")) {
                    children.put("e" + index++, element(element.getAsJsonObject(), w, h, px, py, pz));
                }
                if (withHat && bone.get("name").getAsString().equals("head")) {
                    children.put("hat", new ModelPart(List.of(), Map.of()));
                }
                ModelPart part = new ModelPart(List.of(), children);
                PartPose pose = PartPose.offset(px, py, pz);
                part.setInitialPose(pose);
                part.loadPose(pose);
                bones.put(bone.get("name").getAsString(), part);
            }
            ModelPart root = new ModelPart(List.of(), bones);
            root.setInitialPose(PartPose.ZERO);
            return root;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static float[] array(JsonArray a) {
        return new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }

    /** One cube, positioned relative to its bone (bx, by, bz are the bone's Java-space position). */
    private static ModelPart element(JsonObject e, int texW, int texH, float bx, float by, float bz) {
        float[] from = array(e.getAsJsonArray("f")), to = array(e.getAsJsonArray("t"));
        float[] pivot = array(e.getAsJsonArray("o")), rot = array(e.getAsJsonArray("r"));
        float inflate = e.get("i").getAsFloat();
        float x = -(to[0] - pivot[0]), y = -(to[1] - pivot[1]), z = from[2] - pivot[2];
        ModelPart.Cube cube = new ModelPart.Cube(0, 0, x, y, z, to[0] - from[0], to[1] - from[1], to[2] - from[2],
                inflate, inflate, inflate, false, texW, texH, EnumSet.allOf(Direction.class));
        JsonObject uvs = e.getAsJsonObject("uv");
        for (int i = 0; i < cube.polygons.length; i++) {
            ModelPart.Polygon old = cube.polygons[i];
            Direction dir = Direction.getApproximateNearest(old.normal().x(), old.normal().y(), old.normal().z());
            String key = switch (dir) {
                case EAST -> "west";
                case WEST -> "east";
                default -> dir.getName();
            };
            JsonArray uv = uvs.getAsJsonArray(key);
            cube.polygons[i] = new ModelPart.Polygon(old.vertices(), uv.get(0).getAsFloat(), uv.get(1).getAsFloat(),
                    uv.get(2).getAsFloat(), uv.get(3).getAsFloat(), texW, texH, false, dir);
        }
        ModelPart part = new ModelPart(new ArrayList<>(List.of(cube)), Map.of());
        PartPose pose = PartPose.offsetAndRotation(-pivot[0] - bx, 24.0F - pivot[1] - by, pivot[2] - bz,
                (float) Math.toRadians(-rot[0]), (float) Math.toRadians(-rot[1]), (float) Math.toRadians(rot[2]));
        part.setInitialPose(pose);
        part.loadPose(pose);
        return part;
    }
}
