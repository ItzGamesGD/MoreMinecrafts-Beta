package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Snow Charm powder-snow sinking checks through vanilla's stuck-in-block movement path. */
public final class SnowCharmAbilityGameTests implements CustomTestMethodInvoker {
    private static final Vec3 POWDER_SNOW_MULTIPLIER = new Vec3(0.9F, 1.5D, 0.9F);
    private static final Vec3 COBWEB_MULTIPLIER = new Vec3(0.25D, 0.05F, 0.25D);
    private static final double STEP = 0.1D;
    private static final double VANILLA_SINK = -STEP * 1.5D;
    private static final double EPSILON = 1.0E-9D;

    @GameTest
    public void snowCharmReducesPowderSnowSinkingOnlyFromItsExplorationSlot(GameTestHelper ctx) {
        ServerPlayer player = mockPlayer(ctx);
        ctx.assertTrue(near(stuckMove(player, POWDER_SNOW_MULTIPLIER, -STEP).y, VANILLA_SINK),
                "without the charm powder snow keeps vanilla sinking");

        ItemStack charm = charm(0);
        player.getInventory().add(charm.copy());
        ctx.assertTrue(near(stuckMove(player, POWDER_SNOW_MULTIPLIER, -STEP).y, VANILLA_SINK),
                "an inventory-only charm has no effect");

        AccessoryEquipmentContainer equipment = new AccessoryEquipmentContainer(player);
        equipment.setItem(AccessoryCategory.COMBAT.ordinal(), charm.copy());
        ctx.assertTrue(equipment.getItem(AccessoryCategory.COMBAT.ordinal()).isEmpty(),
                "the combat slot rejects the exploration charm");
        boolean rejected = false;
        try {
            AccessoryLoadout.empty().with(AccessoryCategory.COMBAT.ordinal(), charm.copy());
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        ctx.assertTrue(rejected, "the persisted loadout cannot hold the charm outside its exploration slot");
        ctx.assertTrue(near(stuckMove(player, POWDER_SNOW_MULTIPLIER, -STEP).y, VANILLA_SINK),
                "a rejected wrong-slot equip has no effect");

        equipment.setItem(AccessoryCategory.EXPLORATION_MOBILITY.ordinal(), charm.copy());
        ctx.assertTrue(near(stuckMove(player, POWDER_SNOW_MULTIPLIER, -STEP).y, VANILLA_SINK * 0.30D),
                "+0 reduces powder snow sinking speed by exactly 70 percent");

        equipment.setItem(AccessoryCategory.EXPLORATION_MOBILITY.ordinal(), ItemStack.EMPTY);
        ctx.assertTrue(near(stuckMove(player, POWDER_SNOW_MULTIPLIER, -STEP).y, VANILLA_SINK),
                "unequip restores vanilla sinking");
        ctx.succeed();
    }

    @GameTest
    public void snowCharmUsesApprovedEnhancementTable(GameTestHelper ctx) {
        ServerPlayer player = mockPlayer(ctx);
        AccessoryEquipmentContainer equipment = new AccessoryEquipmentContainer(player);
        double[] reductions = {70, 75, 75, 80, 80, 85, 85, 90, 90, 95, 95};
        for (int level = 0; level <= 10; level++) {
            equipment.setItem(AccessoryCategory.EXPLORATION_MOBILITY.ordinal(), charm(level));
            double expected = VANILLA_SINK * (1.0D - reductions[level] / 100.0D);
            ctx.assertTrue(near(stuckMove(player, POWDER_SNOW_MULTIPLIER, -STEP).y, expected),
                    "+" + level + " reduces sinking by " + reductions[level] + " percent");
        }
        ctx.succeed();
    }

    @GameTest
    public void snowCharmLeavesRisingHorizontalAndOtherStuckBlocksUnchanged(GameTestHelper ctx) {
        ServerPlayer player = mockPlayer(ctx);
        new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.EXPLORATION_MOBILITY.ordinal(), charm(10));
        ctx.assertTrue(near(stuckMove(player, POWDER_SNOW_MULTIPLIER, STEP).y, STEP * 1.5D),
                "rising through powder snow is unchanged");
        Vec3 sideways = stuckMove(player, POWDER_SNOW_MULTIPLIER, new Vec3(STEP, -STEP, 0.0D));
        ctx.assertTrue(near(sideways.x, STEP * 0.9F) && near(sideways.y, VANILLA_SINK * 0.05D),
                "horizontal powder snow movement keeps vanilla slowing while sinking is reduced");
        ctx.assertTrue(near(stuckMove(player, COBWEB_MULTIPLIER, Blocks.COBWEB, new Vec3(0.0D, -STEP, 0.0D)).y,
                        -STEP * 0.05F),
                "cobwebs are unaffected");
        ctx.assertTrue(near(freeMove(player, -STEP).y, -STEP), "ordinary falling is unaffected");
        ctx.assertTrue(near(stuckMove(player, POWDER_SNOW_MULTIPLIER, -STEP).y, VANILLA_SINK * 0.05D)
                        && near(stuckMove(player, POWDER_SNOW_MULTIPLIER, -STEP).y, VANILLA_SINK * 0.05D),
                "repeated powder snow ticks never compound the reduction");
        ctx.succeed();
    }

