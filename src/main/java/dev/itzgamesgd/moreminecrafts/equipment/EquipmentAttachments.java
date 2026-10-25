package dev.itzgamesgd.moreminecrafts.equipment;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.accessory.AccessoryLoadout;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;

public final class EquipmentAttachments {
    public static final AttachmentType<BackpackStorage> BACKPACK = AttachmentRegistry.create(
            ModIds.id("backpack_storage"), builder -> builder.initializer(BackpackStorage::empty)
                    .persistent(BackpackStorage.CODEC).copyOnDeath());
    public static final AttachmentType<ItemStack> LANTERN = AttachmentRegistry.create(
            ModIds.id("miner_lantern_socket"), builder -> builder.initializer(() -> ItemStack.EMPTY)
                    .persistent(ItemStack.OPTIONAL_CODEC)
                    .copyOnDeath()
                    .syncWith(ItemStack.OPTIONAL_STREAM_CODEC, AttachmentSyncPredicate.all()));
    public static final AttachmentType<AccessoryLoadout> ACCESSORIES = AttachmentRegistry.create(
            ModIds.id("accessory_equipment"), builder -> builder.initializer(AccessoryLoadout::empty)
                    .persistent(AccessoryLoadout.CODEC)
                    .copyOnDeath()
                    .syncWith(ByteBufCodecs.fromCodecWithRegistries(AccessoryLoadout.CODEC),
                            AttachmentSyncPredicate.all()));
    /** Ring of Life fired and the player has not yet recovered above the threshold; absent means armed. */
    public static final AttachmentType<Boolean> RING_OF_LIFE_SPENT = AttachmentRegistry.create(
            ModIds.id("accessory_ring_of_life_spent"), builder -> builder.persistent(Codec.BOOL).copyOnDeath());

    private EquipmentAttachments() {}
    public static void initialize() {}
}
