package dev.itzgamesgd.moreminecrafts.furnace;

import dev.itzgamesgd.moreminecrafts.equipment.PortableFurnaceMenu;
import dev.itzgamesgd.moreminecrafts.equipment.PortableFurnaceItem;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import dev.itzgamesgd.moreminecrafts.progression.ResearchProgressLookup;
import dev.itzgamesgd.moreminecrafts.progression.ResearchProgressState;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.RegistryOps;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/** Integration tests for attribution events, persistent block-entity data, and portable lifecycle. */
public final class FurnaceResearchInfrastructureGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void furnaceOpenInteractionMultiplePlayersAndCloseReopenUseServerEvents(GameTestHelper ctx) {
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ServerPlayer first = ctx.makeMockServerPlayerInLevel();
        ServerPlayer second = ctx.makeMockServerPlayerInLevel();

        FurnaceResearchAttribution.opened(furnace, first);
        assertOwner(ctx, furnace, first.getUUID(), "opening establishes the first player's attribution");
        furnace.stopOpen(first);
        assertOwner(ctx, furnace, first.getUUID(), "closing the GUI retains attribution");

        FurnaceResearchAttribution.opened(furnace, second);
        assertOwner(ctx, furnace, second.getUUID(), "a later GUI open becomes the current attribution");
        FurnaceMenu menu = new FurnaceMenu(81, first.getInventory(), furnace, new SimpleContainerData(4));
        first.containerMenu = menu;
        menu.setCarried(new ItemStack(Items.RAW_IRON));
        menu.clicked(0, 0, ContainerInput.PICKUP, first);
        ctx.assertTrue(furnace.getItem(0).is(Items.RAW_IRON), "the server committed the input-slot transfer");
        assertOwner(ctx, furnace, first.getUUID(), "actual input interaction supersedes another player's open");

        menu.setCarried(new ItemStack(Items.COAL));
        menu.clicked(1, 0, ContainerInput.PICKUP, first);
        ctx.assertTrue(furnace.getItem(1).is(Items.COAL), "the server committed the fuel-slot transfer");
        assertOwner(ctx, furnace, first.getUUID(), "actual fuel interaction retains its interacting player");
        furnace.stopOpen(second);
        assertOwner(ctx, furnace, first.getUUID(), "closing another viewer does not clear actual attribution");

