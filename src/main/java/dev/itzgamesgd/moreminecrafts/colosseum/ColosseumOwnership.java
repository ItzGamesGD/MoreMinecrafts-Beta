package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent trial-ownership tag on a spawned mob: arena key, trial id and allocation index. Mobs without it are
 * external and never count. A tagged mob is the trial's only if the controller still records it ALIVE under the
 * same trial, allocation and entity id; any other tagged copy (old trial, stale chunk save) is an orphan.
 * Not copied on death or conversion: a converted mob is external and its allocation is requeued.
 */
public final class ColosseumOwnership {
    private ColosseumOwnership() {}

    public record Tag(long arena, UUID trial, int allocation) {
        static final Codec<Tag> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("arena").forGetter(Tag::arena),
                UUIDUtil.CODEC.fieldOf("trial").forGetter(Tag::trial),
                Codec.INT.fieldOf("allocation").forGetter(Tag::allocation)
        ).apply(i, Tag::new));
    }

    public static final AttachmentType<Tag> TAG = AttachmentRegistry.create(ModIds.id("st05_trial_owner"),
            builder -> builder.persistent(Tag.CODEC));

    public static void initialize() {
        // Class loading performs attachment registration.
    }

    @Nullable public static Tag tag(Entity entity) { return entity.getAttached(TAG); }

    static void set(Entity entity, Tag tag) { entity.setAttached(TAG, tag); }
}
