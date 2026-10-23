package dev.itzgamesgd.moreminecrafts.bounty;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;

/** Verifies every catalog ID against live registries and actual entity behavior. */
public final class ContractCatalogGameTests implements CustomTestMethodInvoker {
    private static List<ContractCatalog.Mob> allCombatMobs() {
        List<ContractCatalog.Mob> all = new ArrayList<>(ContractCatalog.VANILLA_MOBS);
        all.addAll(ContractCatalog.MM_ORDINARY_MOBS);
        all.addAll(ContractCatalog.MM_STRONG_MOBS);
        return all;
    }

    @GameTest
    public void everyCombatTargetIsRegisteredAndHostile(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        for (ContractCatalog.Mob mob : allCombatMobs()) {
            Identifier id = Identifier.parse(mob.id());
            ctx.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id), "registered: " + mob.id());
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
            Entity entity = type.create(level, EntitySpawnReason.COMMAND);
            ctx.assertTrue(entity != null, "creatable: " + mob.id());
            ctx.assertTrue(entity instanceof Enemy, "hostile by behavior: " + mob.id());
            ctx.assertTrue(!(entity instanceof NeutralMob) && !(entity instanceof TamableAnimal),
                    "not neutral or tameable: " + mob.id());
        }
        ctx.succeed();
    }

    @GameTest
    public void vanillaStrengthMatchesMaxHealthRule(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        for (ContractCatalog.Mob mob : ContractCatalog.VANILLA_MOBS) {
            Entity entity = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(mob.id())).create(level, EntitySpawnReason.COMMAND);
            double maxHealth = ((LivingEntity) entity).getMaxHealth();
            boolean strong = maxHealth >= 40.0;
            ctx.assertTrue(strong == (mob.strength() == ContractCatalog.Strength.STRONG),
                    "strength matches MAX_HEALTH rule: " + mob.id() + " health=" + maxHealth);
        }
        ctx.succeed();
    }

    @GameTest
    public void excludedMobsAreAbsentFromPools(GameTestHelper ctx) {
        for (String excluded : List.of("moreminecrafts:feral_wolf", "moreminecrafts:cave_bat", "moreminecrafts:giant_bee")) {
            ctx.assertTrue(allCombatMobs().stream().noneMatch(mob -> mob.id().equals(excluded)), "excluded: " + excluded);
        }
        ctx.succeed();
    }

    @GameTest
    public void specialOnlyAndPendingMobsAreRegisteredButNeverPooled(GameTestHelper ctx) {
        List<String> withheld = new ArrayList<>(ContractCatalog.SPECIAL_ONLY_MOBS);
        withheld.addAll(ContractCatalog.MM_PENDING_VERIFICATION);
        for (String id : withheld) {
            ctx.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.parse(id)), "registered: " + id);
            ctx.assertTrue(allCombatMobs().stream().noneMatch(mob -> mob.id().equals(id)), "not pooled: " + id);
        }
        ctx.succeed();
    }

    @GameTest
    public void allFortyMmEntitiesAreAccountedForExactlyOnce(GameTestHelper ctx) {
        List<String> accounted = new ArrayList<>();
        ContractCatalog.MM_ORDINARY_MOBS.forEach(m -> accounted.add(m.id()));
        ContractCatalog.MM_STRONG_MOBS.forEach(m -> accounted.add(m.id()));
        accounted.addAll(ContractCatalog.MM_EXCLUDED);
        accounted.addAll(ContractCatalog.MM_PENDING_VERIFICATION);
        List<String> registered = new ArrayList<>();
        BuiltInRegistries.ENTITY_TYPE.keySet().forEach(id -> {
            if (id.getNamespace().equals("moreminecrafts")) registered.add(id.toString());
        });
        ctx.assertTrue(registered.size() == 40, "40 registered MM entities, found " + registered.size());
        ctx.assertTrue(accounted.size() == 40, "40 accounted, found " + accounted.size());
        ctx.assertTrue(accounted.stream().distinct().count() == 40, "no duplicates");
        ctx.assertTrue(new java.util.HashSet<>(accounted).equals(new java.util.HashSet<>(registered)),
                "accounted set equals registry set");
        ctx.succeed();
    }

    @GameTest
    public void deliveryItemsAreRegistered(GameTestHelper ctx) {
        List<String> ids = new ArrayList<>(ContractCatalog.DELIVERY_ORDINARY);
        ids.addAll(ContractCatalog.DELIVERY_RARE);
        ids.addAll(ContractCatalog.HIGH_VALUE_DELIVERY);
        for (String id : ids)
            ctx.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse(id)), "registered item: " + id);
        ctx.succeed();
    }

    @GameTest
    public void explorationStructuresAreRegistered(GameTestHelper ctx) {
        for (String id : ContractCatalog.EXPLORATION_STRUCTURES) {
            ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> key =
                    ResourceKey.create(Registries.STRUCTURE, Identifier.parse(id));
            Holder<net.minecraft.world.level.levelgen.structure.Structure> holder = ctx.getLevel().registryAccess()
                    .lookupOrThrow(Registries.STRUCTURE).get(key).orElse(null);
            ctx.assertTrue(holder != null, "registered structure: " + id);
        }
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
