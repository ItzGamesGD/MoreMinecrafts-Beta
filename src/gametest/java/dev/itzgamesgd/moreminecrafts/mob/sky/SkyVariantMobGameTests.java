package dev.itzgamesgd.moreminecrafts.mob.sky;

import dev.itzgamesgd.moreminecrafts.mob.CustomMobCatalog;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Group B sky-island vanilla variants: registry, PROVISIONAL stats, conversion policy, slime traits, spawn gates, loot. */
public final class SkyVariantMobGameTests {
    private static final List<EntityType<? extends Mob>> ALL = SkyVariantMobs.allTypes();

    private static <T extends Mob> T create(GameTestHelper c, EntityType<T> type) {
        T mob = type.create(c.getLevel(), EntitySpawnReason.COMMAND);
        if (mob == null) throw new AssertionError("cannot create " + type);
        return mob;
    }

    private static <T extends Mob> T finalized(GameTestHelper c, EntityType<T> type) {
        T mob = create(c, type);
        mob.finalizeSpawn(c.getLevel(), c.getLevel().getCurrentDifficultyAt(mob.blockPosition()),
                EntitySpawnReason.COMMAND, null);
        return mob;
    }

    private static String id(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }

    private static boolean is(EntityType<?> type, net.minecraft.tags.TagKey<EntityType<?>> tag) {
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(tag);
    }

