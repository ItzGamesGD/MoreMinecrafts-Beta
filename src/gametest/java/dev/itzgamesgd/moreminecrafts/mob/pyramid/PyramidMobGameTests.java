package dev.itzgamesgd.moreminecrafts.mob.pyramid;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;

/** Runtime parity of the four ST-01 pyramid variants with their vanilla base types. */
public final class PyramidMobGameTests implements CustomTestMethodInvoker {
    private static final List<EntityType<? extends Mob>> VARIANTS = List.of(
            PyramidMobs.BURIED_MUMMY, PyramidMobs.CRYPT_ARCHER, PyramidMobs.FUNERAL_SERVANT, PyramidMobs.GILDED_MUMMY);
    private static final List<EntityType<? extends Mob>> BASES = List.of(
            EntityTypes.HUSK, EntityTypes.SKELETON, EntityTypes.ZOMBIE, EntityTypes.HUSK);
    private static final List<Holder<Attribute>> ATTRIBUTES = List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE,
            Attributes.MOVEMENT_SPEED, Attributes.ARMOR, Attributes.FOLLOW_RANGE, Attributes.KNOCKBACK_RESISTANCE,
            Attributes.SPAWN_REINFORCEMENTS_CHANCE);
    private static final List<TagKey<EntityType<?>>> TAGS = List.of(EntityTypeTags.UNDEAD, EntityTypeTags.ZOMBIES,
            EntityTypeTags.SKELETONS, EntityTypeTags.BURN_IN_DAYLIGHT, EntityTypeTags.SENSITIVE_TO_SMITE,
            EntityTypeTags.INVERTED_HEALING_AND_HARM, EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE,
            EntityTypeTags.WITHER_FRIENDS, EntityTypeTags.IGNORES_POISON_AND_REGEN, EntityTypeTags.CAN_BREATHE_UNDER_WATER);

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }

    @GameTest
    public void variantsKeepVanillaClassAttributesDimensionsAndTags(GameTestHelper helper) {
        for (int i = 0; i < VARIANTS.size(); i++) {
            EntityType<? extends Mob> variant = VARIANTS.get(i);
            EntityType<? extends Mob> base = BASES.get(i);
            Mob mob = variant.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            Mob reference = base.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            helper.assertTrue(mob != null && reference != null, "creatable " + variant);
            // Husk/Zombie variants are subclasses of the vanilla class; the archer sits on AbstractSkeleton (see CryptArcher).
            Class<?> vanillaBase = base == EntityTypes.SKELETON ? AbstractSkeleton.class : reference.getClass();
            helper.assertTrue(vanillaBase.isInstance(mob), "vanilla class/AI " + variant);
            helper.assertTrue(mob.getClass().getSuperclass() == vanillaBase, "direct vanilla subclass " + variant);
            helper.assertTrue(mob.canFreeze() == reference.canFreeze(), "vanilla freeze rule " + variant);
            for (Holder<Attribute> attribute : ATTRIBUTES) {
                boolean present = reference.getAttributes().hasAttribute(attribute);
                helper.assertTrue(mob.getAttributes().hasAttribute(attribute) == present,
                        "same attribute set " + attribute.getRegisteredName() + " " + variant);
                if (!present) continue;
                helper.assertTrue(mob.getAttributeBaseValue(attribute) == reference.getAttributeBaseValue(attribute),
                        "vanilla " + attribute.getRegisteredName() + " " + variant);
            }
            var dims = variant.getDimensions();
            var baseDims = base.getDimensions();
            helper.assertTrue(dims.width() == baseDims.width() && dims.height() == baseDims.height()
                    && dims.eyeHeight() == baseDims.eyeHeight() && dims.fixed() == baseDims.fixed(),
                    "vanilla dimensions " + variant);
            for (var attachment : net.minecraft.world.entity.EntityAttachment.values()) {
                helper.assertTrue(dims.attachments().getNullable(attachment, 0, 0F) == null
                        ? baseDims.attachments().getNullable(attachment, 0, 0F) == null
                        : dims.attachments().getNullable(attachment, 0, 0F)
                                .equals(baseDims.attachments().getNullable(attachment, 0, 0F)),
                        "vanilla " + attachment + " attachment " + variant);
            }
            helper.assertTrue(mob.getEyeHeight() == reference.getEyeHeight(), "vanilla eye height " + variant);
            helper.assertTrue(variant.getCategory() == base.getCategory(), "monster category " + variant);
            helper.assertTrue(variant.clientTrackingRange() == base.clientTrackingRange(), "tracking range " + variant);
            helper.assertTrue(SpawnPlacements.getPlacementType(variant) == SpawnPlacements.getPlacementType(base)
                    && SpawnPlacements.getHeightmapType(variant) == SpawnPlacements.getHeightmapType(base),
                    "vanilla spawn placement " + variant);
            for (TagKey<EntityType<?>> tag : TAGS) {
                helper.assertTrue(variant.builtInRegistryHolder().is(tag) == base.builtInRegistryHolder().is(tag), "tag parity " + tag.location() + " " + variant);
            }
        }
        helper.succeed();
    }

    @GameTest
    public void summonCommandCreatesEachVariant(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var source = level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput()
                .withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.OWNER);
        for (EntityType<? extends Mob> variant : VARIANTS) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(variant).toString();
            level.getServer().getCommands().performPrefixedCommand(source,
                    "summon " + id + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " {NoAI:1b}");
        }
        helper.runAfterDelay(2, () -> {
            for (EntityType<? extends Mob> variant : VARIANTS) {
                List<? extends Entity> found = level.getEntities(variant, new AABB(pos).inflate(3), e -> true);
                helper.assertTrue(found.size() == 1, "/summon created " + variant + ": " + found.size());
                found.forEach(Entity::discard);
            }
            helper.succeed();
        });
    }

    @GameTest
    public void zombieFamilyVariantsSupportBabies(GameTestHelper helper) {
        for (EntityType<? extends Mob> variant : List.of(PyramidMobs.BURIED_MUMMY, PyramidMobs.FUNERAL_SERVANT,
                PyramidMobs.GILDED_MUMMY)) {
            Zombie zombie = (Zombie) variant.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            helper.assertTrue(zombie != null, "creatable " + variant);
            zombie.setBaby(true);
            helper.assertTrue(zombie.isBaby(), "baby state " + variant);
            Zombie vanilla = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            vanilla.setBaby(true);
            helper.assertTrue(zombie.getBbHeight() == vanilla.getBbHeight(), "vanilla baby size " + variant);
        }
        helper.succeed();
    }

    @GameTest
    public void lootTablesAliasTheVanillaBaseTables(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player killer = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int i = 0; i < VARIANTS.size(); i++) {
            EntityType<? extends Mob> variant = VARIANTS.get(i);
            Mob victim = variant.create(level, EntitySpawnReason.COMMAND);
            victim.snapTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)));
            ResourceKey<LootTable> own = variant.getDefaultLootTable().orElseThrow();
            ResourceKey<LootTable> vanilla = BASES.get(i).getDefaultLootTable().orElseThrow();
            helper.assertTrue(own.identifier().getNamespace().equals("moreminecrafts"), "own table " + own);
            LootTable ownTable = level.getServer().reloadableRegistries().getLootTable(own);
            helper.assertTrue(ownTable != LootTable.EMPTY, "loot table present " + own);
            // Same table structure: the alias resolves to the vanilla table. Over fixed seeds the same item kinds drop
            // and the totals agree (the alias consumes the random stream differently, so per-seed equality is not expected).
            java.util.Map<net.minecraft.world.item.Item, Integer> ownTotals = new java.util.HashMap<>();
            java.util.Map<net.minecraft.world.item.Item, Integer> vanillaTotals = new java.util.HashMap<>();
            for (long seed = 0; seed < 1000; seed++) {
                roll(level, own, victim, killer, seed).forEach(st -> ownTotals.merge(st.getItem(), st.getCount(), Integer::sum));
                roll(level, vanilla, victim, killer, seed).forEach(st -> vanillaTotals.merge(st.getItem(), st.getCount(), Integer::sum));
            }
            helper.assertTrue(ownTotals.keySet().equals(vanillaTotals.keySet()),
                    "drop kinds " + variant + ": " + ownTotals + " vs " + vanillaTotals);
            for (var e : vanillaTotals.entrySet()) {
                int mine = ownTotals.getOrDefault(e.getKey(), 0);
                helper.assertTrue(Math.abs(mine - e.getValue()) <= Math.max(12, e.getValue() * 0.12),
                        "drop totals " + variant + " " + e.getKey() + ": " + mine + " vs " + e.getValue());
            }
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ conversion blocking (owner decision 2026-11-14)

    @GameTest(maxTicks = 100)
    public void waterNeverConvertsHuskVariantsOrTheFuneralServant(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pool = new BlockPos(3, 1, 3);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            helper.setBlock(pool.offset(dx, 0, dz), Blocks.WATER);
            helper.setBlock(pool.offset(dx, 1, dz), Blocks.WATER);
        }
        List<Zombie> variants = List.of(
                helper.spawn(PyramidMobs.BURIED_MUMMY, pool.offset(-2, 0, 0)),
                helper.spawn(PyramidMobs.GILDED_MUMMY, pool.offset(2, 0, 0)),
                helper.spawn(PyramidMobs.FUNERAL_SERVANT, pool.offset(0, 0, 2)));
        // positive controls: the same setup converts the vanilla Husk (to Zombie) and Zombie (to Drowned)
        Zombie huskControl = helper.spawn(EntityTypes.HUSK, pool.offset(-1, 0, -2));
        Zombie zombieControl = helper.spawn(EntityTypes.ZOMBIE, pool.offset(1, 0, -2));
        List<Zombie> all = new ArrayList<>(variants);
        all.add(huskControl);
        all.add(zombieControl);
        for (Zombie z : all) {
            z.setNoAi(false);
            z.setPersistenceRequired();
            z.setInWaterTime(10_000);
        }
        helper.runAfterDelay(4, () -> all.forEach(z -> z.setConversionTime(0)));
        helper.runAfterDelay(40, () -> {
            for (Zombie z : variants) {
                helper.assertTrue(z.isAlive() && !z.isRemoved() && !z.isUnderWaterConverting(),
                        "no water conversion for " + z.getType());
            }
            helper.assertTrue(huskControl.isRemoved() || !huskControl.isAlive(), "control: vanilla Husk converts in water");
            helper.assertTrue(zombieControl.isRemoved() || !zombieControl.isAlive(), "control: vanilla Zombie converts in water");
            helper.assertTrue(level.getEntities(EntityTypes.DROWNED, helper.getBounds().inflate(8), e -> true).size() >= 1,
                    "control: the vanilla Zombie became a Drowned");
            for (var type : List.of(PyramidMobs.BURIED_MUMMY, PyramidMobs.GILDED_MUMMY, PyramidMobs.FUNERAL_SERVANT)) {
                helper.assertTrue(level.getEntities(type, helper.getBounds().inflate(8), e -> true).size() == 1,
                        "the variant is still present exactly once: " + type);
            }
            level.getEntities(EntityTypes.DROWNED, helper.getBounds().inflate(8), e -> true).forEach(Entity::discard);
            level.getEntities(EntityTypes.ZOMBIE, helper.getBounds().inflate(8), e -> true).forEach(Entity::discard);
            all.forEach(Entity::discard);
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 450)
    public void powderSnowNeverConvertsTheCryptArcher(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(!Skeleton.class.isAssignableFrom(CryptArcher.class),
                "the archer is not a vanilla Skeleton, so the powder-snow Stray conversion does not exist");
        BlockPos archerPos = new BlockPos(2, 1, 2);
        BlockPos controlPos = new BlockPos(5, 1, 2);
        for (BlockPos center : List.of(archerPos, controlPos)) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(center.offset(dx, 0, dz), Blocks.POWDER_SNOW);
                helper.setBlock(center.offset(dx, 1, dz), Blocks.AIR);
            }
        }
        CryptArcher archer = helper.spawn(PyramidMobs.CRYPT_ARCHER, archerPos);
        Skeleton control = helper.spawn(EntityTypes.SKELETON, controlPos);
        for (Mob m : List.of(archer, control)) {
            m.setNoAi(true); // nothing wanders out of the powder snow; entity ticking (and the conversion timer) still runs
            m.setPersistenceRequired();
        }
        helper.runAfterDelay(400, () -> {
            System.out.println("PYRAMID_POWDER_SNOW control removed=" + control.isRemoved() + " alive=" + control.isAlive()
                    + " shaking=" + control.isShaking() + " inPowder=" + control.isInPowderSnow);
            helper.assertTrue(archer.isAlive() && !archer.isRemoved() && !archer.isShaking()
                            && archer.getHealth() == archer.getMaxHealth(),
                    "the Crypt Archer stays an unharmed archer in powder snow");
            helper.assertTrue(level.getEntities(EntityTypes.STRAY, helper.getBounds().inflate(8), e -> true).size() <= 1,
                    "at most the control became a Stray");
            helper.assertTrue(control.isRemoved() || !control.isAlive() || control.isShaking(),
                    "control: the vanilla Skeleton starts or completes a Stray conversion in the same powder snow");
            level.getEntities(EntityTypes.STRAY, helper.getBounds().inflate(8), e -> true).forEach(Entity::discard);
            archer.discard();
            control.discard();
            helper.succeed();
        });
    }

    private static List<ItemStack> roll(ServerLevel level, ResourceKey<LootTable> key, Mob victim, Player killer, long seed) {
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, victim)
                .withParameter(LootContextParams.ORIGIN, victim.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
                .withParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                .create(LootContextParamSets.ENTITY);
        return level.getServer().reloadableRegistries().getLootTable(key).getRandomItems(params, seed);
    }
}
