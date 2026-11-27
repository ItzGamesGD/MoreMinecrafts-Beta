package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * {@code xpq.cmb.wither_skulls_3}: genuine Wither Skeleton drops are marked on the item entity and credited
 * once, on the first player pickup, to whichever player collects them.
 */
public final class QuestWitherSkullGameTests implements CustomTestMethodInvoker {
    private static final String QUEST = "xpq.cmb.wither_skulls_3";
    private static final ResourceKey<LootTable> ALWAYS_SKULL = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.parse("moreminecrafts-gametest:wither_skull_always"));

    private static BlockPos at(GameTestHelper ctx) { return ctx.absolutePos(new BlockPos(1, 1, 1)); }

    /** Spawns {@code type} whose death loot is a guaranteed skull, and kills it. Returns the dropped item entities. */
    private static java.util.List<ItemEntity> killDroppingSkull(GameTestHelper ctx, ServerPlayer killer,
                                                                EntityType<? extends LivingEntity> type) {
        return kill(ctx, killer, type, ALWAYS_SKULL.identifier().toString());
    }

    private static java.util.List<ItemEntity> kill(GameTestHelper ctx, ServerPlayer killer,
                                                   EntityType<? extends LivingEntity> type, String lootTable) {
        ServerLevel level = ctx.getLevel();
        LivingEntity victim = type.create(level, EntitySpawnReason.COMMAND);
        BlockPos pos = at(ctx);
        victim.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        victim.saveWithoutId(out);
        var tag = out.buildResult();
        tag.putString("DeathLootTable", lootTable);
        victim.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        level.addFreshEntity(victim);
        victim.hurtServer(level, killer.damageSources().playerAttack(killer), 100_000F);
        return items(ctx);
    }

    private static java.util.List<ItemEntity> items(GameTestHelper ctx) {
        var box = new net.minecraft.world.phys.AABB(at(ctx)).inflate(4.0D);
        return ctx.getLevel().getEntitiesOfClass(ItemEntity.class, box,
                e -> e.getItem().is(Items.WITHER_SKELETON_SKULL));
    }

    private static void clearItems(GameTestHelper ctx) { items(ctx).forEach(ItemEntity::discard); }

    private static ItemEntity plainSkull(GameTestHelper ctx, int count) {
        BlockPos pos = at(ctx);
        ItemEntity item = new ItemEntity(ctx.getLevel(), pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                new ItemStack(Items.WITHER_SKELETON_SKULL, count));
        item.setNoPickUpDelay();
        ctx.getLevel().addFreshEntity(item);
        return item;
    }

    private static void touch(ItemEntity item, ServerPlayer player) {
        item.setNoPickUpDelay();
        item.playerTouch(player);
    }

    private static int credited(ServerPlayer player) {
        return QuestRules.counter(KnowledgeRuntime.state(player), QuestWitherSkullProvenance.DROPS);
    }

    private static int progress(ServerPlayer player) {
        XpQuestTracking.reconcile(player);
        return KnowledgeRuntime.state(player).questProgress(QUEST);
    }

    private static int skulls(ServerPlayer player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.WITHER_SKELETON_SKULL)) total += stack.getCount();
        }
        return total;
    }

    @GameTest
    public void questIsRegisteredWithApprovedIdentity(GameTestHelper ctx) {
        QuestDefinition quest = QuestCatalog.byId(QUEST);
        ctx.assertTrue(quest != null && quest.objectiveTarget() == 3 && quest.rewardRawXp() == 1_100,
                "registered with target 3 and 1,100 raw XP");
        ctx.succeed();
    }

    @GameTest
    public void firstPickupCreditsTheCollectorNotTheKiller(GameTestHelper ctx) {
        ServerPlayer killer = ctx.makeMockServerPlayerInLevel();
        ServerPlayer collector = ctx.makeMockServerPlayerInLevel();
        var drops = killDroppingSkull(ctx, killer, EntityTypes.WITHER_SKELETON);
        ctx.assertTrue(drops.size() == 1 && QuestWitherSkullProvenance.isGenuine(drops.getFirst()),
                "the Wither Skeleton's loot-table skull is marked as a genuine drop");
        touch(drops.getFirst(), collector);
        ctx.assertTrue(skulls(collector) == 1 && credited(collector) == 1 && progress(collector) == 1,
                "the collector is credited");
        ctx.assertTrue(credited(killer) == 0 && progress(killer) == 0, "the killer, who collected nothing, is not");
        ctx.succeed();
    }

    @GameTest
    public void threeDistinctDropsCompleteTheQuestAndMoreDoNotOvercount(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        for (int i = 1; i <= 3; i++) {
            clearItems(ctx);
            var drops = killDroppingSkull(ctx, player, EntityTypes.WITHER_SKELETON);
            touch(drops.getFirst(), player);
            ctx.assertTrue(progress(player) == i, "progress after drop " + i + " was " + progress(player));
        }
        ctx.assertTrue(KnowledgeRuntime.state(player).questComplete(QUEST), "three credited drops complete the quest");
        ctx.assertTrue(QuestProgressService.claim(player, QUEST) == QuestProgressService.ClaimResult.CLAIMED,
                "the completed quest claims");
        ctx.succeed();
    }

    @GameTest
    public void duplicatePickupCallbacksAndRepickupOfADroppedSkullCreditOnce(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        var drop = killDroppingSkull(ctx, player, EntityTypes.WITHER_SKELETON).getFirst();
        touch(drop, player);
        touch(drop, player);
        touch(drop, player);
        ctx.assertTrue(credited(player) == 1, "repeated callbacks for one pickup credit once");
        clearItems(ctx);
        // Dropping from an inventory always creates a brand-new item entity (Player.drop), never the old one.
        player.getInventory().clearContent();
        plainSkull(ctx, 1);
        var redropped = items(ctx);
        ctx.assertTrue(redropped.size() == 1 && !QuestWitherSkullProvenance.isGenuine(redropped.getFirst()),
                "a skull the player dropped has no provenance");
        touch(redropped.getFirst(), player);
        ctx.assertTrue(skulls(player) == 1 && credited(player) == 1 && progress(player) == 1,
                "dropping and re-picking an already credited skull grants nothing");
        ctx.succeed();
    }

    @GameTest
    public void unknownOriginSkullsNeverCredit(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ServerPlayer creative = QuestPersistentPlayers.mock(ctx.getLevel(), GameType.CREATIVE);
        // Command / chest / unknown source: an item entity nobody marked.
        touch(plainSkull(ctx, 1), player);
        // A real item entity with the right stack, spawned outside a Wither Skeleton's death loot.
        touch(plainSkull(ctx, 2), creative);
        ctx.assertTrue(credited(player) == 0 && credited(creative) == 0, "unmarked skull entities never credit");
        // A different mob whose loot table yields a skull is not a Wither Skeleton.
        clearItems(ctx);
        var zombie = killDroppingSkull(ctx, player, EntityTypes.ZOMBIE);
        ctx.assertTrue(zombie.size() == 1 && !QuestWitherSkullProvenance.isGenuine(zombie.getFirst()),
                "a non-Wither-Skeleton drop is not genuine");
        touch(zombie.getFirst(), player);
        // A Wither Skeleton's equipment drop (not its death loot table) is not a genuine drop either.
        clearItems(ctx);
        ServerLevel level = ctx.getLevel();
        Mob skeleton = (Mob) EntityTypes.WITHER_SKELETON.create(level, EntitySpawnReason.COMMAND);
        BlockPos pos = at(ctx);
        skeleton.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.WITHER_SKELETON_SKULL));
        skeleton.setDropChance(EquipmentSlot.HEAD, 1.0F);
        TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        skeleton.saveWithoutId(saved);
        var skeletonTag = saved.buildResult();
        skeletonTag.putString("DeathLootTable", "moreminecrafts-gametest:no_loot"); // only the equipment can drop
        skeleton.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), skeletonTag));
        level.addFreshEntity(skeleton);
        skeleton.hurtServer(level, player.damageSources().playerAttack(player), 100_000F);
        ctx.assertTrue(!items(ctx).isEmpty(), "the equipment skull dropped");
        for (ItemEntity item : items(ctx)) {
            ctx.assertTrue(!QuestWitherSkullProvenance.isGenuine(item), "equipment-slot skull is not a loot drop: " + item
                    + " attached=" + item.getAttached(QuestWitherSkullProvenance.GENUINE_DROP) + " n=" + items(ctx).size());
            touch(item, player);
        }
        ctx.assertTrue(credited(player) == 0, "no invalid origin ever credits: " + credited(player));
        ctx.succeed();
    }

    @GameTest
    public void genuineAndOrdinarySkullsNeverMergeButGenuineDropsDo(GameTestHelper ctx) throws Exception {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        var first = killDroppingSkull(ctx, player, EntityTypes.WITHER_SKELETON).getFirst();
        ItemEntity ordinary = plainSkull(ctx, 1);
        Method merge = ItemEntity.class.getDeclaredMethod("tryToMerge", ItemEntity.class);
        merge.setAccessible(true);
        merge.invoke(first, ordinary);
        ctx.assertTrue(!first.isRemoved() && !ordinary.isRemoved() && first.getItem().getCount() == 1
                && ordinary.getItem().getCount() == 1, "a genuine and an ordinary skull stay separate entities");
        ordinary.discard();
        var second = killDroppingSkull(ctx, player, EntityTypes.WITHER_SKELETON).stream()
                .filter(e -> e != first).findFirst().orElseThrow();
        merge.invoke(first, second);
        ItemEntity survivor = first.isRemoved() ? second : first;
        ctx.assertTrue(survivor.getItem().getCount() == 2 && QuestWitherSkullProvenance.isGenuine(survivor),
                "two genuine drops merge into one genuine stack of two");
        touch(survivor, player);
        ctx.assertTrue(credited(player) == 2 && progress(player) == 2, "the merged stack credits both skulls");
        ctx.succeed();
    }

    @GameTest
    public void partialPickupCreditsOnlyWhatTransferredAndKeepsTheRestGenuine(GameTestHelper ctx) {
        ServerPlayer player = QuestPersistentPlayers.mock(ctx.getLevel(), GameType.SURVIVAL);
        player.getAbilities().instabuild = false; // the mock joins with the server's default creative abilities
        player.getInventory().clearContent();
        for (int i = 0; i < 36; i++)
            player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        player.getInventory().setItem(5, new ItemStack(Items.WITHER_SKELETON_SKULL, 63));
        var drop = killDroppingSkull(ctx, player, EntityTypes.WITHER_SKELETON).getFirst();
        drop.setItem(new ItemStack(Items.WITHER_SKELETON_SKULL, 3)); // split across stacks: only one fits
        touch(drop, player);
        ctx.assertTrue(credited(player) == 1 && drop.getItem().getCount() == 2 && !drop.isRemoved()
                && QuestWitherSkullProvenance.isGenuine(drop), "one transferred, two remain genuine: credited=" + credited(player)
                + " left=" + drop.getItem().getCount() + " removed=" + drop.isRemoved()
                + " genuine=" + QuestWitherSkullProvenance.isGenuine(drop) + " held=" + skulls(player));
        player.getInventory().setItem(5, ItemStack.EMPTY);
        touch(drop, player);
        ctx.assertTrue(credited(player) == 3, "the remainder is credited when later collected: " + credited(player));
        touch(drop, player);
        ctx.assertTrue(credited(player) == 3, "nothing further to credit");
        ctx.succeed();
    }

    @GameTest
    public void provenanceSurvivesEntitySaveAndReload(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        var drop = killDroppingSkull(ctx, player, EntityTypes.WITHER_SKELETON).getFirst();
        ServerLevel level = ctx.getLevel();
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        drop.saveWithoutId(out);
        var tag = out.buildResult();
        drop.discard();
        ItemEntity reloaded = new ItemEntity(EntityTypes.ITEM, level);
        reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        level.addFreshEntity(reloaded);
        ctx.assertTrue(QuestWitherSkullProvenance.isGenuine(reloaded), "provenance written to and read from entity NBT");
        touch(reloaded, player);
        ctx.assertTrue(credited(player) == 1, "a reloaded genuine drop still credits its first pickup");
        // An ordinary skull saved and reloaded stays ordinary.
        ItemEntity ordinary = plainSkull(ctx, 1);
        TagValueOutput out2 = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        ordinary.saveWithoutId(out2);
        ordinary.discard();
        ItemEntity reloadedOrdinary = new ItemEntity(EntityTypes.ITEM, level);
        reloadedOrdinary.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out2.buildResult()));
        level.addFreshEntity(reloadedOrdinary);
        touch(reloadedOrdinary, player);
        ctx.assertTrue(!QuestWitherSkullProvenance.isGenuine(reloadedOrdinary) && credited(player) == 1,
                "reload never invents provenance");
        ctx.succeed();
    }

    @GameTest
    public void hopperIntakeDropsProvenanceAndFailsClosed(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        var drop = killDroppingSkull(ctx, player, EntityTypes.WITHER_SKELETON).getFirst();
        var container = new net.minecraft.world.SimpleContainer(5);
        ctx.assertTrue(net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(container, drop),
                "the hopper path consumes the item entity");
        ItemStack held = container.getItem(0);
        ctx.assertTrue(held.is(Items.WITHER_SKELETON_SKULL), "skull moved into the container");
        // Once out of the item entity the skull is an ordinary item: dropping it again carries no provenance.
        ItemEntity out = new ItemEntity(ctx.getLevel(), at(ctx).getX() + 0.5D, at(ctx).getY(), at(ctx).getZ() + 0.5D, held.copy());
        ctx.getLevel().addFreshEntity(out);
        touch(out, player);
        ctx.assertTrue(credited(player) == 0, "a skull that left its item entity via a hopper never credits");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
