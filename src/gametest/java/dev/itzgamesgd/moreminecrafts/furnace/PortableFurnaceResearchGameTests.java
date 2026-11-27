package dev.itzgamesgd.moreminecrafts.furnace;

import dev.itzgamesgd.moreminecrafts.equipment.*;
import dev.itzgamesgd.moreminecrafts.progression.*;
import dev.itzgamesgd.moreminecrafts.registry.*;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;

public final class PortableFurnaceResearchGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void everyFuelAndSpeedLevelUsesAttributedOwnerAndSurvivesReopen(GameTestHelper ctx) {
        var owner = ctx.makeMockServerPlayerInLevel();
        var viewer = ctx.makeMockServerPlayerInLevel();
        for (int level = 0; level <= 3; level++) {
            owner.setAttached(KnowledgeRuntime.STATE, ResearchProgressState.empty()
                    .with("research.v3.research.06", level).with("research.v3.research.11", level));
            viewer.setAttached(KnowledgeRuntime.STATE, ResearchProgressState.empty());
            ItemStack item = new ItemStack(ModItems.PORTABLE_FURNACE);
            item.set(ModDataComponents.PORTABLE_FURNACE_RESEARCH_STATE,
                    FurnaceResearchState.empty().interactedBy(owner.getUUID()));
            viewer.setItemInHand(InteractionHand.MAIN_HAND, item);
            ModItems.PORTABLE_FURNACE.use(ctx.getLevel(), viewer, InteractionHand.MAIN_HAND);
            var menu = (PortableFurnaceMenu) viewer.containerMenu;
            menu.getSlot(0).set(new ItemStack(Items.RAW_IRON, 4));
            menu.getSlot(1).set(new ItemStack(Items.COAL));
            for (int tick = 0; tick < 10; tick++) menu.serverTick(viewer);
            var saved = item.get(ModDataComponents.PORTABLE_FURNACE_CONTENTS);
            var expectedFuel = FurnaceResearchRules.fuelDuration(1600 / 3, level, 0);
            ctx.assertTrue(saved.burnDuration() == expectedFuel.burnTicks(), "fuel uses attributed owner at level " + level);
            ctx.assertTrue(saved.cookTime() == 10 + level, "ten ticks retain accurate cooking rate at level " + level);
            var research = item.get(ModDataComponents.PORTABLE_FURNACE_RESEARCH_STATE);
            ctx.assertTrue(research.attributedPlayer().orElseThrow().equals(owner.getUUID()), "viewer cannot replace actual owner");
            menu.removed(viewer);
            var ops = RegistryOps.create(NbtOps.INSTANCE, ctx.getLevel().registryAccess());
            ItemStack restored = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, item).getOrThrow()).getOrThrow();
            ctx.assertTrue(restored.get(ModDataComponents.PORTABLE_FURNACE_RESEARCH_STATE).equals(research), "all accounting survives serialization");
            viewer.setItemInHand(InteractionHand.MAIN_HAND, restored);
            ModItems.PORTABLE_FURNACE.use(ctx.getLevel(), viewer, InteractionHand.MAIN_HAND);
            var reopened = (PortableFurnaceMenu) viewer.containerMenu;
            ctx.assertTrue(restored.get(ModDataComponents.PORTABLE_FURNACE_CONTENTS).cookTime() == saved.cookTime(), "reopen preserves cooking");
            reopened.serverTick(viewer);
            ctx.assertTrue(restored.get(ModDataComponents.PORTABLE_FURNACE_CONTENTS).burnTime() == saved.burnTime() - 1, "burn does not reset");
            reopened.removed(viewer);
        }
        ctx.succeed();
    }

    @GameTest
    public void precisionReservesOneBonusAndNeverRerollsOnReopen(GameTestHelper ctx) {
        var player = ctx.makeMockServerPlayerInLevel();
        for (int level = 0; level <= 5; level++) {
            player.setAttached(KnowledgeRuntime.STATE, ResearchProgressState.empty().with("research.v3.research.07", level));
            ItemStack item = new ItemStack(ModItems.PORTABLE_FURNACE);
            player.setItemInHand(InteractionHand.MAIN_HAND, item);
            ModItems.PORTABLE_FURNACE.use(ctx.getLevel(), player, InteractionHand.MAIN_HAND);
            var menu = (PortableFurnaceMenu) player.containerMenu;
            menu.getSlot(0).set(new ItemStack(Items.RAW_IRON));
            menu.getSlot(1).set(new ItemStack(Items.COAL));
            menu.getSlot(2).set(new ItemStack(Items.IRON_INGOT, 15));
            menu.serverTick(player);
            var saved = item.get(ModDataComponents.PORTABLE_FURNACE_CONTENTS);
            ctx.assertTrue(level == 0 ? saved.burnDuration() > 0 : saved.burnDuration() == 0, "possible bonus reserves space before fuel at level " + level);
            menu.getSlot(2).set(ItemStack.EMPTY);
            for (int tick = 0; tick < 67; tick++) menu.serverTick(player);
            int count = menu.getSlot(2).getItem().getCount();
            ctx.assertTrue(menu.getSlot(0).getItem().isEmpty() && count >= 1 && count <= 2, "one completed input yields at most one bonus");
            menu.removed(player);
            ModItems.PORTABLE_FURNACE.use(ctx.getLevel(), player, InteractionHand.MAIN_HAND);
            var reopened = (PortableFurnaceMenu) player.containerMenu;
            reopened.serverTick(player);
            ctx.assertTrue(reopened.getSlot(2).getItem().getCount() == count, "reopen cannot roll output again");
            reopened.removed(player);
        }
        ctx.succeed();
    }
    @GameTest
    public void unattributedAndMissingOwnersNeverBorrowViewerResearchAndFoodIsIneligible(GameTestHelper ctx) {
        var player = ctx.makeMockServerPlayerInLevel();
        player.setAttached(KnowledgeRuntime.STATE, ResearchProgressState.empty()
                .with("research.v3.research.06", 3).with("research.v3.research.07", 5).with("research.v3.research.11", 3));
        for (boolean missingOwner : new boolean[]{false, true}) {
            var item = new ItemStack(ModItems.PORTABLE_FURNACE);
            if (missingOwner) item.set(ModDataComponents.PORTABLE_FURNACE_RESEARCH_STATE,
                    FurnaceResearchState.empty().interactedBy(java.util.UUID.randomUUID()));
            var inventory = new PortableFurnaceMenu.PortableContainer();
            var menu = new PortableFurnaceMenu(95, player.getInventory(), inventory, item);
            inventory.setItem(0, new ItemStack(Items.RAW_IRON));
            inventory.setItem(1, new ItemStack(Items.COAL));
            for (int i = 0; i < 10; i++) menu.serverTick(player);
            var saved = item.get(ModDataComponents.PORTABLE_FURNACE_CONTENTS);
            ctx.assertTrue(saved.cookTime() == 10 && saved.burnDuration() == 1600 / 3,
                    "missing owner or attribution resolves zero instead of viewer level");
            menu.removed(player);
        }
        var item = new ItemStack(ModItems.PORTABLE_FURNACE);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        ModItems.PORTABLE_FURNACE.use(ctx.getLevel(), player, InteractionHand.MAIN_HAND);
        var menu = (PortableFurnaceMenu) player.containerMenu;
        menu.getSlot(0).set(new ItemStack(Items.BEEF));
        menu.getSlot(1).set(new ItemStack(Items.COAL));
        menu.getSlot(2).set(new ItemStack(Items.COOKED_BEEF, 15));
        for (int i = 0; i < 67; i++) menu.serverTick(player);
        ctx.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getSlot(2).getItem().getCount() == 16,
                "food is ineligible and needs no mineral bonus reservation");
        menu.removed(player);
        ctx.succeed();
    }

    @GameTest
    public void combinedResearchConservesInputsAcrossManyCompletionsAndReopens(GameTestHelper ctx) {
        var player = ctx.makeMockServerPlayerInLevel();
        player.setAttached(KnowledgeRuntime.STATE, ResearchProgressState.empty()
                .with("research.v3.research.06", 3).with("research.v3.research.07", 5).with("research.v3.research.11", 3));
        var item = new ItemStack(ModItems.PORTABLE_FURNACE);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        ModItems.PORTABLE_FURNACE.use(ctx.getLevel(), player, InteractionHand.MAIN_HAND);
        var menu = (PortableFurnaceMenu) player.containerMenu;
        menu.getSlot(0).set(new ItemStack(Items.RAW_IRON, 4));
        menu.getSlot(1).set(new ItemStack(Items.COAL, 2));
        for (int i = 0; i < 207; i++) {
            if (i % 11 == 0) {
                menu.removed(player);
                ModItems.PORTABLE_FURNACE.use(ctx.getLevel(), player, InteractionHand.MAIN_HAND);
                menu = (PortableFurnaceMenu) player.containerMenu;
            }
            menu.serverTick(player);
        }
        ctx.assertTrue(menu.getSlot(0).getItem().isEmpty(), "130% cooking completes four 67-tick units in 207 ticks despite reopen");
        int output = menu.getSlot(2).getItem().getCount();
        ctx.assertTrue(output >= 4 && output <= 8, "four inputs create four base products and at most four bonuses");
        ctx.assertTrue(menu.getSlot(1).getItem().getCount() == 1, "only one fuel unit consumed");
        menu.removed(player);
        ctx.succeed();
    }

    @GameTest
    public void precisionCreationRollsExactlyOnceForEachPurchasedLevel(GameTestHelper ctx) {
        var player = ctx.makeMockServerPlayerInLevel();
        var random = ctx.getLevel().getRandom();
        long successSeed = -1, failureSeed = -1;
        for (long seed = 0; successSeed < 0 || failureSeed < 0; seed++) {
            random.setSeed(seed);
            int roll = random.nextInt(10000);
            if (roll < 750 && successSeed < 0) successSeed = seed;
            if (roll >= 3750 && failureSeed < 0) failureSeed = seed;
        }
        for (int level = 0; level <= 5; level++) {
            player.setAttached(KnowledgeRuntime.STATE, ResearchProgressState.empty().with("research.v3.research.07", level));
            for (boolean succeeds : new boolean[]{false, true}) {
                var item = new ItemStack(ModItems.PORTABLE_FURNACE);
                item.set(ModDataComponents.PORTABLE_FURNACE_RESEARCH_STATE,
                        FurnaceResearchState.empty().interactedBy(player.getUUID()));
                item.set(ModDataComponents.PORTABLE_FURNACE_CONTENTS, new PortableFurnaceContents(
                        java.util.List.of(new ItemStack(Items.RAW_IRON), ItemStack.EMPTY, ItemStack.EMPTY), 10, 10, 66, 67));
                player.setItemInHand(InteractionHand.MAIN_HAND, item);
                ModItems.PORTABLE_FURNACE.use(ctx.getLevel(), player, InteractionHand.MAIN_HAND);
                var menu = (PortableFurnaceMenu) player.containerMenu;
                random.setSeed(succeeds ? successSeed : failureSeed);
                menu.serverTick(player);
                int expected = level > 0 && succeeds ? 2 : 1;
                ctx.assertTrue(menu.getSlot(2).getItem().getCount() == expected && menu.getSlot(0).getItem().isEmpty(),
                        "completion uses one deterministic roll at level " + level);
                menu.removed(player);
                ModItems.PORTABLE_FURNACE.use(ctx.getLevel(), player, InteractionHand.MAIN_HAND);
                var reopened = (PortableFurnaceMenu) player.containerMenu;
                reopened.serverTick(player);
                ctx.assertTrue(reopened.getSlot(2).getItem().getCount() == expected, "restoration cannot reroll a completed unit");
                reopened.removed(player);
            }
        }
        ctx.assertTrue(!FurnaceResearchRuntime.eligibleMineral(new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.IRON_NUGGET)), "finished Auto Smelt products do not recursively qualify as mineral inputs");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper ctx, Method method) throws ReflectiveOperationException {
        method.invoke(this, ctx);
    }
}
