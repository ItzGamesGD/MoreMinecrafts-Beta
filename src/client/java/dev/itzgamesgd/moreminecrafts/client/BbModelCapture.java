package dev.itzgamesgd.moreminecrafts.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyNormalMobs;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/**
 * Capture-only static renderer for Blockbench (Bedrock, per-face UV) models, opt-in via
 * {@code -Dmoreminecrafts.sky.bbmodel.capture=true}. It shows the modelled geometry exactly as exported (no animation,
 * no bow pose) so the three remodelled Sky Mobs can be reviewed in a vanilla client. Data is produced by
 * scripts/bbmodel-capture/convert.py.
 *
 * <p>Bedrock to Java: x and y are negated (y is down in model space, root lifted by 24), z is unchanged; the
 * east/west faces swap with the x flip, and rotations about x and y change sign.
 */
final class BbModelCapture {
    private BbModelCapture() {}

    static final boolean ENABLED = Boolean.getBoolean("moreminecrafts.sky.bbmodel.capture");

    static void register() {
        add(SkyNormalMobs.CRYSTAL_BUG, "crystal_bug");
        add(SkyNormalMobs.SKYGUARD_ARCHER, "skyguard_archer");
        add(SkyNormalMobs.FALLEN_SENTINEL, "fallen_sentinel");
    }

    private static <T extends Mob> void add(EntityType<T> type, String name) {
        Identifier texture = ModIds.id("textures/entity/bb_" + name + ".png");
        EntityRendererRegistry.register(type, context -> new Renderer<T>(context, load(name), texture));
    }

    static final class State extends LivingEntityRenderState {}

    static final class Model extends EntityModel<State> {
        Model(ModelPart root) {
            super(root);
        }
    }

    static final class Renderer<T extends Mob> extends MobRenderer<T, State, Model> {
        private final Identifier texture;

        Renderer(EntityRendererProvider.Context context, ModelPart root, Identifier texture) {
            super(context, new Model(root), 0.5F);
            this.texture = texture;
        }

        @Override
        public State createRenderState() {
            return new State();
        }

        @Override
        public Identifier getTextureLocation(State state) {
            return texture;
        }
    }

    private static ModelPart load(String name) {
        try (InputStream in = BbModelCapture.class.getResourceAsStream("/assets/moreminecrafts/bbcapture/" + name + ".json")) {
            if (in == null) throw new IllegalStateException("missing bbcapture/" + name + ".json");
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            int w = json.get("w").getAsInt(), h = json.get("h").getAsInt();
            Map<String, ModelPart> children = new HashMap<>();
            int index = 0;
            for (JsonElement element : json.getAsJsonArray("elements")) {
                children.put("e" + index++, part(element.getAsJsonObject(), w, h));
            }
            ModelPart root = new ModelPart(List.of(), children);
            root.setPos(0.0F, 24.0F, 0.0F);
            root.setInitialPose(PartPose.offset(0.0F, 24.0F, 0.0F));
            return root;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static float[] array(JsonArray a) {
        return new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }

    private static ModelPart part(JsonObject e, int texW, int texH) {
        float[] from = array(e.getAsJsonArray("f")), to = array(e.getAsJsonArray("t"));
        float[] pivot = array(e.getAsJsonArray("o")), rot = array(e.getAsJsonArray("r"));
        float inflate = e.get("i").getAsFloat();
        // Cube geometry relative to the pivot, in Java axes.
        float x = -(to[0] - pivot[0]), y = -(to[1] - pivot[1]), z = from[2] - pivot[2];
        float sx = to[0] - from[0], sy = to[1] - from[1], sz = to[2] - from[2];
        ModelPart.Cube cube = new ModelPart.Cube(0, 0, x, y, z, sx, sy, sz, inflate, inflate, inflate, false,
                texW, texH, EnumSet.allOf(Direction.class));
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
        PartPose pose = PartPose.offsetAndRotation(-pivot[0], -pivot[1], pivot[2],
                (float) Math.toRadians(-rot[0]), (float) Math.toRadians(-rot[1]), (float) Math.toRadians(rot[2]));
        part.setInitialPose(pose);
        part.loadPose(pose);
        return part;
    }
}
