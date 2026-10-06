package dev.itzgamesgd.moreminecrafts.mob;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

public final class CustomMobGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void allRosterEntriesRegisterAttributesAndInstantiate(GameTestHelper context) {
        var types = CustomMobCatalog.allTypes();
        context.assertTrue(types.size() == 40, "All forty approved mob types must have spawn definitions");
        Set<String> ids = new HashSet<>();
        for (var type : types) {
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            context.assertTrue(id != null && id.getNamespace().equals("moreminecrafts"),
                    "Every roster type must have a stable MoreMinecrafts registry id");
            context.assertTrue(ids.add(id.toString()), "Roster registry ids must be unique: " + id);
            context.assertTrue(SpawnPlacements.getPlacementType(type) != null,
                    "Every naturally spawned roster entry must have a placement rule: " + id);
            Mob mob = type.create(context.getLevel(), EntitySpawnReason.COMMAND);
            context.assertTrue(mob != null, "Roster entity must instantiate: " + id);
            DifficultyInstance difficulty = context.getLevel().getCurrentDifficultyAt(mob.blockPosition());
            mob.finalizeSpawn(context.getLevel(), difficulty, EntitySpawnReason.COMMAND, null);
            context.assertTrue(mob.getAttribute(Attributes.MAX_HEALTH) != null
                            && mob.getAttributeValue(Attributes.MAX_HEALTH) > 0,
                    "Roster entity must have registered health attributes: " + id);
            context.assertTrue(mob.getAttribute(Attributes.MOVEMENT_SPEED) != null,
                    "Roster entity must have movement attributes: " + id);
            String path = id.getPath();
            if (path.endsWith("_zombie") || path.endsWith("_skeleton") || path.contains("drowned")
                    || path.equals("lesser_phantom")) {
                context.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(EntityTypeTags.UNDEAD),
                        "Undead variants must retain vanilla undead semantics: " + id);
            }
            if (path.contains("spider") || path.endsWith("_bug") || path.equals("giant_bee")) {
                context.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(EntityTypeTags.ARTHROPOD),
                        "Arthropod variants must retain vanilla Bane semantics: " + id);
            }
            if (id.getPath().equals("miner_zombie")) {
                context.assertTrue(mob.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_PICKAXE),
                        "Miner Zombie must spawn with its pickaxe");
            } else if (id.getPath().equals("rotten_skeleton") || id.getPath().equals("cave_skeleton")) {
                context.assertTrue(mob.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.STONE_SWORD),
                        "Melee skeleton variants must spawn with stone swords");
            } else if (id.getPath().equals("miner_skeleton")) {
                context.assertTrue(mob.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_PICKAXE),
                        "Miner Skeleton must spawn with its pickaxe");
            } else if (id.getPath().equals("armored_skeleton")) {
                context.assertTrue(mob.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)
                                && mob.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE),
                        "Armored Skeleton must spawn with its armor");
            } else if (id.getPath().equals("bandit_archer")) {
                context.assertTrue(mob.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.BOW),
                        "Bandit Archer must spawn with a bow");
            } else if (id.getPath().equals("bandit")) {
                context.assertTrue(mob.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_SWORD),
                        "Bandit must use its approved melee sword");
            }
            mob.discard();
        }
        context.assertTrue(ids.size() == 40, "All forty resource identities must remain unique");
        context.assertTrue(SpawnPlacements.getPlacementType(CustomMobCatalog.LESSER_PHANTOM)
                        == net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS,
                "Lesser Phantom must use a sky-capable spawn placement rather than a ground-only placement");
        Mob largeZombie = CustomMobCatalog.LARGE_ZOMBIE.create(context.getLevel(), EntitySpawnReason.COMMAND);
        largeZombie.finalizeSpawn(context.getLevel(), context.getLevel().getCurrentDifficultyAt(largeZombie.blockPosition()),
                EntitySpawnReason.COMMAND, null);
        double healthAfterFirstFinalize = largeZombie.getAttributeValue(Attributes.MAX_HEALTH);
        double speedAfterFirstFinalize = largeZombie.getAttributeValue(Attributes.MOVEMENT_SPEED);
        largeZombie.finalizeSpawn(context.getLevel(), context.getLevel().getCurrentDifficultyAt(largeZombie.blockPosition()),
                EntitySpawnReason.COMMAND, null);
        context.assertTrue(Math.abs(largeZombie.getAttributeValue(Attributes.MAX_HEALTH) - healthAfterFirstFinalize) < 0.001
                        && Math.abs(largeZombie.getAttributeValue(Attributes.MOVEMENT_SPEED) - speedAfterFirstFinalize) < 0.000001,
                "Catalog stat multipliers must not compound when spawn finalization is invoked twice");
        context.assertTrue(Math.abs(largeZombie.getAttributeValue(Attributes.SCALE) - 1.30) < 0.001,
                "Large Zombie must have its approved visible scale profile");
        largeZombie.discard();
        context.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
