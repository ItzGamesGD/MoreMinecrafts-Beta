package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Exercises Animal.mobInteract so feeding credit is checked at the installed mixin. */
public final class KnowledgeAnimalFeedGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void successfulFoodConsumptionCountsDistinctAnimalSpecies(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        Animal cow = animal(ctx, EntityType.COW, 2, 2, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 2));
        InteractionResult fed = cow.mobInteract(player, InteractionHand.MAIN_HAND);
        ctx.assertTrue(fed.consumesAction(), "adult cow accepts and consumes breeding wheat");
        ctx.assertTrue(player.getMainHandItem().getCount() == 1, "successful feed consumes exactly one item");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("animal_types_fed") == 1,
                "the installed Animal.mobInteract return hook records the cow species");

        Animal secondCow = animal(ctx, EntityType.COW, 4, 2, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT));
        InteractionResult fedAgain = secondCow.mobInteract(player, InteractionHand.MAIN_HAND);
        ctx.assertTrue(fedAgain.consumesAction(), "a second cow also accepts wheat");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("animal_types_fed") == 1,
                "feeding the same species again is deduplicated");

        Animal sheep = animal(ctx, EntityType.SHEEP, 6, 2, 2);
        ServerPlayer otherPlayer = ctx.makeMockServerPlayerInLevel();
        otherPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT));
        InteractionResult otherFed = sheep.mobInteract(otherPlayer, InteractionHand.MAIN_HAND);
        ctx.assertTrue(otherFed.consumesAction(), "another player successfully feeds a different species");
        ctx.assertTrue(KnowledgeRuntime.state(otherPlayer).conditionProgress("animal_types_fed") == 1,
                "the feeding player receives the sheep observation");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("animal_types_fed") == 1,
                "animal progress remains private to the interacting player");
        ctx.succeed();
    }

    @GameTest
    public void unsuccessfulNonFoodInteractionDoesNotCountAnAnimalSpecies(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        Animal cow = animal(ctx, EntityType.COW, 2, 2, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        InteractionResult result = cow.mobInteract(player, InteractionHand.MAIN_HAND);
        ctx.assertTrue(!result.consumesAction(), "cow rejects a non-food item");
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("animal_types_fed") == 0,
                "rejected interaction adds no species progress");
        ctx.succeed();
    }

    private static Animal animal(GameTestHelper ctx, EntityType<? extends Animal> type, double x, double y, double z) {
        Animal animal = type.create(ctx.getLevel());
        if (animal == null) throw new IllegalStateException("Could not create test animal " + type);
        var origin = ctx.absolutePos(new net.minecraft.core.BlockPos((int) x, (int) y, (int) z));
        animal.setPos(origin.getX() + .5, origin.getY(), origin.getZ() + .5);
        ctx.getLevel().addFreshEntity(animal);
        return animal;
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
