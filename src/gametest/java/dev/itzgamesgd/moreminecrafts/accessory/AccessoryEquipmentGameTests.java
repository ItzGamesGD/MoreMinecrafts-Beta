package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;

/** Exercises player-owned storage and inventory-menu callbacks in a real server GameTest level. */
public final class AccessoryEquipmentGameTests implements CustomTestMethodInvoker {
    private static final int LANTERN_SLOT = 46;
    private static final int ACCESSORY_FIRST = 47;

    @GameTest
    public void categoryValidationAndStackComponentsArePreserved(GameTestHelper ctx) {
        var player = ctx.makeMockServerPlayerInLevel();
        var container = new AccessoryEquipmentContainer(player);
        ItemStack combat = new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.PHANTOM_MIRROR));
        combat.set(DataComponents.CUSTOM_NAME, Component.literal("named accessory"));
        combat.set(ModDataComponents.ENHANCEMENT_LEVEL, 10);
        combat.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY.update(tag -> tag.putString("verification", "kept")));

        ctx.assertTrue(container.canPlaceItem(AccessoryCategory.COMBAT.ordinal(), combat), "combat item fits combat slot");
        ctx.assertTrue(!container.canPlaceItem(AccessoryCategory.DEFENSE_SURVIVAL.ordinal(), combat),
                "wrong category is rejected");
        container.setItem(AccessoryCategory.COMBAT.ordinal(), combat);
        ItemStack stored = container.getItem(AccessoryCategory.COMBAT.ordinal());
        ctx.assertTrue(stored.get(ModDataComponents.ENHANCEMENT_LEVEL) == 10, "enhancement component is preserved");
        ctx.assertTrue(stored.get(DataComponents.CUSTOM_NAME).equals(Component.literal("named accessory")),
                "custom name is preserved");
        ctx.assertTrue(stored.get(DataComponents.CUSTOM_DATA).copyTag().getString("verification").equals("kept"),
                "custom item data is preserved");

        container.setItem(AccessoryCategory.DEFENSE_SURVIVAL.ordinal(), combat);
        ctx.assertTrue(container.getItem(AccessoryCategory.DEFENSE_SURVIVAL.ordinal()).isEmpty(),
                "invalid direct container write is rejected");
        ctx.assertTrue(container.getItem(AccessoryCategory.COMBAT.ordinal()).getCount() == 1,
                "stored item count remains one");
        ctx.succeed();
    }

    @GameTest
    public void shiftClickMovesAccessoriesBothDirectionsAndRejectsReplay(GameTestHelper ctx) {
        var player = ctx.makeMockServerPlayerInLevel();
        var menu = player.inventoryMenu;
        ItemStack source = new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.EMBER_RING));
        player.getInventory().setItem(9, source.copy());
        int before = count(player.getInventory(), source.getItem())
                + attachedCount(player, source.getItem());

        menu.quickMoveStack(player, 9);
        ItemStack equipped = menu.getSlot(ACCESSORY_FIRST + AccessoryCategory.DEFENSE_SURVIVAL.ordinal()).getItem();
        ctx.assertTrue(!equipped.isEmpty() && equipped.getItem() == source.getItem(), "shift-click equips the matching category");
        ctx.assertTrue(count(player.getInventory(), source.getItem()) + attachedCount(player, source.getItem()) == before,
                "equip conserves item count");

        menu.quickMoveStack(player, 9);
        ctx.assertTrue(attachedCount(player, source.getItem()) == 1, "replayed shift-click does not duplicate equipment");

        menu.quickMoveStack(player, ACCESSORY_FIRST + AccessoryCategory.DEFENSE_SURVIVAL.ordinal());
        ctx.assertTrue(attachedCount(player, source.getItem()) == 0, "shift-click unequips to inventory");
        ctx.assertTrue(count(player.getInventory(), source.getItem()) == 1, "unequip conserves exactly one item");
        ctx.assertTrue(menu.getSlot(LANTERN_SLOT).getItem().isEmpty(), "accessory transfers do not alter lantern slot");
        ctx.succeed();
    }

    @GameTest
    public void cursorPickupAndCompatibleSwapUseServerSlotValidation(GameTestHelper ctx) {
        var player = ctx.makeMockServerPlayerInLevel();
        var menu = player.inventoryMenu;
        int target = ACCESSORY_FIRST + AccessoryCategory.COMBAT.ordinal();
        ItemStack first = new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.STAR_COVENANT));
        ItemStack second = new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.BLOOD_COVENANT));
        ItemStack invalid = new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.SEED_OF_LIFE));

        menu.setCarried(first.copy());
        menu.clicked(target, 0, ContainerInput.PICKUP, player);
        ctx.assertTrue(menu.getCarried().isEmpty(), "cursor placement consumes the carried copy");
        ctx.assertTrue(menu.getSlot(target).getItem().getItem() == first.getItem(), "valid cursor placement equips item");

        menu.setCarried(second.copy());
        menu.clicked(target, 0, ContainerInput.PICKUP, player);
        ctx.assertTrue(menu.getCarried().getItem() == first.getItem(), "compatible cursor swap returns old item");
        ctx.assertTrue(menu.getSlot(target).getItem().getItem() == second.getItem(), "compatible cursor swap equips replacement");

        menu.setCarried(invalid.copy());
        menu.clicked(target, 0, ContainerInput.PICKUP, player);
        ctx.assertTrue(menu.getCarried().getItem() == invalid.getItem(), "wrong-category cursor stack remains carried");
        ctx.assertTrue(menu.getSlot(target).getItem().getItem() == second.getItem(), "wrong-category click leaves equipment unchanged");
        ctx.succeed();
    }

    @GameTest
    public void fullInventoryDoesNotDeleteAccessoryAndDeathPolicyDetachesExactlyOnce(GameTestHelper ctx) {
        var player = ctx.makeMockServerPlayerInLevel();
        var container = new AccessoryEquipmentContainer(player);
        ItemStack equipped = new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.SNOW_CHARM));
        container.setItem(AccessoryCategory.EXPLORATION_MOBILITY.ordinal(), equipped);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));

        player.inventoryMenu.quickMoveStack(player, ACCESSORY_FIRST + AccessoryCategory.EXPLORATION_MOBILITY.ordinal());
        ctx.assertTrue(attachedCount(player, equipped.getItem()) == 1, "full inventory leaves accessory equipped");

        List<ItemStack> retainedDrops = AccessoryDeathDrops.detachForDeath(player, true);
        ctx.assertTrue(retainedDrops.isEmpty(), "keepInventory emits no accessory drops");
        ctx.assertTrue(attachedCount(player, equipped.getItem()) == 1, "keepInventory retains complete equipment state");

        List<ItemStack> drops = AccessoryDeathDrops.detachForDeath(player, false);
        ctx.assertTrue(drops.size() == 1 && drops.getFirst().getItem() == equipped.getItem(),
                "normal death detaches the equipped stack for one drop");
        ctx.assertTrue(attachedCount(player, equipped.getItem()) == 0, "normal death clears the equipment source");
        ctx.assertTrue(AccessoryDeathDrops.detachForDeath(player, false).isEmpty(), "repeated death processing cannot duplicate drops");
        ctx.succeed();
    }

    @GameTest
    public void attachmentCodecRoundTripPreservesEquippedStacks(GameTestHelper ctx) {
        var player = ctx.makeMockServerPlayerInLevel();
        ItemStack equipped = new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.SOUL_CHAIN));
        equipped.set(ModDataComponents.ENHANCEMENT_LEVEL, 7);
        equipped.set(DataComponents.CUSTOM_NAME, Component.literal("saved name"));
        new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.COMBAT.ordinal(), equipped);

        var ops = net.minecraft.resources.RegistryOps.create(
                com.mojang.serialization.JsonOps.INSTANCE,
                ctx.getLevel().registryAccess());
        var encoded = AccessoryLoadout.CODEC.encodeStart(ops,
                player.getAttachedOrElse(EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty())).getOrThrow();
        var decoded = AccessoryLoadout.CODEC.parse(ops, encoded).getOrThrow();
        ItemStack restored = decoded.get(AccessoryCategory.COMBAT.ordinal());
        ctx.assertTrue(restored.getItem() == equipped.getItem(), "save/load retains registered accessory item");
        ctx.assertTrue(restored.get(ModDataComponents.ENHANCEMENT_LEVEL) == 7, "save/load retains enhancement metadata");
        ctx.assertTrue(restored.get(DataComponents.CUSTOM_NAME).equals(Component.literal("saved name")), "save/load retains stack components");
        ctx.succeed();
    }

    private static int attachedCount(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.Item item) {
        var loadout = player.getAttachedOrElse(EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty());
        return (int) loadout.stacks().stream().filter(stack -> !stack.isEmpty() && stack.getItem() == item)
                .mapToInt(ItemStack::getCount).sum();
    }

    private static int count(net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == item) count += stack.getCount();
        }
        return count;
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
