package dev.itzgamesgd.moreminecrafts.progression;
import java.lang.reflect.Method;
import java.util.*;
import dev.itzgamesgd.moreminecrafts.registry.*;
import net.fabricmc.fabric.api.gametest.v1.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;

public final class T1ProgressionGameTests implements CustomTestMethodInvoker {
    @GameTest public void itemTransactionsAndPersistence(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.giveExperiencePoints(10000);
        var sword=new ItemStack(Items.DIAMOND_SWORD);int before=RawXp.available(player);
        ctx.assertTrue(T1Operations.purchase(player,sword,"minecraft:sharpness"),"purchase Sharpness I");
        ctx.assertTrue(RawXp.available(player)==before-100,"deduct raw XP, not levels");
        ctx.assertTrue(!T1Operations.purchase(player,sword,"minecraft:mending"),"Mending requires Unbreaking III");
        var book=book(player,"minecraft:sharpness",1);
        ctx.assertTrue(T1Operations.apply(player,sword,book,()->0),"apply compatible book");
        ctx.assertTrue(book.isEmpty()&&T1Operations.option(sword,"sharpness")==5,"book consumed and low level can roll maximum");
        ctx.assertTrue(!T1Operations.apply(player,sword,book(player,"minecraft:sharpness",5),()->0),"cannot overwrite locked option");
        T1Operations.purchase(player,sword,"minecraft:fire_aspect");
        ctx.assertTrue(T1Operations.apply(player,sword,book(player,"minecraft:fire_aspect",1),()->.99),"second independent option");
        var ops=RegistryOps.create(NbtOps.INSTANCE,ctx.getLevel().registryAccess());
        var data=ItemStack.CODEC.encodeStart(ops,sword).getOrThrow();var restored=ItemStack.CODEC.parse(ops,data).getOrThrow();
        ctx.assertTrue(T1Operations.options(restored).equals(T1Operations.options(sword)),"Book Options survive item save/load");
        ctx.assertTrue(!T1Operations.reset(player,sword,"minecraft:sharpness"),"reset cannot omit materials");
        for(Item item:List.of(Items.DIAMOND,ModItems.MYTHRIL,Items.NETHERITE_INGOT,ModItems.ADAMANTIUM_INGOT))player.getInventory().add(new ItemStack(item));
        ctx.assertTrue(T1Operations.reset(player,sword,"minecraft:sharpness"),"reset succeeds with all four");
        ctx.assertTrue(!T1Operations.options(sword).containsKey("minecraft:sharpness")&&T1Operations.option(sword,"fire_aspect")==1,"only selected option cleared");
        for(Item item:List.of(Items.DIAMOND,ModItems.MYTHRIL,Items.NETHERITE_INGOT,ModItems.ADAMANTIUM_INGOT))ctx.assertTrue(player.getInventory().countItem(item)==0,"reset consumes each material");
        ctx.succeed();
    }
    @GameTest public void anvilModesAndBookCombination(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.giveExperiencePoints(10000);
        var menu=new AnvilMenu(1,player.getInventory());var modes=(T1AnvilModes)menu;
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE));menu.createResult();
        ctx.assertTrue(menu.getSlot(2).hasItem(),"enhancement accepts stone");
        menu.getSlot(1).set(new ItemStack(Items.DIRT));menu.createResult();ctx.assertTrue(!menu.getSlot(2).hasItem(),"enhancement rejects unrelated materials");
        modes.moreminecrafts$button(player,1);menu.getSlot(1).set(book(player,"minecraft:sharpness",1));menu.createResult();
        ctx.assertTrue(!menu.getSlot(2).hasItem(),"normal anvil blocks direct book enchantment");
        modes.moreminecrafts$button(player,2);menu.getSlot(0).set(book(player,"minecraft:sharpness",1));menu.getSlot(1).set(book(player,"minecraft:sharpness",1));menu.createResult();
        ctx.assertTrue(T1Operations.book(menu.getSlot(2).getItem()).orElseThrow().level()==2,"equal books combine one level");
        int before=RawXp.available(player);menu.getSlot(2).onTake(player,menu.getSlot(2).getItem().copy());
        ctx.assertTrue(RawXp.available(player)==before-200,"book combination pays raw XP");
        ctx.assertTrue(menu.getSlot(0).getItem().isEmpty()&&menu.getSlot(1).getItem().isEmpty(),"combination consumes inputs");
        ctx.succeed();
    }
    @GameTest public void supplyFilteringAndSmithInputs(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ctx.assertTrue(T1Supply.filterLoot(book(player,"minecraft:looting",1),()->0).isEmpty(),"removed book filtered even at successful rarity roll");
        ctx.assertTrue(T1Supply.filterLoot(book(player,"minecraft:sharpness",1),()->.1).isEmpty(),"retained book extremely rare");
        ctx.assertTrue(!T1Supply.filterLoot(book(player,"minecraft:sharpness",1),()->0).isEmpty(),"retained book rare success");
        MerchantOffer old=new MerchantOffer(new ItemCost(Items.EMERALD,10),new ItemStack(Items.DIAMOND_SWORD),3,10,.05f);
        MerchantOffer offer=T1Supply.smithOffer(player,old);
        ctx.assertTrue(!offer.satisfiedBy(new ItemStack(Items.EMERALD,64),new ItemStack(Items.EMERALD,64)),"emerald-only bypass closed");
        ItemStack damaged=new ItemStack(Items.DIAMOND_SWORD);damaged.setDamageValue(1);
        ctx.assertTrue(!offer.satisfiedBy(damaged,new ItemStack(Items.EMERALD,64)),"smith requires undamaged equipment");
        ctx.assertTrue(offer.satisfiedBy(new ItemStack(Items.DIAMOND_SWORD),new ItemStack(Items.EMERALD,32)),"smith accepts corresponding intact equipment and emeralds");
        ctx.succeed();
    }
    @GameTest public void bonusHooksAndTooltip(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack crossbow=new ItemStack(Items.CROSSBOW);
        EnchantmentHelper.updateEnchantments(crossbow,e->{e.set(T1Operations.enchantment(player,"minecraft:quick_charge").orElseThrow(),3);e.set(T1Operations.enchantment(player,"minecraft:piercing").orElseThrow(),4);});
        float normal=EnchantmentHelper.modifyCrossbowChargingTime(crossbow,player,1.25f);
        int piercing=EnchantmentHelper.getPiercingCount(ctx.getLevel(),crossbow,new ItemStack(Items.ARROW));
        crossbow.set(ModDataComponents.BOOK_OPTIONS,Map.of("minecraft:quick_charge",3,"minecraft:piercing",4));
        ctx.assertTrue(Math.abs(EnchantmentHelper.modifyCrossbowChargingTime(crossbow,player,1.25f)-(normal-.3f))<.0001,"Quick Charge subtracts 0.1 seconds per option");
        ctx.assertTrue(EnchantmentHelper.getPiercingCount(ctx.getLevel(),crossbow,new ItemStack(Items.ARROW))==piercing+4,"Piercing adds entity penetration");
        ItemStack sword=new ItemStack(Items.DIAMOND_SWORD);
        EnchantmentHelper.updateEnchantments(sword,e->{e.set(T1Operations.enchantment(player,"minecraft:mending").orElseThrow(),1);e.set(T1Operations.enchantment(player,"minecraft:sharpness").orElseThrow(),5);e.set(T1Operations.enchantment(player,"minecraft:fire_aspect").orElseThrow(),2);});
        ctx.assertTrue(EnchantmentHelper.modifyDurabilityToRepairFromXp(ctx.getLevel(),sword,1)==2,"vanilla mending remains 2 durability/XP");
        sword.set(ModDataComponents.BOOK_OPTIONS,Map.of("minecraft:mending",1,"minecraft:sharpness",4,"minecraft:fire_aspect",2));
        ctx.assertTrue(EnchantmentHelper.modifyDurabilityToRepairFromXp(ctx.getLevel(),sword,1)==4,"option mending repairs 4 durability/XP");
        List<net.minecraft.network.chat.Component> lines=new ArrayList<>();
        EnchantmentHelper.getEnchantmentsForCrafting(sword).addToTooltip(Item.TooltipContext.of(ctx.getLevel()),lines::add,TooltipFlag.NORMAL,sword);
        ctx.assertTrue(lines.stream().anyMatch(line->line.getString().contains("(+IV)")),"tooltip adds option next to enchantment");
        var target=net.minecraft.world.entity.EntityTypes.ZOMBIE.create(ctx.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        new net.minecraft.world.item.enchantment.effects.Ignite(LevelBasedValue.constant(8)).apply(ctx.getLevel(),2,new EnchantedItemInUse(sword,null,null,ignored->{}),target,target.position());
        ctx.assertTrue(target.getRemainingFireTicks()==240,"Fire Aspect adds four seconds to vanilla eight seconds");
        ctx.succeed();
    }
    @GameTest public void insufficientXpAndContextualEquipment(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack sword=new ItemStack(Items.DIAMOND_SWORD);
        ctx.assertTrue(!T1Operations.purchase(player,sword,"minecraft:sharpness"),"insufficient XP rejects purchase");
        ctx.assertTrue(T1Operations.enchantments(sword).isEmpty(),"failed purchase does not mutate item");
        ctx.assertTrue(EquipmentTreeResolver.resolve(new ItemStack(ModItems.DAGGERS.get(dev.itzgamesgd.moreminecrafts.equipment.WeaponBalance.Tier.IRON)))==EnchantmentTrees.Equipment.SWORD,"dagger uses Sword tree");
        ctx.assertTrue(new ItemStack(ModItems.DAGGERS.get(dev.itzgamesgd.moreminecrafts.equipment.WeaponBalance.Tier.IRON)).isDamageableItem(),"tiered basic weapons need functional durability branches");
        ctx.assertTrue(dev.itzgamesgd.moreminecrafts.enhancement.EnhancementCatalog.definition(ModItems.DAGGERS.get(dev.itzgamesgd.moreminecrafts.equipment.WeaponBalance.Tier.IRON)).isPresent(),"T1 basic weapon supports parallel Enhancement");
        ctx.assertTrue(EquipmentTreeResolver.resolve(new ItemStack(ModItems.GREATSWORDS.get(dev.itzgamesgd.moreminecrafts.equipment.WeaponBalance.Tier.IRON)))==EnchantmentTrees.Equipment.SWORD,"greatsword uses Sword tree");
        var menu=new AnvilMenu(1,player.getInventory());((T1AnvilModes)menu).moreminecrafts$button(player,2);
        menu.getSlot(0).set(book(player,"minecraft:sharpness",1));menu.getSlot(1).set(book(player,"minecraft:sharpness",1));menu.createResult();
        ctx.assertTrue(menu.getSlot(2).hasItem()&&!menu.getSlot(2).mayPickup(player),"combination preview requires XP before pickup");
        ctx.succeed();
    }
    @GameTest public void workstationProviderAndEquipmentChanges(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var pos=ctx.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        ctx.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE.defaultBlockState(),3);
        var provider=ctx.getLevel().getBlockState(pos).getMenuProvider(ctx.getLevel(),pos);
        var container=provider.createMenu(2,player.getInventory(),player);
        ctx.assertTrue(container instanceof EnchantingProgressionMenu,"all enchanting table interactions replace random enchanting");
        var menu=(EnchantingProgressionMenu)container;
        ctx.assertTrue(menu.tree().isEmpty(),"no equipment means no category tree");
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        ctx.assertTrue(menu.tree().stream().anyMatch(n->n.id().equals("minecraft:sharpness")),"equipment slot shows sword branches");
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_BOOTS));
        ctx.assertTrue(menu.tree().stream().noneMatch(n->n.id().equals("minecraft:sharpness")),"changing equipment removes unrelated branches");
        ctx.succeed();
    }
    @GameTest public void normalAnvilCannotMergeProgressionOrConflicts(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu=new AnvilMenu(1,player.getInventory());((T1AnvilModes)menu).moreminecrafts$button(player,1);
        ItemStack first=new ItemStack(Items.CROSSBOW),second=new ItemStack(Items.CROSSBOW);first.setDamageValue(50);
        EnchantmentHelper.updateEnchantments(first,e->e.set(T1Operations.enchantment(player,"moreminecrafts:crossbow_barrage").orElseThrow(),1));
        EnchantmentHelper.updateEnchantments(second,e->e.set(T1Operations.enchantment(player,"minecraft:multishot").orElseThrow(),1));
        menu.getSlot(0).set(first);menu.getSlot(1).set(second);menu.createResult();
        ctx.assertTrue(menu.getSlot(2).hasItem(),"normal anvil still repairs equipment");
        ctx.assertTrue(!T1Operations.enchantments(menu.getSlot(2).getItem()).containsKey("minecraft:multishot"),"repair cannot introduce structural conflict from sacrificial equipment");
        ctx.assertTrue(T1Operations.enchantments(menu.getSlot(2).getItem()).containsKey("moreminecrafts:crossbow_barrage"),"first item's progression is preserved");
        ctx.succeed();
    }
    @GameTest public void smithUpgradePreservesSubmittedProgress(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var villager=net.minecraft.world.entity.EntityTypes.VILLAGER.create(ctx.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        villager.setVillagerData(villager.getVillagerData().withProfession(ctx.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION).getOrThrow(net.minecraft.world.entity.npc.villager.VillagerProfession.WEAPONSMITH)));
        var offer=T1Supply.smithOffer(player,new MerchantOffer(new ItemCost(Items.EMERALD,32),new ItemStack(Items.DIAMOND_SWORD),3,10,0));
        MerchantOffers offers=villager.getOffers();offers.clear();offers.add(offer);
        ItemStack submitted=new ItemStack(Items.DIAMOND_SWORD);submitted.set(ModDataComponents.ENHANCEMENT_LEVEL,20);
        EnchantmentHelper.updateEnchantments(submitted,e->e.set(T1Operations.enchantment(player,"minecraft:sharpness").orElseThrow(),5));
        submitted.set(ModDataComponents.BOOK_OPTIONS,Map.of("minecraft:sharpness",4));
        MerchantContainer container=new MerchantContainer(villager);container.setItem(0,submitted);container.setItem(1,new ItemStack(Items.EMERALD,32));
        ItemStack output=container.getItem(2);
        ctx.assertTrue(output.getOrDefault(ModDataComponents.ENHANCEMENT_LEVEL,0)==20,"smith preserves Enhancement");
        ctx.assertTrue(T1Operations.option(output,"sharpness")==4,"smith preserves Book Options");
        ctx.assertTrue(T1Operations.enchantments(output).get("minecraft:sharpness")==5,"smith cannot downgrade purchased enchantments");
        ctx.succeed();
    }
    @GameTest public void normalRepairIgnoresSacrificialEnchantmentFees(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu=new AnvilMenu(1,player.getInventory());((T1AnvilModes)menu).moreminecrafts$button(player,1);
        ItemStack first=new ItemStack(Items.DIAMOND_SWORD),second=new ItemStack(Items.DIAMOND_SWORD);first.setDamageValue(100);
        EnchantmentHelper.updateEnchantments(first,e->e.set(T1Operations.enchantment(player,"minecraft:sharpness").orElseThrow(),5));
        EnchantmentHelper.updateEnchantments(second,e->e.set(T1Operations.enchantment(player,"minecraft:smite").orElseThrow(),5));
        menu.getSlot(0).set(first);menu.getSlot(1).set(second);menu.createResult();
        ctx.assertTrue(menu.getSlot(2).hasItem(),"conflicting sacrifice enchantments do not block ordinary repair");
        ctx.assertTrue(menu.getCost()<=3,"repair does not charge for enchantments never transferred");
        ctx.assertTrue(T1Operations.enchantments(second).containsKey("minecraft:smite"),"calculation does not mutate sacrificial input");
        ctx.succeed();
    }
    private static ItemStack book(net.minecraft.world.entity.player.Player player,String id,int level){
        var result=new ItemStack(Items.ENCHANTED_BOOK);EnchantmentHelper.updateEnchantments(result,e->e.set(T1Operations.enchantment(player,id).orElseThrow(),level));return result;
    }
    @Override public void invokeTestMethod(GameTestHelper context,Method method)throws ReflectiveOperationException{method.invoke(this,context);}
}
