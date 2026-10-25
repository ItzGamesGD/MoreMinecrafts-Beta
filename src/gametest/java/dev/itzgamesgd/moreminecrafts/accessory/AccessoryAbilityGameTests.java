package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import dev.itzgamesgd.moreminecrafts.ability.AbilityAttachments;
import dev.itzgamesgd.moreminecrafts.ability.AbilityCooldowns;
import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import java.lang.reflect.Method;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Server-side behavior and actual player attachment serialization round trips. */
public final class AccessoryAbilityGameTests implements CustomTestMethodInvoker {
    private static final UUID RESTART_ID = UUID.fromString("50835941-38da-481b-86db-52a3e1ed7b39");

    @GameTest(maxTicks = 180)
    public void accessoryCooldownSurvivesProcessRestart(GameTestHelper ctx) {
        String phase = System.getProperty("moreminecrafts.accessory.restart.phase");
        if (phase == null || phase.isBlank()) { ctx.succeed(); return; }
        var server = ctx.getLevel().getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(RESTART_ID, "wave7b-cooldown"), false);
        ServerPlayer player = new ServerPlayer(server, ctx.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        if (!"write".equals(phase)) {
            var saved = server.getPlayerList().loadPlayerData(player.nameAndId());
            ctx.assertTrue(saved.isPresent(), "previous process wrote player data");
            player.load(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), saved.orElseThrow()));
            long expected = "reload1".equals(phase) ? 600 : 500;
            long actual = AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.EMBER_RING);
            ctx.assertTrue(actual >= expected - 1 && actual <= expected,
                    "offline time did not decrement cooldown across process restart, expected=" + expected
                            + " actual=" + actual);
            ctx.assertTrue(player.hasAttached(EquipmentAttachments.RING_OF_LIFE_SPENT),
                    "Ring of Life re-arm latch survives process restart while still below threshold");
        }
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        if ("write".equals(phase)) {
            AccessoryCooldownRuntime.start(player, AccessoryDefinition.EMBER_RING, 600);
            // Stay below the Ring of Life threshold until shutdown: no natural regeneration.
            player.getFoodData().setFoodLevel(17);
            player.getFoodData().setSaturation(0.0F);
            player.setHealth(1.0F);
            player.setAttached(EquipmentAttachments.RING_OF_LIFE_SPENT, true);
        }
        if ("reload1".equals(phase)) {
            player.setAttached(AbilityAttachments.COOLDOWNS, AbilityCooldowns.EMPTY);
            AccessoryCooldownRuntime.start(player, AccessoryDefinition.EMBER_RING, 500);
        }
        server.getPlayerList().saveAll();
        System.out.println("WAVE7B_COOLDOWN_RESTART_" + phase.toUpperCase());
        ctx.succeed();
    }
    @GameTest
    public void cooldownContinuesUnequippedAndSurvivesSaveLoad(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ctx.assertTrue(AccessoryCooldownRuntime.start(player, AccessoryDefinition.EMBER_RING, 600), "first activation starts");
        ctx.assertTrue(!AccessoryCooldownRuntime.start(player, AccessoryDefinition.EMBER_RING, 600), "same ability cannot restart");
        ctx.assertTrue(AccessoryCooldownRuntime.start(player, AccessoryDefinition.DEEP_SEA_PEARL, 900), "other ability independent");
        ServerPlayer other = ctx.makeMockServerPlayerInLevel();
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(other, AccessoryDefinition.EMBER_RING) == 0,
                "another player does not inherit this player's cooldown");
        AccessoryCooldownRuntime.tick(player);
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.EMBER_RING) == 599,
                "online tick decrements while unequipped");
        TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
        player.saveWithoutId(saved);
        ServerPlayer loaded = ctx.makeMockServerPlayerInLevel();
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, loaded.registryAccess(), saved.buildResult()));
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(loaded, AccessoryDefinition.EMBER_RING) == 599,
                "save/load preserves online ticks without offline decrement");
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(loaded, AccessoryDefinition.DEEP_SEA_PEARL) == 899,
                "other ability remains independent");
        ctx.succeed();
    }

    @GameTest
    public void ringOfLifeUsesEquippedLevelAndCooldown(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setHealth(4.0F);
        AccessoryAbilities.tick(player);
        ctx.assertTrue(!player.hasEffect(MobEffects.REGENERATION), "unequipped ring does not activate");
        ItemStack ring = new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE));
        new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.DEFENSE_SURVIVAL.ordinal(), ring);
        new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.COMBAT.ordinal(),
                new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.STAR_COVENANT)));
        AccessoryAbilities.tick(player);
        ctx.assertTrue(player.hasEffect(MobEffects.REGENERATION), "equipped ring activates below 25 percent health");
        ctx.assertTrue(player.getEffect(MobEffects.REGENERATION).getDuration() == 80, "+0 gives four seconds");
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.RING_OF_LIFE) == 2400,
                "+0 gives 120 second cooldown");
        new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.DEFENSE_SURVIVAL.ordinal(), ItemStack.EMPTY);
        ring.set(ModDataComponents.ENHANCEMENT_LEVEL, 10);
        new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.DEFENSE_SURVIVAL.ordinal(), ring);
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.RING_OF_LIFE) == 2400,
                "swapping identical ability does not reset cooldown");
        player.setAttached(AbilityAttachments.COOLDOWNS, AbilityCooldowns.EMPTY);
        player.removeEffect(MobEffects.REGENERATION);
        player.setHealth(20.0F);
        AccessoryAbilities.tick(player);
        player.setHealth(4.0F);
        AccessoryAbilities.tick(player);
        ctx.assertTrue(player.getEffect(MobEffects.REGENERATION).getDuration() == 140, "+10 gives seven seconds");
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.RING_OF_LIFE) == 2000,
                "+10 gives 100 second cooldown");
        ctx.assertTrue(new AccessoryEquipmentContainer(player).getItem(AccessoryCategory.COMBAT.ordinal()).getItem()
                == ModItems.ACCESSORIES.get(AccessoryDefinition.STAR_COVENANT),
                "independent combat slot remains equipped throughout ring activation");
        ctx.succeed();
    }

    @GameTest
    public void emberOnlyReducesLaterFireHitsAndRemovalEndsProtection(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        var fire = ctx.getLevel().damageSources().inFire();
        var generic = ctx.getLevel().damageSources().generic();
        AccessoryAbilities.afterDamage(player, fire);
        ctx.assertTrue(AccessoryAbilities.reduceFireDamage(player, fire, 10) == 10,
                "unequipped fire hit does not activate");
        ItemStack ring = new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.EMBER_RING));
        new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.DEFENSE_SURVIVAL.ordinal(), ring);
        AccessoryAbilities.afterDamage(player, fire);
        ctx.assertTrue(AccessoryAbilities.reduceFireDamage(player, fire, 10) == 6,
                "+0 reduces subsequent fire damage 40 percent");
        ctx.assertTrue(AccessoryAbilities.reduceFireDamage(player, generic, 10) == 10,
                "nonfire damage is untouched");
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.EMBER_RING) == 600,
                "ember starts 30 second cooldown");
        new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.DEFENSE_SURVIVAL.ordinal(), ItemStack.EMPTY);
        ctx.assertTrue(AccessoryAbilities.reduceFireDamage(player, fire, 10) == 10,
                "unequipping immediately stops protection");
        new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.DEFENSE_SURVIVAL.ordinal(), ring);
        ctx.assertTrue(AccessoryAbilities.reduceFireDamage(player, fire, 10) == 10,
                "re-equipping during cooldown does not restore the old protection window");
        ring.set(ModDataComponents.ENHANCEMENT_LEVEL, 10);
        new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.DEFENSE_SURVIVAL.ordinal(), ring);
        player.setAttached(AbilityAttachments.COOLDOWNS, AbilityCooldowns.EMPTY);
        AccessoryAbilities.afterDamage(player, fire);
        ctx.assertTrue(AccessoryAbilities.reduceFireDamage(player, fire, 10) == 3.5F,
                "+10 reduces subsequent fire damage 65 percent");
        ctx.succeed();
    }

    @GameTest
    public void actualFireDamageTriggersEmberAfterFirstHit(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().invulnerable = false;
        player.setPermanentlyInvulnerable(false);
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        player.setInvulnerableTime(0);
        BlockPos position = ctx.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
        new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.DEFENSE_SURVIVAL.ordinal(),
                new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.EMBER_RING)));
        float before = player.getHealth();
        ctx.assertTrue(player.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().inFire(), 2.0F),
                "first vanilla fire hit is accepted: invulnerable="
                        + player.isInvulnerableTo(ctx.getLevel(), ctx.getLevel().damageSources().inFire())
                        + " time=" + player.getInvulnerableTime() + " health=" + player.getHealth());
        float firstLoss = before - player.getHealth();
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.EMBER_RING) == 600,
                "accepted fire hit triggers cooldown");
        ctx.runAfterDelay(12, () -> {
            float afterFirst = player.getHealth();
            ctx.assertTrue(player.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().inFire(), 2.0F),
                    "second vanilla fire hit is accepted after the normal hurt interval");
            float secondLoss = afterFirst - player.getHealth();
            ctx.assertTrue(firstLoss > 0 && Math.abs(secondLoss / firstLoss - 0.6F) < 0.01F,
                    "only subsequent fire damage receives 40 percent reduction: first=" + firstLoss
                            + " second=" + secondLoss + " direct="
                            + AccessoryAbilities.reduceFireDamage(player, ctx.getLevel().damageSources().inFire(), 2.0F));
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void deepSeaPearlTriggersAfterFortyStillUnderwaterTicks(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        BlockPos water = ctx.absolutePos(new BlockPos(2, 2, 2));
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos column = water.offset(dx, 0, dz);
            ctx.getLevel().setBlock(column.below(), Blocks.STONE.defaultBlockState(), 3);
            for (int y = 0; y < 4; y++)
                ctx.getLevel().setBlock(column.above(y), Blocks.WATER.defaultBlockState(), 3);
            ctx.getLevel().setBlock(column.above(4), Blocks.STONE.defaultBlockState(), 3);
        }
        player.setPos(water.getX() + 0.5D, water.getY(), water.getZ() + 0.5D);
        player.setShiftKeyDown(true);
        ItemStack pearl = new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.DEEP_SEA_PEARL));
        new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.EXPLORATION_MOBILITY.ordinal(), pearl);
        ctx.runAfterDelay(45, () -> {
            ctx.assertTrue(player.hasEffect(MobEffects.WATER_BREATHING),
                    "forty still underwater ticks activate pearl: underwater=" + player.isUnderWater()
                            + " eyeFluidWater=" + player.level().getFluidState(BlockPos.containing(player.getEyePosition())).is(FluidTags.WATER)
                            + " crouching=" + player.isShiftKeyDown() + " alive=" + player.isAlive()
                            + " position=" + player.position() + " cooldown="
                            + AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.DEEP_SEA_PEARL));
            ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.DEEP_SEA_PEARL) > 0,
                    "pearl starts its cooldown");
            pearl.set(ModDataComponents.ENHANCEMENT_LEVEL, 10);
            new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.EXPLORATION_MOBILITY.ordinal(), pearl);
            player.removeEffect(MobEffects.WATER_BREATHING);
            player.setAttached(AbilityAttachments.COOLDOWNS, AbilityCooldowns.EMPTY);
            ctx.runAfterDelay(42, () -> {
                ctx.assertTrue(player.getEffect(MobEffects.WATER_BREATHING).getDuration() >= 318,
                        "+10 pearl gives sixteen seconds water breathing");
                ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.DEEP_SEA_PEARL) >= 598,
                        "+10 pearl gives 30 second cooldown");
                ctx.succeed();
            });
        });
    }

    @GameTest
    public void cooldownSurvivesDeathRespawnAndDimensionTravel(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        AccessoryCooldownRuntime.start(player, AccessoryDefinition.EMBER_RING, 600);
        var nether = ctx.getLevel().getServer().getLevel(Level.NETHER);
        ctx.assertTrue(nether != null, "Nether exists");
        ctx.assertTrue(player.teleportTo(nether, 0.5D, 80.0D, 0.5D, java.util.Set.<Relative>of(),
                player.getYRot(), player.getXRot(), true), "dimension transfer succeeds");
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.EMBER_RING) == 600,
                "dimension transfer preserves remaining ticks");
        player.die(nether.damageSources().generic());
        ServerPlayer respawned = ctx.getLevel().getServer().getPlayerList().respawn(
                player, false, Entity.RemovalReason.KILLED);
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(respawned, AccessoryDefinition.EMBER_RING) == 600,
                "death and respawn copy cooldown attachment");
        ctx.succeed();
    }

    @GameTest(maxTicks = 40)
    public void ringOfLifeRequiresRecoveryAboveThresholdBeforeReactivation(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ServerPlayer other = ctx.makeMockServerPlayerInLevel();
        int slot = AccessoryCategory.DEFENSE_SURVIVAL.ordinal();
        new AccessoryEquipmentContainer(player).setItem(slot,
                new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE)));
        new AccessoryEquipmentContainer(other).setItem(slot,
                new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE)));
        player.setHealth(4.0F);
        AccessoryAbilities.tick(player);
        ctx.assertTrue(player.hasEffect(MobEffects.REGENERATION), "entering low health activates");
        ctx.assertTrue(player.hasAttached(EquipmentAttachments.RING_OF_LIFE_SPENT), "activation disarms the ring");
        player.removeEffect(MobEffects.REGENERATION);

        for (int i = 0; i < 2400; i++) AccessoryAbilities.tick(player);
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.RING_OF_LIFE) == 0,
                "cooldown expired through online ticks");
        ctx.assertTrue(!player.hasEffect(MobEffects.REGENERATION),
                "remaining continuously below threshold does not reactivate after cooldown expiry");

        player.setHealth(5.0F);
        AccessoryAbilities.tick(player);
        ctx.assertTrue(player.hasAttached(EquipmentAttachments.RING_OF_LIFE_SPENT) && !player.hasEffect(MobEffects.REGENERATION),
                "exactly 25 percent is still the low-health condition and does not re-arm");

        new AccessoryEquipmentContainer(player).setItem(slot, ItemStack.EMPTY);
        AccessoryAbilities.tick(player);
        new AccessoryEquipmentContainer(player).setItem(slot,
                new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE)));
        player.setHealth(4.0F);
        AccessoryAbilities.tick(player);
        ctx.assertTrue(!player.hasEffect(MobEffects.REGENERATION), "re-equipping while below threshold does not re-arm");

        TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
        player.saveWithoutId(saved);
        ServerPlayer loaded = ctx.makeMockServerPlayerInLevel();
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, loaded.registryAccess(), saved.buildResult()));
        ctx.assertTrue(loaded.hasAttached(EquipmentAttachments.RING_OF_LIFE_SPENT), "latch survives save/load");
        loaded.setHealth(4.0F);
        AccessoryAbilities.tick(loaded);
        ctx.assertTrue(!loaded.hasEffect(MobEffects.REGENERATION), "reload while below threshold does not re-arm");

        other.setHealth(4.0F);
        AccessoryAbilities.tick(other);
        ctx.assertTrue(other.hasEffect(MobEffects.REGENERATION) && !player.hasEffect(MobEffects.REGENERATION),
                "each player's latch is independent");

        player.setHealth(6.0F);
        AccessoryAbilities.tick(player);
        ctx.assertTrue(!player.hasAttached(EquipmentAttachments.RING_OF_LIFE_SPENT) && !player.hasEffect(MobEffects.REGENERATION),
                "recovering above threshold re-arms without activating");
        player.setHealth(4.0F);
        AccessoryAbilities.tick(player);
        ctx.assertTrue(player.hasEffect(MobEffects.REGENERATION)
                        && AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.RING_OF_LIFE) == 2400,
                "a later drop below threshold activates again once cooldown has expired");

        player.die(ctx.getLevel().damageSources().generic());
        ServerPlayer respawned = ctx.getLevel().getServer().getPlayerList().respawn(
                player, false, Entity.RemovalReason.KILLED);
        AccessoryAbilities.tick(respawned);
        ctx.assertTrue(!respawned.hasAttached(EquipmentAttachments.RING_OF_LIFE_SPENT),
                "respawning at full health re-arms the ring");
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(respawned, AccessoryDefinition.RING_OF_LIFE) >= 2398,
                "death does not reset the cooldown");
        ctx.succeed();
    }

    private static ServerPlayer crouchingInPool(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        BlockPos water = ctx.absolutePos(new BlockPos(2, 2, 2));
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos column = water.offset(dx, 0, dz);
            ctx.getLevel().setBlock(column.below(), Blocks.STONE.defaultBlockState(), 3);
            for (int y = 0; y < 4; y++)
                ctx.getLevel().setBlock(column.above(y), Blocks.WATER.defaultBlockState(), 3);
            ctx.getLevel().setBlock(column.above(4), Blocks.STONE.defaultBlockState(), 3);
        }
        player.setPos(water.getX() + 0.5D, water.getY(), water.getZ() + 0.5D);
        player.setShiftKeyDown(true);
        new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.EXPLORATION_MOBILITY.ordinal(),
                new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.DEEP_SEA_PEARL)));
        return player;
    }

    @GameTest(maxTicks = 100)
    public void deepSeaPearlIgnoresExternalDriftWithoutHorizontalInput(GameTestHelper ctx) {
        ServerPlayer player = crouchingInPool(ctx);
        double x = player.getX();
        double z = player.getZ();
        ctx.onEachTick(() -> {
            // External displacement every tick, as from a current; no movement keys are held.
            double offset = (ctx.getTick() % 2 == 0) ? 0.2D : -0.2D;
            player.setPos(x + offset, player.getY(), z + offset);
        });
        ctx.runAfterDelay(50, () -> {
            ctx.assertTrue(player.getLastClientInput().equals(Input.EMPTY), "no horizontal input was provided");
            ctx.assertTrue(player.hasEffect(MobEffects.WATER_BREATHING),
                    "drifting without horizontal input still counts as motionless");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 140)
    public void deepSeaPearlHorizontalInputBlocksAndResetsStillness(GameTestHelper ctx) {
        ServerPlayer player = crouchingInPool(ctx);
        Input forward = new Input(true, false, false, false, false, true, false);
        Input strafe = new Input(false, false, true, false, false, true, false);
        player.setLastClientInput(forward);
        ctx.runAfterDelay(45, () -> {
            ctx.assertTrue(!player.hasEffect(MobEffects.WATER_BREATHING),
                    "holding a horizontal movement key prevents activation even without displacement");
            player.setLastClientInput(Input.EMPTY);
            ctx.runAfterDelay(30, () -> {
                player.setLastClientInput(strafe);
                ctx.runAfterDelay(1, () -> {
                    player.setLastClientInput(Input.EMPTY);
                    ctx.runAfterDelay(30, () -> {
                        ctx.assertTrue(!player.hasEffect(MobEffects.WATER_BREATHING),
                                "a single tick of horizontal input resets the two-second count");
                        ctx.runAfterDelay(15, () -> {
                            ctx.assertTrue(player.hasEffect(MobEffects.WATER_BREATHING),
                                    "forty consecutive ticks without horizontal input activate");
                            ctx.succeed();
                        });
                    });
                });
            });
        });
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