    @GameTest
    public void realPowderSnowBlockAppliesTheCharmThroughVanillaEntityInside(GameTestHelper ctx) {
        BlockPos snow = ctx.absolutePos(new BlockPos(2, 4, 2));
        ctx.getLevel().setBlockAndUpdate(snow, Blocks.POWDER_SNOW.defaultBlockState());
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setPos(snow.getX() + 0.5D, snow.getY() + 0.5D, snow.getZ() + 0.5D);
        ctx.assertTrue(near(blockMove(ctx, player, snow), VANILLA_SINK), "vanilla powder snow sinking without the charm");
        new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.EXPLORATION_MOBILITY.ordinal(), charm(0));
        ctx.assertTrue(near(blockMove(ctx, player, snow), VANILLA_SINK * 0.30D),
                "the real powder snow block applies the +0 reduction");
        ctx.succeed();
    }

    @GameTest
    public void snowCharmIsIsolatedPerPlayer(GameTestHelper ctx) {
        ServerPlayer wearer = mockPlayer(ctx);
        ServerPlayer other = mockPlayer(ctx);
        new AccessoryEquipmentContainer(wearer).setItem(AccessoryCategory.EXPLORATION_MOBILITY.ordinal(), charm(0));
        ctx.assertTrue(near(stuckMove(wearer, POWDER_SNOW_MULTIPLIER, -STEP).y, VANILLA_SINK * 0.30D),
                "the wearer sinks slower");
        ctx.assertTrue(near(stuckMove(other, POWDER_SNOW_MULTIPLIER, -STEP).y, VANILLA_SINK),
                "another player in powder snow keeps vanilla sinking");
        ctx.succeed();
    }

    @GameTest
    public void snowCharmFollowsSavedEquipmentAcrossReloadDeathAndRespawn(GameTestHelper ctx) {
        ServerPlayer original = mockPlayer(ctx);
        new AccessoryEquipmentContainer(original).setItem(AccessoryCategory.EXPLORATION_MOBILITY.ordinal(), charm(3));
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, original.registryAccess());
        original.saveWithoutId(output);
        ServerPlayer restored = mockPlayer(ctx);
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, restored.registryAccess(), output.buildResult()));
        ctx.assertTrue(near(stuckMove(restored, POWDER_SNOW_MULTIPLIER, -STEP).y, VANILLA_SINK * 0.20D),
                "reload applies the saved +3 charm with no stored effect state");

        var rules = ctx.getLevel().getGameRules();
        boolean previous = rules.get(GameRules.KEEP_INVENTORY);
        rules.set(GameRules.KEEP_INVENTORY, true, ctx.getLevel().getServer());
        try {
            restored.setHealth(0.0F);
            restored.die(ctx.getLevel().damageSources().generic());
            ctx.assertTrue(SnowCharmAbility.sinkingMultiplier(restored) == 1.0D, "a dead player has no active reduction");
            ServerPlayer kept = ctx.getLevel().getServer().getPlayerList().respawn(
                    restored, false, Entity.RemovalReason.KILLED);
            place(ctx, kept);
            ctx.assertTrue(near(stuckMove(kept, POWDER_SNOW_MULTIPLIER, -STEP).y, VANILLA_SINK * 0.20D),
                    "keepInventory respawn keeps the equipped charm working");
        } finally {
            rules.set(GameRules.KEEP_INVENTORY, previous, ctx.getLevel().getServer());
        }

        ServerPlayer dropped = mockPlayer(ctx);
        new AccessoryEquipmentContainer(dropped).setItem(AccessoryCategory.EXPLORATION_MOBILITY.ordinal(), charm(0));
        dropped.setHealth(0.0F);
        dropped.die(ctx.getLevel().damageSources().generic());
        ServerPlayer respawned = ctx.getLevel().getServer().getPlayerList().respawn(
                dropped, false, Entity.RemovalReason.KILLED);
        place(ctx, respawned);
        ctx.assertTrue(near(stuckMove(respawned, POWDER_SNOW_MULTIPLIER, -STEP).y, VANILLA_SINK),
                "a normal death drops the charm and the respawned player sinks normally");
        ctx.succeed();
    }

    private static ItemStack charm(int level) {
        ItemStack stack = new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.SNOW_CHARM));
        stack.set(ModDataComponents.ENHANCEMENT_LEVEL, level);
        return stack;
    }

    private static Vec3 stuckMove(ServerPlayer player, Vec3 multiplier, double dy) {
        return stuckMove(player, multiplier, new Vec3(0.0D, dy, 0.0D));
    }

    private static Vec3 stuckMove(ServerPlayer player, Vec3 multiplier, Vec3 movement) {
        return stuckMove(player, multiplier, Blocks.POWDER_SNOW, movement);
    }

    private static Vec3 stuckMove(ServerPlayer player, Vec3 multiplier, net.minecraft.world.level.block.Block block,
                                  Vec3 movement) {
        player.makeStuckInBlock(block.defaultBlockState(), multiplier);
        return freeMove(player, movement);
    }

    private static Vec3 freeMove(ServerPlayer player, double dy) {
        return freeMove(player, new Vec3(0.0D, dy, 0.0D));
    }

    /** Moves through open air and returns the displacement, then restores the starting position. */
    private static Vec3 freeMove(ServerPlayer player, Vec3 movement) {
        Vec3 start = player.position();
        player.move(MoverType.PLAYER, movement);
        Vec3 moved = player.position().subtract(start);
        player.setPos(start);
        return moved;
    }

    private static double blockMove(GameTestHelper ctx, ServerPlayer player, BlockPos snow) {
        Blocks.POWDER_SNOW.defaultBlockState().entityInside(ctx.getLevel(), snow, player,
                InsideBlockEffectApplier.NOOP, true);
        return freeMove(player, -STEP).y;
    }

    private static ServerPlayer mockPlayer(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        place(ctx, player);
        return player;
    }

    /** Puts the player in open air inside the test area, away from any floor. */
    private static void place(GameTestHelper ctx, ServerPlayer player) {
        BlockPos position = ctx.absolutePos(new BlockPos(2, 4, 2));
        player.setPos(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D);
    }

    private static boolean near(double actual, double expected) {
        return Math.abs(actual - expected) < EPSILON;
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
