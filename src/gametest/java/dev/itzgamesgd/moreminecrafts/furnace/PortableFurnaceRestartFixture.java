package dev.itzgamesgd.moreminecrafts.furnace;

import dev.itzgamesgd.moreminecrafts.equipment.*;
import dev.itzgamesgd.moreminecrafts.progression.*;
import dev.itzgamesgd.moreminecrafts.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Included in the existing two-process production restart probe, never production code. */
public final class PortableFurnaceRestartFixture {
    private static final BlockPos CHEST = new BlockPos((64 << 4) + 12, 100, (64 << 4) + 14);
    private PortableFurnaceRestartFixture() {}
    public static void write(GameTestHelper ctx, ServerPlayer owner) {
        owner.setAttached(KnowledgeRuntime.STATE, KnowledgeRuntime.state(owner)
                .with("research.v3.research.07", 5).with("research.v3.research.11", 3));
        var item = new ItemStack(ModItems.PORTABLE_FURNACE);
        owner.setItemInHand(InteractionHand.MAIN_HAND, item);
        ModItems.PORTABLE_FURNACE.use(ctx.getLevel(), owner, InteractionHand.MAIN_HAND);
        var menu = (PortableFurnaceMenu) owner.containerMenu;
        menu.setCarried(new ItemStack(Items.RAW_IRON, 4));
        menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, owner);
        menu.setCarried(new ItemStack(Items.COAL, 2));
        menu.clicked(1, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, owner);
        for (int tick = 0; tick < 13; tick++) menu.serverTick(owner);
        menu.removed(owner);
        owner.containerMenu = owner.inventoryMenu;
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        ctx.getLevel().setBlock(CHEST, Blocks.CHEST.defaultBlockState(), 3);
        ((ChestBlockEntity) ctx.getLevel().getBlockEntity(CHEST)).setItem(0, item);
        System.out.println("PORTABLE_RESTART_WRITE cook=" + item.get(ModDataComponents.PORTABLE_FURNACE_CONTENTS).cookTime());
    }
    public static void readOffline(GameTestHelper ctx, java.util.UUID ownerId) {
        ctx.assertTrue(ctx.getLevel().getServer().getPlayerList().getPlayer(ownerId) == null,
                "attributed owner is offline in fresh process");
        var chest = (ChestBlockEntity) ctx.getLevel().getBlockEntity(CHEST);
        ctx.assertTrue(chest != null, "saved portable-furnace chest restored from world data");
        var item = chest.removeItemNoUpdate(0);
        var before = item.get(ModDataComponents.PORTABLE_FURNACE_CONTENTS);
        var research = item.get(ModDataComponents.PORTABLE_FURNACE_RESEARCH_STATE);
        ctx.assertTrue(before != null && research != null && before.cookTime() == 16
                && research.attributedPlayer().orElseThrow().equals(ownerId), "inventory, owner and cooking progress restored");
        var viewer = ctx.makeMockServerPlayerInLevel();
        viewer.setItemInHand(InteractionHand.MAIN_HAND, item);
        ModItems.PORTABLE_FURNACE.use(ctx.getLevel(), viewer, InteractionHand.MAIN_HAND);
        var menu = (PortableFurnaceMenu) viewer.containerMenu;
        menu.serverTick(viewer);
        var after = item.get(ModDataComponents.PORTABLE_FURNACE_CONTENTS);
        ctx.assertTrue(after.cookTime() == 18 && after.burnTime() == before.burnTime() - 1,
                "offline owner lookup and saved fractional cooking resume without reset");
        ctx.assertTrue(after.burnDuration() == FurnaceResearchRules.fuelDuration(1600 / 3, 2, 0).burnTicks()
                && after.items().get(0).getCount() == 4 && after.items().get(1).getCount() == 1,
                "saved fuel savings and all input/fuel units survive the process boundary");
        menu.removed(viewer);
        System.out.println("PORTABLE_RESTART_READ offlineOwner=true cook=" + after.cookTime());
    }
}
