package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import io.netty.channel.embedded.EmbeddedChannel;

/** Lifecycle-focused accessory checks; menu callback tests live in AccessoryEquipmentGameTests. */
public final class AccessoryLifecycleGameTests implements CustomTestMethodInvoker {
    private static final UUID RESTART_PLAYER_ID = UUID.fromString("3c1f6bfa-0c58-4999-8fa8-e4216f452dc9");
    private static final AccessoryDefinition[] SLOT_ITEMS = {
            AccessoryDefinition.SOUL_CHAIN,
            AccessoryDefinition.EMBER_RING,
            AccessoryDefinition.SEED_OF_LIFE,
            AccessoryDefinition.SNOW_CHARM,
            AccessoryDefinition.WHISPER_OF_THE_SEA
    };

    @GameTest
    public void accessoryAttachmentSurvivesPlayerEntitySaveAndReload(GameTestHelper ctx) {
        ServerPlayer original = mockPlayer(ctx);
        equipFixture(original);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, original.registryAccess());
        original.saveWithoutId(output);

        ServerPlayer restored = mockPlayer(ctx);
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, restored.registryAccess(), output.buildResult()));
        assertFixture(ctx, restored, "player entity save/load");
        ctx.succeed();
    }

    @GameTest
    public void categorySlotsPreventDuplicatesAndOnlyCombatCanBeActive(GameTestHelper ctx) {
        ServerPlayer player = mockPlayer(ctx);
        equipFixture(player);
        AccessoryEquipmentContainer container = new AccessoryEquipmentContainer(player);
        ctx.assertTrue(container.getItem(AccessoryCategory.COMBAT.ordinal()).getItem()
                        == ModItems.ACCESSORIES.get(AccessoryDefinition.SOUL_CHAIN),
                "the only manually active item occupies the Combat slot");
        ctx.assertTrue(container.getItem(AccessoryCategory.COMBAT.ordinal()).getCount() == 1,
                "Combat accepts one active accessory");
        for (int slot = 0; slot < AccessoryLoadout.SIZE; slot++) {
            ctx.assertTrue(container.getItem(slot).getCount() == 1, "category slot " + slot + " contains one item");
            ctx.assertTrue(container.canPlaceItem(slot, container.getItem(slot)), "category item remains valid in its slot");
        }
        ctx.assertTrue(!container.canPlaceItem(AccessoryCategory.COMBAT.ordinal(),
                        container.getItem(AccessoryCategory.DEFENSE_SURVIVAL.ordinal())),
                "second category placement into Combat is rejected");
        ctx.assertTrue(container.getItem(AccessoryCategory.COMBAT.ordinal()).getItem()
                        == ModItems.ACCESSORIES.get(AccessoryDefinition.SOUL_CHAIN),
                "rejected duplicate placement leaves the existing active item intact");
        var loadout = player.getAttachedOrElse(EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty());
        for (int slot = 0; slot < AccessoryLoadout.SIZE; slot++) {
            ctx.assertTrue(loadout.canManuallyActivate(slot) == (slot == AccessoryCategory.COMBAT.ordinal()),
                    "only the single Combat category is active at slot " + slot);
        }
        ctx.succeed();
    }

    @GameTest
    public void accessoriesSurviveNetherAndEndTransfers(GameTestHelper ctx) {
        ServerPlayer player = mockPlayer(ctx);
        equipFixture(player);
        var server = ctx.getLevel().getServer();
        var nether = server.getLevel(net.minecraft.world.level.Level.NETHER);
        var theEnd = server.getLevel(net.minecraft.world.level.Level.END);
        ctx.assertTrue(nether != null && theEnd != null, "Nether and End server levels are available");
        int before = accessoryCount(player);

        ctx.assertTrue(player.teleportTo(nether, 0.5D, 80.0D, 0.5D, java.util.Set.<Relative>of(),
                player.getYRot(), player.getXRot(), true), "player transfers from Overworld to Nether");
        assertFixture(ctx, player, "Nether transfer");
        ctx.assertTrue(accessoryCount(player) == before, "Nether transfer conserves equipped accessories");
        ctx.assertTrue(player.teleportTo(ctx.getLevel(), 0.5D, 80.0D, 0.5D, java.util.Set.<Relative>of(),
                player.getYRot(), player.getXRot(), true), "player transfers from Nether to Overworld");
        assertFixture(ctx, player, "Nether return");

        ctx.assertTrue(player.teleportTo(theEnd, 0.5D, 80.0D, 0.5D, java.util.Set.<Relative>of(),
                player.getYRot(), player.getXRot(), true), "player transfers from Overworld to End");
        assertFixture(ctx, player, "End transfer");
        ctx.assertTrue(accessoryCount(player) == before, "End transfer conserves equipped accessories");
        ctx.assertTrue(player.teleportTo(ctx.getLevel(), 0.5D, 80.0D, 0.5D, java.util.Set.<Relative>of(),
                player.getYRot(), player.getXRot(), true), "player transfers from End to Overworld");
        assertFixture(ctx, player, "End return");
        ctx.assertTrue(accessoryCount(player) == before, "round-trip dimension transfers neither duplicate nor lose accessories");
        ctx.succeed();
    }

    @GameTest
    public void accessoryDeathDropsOnceAndRespawnStartsEmpty(GameTestHelper ctx) {
        ServerPlayer player = mockPlayer(ctx);
        player.setGameMode(GameType.SURVIVAL);
        equipFixture(player);
        player.die(ctx.getLevel().damageSources().generic());

        List<ItemEntity> drops = accessoryDrops(ctx, player);
        ctx.assertTrue(drops.size() == SLOT_ITEMS.length, "normal death creates exactly five accessory drops");
        int firstDeathDrops = drops.size();
        for (int slot = 0; slot < SLOT_ITEMS.length; slot++) {
            AccessoryDefinition definition = SLOT_ITEMS[slot];
            List<ItemEntity> matching = drops.stream()
                    .filter(drop -> drop.getItem().getItem() == ModItems.ACCESSORIES.get(definition))
                    .toList();
            ctx.assertTrue(matching.size() == 1, "normal death drops " + definition.registryId() + " exactly once");
            assertStackMetadata(ctx, matching.getFirst().getItem(), slot, "death drop");
        }
        // Remove the already-accounted first-death drops so later death processing is isolated.
        drops.forEach(ItemEntity::discard);

        ServerPlayer respawned = ctx.getLevel().getServer().getPlayerList().respawn(
                player, false, Entity.RemovalReason.KILLED);
        ctx.assertTrue(new AccessoryEquipmentContainer(respawned).isEmpty(), "normal respawn has empty accessory slots");
        respawned.setPos(player.getX(), player.getY(), player.getZ());
        respawned.die(ctx.getLevel().damageSources().generic());
        List<ItemEntity> repeatedDeathDrops = accessoryDrops(ctx, respawned);
        ctx.assertTrue(new AccessoryEquipmentContainer(respawned).isEmpty(),
                "repeated death leaves the respawned accessory source empty");
        ctx.assertTrue(repeatedDeathDrops.isEmpty(),
                "repeated death creates no new accessory drops after " + firstDeathDrops + " first-death drops");
        ctx.succeed();
    }

    @GameTest
    public void keepInventoryRetainsStacksAndMetadataWithoutDrops(GameTestHelper ctx) {
        var rules = ctx.getLevel().getGameRules();
        boolean previous = rules.get(GameRules.KEEP_INVENTORY);
        rules.set(GameRules.KEEP_INVENTORY, true, ctx.getLevel().getServer());
        try {
            ctx.assertTrue(rules.get(GameRules.KEEP_INVENTORY), "keepInventory test rule is enabled");
            ServerPlayer player = mockPlayer(ctx);
            ctx.assertTrue(player.level().getGameRules().get(GameRules.KEEP_INVENTORY),
                    "keepInventory is enabled for the tested player's level");
            equipFixture(player);
            player.die(ctx.getLevel().damageSources().generic());
            List<ItemEntity> drops = accessoryDrops(ctx, player);
            ctx.assertTrue(drops.isEmpty(), "keepInventory produces no accessory drops; found "
                    + drops.stream().map(drop -> drop.getItem().getHoverName().getString() + "@" + drop.blockPosition())
                    .toList());
            assertFixture(ctx, player, "keepInventory death");

            ServerPlayer respawned = ctx.getLevel().getServer().getPlayerList().respawn(
                    player, false, Entity.RemovalReason.KILLED);
            assertFixture(ctx, respawned, "keepInventory respawn");
        } finally {
            rules.set(GameRules.KEEP_INVENTORY, previous, ctx.getLevel().getServer());
        }
        ctx.succeed();
    }

    /** Three separate GameTest JVMs use this same world and player file to verify repeated restarts. */
    @GameTest(maxTicks = 180)
    public void accessoriesSurviveRepeatedServerRestarts(GameTestHelper ctx) {
        String phase = System.getProperty("moreminecrafts.accessory.restart.phase");
        if (phase == null || phase.isBlank()) {
            ctx.succeed();
            return;
        }
        ServerPlayer player = restartPlayer(ctx, "write".equals(phase));
        if ("write".equals(phase)) {
            equipFixture(player);
            saveRestartFixture(ctx, player, phase);
            return;
        }
        ctx.assertTrue("reload1".equals(phase) || "reload2".equals(phase), "restart phase is recognized");
        assertFixture(ctx, player, phase);
        saveRestartFixture(ctx, player, phase);
    }

    private static ServerPlayer restartPlayer(GameTestHelper ctx, boolean create) {
        var level = ctx.getLevel();
        var profile = new com.mojang.authlib.GameProfile(RESTART_PLAYER_ID, "accessory-restart-player");
        var cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        if (!create) {
            var saved = level.getServer().getPlayerList().loadPlayerData(player.nameAndId());
            ctx.assertTrue(saved.isPresent(), "Minecraft player data storage returns the saved accessory player");
            player.load(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), saved.orElseThrow()));
        }
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void equipFixture(ServerPlayer player) {
        AccessoryEquipmentContainer container = new AccessoryEquipmentContainer(player);
        for (int slot = 0; slot < SLOT_ITEMS.length; slot++) {
            int accessorySlot = slot;
            ItemStack stack = new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.ACCESSORIES.get(SLOT_ITEMS[slot]));
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("qa-slot-" + slot));
            stack.set(ModDataComponents.ENHANCEMENT_LEVEL, slot + 1);
            stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY
                    .update(tag -> tag.putInt("qa_slot", accessorySlot)));
            container.setItem(slot, stack);
        }
    }

    private static ServerPlayer mockPlayer(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        net.minecraft.core.BlockPos position = ctx.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        player.setPos(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
        return player;
    }

    private static void assertFixture(GameTestHelper ctx, ServerPlayer player, String scenario) {
        AccessoryEquipmentContainer container = new AccessoryEquipmentContainer(player);
        ctx.assertTrue(container.getContainerSize() == 5, scenario + " retains exactly five slots");
        for (int slot = 0; slot < SLOT_ITEMS.length; slot++) {
            ItemStack stack = container.getItem(slot);
            ctx.assertTrue(stack.getItem() == ModItems.ACCESSORIES.get(SLOT_ITEMS[slot]),
                    scenario + " retains exact item identity in slot " + slot);
            assertStackMetadata(ctx, stack, slot, scenario);
            ctx.assertTrue(stack.getCount() == 1, scenario + " conserves one stack in slot " + slot);
        }
    }

    private static void assertStackMetadata(GameTestHelper ctx, ItemStack stack, int slot, String scenario) {
        ctx.assertTrue(Component.literal("qa-slot-" + slot).equals(stack.get(DataComponents.CUSTOM_NAME)),
                scenario + " retains custom name for slot " + slot);
        ctx.assertTrue(Integer.valueOf(slot + 1).equals(stack.get(ModDataComponents.ENHANCEMENT_LEVEL)),
                scenario + " retains enhancement metadata for slot " + slot);
        ctx.assertTrue(java.util.Optional.of(slot).equals(stack.get(DataComponents.CUSTOM_DATA).copyTag().getInt("qa_slot")),
                scenario + " retains custom component data for slot " + slot);
    }

    private static List<ItemEntity> accessoryDrops(GameTestHelper ctx, ServerPlayer player) {
        AABB bounds = new AABB(player.blockPosition()).inflate(2.0D);
        return ctx.getLevel().getEntitiesOfClass(ItemEntity.class, bounds,
                item -> SLOT_ITEMS.length > 0 && java.util.Arrays.stream(SLOT_ITEMS)
                        .anyMatch(definition -> item.getItem().getItem() == ModItems.ACCESSORIES.get(definition))
                        && item.getItem().has(DataComponents.CUSTOM_NAME)
                        && item.getItem().get(DataComponents.CUSTOM_NAME).getString().startsWith("qa-slot-"));
    }

    private static void saveRestartFixture(GameTestHelper ctx, ServerPlayer player, String phase) {
        ctx.getLevel().getServer().getPlayerList().saveAll();
        ctx.getLevel().getChunkSource().save(true);
        System.out.println("ACCESSORY_RESTART_" + phase.toUpperCase()
                + " player=" + RESTART_PLAYER_ID + " slots=5 metadata=custom_name,enhancement,custom_data");
        ctx.succeed();
    }

    private static int accessoryCount(ServerPlayer player) {
        AccessoryEquipmentContainer container = new AccessoryEquipmentContainer(player);
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) count += container.getItem(slot).getCount();
        return count;
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
