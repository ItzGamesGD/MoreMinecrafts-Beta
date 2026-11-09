package dev.itzgamesgd.moreminecrafts.fishing;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shift-right-click a Fisherman with one custom fish to sell that individual fish. */
public final class FishermanFishSale {
    private FishermanFishSale() {}

    public static void initialize() {
        UseEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            if (!(target instanceof Villager villager)
                    || !villager.getVillagerData().profession().is(VillagerProfession.FISHERMAN)
                    || !player.isShiftKeyDown()) return InteractionResult.PASS;
            ItemStack fishStack = player.getItemInHand(hand);
            if (!(fishStack.getItem() instanceof FishItem fish)) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.SUCCESS;

            int grade = fishStack.getOrDefault(ModDataComponents.FISH_GRADE, 1);
            int length = fishStack.getOrDefault(ModDataComponents.FISH_LENGTH_TENTHS_CM,
                    fish.species().lengthDistribution().minimum());
            int weight = fishStack.getOrDefault(ModDataComponents.FISH_WEIGHT_TENTHS_KG,
                    fish.species().expectedWeightForLength(length));
            int price = FishingSaleRules.emeraldPrice(fish.species(), grade, length, weight);
            fishStack.shrink(1);
            ItemStack payment = new ItemStack(Items.EMERALD, price);
            if (!player.getInventory().add(payment))
                level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), payment));
            player.sendSystemMessage(Component.translatable("message.moreminecrafts.fisherman.sold", price));
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                dev.itzgamesgd.moreminecrafts.progression.XpQuestHooks.fishSold(serverPlayer, price);
            return InteractionResult.SUCCESS_SERVER;
        });
    }
}
