package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** The five accessory abilities running together from the Wave 9 combined tree. */
public final class AccessoryAbilityIntegrationGameTests implements CustomTestMethodInvoker {
    private static final Identifier STAR_BONUS_ID = ModIds.id("star_covenant_melee_bonus");

    private static ItemStack stack(AccessoryDefinition ability) {
        return new ItemStack(ModItems.ACCESSORIES.get(ability));
    }

    private static void equip(ServerPlayer player, AccessoryDefinition ability) {
        new AccessoryEquipmentContainer(player).setItem(ability.category().ordinal(), stack(ability));
    }

    private static long starModifiers(ServerPlayer player) {
        return player.getAttribute(Attributes.ATTACK_DAMAGE).getModifiers().stream()
                .filter(modifier -> modifier.id().equals(STAR_BONUS_ID)).count();
    }

    @GameTest
    public void starCovenantRingOfLifeAndSnowCharmCoexistWithoutInterference(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        equip(player, AccessoryDefinition.STAR_COVENANT);
        equip(player, AccessoryDefinition.RING_OF_LIFE);
        equip(player, AccessoryDefinition.SNOW_CHARM);
        player.setHealth(4.0F);
        for (int i = 0; i < 5; i++) {
            StarCovenantAbility.refresh(player);
            AccessoryAbilities.tick(player);
        }
        ctx.assertTrue(starModifiers(player) == 1, "Star Covenant keeps exactly one modifier across repeated ticks");
        ctx.assertTrue(Math.abs(player.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(STAR_BONUS_ID).amount() - 0.08D) < 1.0E-9D,
                "+0 Star Covenant gives 8 percent");
        ctx.assertTrue(player.hasEffect(MobEffects.REGENERATION), "Ring of Life fired");
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.RING_OF_LIFE) == 2400 - 4,
                "Ring of Life activated once and then cooled down on later ticks");
        ctx.assertTrue(Math.abs(SnowCharmAbility.sinkingMultiplier(player) - 0.30D) < 1.0E-9D,
                "Snow Charm keeps its 70 percent sinking reduction alongside the other abilities");

        TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
        player.saveWithoutId(saved);
        ServerPlayer loaded = ctx.makeMockServerPlayerInLevel();
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, loaded.registryAccess(), saved.buildResult()));
        loaded.setHealth(4.0F);
        StarCovenantAbility.refresh(loaded);
        AccessoryAbilities.tick(loaded);
        ctx.assertTrue(starModifiers(loaded) == 1, "Star Covenant recomputes once after reload");
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(loaded, AccessoryDefinition.RING_OF_LIFE) == 2400 - 5,
                "Ring of Life cooldown survives reload and is not restarted");
        ctx.assertTrue(loaded.hasAttached(EquipmentAttachments.RING_OF_LIFE_SPENT),
                "Ring of Life re-arm state survives reload");
        ctx.assertTrue(Math.abs(SnowCharmAbility.sinkingMultiplier(loaded) - 0.30D) < 1.0E-9D,
                "Snow Charm follows the saved loadout after reload");

        ServerPlayer other = ctx.makeMockServerPlayerInLevel();
        ctx.assertTrue(starModifiers(other) == 0 && AccessoryCooldownRuntime.remaining(other, AccessoryDefinition.RING_OF_LIFE) == 0
                        && SnowCharmAbility.sinkingMultiplier(other) == 1.0D,
                "another player is unaffected by this player's accessories and cooldowns");
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void emberRingAndDeepSeaPearlKeepIndependentCooldowns(GameTestHelper ctx) {
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
        equip(player, AccessoryDefinition.STAR_COVENANT);
        equip(player, AccessoryDefinition.EMBER_RING);
        equip(player, AccessoryDefinition.DEEP_SEA_PEARL);
        var fire = ctx.getLevel().damageSources().inFire();
        AccessoryAbilities.afterDamage(player, fire);
        ctx.assertTrue(AccessoryAbilities.reduceFireDamage(player, fire, 10) == 6, "Ember Ring protects later fire hits");
        for (int i = 0; i < 41; i++) AccessoryAbilities.tick(player);
        ctx.assertTrue(player.hasEffect(MobEffects.WATER_BREATHING), "Deep Sea Pearl fired with no horizontal input");
        long ember = AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.EMBER_RING);
        long pearl = AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.DEEP_SEA_PEARL);
        ctx.assertTrue(ember == 600 - 41 && pearl > 0 && pearl < 900,
                "each ability owns its own cooldown entry: ember=" + ember + " pearl=" + pearl);
        TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
        player.saveWithoutId(saved);
        ServerPlayer loaded = ctx.makeMockServerPlayerInLevel();
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, loaded.registryAccess(), saved.buildResult()));
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(loaded, AccessoryDefinition.EMBER_RING) == ember
                        && AccessoryCooldownRuntime.remaining(loaded, AccessoryDefinition.DEEP_SEA_PEARL) == pearl,
                "both cooldowns survive save/load exactly");
        ctx.succeed();
    }

    @GameTest(maxTicks = 40)
    public void registeredServerTickHandlersRunExactlyOncePerTick(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ctx.assertTrue(ctx.getLevel().getServer().getPlayerList().getPlayers().contains(player),
                "the mock player is in the real player list so the registered tick events reach it");
        AccessoryCooldownRuntime.start(player, AccessoryDefinition.EMBER_RING, 600);
        ctx.runAfterDelay(10, () -> {
            long remaining = AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.EMBER_RING);
            ctx.assertTrue(remaining >= 589 && remaining <= 590,
                    "ten server ticks decrement the cooldown by ten (no duplicate listener): " + remaining);
            ctx.succeed();
        });
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
