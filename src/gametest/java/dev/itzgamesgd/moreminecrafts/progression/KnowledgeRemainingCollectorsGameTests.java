package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Action-level tests for the remaining practical Knowledge hooks. */
public final class KnowledgeRemainingCollectorsGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 280)
    public void k12RequiresCraftingAndTakingARealBlastFurnaceProduct(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        CraftingMenu crafting = new CraftingMenu(1, player.getInventory(),
                net.minecraft.world.inventory.ContainerLevelAccess.create(ctx.getLevel(),
                        ctx.absolutePos(new BlockPos(1, 1, 1))));
        crafting.getSlot(1).set(new ItemStack(Items.IRON_INGOT));
        crafting.getSlot(2).set(new ItemStack(Items.IRON_INGOT));
        crafting.getSlot(3).set(new ItemStack(Items.IRON_INGOT));
        crafting.getSlot(4).set(new ItemStack(Items.IRON_INGOT));
        crafting.getSlot(5).set(new ItemStack(Items.FURNACE));
        crafting.getSlot(6).set(new ItemStack(Items.IRON_INGOT));
        crafting.getSlot(7).set(new ItemStack(Items.SMOOTH_STONE));
        crafting.getSlot(8).set(new ItemStack(Items.SMOOTH_STONE));
        crafting.getSlot(9).set(new ItemStack(Items.SMOOTH_STONE));
        crafting.slotsChanged(crafting.getSlot(1).container);
        player.containerMenu = crafting;
        var craftingResult = crafting.getSlot(0);
        ItemStack crafted = craftingResult.getItem().copy();
        ctx.assertTrue(crafted.is(Items.BLAST_FURNACE), "the vanilla recipe creates a blast furnace");
        ItemStack takenCraft = craftingResult.remove(1);
        craftingResult.onTake(player, takenCraft);
        ctx.assertTrue(player.getStats().getValue(Stats.ITEM_CRAFTED.get(Items.BLAST_FURNACE)) == 1,
                "taking the actual result-slot output records the vanilla craft statistic");

        BlockPos pos = ctx.absolutePos(new BlockPos(4, 2, 2));
        ctx.setBlock(new BlockPos(4, 2, 2), Blocks.BLAST_FURNACE);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) ctx.getLevel().getBlockEntity(pos);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));
        ctx.runAfterDelay(230, () -> {
            AbstractContainerMenu menu = furnace.createMenu(2, player.getInventory(), player);
            var output = menu.getSlot(2);
            ctx.assertTrue(output.getItem().is(Items.IRON_INGOT),
                    "vanilla blast-furnace processing creates an iron ingot");
            ItemStack taken = output.remove(1);
            output.onTake(player, taken);
            ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("furnace_blast_furnace_used") == 1,
                    "the installed furnace result-slot hook credits successful blast-furnace use");
            ctx.succeed();
        });
    }

    @GameTest
    public void k12ManualBlastFurnaceOutputWithoutCraftingDoesNotQualify(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.BLAST_FURNACE);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) ctx.getLevel()
                .getBlockEntity(ctx.absolutePos(new BlockPos(2, 2, 2)));
        furnace.setItem(2, new ItemStack(Items.IRON_INGOT));
        AbstractContainerMenu menu = furnace.createMenu(1, player.getInventory(), player);
        player.containerMenu = menu;
        var output = menu.getSlot(2);
        ItemStack taken = output.remove(1);
        output.onTake(player, taken);
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("furnace_blast_furnace_used") == 0,
                "a manually inserted output without the vanilla blast-furnace craft statistic is excluded");
        ctx.succeed();
    }

    @GameTest(maxTicks = 20)
    public void k36CountsOnlyAnActualShieldBlock(GameTestHelper ctx) {
        ServerPlayer shielded = ctx.makeMockServerPlayerInLevel();
        shielded.setGameMode(GameType.SURVIVAL);
        shielded.setYRot(0.0F);
        shielded.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        Items.SHIELD.use(ctx.getLevel(), shielded, InteractionHand.OFF_HAND);
        ctx.assertTrue(shielded.getUseItem().is(Items.SHIELD), "vanilla shield use starts a shield block");
        int shieldDamageBefore = shielded.getUseItem().getDamageValue();
        var attacker = ctx.spawnMob(EntityTypes.ZOMBIE, 2, 2, 4).spawn();
        attacker.setNoAi(true);
        attacker.setPos(shielded.getX(), shielded.getY(), shielded.getZ() + 2.0D);

        ctx.runAfterDelay(6, () -> {
            ctx.assertTrue(shielded.isBlocking(), "the shield has completed vanilla's raise delay");
            ctx.assertTrue(!shielded.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().mobAttack(attacker),
                    6.0F), "the raised shield blocks the incoming front-facing melee hit");
            ctx.assertTrue(shielded.getUseItem().getDamageValue() > shieldDamageBefore,
                    "vanilla damages the shield as a result of blocking the hit");
            ctx.assertTrue(KnowledgeRuntime.state(shielded).conditionProgress("shield_blocks") == 1,
                    "the installed blockedByItem hook records the successfully blocked hit");

            ServerPlayer unshielded = ctx.makeMockServerPlayerInLevel();
            ctx.assertTrue(unshielded.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().mobAttack(attacker),
                    6.0F), "the control player takes the same hit without a shield");
            ctx.assertTrue(KnowledgeRuntime.state(unshielded).conditionProgress("shield_blocks") == 0,
                    "an unblocked hit does not advance shield knowledge");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 360)
    public void k30CookingResultSlotAddsTheActualCookedFoodId(GameTestHelper ctx) {
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        furnace.setItem(0, new ItemStack(Items.BEEF));
        furnace.setItem(1, new ItemStack(Items.COAL));
        ctx.runAfterDelay(230, () -> {
            AbstractContainerMenu menu = furnace.createMenu(1, player.getInventory(), player);
            player.containerMenu = menu;
            var result = menu.getSlot(2);
            ctx.assertTrue(result.getItem().is(Items.COOKED_BEEF), "vanilla cooking produces steak");
            ItemStack taken = result.remove(1);
            result.onTake(player, taken);
            ctx.runAfterDelay(110, () -> {
                ctx.assertTrue(KnowledgeRuntime.state(player)
                                .get("condition.foods_crafted_or_cooked.seen.minecraft.cooked_beef") == 1,
                        "the actual result-slot take contributes the cooked food item ID");
                ctx.succeed();
            });
        });
    }

    @GameTest(maxTicks = 360)
    public void k30NonFoodFurnaceOutputDoesNotAddAFoodType(GameTestHelper ctx) {
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));
        ctx.runAfterDelay(230, () -> {
            AbstractContainerMenu menu = furnace.createMenu(1, player.getInventory(), player);
            player.containerMenu = menu;
            var result = menu.getSlot(2);
            ctx.assertTrue(result.getItem().is(Items.IRON_INGOT), "vanilla smelting produces an iron ingot");
            ItemStack taken = result.remove(1);
            result.onTake(player, taken);
            ctx.runAfterDelay(110, () -> {
                ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("foods_crafted_or_cooked") == 0,
                        "a non-food crafted statistic does not add a food ID");
                ctx.succeed();
            });
        });
    }

    @GameTest(maxTicks = 230)
    public void k48CreditsCompletedPiglinBarterToTheDirectGoldOfferer(GameTestHelper ctx) {
        ServerPlayer offerer = ctx.makeMockServerPlayerInLevel();
        Piglin piglin = ctx.spawnMob(EntityTypes.PIGLIN, 3, 2, 2).spawn();
        offerer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLD_INGOT));
        InteractionResult offered = offerer.interactOn(piglin, InteractionHand.MAIN_HAND, piglin.position());
        ctx.assertTrue(offered.consumesAction(), "the piglin accepts the player's direct gold-ingot offer");

        ctx.runAfterDelay(200, () -> {
            ctx.assertTrue(KnowledgeRuntime.state(offerer).conditionProgress("piglin_barters") == 1,
                    "the installed barter completion hook credits the verified offerer");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 220)
    public void k48PiglinWithoutAnOfferDoesNotCreateBarterProgress(GameTestHelper ctx) {
        ServerPlayer bystander = ctx.makeMockServerPlayerInLevel();
        ctx.spawnMob(EntityTypes.PIGLIN, 3, 2, 2).spawn();
        ctx.runAfterDelay(200, () -> {
            ctx.assertTrue(KnowledgeRuntime.state(bystander).conditionProgress("piglin_barters") == 0,
                    "piglin activity without a gold offer does not create player progress");
            ctx.succeed();
        });
    }

    @GameTest
    public void firstKnowledgeUnlockChangesPersistentStateOnlyOnce(GameTestHelper ctx) {
        ServerPlayer first = ctx.makeMockServerPlayerInLevel();
        ServerPlayer second = ctx.makeMockServerPlayerInLevel();
        KnowledgeRuntime.grantKnowledge(first, 8);
        ctx.assertTrue(KnowledgeRuntime.knows(first, 8) && !KnowledgeRuntime.knows(second, 8),
                "the real unlock transition updates only the acquiring player's persistent Knowledge state");
        KnowledgeRuntime.grantKnowledge(first, 8);
        ctx.assertTrue(KnowledgeRuntime.state(first).get("knowledge.8") == 1
                        && KnowledgeRuntime.state(second).get("knowledge.8") == 0,
                "repeating an unlock does not duplicate or route its persistent state to another player");
        ctx.succeed();
    }

    @GameTest
    public void projectileKillUsesStoredWeaponAndOwnerInsteadOfTheHeldWeapon(GameTestHelper ctx) {
        ServerPlayer bowOwner = ctx.makeMockServerPlayerInLevel();
        bowOwner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CROSSBOW));
        Arrow bowArrow = arrow(ctx, bowOwner, Items.BOW);
        invokeShootProjectile((BowItem) Items.BOW, bowOwner, bowArrow);
        var bowTarget = ctx.spawnMob(EntityTypes.ZOMBIE, 3, 2, 2).spawn();
        ctx.assertTrue(bowTarget.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().arrow(bowArrow, bowOwner), 100.0F),
                "a hostile mob dies from a direct bow-origin arrow damage source");
        ctx.assertTrue(KnowledgeRuntime.state(bowOwner).conditionProgress("bow_hostile_kills") == 1,
                "the arrow's stored bow and owner receive bow kill credit");
        ctx.assertTrue(KnowledgeRuntime.state(bowOwner).conditionProgress("crossbow_hostile_kills") == 0,
                "holding a crossbow does not change the stored bow projectile origin");

        ServerPlayer crossbowOwner = ctx.makeMockServerPlayerInLevel();
        crossbowOwner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
        Arrow crossbowArrow = arrow(ctx, crossbowOwner, Items.CROSSBOW);
        invokeShootProjectile((CrossbowItem) Items.CROSSBOW, crossbowOwner, crossbowArrow);
        var crossbowTarget = ctx.spawnMob(EntityTypes.ZOMBIE, 4, 2, 2).spawn();
        ctx.assertTrue(crossbowTarget.hurtServer(ctx.getLevel(),
                ctx.getLevel().damageSources().arrow(crossbowArrow, crossbowOwner), 100.0F),
                "a hostile mob dies from a direct crossbow-origin arrow damage source");
        ctx.assertTrue(KnowledgeRuntime.state(crossbowOwner).conditionProgress("crossbow_hostile_kills") == 1,
                "the arrow's stored crossbow and owner receive crossbow kill credit");
        ctx.assertTrue(KnowledgeRuntime.state(crossbowOwner).conditionProgress("bow_hostile_kills") == 0,
                "holding a bow does not change the stored crossbow projectile origin");

        ServerPlayer launchActor = ctx.makeMockServerPlayerInLevel();
        ServerPlayer projectileOwner = ctx.makeMockServerPlayerInLevel();
        Arrow transferredArrow = arrow(ctx, projectileOwner, Items.BOW);
        invokeShootProjectile((BowItem) Items.BOW, launchActor, transferredArrow);
        var ownerTarget = ctx.spawnMob(EntityTypes.ZOMBIE, 5, 2, 2).spawn();
        ctx.assertTrue(ownerTarget.hurtServer(ctx.getLevel(),
                ctx.getLevel().damageSources().arrow(transferredArrow, launchActor), 100.0F),
                "a hostile mob dies from a projectile whose stored owner differs from the launch argument");
        ctx.assertTrue(KnowledgeRuntime.state(projectileOwner).conditionProgress("bow_hostile_kills") == 1,
                "kill attribution follows the projectile's actual owner");
        ctx.assertTrue(KnowledgeRuntime.state(launchActor).conditionProgress("bow_hostile_kills") == 0,
                "the launch-argument player receives no credit when they are not the projectile owner");
        ctx.succeed();
    }

    @GameTest
    public void specialAmmunitionRequiresTheActualBowOrCrossbowLaunchHook(GameTestHelper ctx) {
        ServerPlayer bowPlayer = ctx.makeMockServerPlayerInLevel();
        Arrow tipped = arrow(ctx, bowPlayer, Items.BOW, Items.TIPPED_ARROW);
        invokeShootProjectile((BowItem) Items.BOW, bowPlayer, tipped);
        ctx.assertTrue(KnowledgeRuntime.state(bowPlayer).conditionProgress("special_ammunition_used") == 1,
                "a tipped arrow fired through BowItem.shootProjectile reaches the installed launch observer");

        ServerPlayer ordinaryPlayer = ctx.makeMockServerPlayerInLevel();
        Arrow ordinary = arrow(ctx, ordinaryPlayer, Items.BOW, Items.ARROW);
        invokeShootProjectile((BowItem) Items.BOW, ordinaryPlayer, ordinary);
        ctx.assertTrue(KnowledgeRuntime.state(ordinaryPlayer).conditionProgress("special_ammunition_used") == 0,
                "an ordinary arrow fired through the same hook does not count as special ammunition");
        ctx.succeed();
    }

    @GameTest
    public void crossbowFireworkLaunchAndExplosionDamageUseTheActualCrossbowFlag(GameTestHelper ctx) {
        ServerPlayer crossbowPlayer = ctx.makeMockServerPlayerInLevel();
        FireworkRocketEntity rocket = new FireworkRocketEntity(ctx.getLevel(), crossbowPlayer.getX(),
                crossbowPlayer.getY(), crossbowPlayer.getZ(), new ItemStack(Items.FIREWORK_ROCKET));
        rocket.setOwner(crossbowPlayer);
        invokeShootProjectile((CrossbowItem) Items.CROSSBOW, crossbowPlayer, rocket);
        ctx.assertTrue(KnowledgeRuntime.state(crossbowPlayer).conditionProgress("special_ammunition_used") == 1,
                "the actual crossbow shootProjectile hook recognizes a launched firework");

        var target = ctx.spawnMob(EntityTypes.ZOMBIE, 3, 2, 2).spawn();
        ctx.assertTrue(target.hurtServer(ctx.getLevel(),
                ctx.getLevel().damageSources().fireworks(rocket, crossbowPlayer), 100.0F),
                "a hostile mob dies from a fireworks explosion damage source");
        ctx.assertTrue(KnowledgeRuntime.state(crossbowPlayer).conditionProgress("crossbow_hostile_kills") == 1,
                "the crossbow launch attribution follows the firework into explosion damage");

        ServerPlayer bowPlayer = ctx.makeMockServerPlayerInLevel();
        FireworkRocketEntity wrongOrigin = new FireworkRocketEntity(ctx.getLevel(), bowPlayer.getX(),
                bowPlayer.getY(), bowPlayer.getZ(), new ItemStack(Items.FIREWORK_ROCKET));
        wrongOrigin.setOwner(bowPlayer);
        invokeShootProjectile((BowItem) Items.BOW, bowPlayer, wrongOrigin);
        var wrongTarget = ctx.spawnMob(EntityTypes.ZOMBIE, 4, 2, 2).spawn();
        wrongTarget.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().fireworks(wrongOrigin, bowPlayer), 100.0F);
        ctx.assertTrue(KnowledgeRuntime.state(bowPlayer).conditionProgress("crossbow_hostile_kills") == 0,
                "a firework passed through a bow-origin hook cannot receive crossbow kill credit");
        ctx.assertTrue(KnowledgeRuntime.state(bowPlayer).conditionProgress("special_ammunition_used") == 0,
                "a bow-fired firework is not crossbow special ammunition");
        ctx.succeed();
    }

    @GameTest
    public void crossbowFireworkAttributionDoesNotSurviveProjectileSerialization(GameTestHelper ctx) {
        ServerPlayer owner = ctx.makeMockServerPlayerInLevel();
        FireworkRocketEntity launched = new FireworkRocketEntity(ctx.getLevel(), owner.getX(), owner.getY(),
                owner.getZ(), new ItemStack(Items.FIREWORK_ROCKET));
        launched.setOwner(owner);
        invokeShootProjectile((CrossbowItem) Items.CROSSBOW, owner, launched);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,
                ctx.getLevel().registryAccess());
        launched.saveWithoutId(output);
        CompoundTag saved = output.buildResult();
        FireworkRocketEntity reloaded = EntityTypes.FIREWORK_ROCKET.create(ctx.getLevel(), EntitySpawnReason.COMMAND);
        ctx.assertTrue(reloaded != null, "a vanilla firework projectile can be recreated from saved state");
        reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess(), saved));
        var target = ctx.spawnMob(EntityTypes.ZOMBIE, 3, 2, 2).spawn();
        target.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().fireworks(reloaded, owner), 100.0F);
        ctx.assertTrue(KnowledgeRuntime.state(owner).conditionProgress("crossbow_hostile_kills") == 0,
                "ephemeral launch provenance cannot be forged or duplicated after projectile reload");
        ctx.succeed();
    }

    @GameTest
    public void tridentKillRequiresAStoredTridentAndServerPlayerOwner(GameTestHelper ctx) {
        ServerPlayer owner = ctx.makeMockServerPlayerInLevel();
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
        ThrownTrident trident = new ThrownTrident(ctx.getLevel(), owner, new ItemStack(Items.TRIDENT));
        ctx.getLevel().addFreshEntity(trident);
        var target = ctx.spawnMob(EntityTypes.ZOMBIE, 3, 2, 2).spawn();
        ctx.assertTrue(target.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().trident(trident, owner), 100.0F),
                "the hostile mob dies from the direct thrown-trident damage source");
        ctx.assertTrue(KnowledgeRuntime.state(owner).conditionProgress("trident_hostile_kills") == 1,
                "the direct projectile type, stored trident, and owner satisfy the trident condition");

        ThrownTrident unowned = new ThrownTrident(ctx.getLevel(), owner, new ItemStack(Items.TRIDENT));
        unowned.setOwner(null);
        ctx.getLevel().addFreshEntity(unowned);
        var secondTarget = ctx.spawnMob(EntityTypes.ZOMBIE, 4, 2, 2).spawn();
        ctx.assertTrue(secondTarget.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().trident(unowned, null),
                100.0F), "a hostile mob dies from a trident without a player owner");
        ctx.assertTrue(KnowledgeRuntime.state(owner).conditionProgress("trident_hostile_kills") == 1,
                "an unowned trident kill is not transferred to the other player");
        ctx.succeed();
    }

    @GameTest
    public void k59SuccessfulArchaeologyLootSpawnReachesTheInstalledSpawnHook(GameTestHelper ctx) {
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.SUSPICIOUS_GRAVEL);
        BrushableBlockEntity brushable = (BrushableBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ServerPlayer brusher = ctx.makeMockServerPlayerInLevel();
        setArchaeologyLoot(brushable, "minecraft:archaeology/desert_well");
        int itemsBefore = nearbyItems(ctx, pos).size();
        invokeDropContent(brushable, ctx, brusher);
        List<ItemEntity> spawned = nearbyItems(ctx, pos);
        ctx.assertTrue(spawned.size() > itemsBefore, "the real archaeology dropContent method spawns a loot entity");
        ctx.assertTrue(KnowledgeRuntime.state(brusher).conditionProgress("archaeology_items_brushed") > 0,
                "successful addFreshEntity reaches the installed archaeology mixin and credits the brusher");
        ctx.succeed();
    }

    @GameTest
    public void k59ArchaeologyWithoutLootDoesNotProduceProgress(GameTestHelper ctx) {
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.SUSPICIOUS_GRAVEL);
        BrushableBlockEntity brushable = (BrushableBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ServerPlayer brusher = ctx.makeMockServerPlayerInLevel();
        invokeDropContent(brushable, ctx, brusher);
        ctx.assertTrue(nearbyItems(ctx, pos).isEmpty(), "without a configured archaeology loot table no item is spawned");
        ctx.assertTrue(KnowledgeRuntime.state(brusher).conditionProgress("archaeology_items_brushed") == 0,
                "failed or empty archaeology item production does not count");
        ctx.succeed();
    }

    private static Arrow arrow(GameTestHelper ctx, ServerPlayer owner, net.minecraft.world.item.Item weapon) {
        return arrow(ctx, owner, weapon, Items.ARROW);
    }

    private static Arrow arrow(GameTestHelper ctx, ServerPlayer owner, net.minecraft.world.item.Item weapon,
            net.minecraft.world.item.Item ammunition) {
        return new Arrow(ctx.getLevel(), owner, new ItemStack(ammunition), new ItemStack(weapon));
    }

    private static void setProjectileItem(Projectile projectile, String methodName, ItemStack item) {
        try {
            Method setter = findMethod(projectile.getClass(), methodName, ItemStack.class);
            setter.setAccessible(true);
            setter.invoke(projectile, item);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("could not configure vanilla projectile item state", exception);
        }
    }

    private static void invokeShootProjectile(Object weapon, ServerPlayer shooter, Projectile projectile) {
        try {
            Method shoot = findMethod(weapon.getClass(), "shootProjectile", LivingEntity.class,
                    Projectile.class, int.class, float.class, float.class, float.class, LivingEntity.class);
            shoot.setAccessible(true);
            shoot.invoke(weapon, shooter, projectile, 0, 3.0F, 0.0F, shooter.getYRot(), null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("could not execute the vanilla bow/crossbow projectile launch", exception);
        }
    }

    private static void setArchaeologyLoot(BrushableBlockEntity brushable, String lootTable) {
        try {
            Method setter = findMethod(brushable.getClass(), "setLootTable", ResourceKey.class, long.class);
            setter.setAccessible(true);
            setter.invoke(brushable, ResourceKey.create(Registries.LOOT_TABLE,
                    Identifier.parse(lootTable)), 1L);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("could not configure the vanilla archaeology loot table", exception);
        }
    }

    private static void invokeDropContent(BrushableBlockEntity brushable, GameTestHelper ctx, ServerPlayer brusher) {
        try {
            Method drop = java.util.Arrays.stream(brushable.getClass().getDeclaredMethods())
                    .filter(method -> method.getName().equals("dropContent")
                            && java.util.Arrays.stream(method.getParameterTypes())
                                    .anyMatch(type -> net.minecraft.server.level.ServerLevel.class.isAssignableFrom(type))
                            && java.util.Arrays.stream(method.getParameterTypes())
                                    .anyMatch(type -> net.minecraft.world.entity.LivingEntity.class.isAssignableFrom(type)))
                    .findFirst().orElseThrow();
            drop.setAccessible(true);
            Object[] arguments = java.util.Arrays.stream(drop.getParameterTypes()).map(type -> {
                if (net.minecraft.server.level.ServerLevel.class.isAssignableFrom(type)) return ctx.getLevel();
                if (net.minecraft.world.entity.LivingEntity.class.isAssignableFrom(type)) return brusher;
                if (BlockPos.class.isAssignableFrom(type)) return ctx.absolutePos(new BlockPos(2, 2, 2));
                if (net.minecraft.world.level.block.state.BlockState.class.isAssignableFrom(type))
                    return Blocks.SUSPICIOUS_GRAVEL.defaultBlockState();
                if (ItemStack.class.isAssignableFrom(type)) return new ItemStack(Items.BRUSH);
                if (type == long.class) return 0L;
                if (type == int.class) return 0;
                return null;
            }).toArray();
            drop.invoke(brushable, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("could not execute the vanilla archaeology loot-spawn hook", exception);
        }
    }

    private static List<ItemEntity> nearbyItems(GameTestHelper ctx, BlockPos pos) {
        return ctx.getLevel().getEntitiesOfClass(ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(4.0D));
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... arguments)
            throws NoSuchMethodException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredMethod(name, arguments);
            } catch (NoSuchMethodException ignored) {
                // Continue until the vanilla superclass that owns the method.
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
