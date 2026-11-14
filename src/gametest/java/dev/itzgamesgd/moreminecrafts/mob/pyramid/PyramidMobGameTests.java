package dev.itzgamesgd.moreminecrafts.mob.pyramid;

import java.lang.reflect.Method;
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
import net.minecraft.world.entity.monster.zombie.Zombie;
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
            helper.assertTrue(mob.getClass() == reference.getClass(), "vanilla class/AI " + variant);
            for (Holder<Attribute> attribute : ATTRIBUTES) {
                helper.assertTrue(mob.getAttributeBaseValue(attribute) == reference.getAttributeBaseValue(attribute),
                        "vanilla " + attribute.getRegisteredName() + " " + variant);
            }
            helper.assertTrue(variant.getDimensions().equals(base.getDimensions()), "vanilla dimensions " + variant);
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
            for (long seed = 0; seed < 40; seed++) {
                List<ItemStack> a = roll(level, own, victim, killer, seed);
                List<ItemStack> b = roll(level, vanilla, victim, killer, seed);
                helper.assertTrue(a.size() == b.size(), "drop count parity " + variant + " seed " + seed);
                for (int k = 0; k < a.size(); k++) {
                    helper.assertTrue(ItemStack.matches(a.get(k), b.get(k)), "drop parity " + variant + " seed " + seed);
                }
            }
        }
        helper.succeed();
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