    @GameTest
    public void registryIsInitializedAndSeparateFromGroupCAndTheCatalog(GameTestHelper c) {
        c.assertTrue(ALL.size() == 5, "five group B variants");
        Set<EntityType<?>> others = new HashSet<>(SkyNormalMobs.allTypes());
        others.addAll(CustomMobCatalog.allTypes());
        c.assertTrue(CustomMobCatalog.allTypes().size() == 40, "the forty-entry catalog is unchanged");
        List<String> expected = List.of("moreminecrafts:weathered_zombie", "moreminecrafts:sky_archer",
                "moreminecrafts:rock_creeper", "moreminecrafts:pale_enderman", "moreminecrafts:azure_slime");
        for (int i = 0; i < ALL.size(); i++) {
            EntityType<? extends Mob> type = ALL.get(i);
            c.assertTrue(id(type).equals(expected.get(i)), "registered as " + expected.get(i) + ", was " + id(type));
            c.assertTrue(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(expected.get(i))) == type, "lookup");
            c.assertTrue(!others.contains(type), "no group C / catalog identity: " + id(type));
            c.assertTrue(type.getCategory() == MobCategory.MONSTER && !type.isAllowedInPeaceful(), "hostile: " + id(type));
            c.assertTrue(is(type, SkyVariantMobs.TAG) && !is(type, SkyNormalMobs.TAG), "marker tags: " + id(type));
            c.assertTrue(SpawnPlacements.getPlacementType(type) == SpawnPlacementTypes.ON_GROUND, "on ground: " + id(type));
            c.assertTrue(create(c, type) != null, "factory works: " + id(type));
        }
        c.assertTrue(create(c, SkyVariantMobs.WEATHERED_ZOMBIE) instanceof Zombie, "Zombie subclass");
        c.assertTrue(create(c, SkyVariantMobs.SKY_ARCHER) instanceof Skeleton, "Skeleton subclass");
        c.assertTrue(create(c, SkyVariantMobs.AZURE_SLIME) instanceof Slime, "Slime subclass");
        c.assertTrue(is(SkyVariantMobs.WEATHERED_ZOMBIE, EntityTypeTags.ZOMBIES)
                && is(SkyVariantMobs.WEATHERED_ZOMBIE, EntityTypeTags.UNDEAD)
                && is(SkyVariantMobs.WEATHERED_ZOMBIE, EntityTypeTags.BURN_IN_DAYLIGHT)
                && is(SkyVariantMobs.WEATHERED_ZOMBIE, EntityTypeTags.SENSITIVE_TO_SMITE), "zombie family tags");
        c.assertTrue(is(SkyVariantMobs.SKY_ARCHER, EntityTypeTags.SKELETONS)
                && is(SkyVariantMobs.SKY_ARCHER, EntityTypeTags.UNDEAD)
                && is(SkyVariantMobs.SKY_ARCHER, EntityTypeTags.BURN_IN_DAYLIGHT)
                && is(SkyVariantMobs.SKY_ARCHER, EntityTypeTags.SENSITIVE_TO_SMITE), "skeleton family tags");
        c.assertTrue(is(SkyVariantMobs.AZURE_SLIME, EntityTypeTags.FALL_DAMAGE_IMMUNE), "azure slime fall immune tag");
        c.succeed();
    }

    @GameTest
    public void provisionalStatMultipliersApplyOnceAtFinalizeSpawn(GameTestHelper c) {
        stats(c, SkyVariantMobs.WEATHERED_ZOMBIE, EntityTypes.ZOMBIE, 1.10, 0.95, 1.05, 22.0);
        stats(c, SkyVariantMobs.SKY_ARCHER, EntityTypes.SKELETON, 0.95, 1.05, 1.00, 19.0);
        stats(c, SkyVariantMobs.ROCK_CREEPER, EntityTypes.CREEPER, 1.15, 0.95, 1.00, 23.0);
        stats(c, SkyVariantMobs.PALE_ENDERMAN, EntityTypes.ENDERMAN, 0.90, 1.05, 1.00, 36.0);
        // Re-finalizing must not compound the multipliers.
        Mob zombie = finalized(c, SkyVariantMobs.WEATHERED_ZOMBIE);
        zombie.finalizeSpawn(c.getLevel(), c.getLevel().getCurrentDifficultyAt(zombie.blockPosition()),
                EntitySpawnReason.COMMAND, null);
        c.assertTrue(Math.abs(zombie.getAttributeBaseValue(Attributes.MAX_HEALTH) - 22.0) < 1.0E-6,
                "repeat finalization keeps 22 HP");
        c.succeed();
    }

    private static void stats(GameTestHelper c, EntityType<? extends Mob> type, EntityType<? extends Mob> vanilla,
                              double health, double speed, double damage, double expectedHealth) {
        Mob mob = finalized(c, type);
        Mob base = create(c, vanilla);
        c.assertTrue(Math.abs(mob.getAttributeBaseValue(Attributes.MAX_HEALTH) - expectedHealth) < 1.0E-6
                        && Math.abs(base.getAttributeBaseValue(Attributes.MAX_HEALTH) * health - expectedHealth) < 1.0E-6,
                id(type) + " health " + mob.getAttributeBaseValue(Attributes.MAX_HEALTH));
        c.assertTrue(Math.abs(mob.getHealth() - expectedHealth) < 1.0E-4, id(type) + " spawns at full health");
        close(c, mob, base, Attributes.MOVEMENT_SPEED, speed, id(type) + " speed");
        if (base.getAttribute(Attributes.ATTACK_DAMAGE) != null)
            close(c, mob, base, Attributes.ATTACK_DAMAGE, damage, id(type) + " attack");
    }

    private static void close(GameTestHelper c, Mob mob, Mob base, Holder<Attribute> attribute, double multiplier,
                              String message) {
        double expected = base.getAttributeBaseValue(attribute) * multiplier;
        c.assertTrue(Math.abs(mob.getAttributeBaseValue(attribute) - expected) < 1.0E-6,
                message + ": expected " + expected + ", was " + mob.getAttributeBaseValue(attribute));
    }

    @GameTest
    public void weatheredZombieArmorIsOnlyLeatherOrChainAndArcherKeepsItsBow(GameTestHelper c) {
        c.assertTrue(WeatheredZombie.weatheredArmor(EquipmentSlot.HEAD, Items.IRON_HELMET) == Items.CHAINMAIL_HELMET
                && WeatheredZombie.weatheredArmor(EquipmentSlot.CHEST, Items.DIAMOND_CHESTPLATE) == Items.CHAINMAIL_CHESTPLATE
                && WeatheredZombie.weatheredArmor(EquipmentSlot.LEGS, Items.GOLDEN_LEGGINGS) == Items.LEATHER_LEGGINGS
                && WeatheredZombie.weatheredArmor(EquipmentSlot.FEET, Items.LEATHER_BOOTS) == Items.LEATHER_BOOTS,
                "material remap");
        Set<Item> allowed = Set.of(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS,
                Items.LEATHER_BOOTS, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS,
                Items.CHAINMAIL_BOOTS);
        int armored = 0;
        for (int i = 0; i < 400; i++) {
            Mob zombie = finalized(c, SkyVariantMobs.WEATHERED_ZOMBIE);
            for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                ItemStack stack = zombie.getItemBySlot(slot);
                if (stack.isEmpty()) continue;
                armored++;
                c.assertTrue(allowed.contains(stack.getItem()), "only leather/chain armor, got " + stack);
            }
            zombie.discard();
        }
        System.out.println("SKY_VARIANT weathered_zombie armor pieces over 400 spawns: " + armored);
        Mob archer = finalized(c, SkyVariantMobs.SKY_ARCHER);
        c.assertTrue(archer.getMainHandItem().is(Items.BOW), "Sky Archer spawns with a bow");
        c.succeed();
    }

    @GameTest(maxTicks = 100)
    public void weatheredZombieNeverBecomesADrowned(GameTestHelper c) {
        ServerLevel level = c.getLevel();
        BlockPos pool = new BlockPos(3, 1, 3);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            c.setBlock(pool.offset(dx, 0, dz), Blocks.WATER);
            c.setBlock(pool.offset(dx, 1, dz), Blocks.WATER);
        }
        Zombie weathered = c.spawn(SkyVariantMobs.WEATHERED_ZOMBIE, pool.offset(-1, 0, 0));
        Zombie control = c.spawn(EntityTypes.ZOMBIE, pool.offset(1, 0, 0));
        for (Zombie z : List.of(weathered, control)) {
            z.setNoAi(false);
            z.setPersistenceRequired();
            z.setBaby(false);
            z.setInWaterTime(10_000);
        }
        c.runAfterDelay(4, () -> {
            for (Zombie z : List.of(weathered, control)) z.setConversionTime(0);
        });
        c.runAfterDelay(12, () -> {
            c.assertTrue(weathered.isAlive() && !weathered.isRemoved() && !weathered.isUnderWaterConverting()
                            && weathered.getType() == SkyVariantMobs.WEATHERED_ZOMBIE,
                    "required policy: Weathered Zombie never starts a Drowned conversion");
            c.assertTrue(control.isRemoved(), "control: the vanilla Zombie converts under the same conditions");
            var drowned = level.getEntities(EntityTypes.DROWNED, c.getBounds().inflate(8), e -> true);
            c.assertTrue(drowned.size() == 1, "only the control became a Drowned, found " + drowned.size());
            drowned.forEach(Entity::discard);
            weathered.discard();
            c.succeed();
        });
    }

    @GameTest(maxTicks = 300)
    public void skyArcherNeverBecomesAStrayInPowderSnow(GameTestHelper c) {
        ServerLevel level = c.getLevel();
        BlockPos archerCell = new BlockPos(1, 1, 1);
        BlockPos controlCell = new BlockPos(3, 1, 3);
        for (BlockPos cell : List.of(archerCell, controlCell)) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 2; dy++)
                c.setBlock(cell.offset(dx, dy, dz), Blocks.STONE);
            c.setBlock(cell, Blocks.POWDER_SNOW);
            c.setBlock(cell.above(), Blocks.POWDER_SNOW);
        }
        Skeleton archer = c.spawn(SkyVariantMobs.SKY_ARCHER, archerCell);
        Skeleton control = c.spawn(EntityTypes.SKELETON, controlCell);
        for (Skeleton s : List.of(archer, control)) {
            s.setNoAi(false);
            s.setPersistenceRequired();
        }
        archer.startFreezeConversion(1);
        c.assertTrue(!archer.isShaking(), "the explicit Stray conversion entry point is inert for the Sky Archer");
        // Vanilla starts the conversion after 140 ticks in powder snow; check well after that.
        c.runAfterDelay(200, () -> {
            c.assertTrue(control.isShaking() || control.isRemoved(),
                    "control: a vanilla Skeleton in the same powder snow starts converting");
            c.assertTrue(archer.isAlive() && !archer.isRemoved() && !archer.isShaking()
                            && archer.getType() == SkyVariantMobs.SKY_ARCHER,
                    "required policy: the Sky Archer never starts a Stray conversion");
            c.assertTrue(level.getEntities(EntityTypes.STRAY, c.getBounds().inflate(4),
                    e -> e.blockPosition().closerThan(c.absolutePos(archerCell), 2.0)).isEmpty(), "no Stray from the archer");
            level.getEntities(EntityTypes.STRAY, c.getBounds().inflate(4), e -> true).forEach(Entity::discard);
            archer.discard();
            control.discard();
            c.succeed();
        });
    }

    @GameTest(maxTicks = 120)
    public void azureSlimeJumpsHigherIgnoresFallDamageAndSplitsIntoAzureSlimes(GameTestHelper c) {
        ServerLevel level = c.getLevel();
        Slime azure = create(c, SkyVariantMobs.AZURE_SLIME);
        Slime vanilla = create(c, EntityTypes.SLIME);
        double expectedJump = vanilla.getAttributeBaseValue(Attributes.JUMP_STRENGTH) * 1.25D;
        c.assertTrue(Math.abs(azure.getAttributeValue(Attributes.JUMP_STRENGTH) - expectedJump) < 1.0E-6,
                "jump strength x1.25: " + azure.getAttributeValue(Attributes.JUMP_STRENGTH));
        azure.setSize(4, true);
        vanilla.setSize(4, true);
        c.assertTrue(azure.getMaxHealth() == vanilla.getMaxHealth(), "HP x1.00 per size");
        c.assertTrue(Math.abs(azure.getAttributeValue(Attributes.JUMP_STRENGTH) - expectedJump) < 1.0E-6,
                "size changes keep the jump bonus");
        float before = azure.getHealth();
        azure.causeFallDamage(30.0, 1.0F, level.damageSources().fall());
        c.assertTrue(azure.getHealth() == before, "fall damage immune");

        Slime parent = c.spawn(SkyVariantMobs.AZURE_SLIME, new BlockPos(2, 2, 2));
        parent.setSize(2, true);
        parent.setNoAi(true);
        parent.hurtServer(level, level.damageSources().generic(), 1000.0F);
        c.runAfterDelay(40, () -> {
            var children = level.getEntities(SkyVariantMobs.AZURE_SLIME, c.getBounds().inflate(4),
                    e -> e != parent && e.isAlive());
            c.assertTrue(!children.isEmpty(), "a medium Azure Slime splits into Azure Slimes");
            for (Slime child : children) {
                c.assertTrue(child.getSize() == 1, "children are small");
                c.assertTrue(Math.abs(child.getAttributeValue(Attributes.JUMP_STRENGTH) - expectedJump) < 1.0E-6,
                        "split children keep the jump bonus");
            }
            children.forEach(Entity::discard);
            c.succeed();
        });
    }

    /** Stone box with a 1x2 dark interior at local (2,2,2). */
    private static BlockPos shell(GameTestHelper ctx) {
        var level = ctx.getLevel();
        for (int x = 0; x <= 4; x++) for (int y = 1; y <= 5; y++) for (int z = 0; z <= 4; z++)
            level.setBlock(ctx.absolutePos(new BlockPos(x, y, z)), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(ctx.absolutePos(new BlockPos(2, 2, 2)), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(ctx.absolutePos(new BlockPos(2, 3, 2)), Blocks.AIR.defaultBlockState(), 3);
        return ctx.absolutePos(new BlockPos(2, 2, 2));
    }

    @GameTest(maxTicks = 60)
    public void naturalSpawnsNeedTheSkyIslandSurfaceAndRegion(GameTestHelper c) {
        ServerLevel level = c.getLevel();
        BlockPos dark = shell(c);
        c.runAfterDelay(20, () -> {
            for (var type : ALL) {
                c.assertTrue(SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.SPAWNER, dark, level.getRandom()),
                        id(type) + ": non-natural spawns keep the vanilla monster rule");
                c.assertTrue(!SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, dark, level.getRandom()),
                        id(type) + ": no natural spawn off the sky islands");
            }
            c.assertTrue(!CustomMobCatalog.skyIslandSurface(level, dark), "covered position is not a sky-island surface");
            // Open-air platform high above sea level: the SKY_ISLAND surface kind accepts it, but it is still outside
            // a generated sky-island piece, so the region gate keeps natural spawning off.
            BlockPos origin = c.absolutePos(BlockPos.ZERO);
            int high = Math.min(level.getMaxY() - 4, level.getSeaLevel() + CustomMobCatalog.SKY_ISLAND_MIN_HEIGHT_ABOVE_SEA + 10);
            BlockPos highFeet = new BlockPos(origin.getX(), high, origin.getZ());
            BlockPos lowFeet = new BlockPos(origin.getX() + 2, level.getSeaLevel() + 20, origin.getZ());
            for (BlockPos feet : List.of(highFeet, lowFeet)) {
                level.setBlock(feet.below(), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(feet, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(feet.above(), Blocks.AIR.defaultBlockState(), 3);
            }
            boolean highOk = CustomMobCatalog.skyIslandSurface(level, highFeet);
            boolean lowOk = CustomMobCatalog.skyIslandSurface(level, lowFeet);
            boolean highRegion = SkyIslandRegion.isIsland(level, highFeet);
            boolean highNatural = SpawnPlacements.checkSpawnRules(SkyVariantMobs.WEATHERED_ZOMBIE, level,
                    EntitySpawnReason.NATURAL, highFeet, level.getRandom());
            for (BlockPos feet : List.of(highFeet, lowFeet)) level.setBlock(feet.below(), Blocks.AIR.defaultBlockState(), 3);
            c.assertTrue(lowOk == false, "Y <= sea level + 30 is rejected");
            c.assertTrue(highOk || !level.canSeeSky(highFeet), "open sky above sea level + 30 is a sky-island surface");
            c.assertTrue(!highRegion && !highNatural, "outside a sky-island piece no natural spawn happens");
            c.succeed();
        });
    }

    @GameTest
    public void lootKeepsVanillaFamilyDropsAndAddsTheBonusPools(GameTestHelper c) {
        Player killer = c.makeMockPlayer(GameType.SURVIVAL);
        int n = 4000;
        int[] nuggets = range(c, "weathered_zombie", SkyVariantMobs.WEATHERED_ZOMBIE, killer, Items.IRON_NUGGET, n, 1);
        int[] feathers = range(c, "sky_archer", SkyVariantMobs.SKY_ARCHER, killer, Items.FEATHER, n, 1);
        int[] cobble = range(c, "rock_creeper", SkyVariantMobs.ROCK_CREEPER, killer, Items.COBBLESTONE, n, 2);
        int[] andesite = range(c, "rock_creeper", SkyVariantMobs.ROCK_CREEPER, killer, Items.ANDESITE, n, 2);
        int[] flesh = range(c, "weathered_zombie", SkyVariantMobs.WEATHERED_ZOMBIE, killer, Items.ROTTEN_FLESH, n, 2);
        int[] bones = range(c, "sky_archer", SkyVariantMobs.SKY_ARCHER, killer, Items.BONE, n, 2);
        int[] gunpowder = range(c, "rock_creeper", SkyVariantMobs.ROCK_CREEPER, killer, Items.GUNPOWDER, n, 2);
        int[] pearls = range(c, "pale_enderman", SkyVariantMobs.PALE_ENDERMAN, killer, Items.ENDER_PEARL, n, 1);
        for (int[] seen : List.of(nuggets, feathers, cobble, andesite))
            c.assertTrue(seen[0] > 0 && seen[1] > 0, "bonus pool rolls both 0 and its maximum");
        for (int[] seen : List.of(flesh, bones, gunpowder, pearls))
            c.assertTrue(seen[1] > 0, "vanilla family drops are preserved");
        Slime slime = create(c, SkyVariantMobs.AZURE_SLIME);
        slime.setSize(1, false);
        int balls = 0;
        for (long seed = 0; seed < n; seed++)
            balls += count(roll(c, "moreminecrafts:entities/azure_slime", slime, killer, seed), Items.SLIME_BALL);
        c.assertTrue(balls > 0, "small Azure Slimes keep the vanilla slime-ball drop");
        c.succeed();
    }

    /** Returns {count of rolls with zero of item, count of rolls reaching max}; fails if any roll exceeds max. */
    private static int[] range(GameTestHelper c, String id, EntityType<? extends Mob> type, Player killer, Item item,
                               int n, int max) {
        Mob victim = create(c, type);
        int zero = 0, top = 0;
        for (long seed = 0; seed < n; seed++) {
            int got = count(roll(c, "moreminecrafts:entities/" + id, victim, killer, seed), item);
            if (item == Items.IRON_NUGGET || item == Items.FEATHER || item == Items.COBBLESTONE || item == Items.ANDESITE)
                c.assertTrue(got <= max, id + " " + item + " at most " + max + ", got " + got);
            if (got == 0) zero++;
            if (got >= max) top++;
        }
        return new int[]{zero, top};
    }

    private static List<ItemStack> roll(GameTestHelper c, String table, Mob victim, Player killer, long seed) {
        ServerLevel level = c.getLevel();
        LootTable lootTable = level.getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(table)));
        LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, victim)
                .withParameter(LootContextParams.ORIGIN, victim.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
                .withParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer);
        return lootTable.getRandomItems(builder.create(LootContextParamSets.ENTITY), seed);
    }

    private static int count(List<ItemStack> stacks, Item item) {
        int total = 0;
        for (ItemStack stack : stacks) if (stack.is(item)) total += stack.getCount();
        return total;
    }
}
