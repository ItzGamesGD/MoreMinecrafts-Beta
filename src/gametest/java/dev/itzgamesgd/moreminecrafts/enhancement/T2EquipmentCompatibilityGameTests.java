package dev.itzgamesgd.moreminecrafts.enhancement;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.progression.RawXp;
import dev.itzgamesgd.moreminecrafts.progression.T1AnvilModes;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;

/**
 * T2 Wave C: the six T2 elemental equipment pieces have infinite durability, keep the ordinary +20 Enhancement with its
 * +5/+10/+15/+20 milestones, and keep enchantments, legacy damage data and Enhancement state through save/load.
 */
public final class T2EquipmentCompatibilityGameTests {
    private static final List<Item> SIX = List.of(ModItems.FLAME_SWORD, ModItems.FLOWING_WATER_SWORD, ModItems.WIND_SWORD,
            ModItems.WIND_ARCHERS_BOW, ModItems.EARTH_MACE, ModItems.LIGHTNING_SWORD);

    private static String name(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    private static void enchant(GameTestHelper ctx, ItemStack stack) {
        var holder = ctx.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
        EnchantmentHelper.updateEnchantments(stack, enchantments -> enchantments.set(holder, 3));
    }

    @GameTest
    public void sixEquipmentNeverLoseDurability(GameTestHelper ctx) {
        ServerPlayer player = EnhancementTransactionGameTests.survival(ctx);
        for (Item item : SIX) {
            ItemStack stack = new ItemStack(item);
            ctx.assertTrue(stack.has(DataComponents.UNBREAKABLE), name(item) + " is Unbreakable");
            ctx.assertTrue(!stack.isDamageableItem() && !stack.isDamaged(), name(item) + " is not damageable");
            stack.hurtAndBreak(5_000, ctx.getLevel(), player, broken -> ctx.fail(name(item) + " must never break"));
            stack.hurtAndBreak(5_000, player, EquipmentSlot.MAINHAND);
            ctx.assertTrue(!stack.isEmpty() && stack.getCount() == 1 && stack.getDamageValue() == 0 && !stack.isDamaged(),
                    name(item) + " takes no wear from hurtAndBreak");
            ctx.assertTrue(stack.getItem() == item, name(item) + " keeps its item ID");
        }
        ctx.succeed();
    }

    @GameTest
    public void legacyDamagedStacksLoadWithoutLossAndStayUndamaged(GameTestHelper ctx) {
        var ops = RegistryOps.create(NbtOps.INSTANCE, ctx.getLevel().registryAccess());
        for (Item item : SIX) {
            // A pre-Wave-C save stores only deltas: heavy wear, an enchantment and an Enhancement level.
            ItemStack source = new ItemStack(item);
            enchant(ctx, source);
            source.set(ModDataComponents.ENHANCEMENT_LEVEL, 7);
            CompoundTag saved = (CompoundTag) ItemStack.CODEC.encodeStart(ops, source).getOrThrow();
            CompoundTag components = saved.getCompoundOrEmpty("components");
            components.putInt("minecraft:damage", 450);
            saved.put("components", components);

            ItemStack loaded = ItemStack.CODEC.parse(ops, saved).getOrThrow();
            ctx.assertTrue(!loaded.isEmpty() && loaded.getItem() == item, name(item) + " legacy stack still loads (no item loss)");
            ctx.assertTrue(!loaded.isDamaged() && !loaded.isDamageableItem(), name(item) + " legacy wear no longer applies");
            ctx.assertTrue(Integer.valueOf(450).equals(loaded.get(DataComponents.DAMAGE)),
                    name(item) + " keeps the stored damage value untouched");
            ctx.assertTrue(EnhancementOperation.level(loaded) == 7, name(item) + " keeps Enhancement +7");
            ctx.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(loaded).size() == 1, name(item) + " keeps its enchantment");
            loaded.hurtAndBreak(900, ctx.getLevel(), EnhancementTransactionGameTests.survival(ctx), broken -> ctx.fail("break"));
            ctx.assertTrue(!loaded.isEmpty() && Integer.valueOf(450).equals(loaded.get(DataComponents.DAMAGE)),
                    name(item) + " legacy stack takes no further wear");

            ItemStack again = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, loaded).getOrThrow()).getOrThrow();
            ctx.assertTrue(ItemStack.matches(loaded, again), name(item) + " round-trips through save/load unchanged");
        }
        ctx.succeed();
    }

    @GameTest
    public void enhancementBoundariesToPlusTwentyWithMilestones(GameTestHelper ctx) {
        ServerPlayer player = EnhancementTransactionGameTests.survival(ctx);
        for (Item item : SIX) {
            ctx.assertTrue(EnhancementCatalog.definition(item).orElseThrow().category().maximumLevel() == 20,
                    name(item) + " is capped at +20");
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponents.DAMAGE, 123); // legacy wear must not obstruct Enhancement
            int totalXp = 0;
            for (int target = 1; target <= 20; target++) {
                EnhancementRules.Cost cost = EnhancementRules.costFor(target);
                totalXp += cost.rawXpCost();
                RawXp.setTotal(player, cost.rawXpCost());
                ItemStack stones = new ItemStack(ModItems.ENHANCEMENT_STONE, cost.materialCount());
                ctx.assertTrue(EnhancementOperation.apply(player, stack, stones), name(item) + " +" + target + " applies");
                ctx.assertTrue(EnhancementOperation.level(stack) == target && stones.isEmpty() && RawXp.available(player) == 0,
                        name(item) + " +" + target + " charges exactly " + cost);
                ctx.assertTrue(EnhancementRules.isMilestone(target) == (target % 5 == 0)
                        && cost.materialCount() == (target % 5 == 0 ? 2 : 1), name(item) + " milestone cost at +" + target);
            }
            ctx.assertTrue(totalXp == 1_900, name(item) + " +0→+20 costs 1,900 XP");
            RawXp.setTotal(player, 10_000);
            ItemStack extra = new ItemStack(ModItems.ENHANCEMENT_STONE, 8);
            ctx.assertTrue(!EnhancementOperation.apply(player, stack, extra) && EnhancementOperation.level(stack) == 20
                    && extra.getCount() == 8 && RawXp.available(player) == 10_000, name(item) + " refuses +21 and changes nothing");
            ctx.assertTrue(!stack.isDamaged() && Integer.valueOf(123).equals(stack.get(DataComponents.DAMAGE)),
                    name(item) + " Enhancement left the legacy damage data untouched");
        }
        ctx.succeed();
    }

    @GameTest
    public void anvilEnhancementKeepsEnchantmentsAndSurvivesSaveLoad(GameTestHelper ctx) {
        ServerPlayer player = EnhancementTransactionGameTests.survival(ctx);
        var ops = RegistryOps.create(NbtOps.INSTANCE, ctx.getLevel().registryAccess());
        BlockPos pos = ctx.absolutePos(new BlockPos(1, 1, 1));
        ctx.getLevel().setBlock(pos, Blocks.ANVIL.defaultBlockState(), 3);
        player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        for (Item item : SIX) {
            // Each anvil use can damage the block at random; restore a fresh anvil so the loop is deterministic.
            ctx.getLevel().setBlock(pos, Blocks.ANVIL.defaultBlockState(), 3);
            RawXp.setTotal(player, 40);
            var menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.create(ctx.getLevel(), pos));
            ItemStack stack = new ItemStack(item);
            enchant(ctx, stack);
            menu.getSlot(0).set(stack);
            menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE, 4));
            menu.createResult();
            ctx.assertTrue(((T1AnvilModes) menu).moreminecrafts$mode() == 0, "default anvil tab is Enhance");
            ctx.assertTrue(((T1AnvilModes) menu).moreminecrafts$button(player, 3), name(item) + " enhances through the server-side anvil action");
            ItemStack result = menu.getSlot(0).getItem();
            ctx.assertTrue(EnhancementOperation.level(result) == 1 && menu.getSlot(1).getItem().getCount() == 3
                    && RawXp.available(player) == 0, name(item) + " anvil +1 charges 40 XP and 1 stone");
            ctx.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(result).size() == 1, name(item) + " enchantment survives Enhancement");
            ItemStack restored = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, result).getOrThrow()).getOrThrow();
            ctx.assertTrue(ItemStack.matches(result, restored) && EnhancementOperation.level(restored) == 1,
                    name(item) + " enchanted +1 item round-trips through save/load");
        }
        ctx.succeed();
    }

    @GameTest
    public void normalAnvilOffersNoRepairForSixEquipment(GameTestHelper ctx) {
        ServerPlayer player = EnhancementTransactionGameTests.survival(ctx);
        player.giveExperiencePoints(10_000);
        List<Item> repairMaterials = List.of(Items.DIAMOND, Items.NETHERITE_INGOT, Items.BREEZE_ROD, Items.STRING, Items.IRON_INGOT);
        for (Item item : SIX) {
            for (Item material : repairMaterials) {
                var menu = new AnvilMenu(1, player.getInventory());
                ((T1AnvilModes) menu).moreminecrafts$button(player, 1);
                ItemStack stack = new ItemStack(item);
                stack.set(DataComponents.DAMAGE, 200);
                menu.getSlot(0).set(stack);
                menu.getSlot(1).set(new ItemStack(material));
                menu.createResult();
                ctx.assertTrue(!menu.getSlot(2).hasItem(), name(item) + " gets no repair result from " + BuiltInRegistries.ITEM.getKey(material));
            }
            var menu = new AnvilMenu(1, player.getInventory());
            ((T1AnvilModes) menu).moreminecrafts$button(player, 1);
            ItemStack first = new ItemStack(item);
            first.set(DataComponents.DAMAGE, 200);
            ItemStack second = new ItemStack(item);
            second.set(DataComponents.DAMAGE, 300);
            menu.getSlot(0).set(first);
            menu.getSlot(1).set(second);
            menu.createResult();
            ctx.assertTrue(!menu.getSlot(2).hasItem(), name(item) + " cannot be combined to repair");
        }
        ctx.succeed();
    }

    @GameTest
    public void itemIdsAreUnchanged(GameTestHelper ctx) {
        List<String> ids = List.of("flame_sword", "flowing_water_sword", "wind_sword", "wind_archers_bow", "earth_mace", "lightning_sword");
        for (int i = 0; i < ids.size(); i++) {
            ctx.assertTrue(BuiltInRegistries.ITEM.getValue(ModIds.id(ids.get(i))) == SIX.get(i), ids.get(i) + " keeps its registry ID");
        }
        ctx.assertTrue(!new ItemStack(ModItems.DARK_ENERGY_STAFF).has(DataComponents.UNBREAKABLE),
                "Dark Energy Staff (deferred placeholder) is not touched");
        ctx.succeed();
    }
}
