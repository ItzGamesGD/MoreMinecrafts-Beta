package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent ownership tags. A mob or boss carries a {@link Tag} (arena instance, trial, round, kind, allocation);
 * the shared wave key carries a {@link KeyTag}. Untagged entities are external and never count. A tagged entity is
 * the trial's only if the controller still records it under the same ids; any other tagged copy (an old trial, a
 * stale chunk save) is handled by the I-ID2 table in {@link ColosseumController#decideLoad}.
 * <p>
 * Neither attachment is copied on death or mob conversion, so a converted mob is external.
 */
public final class ColosseumOwnership {
    private ColosseumOwnership() {}

    /** {@code MOB}: an R1-R6 allocation. {@code BOSS}: the R7 Immortal Gladiator ({@code allocation = -1}). */
    public enum Kind { MOB, BOSS }

    public static final int BOSS_ALLOCATION = -1;

    /**
     * {@code legacyArena} is only read from worlds saved by the pre-schema build, where the arena was identified by
     * its packed center; new tags always carry {@code instance}.
     */
    public record Tag(String instance, long legacyArena, UUID trial, int round, Kind kind, int allocation) {
        public static Tag of(String instance, UUID trial, int round, Kind kind, int allocation) {
            return new Tag(instance, 0L, trial, round, kind, allocation);
        }

        static final Codec<Tag> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("instance", "").forGetter(Tag::instance),
                Codec.LONG.optionalFieldOf("arena").forGetter(t -> t.legacyArena == 0L ? Optional.empty() : Optional.of(t.legacyArena)),
                UUIDUtil.CODEC.fieldOf("trial").forGetter(Tag::trial),
                Codec.INT.optionalFieldOf("round", 0).forGetter(Tag::round),
                Codec.STRING.xmap(Kind::valueOf, Kind::name).optionalFieldOf("kind", Kind.MOB).forGetter(Tag::kind),
                Codec.INT.fieldOf("allocation").forGetter(Tag::allocation)
        ).apply(i, (instance, arena, trial, round, kind, allocation) ->
                new Tag(instance, arena.orElse(0L), trial, round, kind, allocation)));
    }

    /** Marks the one shared Trial Key item entity of a cleared wave. The item stack itself is left untouched. */
    public record KeyTag(String instance, UUID trial, int round) {
        static final Codec<KeyTag> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("instance").forGetter(KeyTag::instance),
                UUIDUtil.CODEC.fieldOf("trial").forGetter(KeyTag::trial),
                Codec.INT.fieldOf("round").forGetter(KeyTag::round)
        ).apply(i, KeyTag::new));
    }

    public static final AttachmentType<Tag> TAG = AttachmentRegistry.create(ModIds.id("st05_trial_owner"),
            builder -> builder.persistent(Tag.CODEC));

    public static final AttachmentType<KeyTag> KEY_TAG = AttachmentRegistry.create(ModIds.id("st05_wave_key"),
            builder -> builder.persistent(KeyTag.CODEC));

    public static void initialize() {
        // Class loading performs attachment registration.
    }

    @Nullable public static Tag tag(Entity entity) { return entity.getAttached(TAG); }

    @Nullable public static KeyTag keyTag(Entity entity) { return entity.getAttached(KEY_TAG); }

    static void set(Entity entity, Tag tag) { entity.setAttached(TAG, tag); }

    static void setKey(Entity entity, KeyTag tag) { entity.setAttached(KEY_TAG, tag); }

    /** Detaches the ownership tag: the entity becomes a plain external mob (timed-out trials, Q116). */
    static void release(Entity entity) { entity.removeAttached(TAG); }
}
