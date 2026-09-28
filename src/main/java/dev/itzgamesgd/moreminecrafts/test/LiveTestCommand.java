package dev.itzgamesgd.moreminecrafts.test;

import dev.itzgamesgd.moreminecrafts.alchemy.ModAlchemy;
import dev.itzgamesgd.moreminecrafts.fishing.FishingEquipmentFamily;
import dev.itzgamesgd.moreminecrafts.fishing.FishingEquipmentTier;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import dev.itzgamesgd.moreminecrafts.skeleton.SkeletonKingState;
import dev.itzgamesgd.moreminecrafts.zombie.ZombieVariant;
import dev.itzgamesgd.moreminecrafts.zombie.ZombieVariantState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.function.BiConsumer;

public final class LiveTestCommand {
    private LiveTestCommand() {}

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> dispatcher.register(
                Commands.literal("mmtest")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("give")
                                .then(Commands.literal("enhancement").executes(c -> giveEnhancement(c.getSource().getPlayerOrException())))
                                .then(Commands.literal("equipment").executes(c -> giveEquipment(c.getSource().getPlayerOrException())))
                                .then(Commands.literal("fishing").executes(c -> giveFishing(c.getSource().getPlayerOrException())))
                                .then(Commands.literal("materials").executes(c -> giveMaterials(c.getSource().getPlayerOrException())))
                                .then(Commands.literal("shock").executes(c -> giveShock(c.getSource().getPlayerOrException())))
                                .then(Commands.literal("all").executes(c -> giveAll(c.getSource().getPlayerOrException()))))
                        .then(Commands.literal("spawn")
                                .then(Commands.literal("bomb").executes(c -> spawnZombie(c.getSource(), ZombieVariant.BOMB)))
                                .then(Commands.literal("leap").executes(c -> spawnZombie(c.getSource(), ZombieVariant.LEAP)))
                                .then(Commands.literal("mining_giant").executes(c -> spawnZombie(c.getSource(), ZombieVariant.MINING_GIANT)))
                                .then(Commands.literal("skeleton_king").executes(c -> spawnSkeletonKing(c.getSource()))))));
    }

    private static int giveEnhancement(ServerPlayer player) {
        give(player, ModItems.ENHANCEMENT_STONE, 64);
        give(player, Items.IRON_SWORD, 1);
        give(player, Items.IRON_PICKAXE, 1);
        give(player, Items.IRON_CHESTPLATE, 1);
        player.sendSystemMessage(Component.literal("MoreMinecrafts test kit: enhancement"));
        return 1;
    }

    private static int giveEquipment(ServerPlayer player) {
        give(player, ModItems.FLAME_SWORD, 1);
        give(player, ModItems.WIND_SWORD, 1);
        player.sendSystemMessage(Component.literal("MoreMinecrafts test kit: equipment"));
        return 1;
    }

    private static int giveFishing(ServerPlayer player) {
        give(player, Items.FISHING_ROD, 1);
        forEachFishingEquipment((family, tier) -> give(player, ModItems.equipmentItem(family, tier), 1));
        player.sendSystemMessage(Component.literal("MoreMinecrafts test kit: fishing"));
        return 1;
    }

    static void forEachFishingEquipment(BiConsumer<FishingEquipmentFamily, Integer> consumer) {
        for (FishingEquipmentFamily family : FishingEquipmentFamily.values())
            for (int tier = FishingEquipmentTier.MIN; tier <= FishingEquipmentTier.MAX; tier++)
                consumer.accept(family, tier);
    }

    private static int giveMaterials(ServerPlayer player) {
        give(player, ModItems.RAW_TITANIUM, 64);
        give(player, ModItems.TITANIUM_INGOT, 64);
        give(player, ModItems.RAW_TUNGSTEN, 64);
        give(player, ModItems.TUNGSTEN_INGOT, 64);
        give(player, ModItems.MYTHRIL, 64);
        give(player, ModItems.ETHERIUM, 64);
        give(player, ModItems.ADAMANTIUM_INGOT, 64);
        player.sendSystemMessage(Component.literal("MoreMinecrafts test kit: materials"));
        return 1;
    }

    private static int giveShock(ServerPlayer player) {
        give(player, PotionContents.createItemStack(Items.POTION, Potions.THICK), 3);
        give(player, Items.COPPER_INGOT, 3);
        give(player, PotionContents.createItemStack(Items.POTION, ModAlchemy.SHOCK_POTION), 1);
        player.sendSystemMessage(Component.literal("MoreMinecrafts test kit: shock (brew supplies + direct-test potion)"));
        return 1;
    }

    private static int giveAll(ServerPlayer player) {
        giveEnhancement(player);
        giveEquipment(player);
        giveFishing(player);
        giveMaterials(player);
        giveShock(player);
        player.sendSystemMessage(Component.literal("MoreMinecrafts test kit: all"));
        return 1;
    }

    private static void give(ServerPlayer player, Item item, int count) {
        give(player, new ItemStack(item), count);
    }

    private static void give(ServerPlayer player, ItemStack stack, int count) {
        stack.setCount(count);
        player.getInventory().add(stack);
    }

    private static int spawnZombie(net.minecraft.commands.CommandSourceStack source, ZombieVariant variant) {
        Zombie zombie = EntityTypes.ZOMBIE.create(source.getLevel(), EntitySpawnReason.COMMAND);
        if (zombie == null) return 0;
        var position = source.getPosition();
        zombie.setPos(position.x, position.y, position.z);
        ((ZombieVariantState) zombie).moreminecrafts$setVariant(variant);
        if (variant == ZombieVariant.MINING_GIANT) zombie.setHealth(zombie.getMaxHealth());
        source.getLevel().addFreshEntity(zombie);
        source.sendSuccess(() -> Component.literal("Spawned MoreMinecrafts test mob: " + variant.name()), true);
        return 1;
    }

    private static int spawnSkeletonKing(net.minecraft.commands.CommandSourceStack source) {
        Skeleton skeleton = EntityTypes.SKELETON.create(source.getLevel(), EntitySpawnReason.COMMAND);
        if (skeleton == null) return 0;
        var position = source.getPosition();
        skeleton.setPos(position.x, position.y, position.z);
        ((SkeletonKingState) skeleton).moreminecrafts$makeSkeletonKing();
        source.getLevel().addFreshEntity(skeleton);
        source.sendSuccess(() -> Component.literal("Spawned MoreMinecrafts test mob: SKELETON_KING"), true);
        return 1;
    }
}
