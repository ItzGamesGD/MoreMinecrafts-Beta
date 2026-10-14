package dev.itzgamesgd.moreminecrafts.equipment;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.registries.Registries;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import dev.itzgamesgd.moreminecrafts.fishing.FishSpecies;
import dev.itzgamesgd.moreminecrafts.fishing.FishItem;
import dev.itzgamesgd.moreminecrafts.fishing.FishingEquipmentFamily;
import dev.itzgamesgd.moreminecrafts.fishing.FishingLogic;
import dev.itzgamesgd.moreminecrafts.fishing.ModuleDurability;
import dev.itzgamesgd.moreminecrafts.fishing.RodEquipment;

public final class LowerEquipmentGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void greatswordRunsBeforeArmorStandInteraction(GameTestHelper context) {
        var level = context.getLevel();
        BlockPos base = context.absolutePos(new BlockPos(1, 1, 1));
        var attacker = context.makeMockServerPlayerInLevel();
        attacker.setPos(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D);
        attacker.setYRot(0.0F);
        attacker.setXRot(0.0F);
        attacker.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(ModItems.GREATSWORDS.get(WeaponBalance.Tier.WOOD)));

        ArmorStand stand = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
        context.assertTrue(stand != null, "Armor Stand should be constructible");
        Vec3 direction = attacker.getLookAngle().normalize();
        stand.setPos(attacker.getX() + direction.x * 2.5D, attacker.getY(),
                attacker.getZ() + direction.z * 2.5D);
        level.addFreshEntity(stand);
        float initialHealth = stand.getHealth();

        var result = attacker.interactOn(stand, InteractionHand.MAIN_HAND, stand.position());
        context.assertTrue(result.consumesAction(), "Greatsword should consume the entity-interaction path");
        context.assertTrue(stand.isRemoved() || stand.getHealth() < initialHealth,
                "The sweep should damage or destroy an Armor Stand before its equipment interaction");
        context.succeed();
    }

    @GameTest
    public void miningDrillFuelSlotTracksItsSourceItem(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        ItemStack drill = new ItemStack(ModItems.MINING_DRILL);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        ModItems.MINING_DRILL.use(context.getLevel(), player, InteractionHand.MAIN_HAND);
        var menu = player.containerMenu;
        var fuelSlot = menu.getSlot(0);
        context.assertTrue(fuelSlot.mayPlace(new ItemStack(Items.COAL)), "Drill slot accepts coal");
        context.assertTrue(!fuelSlot.mayPlace(new ItemStack(Items.DIAMOND)), "Drill slot rejects unrelated items");
        fuelSlot.set(new ItemStack(Items.COAL));
        context.assertTrue(fuelSlot.getItem().is(Items.COAL), "Fuel is stored in the drill's component");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        context.assertTrue(fuelSlot.getItem().isEmpty(), "The menu hides component contents after the source leaves inventory");
        context.assertTrue(!fuelSlot.mayPlace(new ItemStack(Items.COAL)), "The detached menu cannot accept ghost fuel");
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        context.assertTrue(fuelSlot.getItem().is(Items.COAL), "Returning the source item restores its stored fuel");
        context.succeed();
    }

    @GameTest
    public void portableFurnaceUsesVanillaFuelProvidersAndStackLimit(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        int lavaBucketTicks = PortableFurnaceMenu.vanillaFuelTicks(context.getLevel(), player,
                new ItemStack(Items.LAVA_BUCKET));
        context.assertTrue(lavaBucketTicks == 20_000, "Fuel providers resolve vanilla lava-bucket burn time");
        int boatTicks = PortableFurnaceMenu.vanillaFuelTicks(context.getLevel(), player,
                new ItemStack(Items.OAK_BOAT));
        context.assertTrue(boatTicks == 1_200, "Fuel providers resolve vanilla boat burn time");

        var contents = new PortableFurnaceMenu.PortableContainer();
        contents.setItem(0, new ItemStack(Items.OAK_LOG, 64));
        context.assertTrue(contents.getItem(0).getCount() == 16,
                "The portable furnace applies its 16-item slot limit while setting contents");
        context.assertTrue(PortableFurnaceMenu.canAcceptResult(ItemStack.EMPTY,
                        new ItemStack(Items.OAK_LOG, 16), 16)
                        && !PortableFurnaceMenu.canAcceptResult(ItemStack.EMPTY,
                        new ItemStack(Items.OAK_LOG, 17), 16)
                        && !PortableFurnaceMenu.canAcceptResult(new ItemStack(Items.OAK_LOG, 10),
                        new ItemStack(Items.OAK_LOG, 7), 16),
                "Furnace output must reject oversized recipe results before consuming input or fuel");
        context.succeed();
    }

    @GameTest
    public void aSecondFishRollCannotReuseExhaustedCaptureModules(GameTestHelper context) {
        ItemStack minnow = new ItemStack(ModItems.equipmentItem(FishingEquipmentFamily.MINNOW, 1));
        ItemStack crankbait = new ItemStack(ModItems.equipmentItem(FishingEquipmentFamily.CRANKBAIT, 1));
        minnow.setDamageValue(minnow.getMaxDamage() - 2);
        crankbait.setDamageValue(crankbait.getMaxDamage() - 2);
        RodEquipment equipment = new RodEquipment(ItemStack.EMPTY, crankbait, minnow);
        context.assertTrue(FishingLogic.canCapture(FishSpecies.CRUCIAN_CARP,
                        equipment.tier(FishingEquipmentFamily.MINNOW)),
                "A two-durability Minnow permits the first fish");

        RodEquipment afterFirstFish = equipment.withWear(FishingEquipmentFamily.MINNOW)
                .withWear(FishingEquipmentFamily.CRANKBAIT);
        context.assertTrue(ModuleDurability.remaining(afterFirstFish.minnow()) == 1
                        && ModuleDurability.remaining(afterFirstFish.crankbait()) == 1,
                "Each permitted fish spends one charge from Minnow and Crankbait");
        context.assertTrue(!FishingLogic.canCapture(FishSpecies.CRUCIAN_CARP,
                        afterFirstFish.tier(FishingEquipmentFamily.MINNOW)),
                "The second roll cannot reuse a now-broken Minnow");
        context.succeed();
    }

    @GameTest
    public void fishermanSalePricesEachFishAndPreservesVanillaInteraction(GameTestHelper context) {
        var level = context.getLevel();
        BlockPos base = context.absolutePos(new BlockPos(1, 1, 1));
        var profession = level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION)
                .getOrThrow(VillagerProfession.FISHERMAN);
        Villager fisherman = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
        context.assertTrue(fisherman != null, "Fisherman villager should be constructible");
        fisherman.setVillagerData(fisherman.getVillagerData().withProfession(profession));
        fisherman.setNoAi(true);
        fisherman.setPos(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D);
        level.addFreshEntity(fisherman);

        var firstSeller = context.makeMockServerPlayerInLevel();
        var secondSeller = context.makeMockServerPlayerInLevel();
        firstSeller.setShiftKeyDown(true);
        secondSeller.setShiftKeyDown(true);
        firstSeller.setPos(base.getX() + 1.5D, base.getY(), base.getZ() + 0.5D);
        secondSeller.setPos(base.getX() + 2.5D, base.getY(), base.getZ() + 0.5D);
        ItemStack small = customFish(FishSpecies.BLUE_MARLIN, 1,
                FishSpecies.BLUE_MARLIN.lengthDistribution().minimum(),
                FishSpecies.BLUE_MARLIN.weightDistribution().minimum());
        ItemStack premium = customFish(FishSpecies.BLUE_MARLIN, 5,
                FishSpecies.BLUE_MARLIN.lengthDistribution().maximum(),
                FishSpecies.BLUE_MARLIN.weightDistribution().maximum());
        firstSeller.setItemInHand(InteractionHand.MAIN_HAND, small);
        secondSeller.setItemInHand(InteractionHand.MAIN_HAND, premium);
        var hit = new EntityHitResult(fisherman, new Vec3(fisherman.getX(), fisherman.getEyeY(), fisherman.getZ()));

        var firstResult = UseEntityCallback.EVENT.invoker().interact(firstSeller, level,
                InteractionHand.MAIN_HAND, fisherman, hit);
        var secondResult = UseEntityCallback.EVENT.invoker().interact(secondSeller, level,
                InteractionHand.MAIN_HAND, fisherman, hit);
        context.assertTrue(firstResult.consumesAction() && secondResult.consumesAction(),
                "Shift-right-click sale is server-accepted for both players; results=" + firstResult + "/"
                        + secondResult + ", shift=" + firstSeller.isShiftKeyDown() + "/" + secondSeller.isShiftKeyDown()
                        + ", profession=" + fisherman.getVillagerData().profession().value()
                        + ", item=" + firstSeller.getItemInHand(InteractionHand.MAIN_HAND).getItem());
        context.assertTrue(firstSeller.getItemInHand(InteractionHand.MAIN_HAND).is(Items.EMERALD)
                        && secondSeller.getItemInHand(InteractionHand.MAIN_HAND).is(Items.EMERALD),
                "Each sale consumes the offered fish and pays emeralds; held="
                        + firstSeller.getItemInHand(InteractionHand.MAIN_HAND) + "/"
                        + secondSeller.getItemInHand(InteractionHand.MAIN_HAND) + ", client=" + level.isClientSide());
        context.assertTrue(firstSeller.getInventory().countItem(Items.EMERALD) == 1
                        && secondSeller.getInventory().countItem(Items.EMERALD) == 6,
                "The dynamic price follows species, grade, length, and weight independently");

        var vanillaSeller = context.makeMockServerPlayerInLevel();
        ItemStack retainedFish = customFish(FishSpecies.BLUE_MARLIN, 1,
                FishSpecies.BLUE_MARLIN.lengthDistribution().minimum(),
                FishSpecies.BLUE_MARLIN.weightDistribution().minimum());
        vanillaSeller.setItemInHand(InteractionHand.MAIN_HAND, retainedFish);
        var vanillaResult = UseEntityCallback.EVENT.invoker().interact(vanillaSeller, level,
                InteractionHand.MAIN_HAND, fisherman, hit);
        context.assertTrue(vanillaResult == net.minecraft.world.InteractionResult.PASS
                        && !vanillaSeller.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "Without Shift, the custom callback leaves the Fisherman's normal vanilla trade interaction alone");
        context.succeed();
    }

    private static ItemStack customFish(FishSpecies species, int grade, int length, int weight) {
        ItemStack stack = new ItemStack(ModItems.fishItem(species));
        stack.set(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.FISH_GRADE, grade);
        stack.set(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.FISH_LENGTH_TENTHS_CM, length);
        stack.set(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.FISH_WEIGHT_TENTHS_KG, weight);
        return stack;
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
