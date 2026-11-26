package dev.itzgamesgd.moreminecrafts.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Vector3f;

/**
 * Reads the {@code animations} block that {@code tools/bbmodel/convert_bbmodel.py --animations} writes next to the bones of
 * a "moreminecrafts:bone_model/1" file and bakes them into vanilla {@link KeyframeAnimation}s. Values are already in
 * ModelPart convention (rotation degrees, position pixels, scale multiplier), so no sign handling happens here.
 */
public final class BoneAnimations {
    private final Map<String, AnimationDefinition> definitions;

    private BoneAnimations(Map<String, AnimationDefinition> definitions) {
        this.definitions = definitions;
    }

    public static BoneAnimations load(ResourceManager resources, Identifier id) {
        try (Reader reader = resources.openAsReader(id)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read bone model animations " + id, e);
        }
    }

    public static BoneAnimations parse(JsonObject json) {
        Map<String, AnimationDefinition> out = new LinkedHashMap<>();
        JsonObject anims = json.has("animations") ? json.getAsJsonObject("animations") : new JsonObject();
        for (Map.Entry<String, JsonElement> entry : anims.entrySet()) {
            JsonObject anim = entry.getValue().getAsJsonObject();
            AnimationDefinition.Builder builder = AnimationDefinition.Builder.withLength(anim.get("length").getAsFloat());
            if ("loop".equals(anim.get("loop").getAsString())) {
                builder.looping();
            }
            for (Map.Entry<String, JsonElement> bone : anim.getAsJsonObject("bones").entrySet()) {
                for (Map.Entry<String, JsonElement> channel : bone.getValue().getAsJsonObject().entrySet()) {
                    AnimationChannel built = channel(channel.getKey(), channel.getValue().getAsJsonArray());
                    if (built != null) {
                        builder.addAnimation(bone.getKey(), built);
                    }
                }
            }
            out.put(entry.getKey(), builder.build());
        }
        return new BoneAnimations(out);
    }

    private static AnimationChannel channel(String kind, JsonArray frames) {
        AnimationChannel.Target target;
        switch (kind) {
            case "rotation" -> target = AnimationChannel.Targets.ROTATION;
            case "position" -> target = AnimationChannel.Targets.POSITION;
            case "scale" -> target = AnimationChannel.Targets.SCALE;
            default -> {
                return null;
            }
        }
        List<Keyframe> keyframes = new ArrayList<>();
        for (JsonElement f : frames) {
            JsonArray pair = f.getAsJsonArray();
            float t = pair.get(0).getAsFloat();
            JsonArray v = pair.get(1).getAsJsonArray();
            float x = v.get(0).getAsFloat();
            float y = v.get(1).getAsFloat();
            float z = v.get(2).getAsFloat();
            Vector3f vec = switch (kind) {
                case "rotation" -> KeyframeAnimations.degreeVec(x, y, z);
                case "scale" -> KeyframeAnimations.scaleVec(x, y, z);
                default -> new Vector3f(x, y, z); // position is a raw ModelPart offset in pixels
            };
            keyframes.add(new Keyframe(t, vec, AnimationChannel.Interpolations.LINEAR));
        }
        return new AnimationChannel(target, keyframes.toArray(new Keyframe[0]));
    }

    public boolean has(String name) {
        return definitions.containsKey(name);
    }

    public AnimationDefinition definition(String name) {
        AnimationDefinition def = definitions.get(name);
        if (def == null) {
            throw new IllegalStateException("Bone model has no animation '" + name + "'");
        }
        return def;
    }

    public KeyframeAnimation bake(String name, ModelPart root) {
        return definition(name).bake(root);
    }
}
