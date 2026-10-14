package dev.itzgamesgd.moreminecrafts.fishing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import net.minecraft.world.item.ItemStack;

public record RodEquipment(ItemStack reel, ItemStack crankbait, ItemStack minnow) {
    public static final RodEquipment EMPTY=new RodEquipment(ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY);
    public static final Codec<RodEquipment> CODEC=RecordCodecBuilder.create(instance->instance.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("reel").forGetter(RodEquipment::reel),
            ItemStack.OPTIONAL_CODEC.fieldOf("crankbait").forGetter(RodEquipment::crankbait),
            ItemStack.OPTIONAL_CODEC.fieldOf("minnow").forGetter(RodEquipment::minnow)
    ).apply(instance,RodEquipment::new));
    public RodEquipment { reel=reel.copy(); crankbait=crankbait.copy(); minnow=minnow.copy(); }
    public ItemStack slot(int slot){return switch(slot){case 0->reel;case 1->crankbait;case 2->minnow;default->ItemStack.EMPTY;};}
    /** Returns zero for absent, mismatched, or broken equipment. */
    public int tier(FishingEquipmentFamily family) {
        ItemStack stack = switch (family) { case REEL -> reel; case CRANKBAIT -> crankbait; case MINNOW -> minnow; };
        return ModuleDurability.isActive(stack, family) && stack.getItem() instanceof FishingEquipmentItem item
                ? item.tier() : 0;
    }

    /** Returns a replacement value with wear applied to a copy of only the selected module. */
    public RodEquipment withWear(FishingEquipmentFamily family) {
        return withModuleUpdate(family, stack -> ModuleDurability.wear(stack, family));
    }

    RodEquipment withModuleUpdate(FishingEquipmentFamily family, Consumer<ItemStack> update) {
        return switch (family) {
            case REEL -> new RodEquipment(copyForUpdate(reel, ItemStack::copy, update), crankbait, minnow);
            case CRANKBAIT -> new RodEquipment(reel, copyForUpdate(crankbait, ItemStack::copy, update), minnow);
            case MINNOW -> new RodEquipment(reel, crankbait, copyForUpdate(minnow, ItemStack::copy, update));
        };
    }

    static <T> T copyForUpdate(T original, UnaryOperator<T> copy, Consumer<T> update) {
        T replacement = copy.apply(original);
        update.accept(replacement);
        return replacement;
    }
}
