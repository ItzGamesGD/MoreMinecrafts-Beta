package dev.itzgamesgd.moreminecrafts.bounty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.item.ItemStack;

/** Persistent state that must be saved in the same file as the player's inventory. */
public final class ContractAttachments {
    /**
     * A redemption that reached the player's data. {@code overflow} holds reward stacks that did not fit, and
     * {@code delivered} says whether they have been dropped.
     */
    public record Receipt(String tx, String contractId, List<ItemStack> overflow, boolean delivered) {
        public static final Codec<Receipt> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("tx").forGetter(Receipt::tx),
                Codec.STRING.fieldOf("contract").forGetter(Receipt::contractId),
                ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("overflow", List.of()).forGetter(Receipt::overflow),
                Codec.BOOL.optionalFieldOf("delivered", false).forGetter(Receipt::delivered)).apply(instance, Receipt::new));

        Receipt asDelivered() { return new Receipt(tx, contractId, overflow, true); }
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
            ModIds.id("contract_receipts"), builder -> builder.initializer(Receipts::empty)
                    .persistent(Receipts.CODEC).copyOnDeath());

    /** Marks dropped overflow items with their transaction so recovery can recognize them. */
    public static final AttachmentType<String> DROP_TX = AttachmentRegistry.create(
            ModIds.id("contract_drop_tx"), builder -> builder.persistent(Codec.STRING));

    private ContractAttachments() {}

    public static void initialize() {}
}
