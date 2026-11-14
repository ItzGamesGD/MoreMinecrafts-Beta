package dev.itzgamesgd.moreminecrafts.client.crystalguardian;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Vector3f;

/**
 * Builds a vanilla {@link ModelPart} tree from a "moreminecrafts:bone_model/1" JSON produced by
 * {@code tools/bbmodel/convert_bbmodel.py}. All coordinate/UV conversion was done by the converter; this class only turns explicit
 * vertices (model-space pixels + pixel UVs) into vanilla {@link ModelPart.Polygon}s, so per-face and mirrored UVs are preserved
 * exactly. Because the result is plain {@code ModelPart}s it renders through the unmodified vanilla entity pipeline.
 */
final class BoneModelLoader {
    static final String FORMAT = "moreminecrafts:bone_model/1";

    /** Loaded model: the root part plus every bone by name (root's single child is the top bone). */
    record Loaded(ModelPart root, Map<String, ModelPart> bones, int cubeCount, int textureWidth, int textureHeight) {}

    private BoneModelLoader() {}

    static Loaded load(ResourceManager resources, Identifier id) {
        try (Reader reader = resources.openAsReader(id)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read bone model " + id, e);
        }
    }

    static Loaded parse(JsonObject json) {
        String format = json.get("format").getAsString();
        if (!FORMAT.equals(format)) throw new IllegalStateException("Unsupported bone model format " + format);
        JsonObject tex = json.getAsJsonObject("texture");
        int texW = tex.get("uv_width").getAsInt(), texH = tex.get("uv_height").getAsInt();
        Map<String, JsonObject> byName = new LinkedHashMap<>();
        Map<String, List<JsonObject>> children = new LinkedHashMap<>();
        List<JsonObject> roots = new ArrayList<>();
        for (JsonElement e : json.getAsJsonArray("bones")) {
            JsonObject bone = e.getAsJsonObject();
            byName.put(bone.get("name").getAsString(), bone);
            if (bone.get("parent").isJsonNull()) roots.add(bone);
            else children.computeIfAbsent(bone.get("parent").getAsString(), k -> new ArrayList<>()).add(bone);
        }
        Map<String, ModelPart> parts = new LinkedHashMap<>();
        int[] cubeCount = {0};
        Map<String, ModelPart> rootChildren = new LinkedHashMap<>();
        for (JsonObject r : roots) rootChildren.put(r.get("name").getAsString(), build(r, null, children, parts, texW, texH, cubeCount));
        return new Loaded(new ModelPart(List.of(), rootChildren), parts, cubeCount[0], texW, texH);
    }

    private static ModelPart build(JsonObject bone, JsonObject parent, Map<String, List<JsonObject>> children,
                                   Map<String, ModelPart> registry, int texW, int texH, int[] cubeCount) {
        float[] pivot = floats(bone.getAsJsonArray("pivot"));
        List<ModelPart.Cube> cubes = new ArrayList<>();
        Map<String, ModelPart> sub = new LinkedHashMap<>();
        for (JsonElement ce : bone.getAsJsonArray("cubes")) {
            JsonObject cube = ce.getAsJsonObject();
            cubeCount[0]++;
            ModelPart.Cube built = cube(cube, texW, texH);
            if (cube.get("rotated").getAsBoolean()) {
                float[] o = floats(cube.getAsJsonArray("origin")), r = floats(cube.getAsJsonArray("rotation"));
                ModelPart cubePart = new ModelPart(List.of(built), Map.of());
                cubePart.setInitialPose(PartPose.offsetAndRotation(o[0] - pivot[0], o[1] - pivot[1], o[2] - pivot[2],
                        rad(r[0]), rad(r[1]), rad(r[2])));
                cubePart.resetPose();
                sub.put(cube.get("name").getAsString(), cubePart);
            } else {
                cubes.add(built);
            }
        }
        for (JsonObject child : children.getOrDefault(bone.get("name").getAsString(), List.of())) {
            sub.put(child.get("name").getAsString(), build(child, bone, children, registry, texW, texH, cubeCount));
        }
        ModelPart part = new ModelPart(cubes, sub);
        float[] off = floats(bone.getAsJsonArray("offset")), rot = floats(bone.getAsJsonArray("rotation"));
        part.setInitialPose(PartPose.offsetAndRotation(off[0], off[1], off[2], rad(rot[0]), rad(rot[1]), rad(rot[2])));
        part.resetPose();
        registry.put(bone.get("name").getAsString(), part);
        return part;
    }

    private static ModelPart.Cube cube(JsonObject cube, int texW, int texH) {
        JsonObject box = cube.getAsJsonObject("box");
        float[] min = floats(box.getAsJsonArray("min")), size = floats(box.getAsJsonArray("size"));
        JsonObject faces = cube.getAsJsonObject("faces");
        EnumSet<Direction> present = EnumSet.noneOf(Direction.class);
        for (String key : faces.keySet()) present.add(Direction.byName(key));
        ModelPart.Cube out = new ModelPart.Cube(0, 0, min[0], min[1], min[2], size[0], size[1], size[2], 0.0F, 0.0F, 0.0F,
                false, texW, texH, present);
        int i = 0;
        for (String key : faces.keySet()) {
            JsonObject face = faces.getAsJsonObject(key);
            float[] n = floats(face.getAsJsonArray("normal"));
            JsonArray verts = face.getAsJsonArray("vertices");
            ModelPart.Vertex[] vs = new ModelPart.Vertex[verts.size()];
            for (int v = 0; v < vs.length; v++) {
                float[] f = floats(verts.get(v).getAsJsonArray());
                vs[v] = new ModelPart.Vertex(f[0], f[1], f[2], f[3] / texW, f[4] / texH);
            }
            out.polygons[i++] = new ModelPart.Polygon(vs, new Vector3f(n[0], n[1], n[2]));
        }
        return out;
    }

    private static float[] floats(JsonArray a) {
        float[] out = new float[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsFloat();
        return out;
    }

    private static float rad(float degrees) { return (float) Math.toRadians(degrees); }
}
