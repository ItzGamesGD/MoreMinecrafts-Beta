package dev.itzgamesgd.moreminecrafts.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Draws a pre-baked pose of a Blockbench model as an item model ({@code moreminecrafts:pose_mesh} special model).
 *
 * <p>The poses come from {@code assets/moreminecrafts/bbmodel/pose/<model>.json}, written by the offline keyframe baker:
 * every entry is one textured quad (4 vertices in block units, centred on the model, plus UV and normal). Hand and GUI
 * placement comes from the {@code base} model's display transforms in the item definition.
 */
public final class PoseMeshRenderer implements NoDataSpecialModelRenderer {
    private record Quad(float[] v, float[] uv, float[] n) {}

    private static final Map<String, Map<String, List<Quad>>> CACHE = new HashMap<>();

    private final List<Quad> quads;
    private final RenderType renderType;

    private PoseMeshRenderer(List<Quad> quads, Identifier texture) {
        this.quads = quads;
        this.renderType = RenderTypes.entityCutout(texture);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outlineColor) {
        collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
            for (Quad q : quads) {
                for (int i = 0; i < 4; i++) {
                    consumer.addVertex(pose, q.v[i * 3], q.v[i * 3 + 1], q.v[i * 3 + 2])
                            .setColor(-1)
                            .setUv(q.uv[i * 2], q.uv[i * 2 + 1])
                            .setOverlay(overlay)
                            .setLight(light)
                            .setNormal(pose, q.n[0], q.n[1], q.n[2]);
                }
            }
        });
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (Quad q : quads) {
            for (int i = 0; i < 4; i++) output.accept(new Vector3f(q.v[i * 3], q.v[i * 3 + 1], q.v[i * 3 + 2]));
        }
    }

    private static synchronized List<Quad> load(String model, String pose) {
        Map<String, List<Quad>> poses = CACHE.computeIfAbsent(model, PoseMeshRenderer::loadModel);
        List<Quad> quads = poses.get(pose);
        if (quads == null) throw new IllegalStateException("pose " + pose + " missing in bbmodel/pose/" + model + ".json");
        return quads;
    }

    private static Map<String, List<Quad>> loadModel(String model) {
        try (InputStream in = PoseMeshRenderer.class.getResourceAsStream("/assets/moreminecrafts/bbmodel/pose/" + model + ".json")) {
            if (in == null) throw new IllegalStateException("missing bbmodel/pose/" + model + ".json");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, List<Quad>> result = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("poses").entrySet()) {
                List<Quad> quads = new ArrayList<>();
                for (JsonElement quadElement : entry.getValue().getAsJsonArray()) {
                    JsonObject quad = quadElement.getAsJsonObject();
                    float[] v = new float[12], uv = new float[8], n = new float[3];
                    JsonArray vs = quad.getAsJsonArray("v"), uvs = quad.getAsJsonArray("uv"), ns = quad.getAsJsonArray("n");
                    for (int i = 0; i < 4; i++) {
                        for (int k = 0; k < 3; k++) v[i * 3 + k] = vs.get(i).getAsJsonArray().get(k).getAsFloat();
                        for (int k = 0; k < 2; k++) uv[i * 2 + k] = uvs.get(i).getAsJsonArray().get(k).getAsFloat();
                    }
                    for (int k = 0; k < 3; k++) n[k] = ns.get(k).getAsFloat();
                    quads.add(new Quad(v, uv, n));
                }
                result.put(entry.getKey(), quads);
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Item definition entry: {@code {"type":"moreminecrafts:pose_mesh","model":"<name>","pose":"<pose>","texture":"<id>"}}. */
    public record Unbaked(String model, String pose, Identifier texture) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("model").forGetter(Unbaked::model),
                Codec.STRING.fieldOf("pose").forGetter(Unbaked::pose),
                Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture)
        ).apply(instance, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() { return MAP_CODEC; }

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
            return new PoseMeshRenderer(load(model, pose), texture);
        }
    }
}