        FurnaceResearchAttribution.opened(furnace, second);
        assertOwner(ctx, furnace, first.getUUID(), "reopening cannot supersede actual inventory interaction");
        ctx.succeed();
    }

    @GameTest(maxTicks = 20)
    public void hopperOnlyFurnaceProcessingDoesNotCreatePlayerAttribution(GameTestHelper ctx) {
        BlockPos furnacePos = ctx.absolutePos(new BlockPos(2, 2, 2));
        BlockPos inputHopperPos = ctx.absolutePos(new BlockPos(2, 3, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.FURNACE);
        ctx.setBlock(new BlockPos(2, 3, 2), Blocks.HOPPER);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) ctx.getLevel().getBlockEntity(furnacePos);
        var hopper = (net.minecraft.world.level.block.entity.HopperBlockEntity)
                ctx.getLevel().getBlockEntity(inputHopperPos);
        hopper.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));
        ctx.runAfterDelay(5, () -> {
            ctx.assertTrue(furnace.getItem(0).is(Items.RAW_IRON), "the hopper inserted the smelting input");
            ctx.assertTrue(((FurnaceResearchStateAccess) furnace).moreminecrafts$getResearchState()
                    .attributedPlayer().isEmpty(), "automation alone leaves the furnace unattributed");
            ctx.succeed();
        });
    }

    @GameTest
    public void furnaceStateSurvivesBlockEntitySaveLoadLegacyDefaultsAndBreakReplace(GameTestHelper ctx) {
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) ctx.getLevel().getBlockEntity(pos);
        UUID owner = UUID.randomUUID();
        ((FurnaceResearchStateAccess) furnace).moreminecrafts$setResearchState(
                new FurnaceResearchState(Optional.of(owner), 0.375D, 0.625D));

        CompoundTag saved = saveAdditional(furnace, ctx);
        FurnaceBlockEntity reloaded = new FurnaceBlockEntity(pos, Blocks.FURNACE.defaultBlockState());
        loadAdditional(reloaded, saved, ctx);
        ctx.assertTrue(((FurnaceResearchStateAccess) reloaded).moreminecrafts$getResearchState()
                        .equals(new FurnaceResearchState(Optional.of(owner), 0.375D, 0.625D)),
                "chunk block-entity serialization restores owner and fractional carry state");

        FurnaceBlockEntity legacy = new FurnaceBlockEntity(pos, Blocks.FURNACE.defaultBlockState());
        loadAdditional(legacy, new CompoundTag(), ctx);
        ctx.assertTrue(((FurnaceResearchStateAccess) legacy).moreminecrafts$getResearchState()
                        .equals(FurnaceResearchState.empty()),
                "pre-infrastructure furnace saves load with safe empty defaults");

        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.FURNACE);
        AbstractFurnaceBlockEntity replaced = (AbstractFurnaceBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ctx.assertTrue(((FurnaceResearchStateAccess) replaced).moreminecrafts$getResearchState()
                        .equals(FurnaceResearchState.empty()),
                "breaking and replacing a furnace starts with no inherited attribution");
        ctx.succeed();
    }

    @GameTest
    public void savedOfflineProgressLookupReadsOnlyTheExistingResearchAttachment(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setAttached(KnowledgeRuntime.STATE,
                ResearchProgressState.empty().with("research.v3.research.06", 2));
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,
                ctx.getLevel().registryAccess());
        player.saveWithoutId(output);

        var saved = ResearchProgressLookup.decodePlayerData(ctx.getLevel().getServer(), output.buildResult());
        ctx.assertTrue(saved.isPresent() && saved.orElseThrow()
                        .researchLevel("v3.research.06") == 2,
                "offline lookup decodes purchased levels from Fabric's persisted player attachment");
        ctx.assertTrue(ResearchProgressLookup.decodePlayerData(ctx.getLevel().getServer(), new CompoundTag()).isEmpty(),
                "missing or malformed saved attachment data resolves to no research state");
        ctx.succeed();
    }

    @GameTest
    public void portableFurnaceInventoryAndCookingPersistOnItsItem(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ItemStack opener = new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.PORTABLE_FURNACE);
        player.setItemInHand(InteractionHand.MAIN_HAND, opener);
        UUID owner = player.getUUID();
        ((PortableFurnaceItem) opener.getItem()).use(ctx.getLevel(), player, InteractionHand.MAIN_HAND);
        ctx.assertTrue(player.containerMenu instanceof PortableFurnaceMenu,
                "using the item still opens its existing session menu");
        PortableFurnaceMenu menu = (PortableFurnaceMenu) player.containerMenu;
        int openerSlot = -1;
        for (int slot = 3; slot < menu.slots.size(); slot++)
            if (menu.getSlot(slot).getItem() == opener) openerSlot = slot;
        ctx.assertTrue(openerSlot >= 0, "the opener remains in the player's inventory while the menu is open");
        menu.clicked(openerSlot, -1, ContainerInput.THROW, player);
        ctx.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND) == opener && opener.getCount() == 1,
                "the active portable furnace cannot be removed while its menu is bound to that item");
        menu.setCarried(new ItemStack(Items.RAW_IRON, 2));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        menu.setCarried(new ItemStack(Items.COAL, 1));
        menu.clicked(1, 0, ContainerInput.PICKUP, player);
        menu.serverTick(player);
        var active = opener.getOrDefault(ModDataComponents.PORTABLE_FURNACE_CONTENTS,
                dev.itzgamesgd.moreminecrafts.equipment.PortableFurnaceContents.empty(67));
        ctx.assertTrue(active.items().get(0).is(Items.RAW_IRON)
                        && active.items().get(0).getCount() == 2
                        && active.items().get(1).isEmpty()
                        && active.burnTime() > 0 && active.burnDuration() > 0 && active.cookTime() == 1,
                "menu inventory and live cooking progress are written to the opener item");
        var savedItemOps = RegistryOps.create(NbtOps.INSTANCE, ctx.getLevel().registryAccess());
        var encodedOpener = ItemStack.CODEC.encodeStart(savedItemOps, opener).getOrThrow();
        ItemStack restoredOpener = ItemStack.CODEC.parse(savedItemOps, encodedOpener).getOrThrow();
        ctx.assertTrue(restoredOpener.getOrDefault(ModDataComponents.PORTABLE_FURNACE_RESEARCH_STATE,
                        FurnaceResearchState.empty()).attributedPlayer().orElseThrow().equals(owner),
                "portable metadata survives the existing item-stack save format");
        menu.removed(player);

        ctx.assertTrue(menu.getSlot(0).getItem().getCount() == 2,
                "closing keeps the furnace inventory in its persistent item state");
        var dropped = ctx.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(player.blockPosition()).inflate(8.0D)).stream()
                .anyMatch(item -> item.getItem().is(Items.RAW_IRON) && item.getItem().getCount() == 2);
        ctx.assertTrue(!dropped, "closing does not drop a second copy of the stored inventory");
        ctx.assertTrue(opener.getOrDefault(ModDataComponents.PORTABLE_FURNACE_RESEARCH_STATE,
                        FurnaceResearchState.empty()).attributedPlayer().orElseThrow().equals(owner),
                "portable attribution remains item-backed and separate from processing effects");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, opener);
        ((PortableFurnaceItem) opener.getItem()).use(ctx.getLevel(), player, InteractionHand.OFF_HAND);
        ctx.assertTrue(player.containerMenu instanceof PortableFurnaceMenu,
                "using the same item reopens its portable furnace");
        PortableFurnaceMenu reopened = (PortableFurnaceMenu) player.containerMenu;
        var reopenedState = opener.getOrDefault(ModDataComponents.PORTABLE_FURNACE_CONTENTS,
                dev.itzgamesgd.moreminecrafts.equipment.PortableFurnaceContents.empty(67));
        ctx.assertTrue(reopened.getSlot(0).getItem().is(Items.RAW_IRON)
                        && reopened.getSlot(0).getItem().getCount() == 2
                        && reopenedState.cookTime() == 1 && reopenedState.burnTime() > 0,
                "reopening restores all slots and exact in-progress cooking state");
        ctx.succeed();
    }

    private static void assertOwner(GameTestHelper ctx, AbstractFurnaceBlockEntity furnace,
                                    UUID expected, String message) {
        ctx.assertTrue(((FurnaceResearchStateAccess) furnace).moreminecrafts$getResearchState()
                .attributedPlayer().filter(expected::equals).isPresent(), message);
    }

    private static CompoundTag saveAdditional(AbstractFurnaceBlockEntity furnace, GameTestHelper ctx) {
        try {
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,
                    ctx.getLevel().registryAccess());
            Method save = AbstractFurnaceBlockEntity.class.getDeclaredMethod("saveAdditional", ValueOutput.class);
            save.setAccessible(true);
            save.invoke(furnace, output);
            return output.buildResult();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("could not save furnace block-entity state", exception);
        }
    }

    private static void loadAdditional(AbstractFurnaceBlockEntity furnace, CompoundTag data, GameTestHelper ctx) {
        try {
            Method load = AbstractFurnaceBlockEntity.class.getDeclaredMethod("loadAdditional", ValueInput.class);
            load.setAccessible(true);
            load.invoke(furnace, TagValueInput.create(ProblemReporter.DISCARDING,
                    ctx.getLevel().registryAccess(), data));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("could not load furnace block-entity state", exception);
        }
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
