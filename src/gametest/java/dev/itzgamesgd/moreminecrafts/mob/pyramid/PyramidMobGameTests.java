package dev.itzgamesgd.moreminecrafts.mob.pyramid;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class PyramidMobGameTests implements CustomTestMethodInvoker {
    @Override
    public void invokeTestMethod(GameTestHelper context, java.lang.reflect.Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }

    @GameTest
    public void variantsMatchVanillaAttributes(GameTestHelper helper) {
        compare(helper, PyramidMobs.BURIED_MUMMY, EntityTypes.HUSK);
        compare(helper, PyramidMobs.GILDED_MUMMY, EntityTypes.HUSK);
        compare(helper, PyramidMobs.CRYPT_ARCHER, EntityTypes.SKELETON);
        compare(helper, PyramidMobs.FUNERAL_SERVANT, EntityTypes.ZOMBIE);
        helper.succeed();
    }
    private static void compare(GameTestHelper helper, EntityType<? extends Mob> variant, EntityType<? extends Mob> vanilla) {
        Mob mob = variant.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Mob reference = vanilla.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(mob != null && reference != null, "summonable " + variant);
        helper.assertTrue(mob.getClass() == reference.getClass(), "vanilla behavior " + variant);
        helper.assertTrue(mob.getAttributeBaseValue(Attributes.MAX_HEALTH) == reference.getAttributeBaseValue(Attributes.MAX_HEALTH), "vanilla health " + variant);
        helper.assertTrue(mob.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == reference.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), "vanilla damage " + variant);
    }
}
