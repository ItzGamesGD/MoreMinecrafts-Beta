package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Persistent state saved in the player's file alongside inventory and quest progress. */
public final class QuestWitherSkullAttachments {
    public record Receipt(String tx, String token, int count) {
        public static final Codec<Receipt> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("tx").forGetter(Receipt::tx),
                Codec.STRING.fieldOf("token").forGetter(Receipt::token),
                Codec.INT.fieldOf("count").forGetter(Receipt::count)).apply(instance, Receipt::new));
    }

    public record Receipts(List<Receipt> list) {
        public static final Codec<Receipts> CODEC = Receipt.CODEC.listOf().xmap(Receipts::new, Receipts::list);

        public static Receipts empty() { return new Receipts(List.of()); }

        public Receipt find(String tx) {
            for (Receipt receipt : list) if (receipt.tx().equals(tx)) return receipt;
            return null;
        }

        public Receipts with(Receipt receipt) {
            List<Receipt> next = new ArrayList<>(list);
            next.removeIf(existing -> existing.tx().equals(receipt.tx()));
            next.add(receipt);
            return new Receipts(List.copyOf(next));
        }

        public Receipts without(String tx) {
            List<Receipt> next = new ArrayList<>(list);
            next.removeIf(existing -> existing.tx().equals(tx));
            return new Receipts(List.copyOf(next));
        }
    }

    public static final AttachmentType<Receipts> RECEIPTS = AttachmentRegistry.create(
            ModIds.id("quest_wither_skull_receipts"), builder -> builder.initializer(Receipts::empty)
                    .persistent(Receipts.CODEC).copyOnDeath());

    private QuestWitherSkullAttachments() {}

    public static void initialize() {
        java.util.Objects.requireNonNull(RECEIPTS);
    }
}
