package dev.itzgamesgd.moreminecrafts.colosseum;
import com.mojang.serialization.Codec;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.world.level.block.entity.BlockEntity;
/** A replacement player chest has a new block entity and never inherits the attempt's ownership. */
final class ArenaSupplyOwnership {
    static final AttachmentType<String> OWNER=AttachmentRegistry.create(ModIds.id("st05_supply_owner"),b->b.persistent(Codec.STRING));
    static void initialize() {}
    static void mark(BlockEntity b,String owner) {((AttachmentTarget)b).setAttached(OWNER,owner);b.setChanged();}
    static boolean owned(BlockEntity b,String owner) {return b!=null && owner.equals(((AttachmentTarget)b).getAttached(OWNER));}
}
